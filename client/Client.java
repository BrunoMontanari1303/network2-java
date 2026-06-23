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
    private String clientId;
    private KeyPair keyPair;
    private ClientGUI gui;
    private volatile boolean authenticated = false;

    private final Set<String> topicosInscritos =
            java.util.Collections.synchronizedSet(new HashSet<>());

    private final Set<String> todosOsTopicos =
            java.util.Collections.synchronizedSet(new HashSet<>());

    public Client(String clientId) {
        this.clientId = clientId;
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

    public Set<String> getTopicosInscritos() {
        return topicosInscritos;
    }

    public Set<String> getTodosOsTopicos() {
        return todosOsTopicos;
    }

    public void subscribe(String topic) throws IOException {
        if (!authenticated) {
            System.out.println("Cliente ainda não autenticado.");
            return;
        }

        ProtocolMessage msg = new ProtocolMessage(MessageType.SUBSCRIBE, clientId, topic, null);
        writer.send(msg);
    }

    public void publish(String topic, String content) throws IOException {
        if (!authenticated) {
            System.out.println("Cliente ainda não autenticado.");
            return;
        }

        ProtocolMessage msg = new ProtocolMessage(MessageType.PUBLISH, clientId, topic, content);
        writer.send(msg);
    }

    public void createTopic(String topic) throws IOException {
        if (!authenticated) {
            System.out.println("Cliente ainda não autenticado.");
            return;
        }

        ProtocolMessage msg = new ProtocolMessage(MessageType.CREATE_TOPIC, clientId, topic, null);
        writer.send(msg);
    }

    public void unsubscribe(String topic) throws IOException {
        if (!authenticated) {
            System.out.println("Cliente ainda não autenticado.");
            return;
        }

        ProtocolMessage msg = new ProtocolMessage(MessageType.UNSUBSCRIBE, clientId, topic, null);
        writer.send(msg);
    }

    public void register(String username, String password) {
        this.clientId = username;
        ensureKeysForUser(username);
        authenticated = false;

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
        this.clientId = username;
        ensureKeysForUser(username);
        authenticated = false;

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
                            String texto = "[" + msg.getTopic() + "] "
                                    + msg.getClientId() + ": "
                                    + msg.getPayload();

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

                            if ("Inscrição realizada com sucesso.".equals(msg.getPayload())
                                    && msg.getTopic() != null) {
                                topicosInscritos.add(msg.getTopic());

                                if (gui != null) {
                                    gui.atualizarListaTopicos();
                                    gui.adicionarMensagem("[BROKER] Inscrito no tópico: " + msg.getTopic());
                                }
                            }

                            if ("Inscrição removida com sucesso.".equals(msg.getPayload())
                                    && msg.getTopic() != null) {
                                topicosInscritos.remove(msg.getTopic());

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
                            authenticated = true;

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
                            String challenge = msg.getPayload();
                            String signedChallenge = signChallenge(challenge);

                            ProtocolMessage response = new ProtocolMessage(
                                    MessageType.AUTH_RESPONSE,
                                    clientId,
                                    null,
                                    null
                            );

                            response.setSignature(signedChallenge);
                            writer.send(response);

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
            } catch (IOException e) {
                System.out.println("Desconectado do broker.");
            }
        }).start();
    }

    public void requestPendingMessages() {
        ProtocolMessage msg = new ProtocolMessage(
                MessageType.DOWNLOAD_PENDING,
                clientId,
                null,
                null
        );
        writer.send(msg);
    }

    public void authenticate(Certificate cert) {
        ProtocolMessage msg = new ProtocolMessage(
                MessageType.AUTH_REQUEST,
                clientId,
                null,
                null
        );
        msg.setCertificate(cert);
        writer.send(msg);
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
}