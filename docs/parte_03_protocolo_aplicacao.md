# Parte 3 — Protocolos de Aplicação e Multithreading

**Disciplina:** Sistemas Distribuídos I | **Código:** PF_CC.34  
**Professor:** Élder F. F. Bernardi  
**Unidade:** II — Comunicação em Sistemas Distribuídos  
**Código-base:** [`code/socket-server-basic/`](../)  

---

## 1. O que é um Protocolo de Aplicação?

Na Parte 2, o servidor echo simplesmente devolvia a mensagem recebida. Não havia "regras" de conversação — qualquer texto era aceito e devolvido. Na prática, aplicações distribuídas precisam de **regras bem definidas** para que cliente e servidor se entendam.

Um **protocolo de aplicação** é um conjunto de regras que define:

1. **Formato das mensagens**: Como os dados são estruturados (campos, delimitadores, codificação)
2. **Sequência de troca**: Quem fala primeiro? Quantas mensagens são trocadas?
3. **Semântica dos comandos**: O que cada mensagem significa?
4. **Tratamento de erros**: O que acontece quando algo dá errado?

### 1.1 Exemplos no Cotidiano

Você já usa protocolos de aplicação todos os dias:

| Protocolo | Porta | Formato | Exemplo de Comando |
|-----------|-------|---------|-------------------|
| HTTP | 80/443 | Textual | `GET /index.html HTTP/1.1` |
| SMTP | 25 | Textual | `MAIL FROM: <user@example.com>` |
| FTP | 21 | Textual | `RETR arquivo.txt` |
| DNS | 53 | Binário | *(consulta estruturada)* |

> **Observação-chave:** HTTP, SMTP e FTP são todos **protocolos textuais sobre TCP** — exatamente o tipo de protocolo que nosso esqueleto Java já está preparado para manipular com `BufferedReader.readLine()` e `PrintWriter.println()`.

---

## 2. Anatomia de um Protocolo Textual

### 2.1 Elementos Fundamentais

Todo protocolo textual precisa definir:

```
┌─────────────────────────────────────────────┐
│  COMANDO  [ARGUMENTOS]  CRLF               │
│                                             │
│  CRLF = \r\n (Carriage Return + Line Feed)  │
│  Marca o fim de cada mensagem/linha         │
└─────────────────────────────────────────────┘
```

- **Comando**: Verbo que indica a ação (ex: `GET`, `CALC`, `SAIR`)
- **Argumentos**: Dados adicionais, separados por espaço
- **Delimitador**: `\r\n` (CRLF) marca o final da mensagem — compatível com `readLine()`
- **Código de status**: Na resposta, indica sucesso ou tipo de erro

### 2.2 Padrão Comando-Resposta

A maioria dos protocolos segue o padrão **request-reply**:

```
  Cliente                    Servidor
    │                           │
    │─── COMANDO args \r\n ────►│
    │                           │ (processa)
    │◄── STATUS resultado \r\n ─│
    │                           │
```

---

## 3. Exemplo Prático: Protocolo de Calculadora Remota

Vamos projetar um protocolo simples para uma calculadora que opera remotamente.

### 3.1 Especificação do Protocolo

**Requisição do cliente:**
```
CALC <OPERAÇÃO> <OPERANDO1> <OPERANDO2>\r\n
```

**Resposta do servidor:**
```
RESULT <VALOR>\r\n          (sucesso)
ERROR <MENSAGEM>\r\n        (erro)
```

**Comandos suportados:**

| Comando | Exemplo | Resposta Esperada |
|---------|---------|-------------------|
| `CALC ADD 5 3` | Soma | `RESULT 8` |
| `CALC SUB 10 4` | Subtração | `RESULT 6` |
| `CALC MUL 3 7` | Multiplicação | `RESULT 21` |
| `CALC DIV 10 2` | Divisão | `RESULT 5` |
| `CALC DIV 10 0` | Divisão por zero | `ERROR Divisão por zero` |
| `SAIR` | Encerrar | `BYE` |
| `BLABLA` | Comando inválido | `ERROR Comando desconhecido` |

### 3.2 Implementação do Servidor

```java
private void clientHandle(Socket socket) {
    try (
        BufferedReader input = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(
            socket.getOutputStream(), true)
    ) {
        String linha;
        while ((linha = input.readLine()) != null) {
            System.out.println("Recebido: " + linha);
            String resposta = processarComando(linha);
            output.println(resposta);
            System.out.println("Enviado: " + resposta);
            
            if (linha.equalsIgnoreCase("SAIR")) break;
        }
    } catch (IOException e) {
        System.out.println("Erro: " + e.getMessage());
    }
}

private String processarComando(String linha) {
    if (linha.equalsIgnoreCase("SAIR")) {
        return "BYE";
    }
    
    String[] partes = linha.split(" ");
    
    if (partes.length != 4 || !partes[0].equalsIgnoreCase("CALC")) {
        return "ERROR Comando desconhecido";
    }
    
    String operacao = partes[1].toUpperCase();
    try {
        double a = Double.parseDouble(partes[2]);
        double b = Double.parseDouble(partes[3]);
        
        double resultado = switch (operacao) {
            case "ADD" -> a + b;
            case "SUB" -> a - b;
            case "MUL" -> a * b;
            case "DIV" -> {
                if (b == 0) throw new ArithmeticException("Divisão por zero");
                yield a / b;
            }
            default -> throw new IllegalArgumentException(
                "Operação desconhecida: " + operacao);
        };
        
        return "RESULT " + resultado;
    } catch (NumberFormatException e) {
        return "ERROR Operandos inválidos";
    } catch (ArithmeticException e) {
        return "ERROR " + e.getMessage();
    } catch (IllegalArgumentException e) {
        return "ERROR " + e.getMessage();
    }
}
```

### 3.3 Sessão de Teste

```
$ telnet localhost 5555
CALC ADD 5 3
RESULT 8.0
CALC MUL 4 7
RESULT 28.0
CALC DIV 10 0
ERROR Divisão por zero
HELLO
ERROR Comando desconhecido
SAIR
BYE
Connection closed by foreign host.
```

### 3.4 Lições Aprendidas

1. **Separação de responsabilidades**: O método `processarComando()` é independente do socket — ele recebe uma String e retorna uma String. Isso facilita testes unitários.
2. **Tratamento de erros**: Todo input inesperado gera uma resposta `ERROR` legível, em vez de crashar o servidor.
3. **Protocolo documentável**: Qualquer programador pode implementar um cliente compatível apenas lendo a especificação da seção 3.1.

---

## 4. O Problema da Concorrência

### 4.1 O Servidor Iterativo

Observe o `connectionLoop()` do esqueleto:

```java
while (true) {
    Socket socket = this.esperaConexao(); // BLOQUEIA
    clientHandle(socket);                 // BLOQUEIA
}
```

**Problema:** Enquanto o servidor está tratando um cliente em `clientHandle()`, ele **não pode aceitar novas conexões**. Se o Cliente A está conectado e o Cliente B tenta se conectar, o Cliente B fica **esperando** até o Cliente A terminar.

```
Tempo →  ─────────────────────────────────────────────►

Servidor: │ accept() │ handle(A) ████████│ accept() │ handle(B) ████│

Cliente A: ──────────── conectado ────────│ encerrado

Cliente B: ──── esperando ████████████████│ conectado ────────│
                                         ↑
                                         B só conecta quando A termina!
```

**Isso é inaceitável** para qualquer servidor real. Um web server que atendesse um cliente por vez seria inutilizável.

### 4.2 A Solução: Multithreading

O próprio esqueleto do professor já antecipa essa evolução nos comentários (linhas 36-46 do `Server.java`):

```
/**
 * Criação da estrutura multithreading para atender clientes concorrentes
 * 1. Criar uma classe que implemente Runnable para gerir a comunicação
 * 2. Passar o socket do cliente para o construtor dessa classe
 * 3. Criar uma nova Thread, passando uma instância dessa classe
 * 4. Iniciar a Thread
 * 5. dentro do run(), implementar a lógica de comunicação
 */
```

A ideia é simples: em vez de o loop principal tratar o cliente diretamente, ele **delega** o tratamento para uma thread separada e volta imediatamente a aceitar novas conexões.

### 4.3 Implementação do ClientHandler

**Passo 1:** Criar a classe `ClientHandler` que implementa `Runnable`:

```java
package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader input = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(
                socket.getOutputStream(), true)
        ) {
            String msgCliente;
            while ((msgCliente = input.readLine()) != null) {
                System.out.println("[" + Thread.currentThread().getName() 
                    + "] Recebido: " + msgCliente);
                
                if (msgCliente.equalsIgnoreCase("SAIR")) {
                    output.println("BYE");
                    break;
                }
                output.println("Echo: " + msgCliente);
            }
        } catch (IOException e) {
            System.out.println("Erro no handler: " + e.getMessage());
        } finally {
            try { socket.close(); } 
            catch (IOException e) { /* ignora */ }
        }
    }
}
```

**Passo 2:** Modificar o `connectionLoop()` para usar threads:

```java
public void connectionLoop() throws IOException {
    try {
        while (true) {
            System.out.println("Esperando conexao...");
            Socket socket = this.esperaConexao();
            System.out.println("Conexao recebida! Criando thread...");

            // Em vez de clientHandle(socket), delegamos para uma thread:
            Thread thread = new Thread(new ClientHandler(socket));
            thread.start();
            // O loop volta IMEDIATAMENTE para aceitar a próxima conexão
        }
    } catch (Exception e) {
        System.out.println("Erro: " + e.getMessage());
    }
}
```

### 4.4 Visualização

```
Tempo →  ─────────────────────────────────────────────►

Main:     │accept()│ start(A) │accept()│ start(B) │accept()│ ...

Thread-A:          │████ handle(A) ████████████████│

Thread-B:                     │████ handle(B) ██████████│

Clientes A e B são atendidos SIMULTANEAMENTE!
```

### 4.5 Limitações de Thread-per-Connection

Criar uma thread para **cada** conexão funciona para poucos clientes, mas não escala:

| Clientes | Threads | RAM (~512KB/thread) | Status |
|----------|---------|---------------------|--------|
| 10 | 10 | ~5 MB | Tranquilo |
| 100 | 100 | ~50 MB | OK |
| 10.000 | 10.000 | ~5 GB | Problemático |
| 100.000 | 100.000 | ~50 GB | Impossível |

**Alternativas para escala (menção):**
- **Thread Pool** (`ExecutorService`): Reutiliza um número fixo de threads
- **Java NIO** (`java.nio`): I/O não-bloqueante com `Selector`
- **Event-driven** (modelo do Node.js): Um único thread com event loop

> Estas alternativas estão fora do escopo deste material, mas é importante saber que existem. Para o exercício do web server HTTP, o modelo thread-per-connection é mais que suficiente.

---

## 5. Exercícios

### Exercício 3.1 — Implementar o ClientHandler

Modifique o esqueleto `Server.java`:
1. Crie a classe `ClientHandler.java` no pacote `server`
2. Implemente `Runnable` conforme o modelo da seção 4.3
3. Modifique `connectionLoop()` para usar threads
4. Teste com 2 terminais `telnet` conectados simultaneamente

### Exercício 3.2 — Implementar o Protocolo da Calculadora

Usando o servidor multithread do exercício 3.1:
1. Implemente o método `processarComando()` da seção 3.2
2. Adicione as operações `MOD` (módulo) e `POW` (potência)
3. Teste com múltiplos clientes simultâneos fazendo cálculos diferentes

### Exercício 3.3 — Protocolo Customizado

Projete e implemente seu próprio protocolo de aplicação para um dos cenários:
- **Chat simples**: `MSG <texto>`, `NICK <nome>`, `LIST`, `SAIR`
- **To-Do List**: `ADD <tarefa>`, `LIST`, `DONE <id>`, `REMOVE <id>`
- **Quiz**: `NEXT` (próxima pergunta), `ANSWER <resposta>`, `SCORE`

Para cada um, documente: (a) formato das mensagens, (b) sequência de troca, (c) códigos de erro.

---

## 6. Resumo do Capítulo

| Conceito | Descrição |
|----------|-----------|
| **Protocolo de aplicação** | Regras de conversação sobre TCP |
| **Request-Reply** | Cliente envia comando, servidor responde |
| **CRLF** | `\r\n` — delimitador padrão de protocolos textuais |
| **Servidor iterativo** | Atende um cliente por vez (não escalável) |
| **Servidor multithread** | Cria thread para cada cliente (escalável até ~milhares) |
| **ClientHandler** | Classe `Runnable` que encapsula a lógica de comunicação |

**Próximo passo:** Na Parte 4, aplicaremos tudo isso para entender e implementar o início de um Web Server HTTP — o protocolo de aplicação mais importante da internet.
