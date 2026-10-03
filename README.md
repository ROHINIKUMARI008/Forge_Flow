# ForgeFlow

Backend workflow engine split into two processes. **workflow-service** accepts HTTP calls, stores the job in **PostgreSQL**, and publishes to **RabbitMQ** after commit. **execution-worker** consumes the queue and updates job status. **Groq** turns an English prompt into a job inside the API process.

This is a **backend-only** project. The client is Postman or curl.

## Layout

| Module | Process | Role |
| --- | --- | --- |
| `forgeflow-common` | library, not a server | `JobEntity`, `WorkflowJobMessage`, RabbitMQ exchange/queue |
| `workflow-service` | port 8080 | REST, Flyway, Groq, publish |
| `execution-worker` | no HTTP port | consume, retry, mark completed or failed |

## Stack

Java 21, Spring Boot 4, RabbitMQ, PostgreSQL, Flyway, Docker Compose, Groq (`openai/gpt-oss-20b`).

## Run locally

1. Docker Desktop running.
2. Start broker + database:

```powershell
docker compose up -d postgres rabbitmq redis
```

3. Start the API (Flyway runs here):

```powershell
.\mvnw.cmd -pl workflow-service -am spring-boot:run
```

4. In a second terminal, start the worker:

```powershell
.\mvnw.cmd -pl execution-worker -am spring-boot:run
```

Wait for `Started WorkflowServiceApplication` and `Started ExecutionWorkerApplication`.

Set the Groq key in the environment when you call `/generate`. Do not commit the key.

```powershell
$env:GROQ_API_KEY="gsk_your_key"
```

RabbitMQ UI: http://localhost:15672 (`forgeflow` / `forgeflow`).

## APIs (Postman)

Base: `http://localhost:8080`

| Method | URL                              | Body                                                   |
| ------ | -------------------------------- | ------------------------------------------------------ |
| GET    | `/hello`                         | —                                                      |
| POST   | `/api/v1/workflows/execute`      | `{"workflowName":"welcome-email","payload":"user=42"}` |
| GET    | `/api/v1/workflows/jobs/{jobId}` | —                                                      |
| POST   | `/api/v1/workflows/generate`     | `{"prompt":"send welcome email to user 42"}`           |

Retry demo: execute with `"payload":"FORCE_FAIL"` until status `FAILED` and `attemptCount` is 3. The worker process logs the retries.

## Docker (both services)

```powershell
$env:GROQ_API_KEY="gsk_your_key"
docker compose up --build
```

The worker starts after `/hello` on the API is healthy, so Flyway has already created the tables.

## License

Student / portfolio project.
