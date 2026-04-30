package client;

import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) throws Exception {

        Client client = new Client();
        Scanner scanner = new Scanner(System.in);

        client.connect("localhost", 5000);
        client.startListening();

        int opcao;

        do {

            System.out.println("\n---MENU---");
            System.out.println("1 - Criar topico");
            System.out.println("2 - Increver em topico");
            System.out.println("3 - Publicar mensagem");
            System.out.println("4 - Sair");
            System.out.print("Escolha: ");

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome do topico: ");
                    String topicoCriar = scanner.nextLine();
                    client.createTopic(topicoCriar);
                    client.esperarResposta();
                    break;

                case 2:
                    System.out.print("Nome do topico: ");
                    String topicoSub = scanner.nextLine();
                    client.subscribe(topicoSub);
                    client.esperarResposta();
                    break;
                    
                case 3:
                    System.out.print("Nome do topico: ");
                    String topicoPub = scanner.nextLine();

                    System.out.print("Mensagem: ");
                    String mensagem = scanner.nextLine();

                    client.publish(topicoPub, mensagem);
                    client.esperarResposta();
                    break;

                case 4:
                    System.out.println("Saindo...");
                    break;

                default:
                    System.out.println("Opção invalida!");
            }

        } while (opcao != 4);

        scanner.close();

    }

}
