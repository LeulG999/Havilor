# Havilor — project guide for Claude

## What Havilor is
An AI-assisted security investigation and analyst-guidance SaaS. The MVP handles **phishing / email cases**.
It helps a SOC analyst work faster **and teaches her the concepts** while she works (every step explains *why it matters*).
Owner: Leul Tsige (GitHub: LeulG999). Java/Spring Boot developer, MSCS student.

## Owner's goals
1. Gain senior-developer knowledge and skill; produce work he is proud of and can explain confidently in interviews.
2. Help a working security analyst do her job efficiently while Havilor teaches her.

## HOW WE WORK (most important section)
Claude writes the code, but the goal is that Leul **understands every line and could write it himself**. For every step ("brick"):
1. **Concept first** — what it is, what problem it solves, where it sits in the architecture. Simple English. Numbered, in the order things happen.
2. **Write the code** for ONE small brick only (a few files max). Never jump ahead, and never build a whole module at once — follow the vertical flow order below.
3. **Walk through it line by line**, in order, in very simple English, as if he knows nothing. Show exactly where each piece lives. No Java-vs-other-language comparisons.
4. **He runs it** (mvn, Postman, or a test) and sees it work.
5. **Check understanding** — 2–3 short questions, plus one small change he makes himself (add a field, a rule, a test case). Review his change.
6. Only when he says he understands: **commit and push** to GitHub (see Git rules). Then a short journal note. Then the next brick.
- Do not move to the next brick until he confirms.
- Keep answers short and focused; explain one concept at a time ("A, B, C, then word, then sentence").
- Diagrams: detailed components and flow, not plain boxes.
- No code during pure design/concept discussions unless he asks.
- YAGNI: keep it simple; just-in-time design before each slice.

## Safety rule
Use **fake/synthetic data only**. Never real employer data, real email addresses, tenants or credentials.

## Stack
- Spring Boot 4.1.2, Java 21 (LTS), Maven (keep mvnw files), `application.yml`
- Dependencies now: Spring Web, Spring Data JPA, PostgreSQL Driver, Validation, Lombok
- Package root: `com.leul.havilor`
- PostgreSQL in Docker
- Planned: Flyway migrations, Testcontainers, JaCoCo, OpenAPI/Swagger, Spring Security + JWT, SSE streaming, LLM client, pgvector (RAG), MuleSoft adapter, GitHub Actions, DigitalOcean, React + TypeScript front end

## Architecture
**Modular monolith**: one Spring Boot app, split into **feature modules** with strict borders; inside each module, **layered**: Controller → Service → Repository → PostgreSQL.
- Border rule: a module never uses another module's Repository; it calls that module's Service.
- Package **by feature**: `alert/`, `playbook/`, `user/`, `finding/`, `escalation/`, `audit/`, `standup/`, `common/` (later `identity/`).
- Every way in passes the Security filter (Slice 4). Alerts arrive via `POST /api/v1/alerts` from the Test generator (now) or MuleSoft (later, translating ServiceNow / Proofpoint TAP).
- Services talk to the LLM only through an AI client (Slice 3).

## Request pattern (from the sequence diagrams)
1. Controller validates the request *shape* → bad = 400.
2. Controller hands off to the Service (no logic in controllers).
3. Service loads via Repository (Optional) → empty = 404.
4. Service checks business rules → broken = 409.
5. Service changes the entity, saves (`@Transactional` on data-changing methods).
6. Service converts to a DTO → Controller returns 200/201 + JSON.
Exceptions go to one global error handler (`@RestControllerAdvice`).

## Domain (class diagram v3, MVP)
User, Alert (with embedded EmailDetails), Recipient, Url, Attachment, ClickEvent, Finding, Action, Escalation, Playbook, PlaybookStep, StepProgress (association class Alert↔PlaybookStep), AuditEntry, StandupNote (mentions Alert, many-to-many).
- Alert → 1 Playbook (stores chosen playbook + version).
- Havilor *suggests* a verdict (`suggestedVerdict`, `suggestionReason`); the analyst decides (`verdict`).
- Enums: Role, Source, CaseType, Priority, AlertStatus (New, Investigating, Escalated, Waiting on external, Closed), Verdict (Undecided, Malicious, Benign, False positive), AuthResult, DeliveryStatus, FindingSource, ActionType, ActionResult, StepStatus, EscalationOutcome, ExternalStatus, NoteStatus.
- ERD: 15 tables (`app_user` because `user` is reserved; `step_progress` has UNIQUE(alert_id, playbook_step_id); `standup_note_alert` join table).

## API conventions
`/api/v1`, JWT Bearer (Slice 4), pagination `page`/`size` (default 20), one error format `{status, error, message, path, timestamp}`, DTOs only (never expose entities).

## Build slices
- **S1 (core loop, current):** receive alert, list, view, update, close; playbook steps. Endpoints:
  POST /alerts · GET /alerts · GET /alerts/{id} · PATCH /alerts/{id} · POST /alerts/{id}/close ·
  GET /alerts/{id}/steps · PATCH /alerts/{id}/steps/{stepId} · GET /playbooks · GET /playbooks/{id}
  Rules: assign → Investigating; closed alert can't change (409); can't close with PENDING steps (409); verdict required to close.
- **S2:** automatic checks, Critical rule, findings, teaching text.
- **S3:** AI explanations, summary + suggested verdict, email drafts, then RAG.
- **S4:** login/roles (JWT), escalation, actions, audit log, standup notes.
- Later: performance, MuleSoft (ServiceNow first), **identity investigation** (Entra ID sign-ins + Conditional Access posture via Microsoft Graph: posture lookup → correlation → per-user baseline), microservices split, React front end, Docker/DigitalOcean/CI.

## Slice 1 coding order — VERTICAL FLOWS, NOT LAYERS
Never build layer by layer (all entities, then all repositories...). Build ONE small flow end to end
(migration → entity → repository → service → DTO → controller → test), see it work, commit, then the next flow.
Each flow adds exactly ONE new idea. Inside a flow, still go one brick at a time with the working loop.
Only add the tables/fields/classes the current flow needs (Flyway: one new migration file per flow).

0. **Foundation** — app starts; PostgreSQL in Docker; application.yml; Flyway added; empty module folders.
1. **Save one alert and read it back** — POST /alerts + GET /alerts/{id}, only 4 fields (ticketNumber, subject, status, receivedAt). Teaches the whole pattern.
2. **List alerts** — GET /alerts with paging. New idea: pagination.
3. **Handle mistakes** — validation (400), not found (404), one error format. New idea: global error handler.
4. **Grow the alert** — remaining fields, embedded EmailDetails, enums. New idea: @Embedded + enum mapping.
5. **Alert children** — Recipient, Url, Attachment arrive with the alert. New idea: one-to-many.
6. **Update an alert** — PATCH /alerts/{id}. New idea: business rules (assign → Investigating; closed → 409).
7. **Playbook module** — seed phishing playbook + steps; GET /playbooks, GET /playbooks/{id}. New idea: second module + seed data.
8. **Steps on an alert** — attach playbook when an alert arrives; GET /alerts/{id}/steps; PATCH /alerts/{id}/steps/{stepId}. New idea: crossing a module border through a Service.
9. **Close an alert** — POST /alerts/{id}/close. New idea: @Transactional with several rules.
10. **Finish the slice** — ClickEvent, fake alert generator, Testcontainers integration tests, full loop in Postman.
Every flow ends with: tests passing → Leul confirms understanding → commit + push.

## Git rules
- One branch per feature (e.g. `feature/alert-entities`); small commits, one per brick.
- Conventional commit messages: `feat(alert): ...`, `fix(...)`, `test(...)`, `docs(...)`, `chore(...)`.
- Commit and push only after Leul confirms he understands the brick. Merge to `main` via PR when a feature is complete.

## Learning journal
Claude Doc "Havilor Learning Journal": https://claude.ai/artifact/AiVxLxhtikjL5hC1C3AeKu
(Roadmap, requirements FR-1..35, NFR-1..25, use cases, class diagram, ERD, API, sequence + architecture diagrams.)
