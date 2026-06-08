package broker.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class KeyIO {

    public static void savePrivateKey(PrivateKey privateKey, String filePath) throws IOException {
        Files.write(Path.of(filePath), privateKey.getEncoded());
    }

    public static void savePublicKey(PublicKey publicKey, String filePath) throws IOException {
        Files.write(Path.of(filePath), publicKey.getEncoded());
    }

    public static PrivateKey loadPrivateKey(String filePath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Path.of(filePath));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar chave privada", e);
        }
    }

    public static PublicKey loadPublicKey(String filePath) {
        try {
            byte[] keyBytes = Files.readAllBytes(Path.of(filePath));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar chave pública", e);
        }
    }
}