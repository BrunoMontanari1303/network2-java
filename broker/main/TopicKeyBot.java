package broker.main;

import broker.security.CryptoUtils;

import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TopicKeyBot {

    public static final String BOT_ID = "bot";

    private final Map<String, SecretKey> topicKeys = new ConcurrentHashMap<>();

    public void createTopic(String topic) {
        topicKeys.putIfAbsent(topic, CryptoUtils.generateAESKey());
    }

    public boolean topicExists(String topic) {
        return topicKeys.containsKey(topic);
    }

    public String getEncryptedTopicKeyForUser(String topic, String userPublicKeyBase64) {
        SecretKey topicKey = topicKeys.get(topic);

        if (topicKey == null || userPublicKeyBase64 == null || userPublicKeyBase64.isBlank()) {
            return null;
        }

        PublicKey publicKey = CryptoUtils.publicKeyFromBase64(userPublicKeyBase64);

        return CryptoUtils.encryptRSA(topicKey.getEncoded(), publicKey);
    }

    public Map<String, String> rotateTopicKey(String topic, Map<String, String> subscriberPublicKeys) {
        SecretKey newKey = CryptoUtils.generateAESKey();
        topicKeys.put(topic, newKey);

        Map<String, String> encryptedPerUser = new HashMap<>();

        for (Map.Entry<String, String> entry : subscriberPublicKeys.entrySet()) {
            String subscriberId = entry.getKey();
            String publicKeyBase64 = entry.getValue();

            if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
                continue;
            }

            PublicKey publicKey = CryptoUtils.publicKeyFromBase64(publicKeyBase64);
            String encryptedTopicKey = CryptoUtils.encryptRSA(newKey.getEncoded(), publicKey);

            encryptedPerUser.put(subscriberId, encryptedTopicKey);
        }

        return encryptedPerUser;
    }
}