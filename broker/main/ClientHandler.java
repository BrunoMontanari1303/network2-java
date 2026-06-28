package broker.main;

import broker.model.MessageType;
import broker.model.ProtocolMessage;
import broker.model.UserAccount;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;
import broker.security.BrokerKeyStore;
import broker.security.Certificate;
import broker.security.CryptoUtils;
import broker.security.PasswordUtils;

import java.io.IOException;
import java.net.Socket;
import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final TopicRegistry topicRegistry;
    private final UserRegistry userRegistry;
    private final MessageReader reader;
    private final MessageWriter writer;
    private volatile boolean running = true;
    private String pendingChallenge;
    private Certificate pendingCertificate;
    private String id;
    private boolean loginOk = false;
    private javax.crypto.SecretKey sessionKey;
    private boolean sessionReady = false;

    public ClientHandler(Socket socket, TopicRegistry topicRegistry, UserRegistry userRegistry) throws IOException {
        this.socket = socket;
        this.topicRegistry = topicRegistry;
        this.userRegistry = userRegistry;
        this.reader = new MessageReader(socket);
        this.writer = new MessageWriter(socket);
    }

    private void sendBrokerCertificate() {
        Certificate brokerCert = broker.security.BrokerCertificateStore.loadCertificate();

        ProtocolMessage msg = new ProtocolMessage(
                MessageType.BROKER_CERT,
                "broker",
                null,
                "Certificado do broker"
        );
        msg.setCertificate(brokerCert);
        msg.setTimestamp(System.currentTimeMillis());

        send(msg);
    }
    
    @Override
    public void run() {
        try {
        	sendBrokerCertificate();
        	
            while (running) {
                ProtocolMessage message = reader.read();

                if (message == null) {
                    running = false;
                    break;
                }
                
                if (!sessionReady) {
                    if (message.getType() == MessageType.SESSION_KEY_EXCHANGE) {
                        handleSessionKeyExchange(message);
                        continue;
                    }

                    sendError("Sessao segura ainda nao estabelecida.");
                    closeConnection();
                    break;
                }

                if (!loginOk) {
                    if (message.getType() == MessageType.REGISTER_REQUEST) {
                        handleRegister(message);
                        continue;
                    }

                    if (message.getType() == MessageType.LOGIN_REQUEST) {
                        handleLogin(message);
                        continue;
                    }

                    sendError("Login ainda nao realizado.");
                    closeConnection();
                    break;
                }

                if (id == null) {
                    if (message.getType() == MessageType.AUTH_REQUEST) {
                        handleAuthRequest(message);
                        continue;
                    }

                    if (message.getType() == MessageType.AUTH_RESPONSE) {
                        handleAuthResponse(message);
                        continue;
                    }

                    sendError("Cliente nao autenticado. Conexao recusada.");
                    closeConnection();
                    break;
                }

                processMessage(message);
            }

        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void processMessage(ProtocolMessage message) {
        MessageType type = message.getType();

        if (type == null) {
            sendError("Tipo de mensagem invalido.");
            return;
        }

        switch (type) {
            case CREATE_TOPIC:
                handleCreateTopic(message);
                break;

            case SUBSCRIBE:
                handleSubscribe(message);
                break;

            case UNSUBSCRIBE:
                handleUnsubscribe(message);
                break;

            case PUBLISH:
                handlePublish(message);
                break;

            case DOWNLOAD_PENDING:
                handleDownloadPending();
                break;

            case LIST_TOPICS_REQUEST:
                handleListTopics();
                break;

            case DISCONNECT:
                handleDisconnect();
                break;
                
            case TOPIC_KEY_SHARE:
                handleTopicKeyShare(message);
                break;

            default:
                sendError("Operacao nao suportada pelo broker.");
        }
    }

    private void handleRegister(ProtocolMessage message) {
        String username = message.getUsername();
        String password = message.getPassword();
        Certificate cert = message.getCertificate();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            sendSimple(MessageType.REGISTER_FAIL, "broker", "Usuario e senha sao obrigatorios.");
            return;
        }

        if (cert == null) {
            sendSimple(MessageType.REGISTER_FAIL, "broker", "Certificado ausente no cadastro.");
            return;
        }

        if (cert.getPublicKey() == null || cert.getPublicKey().isBlank()) {
            sendSimple(MessageType.REGISTER_FAIL, "broker", "Chave publica ausente no cadastro.");
            return;
        }

        if (!validateClientCertificate(cert)) {
            sendSimple(MessageType.REGISTER_FAIL, "broker", "Certificado invalido.");
            return;
        }

        if (!username.equals(cert.getClientId())) {
            sendSimple(MessageType.REGISTER_FAIL, "broker", "Usuario e certificado nao correspondem.");
            return;
        }

        if (userRegistry.exists(username)) {
            sendSimple(MessageType.REGISTER_FAIL, "broker", "Usuario ja existe.");
            return;
        }

        String passwordHash = PasswordUtils.hash(password);
        boolean created = userRegistry.register(username, passwordHash, cert.getPublicKey());

        if (!created) {
            sendSimple(MessageType.REGISTER_FAIL, "broker", "Nao foi possivel cadastrar.");
            return;
        }

        sendSimple(MessageType.REGISTER_OK, "broker", "Cadastro realizado com sucesso.");
    }

    private void handleLogin(ProtocolMessage message) {
        String username = message.getUsername();
        String password = message.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            sendSimple(MessageType.LOGIN_FAIL, "broker", "Usuario e senha sao obrigatorios.");
            return;
        }

        UserAccount account = userRegistry.findByUsername(username);

        if (account == null) {
            sendSimple(MessageType.LOGIN_FAIL, "broker", "Usuario nao encontrado.");
            return;
        }

        if (!PasswordUtils.verify(password, account.getPasswordHash())) {
            sendSimple(MessageType.LOGIN_FAIL, "broker", "Senha invalida.");
            return;
        }

        this.loginOk = true;
        sendSimple(MessageType.LOGIN_OK, "broker", "Login realizado com sucesso.");
    }

    private void handleAuthRequest(ProtocolMessage message) {
        if (pendingChallenge != null) {
            sendAuthFail("Ja existe um desafio pendente.");
            closeConnection();
            return;
        }

        Certificate cert = message.getCertificate();

        if (cert == null) {
            sendAuthFail("Certificado ausente.");
            closeConnection();
            return;
        }

        if (!validateClientCertificate(cert)) {
            sendAuthFail("Certificado invalido.");
            closeConnection();
            return;
        }

        UserAccount account = userRegistry.findByUsername(cert.getClientId());

        if (account == null) {
            sendAuthFail("Conta nao encontrada.");
            closeConnection();
            return;
        }

        if (!cert.getPublicKey().equals(account.getPublicKey())) {
            sendAuthFail("Chave publica nao corresponde a conta cadastrada.");
            closeConnection();
            return;
        }

        this.pendingCertificate = cert;
        this.pendingChallenge = UUID.randomUUID().toString();

        ProtocolMessage challenge = new ProtocolMessage(
                MessageType.AUTH_CHALLENGE,
                "broker",
                null,
                pendingChallenge
        );
        challenge.setTimestamp(System.currentTimeMillis());
        send(challenge);
    }

    private void handleAuthResponse(ProtocolMessage message) {
        if (pendingCertificate == null || pendingChallenge == null) {
            sendAuthFail("Nenhum desafio pendente.");
            closeConnection();
            return;
        }

        String signedChallenge = message.getSignature();

        if (signedChallenge == null || signedChallenge.isBlank()) {
            sendAuthFail("Assinatura do desafio ausente.");
            closeConnection();
            return;
        }

        PublicKey clientPublicKey = CryptoUtils.publicKeyFromBase64(pendingCertificate.getPublicKey());

        boolean valid = CryptoUtils.verify(pendingChallenge, signedChallenge, clientPublicKey);

        if (!valid) {
            sendAuthFail("Assinatura invalida. Cliente nao autenticado.");
            closeConnection();
            return;
        }

        String id = pendingCertificate.getClientId();

        if (id == null || id.isBlank()) {
            sendAuthFail("ClientId invalido.");
            closeConnection();
            return;
        }

        if (!topicRegistry.registerOnlineClient(id, this)) {
            sendAuthFail("ID ja em uso. Conexao encerrada.");
            closeConnection();
            running = false;
            return;
        }

        this.id = id;
        this.pendingChallenge = null;
        this.pendingCertificate = null;

        ProtocolMessage response = new ProtocolMessage(
                MessageType.AUTH_OK,
                "broker",
                null,
                "Autenticado com sucesso."
        );
        response.setTimestamp(System.currentTimeMillis());
        send(response);

        // reenvia as topicKeys cifradas já salvas para esse cliente
        sendStoredTopicKeysToClient(this.id);
    }

    private boolean validateClientCertificate(Certificate cert) {
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

        return CryptoUtils.verify(
                data,
                cert.getSignature(),
                BrokerKeyStore.getPublicKey()
        );
    }

    private void handleCreateTopic(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do topico e obrigatorio.");
            return;
        }

        boolean created = topicRegistry.createTopic(topic, id);

        if (created) {
            sendSuccess(topic, "Tópico criado com sucesso.");
        } else {
            sendError(topic, "Topico ja existe.");
        }
    }

    private void handleSubscribe(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do topico e obrigatorio.");
            return;
        }

        boolean subscribed = topicRegistry.subscribe(topic, id);

        if (subscribed) {
            sendSuccess(topic, "Inscrição realizada com sucesso.");
            notifyTopicOwnerToShareKey(topic, id);
        } else {
            sendError(topic, "Topico nao existe.");
        }
    }

    private void handleUnsubscribe(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do topico e obrigatorio.");
            return;
        }

        boolean unsubscribed = topicRegistry.unsubscribe(topic, id);

        if (unsubscribed) {
            sendSuccess(topic, "Inscrição removida com sucesso.");
        } else {
            sendError(topic, "Topico nao existe.");
        }
    }

    private void handlePublish(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do topico e obrigatorio.");
            return;
        }

        if (!topicRegistry.topicExists(topic)) {
            sendError(topic, "Topico nao existe.");
            return;
        }

        if (!topicRegistry.isSubscribed(topic, id)) {
            sendError(topic, "Cliente nao inscrito no topico. Publicacao bloqueada.");
            return;
        }

        topicRegistry.publish(topic, message);
        sendSuccess(topic, "Mensagem publicada com sucesso.");
    }

    private void handleDownloadPending() {
        List<ProtocolMessage> pending = topicRegistry.downloadPendingMessages(id);

        for (ProtocolMessage msg : pending) {
            send(msg);
        }

        ProtocolMessage response = new ProtocolMessage(
                MessageType.DOWNLOAD_OK,
                "broker",
                null,
                "Download de mensagens pendentes concluido."
        );
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void handleListTopics() {
        List<String> topics = topicRegistry.getAllTopics();

        ProtocolMessage response = new ProtocolMessage(
                MessageType.LIST_TOPICS_RESPONSE,
                "broker",
                null,
                "Lista de tópicos carregada."
        );
        response.setTopics(topics);
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void handleDisconnect() {
        sendSuccess(null, "Desconexao realizada.");
        closeConnection();
    }
    
    private void handleSessionKeyExchange(ProtocolMessage message) {
        String encryptedSessionKey = message.getEncryptedSessionKey();

        if (encryptedSessionKey == null || encryptedSessionKey.isBlank()) {
            sendSimple(MessageType.SESSION_KEY_FAIL, "broker", "Chave de sessao ausente.");
            closeConnection();
            return;
        }

        try {
            byte[] aesKeyBytes = broker.security.CryptoUtils.decryptRSA(
                    encryptedSessionKey,
                    broker.security.BrokerKeyStore.getPrivateKey()
            );

            this.sessionKey = broker.security.CryptoUtils.aesKeyFromBytes(aesKeyBytes);
            this.sessionReady = true;

            // ativa a cifragem do canal do lado do broker
            reader.setSessionKey(sessionKey);
            writer.setSessionKey(sessionKey);

            sendSimple(MessageType.SESSION_KEY_OK, "broker", "Chave de sessao estabelecida.");
        } catch (Exception e) {
            sendSimple(MessageType.SESSION_KEY_FAIL, "broker", "Falha ao processar chave de sessao.");
            closeConnection();
        }
    }
    
    private void handleTopicKeyShare(ProtocolMessage message) {
        String targetId = message.getTargetId();

        if (targetId == null || targetId.isBlank()) {
            sendError("Destino da chave do topico ausente.");
            return;
        }

        if (message.getEncryptedTopicKey() == null || message.getEncryptedTopicKey().isBlank()) {
            sendError("Chave do topico cifrada ausente.");
            return;
        }

        topicRegistry.storeEncryptedTopicKey(
                message.getTopic(),
                targetId,
                message.getEncryptedTopicKey()
        );

        ClientHandler targetHandler = topicRegistry.getOnlineClient(targetId);

        if (targetHandler != null) {
            ProtocolMessage forward = new ProtocolMessage(
                    MessageType.TOPIC_KEY_SHARE,
                    message.getId(),
                    message.getTopic(),
                    null
            );

            forward.setEncryptedTopicKey(message.getEncryptedTopicKey());
            forward.setTimestamp(System.currentTimeMillis());

            targetHandler.send(forward);
        }
    }

    public void send(ProtocolMessage message) {
        writer.send(message);
    }

    private void sendSimple(MessageType type, String clientId, String payload) {
        ProtocolMessage response = new ProtocolMessage(type, clientId, null, payload);
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void sendSuccess(String topic, String text) {
        ProtocolMessage response = new ProtocolMessage(MessageType.SUCCESS, "broker", topic, text);
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void sendError(String text) {
        sendError(null, text);
    }

    private void sendError(String topic, String text) {
        ProtocolMessage response = new ProtocolMessage(MessageType.ERROR, "broker", topic, text);
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void sendAuthFail(String text) {
        ProtocolMessage response = new ProtocolMessage(MessageType.AUTH_FAIL, "broker", null, text);
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }
    
    private void sendStoredTopicKeysToClient(String userId) {
        java.util.Map<String, String> keys = topicRegistry.getStoredEncryptedTopicKeys(userId);

        for (java.util.Map.Entry<String, String> entry : keys.entrySet()) {
            ProtocolMessage msg = new ProtocolMessage(
                    MessageType.TOPIC_KEY_SHARE,
                    "broker",
                    entry.getKey(),
                    null
            );

            msg.setEncryptedTopicKey(entry.getValue());
            msg.setTimestamp(System.currentTimeMillis());
            send(msg);
        }

        ProtocolMessage done = new ProtocolMessage(
                MessageType.TOPIC_KEYS_SYNC_DONE,
                "broker",
                null,
                "Sincronizacao de chaves de topicos concluida."
        );
        done.setTimestamp(System.currentTimeMillis());
        send(done);
    }

    private void closeConnection() {
        running = false;

        try {
            topicRegistry.unregisterOnlineClient(id);
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public String getId() {
        return id;
    }
    
    private void notifyTopicOwnerToShareKey(String topic, String newSubscriberId) {
        String ownerId = topicRegistry.getTopicOwner(topic);

        if (ownerId == null || ownerId.equals(newSubscriberId)) {
            return;
        }

        ClientHandler ownerHandler = topicRegistry.getOnlineClient(ownerId);
        UserAccount subscriberAccount = userRegistry.findByUsername(newSubscriberId);

        if (ownerHandler == null || subscriberAccount == null) {
            return;
        }

        ProtocolMessage request = new ProtocolMessage(
                MessageType.TOPIC_KEY_REQUEST,
                "broker",
                topic,
                "Compartilhe a chave do tópico com o novo inscrito."
        );

        request.setTargetId(newSubscriberId);
        request.setTargetPublicKey(subscriberAccount.getPublicKey());
        request.setTimestamp(System.currentTimeMillis());

        ownerHandler.send(request);
    }
}