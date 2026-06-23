package broker.security;

public class Certificate {

    private String clientId;
    private String publicKey;
    private String signature;

    public Certificate() {
    }

    public Certificate(String clientId, String publicKey, String signature) {
        this.clientId = clientId;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSignature() {
        return signature;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}