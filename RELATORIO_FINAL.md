# Relatório Final — Trabalho II: MicroSUS
## Servidor HTTP de Fila de Pronto-Socorro

**Disciplina:** Sistemas Distribuídos I  
**Professor:** Elder Francisco Fontana Bernardi  
**Curso:** Bacharelado em Ciência da Computação — IFSul, Campus Passo Fundo  
**Aluna:** Helen Zanco Neis  
**Data:** Junho de 2026  
**Repositório:** https://github.com/Helenzn/MicroSUS_SocketServer

---

## 1. Introdução

Este relatório documenta o desenvolvimento do trabalho **MicroSUS**, cujo objetivo foi implementar um servidor HTTP funcional construído inteiramente sobre sockets TCP em Java, sem o uso de nenhum framework HTTP pronto. O sistema simula uma fila de triagem de pronto-socorro, com gerenciamento de prioridades e uma máquina de estados por paciente.

O problema foi escolhido por ser uma metáfora direta de desafios reais em sistemas distribuídos: comunicação cliente-servidor via HTTP sobre TCP, estado compartilhado e mutável entre múltiplas requisições, máquina de estados para garantir consistência de recursos, e roteamento de requisições para diferentes lógicas de processamento.

---

## 2. Arquitetura do Sistema

O projeto foi estruturado em camadas bem definidas, seguindo os princípios de separação de responsabilidades:

```
┌─────────────────────────────────────┐
│           Cliente (Postman/curl)     │
└──────────────┬──────────────────────┘
               │ HTTP/1.1 sobre TCP
┌──────────────▼──────────────────────┐
│          ServerV2.java              │
│  (loop de accept + thread por req)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          HttpParser.java            │
│  (parse de request line, headers,   │
│   body; montagem de respostas)      │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       PacienteController.java       │
│  (roteamento método + path)         │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        PacienteService.java         │
│  (fila de prioridade, máquina de    │
│   estados, lógica de negócio)       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│     Paciente.java / PacienteRequest │
│     (modelo de dados + DTOs)        │
└─────────────────────────────────────┘
```

### 2.1 Componentes Principais

**ServerV2.java** — Núcleo do servidor. Abre um `ServerSocket` e entra em loop aguardando conexões. Para cada conexão aceita, delega o processamento para o `HttpParser`, garantindo que o servidor esteja sempre pronto para receber a próxima requisição.

**HttpParser.java** — Responsável pelo parsing manual do protocolo HTTP/1.1. Lê a stream de bytes do socket, extrai a *request line* (método, URI, versão), processa os headers linha a linha até encontrar a linha em branco (`\r\n\r\n`), e lê o body usando o valor do header `Content-Length` — conforme exige o protocolo.

**PacienteController.java** — Implementa o roteador da aplicação. Mapeia combinações de método HTTP + path para as funções de tratamento correspondentes. Rotas inexistentes retornam `404 Not Found`.

**PacienteService.java** — Camada de serviço que encapsula toda a lógica de negócio: gerenciamento da fila de prioridade, validação de transições de estado, geração de IDs sequenciais, e registro de timestamps.

**Paciente.java / PacienteRequest.java** — Modelo de domínio e DTO (Data Transfer Object) de entrada. O `Gson` é utilizado para serializar e desserializar os objetos para/de JSON.

---

## 3. Protocolo HTTP/1.1 — Implementação Manual

Um dos aspectos centrais do trabalho foi implementar o parsing HTTP sem auxílio de bibliotecas. A sequência de leitura segue o padrão definido na RFC 7230:

```
1. Ler a request line:    GET /fila HTTP/1.1\r\n
2. Ler headers:           Host: localhost\r\n
                          Content-Type: application/json\r\n
                          Content-Length: 71\r\n
                          \r\n        ← linha em branco = fim dos headers
3. Ler o body:            (lidos exatamente Content-Length bytes do stream)
```

**Ponto crítico:** para o body de requisições POST, foi necessário usar `read(char[], offset, length)` lendo exatamente `Content-Length` bytes — e não `readLine()`, que bloquearia esperando uma nova linha que pode nunca chegar em um body JSON.

As respostas HTTP são montadas com o formato correto:

```
HTTP/1.1 201 Created\r\n
Content-Type: application/json\r\n
Content-Length: 142\r\n
\r\n
{"id":1,"nome":"Maria Silva",...}
```

---

## 4. Máquina de Estados

Cada paciente possui um estado que evolui conforme o fluxo de atendimento. O sistema rejeita qualquer transição inválida com `409 Conflict` e uma mensagem descritiva.

```
  POST /pacientes           POST /chamar          POST /{id}/finalizar
       │                        │                         │
    (novo) ──────────► EM_FILA ──────────► EM_ATENDIMENTO ──────────► ATENDIDO
                                                                   (estado terminal)
```

### Transições Inválidas (→ 409 Conflict)

| Tentativa | Mensagem Retornada |
|---|---|
| Finalizar paciente `EM_FILA` | "Paciente está EM_FILA. Não é possível finalizar sem antes chamar para atendimento." |
| Finalizar paciente já `ATENDIDO` | "Paciente já está ATENDIDO. Não há transição possível." |
| Chamar paciente `ATENDIDO` | "Paciente já saiu do fluxo de atendimento." |
| Chamar paciente `EM_ATENDIMENTO` | "Paciente já está sendo atendido." |

---

## 5. Fila de Prioridade

A fila respeita a seguinte ordem de atendimento:

1. **Vermelho** (emergência) — atendimento imediato
2. **Amarelo** (urgência) — atendimento prioritário
3. **Verde** (baixa complexidade) — ordem de chegada

Dentro de cada nível de prioridade, aplica-se FIFO (primeiro a chegar, primeiro a ser chamado), usando o campo `horaChegada` como critério de desempate.

O endpoint `POST /chamar` seleciona automaticamente o paciente correto do topo da fila conforme esta ordenação, sem que o cliente precise especificar quem chamar.

---

## 6. Rotas Implementadas

| Método | Rota | Descrição | Status de Sucesso |
|---|---|---|---|
| POST | `/pacientes` | Cadastra paciente (→ EM_FILA) | 201 Created |
| GET | `/pacientes` | Lista todos os pacientes | 200 OK |
| GET | `/pacientes/{id}` | Consulta paciente por ID | 200 OK |
| PUT | `/pacientes/{id}` | Atualiza dados do paciente | 200 OK |
| DELETE | `/pacientes/{id}` | Remove paciente | 200 OK |
| GET | `/fila` | Fila ordenada por prioridade (HTML) | 200 OK |
| POST | `/chamar` | Chama próximo da fila (→ EM_ATENDIMENTO) | 200 OK |
| POST | `/pacientes/{id}/finalizar` | Finaliza atendimento (→ ATENDIDO) | 200 OK |
| GET | `/estatisticas` | Painel de estatísticas | 200 OK |

---

## 7. Evidências de Funcionamento

### Checkpoint 1 (03/06/2026)

Foi demonstrado:
- Servidor aceitando conexão TCP e iniciando o protocolo
- Parse correto da request line e de todos os headers (Host, User-Agent, Accept, Connection, etc.)
- `GET /fila` respondendo com HTML estático (`200 OK`, `Content-Type: text/html`)
- `POST /pacientes` cadastrando paciente com retorno `201 Created` em JSON

Trecho do terminal no CP1:
```
Esperando conexao...
Conexao recebida, inciando protocolo...
Recebido: GET /fila HTTP/1.1
Recebido: Host: localhost
Recebido: User-Agent: curl/8.18.0
Recebido: Accept: */*
Requisição completa recebida.
Enviando resposta:
HTTP/1.1 200 OK
Content-Type: text/html
Content-Length: 132
```

### Checkpoint 2 (10/06/2026)

Foi demonstrado via PowerShell e Postman:
- `POST /pacientes` com body JSON completo retornando `201 Created` com todos os campos (id, nome, sintoma, prioridade, estado, horaChegada)
- `GET /pacientes` listando pacientes cadastrados
- `POST /chamar` selecionando paciente conforme prioridade
- Máquina de estados funcionando (transições válidas e inválidas)

Exemplo de resposta do `POST /pacientes`:
```json
{
  "id": 1,
  "nome": "Maria Silva",
  "sintoma": "dor no peito",
  "prioridade": "vermelho",
  "estado": "EM_FILA",
  "horaChegada": "2026-06-23T23:12:15.829845"
}
```

Exemplo testado no Postman (`GET /pacientes`):
```json
[
  {
    "id": 3,
    "nome": "teste",
    "sintoma": "febre",
    "prioridade": "verde",
    "estado": "EM_FILA",
    "horaChegada": "24/06/2026 08:15"
  }
]
```

---

## 8. Dificuldades Encontradas

**Leitura do body no POST:** A maior dificuldade técnica foi garantir a leitura correta do body. O uso de `readLine()` bloqueava a thread aguardando um `\n` após o JSON. A solução foi extrair o `Content-Length` dos headers e usar `read(char[], offset, length)` para ler exatamente essa quantidade de bytes.

**NullPointerException no request line:** Em algumas situações (conexões de health-check do navegador, ou conexões fechadas prematuramente), o socket chegava com a primeira linha nula. O erro `Cannot invoke "String.split(String)" because "<local5>" is null` foi tratado com verificação prévia antes do parse.

**Codificação de caracteres:** Requisições vindas de browsers em Windows incluíam headers extras (como `Sec-Fetch-*`, `Accept-Encoding`, `Accept-Language`) que precisavam ser ignorados sem interromper o parsing.

---

## 9. Comandos Utilizados

```bash
# Compilar
javac -cp "lib/gson-2.13.1.jar" -d bin src/server/*.java src/model/*.java src/service/*.java src/controller/*.java

# Executar servidor
java -cp "bin;lib/gson-2.13.1.jar" server.ServerV2

# Limpar bytecodes (PowerShell)
Get-ChildItem -Recurse -Filter *.class | Remove-Item

# Testar via curl (GET)
curl http://localhost/fila

# Testar via curl (POST)
curl -X POST http://localhost/pacientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","sintoma":"dor no peito","prioridade":"vermelho"}'

# Testar via PowerShell
Invoke-WebRequest `
  -Uri http://localhost/pacientes `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"nome":"Maria Silva","sintoma":"dor no peito","prioridade":"vermelho"}'
```

---

## 10. Conclusão

O desenvolvimento do MicroSUS proporcionou uma compreensão prática e profunda de como os protocolos de comunicação funcionam na camada mais baixa. Implementar manualmente o parsing HTTP — algo que frameworks como Spring ou Flask abstraem completamente — evidencia o que acontece "por baixo" em toda comunicação web.

Os principais aprendizados foram: o formato e a semântica do protocolo HTTP/1.1, o uso correto de sockets TCP para comunicação cliente-servidor, o design de uma máquina de estados para garantir consistência de recursos, e a estruturação de um servidor em camadas (server → parser → controller → service → model) que facilita a manutenção e a extensão do código.

O projeto cumpriu todos os requisitos do enunciado: 9 rotas funcionais, máquina de estados com validação de transições, fila de prioridade com ordenação correta, e respostas HTTP válidas com status codes, Content-Type e Content-Length corretos. Os dois checkpoints foram entregues dentro dos prazos com evidências demonstráveis.

---

*Relatório gerado para entrega final da disciplina Sistemas Distribuídos I — IFSul, Campus Passo Fundo, 2026/1.*
