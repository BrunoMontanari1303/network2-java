package src.broker.protocol;

import src.broker.model.ProtocolMessage;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageWriter {
    private final PrintWriter writer;

    public MessageWriter(Socket socket) throws IOException {
        this.writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(ProtocolMessage message) {
        writer.println(message.toJson().toString());
    }
}