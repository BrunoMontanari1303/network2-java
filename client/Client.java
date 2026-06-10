package client;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import broker.security.Certificate;

import broker.model.ProtocolMessage;
import broker.model.MessageType;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class Client {

	private Socket socket;
	private MessageReader reader;
	private MessageWriter writer;
	private String clientId;
	private KeyPair keyPair;
	private ClientGUI gui;
	private volatile boolean authenticated = false;

	private final Set<String> topicosInscritos = java.util.Collections.synchronizedSet(new HashSet<>());
	private final Set<String> todosOsTopicos = java.util.Collections.synchronizedSet(new HashSet<>());

	
	// conecta o cliente ao broker
	public void connect(String host, int port) throws IOException {

		socket = new Socket(host, port);
		reader = new MessageReader(socket);
		writer = new MessageWriter(socket);

		System.out.println("Conectado ao broker");

	}

	public void setGUI(ClientGUI gui) {
		this.gui = gui;
	}

	public Client(String clientId) {
	    this.clientId = clientId;
	}

	// retorna os topico que o cliente está inscrito
	public Set<String> getTopicosInscritos() {
		return topicosInscritos;
	}

	// metodo para se inscrever em um topico
	public void subscribe(String topic) throws IOException {
		if (!authenticated) {
			System.out.println("Cliente ainda não autenticado.");
			return;
		}
		ProtocolMessage msg = new ProtocolMessage(MessageType.SUBSCRIBE, clientId, topic, null);
		writer.send(msg);
	}

	// metodo para publicar uma mensagem em um topico
	public void publish(String topic, String content) throws IOException {
		if (!authenticated) {
			System.out.println("Cliente ainda não autenticado.");
			return;
		}
		ProtocolMessage msg = new ProtocolMessage(MessageType.PUBLISH, clientId, topic, content);
		writer.send(msg);

	}

	// metodo para criar um novo topico
	public void createTopic(String topic) throws IOException {
		if (!authenticated) {
			System.out.println("Cliente ainda não autenticado.");
			return;
		}
		ProtocolMessage msg = new ProtocolMessage(MessageType.CREATE_TOPIC, clientId, topic, null);
		writer.send(msg);
	}

	// metodo para remover cliente de um topico
	public void unsubscribe(String topic) throws IOException {
		if (!authenticated) {
			System.out.println("Cliente ainda não autenticado.");
			return;
		}
		ProtocolMessage msg = new ProtocolMessage(MessageType.UNSUBSCRIBE, clientId, topic, null);
		writer.send(msg);
	}
	
	// metodo para registrar cliente de um topico
	public void register(String username, String password) {
	    this.clientId = username;
	    ensureKeysForUser(username);
	    authenticated = false;

	    Certificate cert = new Certificate(
	            username,
	            getPublicKeyString(),
	            ""
	    );

	    ProtocolMessage msg = new ProtocolMessage(MessageType.REGISTER_REQUEST, null, null, null);
	    msg.setUsername(username);
	    msg.setPassword(password);
	    msg.setCertificate(cert);

	    writer.send(msg);
	}

	public void login(String username, String password) {
		authenticated = false;
		this.clientId = username;
		ensureKeysForUser(username);
		
		ProtocolMessage msg = new ProtocolMessage(MessageType.LOGIN_REQUEST, null, null, null);
		msg.setUsername(username);
		msg.setPassword(password);

		writer.send(msg);
	}	

	// metodo responsavel por ficar ouvido mensagens do broker
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
						String texto = "[" + msg.getTopic() + "] " + msg.getClientId() + ": " + msg.getPayload();

						System.out.println(texto);

						if (gui != null) {
							gui.adicionarMensagem(texto);
						}

						break;

					case SUCCESS:

						System.out.println("[BROKER] " + msg.getPayload());

						if ("Inscricao realizada com sucesso.".equals(msg.getPayload()) && msg.getTopic() != null) {
							topicosInscritos.add(msg.getTopic());

							if (gui != null) {
								gui.atualizarListaTopicos();
								gui.adicionarMensagem("[BROKER] Inscrito no tópico: " + msg.getTopic());
							}
						}

						if ("Inscricao removida com sucesso.".equals(msg.getPayload()) && msg.getTopic() != null) {
							topicosInscritos.remove(msg.getTopic());

							if (gui != null) {
								gui.atualizarListaTopicos();
								gui.adicionarMensagem("[BROKER] Saiu do tópico: " + msg.getTopic());
							}
						}

						if ("Topico criado com sucesso.".equals(msg.getPayload())) {
							if (gui != null) {
								gui.adicionarMensagem("[BROKER] Tópico criado com sucesso.");
							}
						}
						
						if ("Topico criado com sucesso.".equals(msg.getPayload())) {
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
						break;

					case REGISTER_OK:
					    System.out.println("[CADASTRO] " + msg.getPayload());

					    if (msg.getCertificate() != null) {
					        ClientCertificateStore.saveCertificate(clientId, msg.getCertificate());
					    }

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

					    if (!ClientCertificateStore.exists(clientId)) {
					        System.out.println("[AUTH FAIL] Certificado do cliente nao encontrado.");
					        if (gui != null) {
					            gui.onAuthFail("Certificado do cliente nao encontrado.");
					        }
					        break;
					    }

					    Certificate cert = ClientCertificateStore.loadCertificate(clientId);
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
						// Cliente autenticado com sucesso.
						authenticated = true;
						// Solicita imediatamente todas as mensagens pendentes armazenadas.
						requestPendingMessages();
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
						
						String challenge = msg.getPayload(); // Recebe o desafio aleatório enviado pelo broker
						
						String signedChallenge = signChallenge(challenge); // Assina o desafio usando a chave privada do cliente.
						 
						ProtocolMessage response = new ProtocolMessage(MessageType.AUTH_RESPONSE, clientId, null, null); // Cria a resposta ao desafio
						
						response.setSignature(signedChallenge); // Coloca a assinatura gerada
						writer.send(response); // Envia a resposta para o broker
						System.out.println("Desafio assinado e enviado ao broker.");
						
						break;
						
					case LIST_TOPICS_RESPONSE:
					    todosOsTopicos.clear();

					    if (msg.getTopics() != null) {
					        todosOsTopicos.addAll(msg.getTopics());
					    }

					    if (gui != null) {
					        gui.atualizarListaTodosTopicos();
					        gui.adicionarMensagem("[BROKER] Lista de todos os tópicos atualizada.");
					    }
					    break;

					default:
						System.out.println("[INFO] Mensagem recebida: " + msg.toJson().toString());
						break;
					}

				}
				// verifica erro de conexão
			} catch (IOException e) {
				System.out.println("Desconectado do broker.");
			}
		}).start();
	}

	// envia solicitacao para receber mensagens pendentes
	public void requestPendingMessages() {
		ProtocolMessage msg = new ProtocolMessage(MessageType.DOWNLOAD_PENDING, clientId, null, null);
		writer.send(msg);
	}

	// Solicita ao broker todas as mensagens que ficaram armazenadas enquanto o cliente estava desconectado.
	// Envia ao broker o certificado digital do cliente.
	// O broker irá verificar se este certificado foi realmente assinado pela Autoridade Certificadora (CA).
	public void authenticate(Certificate cert) {

		ProtocolMessage msg = new ProtocolMessage(MessageType.AUTH_REQUEST, clientId, null, null);

		// Anexa o certificado do cliente na mensagem
		msg.setCertificate(cert);

		 // Envia para o broker iniciar a autenticação
		writer.send(msg);
	}

	public String getPublicKeyString() {

		return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
	}

	public Certificate createCertificate(String assinatura) {
		String publicKey = getPublicKeyString();

		return new Certificate(clientId, publicKey, assinatura);
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
	
	public void requestAllTopics() {
	    ProtocolMessage msg = new ProtocolMessage(
	            MessageType.LIST_TOPICS_REQUEST,
	            clientId,
	            null,
	            null
	    );
	    writer.send(msg);
	}
	
	public Set<String> getTodosOsTopicos() {
	    return todosOsTopicos;
	}
	
}