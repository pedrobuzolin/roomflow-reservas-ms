# reservas-ms

Microsserviço de reservas do [Roomflow](https://github.com/pedrobuzolin/roomflow-microsservicos), um sistema de reserva de salas. Para rodar o conjunto completo, use o repositório central.

- **Porta:** 8084
- **Banco:** PostgreSQL `roomflow_reservas`, gerenciado com Flyway
- **Stack:** Java 26, Spring Boot 4.1, Spring Data JPA, OpenFeign
- **Depende de:** usuarios-ms e salas-ms (chamadas HTTP síncronas)

## Endpoints

Base: `/api/v1/reservas`

| Método | Rota | Descrição | Resposta |
|---|---|---|---|
| GET | `/` | Lista todas as reservas (paginado) | 200 |
| GET | `/ativas` | Lista as reservas ativas (paginado) | 200 |
| GET | `/{id}` | Busca reserva por id | 200, 404 |
| GET | `/periodo?dataInicio=&dataFim=` | Reservas que cruzam o período | 200, 400 |
| GET | `/salas-disponiveis?dataInicio=&dataFim=` | Salas ativas sem reserva ativa no período (paginado) | 200, 400, 503 |
| POST | `/` | Cria reserva (`salaId`, `usuarioId`, `dataInicio`, `dataFim`) | 201, 400, 404, 409, 503 |
| PATCH | `/cancelar/{id}` | Cancela uma reserva futura | 200, 404, 409 |

Datas no formato ISO, por exemplo `2030-01-10T09:00:00`.

## Regras

- **Sem sobreposição:** uma sala não pode ter duas reservas ativas em períodos que se cruzam. A regra é garantida pelo banco (*exclusion constraint* com intervalo semiaberto `[início, fim)`). Reservas consecutivas, como 9h–10h e 10h–11h, são permitidas. Reservas canceladas não bloqueiam a sala.
- **Ao criar**, o serviço consulta o usuarios-ms (404 se o usuário não existe ou está inativo) e o salas-ms (404 se a sala não existe, 400 se está inativa). Se algum deles não responder, a resposta é 503.
- **Cancelar** só é possível antes do início da reserva. Reserva já cancelada, em andamento ou encerrada devolve 409.
- A reserva guarda apenas `salaId` e `usuarioId`, sem chave estrangeira, porque os dados vivem em outros bancos.
- Erros seguem o padrão `ProblemDetail` (RFC 9457).

## Configuração

| Variável | Descrição | Padrão |
|---|---|---|
| `DB_HOST` | Host e porta do PostgreSQL (execução fora do Docker) | — |
| `DB_USER` / `DB_PASSWORD` | Credenciais do banco | — |
| `USUARIOS_URL` | Endereço do usuarios-ms | `http://localhost:8082` |
| `SALAS_URL` | Endereço do salas-ms | `http://localhost:8083` |

Timeouts das chamadas entre serviços: 2s para conexão e 3s para leitura.

## Como rodar isoladamente

Suba antes o usuarios-ms e o salas-ms, cada um pelo seu compose. Em seguida, crie um arquivo `.env` na pasta do serviço (ele não vai para o git) com as credenciais do banco:

```env
DB_USER=roomflow
DB_PASSWORD=troque-esta-senha
```

Depois:

```bash
docker compose up --build
```

O compose sobe a aplicação e o banco (porta 5435 no host) e alcança os outros serviços pelo `host.docker.internal`. Para o conjunto completo na mesma rede, use o compose do repositório central.

## Testes

```bash
./mvnw test
```

Cobrem a entidade, o service (com os clients mockados) e o controller. A sobreposição de horários, que depende do PostgreSQL, será testada com Testcontainers.
