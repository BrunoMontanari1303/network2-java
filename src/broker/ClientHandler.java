package broker;

import broker.model.MessageType;
import broker.model.ProtocolMessage;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final TopicRegistry topicRegistry;
    private final MessageReader reader;
    private final MessageWriter writer;

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
            while (true) {
                ProtocolMessage message = reader.read();

                if (message == null) {
                    break;
                }

                processMessage(message);
            }
        } catch (Exception e) {
            System.out.println("Cliente desconectado: " + socket.getInetAddress() + " - " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void processMessage(ProtocolMessage message) {
        if (message.getClientId() != null && !message.getClientId().isBlank()) {
            this.clientId = message.getClientId();
        }

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

            case DISCONNECT:
                handleDisconnect();
                break;

            default:
                sendError("Operacao nao suportada pelo broker.");
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
            sendSuccess(topic, "Topico criado com sucesso.");
        } else {
            sendError(topic, "Topico ja existe.");
        }
    }

    private void handleSubscribe(ProtocolMessage message) {
        String topic = message.getTopic();

        if (topic == null || topic.isBlank()) {
            sendError("Nome do topico é obrigatorio.");
            return;
        }

        boolean subscribed = topicRegistry.subscribe(topic, this);

        if (subscribed) {
            sendSuccess(topic, "Inscricao realizada com sucesso.");
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

        boolean unsubscribed = topicRegistry.unsubscribe(topic, this);

        if (unsubscribed) {
            sendSuccess(topic, "Inscricao removida com sucesso.");
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

        topicRegistry.publish(topic, message);
        sendSuccess(topic, "Mensagem publicada com sucesso.");
    }

    private void handleDisconnect() {
        sendSuccess(null, "Desconexao realizada.");
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
        try {
            topicRegistry.removeClientFromAllTopics(this);
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public String getClientId() {
        return clientId;
    }
}