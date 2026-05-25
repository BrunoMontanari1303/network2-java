package src.broker.model;
import src.org.json.JSONObject;

public class ProtocolMessage {
    private MessageType type;
    private String clientId;
    private String topic;
    private String payload;
    private Long timestamp;
    
    private String certificate;
    private String signature;
    private String encryptedKey;
    

    public ProtocolMessage() {
    }

    public ProtocolMessage(MessageType type, String clientId, String topic, String payload) {
        this.type = type;
        this.clientId = clientId;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public ProtocolMessage(
            MessageType type,
            String clientId,
            String topic,
            String payload,
            String certificate,
            String signature,
            String encryptedKey,
            Long timestamp
    ) {
        this.type = type;
        this.clientId = clientId;
        this.topic = topic;
        this.payload = payload;
        this.certificate = certificate;
        this.signature = signature;
        this.encryptedKey = encryptedKey;
        this.timestamp = timestamp;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put("type", type != null ? type.name() : JSONObject.NULL);
        json.put("clientId", clientId != null ? clientId : JSONObject.NULL);
        json.put("topic", topic != null ? topic : JSONObject.NULL);
        json.put("payload", payload != null ? payload : JSONObject.NULL);
        json.put("certificate", certificate != null ? certificate : JSONObject.NULL);
        json.put("signature", signature != null ? signature : JSONObject.NULL);
        json.put("encryptedKey", encryptedKey != null ? encryptedKey : JSONObject.NULL);
        json.put("timestamp", timestamp != null ? timestamp : JSONObject.NULL);

        return json;
    }

    public static ProtocolMessage fromJson(JSONObject json) {
        ProtocolMessage message = new ProtocolMessage();

        if (!json.isNull("type")) {
            message.setType(MessageType.valueOf(json.getString("type")));
        }

        message.setClientId(json.optString("clientId", null));
        message.setTopic(json.optString("topic", null));
        message.setPayload(json.optString("payload", null));
        message.setCertificate(json.optString("certificate", null));
        message.setSignature(json.optString("signature", null));
        message.setEncryptedKey(json.optString("encryptedKey", null));

        if (!json.isNull("timestamp")) {
            message.setTimestamp(json.getLong("timestamp"));
        }

        return message;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey(String encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}