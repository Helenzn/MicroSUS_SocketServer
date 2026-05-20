# Apêndice B — Implementações em Node.js

**Disciplina:** Sistemas Distribuídos I | **Código:** PF_CC.34  
**Professor:** Élder F. F. Bernardi  
**Referência:** Node.js Net Module Documentation — https://nodejs.org/api/net.html  

---

Este apêndice apresenta as implementações equivalentes em Node.js dos exemplos Java cobertos na apostila principal. Node.js utiliza um modelo **event-driven** (orientado a eventos) com **I/O não-bloqueante**, que contrasta fundamentalmente com o modelo bloqueante de Java e Python.

## B.1 Echo Server

```javascript
// echo_server.js — Echo Server em Node.js
const net = require('node:net');

const PORTA = 5555;

const server = net.createServer((socket) => {
    console.log(`Conexão recebida de ${socket.remoteAddress}:${socket.remotePort}`);

    // Evento 'data': disparado quando o cliente envia dados
    socket.on('data', (data) => {
        const msg = data.toString().trim();
        console.log(`Recebido: ${msg}`);
        socket.write(msg + '\n'); // Echo: devolve a mesma mensagem
        console.log(`Echo enviado: ${msg}`);
    });

    // Evento 'end': disparado quando o cliente encerra a conexão
    socket.on('end', () => {
        console.log('Cliente desconectado');
    });

    // Evento 'error': trata erros de conexão
    socket.on('error', (err) => {
        console.log(`Erro: ${err.message}`);
    });
});

server.listen(PORTA, () => {
    console.log(`Servidor escutando na porta ${PORTA}...`);
});
```

**Execução:**
```bash
node echo_server.js
# Em outro terminal:
telnet localhost 5555
```

## B.2 Echo Client

```javascript
// echo_client.js — Echo Client em Node.js
const net = require('node:net');
const readline = require('node:readline');

const PORTA = 5555;
const HOST = 'localhost';

const client = net.createConnection({ port: PORTA, host: HOST }, () => {
    console.log('Conectado ao servidor');
    
    // Lê do terminal
    const rl = readline.createInterface({ input: process.stdin });
    process.stdout.write('$> ');
    
    rl.on('line', (mensagem) => {
        client.write(mensagem + '\r\n');
    });
});

// Recebe resposta do servidor
client.on('data', (data) => {
    console.log(`Servidor respondeu: ${data.toString().trim()}`);
    client.end(); // Fecha conexão após primeira resposta
});

client.on('end', () => {
    console.log('Comunicação encerrada');
    process.exit(0);
});
```

## B.3 Servidor com Protocolo de Calculadora

```javascript
// calc_server.js — Servidor com protocolo de calculadora
const net = require('node:net');

const PORTA = 5555;

function processarComando(linha) {
    linha = linha.trim();
    
    if (linha.toUpperCase() === 'SAIR') {
        return 'BYE';
    }
    
    const partes = linha.split(' ');
    
    if (partes.length !== 4 || partes[0].toUpperCase() !== 'CALC') {
        return 'ERROR Comando desconhecido';
    }
    
    const operacao = partes[1].toUpperCase();
    const a = parseFloat(partes[2]);
    const b = parseFloat(partes[3]);
    
    if (isNaN(a) || isNaN(b)) {
        return 'ERROR Operandos inválidos';
    }
    
    switch (operacao) {
        case 'ADD': return `RESULT ${a + b}`;
        case 'SUB': return `RESULT ${a - b}`;
        case 'MUL': return `RESULT ${a * b}`;
        case 'DIV':
            if (b === 0) return 'ERROR Divisão por zero';
            return `RESULT ${a / b}`;
        default:
            return `ERROR Operação desconhecida: ${operacao}`;
    }
}

const server = net.createServer((socket) => {
    const clientId = `${socket.remoteAddress}:${socket.remotePort}`;
    console.log(`[${clientId}] Conectado`);
    
    let buffer = '';
    
    socket.on('data', (data) => {
        buffer += data.toString();
        
        // Processa linha por linha (protocolo delimitado por \n)
        let indice;
        while ((indice = buffer.indexOf('\n')) !== -1) {
            const linha = buffer.substring(0, indice).trim();
            buffer = buffer.substring(indice + 1);
            
            if (!linha) continue;
            
            console.log(`[${clientId}] Recebido: ${linha}`);
            const resposta = processarComando(linha);
            socket.write(resposta + '\n');
            console.log(`[${clientId}] Enviado: ${resposta}`);
            
            if (linha.toUpperCase() === 'SAIR') {
                socket.end();
                return;
            }
        }
    });
    
    socket.on('end', () => {
        console.log(`[${clientId}] Desconectado`);
    });
    
    socket.on('error', (err) => {
        console.log(`[${clientId}] Erro: ${err.message}`);
    });
});

server.listen(PORTA, () => {
    console.log(`Servidor calculadora na porta ${PORTA}`);
});
```

## B.4 Diferenças-Chave: Java vs Node.js

| Aspecto | Java | Node.js |
|---------|------|---------|
| **Modelo de I/O** | Bloqueante (threads) | Não-bloqueante (event loop) |
| **Concorrência** | `Thread` / `ExecutorService` | Event-driven nativo (single-threaded) |
| **accept()** | Método bloqueante explícito | Callback em `createServer()` |
| **readLine()** | Bloqueante | Evento `'data'` (assíncrono) |
| **Threads para N clientes** | Necessário criar N threads | Não precisa — event loop cuida |
| **Complexidade** | Mais código, mais explícito | Menos código, mais implícito |

### B.5 O Modelo Event-Driven

A grande diferença conceitual é que Node.js **nunca bloqueia**:

```
Java (Thread por cliente):               Node.js (Event Loop):

Thread-1: │ accept │ read █ write █│     Event Loop:
Thread-2: │ accept │ read █ write █│     │ accept(A) │ data(A) │ accept(B) │
Thread-3: │ accept │ read █ write █│     │ data(B) │ data(A) │ ...

3 threads para 3 clientes                1 thread para TODOS os clientes
```

Em Node.js, o `createServer()` registra um callback que é chamado automaticamente para cada nova conexão. Dentro desse callback, `socket.on('data', ...)` registra outro callback para quando dados chegam. O event loop alterna entre todos os clientes sem precisar de threads adicionais.

> **Quando o event loop não é suficiente:** Para operações intensivas de CPU (cálculos pesados, criptografia), Node.js pode travar o event loop. Nestes casos, usa-se `worker_threads` ou delega-se para serviços externos.
