package client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Set;

public class ClientGUI extends JFrame {

    private final Client client;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    // ===== tela login/cadastro =====
    private JTextField campoUsuario;
    private JPasswordField campoSenha;
    private JTextArea areaStatusLogin;
    private JButton btnCadastrar;
    private JButton btnEntrar;

    // ===== tela principal =====
    private JTextArea areaMensagens;
    private JTextField campoTopico;
    private JTextField campoMensagem;
    private JComboBox<String> comboTopicos;
    private JButton btnCriarTopico;
    private JButton btnInscrever;
    private JButton btnDesinscrever;
    private JButton btnEnviar;
    private JButton btnAtualizarTopicos;

    public ClientGUI(Client client) {
        this.client = client;
        this.client.setGUI(this);

        setTitle("Cliente Pub/Sub");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(criarTelaLogin(), "login");
        mainPanel.add(criarTelaPrincipal(), "main");

        add(mainPanel);

        mostrarTelaLogin();
        setVisible(true);
    }

    private JPanel criarTelaLogin() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));

        JLabel titulo = new JLabel("Login / Cadastro", SwingConstants.CENTER);
        titulo.setFont(new Font("Arial", Font.BOLD, 20));

        JPanel formulario = new JPanel(new GridLayout(6, 1, 8, 8));

        campoUsuario = new JTextField();
        campoSenha = new JPasswordField();

        formulario.add(new JLabel("Usuário:"));
        formulario.add(campoUsuario);
        formulario.add(new JLabel("Senha:"));
        formulario.add(campoSenha);

        btnCadastrar = new JButton("Cadastrar");
        btnEntrar = new JButton("Entrar");

        JPanel botoes = new JPanel(new GridLayout(1, 2, 10, 10));
        botoes.add(btnCadastrar);
        botoes.add(btnEntrar);

        formulario.add(botoes);

        areaStatusLogin = new JTextArea(5, 20);
        areaStatusLogin.setEditable(false);
        areaStatusLogin.setLineWrap(true);
        areaStatusLogin.setWrapStyleWord(true);

        painel.add(titulo, BorderLayout.NORTH);
        painel.add(formulario, BorderLayout.CENTER);
        painel.add(new JScrollPane(areaStatusLogin), BorderLayout.SOUTH);

        btnCadastrar.addActionListener(e -> acaoCadastrar());
        btnEntrar.addActionListener(e -> acaoEntrar());

        return painel;
    }

    private JPanel criarTelaPrincipal() {
        JPanel painel = new JPanel(new BorderLayout(10, 10));

        areaMensagens = new JTextArea();
        areaMensagens.setEditable(false);
        areaMensagens.setLineWrap(true);
        areaMensagens.setWrapStyleWord(true);

        JPanel topo = new JPanel(new GridLayout(2, 1, 5, 5));

        JPanel painelTopico = new JPanel(new BorderLayout(5, 5));
        campoTopico = new JTextField();
        painelTopico.add(new JLabel("Tópico:"), BorderLayout.WEST);
        painelTopico.add(campoTopico, BorderLayout.CENTER);

        JPanel painelBotoesTopico = new JPanel(new GridLayout(1, 4, 5, 5));
        btnCriarTopico = new JButton("Criar tópico");
        btnInscrever = new JButton("Inscrever");
        btnDesinscrever = new JButton("Sair do tópico");
        btnAtualizarTopicos = new JButton("Atualizar tópicos");

        painelBotoesTopico.add(btnCriarTopico);
        painelBotoesTopico.add(btnInscrever);
        painelBotoesTopico.add(btnDesinscrever);
        painelBotoesTopico.add(btnAtualizarTopicos);

        topo.add(painelTopico);
        topo.add(painelBotoesTopico);

        JPanel rodape = new JPanel(new BorderLayout(5, 5));

        comboTopicos = new JComboBox<>();
        campoMensagem = new JTextField();
        btnEnviar = new JButton("Enviar");

        JPanel painelSelecaoTopico = new JPanel(new BorderLayout(5, 5));
        painelSelecaoTopico.add(new JLabel("Tópico inscrito:"), BorderLayout.WEST);
        painelSelecaoTopico.add(comboTopicos, BorderLayout.CENTER);

        rodape.add(painelSelecaoTopico, BorderLayout.NORTH);
        rodape.add(campoMensagem, BorderLayout.CENTER);
        rodape.add(btnEnviar, BorderLayout.EAST);

        painel.add(topo, BorderLayout.NORTH);
        painel.add(new JScrollPane(areaMensagens), BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);

        btnCriarTopico.addActionListener(e -> acaoCriarTopico());
        btnInscrever.addActionListener(e -> acaoInscrever());
        btnDesinscrever.addActionListener(e -> acaoDesinscrever());
        btnAtualizarTopicos.addActionListener(e -> atualizarListaTopicos());
        btnEnviar.addActionListener(e -> acaoEnviarMensagem());

        return painel;
    }

    private void acaoCadastrar() {
        String usuario = campoUsuario.getText().trim();
        String senha = new String(campoSenha.getPassword()).trim();

        if (usuario.isEmpty() || senha.isEmpty()) {
            adicionarStatusLogin("Preencha usuário e senha.");
            return;
        }

        try {
            client.register(usuario, senha);
        } catch (Exception ex) {
            adicionarStatusLogin("Erro ao cadastrar: " + ex.getMessage());
        }
    }

    private void acaoEntrar() {
        String usuario = campoUsuario.getText().trim();
        String senha = new String(campoSenha.getPassword()).trim();

        if (usuario.isEmpty() || senha.isEmpty()) {
            adicionarStatusLogin("Preencha usuário e senha.");
            return;
        }

        try {
            client.login(usuario, senha);
        } catch (Exception ex) {
            adicionarStatusLogin("Erro ao logar: " + ex.getMessage());
        }
    }

    private void acaoCriarTopico() {
        String topico = campoTopico.getText().trim();

        if (topico.isEmpty()) {
            adicionarMensagem("[GUI] Informe o nome do tópico.");
            return;
        }

        try {
            client.createTopic(topico);
            client.subscribe(topico);
        } catch (IOException e) {
            adicionarMensagem("[GUI] Erro ao criar tópico: " + e.getMessage());
        }
    }

    private void acaoInscrever() {
        String topico = campoTopico.getText().trim();

        if (topico.isEmpty()) {
            adicionarMensagem("[GUI] Informe o nome do tópico.");
            return;
        }

        try {
            client.subscribe(topico);
        } catch (IOException e) {
            adicionarMensagem("[GUI] Erro ao inscrever: " + e.getMessage());
        }
    }

    private void acaoDesinscrever() {
        String topico = campoTopico.getText().trim();

        if (topico.isEmpty()) {
            Object selecionado = comboTopicos.getSelectedItem();
            if (selecionado != null) {
                topico = selecionado.toString();
            }
        }

        if (topico == null || topico.isEmpty()) {
            adicionarMensagem("[GUI] Informe ou selecione um tópico.");
            return;
        }

        try {
            client.unsubscribe(topico);
        } catch (IOException e) {
            adicionarMensagem("[GUI] Erro ao sair do tópico: " + e.getMessage());
        }
    }

    private void acaoEnviarMensagem() {
        Object selecionado = comboTopicos.getSelectedItem();
        String mensagem = campoMensagem.getText().trim();

        if (selecionado == null) {
            adicionarMensagem("[GUI] Selecione um tópico inscrito.");
            return;
        }

        if (mensagem.isEmpty()) {
            adicionarMensagem("[GUI] Digite uma mensagem.");
            return;
        }

        String topico = selecionado.toString();

        try {
            client.publish(topico, mensagem);
            campoMensagem.setText("");
        } catch (IOException e) {
            adicionarMensagem("[GUI] Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    public void mostrarTelaLogin() {
        SwingUtilities.invokeLater(() -> cardLayout.show(mainPanel, "login"));
    }

    public void mostrarTelaPrincipal() {
        SwingUtilities.invokeLater(() -> {
            cardLayout.show(mainPanel, "main");
            atualizarListaTopicos();
        });
    }

    public void adicionarMensagem(String texto) {
        SwingUtilities.invokeLater(() -> {
            areaMensagens.append(texto + "\n");
            areaMensagens.setCaretPosition(areaMensagens.getDocument().getLength());
        });
    }

    public void adicionarStatusLogin(String texto) {
        SwingUtilities.invokeLater(() -> {
            areaStatusLogin.append(texto + "\n");
            areaStatusLogin.setCaretPosition(areaStatusLogin.getDocument().getLength());
        });
    }

    public void atualizarListaTopicos() {
        SwingUtilities.invokeLater(() -> {
            comboTopicos.removeAllItems();
            Set<String> topicos = client.getTopicosInscritos();

            for (String topico : topicos) {
                comboTopicos.addItem(topico);
            }
        });
    }

    public void onRegisterSuccess(String msg) {
        adicionarStatusLogin("[CADASTRO OK] " + msg);
    }

    public void onRegisterFail(String msg) {
        adicionarStatusLogin("[CADASTRO FAIL] " + msg);
    }

    public void onLoginSuccess(String msg) {
        adicionarStatusLogin("[LOGIN OK] " + msg);
    }

    public void onLoginFail(String msg) {
        adicionarStatusLogin("[LOGIN FAIL] " + msg);
    }

    public void onAuthSuccess(String msg) {
        adicionarStatusLogin("[AUTH OK] " + msg);
        mostrarTelaPrincipal();
    }

    public void onAuthFail(String msg) {
        adicionarStatusLogin("[AUTH FAIL] " + msg);
    }
}