package client;

import broker.security.BrokerKeyStore;
import broker.security.Certificate;
import broker.security.CryptoUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

public class OfflineUserSetup {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java client.OfflineUserSetup <username>");
            return;
        }

        String username = args[0];

        try {
            if (!ClientKeyStore.keyPairExists(username)) {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
                generator.initialize(2048);
                KeyPair keyPair = generator.generateKeyPair();
                ClientKeyStore.saveKeyPair(username, keyPair);
                System.out.println("Par de chaves gerado para " + username);
            } else {
                System.out.println("Par de chaves ja existe para " + username);
            }

            KeyPair keyPair = ClientKeyStore.loadKeyPair(username);
            String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

            String signature = CryptoUtils.sign(username + publicKey, BrokerKeyStore.getPrivateKey());

            Certificate cert = new Certificate(username, publicKey, signature);
            ClientCertificateStore.saveCertificate(username, cert);

            System.out.println("Certificado offline gerado para " + username);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}