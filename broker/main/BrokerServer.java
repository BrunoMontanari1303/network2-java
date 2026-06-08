package broker.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BrokerServer {
	private final int port;
    private final TopicRegistry topicRegistry;

    public BrokerServer(int port) {
        this.port = port;
        this.topicRegistry = new TopicRegistry();

        
    System.out.println(
        topicRegistry.signClientCertificate(
            "Stefani",
            "MINHA_CHAVE"
        )
    );
}
    
    public TopicRegistry getTopicRegistry() {
        return topicRegistry;
    }
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Broker iniciado na porta " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, topicRegistry);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }

        } catch (IOException e) {
            System.out.println("Erro ao iniciar broker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BrokerServer server = new BrokerServer(5000);
        server.start();
    }
}
