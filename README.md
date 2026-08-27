# ForgeFlow

Backend workflow engine: HTTP APIs enqueue jobs, **RabbitMQ** workers run them, **PostgreSQL** stores status, **Groq** turns a short English prompt into a job.

This is a **backend-only** project. The client is Postman or curl. A UI is optional later and is not required for a resume.

## Stack

Java 21, Spring Boot 4, RabbitMQ, PostgreSQL, Flyway, Docker Compose, Groq (`openai/gpt-oss-20b`).

## Run locally

1. Docker Desktop running.
2. Copy config and add your Groq key (never commit the key):

```text
copy src\main\resources\application.properties.example src\main\resources\application.properties
```

Set `forgeflow.ai.groq.api-key=gsk_...` in `application.properties`.

3. Start broker + database:

```powershell
cd ForgeFlow
docker compose up -d
```

4. Start the API:

```powershell
.\mvnw.cmd spring-boot:run
```

Wait for `Started ForgeFlowApplication`.

RabbitMQ UI: http://localhost:15672 (`forgeflow` / `forgeflow`).

## APIs (Postman)

Base: `http://localhost:8080`

| Method | URL | Body |
|---|---|---|
| GET | `/hello` | — |
| POST | `/api/v1/workflows/execute` | `{"workflowName":"welcome-email","payload":"user=42"}` |
| GET | `/api/v1/workflows/jobs/{jobId}` | — |
| POST | `/api/v1/workflows/generate` | `{"prompt":"send welcome email to user 42"}` |

Retry demo: execute with `"payload":"FORCE_FAIL"` until status `FAILED` and `attemptCount` is 3.

## Docker (full stack)

```powershell
$env:GROQ_API_KEY="gsk_your_key"
docker compose --profile full up --build
```

## Planned (not in this repo yet)

AWS deploy (EC2 or App Runner + RDS Postgres + RabbitMQ or Amazon MQ). Add that to the resume only after it is live.

## License

Student / portfolio project.
