package broker;

import broker.model.MessageType;
import broker.model.ProtocolMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TopicRegistry {

    private final Map<String, Set<ClientHandler>> topics = new ConcurrentHashMap<>();

    public boolean createTopic(String topic) {
        return topics.putIfAbsent(topic, ConcurrentHashMap.newKeySet()) == null;
    }

    public boolean topicExists(String topic) {
        return topics.containsKey(topic);
    }

    public boolean subscribe(String topic, ClientHandler client) {
        Set<ClientHandler> subscribers = topics.get(topic);

        if (subscribers == null) {
            return false;
        }

        subscribers.add(client);
        return true;
    }

    public boolean unsubscribe(String topic, ClientHandler client) {
        Set<ClientHandler> subscribers = topics.get(topic);

        if (subscribers == null) {
            return false;
        }

        subscribers.remove(client);
        return true;
    }

    public void removeClientFromAllTopics(ClientHandler client) {
        for (Set<ClientHandler> subscribers : topics.values()) {
            subscribers.remove(client);
        }
    }

    public void publish(String topic, ProtocolMessage originalMessage) {
        Set<ClientHandler> subscribers = topics.get(topic);

        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        ProtocolMessage deliverMessage = new ProtocolMessage(
                MessageType.DELIVER,
                originalMessage.getClientId(),
                topic,
                originalMessage.getPayload()
        );

        deliverMessage.setTimestamp(System.currentTimeMillis());

        for (ClientHandler subscriber : subscribers) {
            subscriber.send(deliverMessage);
        }
    }
}