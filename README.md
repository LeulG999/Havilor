# Havilor

An AI-assisted security investigation and analyst-guidance tool, built as a learning project.

The MVP handles **phishing / email cases**. Havilor helps a SOC analyst work a case faster, and it teaches the
concepts along the way: every investigation step explains *why it matters*. Havilor can *suggest* a verdict,
but the analyst always makes the decision.

> **Status:** Slice 1 (the core loop) is complete. Nothing here is production-ready yet (no login until Slice 4).
> All data in this project is fake/synthetic. No real emails, tenants, or credentials.

## Current status: Slice 1 (core loop) ✅

- [x] 0. Foundation: app starts, PostgreSQL in Docker, Flyway, module folders
- [x] 1. Save one alert and read it back (`POST /alerts`, `GET /alerts/{id}`), idempotent on `ticketNumber`
- [x] 2. List alerts with paging (`GET /alerts`), newest first
- [x] 3. Handle mistakes: validation (400), not found (404), conflict (409), one error format
- [x] 4. Grow the alert: full fields, embedded email details, enums
- [x] 5. Alert children: recipients, URLs, attachments
- [x] 6. Update an alert (`PATCH /alerts/{id}`) with business rules and optimistic locking
- [x] 7. Playbook module: seeded phishing playbook with teaching steps (`GET /playbooks`, `GET /playbooks/{id}`)
- [x] 8. Playbook steps on an alert (`GET/PATCH /alerts/{id}/steps`), attached through the playbook service
- [x] 9. Close an alert (`POST /alerts/{id}/close`): verdict required, no pending steps
- [x] 10. Finish the slice: click events, fake alert generator, Testcontainers tests, Postman collection

Build notes for every flow are in [`docs/progress/`](docs/progress).

## Tech stack

- Java 21, Spring Boot 4.1, Maven
- Spring Web, Spring Data JPA (Hibernate 7), Bean Validation, Lombok
- PostgreSQL 18 (in Docker), Flyway migrations
- Tests: JUnit 5, Mockito, MockMvc, **Testcontainers** (a throwaway PostgreSQL per test run), **ArchUnit** (module borders)

Planned: JaCoCo, OpenAPI/Swagger, Spring Security + JWT, Redis, the Claude API (Anthropic) as the LLM client,
pgvector (RAG), GitHub Actions, AWS, and a React + TypeScript front end.

## Architecture

Havilor is a **modular monolith**: one Spring Boot application split into feature modules.
Today `alert`, `playbook` and `common` hold code; `user`, `finding`, `escalation`, `audit` and `standup` are
waiting for their slices.

- Each module is layered: **Controller → Service → Repository → PostgreSQL**, with one sub-package per layer.
- Modules have strict borders: another module may use only a module's `service` and `api.response` packages,
  never its `domain` or `repository`. An **ArchUnit** test (`ModuleBorderTest`) fails the build otherwise.
  For example, `alert` gets playbooks only through `PlaybookService`.
- Business rules live on the entities (`Alert.assignTo`, `Alert.close`...), not in setters spread over services.
- The REST API lives under `/api/v1` and only returns DTOs, never database entities.
- Every error goes through one `@RestControllerAdvice` and has one shape.
- "Now" comes from one injected `java.time.Clock` (tests use a fixed one), never `Instant.now()`.
- Request DTOs are plain data; `AlertFactory` turns a request into an alert with its children.

```
alert/
  api/            controllers           api/request/   request DTOs     api/response/  response DTOs
  domain/         Alert (+ embedded EmailDetails), Recipient, Url, Attachment, ClickEvent, StepProgress
  domain/enums/   AlertStatus, Priority, Source, Verdict, AuthResult, DeliveryStatus, StepStatus
  repository/     AlertRepository       service/       AlertService, AlertStepService, AlertFactory
  dev/            fake alert generator (only with the "dev" profile)
playbook/
  api/  api/response/  domain/ (Playbook, PlaybookStep: seeded, immutable, versioned)  repository/  service/
common/           CaseType, ClockConfiguration (the one Clock for "now")
  error/          ApiError, GlobalExceptionHandler, NotFound/Conflict/BadRequest exceptions
  paging/         PageResponse, Paging
```

## API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/v1/alerts` | Create an alert with its email details, recipients, URLs, attachments and clicks. **201** + `Location` for a new `ticketNumber`; **200** with the stored alert if that ticket already exists (idempotent). Phishing alerts get the active playbook's steps |
| `GET` | `/api/v1/alerts?page=0&size=20` | **200** with a page of alert *summaries*, newest first: `{content, page, size, totalElements, totalPages}`. `size` defaults to 20, maximum 100 |
| `GET` | `/api/v1/alerts/{id}` | **200** with the full alert: email details, recipients, URLs, attachments, clicks, playbook and `version` (4 queries, however many children); **404** if unknown |
| `PATCH` | `/api/v1/alerts/{id}` | Change `priority`, `assigneeId`, `waitingOn`, `externalCaseNumber`, `followUpOn` (only the fields sent). Assigning a `NEW` alert makes it `INVESTIGATING`. **409** if the alert is closed or the sent `version` is stale |
| `GET` | `/api/v1/alerts/{id}/steps` | **200** with the alert's playbook steps in order: what to check, *why it matters*, how, and the alert's progress (`PENDING`/`DONE`/`SKIPPED`) |
| `PATCH` | `/api/v1/alerts/{id}/steps/{stepId}` | Record a step `{status, answer}`. Working a step on a `NEW` alert makes it `INVESTIGATING`. **409** if the alert is closed |
| `POST` | `/api/v1/alerts/{id}/close` | Close with `{verdict}` (required, not `UNDECIDED`, else **400**). **409** if already closed or any step is still `PENDING` |
| `GET` | `/api/v1/playbooks` | **200** with every playbook (name, case type, version, active) |
| `GET` | `/api/v1/playbooks/{id}` | **200** with the playbook and its steps in order; **404** if unknown |
| `POST` | `/api/v1/dev/generate-alerts?count=10` | **dev profile only**: creates 1–100 realistic fake alerts (example.com/.net/.org only). **404** without the profile |

Every error uses one body: `{status, error, message, path, timestamp, fieldErrors}`. Invalid input gives **400**
(one entry per broken field), an unknown id **404**, a broken business rule or a concurrent change **409**, and
anything unexpected **500** without internal details.

```bash
curl -i -X POST http://localhost:8080/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '{"ticketNumber":"INC-DEMO-0001","source":"TEST_GENERATOR","caseType":"DETECTED_PHISHING","priority":"HIGH",
       "receivedAt":"2026-10-28T14:00:00Z",
       "email":{"subject":"Fake invoice overdue","fromAddress":"billing@invoices.example.net","spfResult":"FAIL"},
       "recipients":[{"address":"alex@corp.example.com","delivery":"DELIVERED"}],
       "urls":[{"address":"https://login.fake-bank.example.net/verify"}],
       "clicks":[{"recipientAddress":"alex@corp.example.com","urlAddress":"https://login.fake-bank.example.net/verify",
                  "clickedAt":"2026-10-28T14:03:00Z","permitted":true}]}'
```

## Run locally

You need **JDK 21+** and **Docker Desktop** (running).

```bash
# Tests: start their own PostgreSQL with Testcontainers (Docker must be running; no compose needed)
./mvnw test

# The app: start the dev database (container "havilor-db", host port 5433), then the app on :8080
docker compose up -d
./mvnw spring-boot:run

# ...or with the fake alert generator enabled
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
curl -X POST "http://localhost:8080/api/v1/dev/generate-alerts?count=20"

# Stop the database when you are done (data is kept in a Docker volume)
docker compose down
```

The database uses host port **5433**, not the default 5432, so it doesn't clash with a locally installed
PostgreSQL. The credentials in `compose.yaml` and `application.yml` are for local development only.

### The full loop in Postman

Import [`docs/postman/havilor.postman_collection.json`](docs/postman/havilor.postman_collection.json) and run
the **Full loop** folder with the Collection Runner. It creates an alert, sends it again (idempotent), assigns it,
tries to close too early (409), works all 8 playbook steps, closes it, and checks that a closed alert can't change.
Every request has its own tests. From the command line: `npx newman run docs/postman/havilor.postman_collection.json`.

## Roadmap

- **Slice 1 (done):** core loop: receive, list, view, update and close alerts; playbook steps
- **Slice 2:** automatic checks, priority rules, findings, teaching text
- **Slice 3:** AI explanations, case summary + suggested verdict, email drafts, then RAG
- **Slice 4:** login and roles (JWT), escalation, actions, audit log, standup notes
- **Later:** ServiceNow integration via MuleSoft, identity investigation (Entra ID sign-ins),
  React front end, Docker / CI / AWS deployment
