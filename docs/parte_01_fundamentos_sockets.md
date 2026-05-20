# Parte 1 — Fundamentos: O que é um Socket?

**Disciplina:** Sistemas Distribuídos I | **Código:** PF_CC.34  
**Professor:** Élder F. F. Bernardi  
**Unidade:** II — Comunicação em Sistemas Distribuídos  
**Fontes:** Oracle Java Tutorials, Python Socket HOWTO, Node.js Net Module, Beej's Guide to Network Programming  

---

## 1. Introdução

Quando dois processos precisam se comunicar através de uma rede, eles necessitam de um mecanismo padronizado para estabelecer conexões, enviar dados e receber respostas. Esse mecanismo é o **socket** — a interface de programação (API) fundamental para comunicação em rede.

Este capítulo apresenta os conceitos teóricos essenciais que fundamentam toda a programação cliente-servidor. Dominar esses fundamentos é pré-requisito para compreender qualquer protocolo de aplicação, incluindo o HTTP que move a web.

---

## 2. O que é um Socket?

Um **socket** é um **endpoint de comunicação** — um ponto final através do qual um processo pode enviar e receber dados pela rede. Tecnicamente, um socket é identificado pela combinação de:

- **Endereço IP**: identifica a máquina na rede (ex: `192.168.1.10`)
- **Número de Porta**: identifica o processo específico naquela máquina (ex: `5555`)

```
Socket = (Endereço IP, Porta)

Exemplo: (192.168.1.10, 5555) — identifica unicamente um processo
                                 na máquina 192.168.1.10, porta 5555
```

### 2.1 Analogia

Pense num prédio comercial: o **endereço IP** é o endereço do prédio (Rua X, nº 100), e a **porta** é o número da sala dentro do prédio. Para entregar uma correspondência, você precisa dos dois: o prédio E a sala específica.

### 2.2 Portas Conhecidas (Well-Known Ports)

| Porta | Protocolo | Serviço |
|-------|-----------|---------|
| 22    | TCP       | SSH     |
| 53    | TCP/UDP   | DNS     |
| 80    | TCP       | HTTP    |
| 443   | TCP       | HTTPS   |
| 5555  | TCP       | *(usada no esqueleto do curso)* |
| 8080  | TCP       | HTTP alternativo (dev servers) |

> **Regra prática:** Portas de 0 a 1023 são **reservadas** para serviços do sistema operacional. Use portas acima de 1024 para suas aplicações de desenvolvimento.

---

## 3. Modelo de Camadas (Visão Simplificada)

Para entender onde os sockets se encaixam, é útil visualizar o modelo de camadas de rede. Utilizamos uma versão simplificada do modelo TCP/IP:

```
┌─────────────────────────────────────────┐
│         Camada de Aplicação             │  ← Seu código (HTTP, FTP, SMTP)
│         (Protocolo de Aplicação)        │
├─────────────────────────────────────────┤
│         *** API de Sockets ***          │  ← Interface entre seu código e a rede
├─────────────────────────────────────────┤
│         Camada de Transporte            │  ← TCP ou UDP
│         (TCP / UDP)                     │
├─────────────────────────────────────────┤
│         Camada de Rede                  │  ← IP (roteamento)
│         (IP)                            │
├─────────────────────────────────────────┤
│         Camada de Enlace/Física         │  ← Ethernet, Wi-Fi, etc.
│         (Hardware)                      │
└─────────────────────────────────────────┘
```

**Ponto-chave:** A API de sockets é a **fronteira** entre a camada de aplicação (onde você programa) e a camada de transporte (que o sistema operacional gerencia). Ao programar com sockets, você está essencialmente dizendo ao sistema operacional: *"Quero enviar estes bytes para aquele endereço usando TCP (ou UDP)"*.

---

## 4. TCP vs UDP

A camada de transporte oferece dois protocolos principais. A escolha entre eles é uma das primeiras decisões de projeto em qualquer sistema distribuído.

### 4.1 TCP — Transmission Control Protocol

| Característica | Descrição |
|---------------|-----------|
| **Orientação** | Orientado a conexão (connection-oriented) |
| **Confiabilidade** | Garante entrega, ordenação e integridade dos dados |
| **Fluxo** | Stream de bytes contínuo (sem fronteiras de mensagem) |
| **Handshake** | 3-way handshake antes de transmitir |
| **Uso típico** | HTTP, SSH, e-mail, transferência de arquivos |
| **Analogia** | Ligação telefônica: estabelece conexão, conversa, desliga |

### 4.2 UDP — User Datagram Protocol

| Característica | Descrição |
|---------------|-----------|
| **Orientação** | Sem conexão (connectionless) |
| **Confiabilidade** | Não garante entrega nem ordenação |
| **Fluxo** | Datagramas independentes (cada pacote é uma unidade) |
| **Handshake** | Nenhum — "fire and forget" |
| **Uso típico** | DNS, streaming de vídeo, jogos online, VoIP |
| **Analogia** | Enviar uma carta: coloca no correio e torce para chegar |

### 4.3 Quando usar cada um?

```
Precisa de confiabilidade?
│
├── SIM → Use TCP
│         (Ex: transferir um arquivo, servir uma página web,
│          executar um comando remoto)
│
└── NÃO → Use UDP
          (Ex: streaming ao vivo — se um frame se perder,
           não faz sentido retransmitir um quadro antigo)
```

> **Neste material, trabalharemos exclusivamente com TCP**, pois nosso objetivo final é construir um web server HTTP — e HTTP é um protocolo sobre TCP.

---

## 5. Ciclo de Vida de uma Conexão TCP

Antes de dados serem trocados, o TCP exige um **estabelecimento formal de conexão** (3-way handshake):

```
   Cliente                        Servidor
     │                               │
     │──── SYN ──────────────────────►│  1. "Quero conectar"
     │                               │
     │◄─── SYN-ACK ──────────────────│  2. "OK, aceito. Também quero conectar"
     │                               │
     │──── ACK ──────────────────────►│  3. "Confirmado. Conexão estabelecida"
     │                               │
     │◄═══ Dados (bidirecional) ═════►│  4. Transferência de dados
     │                               │
     │──── FIN ──────────────────────►│  5. "Quero encerrar"
     │◄─── ACK ──────────────────────│
     │◄─── FIN ──────────────────────│  6. "Eu também"
     │──── ACK ──────────────────────►│
     │                               │
```

Esse processo é **transparente** para o programador — a API de sockets cuida disso. Quando seu código chama `connect()`, o 3-way handshake acontece automaticamente.

---

## 6. Fluxo de Chamadas da API de Sockets

Toda comunicação cliente-servidor via sockets TCP segue o mesmo padrão fundamental, independentemente da linguagem de programação:

### 6.1 Lado do Servidor

```
 1. socket()    → Cria o socket do servidor
 2. bind()      → Associa o socket a um endereço IP e porta
 3. listen()    → Marca o socket como passivo (aceita conexões)
 4. accept()    → Bloqueia até um cliente conectar
                   Retorna um NOVO socket para comunicação
 5. read/write  → Troca dados com o cliente pelo novo socket
 6. close()     → Encerra a conexão
```

### 6.2 Lado do Cliente

```
 1. socket()    → Cria o socket do cliente
 2. connect()   → Conecta ao servidor (IP + Porta)
                   Dispara o 3-way handshake
 3. write/read  → Envia e recebe dados
 4. close()     → Encerra a conexão
```

### 6.3 Visão Integrada

```
       SERVIDOR                              CLIENTE
   ┌──────────────┐                     ┌──────────────┐
   │  socket()    │                     │  socket()    │
   │  bind()      │                     │              │
   │  listen()    │                     │              │
   │              │                     │              │
   │  accept() ◄──┼─────── connect() ──┼──────────────│
   │    │         │                     │              │
   │    ▼         │                     │              │
   │  read()  ◄───┼─────── write() ────┼──────────────│
   │  write() ────┼──────► read()  ────┼──────────────│
   │              │                     │              │
   │  close()     │                     │  close()     │
   └──────────────┘                     └──────────────┘
```

> **Observação importante:** O `accept()` retorna um **novo** socket dedicado à comunicação com aquele cliente específico. O socket original do servidor continua escutando por novas conexões. Esta distinção é fundamental para entender concorrência.

---

## 7. Comparativo de APIs: Java, Python e Node.js

As três linguagens oferecem APIs para sockets TCP, mas com abordagens e níveis de abstração distintos. A tabela abaixo resume as equivalências:

### 7.1 Criação e Configuração do Servidor

| Operação | Java | Python | Node.js |
|----------|------|--------|---------|
| Criar servidor | `new ServerSocket(porta)` | `socket.socket(AF_INET, SOCK_STREAM)` | `net.createServer(callback)` |
| Vincular a porta | *(feito no construtor)* | `sock.bind(('', porta))` | `server.listen(porta)` |
| Escutar | *(feito no construtor)* | `sock.listen(backlog)` | *(feito no listen)* |
| Aceitar conexão | `serverSocket.accept()` | `sock.accept()` | *(callback automático)* |

### 7.2 Comunicação (Leitura e Escrita)

| Operação | Java | Python | Node.js |
|----------|------|--------|---------|
| Ler dados | `BufferedReader.readLine()` | `conn.recv(buffer_size)` | `socket.on('data', cb)` |
| Enviar dados | `PrintWriter.println()` | `conn.sendall(bytes)` | `socket.write(data)` |
| Fechar | `socket.close()` | `conn.close()` | `socket.end()` |

### 7.3 Criação do Cliente

| Operação | Java | Python | Node.js |
|----------|------|--------|---------|
| Conectar | `new Socket(host, porta)` | `sock.connect((host, porta))` | `net.createConnection(porta, host)` |

### 7.4 Paradigma de Programação

| Aspecto | Java | Python | Node.js |
|---------|------|--------|---------|
| **Modelo** | Bloqueante (threads) | Bloqueante (ou `selectors`) | Não-bloqueante (event loop) |
| **I/O** | Streams (`InputStream`/`OutputStream`) | Bytes (`recv`/`sendall`) | Events + Buffers |
| **Concorrência** | `Thread` / `ExecutorService` | `threading` / `asyncio` | Event-driven nativo |

> **Para este curso:** Usaremos Java como linguagem primária, pois é a linguagem do código-esqueleto fornecido pelo professor. Implementações equivalentes em Python e Node.js estão disponíveis nos apêndices.

---

## 8. Resumo do Capítulo

| Conceito | Definição |
|----------|-----------|
| **Socket** | Endpoint de comunicação = IP + Porta |
| **TCP** | Protocolo confiável, orientado a conexão, stream de bytes |
| **UDP** | Protocolo não confiável, sem conexão, datagramas independentes |
| **3-way handshake** | SYN → SYN-ACK → ACK (estabelece conexão TCP) |
| **accept()** | Retorna novo socket para comunicar com o cliente |
| **API de Sockets** | Interface entre a aplicação e a camada de transporte |

---

## 9. Referências

1. **Beej's Guide to Network Programming** — https://beej.us/guide/bgnet/html/
2. **Beej's Guide to Network Concepts** — https://beej.us/guide/bgnet0/html/
3. **Oracle Java Tutorials: All About Sockets** — https://docs.oracle.com/javase/tutorial/networking/sockets/
4. **Python Socket Programming HOWTO** — https://docs.python.org/3/howto/sockets.html
5. **Node.js Net Module** — https://nodejs.org/api/net.html
6. **TANENBAUM, A. S.; VAN STEEN, M.** *Sistemas Distribuídos: Princípios e Paradigmas*. 2. ed. Cap. 4. Pearson, 2007.
