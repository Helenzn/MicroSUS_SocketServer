# Parte 2 — Prática: Construindo um Echo Server

**Disciplina:** Sistemas Distribuídos I | **Código:** PF_CC.34
**Professor:** Élder F. F. Bernardi
**Unidade:** II — Comunicação em Sistemas Distribuídos
**Código-base:** [`code/socket-server-basic/`](../)

---

## 1. O que é um Echo Server?

O **Echo Server** é o "Hello World" da programação em rede. Seu funcionamento é trivial: ele recebe uma mensagem do cliente e **devolve exatamente a mesma mensagem** de volta. Apesar da simplicidade, este padrão exercita **todos** os passos fundamentais da comunicação via sockets:

1. Criar o servidor e vincular a uma porta
2. Aguardar e aceitar uma conexão
3. Ler dados do cliente
4. Enviar dados de volta ao cliente
5. Fechar a conexão

Se você conseguir construir um echo server funcional, você domina os fundamentos necessários para implementar **qualquer** protocolo de aplicação sobre TCP.

---

## 2. Análise do Código-Esqueleto

É oferecido um "esqueleto" em Java no diretório `code/socket-server-basic/` para exemplo, que segue exatamente o protocolo de 5 passos documentado nos comentários do `main()`:

```
1 - Criar o servidor de conexões
2 - Esperar um pedido de conexão
3 - Criar streams de entrada e saída
4 - Tratar a conversação entre cliente e servidor (tratar protocolo)
  4.1 - Fechar streams
  4.2 - Fechar socket de comunicação
```

Vamos analisar cada etapa no código real.

---

## 3. Passo a Passo do Servidor

### 3.1 Passo 1 — Criar o ServerSocket

O ponto de partida é criar um `ServerSocket` vinculado a uma porta. No exemplo, isso é feito pelo método `criarServerSocket()`:

```java
private ServerSocket criarServerSocket(int porta) {
    try {
        this.serverSocket = new ServerSocket(porta);
    } catch (Exception e) {
        System.out.println("Erro na Criação do server Socket " + e.getMessage());
        System.exit(0);
    }
    return serverSocket;
}
```

**O que acontece aqui:**

- `new ServerSocket(porta)` executa internamente `socket()` + `bind()` + `listen()`
- O servidor passa a **escutar** na porta especificada (5555 no esqueleto)
- Se a porta já estiver em uso, uma exceção é lançada

> **Dica:** Se você receber `java.net.BindException: Address already in use`, significa que outro processo (ou uma execução anterior do servidor que não foi encerrada) já está usando a porta 5555.

### 3.2 Passo 2 — Loop de Conexões (accept)

O método `connectionLoop()` implementa um loop infinito que espera por conexões:

```java
public void connectionLoop() throws IOException {
    try {
        while (true) {
            System.out.println("Esperando conexao...");
            Socket socket = this.esperaConexao(); // Método bloqueante
            System.out.println("Conexao recebida, inciando protocolo...");
            clientHandle(socket);
        }
    } catch (Exception e) {
        System.out.println("Erro na main do ServerSocket " + e.getMessage());
    }
}
```

**O que acontece aqui:**

- `esperaConexao()` chama `serverSocket.accept()` — um método **bloqueante**
- O servidor **para** nessa linha e espera até um cliente se conectar
- Quando a conexão é estabelecida, `accept()` retorna um **novo** `Socket` representando a conexão com aquele cliente específico
- O servidor passa para `clientHandle()` para tratar a comunicação

```
                         Loop de Conexões
                         ┌──────────┐
                         │  accept() │◄─── BLOQUEIA aqui
                         │    │      │     até um cliente
                         │    ▼      │     conectar
                         │  handle() │
                         │    │      │
                         │    ▼      │
                         │  (volta)  │
                         └──────────┘
```

### 3.3 Passo 3 — Criar Streams de I/O

Dentro de `clientHandle()`, o servidor cria streams de leitura e escrita sobre o socket:

```java
private void clientHandle(Socket socket) {
    BufferedReader input = null;
    PrintWriter output = null;
    try {
        // Streams baseados em caracteres
        input = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(
            socket.getOutputStream(), true);
```

**O que acontece aqui:**

- `socket.getInputStream()` retorna o fluxo de bytes **vindos do cliente**
- `socket.getOutputStream()` retorna o fluxo de bytes **indo para o cliente**
- `InputStreamReader` converte bytes em caracteres (usando o charset padrão)
- `BufferedReader` adiciona a capacidade de ler **linha por linha** (`readLine()`)
- `PrintWriter` facilita a escrita de texto com `println()`
- O parâmetro `true` no `PrintWriter` ativa o **auto-flush**: cada `println()` envia imediatamente

```
                 ┌──────────┐           ┌──────────┐
                 │ SERVIDOR │           │ CLIENTE  │
                 │          │           │          │
  input ◄────── │ InputStream │ ◄────── │ OutputStream │ ◄── output
                 │          │           │          │
  output ─────► │ OutputStream │ ─────► │ InputStream  │ ──► input
                 │          │           │          │
                 └──────────┘           └──────────┘

  O que é INPUT para o servidor é OUTPUT para o cliente (e vice-versa)
```

### 3.4 Passo 4 — O Protocolo Echo

A seção marcada como `INÍCIO DO PROTOCOLO DE APLICAÇÃO` no esqueleto é onde a lógica de negócio reside:

```java
// --- INÍCIO DO PROTOCOLO DE APLICAÇÃO ---
String msgCliente = input.readLine();
System.out.println("Mensagem recebida do cliente: " + msgCliente);

String msgResposta = "Oi - Recebi sua mensagem: " + msgCliente;
output.println(msgResposta);
System.out.println("Resposta enviada ao cliente: " + msgResposta);
// --- FIM DO PROTOCOLO ---
```

**Para transformar em um verdadeiro Echo Server**, basta simplificar:

```java
// --- PROTOCOLO ECHO ---
String msgCliente = input.readLine();
System.out.println("Echo: " + msgCliente);
output.println(msgCliente); // Devolve a mesma mensagem
// --- FIM DO PROTOCOLO ---
```

> **Observação:** `readLine()` é bloqueante — o servidor espera até o cliente enviar uma linha terminada em `\n` ou `\r\n`.

### 3.5 Passo 5 — Fechar Recursos

O bloco `finally` garante o fechamento dos recursos:

```java
} finally {
    try {
        output.close();
        input.close();
        socket.close();
    } catch (IOException e) {
        System.out.println("Erro no fechamento de conexão: " + e.getMessage());
    }
}
```

**Ordem importa:** Feche na ordem inversa da criação (output → input → socket).

> **Melhoria sugerida:** O código poderia usar `try-with-resources` do Java 7+ para simplificar o gerenciamento de recursos. O próprio esqueleto sinaliza isso no comentário do `Client.java`: *"poderia otimizar usando um try-with-resources"*.

---

## 4. O Lado do Cliente

O `Client.java` do esqueleto segue o fluxo complementar:

```java
public void initClient() {
    try {
        // 1. Conexão com servidor
        conectaComServidor("localhost", 5555);

        // 2. Streams de saída e entrada
        PrintWriter output = new PrintWriter(this.socket.getOutputStream(), true);
        BufferedReader input = new BufferedReader(
            new InputStreamReader(this.socket.getInputStream()));

        // 3. Protocolo
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("$>");
        String mensagem = teclado.readLine();
        output.write(mensagem + " \r\n");
        output.write("\r\n");
        output.flush();

        // Recebe resposta
        String msg = input.readLine();
        System.out.println("Servidor respondeu: " + msg);

        // 4. Fecha recursos
        output.close();
        input.close();
        this.socket.close();
    } catch (Exception e) {
        System.out.println("Erro na comunicação: " + e.getLocalizedMessage());
    }
}
```

**Fluxo:** Lê do teclado → Envia ao servidor → Recebe resposta → Fecha tudo.

---

## 5. Diagrama de Sequência Completo

```
    ┌────────┐                              ┌────────┐
    │ Client │                              │ Server │
    └───┬────┘                              └───┬────┘
        │                                       │
        │          new ServerSocket(5555)        │
        │                                  ┌────┤
        │                                  │ bind + listen
        │                                  └────┤
        │                                       │
        │                                  accept() ← BLOQUEIA
        │                                       │
        │     new Socket("localhost", 5555)      │
        │──────────────────────────────────────►│
        │          (3-way handshake TCP)         │
        │◄─────────── conexão aceita ───────────│
        │                                       │
        │      "Hello World\r\n"                │
        │──────────────────────────────────────►│
        │                                       │ readLine()
        │                                       │ → "Hello World"
        │                                       │
        │      "Hello World\r\n"                │
        │◄──────────────────────────────────────│
        │  readLine()                           │ println(echo)
        │  → "Hello World"                      │
        │                                       │
        │          close()                      │ close()
        │──────────────────────────────────────►│
        │                                       │
```

---

## 6. Testando com Ferramentas de Rede

Você não precisa do `Client.java` para testar o servidor. Ferramentas de linha de comando permitem conectar diretamente:

### 6.1 Usando Telnet

```bash
# Terminal 1: Inicia o servidor (ou pela IDE, executando o Server)
java -cp . server.Server

# Terminal 2: Conecta como cliente
telnet localhost 5555
# Digite uma mensagem e pressione Enter
Hello World
# O servidor responde com a mesma mensagem
```

### 6.2 Usando Netcat (nc)

```bash
# Enviar uma única mensagem
echo "Hello World" | nc localhost 5555

# Modo interativo
nc localhost 5555
```

### 6.3 Usando curl (preview HTTP)

Quando o servidor for evoluído para HTTP, você poderá usar:

```bash
curl http://localhost:8080/
```

> **Exercício exploratório:** Inicie o servidor na porta 5555 e acesse `http://localhost:5555` no navegador. Observe no console do servidor a requisição HTTP raw que o navegador envia. Isso será a base da Parte 4.

---

## 7. Exercícios Incrementais

Modifique o esqueleto `Server.java` para implementar as variações abaixo. Cada exercício deve ser testado com `telnet` ou `nc`.

### Exercício 2.1 — Echo em Maiúsculas

Modifique o `clientHandle()` para devolver a mensagem convertida para maiúsculas:

```java
// Dica: use String.toUpperCase()
String msgCliente = input.readLine();
output.println(msgCliente.toUpperCase());
```

**Teste esperado:**

```
$ telnet localhost 5555
hello world
HELLO WORLD
```

### Exercício 2.2 — Echo com Timestamp

Adicione a data/hora do servidor na resposta:

```java
// Dica: use java.time.LocalDateTime
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Dentro do protocolo:
String agora = LocalDateTime.now()
    .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
output.println("[" + agora + "] " + msgCliente);
```

**Teste esperado:**

```
hello
[14:30:55] hello
```

### Exercício 2.3 — Loop de Conversação com Comando SAIR

Modifique o protocolo para manter a conexão aberta até o cliente enviar `SAIR`:

```java
// --- PROTOCOLO COM LOOP ---
String msgCliente;
while ((msgCliente = input.readLine()) != null) {
    if (msgCliente.equalsIgnoreCase("SAIR")) {
        output.println("Até logo!");
        break;
    }
    output.println("Echo: " + msgCliente);
}
// --- FIM DO PROTOCOLO ---
```

**Teste esperado:**

```
$ telnet localhost 5555
ola
Echo: ola
como vai?
Echo: como vai?
SAIR
Até logo!
Connection closed by foreign host.
```

---

## 8. Resumo do Capítulo

| Etapa             | Classe Java        | Método-chave                      |
| ----------------- | ------------------ | ---------------------------------- |
| Criar servidor    | `ServerSocket`   | `new ServerSocket(porta)`        |
| Aceitar conexão  | `ServerSocket`   | `accept()` → retorna `Socket` |
| Ler do cliente    | `BufferedReader` | `readLine()`                     |
| Enviar ao cliente | `PrintWriter`    | `println(msg)`                   |
| Fechar            | `Socket`         | `close()`                        |

**Próximo passo:** Na Parte 3, vamos projetar protocolos de aplicação mais elaborados e introduzir multithreading para atender múltiplos clientes simultaneamente.
