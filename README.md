# 🏥 MicroSUS — Servidor HTTP de Fila de Pronto-Socorro

> Trabalho Final — Sistemas Distribuídos I | IFSul, Campus Passo Fundo  
> Curso: Bacharelado em Ciência da Computação  
> Professora: Elder Francisco Fontana Bernardi  
> Aluna: Helen Zanco Neis

---

## 📋 Sobre o Projeto

O **MicroSUS** é um servidor HTTP construído **do zero sobre sockets TCP em Java**, sem o uso de nenhum framework HTTP pronto (sem Spring, sem servlets). Ele simula um sistema de triagem e fila de pronto-socorro com prioridade, implementando um CRUD completo de pacientes e uma máquina de estados que garante a consistência do fluxo de atendimento.

O objetivo central é demonstrar, na prática, os fundamentos de comunicação de sistemas distribuídos: parsing manual de requisições HTTP, roteamento, gerenciamento de estado compartilhado e respostas HTTP válidas — tudo sobre raw sockets.

---

## ✨ Funcionalidades

- ✅ Servidor TCP que aceita conexões e parseia requisições HTTP/1.1 manualmente
- ✅ Parse completo de request line, headers e body
- ✅ Roteamento por método + path
- ✅ **POST /pacientes** — Cadastro de paciente com geração automática de ID e timestamp
- ✅ **GET /fila** — Visualização da fila ordenada por prioridade (renderiza HTML no navegador)
- ✅ **GET /pacientes/{id}** — Consulta individual de paciente por ID
- ✅ **GET /pacientes** — Listagem completa de pacientes
- ✅ **PUT /pacientes/{id}** — Atualização de dados do paciente
- ✅ **DELETE /pacientes/{id}** — Remoção de paciente
- ✅ **POST /chamar** — Chama o próximo da fila (transição EM_FILA → EM_ATENDIMENTO)
- ✅ **POST /pacientes/{id}/finalizar** — Finaliza atendimento (transição EM_ATENDIMENTO → ATENDIDO)
- ✅ **GET /estatisticas** — Painel de estatísticas do sistema
- ✅ Máquina de estados com validação de transições (retorna 409 Conflict para transições inválidas)
- ✅ Fila de prioridade: vermelho > amarelo > verde, com FIFO dentro da mesma prioridade
- ✅ Integração com Gson para serialização/desserialização de JSON

---

## 🗂️ Estrutura do Projeto

```
MicroSUS_SocketServer/
├── src/
│   ├── Main.java                  # Ponto de entrada
│   ├── controller/
│   │   └── PacienteController.java  # Roteamento e handlers das rotas
│   ├── model/
│   │   ├── Paciente.java            # Entidade Paciente
│   │   └── PacienteRequest.java     # DTO de entrada (POST/PUT)
│   ├── server/
│   │   ├── ServerV2.java            # Loop principal do servidor TCP
│   │   └── HttpParser.java          # Parser manual HTTP/1.1
│   └── service/
│       └── PacienteService.java     # Lógica de negócio e fila de prioridade
├── client/
│   └── Client.java                # Cliente TCP simples (testes)
├── lib/
│   └── gson-2.13.1.jar            # Biblioteca Gson
├── bin/                           # Bytecode compilado (.class)
├── tests/
│   └── requests.http              # Arquivo de testes REST Client
└── postman/
    └── collections/               # Coleção Postman para testes
```

---

## 🔄 Máquina de Estados

Cada paciente percorre o seguinte ciclo de estados:

```
POST /pacientes          POST /chamar         POST /pacientes/{id}/finalizar
     │                       │                           │
  (novo) ──────────► EM_FILA ──────────► EM_ATENDIMENTO ──────────► ATENDIDO
```

**Transições inválidas retornam `409 Conflict`:**

| Tentativa | Motivo |
|---|---|
| Finalizar paciente `EM_FILA` | Atendimento ainda não foi iniciado |
| Finalizar paciente já `ATENDIDO` | Estado terminal — sem saída |
| Chamar paciente `ATENDIDO` | Paciente já saiu do fluxo |
| Chamar paciente `EM_ATENDIMENTO` | Paciente já está sendo atendido |

---

## 🏷️ Modelo de Dados — Paciente

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | int | Gerado automaticamente pelo servidor (sequencial) |
| `nome` | String | Nome completo do paciente |
| `sintoma` | String | Descrição breve do sintoma |
| `prioridade` | String | `"vermelho"`, `"amarelo"` ou `"verde"` |
| `estado` | String | `"EM_FILA"`, `"EM_ATENDIMENTO"` ou `"ATENDIDO"` |
| `horaChegada` | String | Timestamp ISO 8601 do registro |
| `prognostico` | String | Preenchido ao finalizar o atendimento |

---

## 🚀 Como Executar

### Pré-requisitos

- Java JDK 11+
- Gson 2.13.1 (já incluído em `lib/`)

### Compilar

```bash
javac -cp "lib/gson-2.13.1.jar" -d bin src/server/*.java src/model/*.java src/service/*.java src/controller/*.java
```

### Iniciar o servidor

```bash
java -cp "bin;lib/gson-2.13.1.jar" server.ServerV2
```

> O servidor escuta por padrão na porta **80** (localhost).

### Limpar os .class compilados

```powershell
Get-ChildItem -Recurse -Filter *.class | Remove-Item
```

---

## 🧪 Testando as Rotas

### Via Navegador (GET)

```
http://localhost/fila
http://localhost/pacientes
http://localhost/estatisticas
```

### Via curl

```bash
# Cadastrar paciente
curl -X POST http://localhost/pacientes \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Silva","sintoma":"dor no peito","prioridade":"vermelho"}'

# Visualizar fila
curl http://localhost/fila

# Chamar próximo paciente
curl -X POST http://localhost/chamar

# Finalizar atendimento (substitua {id} pelo ID do paciente)
curl -X POST http://localhost/pacientes/1/finalizar \
  -H "Content-Type: application/json" \
  -d '{"prognostico":"Paciente estável, encaminhado para observação."}'

# Estatísticas
curl http://localhost/estatisticas
```

### Via PowerShell (Invoke-WebRequest)

```powershell
Invoke-WebRequest `
  -Uri http://localhost/pacientes `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"nome":"Maria Silva","sintoma":"dor no peito","prioridade":"vermelho"}'
```

---

## 📦 Rotas Disponíveis

| Método | Rota | Descrição | Resposta |
|---|---|---|---|
| `POST` | `/pacientes` | Cadastra paciente (→ EM_FILA) | `201 Created` JSON |
| `GET` | `/pacientes` | Lista todos os pacientes | `200 OK` JSON |
| `GET` | `/pacientes/{id}` | Consulta paciente por ID | `200 OK` JSON |
| `PUT` | `/pacientes/{id}` | Atualiza dados do paciente | `200 OK` JSON |
| `DELETE` | `/pacientes/{id}` | Remove paciente | `200 OK` |
| `GET` | `/fila` | Fila ordenada por prioridade | `200 OK` HTML |
| `POST` | `/chamar` | Chama próximo da fila (→ EM_ATENDIMENTO) | `200 OK` JSON |
| `POST` | `/pacientes/{id}/finalizar` | Finaliza atendimento (→ ATENDIDO) | `200 OK` JSON |
| `GET` | `/estatisticas` | Painel de estatísticas | `200 OK` JSON |

---

## 🛠️ Tecnologias Utilizadas

- **Java** (sockets TCP brutos — `ServerSocket` / `Socket`)
- **HTTP/1.1** (parsing manual — sem frameworks)
- **Gson 2.13.1** (serialização/desserialização JSON)
- **Postman** (testes de API)
- **VS Code** com REST Client

---

## 📝 Checkpoints

| Checkpoint | Data | Status | Entrega |
|---|---|---|---|
| CP1 | 03/06/2026 | ✅ Concluído | Servidor aceita conexão, parseia HTTP, GET /fila com HTML estático, POST /pacientes funcional |
| CP2 | 10/06/2026 | ✅ Concluído | Todas as rotas funcionais, máquina de estados implementada e validada |
| Apresentação | 21/06/2026 | ✅ Concluído | Demonstração individual com todas as rotas e máquina de estados |

---

## 📄 Licença

Projeto acadêmico desenvolvido para a disciplina de Sistemas Distribuídos I — IFSul, Campus Passo Fundo, 2026/1.
