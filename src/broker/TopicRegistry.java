package broker;

import broker.model.MessageType;
import broker.model.ProtocolMessage;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TopicRegistry {

    //armazena nome do topico e clientes incritos e cada
    private final Map<String, Set<ClientHandler>> topics = new ConcurrentHashMap<>();

    public boolean createTopic(String topic) { //cria topico novo
        return topics.putIfAbsent(topic, ConcurrentHashMap.newKeySet()) == null;
    }

    public boolean topicExists(String topic) {//verifica se o topico já existe 
        return topics.containsKey(topic);
    }

    public boolean subscribe(String topic, ClientHandler client) { //busca inscritos no topico
        Set<ClientHandler> subscribers = topics.get(topic);

        if (subscribers == null) {
            return false;
        }

        subscribers.add(client); //adiciona cliente ao topico 
        return true;
    }

    public boolean unsubscribe(String topic, ClientHandler client) {//remove cliente do topico 
        Set<ClientHandler> subscribers = topics.get(topic);

        if (subscribers == null) {
            return false;
        }

        subscribers.remove(client);
        return true;
    }

    public void removeClientFromAllTopics(ClientHandler client) { //remove cliente de todos os topicos 
        for (Set<ClientHandler> subscribers : topics.values()) { //percorre todos os topicos 
            subscribers.remove(client);//remove cliente de todos ele 
        }
    }

    public void publish(String topic, ProtocolMessage originalMessage) { //responsavel por entregar mensagem aos incritos 
        Set<ClientHandler> subscribers = topics.get(topic); //pega todos os inscritos daquele topico 

        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        ProtocolMessage deliverMessage = new ProtocolMessage(//cria mensagem do tipo deliver, a mensagem que sera enviada aos clientes 
                MessageType.DELIVER,
                originalMessage.getClientId(),
                topic,
                originalMessage.getPayload()
        );

        deliverMessage.setTimestamp(System.currentTimeMillis());

        for (ClientHandler subscriber : subscribers) { //percorre todos os inscritos e envia mensagem para cada cliente 
            subscriber.send(deliverMessage);
        }
    }
}