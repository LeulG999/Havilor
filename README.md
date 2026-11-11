# Havilor

An AI-assisted security investigation and analyst-guidance tool, built as a learning project.

The MVP handles **phishing / email cases**. Havilor helps a SOC analyst work a case faster, and it teaches the
concepts along the way: every investigation step explains *why it matters*. Havilor can *suggest* a verdict,
but the analyst always makes the decision.

> **Status:** Slice 1 (the core loop) and Batch 2 (login, findings, actions, escalation, audit, standup notes,
> AI assistant, manual intake) are complete. Nothing here is production-ready yet.
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

## Batch 2 (Slices 2–4 core) ✅

- [x] 11. Generic 409 message for database conflicts
- [x] 12. Users + login (JWT), `GET /users/me`, real assignees
- [x] 13. Automatic checks → findings, critical rule, manual findings
- [x] 14. Actions on an alert
- [x] 15. Escalation
- [x] 16. Audit log
- [x] 17. Standup notes
- [x] 18. AI assistant (Claude): assessment + email drafts
- [x] 19. Manual intake (text, .eml, headers, image/PDF → draft alert)

## Batch 3a (knowledge, cache, operations) — in progress

- [x] 20. pgvector + RAG: playbook steps and closed alerts as knowledge for the AI assessment
- [x] 21. Redis cache for playbook reads
- [x] 22. Performance review (N+1 queries, indexes)
- [ ] 23. Health check (`/actuator/health`)
- [ ] 24. Docker image + full compose stack
- [ ] 25. CI/CD + DevSecOps (GitHub Actions, CodeQL, gitleaks, Dependabot)

Build notes for every flow are in [`docs/progress/`](docs/progress).

## Tech stack

- Java 21, Spring Boot 4.1, Maven
- Spring Web, Spring Data JPA (Hibernate 7), Bean Validation, Lombok
- Spring Security as an OAuth2 resource server: our own HMAC-signed JWTs, BCrypt passwords
- The Claude API (Anthropic) through the official Java SDK, behind one `AiClient` interface (structured outputs)
- PostgreSQL 18 with **pgvector** (in Docker), Flyway migrations
- Voyage AI embeddings (Spring `RestClient`) for knowledge search (RAG)
- Redis (Spring Cache) for playbook reads
- Tests: JUnit 5, Mockito, MockMvc, **Testcontainers** (a throwaway PostgreSQL per test run), **ArchUnit** (module borders)

Planned: JaCoCo, OpenAPI/Swagger,
pgvector (RAG), GitHub Actions, AWS, and a React + TypeScript front end.

## Architecture

Havilor is a **modular monolith**: one Spring Boot application split into feature modules.
Modules: `alert`, `playbook`, `user`, `finding`, `escalation`, `audit`, `standup`, `knowledge`, plus shared
`common`. Dependencies go one way: `alert → playbook, user, finding, audit, knowledge`;
`escalation → alert, audit`; `finding → audit`; `standup → alert`; `knowledge → playbook`.

- Each module is layered: **Controller → Service → Repository → PostgreSQL**, with one sub-package per layer.
- Modules have strict borders: another module may use only a module's `service` and `api.response` packages,
  never its `domain` or `repository`, and modules never depend on each other in a circle. An **ArchUnit** test
  (`ModuleBorderTest`) fails the build otherwise.
  For example, `alert` gets playbooks only through `PlaybookService`.
- Business rules live on the entities (`Alert.assignTo`, `Alert.close`...), not in setters spread over services.
- The REST API lives under `/api/v1` and only returns DTOs, never database entities. Every call except login
  needs a valid access token (`Authorization: Bearer ...`).
- Every error goes through one `@RestControllerAdvice` and has one shape.
- Every change on an alert writes an audit line (who, what, before, after) in the same transaction.
- "Now" comes from one injected `java.time.Clock` (tests use a fixed one), never `Instant.now()`.
- Request DTOs are plain data; `AlertFactory` turns a request into an alert with its children.
- Simplest correct solution first: plain `@Transactional` services, lazy loading, `@Version` for conflicts.

```
alert/
  api/            controllers           api/request/   request DTOs     api/response/  response DTOs
  domain/         Alert (+ embedded EmailDetails), Recipient, Url, Attachment, ClickEvent, StepProgress, Action
  domain/enums/   AlertStatus, Priority, Source, Verdict, AuthResult, DeliveryStatus, StepStatus
  repository/     AlertRepository       service/       AlertService, AlertQueryService, AlertStepService, AlertFactory
  dev/            fake alert generator (only with the "dev" profile)
playbook/
  api/  api/response/  domain/ (Playbook, PlaybookStep: seeded, immutable, versioned)  repository/  service/
user/
  api/ (login, me)  api/request/  api/response/  domain/ (User, Role)  repository/  service/ (AuthService, UserService)
  dev/            the fake dev analyst and manager (only with the "dev" profile)
finding/
  api/  api/request/  api/response/  domain/ (Finding + enums)  repository/  service/ (FindingService, AutomaticChecks)
escalation/
  api/  api/request/  api/response/  domain/ (Escalation + enums)  repository/  service/ (EscalationService)
audit/
  api/  api/response/  domain/ (AuditEntry, append-only)  repository/  service/ (AuditService.record)
standup/
  api/  api/request/  api/response/  domain/ (StandupNote + NoteStatus)  repository/  service/ (StandupService)
knowledge/
  api/response/  domain/enums/  repository/ (pgvector SQL)  service/ (KnowledgeService, KnowledgeStartup)
common/           CaseType, ClockConfiguration (the one Clock for "now")
  error/          ApiError, GlobalExceptionHandler, NotFound/Conflict/BadRequest exceptions
  paging/         PageResponse, Paging
  security/       SecurityConfiguration (filter chain, JWT encoder/decoder, BCrypt)
  ai/             AiClient (+ AnthropicAiClient), EmbeddingClient (+ VoyageEmbeddingClient), AiInput, AiException
```

## API

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/v1/auth/login` | `{email, password}` → **200** `{accessToken, tokenType: "Bearer", expiresIn}` (8 hours); **401** `Invalid email or password`. The only call without a token |
| `GET` | `/api/v1/users/me` | **200** with the logged-in user (id, name, email, role, active) |
| `POST` | `/api/v1/alerts` | Create an alert with its email details, recipients, URLs, attachments and clicks. **201** + `Location` for a new `ticketNumber`; **200** with the stored alert if that ticket already exists (idempotent); **409** if another request is creating the same ticket at that moment. Phishing alerts get the active playbook's steps. New alerts get their automatic findings; a click on a bad-list URL makes the priority **CRITICAL** |
| `POST` | `/api/v1/alerts/manual/extract` | `multipart/form-data` with ONE part: `text` (pasted email / raw headers) or `file` (.eml, image, PDF, max 10 MB) → **200** a draft `CreateAlertRequest` (source `MANUAL`, nothing saved). POST it to `/api/v1/alerts`: a manual alert without a ticket gets `MAN-<yyyyMMdd>-<n>` |
| `GET` | `/api/v1/alerts?page=0&size=20` | **200** with a page of alert *summaries*, newest first: `{content, page, size, totalElements, totalPages}`. `size` defaults to 20, maximum 100 |
| `GET` | `/api/v1/alerts/{id}` | **200** with the full alert: email details, recipients, URLs, attachments, clicks, playbook and `version`; **404** if unknown |
| `PATCH` | `/api/v1/alerts/{id}` | Change `priority`, `assigneeId`, `waitingOn`, `externalCaseNumber`, `followUpOn` (only the fields sent). The assignee must be an existing user (else **400**). Assigning a `NEW` alert makes it `INVESTIGATING`. **409** if the alert is closed or another request changed it at the same time (`@Version`) |
| `GET` | `/api/v1/alerts/{id}/steps` | **200** with the alert's playbook steps in order: what to check, *why it matters*, how, and the alert's progress (`PENDING`/`DONE`/`SKIPPED`) |
| `PATCH` | `/api/v1/alerts/{id}/steps/{stepId}` | Record a step `{status, answer}`. Working a step on a `NEW` alert makes it `INVESTIGATING`. **409** if the alert is closed |
| `POST` | `/api/v1/alerts/{id}/close` | Close with `{verdict}` (required, not `UNDECIDED`, else **400**). **409** if already closed or any step is still `PENDING` |
| `POST` | `/api/v1/alerts/{id}/actions` | Record what you did `{type, result, note}` (`performedBy` = you, `performedAt` = now) → **201**; **409** if the alert is closed |
| `GET` | `/api/v1/alerts/{id}/actions` | **200** with the alert's actions, oldest first |
| `POST` | `/api/v1/alerts/{id}/escalations` | Hand the alert to another team `{team, reason}` → **201**, alert becomes `ESCALATED`; **409** if closed |
| `GET` | `/api/v1/alerts/{id}/escalations` | **200** with the alert's escalations |
| `PATCH` | `/api/v1/escalations/{id}` | `{externalStatus, outcome}` (both optional). An `outcome` sends the alert back to `INVESTIGATING` |
| `GET` | `/api/v1/alerts/{id}/findings` | **200** with the alert's findings (automatic and manual), each with *why it matters* |
| `POST` | `/api/v1/alerts/{id}/findings` | Add a manual finding `{type, severity, title, teachingText}` → **201** |
| `POST` | `/api/v1/alerts/{id}/ai/assessment` | Claude reads the alert + findings + the 5 most similar knowledge chunks → **200** `{summary, suggestedVerdict, suggestionReason, references}`; the suggestion is saved on the alert (the analyst still decides). **502** if the assistant can't answer |
| `POST` | `/api/v1/alerts/{id}/ai/draft-email` | `{type: USER_NOTICE \| ESCALATION}` → **200** `{type, draft}` (not saved, not sent) |
| `GET` | `/api/v1/alerts/{id}/audit` | **200** with the alert's history (updates, close, steps, actions, findings, escalations), oldest first |
| `GET` | `/api/v1/audit?page=0&size=20` | **MANAGER only** (**403** otherwise): the whole audit log, newest first |
| `POST` | `/api/v1/standup/notes` | Write a standup note `{noteDate, text, alertIds}` → **201** `DRAFT` |
| `PATCH` | `/api/v1/standup/notes/{id}` | Change your own draft (**404** someone else's, **409** once shared) |
| `POST` | `/api/v1/standup/notes/{id}/share` | Share your note with the managers → `SHARED` |
| `GET` | `/api/v1/standup/notes` | Your own notes, newest day first (paged) |
| `GET` | `/api/v1/standup/team` | **MANAGER only**: everybody's shared notes (paged) |
| `GET` | `/api/v1/playbooks` | **200** with every playbook (name, case type, version, active) |
| `GET` | `/api/v1/playbooks/{id}` | **200** with the playbook and its steps in order; **404** if unknown |
| `POST` | `/api/v1/dev/generate-alerts?count=10` | **dev profile only**: creates 1–100 realistic fake alerts (example.com/.net/.org only). **404** without the profile |

Every error uses one body: `{status, error, message, path, timestamp, fieldErrors}`. No or a bad token gives **401**,
a role that may not do this **403**. Invalid input gives **400**
(one entry per broken field), an unknown id **404**, a broken business rule, a concurrent change or a database
conflict **409** (`The request conflicts with existing data`), and
anything unexpected **500** without internal details.

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" \
  -d "{\"email\":\"analyst@havilor.example.com\",\"password\":\"$HAVILOR_DEV_PASSWORD\"}" | jq -r .accessToken)

curl -i -X POST http://localhost:8080/api/v1/alerts \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
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

# Secrets come only from the environment (never from git):
export HAVILOR_JWT_SECRET="<at least 32 random characters>"   # signs access tokens; the app won't start without it
export HAVILOR_DEV_PASSWORD="<any password>"                  # dev profile only: password of the two fake dev users
export ANTHROPIC_API_KEY="<your key>"                         # optional: without it the AI endpoints answer 502
export VOYAGE_API_KEY="<your key>"                            # optional: without it knowledge search finds nothing

# The app: start the dev database (container "havilor-db", host port 5433) and Redis (6379), then the app on :8080
docker compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # dev: creates analyst@ / manager@havilor.example.com

# The fake alert generator (dev profile; needs a token like every other call)
curl -X POST "http://localhost:8080/api/v1/dev/generate-alerts?count=20" -H "Authorization: Bearer $TOKEN"

# Stop the database when you are done (data is kept in a Docker volume)
docker compose down
```

Redis (`havilor-redis`, port 6379) caches playbook reads for 10 minutes. The database uses host port **5433**, not the default 5432, so it doesn't clash with a locally installed
PostgreSQL. The credentials in `compose.yaml` and `application.yml` are for local development only.

### The full loop in Postman

Import [`docs/postman/havilor.postman_collection.json`](docs/postman/havilor.postman_collection.json) and run
the **Full loop** folder with the Collection Runner (app running with the `dev` profile; set the collection
variable `devPassword` to your `HAVILOR_DEV_PASSWORD`). It logs in as the dev analyst, creates an alert, sends it
again (idempotent), assigns it to "me",
tries to close too early (409), works all 8 playbook steps, closes it, and checks that a closed alert can't change.
Every request has its own tests. From the command line:
`npx newman run docs/postman/havilor.postman_collection.json --folder "Full loop" --env-var devPassword=...`.

## Roadmap

- **Slice 1 (done):** core loop: receive, list, view, update and close alerts; playbook steps
- **Slice 2:** automatic checks, priority rules, findings, teaching text (first version in Flow 13)
- **Slice 3:** AI explanations, case summary + suggested verdict, email drafts (Flow 18), RAG (Flow 20)
- **Slice 4:** login and roles (JWT), escalation, actions, audit log, standup notes
- **Later:** ServiceNow integration via MuleSoft, identity investigation (Entra ID sign-ins),
  React front end, Docker / CI / AWS deployment
