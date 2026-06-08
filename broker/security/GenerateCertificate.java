package broker.security;

public class GenerateCertificate {

    public static String generateSignature(String clientId, String publicKey) {
        return CertificateAuthority.getInstance().signCertificate(clientId, publicKey);
    }
}