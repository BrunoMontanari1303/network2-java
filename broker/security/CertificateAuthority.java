package broker.security;

import java.security.PrivateKey;
import java.security.PublicKey;

public class CertificateAuthority {

    private static final String PRIVATE_KEY_FILE = "ca_private.key";
    private static final String PUBLIC_KEY_FILE = "ca_public.key";

    private static final CertificateAuthority INSTANCE = new CertificateAuthority();

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    private CertificateAuthority() {
        this.privateKey = KeyIO.loadPrivateKey(PRIVATE_KEY_FILE);
        this.publicKey = KeyIO.loadPublicKey(PUBLIC_KEY_FILE);
    }

    public static CertificateAuthority getInstance() {
        return INSTANCE;
    }

    public String signCertificate(String clientId, String publicKeyBase64) {
        String data = clientId + publicKeyBase64;
        return CryptoUtils.sign(data, privateKey);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}