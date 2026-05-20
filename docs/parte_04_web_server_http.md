# Parte 4 — Introdução ao Web Server HTTP

**Disciplina:** Sistemas Distribuídos I | **Código:** PF_CC.34
**Professor:** Élder F. F. Bernardi
**Unidade:** II — Comunicação em Sistemas Distribuídos
**Referências:** RFC 7230 (HTTP/1.1 Message Syntax), MDN Web Docs (Overview of HTTP)

---

## 1. De Sockets a HTTP: A Conexão

Nas partes anteriores, construímos:

- Um **echo server** que lê e escreve texto sobre TCP (Parte 2)
- Um **protocolo de aplicação** com comandos e respostas estruturados (Parte 3)
- Um **servidor multithread** que atende clientes simultâneos (Parte 3)

O **HTTP** (Hypertext Transfer Protocol) é simplesmente **mais um protocolo de aplicação textual sobre TCP** — exatamente o tipo de coisa que já sabemos construir. O HTTP segue o mesmo padrão request-reply:

```
  Browser (Cliente)              Web Server (Servidor)
       │                               │
       │── GET /index.html HTTP/1.1 ──►│
       │   Host: localhost              │
       │                               │ (lê arquivo, monta resposta)
       │◄── HTTP/1.1 200 OK ───────────│
       │    Content-Type: text/html     │
       │    <html>...</html>            │
       │                               │
```

> **A sacada fundamental:** O nosso `Server.java` já lê linhas com `BufferedReader.readLine()` e escreve com `PrintWriter.println()`. O HTTP envia cabeçalhos **linha por linha**, terminados em `\r\n`. A infraestrutura que temos é **exatamente** o que precisamos.

---

## 2. Anatomia de uma Requisição HTTP

Quando você digita `http://localhost:8080/index.html` no navegador, ele envia um bloco de texto como este pela conexão TCP:

```http
GET /index.html HTTP/1.1\r\n
Host: localhost:8080\r\n
User-Agent: Mozilla/5.0 ...\r\n
Accept: text/html\r\n
Connection: close\r\n
\r\n
```

### 2.1 Estrutura Formal

A requisição HTTP tem três partes:

```
┌──────────────────────────────────────────────────┐
│  LINHA DE REQUISIÇÃO (Request Line)              │
│  MÉTODO  SP  URI  SP  VERSÃO  CRLF              │
│  Ex: GET /index.html HTTP/1.1\r\n               │
├──────────────────────────────────────────────────┤
│  CABEÇALHOS (Headers)                            │
│  Chave: Valor CRLF                               │
│  Chave: Valor CRLF                               │
│  ...                                             │
├──────────────────────────────────────────────────┤
│  LINHA EM BRANCO (CRLF)                          │
│  Marca o fim dos cabeçalhos                      │
├──────────────────────────────────────────────────┤
│  CORPO (Body) — Opcional                         │
│  Presente em POST, PUT                           │
│  O tamanho é indicado pelo header Content-Length │
└──────────────────────────────────────────────────┘
```

### 2.2 Métodos HTTP Essenciais

| Método    | Significado              | Corpo? | Uso                             |
| ---------- | ------------------------ | ------ | ------------------------------- |
| `GET`    | Solicitar um recurso     | Não   | Acessar páginas, imagens, APIs |
| `POST`   | Enviar dados ao servidor | Sim    | Formulários, upload de dados   |
| `PUT`    | Substituir um recurso    | Sim    | Atualizar dados                 |
| `DELETE` | Remover um recurso       | Não   | Apagar dados                    |

### 2.3 Cabeçalhos Importantes

| Header             | Direção | Exemplo                     | Significado                                    |
| ------------------ | --------- | --------------------------- | ---------------------------------------------- |
| `Host`           | Request   | `Host: localhost:8080`    | Servidor de destino (obrigatório em HTTP/1.1) |
| `Content-Type`   | Ambos     | `Content-Type: text/html` | Tipo MIME do conteúdo                         |
| `Content-Length` | Ambos     | `Content-Length: 1234`    | Tamanho do corpo em bytes                      |
| `Connection`     | Ambos     | `Connection: close`       | Se a conexão deve ser mantida ou fechada      |

---

## 3. Anatomia de uma Resposta HTTP

A resposta do servidor segue estrutura similar:

```http
HTTP/1.1 200 OK\r\n
Content-Type: text/html\r\n
Content-Length: 45\r\n
\r\n
<html><body><h1>Hello!</h1></body></html>
```

### 3.1 Estrutura Formal

```
┌──────────────────────────────────────────────────┐
│  LINHA DE STATUS (Status Line)                   │
│  VERSÃO  SP  CÓDIGO  SP  RAZÃO  CRLF            │
│  Ex: HTTP/1.1 200 OK\r\n                        │
├──────────────────────────────────────────────────┤
│  CABEÇALHOS (Headers)                            │
│  Content-Type: text/html CRLF                    │
│  Content-Length: 45 CRLF                         │
├──────────────────────────────────────────────────┤
│  LINHA EM BRANCO (CRLF)                          │
├──────────────────────────────────────────────────┤
│  CORPO (Body)                                    │
│  O conteúdo HTML, JSON, imagem, etc.             │
└──────────────────────────────────────────────────┘
```

### 3.2 Códigos de Status Essenciais

| Código | Razão                | Significado                         |
| ------- | --------------------- | ----------------------------------- |
| `200` | OK                    | Requisição processada com sucesso |
| `301` | Moved Permanently     | Recurso mudou de endereço          |
| `400` | Bad Request           | Requisição malformada             |
| `404` | Not Found             | Recurso não encontrado             |
| `405` | Method Not Allowed    | Método HTTP não suportado         |
| `500` | Internal Server Error | Erro interno do servidor            |

**Padrão dos códigos:**

- **2xx** → Sucesso
- **3xx** → Redirecionamento
- **4xx** → Erro do cliente
- **5xx** → Erro do servidor

---

## 4. Do Socket Server ao Web Server: O Caminho

A evolução do nosso esqueleto para um web server HTTP envolve **quatro incrementos**. Cada um adiciona uma camada de funcionalidade sobre a anterior.

### 4.1 Incremento 1 — Receber e Imprimir a Requisição Raw

O primeiro passo é **simplesmente observar** o que o navegador envia. Modifique o `clientHandle()`:

```java
private void clientHandle(Socket socket) {
    try (
        BufferedReader input = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        PrintWriter output = new PrintWriter(
            socket.getOutputStream(), true)
    ) {
        // Lê e imprime TODAS as linhas da requisição HTTP
        String linha;
        while ((linha = input.readLine()) != null) {
            if (linha.isEmpty()) break; // Linha em branco = fim dos headers
            System.out.println(linha);
        }
    
        // Resposta mínima para o browser não reclamar
        output.println("HTTP/1.1 200 OK");
        output.println("Content-Type: text/plain");
        output.println();
        output.println("Servidor funcionando!");
    
    } catch (IOException e) {
        System.out.println("Erro: " + e.getMessage());
    }
}
```

**Teste:** Acesse `http://localhost:5555` no navegador. No console do servidor, você verá algo como:

```
GET / HTTP/1.1
Host: localhost:5555
User-Agent: Mozilla/5.0 (X11; Linux x86_64) ...
Accept: text/html,application/xhtml+xml,...
Accept-Language: pt-BR,pt;q=0.9
Connection: keep-alive
```

> **Este momento é revelador:** O navegador está enviando texto puro via TCP. Não há mágica — é exatamente o que nosso `BufferedReader.readLine()` já sabe ler.

### 4.2 Incremento 2 — Parsear o Request Line

O segundo passo é **extrair informação** da primeira linha:

```java
// Lê a primeira linha (Request Line)
String requestLine = input.readLine();
if (requestLine == null) return;

// Parseia: "GET /index.html HTTP/1.1"
String[] partes = requestLine.split(" ");
String metodo = partes[0];    // "GET"
String uri = partes[1];       // "/index.html"
String versao = partes[2];    // "HTTP/1.1"

System.out.println("Método: " + metodo);
System.out.println("URI: " + uri);
System.out.println("Versão: " + versao);
```

### 4.3 Incremento 3 — Resolver a Rota (Método + Path)

O terceiro passo é decidir **o que fazer** com a requisição parseada. Para isso, precisamos entender um conceito central em aplicações HTTP: a **rota**.

#### O Conceito de Rota

Uma **rota** é a combinação de **método HTTP + path (URI)** e representa um **recurso** que o servidor disponibiliza. A palavra-chave aqui é *recurso* — que **não é necessariamente um arquivo**:

| Rota                | Recurso                                  | Tipo                                            |
| ------------------- | ---------------------------------------- | ----------------------------------------------- |
| `GET /index.html` | Página HTML armazenada em disco         | **Arquivo estático**                     |
| `GET /styles.css` | Folha de estilos                         | **Arquivo estático**                     |
| `GET /api/hora`   | A hora atual do servidor                 | **Dado dinâmico** (gerado em tempo real) |
| `POST /api/calc`  | Resultado de um cálculo enviado no body | **Dado dinâmico** (processamento)        |
| `GET /api/alunos` | Lista de alunos em JSON                  | **Dado dinâmico** (consulta a dados)     |

> **Analogia com o protocolo da calculadora (Parte 3):** Na calculadora, o comando `CALC ADD 5 3` determinava a ação. Em HTTP, a "ação" é determinada pela rota: o método diz *o que fazer* e o path diz *com qual recurso*.

#### Implementando um Roteador Simples

O servidor precisa de uma lógica que, dado o método e o path, decida como gerar a resposta:

```java
// --- Roteamento: decide o que fazer com base em método + path ---

if (metodo.equals("GET") && uri.equals("/")) {
    // Rota: GET / → servir arquivo index.html
    servirArquivo(output, "./www/index.html");

} else if (metodo.equals("GET") && uri.equals("/api/hora")) {
    // Rota: GET /api/hora → retornar dado dinâmico
    String agora = LocalDateTime.now().toString();
    enviarResposta(output, 200, "text/plain", agora);

} else if (metodo.equals("GET") && uri.startsWith("/")) {
    // Rota genérica: tentar servir arquivo estático
    File arquivo = new File("./www" + uri);
    if (arquivo.exists() && arquivo.isFile()) {
        servirArquivo(output, arquivo.getPath());
    } else {
        enviarResposta(output, 404, "text/plain", "404 - Recurso não encontrado");
    }

} else {
    enviarResposta(output, 405, "text/plain", "405 - Método não suportado");
}
```

Perceba que o roteador trata **dois tipos distintos de recurso**:

1. **Arquivos estáticos** (HTML, CSS, JS, imagens) — o servidor lê do disco e envia
2. **Dados dinâmicos** (hora, cálculos, listas) — o servidor gera a resposta em tempo de execução

> **Na atividade avaliativa**, o web server deverá responder a rotas de ambos os tipos: servir arquivos estáticos de um diretório e responder rotas de API que retornam dados gerados pelo servidor.

### 4.4 Incremento 4 — Construir a Resposta HTTP

O quarto passo é **montar a resposta completa** com os headers corretos. Duas preocupações:

**a) Determinar o Content-Type** (para arquivos estáticos):

```java
private String getContentType(String path) {
    if (path.endsWith(".html")) return "text/html";
    if (path.endsWith(".css"))  return "text/css";
    if (path.endsWith(".js"))   return "application/javascript";
    if (path.endsWith(".json")) return "application/json";
    if (path.endsWith(".png"))  return "image/png";
    if (path.endsWith(".jpg"))  return "image/jpeg";
    return "application/octet-stream";
}
```

**b) Montar a resposta com status line + headers + body:**

```java
private void enviarResposta(PrintWriter output, int codigo, 
                            String contentType, String body) {
    String razao = switch (codigo) {
        case 200 -> "OK";
        case 404 -> "Not Found";
        case 405 -> "Method Not Allowed";
        case 500 -> "Internal Server Error";
        default  -> "Unknown";
    };
    output.println("HTTP/1.1 " + codigo + " " + razao);
    output.println("Content-Type: " + contentType);
    output.println("Content-Length: " + body.getBytes().length);
    output.println(); // Linha em branco: fim dos headers
    output.print(body);
    output.flush();
}
```

---

## 5. Resumo: O Mapa do Caminho

```
  Esqueleto Atual              Evolução                  Web Server HTTP
  ┌───────────┐    ┌─────────────────────────┐    ┌──────────────────────┐
  │ Socket    │    │ Incremento 1:           │    │ Servidor HTTP        │
  │ Echo      │───►│ Imprimir requisição raw │───►│ completo que:        │
  │ Server    │    │                         │    │                      │
  │           │    │ Incremento 2:           │    │ • Parseia request    │
  │ (1 cliente│    │ Parsear request line    │    │ • Resolve rotas      │
  │  por vez) │    │                         │    │ • Serve arquivos     │
  │           │    │ Incremento 3:           │    │ • Retorna dados/API  │
  │           │    │ Resolver rota           │    │ • Retorna 404/500    │
  │           │    │ (método + path)         │    │ • Multithread        │
  │           │    │                         │    │                      │
  │           │    │ Incremento 4:           │    │                      │
  │           │    │ Montar resposta HTTP    │    │                      │
  └───────────┘    └─────────────────────────┘    └──────────────────────┘
```

> **A atividade prática de construção do web server HTTP será proposta como trabalho avaliativo separado.** O material fornece os fundamentos teóricos e os primeiros passos (incrementos 1 e 2) necessários para realizá-la com sucesso.

---

## 6. Para Testar suas Primeiras Modificações

### 6.1 Com o Navegador

Simplesmente acesse `http://localhost:5555` ou `http://localhost:8080` (se alterar a porta).

### 6.2 Com curl

```bash
# Requisição GET simples
curl -v http://localhost:8080/

# Ver apenas os headers
curl -I http://localhost:8080/

# Enviar POST
curl -X POST -d "dados=teste" http://localhost:8080/enviar
```

### 6.3 Com Arquivos .http (VS Code / NetBeans)

O esqueleto já inclui `tests/requests.http`:

```http
###
POST http://localhost/teste
Content-Type: text/html

<html>
  <body>
    <h1>Teste</h1>
  </body>
</html>

###
GET http://localhost/teste
```

> **Dica:** A extensão "REST Client" do VS Code permite executar estas requisições diretamente do editor.

---

## 7. Resumo do Capítulo

| Conceito               | Descrição                                               |
| ---------------------- | --------------------------------------------------------- |
| **HTTP**         | Protocolo de aplicação textual sobre TCP                |
| **Request Line** | `MÉTODO URI VERSÃO` — primeira linha da requisição |
| **Headers**      | Pares `Chave: Valor` com metadados da mensagem          |
| **CRLF duplo**   | `\r\n\r\n` marca o fim dos headers                      |
| **Status Code**  | Número de 3 dígitos indicando resultado (200, 404, 500) |
| **Content-Type** | Header que indica o tipo MIME do recurso                  |

---

## 8. Referências

1. **RFC 7230** — HTTP/1.1: Message Syntax and Routing — https://datatracker.ietf.org/doc/html/rfc7230
2. **MDN Web Docs** — Overview of HTTP — https://developer.mozilla.org/en-US/docs/Web/HTTP/Overview
3. **Oracle Java Tutorials** — All About Sockets — https://docs.oracle.com/javase/tutorial/networking/sockets/
4. **TANENBAUM, A. S.; VAN STEEN, M.** *Sistemas Distribuídos: Princípios e Paradigmas*. 2. ed. Cap. 4. Pearson, 2007.
