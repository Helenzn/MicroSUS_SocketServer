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
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;
import model.Paciente;
import model.PacienteRequest;
import service.PacienteService;
import java.util.List;
/**
 *
 * @author elder
 */
public class ServerV2 {

    private ServerSocket serverSocket;
    int porta;
    Map<String, String> arquivos;

    private PacienteService pacienteService;

    public ServerV2() {
    this.porta = 80;

    arquivos = new HashMap<>();
    arquivos.put("nome", "conteudo");

    this.pacienteService = new PacienteService();
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
            // Leitura da requisição HTTP do cliente
            HttpParser parser = new HttpParser();
            String linha;
            Boolean primeiraLinha = true;
            Integer contentLength = 0;
            
            do{
                linha = input.readLine();
                System.out.println("Recebido: " + linha);
                //verificação se o protocolo é HTTP
                if(primeiraLinha){
                    String[] partes = linha.split(" ");
                    parser.setMethod(partes[0]);
                    parser.setPath(partes[1]);
                    parser.setHttpVersion(partes[2]);
                    primeiraLinha = false;
                }else{
                    //faz o parse dos headers
                    String[] headerParts = linha.split(":");
                    if(headerParts.length == 2){
                        parser.setHeader(headerParts[0].trim(), headerParts[1].trim());
                    }
                    //verifica se existe Content-Length para ler o body depois      
                    if(headerParts[0].equalsIgnoreCase("Content-Length")){    
                        contentLength = Integer.parseInt(headerParts[1].trim());
                    }
                }
            }while( linha != null && !linha.isEmpty() ); //lê todo o cabeçalho da requisição

            if(contentLength > 0){
                CharBuffer bodyBuffer = CharBuffer.allocate(contentLength);
                input.read(bodyBuffer);
                bodyBuffer.flip();
                parser.setBody(bodyBuffer.toString());
    
            }
            /*
                Interpretação do protocolo
                Ação que a req está solicitando
                Validação
                Execução da logica de negócio
                escrita da resposta

            */
            System.out.println("Requisição completa recebida:\n" + parser.toString());

          // System.out.println("Metodo recebido: " + parser.getMethod());
          //  System.out.println("Caminho recebido: " + parser.getPath());
System.out.println("METHOD = [" + parser.getMethod() + "]");
System.out.println("PATH = [" + parser.getPath() + "]");

            String httpResponse =
                    "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 9\r\n" +
                    "\r\n" +
                    "Not Found";

            if(parser.getMethod().equals("GET")
            && parser.getPath().equals("/fila")){


                List<Paciente> fila = pacienteService.listarFila();


                String html =
                        "<html>" +
                        "<head>" +
                        "<title>Fila de Atendimento</title>" +
                        "</head>" +
                        "<body>" +

                        "<h1>Fila de Atendimento</h1>" +


                        "<table border='1'>" +
                        "<tr>" +
                        "<th>ID</th>" +
                        "<th>Nome</th>" +
                        "<th>Sintoma</th>" +
                        "<th>Prioridade</th>" +
                        "<th>Estado</th>" +
                        "</tr>";


                for(Paciente p : fila){

                    html +=
                            "<tr>" +
                            "<td>" + p.getId() + "</td>" +
                            "<td>" + p.getNome() + "</td>" +
                            "<td>" + p.getSintoma() + "</td>" +
                            "<td>" + p.getPrioridade() + "</td>" +
                            "<td>" + p.getEstado() + "</td>" +
                            "</tr>";
                }


                html +=
                        "</table>" +
                        "</body>" +
                        "</html>";


                httpResponse =
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Content-Length: " + html.getBytes().length + "\r\n" +
                        "\r\n" +
                        html;
            }
            else if(parser.getMethod().equals("POST")
                    && parser.getPath().equals("/pacientes")) {

                Gson gson = new Gson();

                PacienteRequest request =
                        gson.fromJson(parser.getBody(), PacienteRequest.class);

                Paciente paciente = pacienteService.cadastrar(
                        request.getNome(),
                        request.getSintoma(),
                        request.getPrioridade()
                );

                String json =
                        "{"
                        + "\"id\":" + paciente.getId() + ","
                        + "\"nome\":\"" + paciente.getNome() + "\","
                        + "\"sintoma\":\"" + paciente.getSintoma() + "\","
                        + "\"prioridade\":\"" + paciente.getPrioridade() + "\","
                        + "\"estado\":\"" + paciente.getEstado() + "\","
                        + "\"horaChegada\":\"" + paciente.getHoraChegada() + "\""
                        + "}";

                httpResponse =
                        "HTTP/1.1 201 Created\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + json.length() + "\r\n" +
                        "\r\n" +
                        json;
            }
            else if(parser.getMethod().equals("GET")
                    && parser.getPath().equals("/pacientes")) {

                Gson gson = new Gson();

                List<Paciente> pacientes = pacienteService.listarTodos();

                String json = gson.toJson(pacientes);

                httpResponse =
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + json.length() + "\r\n" +
                        "\r\n" +
                        json;
            }
            else if(parser.getMethod().equals("GET")
                    && parser.getPath().startsWith("/pacientes/")) {

                String idString = parser.getPath()
                        .replace("/pacientes/", "");

                int id = Integer.parseInt(idString);

                Paciente paciente = pacienteService.buscarPorId(id);

                Gson gson = new Gson();

                String json;

                if(paciente == null){

                    json = "{ \"erro\":\"Paciente com esse id não existe\" }";

                } else {

                    json = gson.toJson(paciente);

                }

                httpResponse =
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + json.getBytes().length + "\r\n" +
                        "\r\n" +
                        json;
            }
            else if(parser.getMethod().equals("PUT")
                    && parser.getPath().startsWith("/pacientes/")) {


                String idString = parser.getPath()
                        .replace("/pacientes/", "");

                int id = Integer.parseInt(idString);


                Gson gson = new Gson();

                PacienteRequest request =
                        gson.fromJson(parser.getBody(), PacienteRequest.class);


                Paciente paciente = pacienteService.atualizar(
                        id,
                        request.getNome(),
                        request.getSintoma(),
                        request.getPrioridade()
                );


                String json = gson.toJson(paciente);


                httpResponse =
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + json.length() + "\r\n" +
                        "\r\n" +
                        json;
            }
            else if(parser.getMethod().equals("POST")
                    && parser.getPath().equals("/chamar")) {


                Gson gson = new Gson();

                Paciente paciente = pacienteService.chamarProximo();


                if(paciente == null){

                    String json =
                            "{ \"erro\":\"Nenhum paciente EM_FILA\" }";


                    httpResponse =
                            "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: " + json.getBytes().length +
                            "\r\n\r\n" +
                            json;


                } else {


                    String json = gson.toJson(paciente);


                    httpResponse =
                            "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: " + json.getBytes().length +
                            "\r\n\r\n" +
                            json;
                }
            }
            else if(parser.getMethod().equals("POST")
                    && parser.getPath().startsWith("/pacientes/")
                    && parser.getPath().endsWith("/finalizar")) {


                String idString =
                        parser.getPath()
                        .replace("/pacientes/", "")
                        .replace("/finalizar", "");

                int id = Integer.parseInt(idString);
                Paciente paciente = pacienteService.buscarPorId(id);
                Gson gson = new Gson();

                // ID não existe
                if(paciente == null){

                    String json =
                            "{ \"erro\":\"Paciente não existe\" }";

                    httpResponse =
                            "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: " + json.getBytes().length +
                            "\r\n\r\n" +
                            json;
                }

                else {

                    String erro =
                            pacienteService.validarFinalizacao(paciente);

                    // Estado inválido
                    if(erro != null){

                        String json =
                                "{ \"erro\":\"" + erro + "\" }";

                        httpResponse =
                                "HTTP/1.1 409 Conflict\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Content-Length: " + json.getBytes().length +
                                "\r\n\r\n" +
                                json;
                    }

                    // Pode finalizar
                    else {

                        Map<String,String> body =
                                gson.fromJson(
                                        parser.getBody(),
                                        Map.class
                                );

                        String prognostico =
                                body.get("prognostico");

                        paciente.setPrognostico(prognostico);
                        paciente.setEstado("ATENDIDO");

                        String json =
                                gson.toJson(paciente);

                        httpResponse =
                                "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: application/json\r\n" +
                                "Content-Length: " + json.getBytes().length +
                                "\r\n\r\n" +
                                json;
                    }
                }
            }
            else if(parser.getMethod().equals("GET")
                    && parser.getPath().equals("/estatisticas")) {


                Gson gson = new Gson();


                String json =
                        gson.toJson(
                            pacienteService.estatisticas()
                        );


                httpResponse =
                        "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json\r\n" +
                        "Content-Length: " + json.getBytes().length +
                        "\r\n\r\n" +
                        json;
            }
           else if(parser.getMethod().equals("DELETE")
                    && parser.getPath().startsWith("/pacientes/")) {


                String idString = parser.getPath()
                        .replace("/pacientes/", "");

                int id = Integer.parseInt(idString);


                boolean removido = pacienteService.remover(id);


                if(!removido){

                    String json =
                            "{ \"erro\":\"Paciente não encontrado\" }";


                    httpResponse =
                            "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: " + json.length() + "\r\n" +
                            "\r\n" +
                            json;

                } else {

                    String json =
                            "{ \"mensagem\":\"Paciente removido com sucesso\" }";


                    httpResponse =
                            "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: application/json\r\n" +
                            "Content-Length: " + json.length() + "\r\n" +
                            "\r\n" +
                            json;
                }
            }

            System.out.println("Enviando resposta:\n" + httpResponse);
            //Dica: para calcular o Content-Length, um caracter equivale a 1 byte em texto simples
            // Envio da resposta HTTP para o cliente
            output.write(httpResponse);
            output.flush();
            // --- FIM DO PROTOCOLO ---
        } catch (IOException e) {
            System.out.println("Erro na conexão: " + e.getMessage());
        }catch(Exception e){
            System.out.println("Erro inesperado: " + e.getMessage());
            //Enviar resposta de erro ao cliente
            String httpResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                                  "Content-Type: text/html\r\n" +
                                  "Content-Length: 25\r\n" +
                                  "\r\n" +
                                  "<h1>Erro no servidor!</h1>";
            output.write(httpResponse);
            output.write("\r\n");
            output.flush();
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
            ServerV2 server = new ServerV2();
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
