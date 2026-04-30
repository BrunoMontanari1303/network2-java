
package client;

import java.io.*;
import java.net.Socket;

import broker.model.ProtocolMessage;
import broker.model.MessageType;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;

public class Client {

    private Socket socket;
    private MessageReader reader;
    private MessageWriter writer;

    public void connect(String host, int port) throws IOException {

        socket = new Socket(host, port);
        reader = new MessageReader(socket);
        writer = new MessageWriter(socket);

        System.out.println("Conectado ao broker");

    }

    public void subscribe(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.SUBSCRIBE, "client1", topic, null);
        writer.send(msg);
    }

    public void publish(String topic, String content) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.PUBLISH, "client1", topic, content);
        writer.send(msg);

    }

    public void createTopic(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.CREATE_TOPIC, "client1", topic, null);
        writer.send(msg);
    }

    public void startListening() {
        new Thread(() -> {
            try {
                while (true) {
                    ProtocolMessage msg = reader.read();

                    if (msg == null) {
                        break;
                    }

                    switch (msg.getType()) {
                        case DELIVER:
                        System.out.println("[" + msg.getTopic() + "] " + msg.getPayload());
                        break;

                        case SUCCESS:
                            System.out.println("\n" + msg.getPayload());
                            break;

                        case ERROR:
                            System.out.println("\n" + msg.getPayload());
                            break;

                        default:
                            System.out.println("\nMensagem: " + msg.getPayload());

                    }

                }
            } catch (IOException e) {
                System.out.println("Desconectado do broker.");
            }
        }).start();
    }
    
    public void esperarResposta() {
    try {
        Thread.sleep(300); // 300ms já resolve
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
}
