# Socket Server Basic

Esqueleto de programação cliente-servidor com sockets TCP em Java, utilizado na disciplina de **Sistemas Distribuídos I** do IFSul — Campus Passo Fundo.

## Objetivo

Este repositório fornece a base de código para as atividades práticas sobre comunicação entre processos via sockets TCP. O esqueleto implementa um servidor e cliente Java mínimos que demonstram o ciclo completo de uma conexão TCP:

1. **Criar** o `ServerSocket` e vincular a uma porta
2. **Aguardar** conexões com `accept()` (bloqueante)
3. **Criar** streams de entrada/saída (`BufferedReader` / `PrintWriter`)
4. **Tratar** o protocolo de aplicação (leitura e resposta de mensagens)
5. **Fechar** streams e conexão

## Estrutura do Projeto

```
socket-server-basic/
├── server/
│   └── Server.java          # Servidor TCP iterativo
├── client/
│   └── Client.java          # Cliente TCP simples
├── tests/
│   └── requests.http         # Requisições HTTP para testes
├── docs/                     # 📚 Material didático de apoio
│   ├── parte_01_fundamentos_sockets.md
│   ├── parte_02_echo_server.md
│   ├── parte_03_protocolo_aplicacao.md
│   ├── parte_04_web_server_http.md
│   ├── apendice_a_python.md
│   └── apendice_b_nodejs.md
└── README.md
```

## Como Executar

### Servidor

```bash
# Compilar
javac server/Server.java

# Executar (escuta na porta 5555)
java server.Server
```

### Cliente

```bash
# Compilar
javac client/Client.java

# Executar (conecta em localhost:5555)
java client.Client
```

### Testando com ferramentas de rede

```bash
# Telnet (interativo)
telnet localhost 5555

# Netcat (enviar mensagem)
echo "Hello World" | nc localhost 5555

# Curl (quando evoluído para HTTP)
curl -v http://localhost:8080/
```

## 📚 Documentação de Apoio

O diretório [`docs/`](docs/) contém o material didático completo que acompanha este código. A leitura é progressiva — cada parte constrói sobre a anterior:

| # | Documento | Conteúdo |
|---|-----------|----------|
| 1 | [Fundamentos de Sockets](docs/parte_01_fundamentos_sockets.md) | O que é um socket, TCP vs UDP, modelo de camadas, fluxo de chamadas `socket()` → `accept()` → `read/write` → `close()`, comparativo Java/Python/Node.js |
| 2 | [Echo Server (Prática)](docs/parte_02_echo_server.md) | Análise passo a passo do código deste repositório, diagrama de sequência, exercícios incrementais (maiúsculas, timestamp, comando SAIR) |
| 3 | [Protocolos de Aplicação](docs/parte_03_protocolo_aplicacao.md) | Design de protocolos textuais sobre TCP, exemplo de calculadora remota (`CALC ADD 5 3`), introdução a multithreading com `ClientHandler implements Runnable` |
| 4 | [Web Server HTTP](docs/parte_04_web_server_http.md) | Anatomia de requisições e respostas HTTP, conceito de rotas (método + path), os 4 incrementos para evoluir o esqueleto em um web server |

### Apêndices (Outras Linguagens)

| # | Documento | Conteúdo |
|---|-----------|----------|
| A | [Python](docs/apendice_a_python.md) | Implementações equivalentes usando o módulo `socket`, contraste de paradigma (bytes vs streams) |
| B | [Node.js](docs/apendice_b_nodejs.md) | Implementações equivalentes usando o módulo `net`, modelo event-driven vs bloqueante |

## Roteiro de Evolução

O código deste repositório é o **ponto de partida**. A documentação guia a evolução progressiva:

```
Echo Server  →  Protocolo customizado  →  Multithreading  →  Web Server HTTP
(este repo)     (Parte 3)                 (Parte 3)          (Parte 4 + Atividade)
```

## Referências

- [Beej's Guide to Network Programming](https://beej.us/guide/bgnet/html/)
- [Oracle Java Tutorials: All About Sockets](https://docs.oracle.com/javase/tutorial/networking/sockets/)
- [RFC 7230: HTTP/1.1 Message Syntax](https://datatracker.ietf.org/doc/html/rfc7230)
- [MDN: Overview of HTTP](https://developer.mozilla.org/en-US/docs/Web/HTTP/Overview)

---

**Disciplina:** Sistemas Distribuídos I (PF_CC.34)  
**Professor:** Élder F. F. Bernardi  
**Instituição:** IFSul — Campus Passo Fundo
