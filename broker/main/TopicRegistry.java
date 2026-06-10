package broker.main;

import broker.model.MessageType;
import broker.model.ProtocolMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TopicRegistry {

    private final Map<String, Set<String>> topicSubscribers = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> onlineClients = new ConcurrentHashMap<>();
    private final Map<String, Map<String, List<ProtocolMessage>>> pendingMessages = new ConcurrentHashMap<>();


    public boolean createTopic(String topic) {
        return topicSubscribers.putIfAbsent(topic, ConcurrentHashMap.newKeySet()) == null;
    }

    public boolean topicExists(String topic) {
        return topicSubscribers.containsKey(topic);
    }

    public boolean subscribe(String topic, String clientId) {
        Set<String> subscribers = topicSubscribers.get(topic);

        if (subscribers == null) {
            return false;
        }

        subscribers.add(clientId);
        return true;
    }

    public boolean unsubscribe(String topic, String clientId) {
        Set<String> subscribers = topicSubscribers.get(topic);

        if (subscribers == null) {
            return false;
        }

        subscribers.remove(clientId);
        return true;
    }

    public boolean isSubscribed(String topic, String clientId) {
        Set<String> subscribers = topicSubscribers.get(topic);

        if (subscribers == null) {
            return false;
        }

        return subscribers.contains(clientId);
    }

    public boolean registerOnlineClient(String clientId, ClientHandler handler) {

    if (clientId == null || clientId.isBlank()) {
        return false;
    }

    if (onlineClients.containsKey(clientId)) {
        return false;
    }

    onlineClients.put(clientId, handler);
    return true;

    }

    public void unregisterOnlineClient(String clientId) {
        if (clientId != null) {
            onlineClients.remove(clientId);
        }
    }

    public void publish(String topic, ProtocolMessage originalMessage) { 
        Set<String> subscribers = topicSubscribers.get(topic);

        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        for (String subscriberId : subscribers) { // cria o modelo da mensagem de entrega
            ProtocolMessage deliverMessage = new ProtocolMessage(
                    MessageType.DELIVER,
                    originalMessage.getClientId(),
                    topic,
                    originalMessage.getPayload()
            );
            deliverMessage.setTimestamp(System.currentTimeMillis());

            ClientHandler onlineHandler = onlineClients.get(subscriberId);

            if (onlineHandler != null) {
                onlineHandler.send(deliverMessage);
            } else {
                bufferMessage(subscriberId, topic, deliverMessage);
            }
        }
    }

    private void bufferMessage(String clientId, String topic, ProtocolMessage message) {
        pendingMessages
                .computeIfAbsent(clientId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(topic, k -> java.util.Collections.synchronizedList(new ArrayList<>()))
                .add(message);
    }

    public List<ProtocolMessage> downloadPendingMessages(String clientId) { //Bufferização de mensagens, manda as mensagnes pendentes para os clientes 
        Map<String, List<ProtocolMessage>> clientPending = pendingMessages.get(clientId);

        if (clientPending == null || clientPending.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProtocolMessage> result = new ArrayList<>();

        for (List<ProtocolMessage> messages : clientPending.values()) {
            result.addAll(messages);
        }

        pendingMessages.remove(clientId);
        return result;
    }

    public Set<String> getSubscribedTopics(String clientId) {
        Set<String> result = ConcurrentHashMap.newKeySet();

        for (Map.Entry<String, Set<String>> entry : topicSubscribers.entrySet()) {
            if (entry.getValue().contains(clientId)) {
                result.add(entry.getKey());
            }
        }

        return result;
    }
    
    public java.util.List<String> getAllTopics() {
        java.util.List<String> topics = new java.util.ArrayList<>(topicSubscribers.keySet());
        java.util.Collections.sort(topics);
        return topics;
    }
}