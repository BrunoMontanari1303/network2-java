package broker.security;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class BrokerCertificateStore {

    private static final String CERT_FILE = "bruno.crt";

    public static X509Certificate getCertificate() {
        try (FileInputStream fis = new FileInputStream(CERT_FILE)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(fis);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar certificado do broker", e);
        }
    }

    public static String getCertificatePem() {
        try {
            return Files.readString(Path.of(CERT_FILE));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler arquivo PEM do broker", e);
        }
    }
}