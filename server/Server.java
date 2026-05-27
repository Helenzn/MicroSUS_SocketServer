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
    private ESTADO estado;
    private Boolean jogada1EhPar;
    private Integer valorJogada1, idJogada;

    public Server() {
        this.porta = 5555;
        estado = ESTADO.JOGADA1;
        idJogada = 0;
    }

    public void connectionLoop() throws IOException {
        try {
            // 2 - Conection Loop: Esperar o um pedido de conexão;
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

    private void clientHandle(Socket socket) throws Exception {

        BufferedReader input = null;
        PrintWriter output = null;
        try {

            /*
             * Clinte -> inputStream.write <- InputStream.read()
             */
            // 3 - Criar streams de entrada e saída (baseados em caracteres)
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            // 4 - Tratar a conversação entre cliente e servidor (tratar protocolo);
            // --- INÍCIO DO PROTOCOLO DE APLICAÇÃO ---
            String msgCliente = input.readLine();
            System.out.println("Mensagem recebida do cliente: " + msgCliente);

            /**
             * Lógica do par/impar.
             * ESTADO=JOGADA1 || JOGADA2
             * 
             * Mensagens do protocolo:
             * JOGADA <par/impar> <nro>
             * JOGADARESPONSE <STATUS> <ganhou/perdeu/1aJogada> |resutado| <idJogo>
             * EXIT
             */

            String[] protocolo = msgCliente.split(" ");
            String resposta = "";

            switch (estado) {
                case JOGADA1:
                    /*
                     * se for a primeira jogada
                     * pegar opcao (par/impar) e nro
                     * armazenar opcao do jogador 1
                     * armazenar nro do jogador 1
                     * enviar msg de espera pela jogada do jogador 2
                     * transiconar estado para JOGADA2
                     */
                    switch (protocolo[0]) {
                        case "JOGADA":
                            resposta = "JOGADARESPONSE ";
                            // processar jogada
                            if (protocolo[1].equals("par")) {
                                jogada1EhPar = true;
                            } else if (protocolo[1].equals("impar")) {
                                jogada1EhPar = false;
                            } else {
                                // opcao errada
                                throw new Exception("Mensagem Invalida");

                            }
                            valorJogada1 = Integer.parseInt(protocolo[2]);
                            resposta += "OK 1aJogada " + idJogada;

                            break;
                        default:
                            // responder com erro
                            resposta += "ERRO";
                            break;
                    }

                    break;
                case JOGADA2:
                    /**
                     * se for segunda jogada
                     * verifica opcao do jogador 1
                     * pega a jogada do jogador 2
                     * compara as jogadas e determina o vencedor
                     * envia resultado;
                     * transiconar estado para JOGADA1
                     * incrementar id
                     */
                    switch (protocolo[0]) {
                        case "JOGADA":
                            resposta = "JOGADARESPONSE ";
                            // processar jogada
                            
                            Boolean Jogada2EhPar = null;

                            if (protocolo[1].equals("par")) {
                                Jogada2EhPar = true;
                            } else if (protocolo[1].equals("impar")) {
                                Jogada2EhPar = false;
                            } else {
                                // opcao errada
                                throw new Exception("Mensagem Invalida");

                            }
                            Integer valorJogada2 = Integer.parseInt(protocolo[2]);
                            
                            
                            break;
                        default:
                            // responder com erro
                            resposta += "ERRO";
                            break;
                    }
                    break;

                default:
                    // se estado indefindo
            }

            /*
             * String msgResposta = "Oi - Recebi sua mensagem: " + msgCliente;
             * output.println(msgResposta);
             * System.out.println("Resposta enviada ao cliente: " + msgResposta);
             */
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
            System.exit(0);
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
