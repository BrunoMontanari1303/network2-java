package broker.protocol;

import broker.model.ProtocolMessage;
import broker.security.CryptoUtils;
import org.json.JSONObject;

import javax.crypto.SecretKey;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Base64;

public class MessageWriter {

    private final PrintWriter writer;
    private SecretKey sessionKey;

    public MessageWriter(Socket socket) {
        try {
            OutputStream output = socket.getOutputStream();
            this.writer = new PrintWriter(output, true);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar MessageWriter", e);
        }
    }

    public void setSessionKey(SecretKey sessionKey) {
        this.sessionKey = sessionKey;
    }

    public void send(ProtocolMessage message) {
        if (sessionKey == null) {
            writer.println(message.toJson().toString());
            return;
        }

        String plaintext = message.toJson().toString();
        byte[] iv = CryptoUtils.generateIV();
        String encrypted = CryptoUtils.encryptAES(plaintext, sessionKey, iv);

        JSONObject envelope = new JSONObject();
        envelope.put("secure", true);
        envelope.put("iv", Base64.getEncoder().encodeToString(iv));
        envelope.put("data", encrypted);

        writer.println(envelope.toString());
    }
}