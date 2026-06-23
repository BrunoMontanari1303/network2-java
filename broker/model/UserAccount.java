package broker.model;

public class UserAccount {

    private String username;
    private String passwordHash;
    private String publicKey;

    public UserAccount() {
    }

    public UserAccount(String username, String passwordHash, String publicKey) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.publicKey = publicKey;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}