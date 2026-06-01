package broker.protocol;

import broker.model.ProtocolMessage;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class MessageReader {
    private final BufferedReader reader; // vai ler texto vindo do socket convertendo JSON em objeto Java 

    public MessageReader(Socket socket) throws IOException {
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); //Pega fluxo de entrada do socket
    }

    public ProtocolMessage read() throws IOException {//responsavel por ler e tranformar em java 
        String line = reader.readLine();

        if (line == null) {
            return null;
        }

        JSONObject json = new JSONObject(line); //transforma texto Json em objeto manipulavel 
        return ProtocolMessage.fromJson(json);//converte JSON para o ProtocolMessage
    }
}