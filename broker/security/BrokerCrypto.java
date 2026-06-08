package broker.security;

import java.security.*;
import broker.security.BrokerCrypto;
import java.security.PublicKey;

public class BrokerCrypto {

    private KeyPair keyPair;

    public BrokerCrypto() {

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            this.keyPair = generator.generateKeyPair();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
            
    }
    
    public PrivateKey getPrivateKey() {
        return keyPair.getPrivate();
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }
}
    

