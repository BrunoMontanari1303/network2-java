package broker.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BrokerServer {

    private final int port;
    private final TopicRegistry topicRegistry;
    private final UserRegistry userRegistry;
    private final TopicKeyBot topicKeyBot;

    public BrokerServer(int port) {
        this.port = port;
        this.topicRegistry = new TopicRegistry();
        this.userRegistry = new UserRegistry();
        this.topicKeyBot = new TopicKeyBot();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Broker iniciado na porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(
                        clientSocket,
                        topicRegistry,
                        userRegistry,
                        topicKeyBot
                );

                new Thread(clientHandler).start();
            }

        } catch (IOException e) {
            System.out.println("Erro ao iniciar broker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
    	String brokerPublicKeyBase64 = java.util.Base64.getEncoder().encodeToString(
    	        broker.security.BrokerKeyStore.getPublicKey().getEncoded()
    	);

    	System.out.println("=== BROKER KEY NO SERVIDOR ===");
    	System.out.println("Broker public key (inicio): " +
    	        brokerPublicKeyBase64.substring(0, Math.min(60, brokerPublicKeyBase64.length())));
        new BrokerServer(5000).start();
    }
}