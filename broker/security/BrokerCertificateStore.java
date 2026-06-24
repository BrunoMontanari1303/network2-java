package broker.security;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;

public class BrokerCertificateStore {

    private static final String CERT_FILE = "broker_cert.json";

    public static Certificate loadCertificate() {
        try {
            String content = Files.readString(Path.of(CERT_FILE));
            JSONObject json = new JSONObject(content);

            return new Certificate(
                    json.getString("id"),
                    json.getString("publicKey"),
                    json.getString("signature")
            );
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar certificado do broker", e);
        }
    }
}