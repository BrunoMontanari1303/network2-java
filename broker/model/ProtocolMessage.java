package broker.model;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;


import broker.security.Certificate;

public class ProtocolMessage {
	
    private MessageType type;
    private String id;
    private String topic;
    private String payload;
    private String username;
    private String password;
    private String signature;
    private Certificate certificate;
    private List<String> topics;
    private Long timestamp;
    private String encryptedSessionKey;
    private String targetId;
    private String targetPublicKey;
    private String encryptedTopicKey;
    private String encryptedPayload;
    private String payloadIv;
    

    public ProtocolMessage() {
    }

    public ProtocolMessage(MessageType type, String clientId, String topic, String payload) {
        this.type = type;
        this.id = clientId;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put("type", type != null ? type.name() : JSONObject.NULL);
        json.put("id", id != null ? id : JSONObject.NULL);
        json.put("topic", topic != null ? topic : JSONObject.NULL);
        json.put("payload", payload != null ? payload : JSONObject.NULL);
        json.put("username", username != null ? username : JSONObject.NULL);
        json.put("password", password != null ? password : JSONObject.NULL);
        json.put("signature", signature != null ? signature : JSONObject.NULL);
        json.put("timestamp", timestamp != null ? timestamp : JSONObject.NULL);
        json.put("encryptedSessionKey", encryptedSessionKey != null ? encryptedSessionKey : JSONObject.NULL);
        json.put("targetId", targetId != null ? targetId : JSONObject.NULL);
        json.put("targetPublicKey", targetPublicKey != null ? targetPublicKey : JSONObject.NULL);
        json.put("encryptedTopicKey", encryptedTopicKey != null ? encryptedTopicKey : JSONObject.NULL);
        json.put("encryptedPayload", encryptedPayload != null ? encryptedPayload : JSONObject.NULL);
        json.put("payloadIv", payloadIv != null ? payloadIv : JSONObject.NULL);

        if (certificate != null) {
            JSONObject certJson = new JSONObject();
            certJson.put("id", certificate.getClientId());
            certJson.put("publicKey", certificate.getPublicKey());
            certJson.put("signature", certificate.getSignature());
            json.put("certificate", certJson);
        } else {
            json.put("certificate", JSONObject.NULL);
        }

        if (topics != null) {
            json.put("topics", topics);
        } else {
            json.put("topics", JSONObject.NULL);
        }

        return json;
    }

    public static ProtocolMessage fromJson(JSONObject json) {
        ProtocolMessage message = new ProtocolMessage();

        if (!json.isNull("type")) {
            message.setType(MessageType.valueOf(json.getString("type")));
        }

        message.setId(json.optString("id", null));
        message.setTopic(json.optString("topic", null));
        message.setPayload(json.optString("payload", null));
        message.setUsername(json.optString("username", null));
        message.setPassword(json.optString("password", null));
        message.setSignature(json.optString("signature", null));
        message.setEncryptedSessionKey(json.optString("encryptedSessionKey", null));
        message.setTargetId(json.optString("targetId", null));
        message.setTargetPublicKey(json.optString("targetPublicKey", null));
        message.setEncryptedTopicKey(json.optString("encryptedTopicKey", null));
        message.setEncryptedPayload(json.optString("encryptedPayload", null));
        message.setPayloadIv(json.optString("payloadIv", null));

        if (!json.isNull("timestamp")) {
            message.setTimestamp(json.getLong("timestamp"));
        }

        if (!json.isNull("certificate")) {
            JSONObject certJson = json.getJSONObject("certificate");
            Certificate cert = new Certificate(
                    certJson.optString("clientId", null),
                    certJson.optString("publicKey", null),
                    certJson.optString("signature", null)
            );
            message.setCertificate(cert);
        }

        if (!json.isNull("topics")) {
            List<String> topics = new ArrayList<>();
            JSONArray array = json.getJSONArray("topics");

            for (int i = 0; i < array.length(); i++) {
                topics.add(array.getString(i));
            }

            message.setTopics(topics);
        }

        return message;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String clientId) {
        this.id = clientId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getEncryptedSessionKey() {
        return encryptedSessionKey;
    }

    public void setEncryptedSessionKey(String encryptedSessionKey) {
        this.encryptedSessionKey = encryptedSessionKey;
    }
    
    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetPublicKey() {
        return targetPublicKey;
    }

    public void setTargetPublicKey(String targetPublicKey) {
        this.targetPublicKey = targetPublicKey;
    }

    public String getEncryptedTopicKey() {
        return encryptedTopicKey;
    }

    public void setEncryptedTopicKey(String encryptedTopicKey) {
        this.encryptedTopicKey = encryptedTopicKey;
    }

    public String getEncryptedPayload() {
        return encryptedPayload;
    }

    public void setEncryptedPayload(String encryptedPayload) {
        this.encryptedPayload = encryptedPayload;
    }

    public String getPayloadIv() {
        return payloadIv;
    }

    public void setPayloadIv(String payloadIv) {
        this.payloadIv = payloadIv;
    }
}