package broker.security;

import java.security.PublicKey;

public class CAStore {

	//Alterar depois que receber assinado
    private static final String PUBLIC_KEY_FILE = "professor_ca_public.key";

    private static final PublicKey publicKey = KeyIO.loadPublicKey(PUBLIC_KEY_FILE);

    public static PublicKey getPublicKey() {
        return publicKey;
    }
}