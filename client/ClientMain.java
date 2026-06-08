package client;

import java.util.Scanner;
import broker.security.Certificate;

public class ClientMain {

    public static void main(String[] args) throws Exception {

        Scanner scanner = new Scanner(System.in);

        System.out.print("Informe seu ID: ");
        String clientId = scanner.nextLine();

        Client client = new Client(clientId);

        ClientGUI gui = new ClientGUI(client);

        client.setGUI(gui);

        System.out.println(
        "\nCHAVE PUBLICA:\n"
        + client.getPublicKeyString()
);

        

        //conecta ao broker local 
        client.connect("localhost", 5000);

        Certificate cert = client.createCertificate(); client.authenticate(cert);



        //inicia a thread para receber mensagem
        client.startListening();
        client.requestPendingMessages();

        while (true) {
            Thread.sleep(1000);
        }

    }

    

      
    //metodo pro chat dentro dos topicos 
    public static void entrarNoTopico(Scanner scanner, Client client, String topico) throws Exception {

        System.out.println("\n--- Topico: " + topico + " ---");
        System.out.println("Digite sua mensagem (/sair para voltar)");

        while (true) {
            System.out.print("> ");
            //le mensagem digitada
            String msg = scanner.nextLine();
            //se digitar /sair volta ao menu
            if (msg.equalsIgnoreCase("/sair")) {
                break;
            }
            //publica mensagem no topico 
            client.publish(topico, msg);
        }
    }

}
