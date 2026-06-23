package broker.security;

import java.security.PrivateKey;
import java.security.PublicKey;

public class BrokerKeyStore {

    private static final String PRIVATE_KEY_FILE = "broker_private.key";
    private static final String PUBLIC_KEY_FILE = "broker_public.key";

    private static final PrivateKey privateKey = KeyIO.loadPrivateKey(PRIVATE_KEY_FILE);
    private static final PublicKey publicKey = KeyIO.loadPublicKey(PUBLIC_KEY_FILE);

    public static PrivateKey getPrivateKey() {
        return privateKey;
    }

    public static PublicKey getPublicKey() {
        return publicKey;
    }
}