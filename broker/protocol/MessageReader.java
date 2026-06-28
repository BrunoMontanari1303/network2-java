package broker.protocol;

import broker.model.ProtocolMessage;
import broker.security.CryptoUtils;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Base64;

public class MessageReader {

    private final BufferedReader reader;
    private SecretKey sessionKey;

    public MessageReader(Socket socket) {
        try {
            InputStream input = socket.getInputStream();
            this.reader = new BufferedReader(new InputStreamReader(input));
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar MessageReader", e);
        }
    }

    public void setSessionKey(SecretKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    public ProtocolMessage read() {
        try {
            String line = reader.readLine();

            if (line == null) {
                return null;
            }

            JSONObject json = new JSONObject(line);

            if (json.optBoolean("secure", false)) {
                if (sessionKey == null) {
                    throw new RuntimeException("Mensagem segura recebida sem chave de sessão.");
                }

                byte[] iv = Base64.getDecoder().decode(json.getString("iv"));
                String encryptedData = json.getString("data");
                String plaintext = CryptoUtils.decryptAES(encryptedData, sessionKey, iv);

                json = new JSONObject(plaintext);
            }

            return ProtocolMessage.fromJson(json);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao ler mensagem", e);
        }
    }
}