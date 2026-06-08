package client;

import java.io.*;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import broker.security.Certificate;

import broker.model.ProtocolMessage;
import broker.model.MessageType;
import broker.protocol.MessageReader;
import broker.protocol.MessageWriter;

import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class Client {

    private Socket socket;
    private MessageReader reader;
    private MessageWriter writer;
    private String clientId;
    private KeyPair keyPair;
    private ClientGUI gui;
    private final Set<String> topicosInscritos = new HashSet<>(); //armazena os topico que o cliente está inscrito


    //conecta o cliente ao broker
    public void connect(String host, int port) throws IOException {

        socket = new Socket(host, port);
        reader = new MessageReader(socket);
        writer = new MessageWriter(socket);

        System.out.println("Conectado ao broker");

    }

    public void setGUI(ClientGUI gui) {
        this.gui = gui;
    }

    public Client(String clientId) {
        this.clientId = clientId;
        generateKeys();
    }

    //retorna os topico que o cliente está inscrito
    public Set<String> getTopicosInscritos() {
        return topicosInscritos;
    }

    public PublicKey getPublicKey() {
        return keyPair.getPublic();

    }

    //metodo para se inscrever em um topico
    public void subscribe(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.SUBSCRIBE, clientId, topic, null);
        writer.send(msg);
    }

    //metodo para publicar uma mensagem em um topico 
    public void publish(String topic, String content) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.PUBLISH, clientId, topic, content);
        writer.send(msg);

    }

    //metodo para criar um novo topico 
    public void createTopic(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.CREATE_TOPIC, clientId, topic, null);
        writer.send(msg);
    }

    //metodo para remover cliente de um topico
    public void unsubscribe(String topic) throws IOException {
        ProtocolMessage msg = new ProtocolMessage(MessageType.UNSUBSCRIBE, clientId, topic, null);
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
                        	String texto =
        "[" + msg.getTopic() + "] "
        + msg.getClientId()
        + ": "
        + msg.getPayload();

    System.out.println(texto);

    if(gui != null){
        gui.adicionarMensagem(texto);
    }

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

                        case AUTH_OK:
                            System.out.println("Autenticado com sucesso!");
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
                clientId,
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

    private void generateKeys() {
    try {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        this.keyPair = generator.generateKeyPair();
        } catch (Exception e) {
                throw new RuntimeException(e);
            }
    }   

    public void authenticate(Certificate cert) {

        ProtocolMessage msg = new ProtocolMessage( MessageType.AUTH_REQUEST,clientId,null,null);

        msg.setCertificate(cert);

        writer.send(msg);
    }

    public String getPublicKeyString() {

    return Base64.getEncoder()
            .encodeToString(
                    keyPair.getPublic().getEncoded()
            );
    }

    public Certificate createCertificate() {

    String publicKey =
            getPublicKeyString();

    return new Certificate(
            clientId,
            publicKey,
            "ASSINADO_PELO_BROKER"
    );
}
}
