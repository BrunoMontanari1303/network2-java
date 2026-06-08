package client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class ClientKeyStore {

    public static void saveKeyPair(String username, KeyPair keyPair) throws IOException {
        Files.createDirectories(Path.of("client_keys"));
        Files.write(Path.of("client_keys", username + "_private.key"), keyPair.getPrivate().getEncoded());
        Files.write(Path.of("client_keys", username + "_public.key"), keyPair.getPublic().getEncoded());
    }

    public static boolean keyPairExists(String username) {
        return Files.exists(Path.of("client_keys", username + "_private.key")) &&
               Files.exists(Path.of("client_keys", username + "_public.key"));
    }

    public static KeyPair loadKeyPair(String username) {
        try {
            byte[] privateBytes = Files.readAllBytes(Path.of("client_keys", username + "_private.key"));
            byte[] publicBytes = Files.readAllBytes(Path.of("client_keys", username + "_public.key"));

            KeyFactory factory = KeyFactory.getInstance("RSA");

            PrivateKey privateKey = factory.generatePrivate(new PKCS8EncodedKeySpec(privateBytes));
            PublicKey publicKey = factory.generatePublic(new X509EncodedKeySpec(publicBytes));

            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar chaves do cliente", e);
        }
    }
}