package broker.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Topic {
    private final String name;
    private final Set<String> subscribers;

    public Topic(String name) {
        this.name = name;
        this.subscribers = ConcurrentHashMap.newKeySet();//Cria conjunto thread-safe
    }

    public String getName() { //retorna nome do topico 
        return name;
    }

    public Set<String> getSubscribers() { //retorna inscritos
        return subscribers;
    }

    public void addSubscriber(String clientId) { //adiciona cliente no topico
        subscribers.add(clientId);
    }

    public void removeSubscriber(String clientId) { //remove cliente do topico
        subscribers.remove(clientId);
    }

    public boolean hasSubscriber(String clientId) { //verifica se cliente está inscrito
        return subscribers.contains(clientId);
    }
}