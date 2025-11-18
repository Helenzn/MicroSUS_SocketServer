package server;
/*
 * ... (imports)
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author elder
 */
public class Server {

    private ServerSocket serverSocket;
    int porta;

    public Server() {
        this.porta = 5555;
    }

    public void connectionLoop() throws IOException {
        try {
            // 2 - Esperar o um pedido de conexÃ£o;
            while (true) {
                System.out.println("Esperando conexao...");
                Socket socket = this.esperaConexao(); // Método bloqueante
                System.out.println("Conexao recebida, inciando protocolo...");
                // objeto socket representa a conexão com o servidor
                clientHandle(socket);// chama método para tratar mensagem do cliente

                /**
                 * Criação da estrutura multithreading para atender clientes concorrentes
                 * 1. Criar uma classe que implemente Runnable para gerir a comunicação do
                 * servidor com o cliente (ex: ClientHandler)
                 * 2. Passar o socket do cliente para o construtor dessa classe
                 * OPCIONAL PARA TORNAR MULTITHREAD
                 * 3. Criar uma nova Thread, passando uma instância dessa classe
                 * 4. Iniciar a Thread
                 * 5. dentro do run(), implementar a lógica de comunicação (passos 3 e 4 do
                 * protocolo)
                 */

            }

        } catch (Exception e) {
            System.out.println("Erro na main do ServerSocket " + e.getMessage());
            System.exit(0);
        } finally {
            System.out.println("Servidor finalizado.");
        }

    }

    private void clientHandle(Socket socket) {

        BufferedReader input = null;
        PrintWriter output = null;
        try {
            // 3 - Criar streams de entrada e saída (baseados em caracteres)
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // 4 - Tratar a conversação entre cliente e servidor (tratar protocolo);
            // --- INÍCIO DO PROTOCOLO ---
            String msgCliente = input.readLine();
            System.out.println("Mensagem recebida do cliente: " + msgCliente);

            String msgResposta = "Oi";
            output.println(msgResposta);
            System.out.println("Resposta enviada ao cliente: " + msgResposta);
            // --- FIM DO PROTOCOLO ---
        } catch (IOException e) {
            System.out.println("Erro no tratamento da conexão: " + e.getMessage());
        } finally {
            // garante o fechamento dos streams de entrada e saida ao final da conexao
            try {
                output.close();
                input.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Erro no fechamento de conexão: " + e.getMessage());
            }
        }
    }

    /**
     * @param args the command line arguments
     *             1 - Criar o servidor de conexões
     *             2 - Esperar um pedido de conexão;
     *             3 - Criar streams de entrada e saída;
     *             4 - Tratar a conversação entre cliente e servidor (tratar
     *             protocolo);
     *             4.1 - Fechar streams
     *             4.2 - Fechar socket de comunicação
     */
    public static void main(String[] args) {
        try {
            Server server = new Server();
            // 1 - Criar o servidor de conexões
            server.criarServerSocket(server.porta);
            // 2 - inicia o looping de espera de conexoes
            server.connectionLoop();

        } catch (Exception e) {
            System.out.println("ERRO NO MAIN: " + e);
        }
    }

    private ServerSocket criarServerSocket(int porta) {
        try {
            this.serverSocket = new ServerSocket(porta);
        } catch (Exception e) {
            System.out.println("Erro na Criação do server Socket " + e.getMessage());
        }
        return serverSocket;
    }

    private Socket esperaConexao() {
        try {
            return this.serverSocket.accept();
        } catch (IOException ex) {
            System.out.println("Erro ao criar socket do cliente " + ex.getMessage());
            return null;
        }
    }
}
