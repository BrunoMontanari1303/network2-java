package client;

import java.io.*;
import java.net.Socket;

import broker.model.ProtocolMessage;
import broker.model.MessageType;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;
import java.util.HashSet;
import java.util.Set;

public class Client {

    private Socket socket;
    private MessageReader reader;
    private MessageWriter writer;
    private final Set<String> topicosInscritos = new HashSet<>(); //armazena os topico que o cliente está inscrito

    //conecta o cliente ao broker
    public void connect(String host, int port) throws IOException {

        socket = new Socket(host, port);
        reader = new MessageReader(socket);
        writer = new MessageWriter(socket);

        System.out.println("Conectado ao broker");

    }

    //retorna os topico que o cliente está inscrito
    public Set<String> getTopicosInscritos() {
        return topicosInscritos;
    }

    //metodo para se inscrever em um topico
    public void subscribe(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.SUBSCRIBE, "client1", topic, null);
        writer.send(msg);
    }

    //metodo para publicar uma mensagem em um topico 
    public void publish(String topic, String content) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.PUBLISH, "client1", topic, content);
        writer.send(msg);

    }

    //metodo para criar um novo topico 
    public void createTopic(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.CREATE_TOPIC, "client1", topic, null);
        writer.send(msg);
    }

    //metodo para remover cliente de um topico
    public void unsubscribe(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.UNSUBSCRIBE, "client1", topic, null);
        writer.send(msg);
    }

    //metodo responsavel por ficar ouvido mensagens do broker 
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
                        	System.out.println("[" + msg.getTopic() + "] " + msg.getClientId() + ": " + msg.getPayload());
                            break;

                        case SUCCESS:
                        	System.out.println("[BROKER] " + msg.getPayload());

                            if ("Inscrição realizada com sucesso.".equals(msg.getPayload()) && msg.getTopic() != null) {
                                topicosInscritos.add(msg.getTopic());
                            }                          
                            if ("Inscrição removida com sucesso.".equals(msg.getPayload()) && msg.getTopic() != null) {
                                topicosInscritos.remove(msg.getTopic());
                            }
                            
                            break;

                        case ERROR:
                        	System.out.println("[ERRO] " + msg.getPayload());
                            break;
                            
                        case DOWNLOAD_OK:
                            System.out.println("[BROKER] " + msg.getPayload());
                            break;

                        default:
                        	System.out.println("[INFO] Mensagem recebida: " + msg.toJson().toString());
                        	break;
                    }

                }
                //verifica erro de conexão
            } catch (IOException e) {
                System.out.println("Desconectado do broker.");
            }
        }).start();
    }
    
    //envia solicitacao para receber mensagens pendentes
    public void requestPendingMessages() {
        ProtocolMessage msg = new ProtocolMessage(
                MessageType.DOWNLOAD_PENDING,
                "client1",
                null,
                null
        );
        writer.send(msg);
    }

    //pausa de 300 milisengudnos pro broker responder
    public void esperarResposta() {
        try {
            Thread.sleep(300);

            //interrompe a thread caso acontecça erro 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
