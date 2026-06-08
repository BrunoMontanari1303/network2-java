package broker.security;

public class Certificate {

    private String clientId;
    private String publicKey;
    private String signature;

    public Certificate(String clientId,
                       String publicKey,
                       String signature) {
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
}
