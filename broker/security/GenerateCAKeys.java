package broker.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class GenerateCAKeys {

    public static void main(String[] args) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            KeyIO.savePrivateKey(keyPair.getPrivate(), "ca_private.key");
            KeyIO.savePublicKey(keyPair.getPublic(), "ca_public.key");

            System.out.println("Chaves da AC geradas com sucesso.");
            System.out.println("Arquivo privado: ca_private.key");
            System.out.println("Arquivo publico: ca_public.key");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}