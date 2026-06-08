package client;

import broker.security.Certificate;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;

public class ClientCertificateStore {

    public static void saveCertificate(String username, Certificate cert) {
        try {
            Files.createDirectories(Path.of("client_certs"));

            JSONObject json = new JSONObject();
            json.put("clientId", cert.getClientId());
            json.put("publicKey", cert.getPublicKey());
            json.put("signature", cert.getSignature());

            Files.writeString(Path.of("client_certs", username + ".json"), json.toString(2));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar certificado", e);
        }
    }

    public static boolean exists(String username) {
        return Files.exists(Path.of("client_certs", username + ".json"));
    }

    public static Certificate loadCertificate(String username) {
        try {
            String content = Files.readString(Path.of("client_certs", username + ".json"));
            JSONObject json = new JSONObject(content);

            return new Certificate(
                    json.getString("clientId"),
                    json.getString("publicKey"),
                    json.getString("signature")
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar certificado", e);
        }
    }
}