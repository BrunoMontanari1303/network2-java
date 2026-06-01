package broker;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BrokerServer {

    private final int port; //porta que ele fica ouvindo conexões
    private final TopicRegistry topicRegistry; //registro responsavel por armazenar topicos e inscritos

    public BrokerServer(int port) {
        this.port = port;
        this.topicRegistry = new TopicRegistry();
    }
    //metodo responsavel por iniciar o broker 
    public void start() {
        //cria um ServerSocket na porta definida 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Broker iniciado na porta " + port);

            while (true) { //loop esperando os conectarem
                Socket clientSocket = serverSocket.accept(); //aceita a conexão de um cliente 
                System.out.println("Novo cliente conectado: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, topicRegistry);//cria um manipoulador para o cliente conectado
                Thread thread = new Thread(clientHandler);//cria uma thread para atender a este cliente 
                thread.start();
            }
          //caso ocorra erro ao iniciar o broker 
        } catch (IOException e) {
            System.out.println("Erro ao iniciar broker: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        BrokerServer server = new BrokerServer(5000);//cria o broker na porta 5000
        server.start();//inicia o broker 
    }
}