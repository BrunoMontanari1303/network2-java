package broker.model;

import broker.security.Certificate;
import org.json.JSONObject;

public class ProtocolMessage {

    private MessageType type;
    private String clientId;
    private String topic;
    private String payload;
    private String username;
    private String password;

    private Certificate certificate;   // certificado do cliente
    private String signature;          // assinatura digital
    private String encryptedKey;       // (opcional futuro)
    private Long timestamp;

    public ProtocolMessage() {
    }

    // construtor básico (mais usado no broker/client)
    public ProtocolMessage(MessageType type, String clientId, String topic, String payload) {
        this.type = type;
        this.clientId = clientId;
        this.topic = topic;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    // construtor completo (auth / segurança)
    public ProtocolMessage(
            MessageType type,
            String clientId,
            String topic,
            String payload,
            Certificate certificate,
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

    // ================= JSON =================

    public JSONObject toJson() {
        JSONObject json = new JSONObject();

        json.put("type", type != null ? type.name() : JSONObject.NULL);
        json.put("clientId", clientId != null ? clientId : JSONObject.NULL);
        json.put("topic", topic != null ? topic : JSONObject.NULL);
        json.put("payload", payload != null ? payload : JSONObject.NULL);
        json.put("username", username != null ? username : JSONObject.NULL);
        json.put("password", password != null ? password : JSONObject.NULL);

        if (certificate != null) {

            JSONObject certJson = new JSONObject();

            certJson.put("clientId",
                    certificate.getClientId());

            certJson.put("publicKey",
                    certificate.getPublicKey());

            certJson.put("signature",
                    certificate.getSignature());

            json.put("certificate", certJson);
        }

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
        message.setUsername(json.optString("username", null));
        message.setPassword(json.optString("password", null));

        if (!json.isNull("certificate")) {

            JSONObject certJson = json.getJSONObject("certificate");

            Certificate cert = new Certificate(
                    certJson.getString("clientId"),
                    certJson.getString("publicKey"),
                    certJson.getString("signature")
            );

            message.setCertificate(cert);
        }

        message.setSignature(json.optString("signature", null));
        message.setEncryptedKey(json.optString("encryptedKey", null));

        if (!json.isNull("timestamp")) {
            message.setTimestamp(json.getLong("timestamp"));
        }

        return message;
    }

    // ================= GETTERS / SETTERS =================

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

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
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