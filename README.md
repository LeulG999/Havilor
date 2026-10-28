# Havilor

An AI-assisted security investigation and analyst-guidance tool, built as a learning project.

The MVP handles **phishing / email cases**. Havilor helps a SOC analyst work a case faster, and it teaches the
concepts along the way: every investigation step explains *why it matters*. Havilor can *suggest* a verdict,
but the analyst always makes the decision.

> **Status:** early development. Nothing here is production-ready yet.
> All data in this project is fake/synthetic. No real emails, tenants, or credentials.

## Current status: Slice 1 (core loop)

- [x] 0. Foundation: app starts, PostgreSQL in Docker, Flyway, module folders
- [ ] 1. Save one alert and read it back (`POST /alerts`, `GET /alerts/{id}`)
- [ ] 2. List alerts with paging (`GET /alerts`)
- [ ] 3. Handle mistakes: validation (400), not found (404), one error format
- [ ] 4. Grow the alert: full fields, embedded email details, enums
- [ ] 5. Alert children: recipients, URLs, attachments
- [ ] 6. Update an alert (`PATCH /alerts/{id}`) with business rules
- [ ] 7. Playbook module: seeded phishing playbook (`GET /playbooks`)
- [ ] 8. Playbook steps on an alert (`GET/PATCH /alerts/{id}/steps`)
- [ ] 9. Close an alert (`POST /alerts/{id}/close`)
- [ ] 10. Finish the slice: click events, fake alert generator, integration tests

## Tech stack

- Java 21, Spring Boot 4.1, Maven
- Spring Web, Spring Data JPA, Bean Validation, Lombok
- PostgreSQL 18 (in Docker), Flyway migrations

Planned: Testcontainers, OpenAPI/Swagger, Spring Security + JWT, an LLM client, pgvector (RAG),
GitHub Actions, and a React + TypeScript front end.

## Architecture

Havilor is a **modular monolith**: one Spring Boot application split into feature modules
(`alert`, `playbook`, `user`, `finding`, `escalation`, `audit`, `standup`, `common`).

- Each module is layered: **Controller → Service → Repository → PostgreSQL**.
- Modules have strict borders. A module never touches another module's repository. It calls that module's service.
- The REST API lives under `/api/v1` and only returns DTOs, never database entities.

## Run locally

You need **JDK 21+** and **Docker Desktop** (running).

```bash
# 1. Start PostgreSQL (container "havilor-db", host port 5433)
docker compose up -d

# 2. Run the tests (they need the database from step 1)
./mvnw test

# 3. Start the app on http://localhost:8080
./mvnw spring-boot:run

# Stop the database when you are done (data is kept in a Docker volume)
docker compose down
```

The database uses host port **5433**, not the default 5432, so it doesn't clash with a locally installed
PostgreSQL. The credentials in `compose.yaml` and `application.yml` are for local development only.

## Roadmap

- **Slice 1:** core loop: receive, list, view, update and close alerts; playbook steps
- **Slice 2:** automatic checks, priority rules, findings, teaching text
- **Slice 3:** AI explanations, case summary + suggested verdict, email drafts, then RAG
- **Slice 4:** login and roles (JWT), escalation, actions, audit log, standup notes
- **Later:** ServiceNow integration via MuleSoft, identity investigation (Entra ID sign-ins),
  React front end, Docker / CI / cloud deployment
