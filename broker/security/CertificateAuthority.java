package broker.security;

import java.security.*;


public class CertificateAuthority {

    private final PrivateKey brokerPrivateKey;

    public CertificateAuthority(PrivateKey brokerPrivateKey) {
        this.brokerPrivateKey = brokerPrivateKey;
    }
    
    public String sign(String data) {

        try{
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(brokerPrivateKey);
            signature.update(data.getBytes());

            byte[] signed = signature.sign();
            return java.util.Base64.getEncoder().encodeToString(signed);

            } catch (Exception e) {
            throw new RuntimeException(e);
        
        }
    }
    
}
