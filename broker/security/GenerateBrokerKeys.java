package broker.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class GenerateBrokerKeys {

    public static void main(String[] args) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            KeyIO.savePrivateKey(keyPair.getPrivate(), "broker_private.key");
            KeyIO.savePublicKey(keyPair.getPublic(), "broker_public.key");

            System.out.println("Chaves do broker geradas com sucesso.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}