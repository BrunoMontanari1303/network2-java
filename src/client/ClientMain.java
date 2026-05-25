package src.client;

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
            System.out.println("2 - Inscrever em topico");
            System.out.println("3 - Entrar em topico");
            System.out.println("4 - Sair");
            System.out.print("Escolha: ");

            opcao = scanner.nextInt();
            scanner.nextLine();

            switch (opcao) {
                case 1:
                    System.out.print("Nome do topico: ");
                    String topicoCriar = scanner.nextLine();
                    
                    client.createTopic(topicoCriar);
                    client.subscribe(topicoCriar);
                    client.esperarResposta();
                    break;

                case 2:
                    System.out.print("Nome do topico: ");
                    String topicoSub = scanner.nextLine();
                    client.subscribe(topicoSub);
                    client.esperarResposta();
                    break;
                    
                case 3:
                    if (client.getTopicosInscritos().isEmpty()){
                        System.out.println("Voce não está inscrito em nenhum topico.");
                        break;
                    }
                    System.out.println("\nSeus topicos: ");
                    
                    int i = 1;
                    for(String t : client.getTopicosInscritos()){
                        System.out.println(i + " - " + t);
                        i++;
                    }
                    System.out.print("Escolha: ");
                    int escolha = Integer.parseInt(scanner.nextLine());
                    
                    String topicoSelecionado = client.getTopicosInscritos()
                            .toArray(new String[0])[escolha - 1];
                    
                    entrarNoTopico(scanner, client, topicoSelecionado);
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
    
    public static void entrarNoTopico(Scanner scanner, Client client, String topico) throws Exception {

    System.out.println("\n--- Tópico: " + topico + " ---");
    System.out.println("Digite sua mensagem (/sair para voltar)");

    while (true) {
        System.out.print("> ");
        String msg = scanner.nextLine();

        if (msg.equalsIgnoreCase("/sair")) {
            break;
        }

        client.publish(topico, msg);
    }
}

}
