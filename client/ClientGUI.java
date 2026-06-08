package client;

import javax.swing.*;
import java.awt.*;

public class ClientGUI extends JFrame {

    private JTextArea areaMensagens;
    private JTextField campoTopico;
    private JTextField campoMensagem;
    private JButton btnEnviar;

    private Client client;

    public ClientGUI(Client client) {

    this.client = client;

    setTitle("Cliente Broker");
    setSize(700, 500);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);

    areaMensagens = new JTextArea();
    areaMensagens.setEditable(false);

    campoTopico = new JTextField();
    campoMensagem = new JTextField();

    btnEnviar = new JButton("Publicar");

    JLabel lblTopico = new JLabel("Nome do tópico:");
    JLabel lblMensagem = new JLabel("Mensagem:");

    JButton btnCriar = new JButton("Criar tópico");
    JButton btnInscrever = new JButton("Inscrever");
    JButton btnDesinscrever = new JButton("Desinscrever");

    JPanel painelCampos = new JPanel(new GridLayout(6,1));

    painelCampos.add(lblTopico);
    painelCampos.add(campoTopico);

    painelCampos.add(lblMensagem);
    painelCampos.add(campoMensagem);

    painelCampos.add(btnEnviar);

    JPanel painelBotoes = new JPanel();

    painelBotoes.add(btnCriar);
    painelBotoes.add(btnInscrever);
    painelBotoes.add(btnDesinscrever);

    add(new JScrollPane(areaMensagens), BorderLayout.CENTER);
    add(painelCampos, BorderLayout.SOUTH);
    add(painelBotoes, BorderLayout.NORTH);

    btnCriar.addActionListener(e -> {
        try {
            
        String topico = campoTopico.getText();

        client.createTopic(topico);

        client.subscribe(topico);

        adicionarMensagem(
            "[SISTEMA] Tópico criado e inscrição realizada: "
            + topico
        );

    } catch (Exception ex) {
        ex.printStackTrace();
    }

    });

    btnInscrever.addActionListener(e -> {
        try {
            client.subscribe(campoTopico.getText());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    });

    btnDesinscrever.addActionListener(e -> {
        try {
            client.unsubscribe(campoTopico.getText());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    });

    btnEnviar.addActionListener(e -> {
        try {

            String topico = campoTopico.getText();
            String mensagem = campoMensagem.getText();

            client.publish(topico, mensagem);

            campoMensagem.setText("");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    });

    setVisible(true);
}
    

    public void adicionarMensagem(String texto) {
        areaMensagens.append(texto + "\n");
    }
}