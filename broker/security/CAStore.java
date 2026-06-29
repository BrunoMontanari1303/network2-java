package broker.security;

import java.io.FileInputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CAStore {

    private static final String CA_CERT_FILE = "ca.crt";

    public static X509Certificate getCertificate() {
        try (FileInputStream fis = new FileInputStream(CA_CERT_FILE)) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(fis);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar certificado da AC", e);
        }
    }

    public static PublicKey getPublicKey() {
        return getCertificate().getPublicKey();
    }
}