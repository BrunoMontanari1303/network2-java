package client;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import broker.model.MessageType;
import broker.model.ProtocolMessage;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;
import broker.security.Certificate;

public class Client {

	private Socket socket;
	private MessageReader reader;
	private MessageWriter writer;
	private String id;
	private KeyPair keyPair;
	private ClientGUI gui;
	private volatile boolean authenticated = false;
	private volatile boolean brokerValidated = false;
	private javax.crypto.SecretKey sessionKey;
	private volatile boolean sessionReady = false;

	private final Set<String> subscribedTopics = java.util.Collections.synchronizedSet(new HashSet<>());
	private final java.util.Map<String, javax.crypto.SecretKey> topicKeys = new java.util.concurrent.ConcurrentHashMap<>();
	private final Set<String> allTopics = java.util.Collections.synchronizedSet(new HashSet<>());

	public Client(String clientId) {
		this.id = clientId;
	}

	public void connect(String host, int port) throws IOException {
		socket = new Socket(host, port);
		reader = new MessageReader(socket);
		writer = new MessageWriter(socket);

		System.out.println("Conectado ao broker");
	}

	public void setGUI(ClientGUI gui) {
		this.gui = gui;
	}

	public Set<String> getSubscribedTopics() {
		return subscribedTopics;
	}

	public Set<String> getAllTopics() {
		return allTopics;
	}

	public void subscribe(String topic) throws IOException {
		if (!authenticated) {
			System.out.println("Cliente ainda não autenticado.");
			return;
		}

		ProtocolMessage msg = new ProtocolMessage(MessageType.SUBSCRIBE, id, topic, null);
		writer.send(msg);
	}

	public void publish(String topic, String content) throws IOException {
	    if (!authenticated) {
	        System.out.println("Cliente ainda não autenticado.");
	        return;
	    }

	    javax.crypto.SecretKey topicKey = topicKeys.get(topic);

	    if (topicKey == null) {
	        System.out.println("Chave do tópico não disponível. Aguarde compartilhamento.");
	        if (gui != null) {
	            gui.adicionarMensagem("[ERRO] Chave do tópico não disponível.");
	        }
	        return;
	    }

	    byte[] iv = broker.security.CryptoUtils.generateIV();
	    String encryptedPayload = broker.security.CryptoUtils.encryptAES(content, topicKey, iv);

	    ProtocolMessage msg = new ProtocolMessage(MessageType.PUBLISH, id, topic, null);
	    msg.setEncryptedPayload(encryptedPayload);
	    msg.setPayloadIv(Base64.getEncoder().encodeToString(iv));

	    writer.send(msg);
	}

	public void createTopic(String topic) throws IOException {
	    if (!authenticated) {
	        System.out.println("Cliente ainda não autenticado.");
	        return;
	    }

	    ProtocolMessage msg = new ProtocolMessage(MessageType.CREATE_TOPIC, id, topic, null);
	    writer.send(msg);
	}

	public void unsubscribe(String topic) throws IOException {
		if (!authenticated) {
			System.out.println("Cliente ainda não autenticado.");
			return;
		}

		ProtocolMessage msg = new ProtocolMessage(MessageType.UNSUBSCRIBE, id, topic, null);
		writer.send(msg);
	}

	public void register(String username, String password) {

		this.id = username;
		ensureKeysForUser(username);
		authenticated = false;

		if (!sessionReady) {
		    System.out.println("Sessao segura ainda nao estabelecida.");
		    if (gui != null) {
		        gui.adicionarStatusLogin("Sessao segura ainda nao estabelecida.");
		    }
		    return;
		}
		
		if (!brokerValidated) {
			System.out.println("Broker ainda nao validado.");
			if (gui != null) {
				gui.adicionarStatusLogin("Broker ainda nao validado.");
			}
			return;
		}

		if (!ClientCertificateStore.exists(username)) {
			if (gui != null) {
				gui.onRegisterFail("Certificado offline do cliente não encontrado.");
			}
			System.out.println("[CADASTRO FAIL] Certificado offline do cliente não encontrado.");
			return;
		}

		Certificate cert = ClientCertificateStore.loadCertificate(username);

		ProtocolMessage msg = new ProtocolMessage(MessageType.REGISTER_REQUEST, null, null, null);
		msg.setUsername(username);
		msg.setPassword(password);
		msg.setCertificate(cert);

		writer.send(msg);
	}

	public void login(String username, String password) {
		this.id = username;
		ensureKeysForUser(username);
		authenticated = false;

		if (!sessionReady) {
		    System.out.println("Sessao segura ainda nao estabelecida.");
		    if (gui != null) {
		        gui.adicionarStatusLogin("Sessao segura ainda nao estabelecida.");
		    }
		    return;
		}
		
		if (!brokerValidated) {
			System.out.println("Broker ainda nao validado.");
			if (gui != null) {
				gui.adicionarStatusLogin("Broker ainda nao validado.");
			}
			return;
		}

		ProtocolMessage msg = new ProtocolMessage(MessageType.LOGIN_REQUEST, null, null, null);
		msg.setUsername(username);
		msg.setPassword(password);

		writer.send(msg);
	}

	public void startListening() {
		new Thread(() -> {
			try {
				while (true) {
					ProtocolMessage msg = reader.read();

					if (msg == null) {
						break;
					}

					switch (msg.getType()) {

					case DELIVER:
						javax.crypto.SecretKey topicKey = topicKeys.get(msg.getTopic());

					    if (topicKey == null) {
					        String aviso = "[ERRO] Mensagem recebida do tópico " + msg.getTopic()
					                + ", mas a chave do tópico não está disponível.";
					        System.out.println(aviso);

					        if (gui != null) {
					            gui.adicionarMensagem(aviso);
					        }
					        break;
					    }

					    byte[] iv = Base64.getDecoder().decode(msg.getPayloadIv());
					    String textoDecifrado = broker.security.CryptoUtils.decryptAES(
					            msg.getEncryptedPayload(),
					            topicKey,
					            iv
					    );

					    String texto = "[" + msg.getTopic() + "] " + msg.getId() + ": " + textoDecifrado;

					    System.out.println(texto);

					    if (gui != null) {
					        gui.adicionarMensagem(texto);
					    }
					    break;

					case SUCCESS:
						System.out.println("[BROKER] " + msg.getPayload());

						if (gui != null) {
							gui.adicionarMensagem("[BROKER] " + msg.getPayload());
						}

						if ("Inscrição realizada com sucesso.".equals(msg.getPayload()) && msg.getTopic() != null) {
							subscribedTopics.add(msg.getTopic());

							if (gui != null) {
								gui.atualizarListaTopicos();
								gui.adicionarMensagem("[BROKER] Inscrito no tópico: " + msg.getTopic());
							}
						}

						if ("Inscrição removida com sucesso.".equals(msg.getPayload()) && msg.getTopic() != null) {
							subscribedTopics.remove(msg.getTopic());

							if (gui != null) {
								gui.atualizarListaTopicos();
								gui.adicionarMensagem("[BROKER] Saiu do tópico: " + msg.getTopic());
							}
						}

						if ("Tópico criado com sucesso.".equals(msg.getPayload())) {
							if (gui != null) {
								gui.adicionarMensagem("[BROKER] Tópico criado com sucesso.");
							}
							requestAllTopics();
						}

						break;

					case ERROR:
						System.out.println("[ERRO] " + msg.getPayload());

						if (gui != null) {
							gui.adicionarMensagem("[ERRO] " + msg.getPayload());
						}
						break;

					case DOWNLOAD_OK:
						System.out.println("[BROKER] " + msg.getPayload());

						if (gui != null) {
							gui.adicionarMensagem("[BROKER] " + msg.getPayload());
						}
						break;

					case REGISTER_OK:
						System.out.println("[CADASTRO] " + msg.getPayload());

						if (gui != null) {
							gui.onRegisterSuccess(msg.getPayload());
						}
						break;

					case REGISTER_FAIL:
						System.out.println("[CADASTRO FAIL] " + msg.getPayload());

						if (gui != null) {
							gui.onRegisterFail(msg.getPayload());
						}
						break;

					case LOGIN_OK:
						System.out.println("[LOGIN] " + msg.getPayload());

						if (gui != null) {
							gui.onLoginSuccess(msg.getPayload());
						}

						if (!ClientCertificateStore.exists(id)) {
							System.out.println("[AUTH FAIL] Certificado do cliente nao encontrado.");
							if (gui != null) {
								gui.onAuthFail("Certificado do cliente nao encontrado.");
							}
							break;
						}

						Certificate cert = ClientCertificateStore.loadCertificate(id);
						authenticate(cert);
						break;

					case LOGIN_FAIL:
						System.out.println("[LOGIN FAIL] " + msg.getPayload());

						if (gui != null) {
							gui.onLoginFail(msg.getPayload());
						}
						break;

					case AUTH_OK:
						System.out.println("Autenticado com sucesso!");
						authenticated = true;

						requestAllTopics();

						if (gui != null) {
							gui.onAuthSuccess(msg.getPayload());
						}
						break;

					case AUTH_FAIL:
						System.out.println("[AUTH FAIL] " + msg.getPayload());

						if (gui != null) {
							gui.onAuthFail(msg.getPayload());
						}
						break;

					case AUTH_CHALLENGE:
						String challenge = msg.getPayload();
						String signedChallenge = signChallenge(challenge);

						ProtocolMessage response = new ProtocolMessage(MessageType.AUTH_RESPONSE, id, null, null);

						response.setSignature(signedChallenge);
						writer.send(response);

						System.out.println("Desafio assinado e enviado ao broker.");
						break;

					case LIST_TOPICS_RESPONSE:
						allTopics.clear();

						if (msg.getTopics() != null) {
							allTopics.addAll(msg.getTopics());
						}

						if (gui != null) {
							gui.atualizarListaTodosTopicos();
							gui.adicionarMensagem("[BROKER] Lista de todos os tópicos atualizada.");
						}
						break;

					case BROKER_CERT:
						Certificate brokerCert = msg.getCertificate();

						if (validateBrokerCertificate(brokerCert)) {
							brokerValidated = true;
							System.out.println("[BROKER CERT] Certificado do broker validado com sucesso.");

							if (gui != null) {
								gui.adicionarStatusLogin("[BROKER CERT OK] Broker validado com sucesso.");
							}

							sendBrokerCertOk();
						} else {
							brokerValidated = false;
							System.out.println("[BROKER CERT FAIL] Certificado do broker invalido.");

							if (gui != null) {
								gui.adicionarStatusLogin("[BROKER CERT FAIL] Certificado do broker invalido.");
							}

							sendBrokerCertFail();
							socket.close();
						}
						break;
						
					case SESSION_KEY_OK:
					    sessionReady = true;
					    System.out.println("[SESSAO] Chave de sessão estabelecida com sucesso.");

					    if (gui != null) {
					        gui.adicionarStatusLogin("[SESSAO OK] Chave de sessão estabelecida.");
					    }
					    break;

					case SESSION_KEY_FAIL:
					    sessionReady = false;
					    System.out.println("[SESSAO FAIL] " + msg.getPayload());

					    if (gui != null) {
					        gui.adicionarStatusLogin("[SESSAO FAIL] " + msg.getPayload());
					    }
					    break;
					    
					
					    
					case TOPIC_KEY_SHARE:
						byte[] topicKeyBytes = broker.security.CryptoUtils.decryptRSA(
					            msg.getEncryptedTopicKey(),
					            keyPair.getPrivate()
					    );

					    javax.crypto.SecretKey receivedTopicKey =
					            broker.security.CryptoUtils.aesKeyFromBytes(topicKeyBytes);

					    topicKeys.put(msg.getTopic(), receivedTopicKey);

					    if (gui != null) {
					        gui.adicionarMensagem("[BROKER] Chave do tópico " + msg.getTopic() + " recebida.");
					    }
					    break;
					    
					case TOPIC_KEYS_SYNC_DONE:
					    if (gui != null) {
					        gui.adicionarMensagem("[BROKER] " + msg.getPayload());
					    }

					    requestPendingMessages();
					    break;
					    					
					default:
						System.out.println("[INFO] Mensagem recebida: " + msg.toJson().toString());
						break;
					}
				}
			} catch (IOException e) {
				System.out.println("Desconectado do broker.");
			}
		}).start();
	}

	public void requestPendingMessages() {
		ProtocolMessage msg = new ProtocolMessage(MessageType.DOWNLOAD_PENDING, id, null, null);
		writer.send(msg);
	}

	public void authenticate(Certificate cert) {
		ProtocolMessage msg = new ProtocolMessage(MessageType.AUTH_REQUEST, id, null, null);
		msg.setCertificate(cert);
		writer.send(msg);
	}

	public void requestAllTopics() {
		ProtocolMessage msg = new ProtocolMessage(MessageType.LIST_TOPICS_REQUEST, id, null, null);
		writer.send(msg);
	}

	public String getPublicKeyString() {
		return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
	}

	private String signChallenge(String challenge) {
		return broker.security.CryptoUtils.sign(challenge, keyPair.getPrivate());
	}

	private void generateKeys() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			this.keyPair = generator.generateKeyPair();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void ensureKeysForUser(String username) {
		try {
			if (ClientKeyStore.keyPairExists(username)) {
				this.keyPair = ClientKeyStore.loadKeyPair(username);
			} else {
				generateKeys();
				ClientKeyStore.saveKeyPair(username, keyPair);
			}
		} catch (Exception e) {
			throw new RuntimeException("Erro ao preparar chaves do usuário", e);
		}
	}

	private boolean validateBrokerCertificate(Certificate cert) {
		if (cert == null) {
			return false;
		}

		if (cert.getClientId() == null || cert.getClientId().isBlank()) {
			return false;
		}

		if (cert.getPublicKey() == null || cert.getPublicKey().isBlank()) {
			return false;
		}

		if (cert.getSignature() == null || cert.getSignature().isBlank()) {
			return false;
		}

		String data = cert.getClientId() + cert.getPublicKey();

		return broker.security.CryptoUtils.verify(data, cert.getSignature(), broker.security.CAStore.getPublicKey());
	}

	private void sendBrokerCertOk() {
		ProtocolMessage msg = new ProtocolMessage(MessageType.BROKER_CERT_OK, id, null,
				"Certificado do broker validado com sucesso.");
		writer.send(msg);
	}

	private void sendBrokerCertFail() {
		ProtocolMessage msg = new ProtocolMessage(MessageType.BROKER_CERT_FAIL, id, null,
				"Falha na validacao do certificado do broker.");
		writer.send(msg);
	}
	
	public void establishSession() {
	    try {
	        this.sessionKey = broker.security.CryptoUtils.generateAESKey();

	        String encryptedSessionKey = broker.security.CryptoUtils.encryptRSA(
	                sessionKey.getEncoded(),
	                broker.security.BrokerKeyStore.getPublicKey()
	        );

	        ProtocolMessage msg = new ProtocolMessage(
	                MessageType.SESSION_KEY_EXCHANGE,
	                id,
	                null,
	                null
	        );
	        msg.setEncryptedSessionKey(encryptedSessionKey);

	        // manda a troca ainda em claro
	        writer.send(msg);

	        // depois disso, o cliente já instala a chave localmente
	        reader.setSessionKey(sessionKey);
	        writer.setSessionKey(sessionKey);

	    } catch (Exception e) {
	        throw new RuntimeException("Erro ao estabelecer chave de sessão", e);
	    }
	}
	
	private void ensureTopicKey(String topic) {
	    topicKeys.computeIfAbsent(topic, t -> broker.security.CryptoUtils.generateAESKey());
	}
		
}