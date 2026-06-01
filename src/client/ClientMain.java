package client;

import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) throws Exception {

        Client client = new Client();
        
        Scanner scanner = new Scanner(System.in);

        //conecta ao broker local 
        client.connect("localhost", 5000);
        //inicia a thread para receber mensagem
        client.startListening();

        int opcao;
        
        //loop principal do menu
        do {

            System.out.println("\n---MENU---");
            System.out.println("1 - Criar topico");
            System.out.println("2 - Inscrever em topico");
            System.out.println("3 - Entrar no chat do topico");
            System.out.println("4 - Desinscrever do topico");
            System.out.println("5 - Sair");
            System.out.print("Escolha: ");

            opcao = scanner.nextInt();
            scanner.nextLine();

            
            switch (opcao) {
                //criar topico 
                case 1:
                    System.out.print("Nome do topico: ");
                    String topicoCriar = scanner.nextLine();//le o nome do topico
                    
                    client.createTopic(topicoCriar);//solicita a criação do topico
                    client.subscribe(topicoCriar);//inscreve automaticamente o cliente no topico criado por ele 
                    client.esperarResposta();
                    break;

                //inscrever em topico existente
                case 2:
                    System.out.print("Nome do topico: ");
                    String topicoSub = scanner.nextLine();//le nome do topico 
                    client.subscribe(topicoSub);//se inscreve no topico 
                    client.esperarResposta();
                    break;
                    
                //entrar em topico     
                case 3:
                    //verifica se o cliente está incrito em topicos
                    if (client.getTopicosInscritos().isEmpty()){
                        System.out.println("Voce nao esta inscrito em nenhum topico.");
                        break;
                    }
                    System.out.println("\nSeus topicos: ");
                    
                    int i = 1;
                    //lista os topicos que o cliente está inscrito
                    for(String t : client.getTopicosInscritos()){
                        System.out.println(i + " - " + t);
                        i++;
                    }
                    System.out.print("Escolha: ");
                    int escolha = Integer.parseInt(scanner.nextLine());//le o numero escolhido
                    
                    String topicoSelecionado = client.getTopicosInscritos()
                            .toArray(new String[0])[escolha - 1];//converte para vetor e pega o topico escolhido
                    
                    entrarNoTopico(scanner, client, topicoSelecionado);// entra no chat do topico
                    break;
                    
                case 4: 
                    System.out.print("Nome do topico: ");
                    String topicoUnsub = scanner.nextLine();//le nome do topico 
                    client.unsubscribe(topicoUnsub);//se desinscreve no topico 
                    client.esperarResposta();
                    break;
                //Encerra programa    
                case 5:
                    System.out.println("Saindo...");
                    break;
                //caso digite opção invalida    
                default:
                    System.out.println("Opcao invalida!");
            }

        } while (opcao != 5);

        scanner.close();          
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
