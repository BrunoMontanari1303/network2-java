package broker.protocol;

import broker.model.ProtocolMessage;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageReader {
    private final BufferedReader reader;

    public MessageReader(Socket socket) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public ProtocolMessage read() throws IOException {
        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        JSONObject json = new JSONObject(line);
        return ProtocolMessage.fromJson(json);
    }
}