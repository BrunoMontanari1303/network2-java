package broker.main;

import broker.model.MessageType;
import broker.model.ProtocolMessage;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;

import java.io.IOException;
import java.net.Socket;
//import java.security.Signature;
import broker.security.Certificate;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final TopicRegistry topicRegistry;
    private final MessageReader reader;
    private final MessageWriter writer;
    private volatile boolean running = true;

    private String clientId;

    public ClientHandler(Socket socket, TopicRegistry topicRegistry) throws IOException {
        this.socket = socket;
        this.topicRegistry = topicRegistry;
        this.reader = new MessageReader(socket);
        this.writer = new MessageWriter(socket);
    }

    @Override
    public void run() {
        try {
            while (running) {

                ProtocolMessage message = reader.read();

                if (message == null) {
                    running = false;
                    break;
                }

                if (clientId == null) {

                if (message.getType() != MessageType.AUTH_REQUEST) {
                    sendError("Cliente nao autenticado. Conexao recusada.");
                    closeConnection();
                    break;
                }

                Certificate cert = message.getCertificate();

                if (cert == null) {
                    sendError("Certificado ausente. Conexao recusada.");
                    closeConnection();
                    break;
                }

                if (!validateCertificate(cert)) {
                    sendError("Certificado invalido. Conexao recusada.");
                    closeConnection();
                    break;
                }

                String id = cert.getClientId();

                if (id == null || id.isBlank()) {
                    sendError("ClientId inválido.");
                    closeConnection();
                    break;
                }

                if (!topicRegistry.registerOnlineClient(id, this)) {
                    sendError("ID ja em uso. Conexao encerrada.");
                    closeConnection();
                    running = false;
                    break;
                }

                this.clientId = id;

                ProtocolMessage response = new ProtocolMessage(
                    MessageType.AUTH_OK,
                    "broker",
                    null,
                    "Autenticado com sucesso."
                    );

                    send(response);
                    continue;
                }
                

                // 🔁 depois que autenticou, processa normalmente
                processMessage(message);
            }

        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void processMessage(ProtocolMessage message) {
        System.out.println("Recebido: " + message.getType());
        MessageType type = message.getType();

        if (type == null) {
            sendError("Tipo de mensagem inválido.");
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

            case DISCONNECT:
                handleDisconnect();
                break;

            default:
                sendError("Operação não suportada pelo broker.");
        }
    }

    private void handleCreateTopic(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do tópico é obrigatório.");
            return;
        }

        boolean created = topicRegistry.createTopic(topic);

        if (created) {
            sendSuccess(topic, "Tópico criado com sucesso.");
        } else {
            sendError(topic, "Tópico já existe.");
        }
    }

    private void handleSubscribe(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do tópico é obrigatório.");
            return;
        }

        boolean subscribed = topicRegistry.subscribe(topic, clientId);

        if (subscribed) {
            sendSuccess(topic, "Inscrição realizada com sucesso.");
        } else {
            sendError(topic, "Tópico não existe.");
        }
    }

    private void handleUnsubscribe(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do tópico é obrigatório.");
            return;
        }

        boolean unsubscribed = topicRegistry.unsubscribe(topic, clientId);

        if (unsubscribed) {
            sendSuccess(topic, "Inscrição removida com sucesso.");
        } else {
            sendError(topic, "Tópico não existe.");
        }
    }

    private void handlePublish(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do tópico é obrigatório.");
            return;
        }

        if (!topicRegistry.topicExists(topic)) {
            sendError(topic, "Tópico não existe.");
            return;
        }

        if (!topicRegistry.isSubscribed(topic, clientId)) {
            sendError(topic, "Cliente não inscrito no tópico. Publicação bloqueada.");
            return;
        }

        topicRegistry.publish(topic, message);
        sendSuccess(topic, "Mensagem publicada com sucesso.");
    }

    private void handleDownloadPending() {
        List<ProtocolMessage> pending = topicRegistry.downloadPendingMessages(clientId);

        for (ProtocolMessage msg : pending) {
            send(msg);
        }

        ProtocolMessage response = new ProtocolMessage(
                MessageType.DOWNLOAD_OK,
                "broker",
                null,
                "Download de mensagens pendentes concluído."
        );
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void handleDisconnect() {
        sendSuccess(null, "Desconexão realizada.");
        closeConnection();
    }

    public void send(ProtocolMessage message) {
        writer.send(message);
    }

    private void sendSuccess(String topic, String text) {
        ProtocolMessage response = new ProtocolMessage(
                MessageType.SUCCESS,
                "broker",
                topic,
                text
        );
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void sendError(String text) {
        sendError(null, text);
    }

    private void sendError(String topic, String text) {
        ProtocolMessage response = new ProtocolMessage(
                MessageType.ERROR,
                "broker",
                topic,
                text
        );
        response.setTimestamp(System.currentTimeMillis());
        send(response);
    }

    private void closeConnection() {
        running = false;

        try {
            topicRegistry.unregisterOnlineClient(clientId);
            socket.close();
        } catch (IOException ignored) {
        }
    }
    public String getClientId() {
        return clientId;
    }

    
    private boolean validateCertificate(Certificate cert) {

        if (cert == null) {
            return false;
        }

        if (cert.getClientId() == null) {
            return false;
        }

        if (cert.getPublicKey() == null) {
            return false;
        }

        if (cert.getSignature() == null) {
            return false;
        }

        return true;
    }
} 