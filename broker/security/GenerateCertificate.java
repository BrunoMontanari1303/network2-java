package broker.security;

import broker.main.TopicRegistry;

public class GenerateCertificate {

    public static void main(String[] args) {

        TopicRegistry registry =
                new TopicRegistry();

        String clientId = "Stefani";

        String publicKey =
                "COLE_A_CHAVE_PUBLICA_AQUI";

        String signature =
                registry.signCertificate(
                        clientId,
                        publicKey
                );

        System.out.println("\nASSINATURA:\n");
        System.out.println(signature);
    }
}