package client;

public class ClientMain {

    public static void main(String[] args) throws Exception {
        Client client = new Client("");
        client.connect("localhost", 5000);
        client.startListening();
        client.establishSession();

        new ClientGUI(client);
    }
}