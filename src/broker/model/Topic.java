package broker.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Topic {
    private final String name;
    private final Set<String> subscribers;

    public Topic(String name) {
        this.name = name;
        this.subscribers = ConcurrentHashMap.newKeySet();
    }

    public String getName() {
        return name;
    }

    public Set<String> getSubscribers() {
        return subscribers;
    }

    public void addSubscriber(String clientId) {
        subscribers.add(clientId);
    }

    public void removeSubscriber(String clientId) {
        subscribers.remove(clientId);
    }

    public boolean hasSubscriber(String clientId) {
        return subscribers.contains(clientId);
    }
}