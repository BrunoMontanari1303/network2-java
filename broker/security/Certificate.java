package broker.security;

public class Certificate {

    private String id;
    private String publicKey;
    private String signature;

    public Certificate() {
    }

    public Certificate(String id, String publicKey, String signature) {
        this.id = id;
        this.publicKey = publicKey;
        this.signature = signature;
    }

    public String getId() {
        return id;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getSignature() {
        return signature;
    }

    public void setId(String clientId) {
        this.id = clientId;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}