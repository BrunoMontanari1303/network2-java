package broker.main;

import broker.model.UserAccount;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRegistry {

    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();

    public boolean register(String username, String passwordHash, String publicKey) {
        UserAccount account = new UserAccount(username, passwordHash, publicKey);
        return users.putIfAbsent(username, account) == null;
    }

    public UserAccount findByUsername(String username) {
        return users.get(username);
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }
}