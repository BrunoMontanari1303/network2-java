package client;

public class BotMain {

    private static final String BOT_ID = "bot";
    private static final String BOT_PASSWORD = "senhaDoBot";

    public static void main(String[] args) throws Exception {
        Client bot = new Client(BOT_ID);

        bot.connect("localhost", 5000);
        bot.startListening();

        while (!bot.isSessionReady()) {
            Thread.sleep(100);
        }

        // tenta cadastrar primeiro
        bot.resetAuthFlowState();
        bot.register(BOT_ID, BOT_PASSWORD);

        // espera resposta do cadastro
        waitForRegisterResponse(bot, 5000);

        if (bot.isRegisterSuccess()) {
            System.out.println("Bot cadastrado com sucesso.");
        } else if (bot.isRegisterFailed()) {
            System.out.println("Cadastro do bot retornou falha: " + bot.getLastRegisterMessage());
            System.out.println("Seguindo para login mesmo assim...");
        }

        // agora tenta login
        bot.resetAuthFlowState();
        bot.login(BOT_ID, BOT_PASSWORD);

        waitForLoginResponse(bot, 5000);

        if (!bot.isLoginSuccess()) {
            throw new RuntimeException("Falha no login do bot: " + bot.getLastLoginMessage());
        }

        waitForAuthentication(bot, 5000);

        if (!bot.isAuthenticated()) {
            throw new RuntimeException("Falha na autenticacao do bot.");
        }

        System.out.println("Bot conectado, cadastrado/logado e autenticado com sucesso.");

        // mantém o processo vivo
        while (true) {
            Thread.sleep(1000);
        }
    }

    private static void waitForRegisterResponse(Client bot, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();

        while (!bot.isRegisterSuccess() && !bot.isRegisterFailed()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new RuntimeException("Timeout esperando resposta do cadastro do bot.");
            }
            Thread.sleep(100);
        }
    }

    private static void waitForLoginResponse(Client bot, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();

        while (!bot.isLoginSuccess() && !bot.isLoginFailed()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new RuntimeException("Timeout esperando resposta do login do bot.");
            }
            Thread.sleep(100);
        }
    }

    private static void waitForAuthentication(Client bot, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();

        while (!bot.isAuthenticated()) {
            if (System.currentTimeMillis() - start > timeoutMs) {
                throw new RuntimeException("Timeout esperando autenticacao do bot.");
            }
            Thread.sleep(100);
        }
    }
}