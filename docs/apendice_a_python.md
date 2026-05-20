# Apêndice A — Implementações em Python

**Disciplina:** Sistemas Distribuídos I | **Código:** PF_CC.34  
**Professor:** Élder F. F. Bernardi  
**Referências:** Python Socket Programming HOWTO, Real Python Socket Guide  

---

Este apêndice apresenta as implementações equivalentes em Python dos exemplos Java cobertos na apostila principal.

## A.1 Echo Server

```python
#!/usr/bin/env python3
"""Echo Server em Python - Equivalente ao Server.java"""

import socket

HOST = ''        # Aceita conexões de qualquer interface
PORTA = 5555

def main():
    # 1 - Criar o servidor de conexões
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as servidor:
        servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        servidor.bind((HOST, PORTA))
        servidor.listen(5)
        print(f"Servidor escutando na porta {PORTA}...")

        # 2 - Loop de conexões
        while True:
            print("Esperando conexao...")
            conn, endereco = servidor.accept()  # Bloqueante
            print(f"Conexao recebida de {endereco}")

            with conn:
                # 3 - Tratar protocolo
                dados = conn.recv(1024)
                if dados:
                    msg = dados.decode('utf-8').strip()
                    print(f"Recebido: {msg}")
                    conn.sendall(msg.encode('utf-8') + b'\n')
                    print(f"Echo enviado: {msg}")

if __name__ == '__main__':
    main()
```

## A.2 Echo Client

```python
#!/usr/bin/env python3
"""Echo Client em Python - Equivalente ao Client.java"""

import socket

HOST = 'localhost'
PORTA = 5555

def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        # 1 - Conexão
        s.connect((HOST, PORTA))
        
        # 2 - Enviar mensagem
        mensagem = input("$> ")
        s.sendall(mensagem.encode('utf-8') + b'\r\n')
        
        # 3 - Receber resposta
        dados = s.recv(1024)
        print(f"Servidor respondeu: {dados.decode('utf-8').strip()}")

if __name__ == '__main__':
    main()
```

## A.3 Servidor Multithread com Protocolo

```python
#!/usr/bin/env python3
"""Servidor multithread com protocolo de calculadora"""

import socket
import threading

HOST = ''
PORTA = 5555

def processar_comando(linha: str) -> str:
    """Processa um comando do protocolo da calculadora."""
    linha = linha.strip()
    
    if linha.upper() == 'SAIR':
        return 'BYE'
    
    partes = linha.split()
    if len(partes) != 4 or partes[0].upper() != 'CALC':
        return 'ERROR Comando desconhecido'
    
    operacao = partes[1].upper()
    try:
        a = float(partes[2])
        b = float(partes[3])
        
        resultado = {
            'ADD': a + b,
            'SUB': a - b,
            'MUL': a * b,
            'DIV': a / b if b != 0 else None,
        }.get(operacao)
        
        if resultado is None:
            if operacao == 'DIV':
                return 'ERROR Divisão por zero'
            return f'ERROR Operação desconhecida: {operacao}'
        
        return f'RESULT {resultado}'
    except ValueError:
        return 'ERROR Operandos inválidos'

def handle_client(conn: socket.socket, endereco: tuple):
    """Handler para cada cliente em thread separada."""
    nome_thread = threading.current_thread().name
    print(f"[{nome_thread}] Cliente conectado: {endereco}")
    
    with conn:
        arquivo = conn.makefile('r')
        for linha in arquivo:
            linha = linha.strip()
            if not linha:
                continue
            print(f"[{nome_thread}] Recebido: {linha}")
            resposta = processar_comando(linha)
            conn.sendall((resposta + '\n').encode('utf-8'))
            print(f"[{nome_thread}] Enviado: {resposta}")
            if linha.upper() == 'SAIR':
                break
    
    print(f"[{nome_thread}] Cliente {endereco} desconectado")

def main():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as servidor:
        servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        servidor.bind((HOST, PORTA))
        servidor.listen(5)
        print(f"Servidor escutando na porta {PORTA}...")
        
        while True:
            conn, endereco = servidor.accept()
            thread = threading.Thread(
                target=handle_client, 
                args=(conn, endereco)
            )
            thread.start()

if __name__ == '__main__':
    main()
```

## A.4 Diferenças-Chave: Java vs Python

| Aspecto | Java | Python |
|---------|------|--------|
| Criação do socket | `new ServerSocket(porta)` | `socket.socket()` + `bind()` + `listen()` |
| Leitura | `BufferedReader.readLine()` | `conn.recv(1024)` ou `makefile()` |
| Escrita | `PrintWriter.println()` | `conn.sendall(bytes)` |
| Encoding | Implícito (charset padrão) | Explícito (`.encode()` / `.decode()`) |
| Gerenciamento de recursos | `try-finally` ou `try-with-resources` | `with` (context manager) |
| Threads | `new Thread(runnable).start()` | `threading.Thread(target=fn).start()` |
| I/O model | Streams (InputStream/OutputStream) | Bytes (recv/sendall) |

> **Python trabalha com bytes**, não strings. Sempre use `.encode('utf-8')` ao enviar e `.decode('utf-8')` ao receber.
