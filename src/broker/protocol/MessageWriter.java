package broker.protocol;

import broker.model.ProtocolMessage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class MessageWriter { //responsavel por enviar texto pela socket

    private final PrintWriter writer;

    public MessageWriter(Socket socket) throws IOException {
        this.writer = new PrintWriter(socket.getOutputStream(), true); //permite enviar texto pela rede
    }

    public void send(ProtocolMessage message) { //responsavel por enviar mensagens 
        writer.println(message.toJson().toString()); //converte java para JSON
    }

}
