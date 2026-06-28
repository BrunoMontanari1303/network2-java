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
    // Armazena mensagens pendentes para clientes offline.
    private final Map<String, Map<String, List<ProtocolMessage>>> pendingMessages = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> storedEncryptedTopicKeys = new ConcurrentHashMap<>();


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
        
        removeStoredEncryptedTopicKey(topic, clientId);
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

        // Obtém todos os clientes inscritos no tópico informado.
        Set<String> subscribers = topicSubscribers.get(topic);

        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        // Percorre cada inscrito no tópico.
        for (String subscriberId : subscribers) { // cria o modelo da mensagem de entrega
            // Cria a mensagem que será entregue para os inscritos.
            ProtocolMessage deliverMessage = new ProtocolMessage(
                    MessageType.DELIVER,
                    originalMessage.getId(),
                    topic,
                    null
            );
            
            deliverMessage.setEncryptedPayload(originalMessage.getEncryptedPayload());
            deliverMessage.setPayloadIv(originalMessage.getPayloadIv());
            deliverMessage.setTimestamp(System.currentTimeMillis());

            // Verifica se o inscrito está online.
            ClientHandler onlineHandler = onlineClients.get(subscriberId);

            // Cliente online recebe imediatamente.
            if (onlineHandler != null) {
                onlineHandler.send(deliverMessage);
                // Cliente offline mensagem é armazenada para download futuro.
            } else {
                bufferMessage(subscriberId, topic, deliverMessage);
            }
        }
    }
    // Cria automaticamente a estrutura: cliente -> tópico -> lista de mensagens e adiciona a nova mensagem ao buffer.
    private void bufferMessage(String clientId, String topic, ProtocolMessage message) {
        pendingMessages
                .computeIfAbsent(clientId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(topic, k -> java.util.Collections.synchronizedList(new ArrayList<>()))
                .add(message);
    }

  
    public List<ProtocolMessage> downloadPendingMessages(String clientId) { //Bufferização de mensagens, manda as mensagnes pendentes para os clientes 
          // Busca todas as mensagens pendentes que pertecem ao cliente.
        Map<String, List<ProtocolMessage>> clientPending = pendingMessages.get(clientId);

        if (clientPending == null || clientPending.isEmpty()) {
            return new ArrayList<>();
        }

        List<ProtocolMessage> result = new ArrayList<>();

        // Junta mensagens de todos os tópicos em uma única lista para envio.
        for (List<ProtocolMessage> messages : clientPending.values()) {
            result.addAll(messages);
        }

        // Remove as mensagens do buffer, pois já foram entregues ao cliente.
        pendingMessages.remove(clientId);
        return result;
    }
    
    public void storeEncryptedTopicKey(String topic, String subscriberId, String encryptedTopicKey) {
        storedEncryptedTopicKeys
                .computeIfAbsent(subscriberId, k -> new ConcurrentHashMap<>())
                .put(topic, encryptedTopicKey);
    }

    public java.util.Map<String, String> getStoredEncryptedTopicKeys(String subscriberId) {
        java.util.Map<String, String> keys = storedEncryptedTopicKeys.get(subscriberId);

        if (keys == null) {
            return new ConcurrentHashMap<>();
        }

        return new ConcurrentHashMap<>(keys);
    }

    public void removeStoredEncryptedTopicKey(String topic, String subscriberId) {
        java.util.Map<String, String> keys = storedEncryptedTopicKeys.get(subscriberId);

        if (keys != null) {
            keys.remove(topic);

            if (keys.isEmpty()) {
                storedEncryptedTopicKeys.remove(subscriberId);
            }
        }
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
    
    public java.util.Set<String> getSubscribers(String topic) {
        java.util.Set<String> subscribers = topicSubscribers.get(topic);

        if (subscribers == null) {
            return java.util.Collections.emptySet();
        }

        return new java.util.HashSet<>(subscribers);
    } 
    
    public java.util.List<String> getAllTopics() {
        java.util.List<String> topics = new java.util.ArrayList<>(topicSubscribers.keySet());
        java.util.Collections.sort(topics);
        return topics;
    }
    
    public ClientHandler getOnlineClient(String id) {
        return onlineClients.get(id);
    }
    
    
}