# Flow 10: Finish Slice 1

**Goal:** close out the core loop. That means four things:
- **click events**, the last Slice 1 table;
- a **fake alert generator** that only exists in the `dev` profile;
- **Testcontainers**, so tests never depend on the dev database;
- a **Postman collection** that runs the full loop.

**New idea:** test and environment infrastructure: Spring profiles, Testcontainers `@ServiceConnection`, and
contract-style API tests in Postman.

**Built on:** `main` · **Status:** done, **86 tests** passing (Testcontainers), Postman loop 23 requests / 25 assertions green, pushed

## 1. Click events

```json
"clicks": [
  {"recipientAddress": "alex@corp.example.com", "urlAddress": "https://login.fake-bank.example.net/verify",
   "clickedAt": "2026-10-28T14:03:00Z", "permitted": true}
]
```

- A click is **one recipient on one URL of the same alert** (ERD `click_event`: `url_id`, `recipient_id`,
  `clicked_at`, `permitted`).
- In `POST /alerts` the children don't have ids yet, so a click **names them by address**. The address must match
  an entry in the same request's `recipients` / `urls`. Email addresses match case-insensitively; URLs match exactly.
  A miss is a **400** with `fieldErrors[{field: "clicks[0].recipientAddress", message: "is not one of the recipients"}]`,
  through a new `common/BadRequestException`. Each field is valid on its own; it's the combination that is wrong,
  which Bean Validation can't express on a single field.
- **`Url` owns its clicks** (`cascade ALL`, `orphanRemoval`). `Url.addClick` refuses a recipient from another
  alert. The DB cascades from both sides.
- **The detail view reads clicks in one extra query** (`findClicksByAlertId`, join-fetching recipient and URL,
  ordered by `clicked_at`), so detail is now **4 queries** however many children. Going through each URL's lazy
  `clicks` list would be N+1 (one query per URL). The list is hidden (`@Getter(NONE)`), and the only accessor,
  `clicksOfNewUrl()`, is used just on an alert built in the same request, where the list is plain memory.

## 2. Fake alert generator (`dev` profile)

`POST /api/v1/dev/generate-alerts?count=N` (1–100, default 10) → **201** `[{id, ticketNumber, caseType, subject}]`.

- `@Profile("dev")` on the controller: without the profile **the bean doesn't exist**, so the URL is a 404 (tested).
  Run with `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`.
- It calls **`AlertService.create`**, the same path as a real alert, so generated alerts get the playbook,
  validation-sized fields and idempotency like any other.
- **Realistic but fake** (`FakeAlertFactory`):
  - Subjects: invoice, password-expiry and "unusual sign-in" lures.
  - Senders sometimes have a mismatched envelope sender or a suspicious Reply-To.
  - Mixed SPF/DKIM/DMARC results and scores.
  - 1–4 recipients, 0–3 URLs, 0–2 attachments (`invoice_1234.pdf.exe`, macro `.docm`), and clicks.
  - Case types are 70% detected phishing.
- **Safety rule as a test:** `FakeAlertFactoryTest` generates 200 alerts with a fixed seed. It checks that every
  address and URL host is under `example.com` / `example.net` / `example.org` (reserved by RFC 2606, never a real
  organisation), and that every generated request passes the API's Bean Validation.
- `@Min(1) @Max(100)` on `count` → Spring's method validation throws `HandlerMethodValidationException`, which the
  global handler now maps to 400 with `fieldErrors[{field: "count", ...}]`.

## 3. Testcontainers

- `TestcontainersConfiguration` (test-only): a `PostgreSQLContainer("postgres:18")` bean with **`@ServiceConnection`**.
  Spring Boot points the datasource (and Flyway) at the container and ignores `application.yml`'s URL. Every DB test
  class has `@Import(TestcontainersConfiguration.class)`.
- **The container is a static field**, so all Spring test contexts in one run share it: started once, migrated once,
  about 2 s of startup. The `testcontainers/ryuk` sidecar removes it when the JVM exits.
- **Verified independent of the dev DB:** with `docker compose stop` (the `havilor-db` container down), `./mvnw test`
  still passes all 86 tests.
- Dependencies: `spring-boot-testcontainers` + `org.testcontainers:testcontainers-postgresql` (Testcontainers **2.0.5**,
  managed by Boot 4.1.2). In 2.x the class moved to `org.testcontainers.postgresql.PostgreSQLContainer` and is no
  longer generic (checked in the jar).
- Tests keep their `INC-IT-` cleanup. The container is shared across test classes, so tests still mustn't assume an
  empty table (the list tests keep using year-2999 dates).

## 4. Postman collection

[`docs/postman/havilor.postman_collection.json`](../postman/havilor.postman_collection.json), with variables
`baseUrl`, `alertId`, `stepIds`, `stepIndex`...

| Folder | Requests |
|---|---|
| Full loop | 1 create (201) → 2 resend (200, same id) → 3 list → 4 detail → 5 assign (INVESTIGATING) → 6 steps (8 PENDING) → 7 close too early (409) → **8 work next step, repeated 8× via `pm.execution.setNextRequest`** → 9 close MALICIOUS (200) → 10 PATCH closed (409) |
| Playbooks | list, detail (8 steps) |
| Errors | invalid body (400, ApiError keys), unknown id (404), close with UNDECIDED (400) |
| Dev | generate 10 (needs the dev profile) |

Every request has test scripts. **Ran with Newman** against the real app (dev profile): 23 requests,
25 assertions, 0 failures.

## Commits

| Commit | Message |
|---|---|
| `ddd8f95` | test: run database tests on Testcontainers PostgreSQL |
| `a2a830f` | feat(alert): add click events between recipients and urls |
| `d321c18` | feat(alert): accept clicks in POST /alerts and return them on the detail view |
| `870ccfe` | test(alert): cover click events and the four-query detail load |
| `3f85fee` | feat(dev): add fake alert generator under the dev profile |
| `96150c9` | test(dev): cover the generator, its fake-only data and the dev-only endpoint |
| `6533bdb` | docs(postman): add Slice 1 collection with the full alert loop |

## Files created / changed

| File | What it does |
|---|---|
| `db/migration/V8__click_event.sql` (new) | `click_event` with FKs to `url` and `recipient` (cascade) and an index on each. |
| `alert/domain/ClickEvent.java` (new) | The entity; created only through `Url.addClick`. |
| `alert/domain/Url.java` | Owns `clicks`; `addClick` (same-alert check); `clicksOfNewUrl`. |
| `alert/domain/Alert.java` | `clicksOfNewAlert()` for the create response. |
| `alert/api/request/ClickRequest.java`, `ClickResponse.java` (new) | The click DTOs (by address in, with id out). |
| `alert/api/request/CreateAlertRequest.java` | `clicks` (max 1000); `toAlert()` resolves addresses and throws `BadRequestException` on a miss. |
| `alert/repository/AlertRepository.java` | `findClicksByAlertId` (the 4th detail query). |
| `alert/api/response/AlertDetailResponse.java`, `AlertService.java` | `clicks` in the detail; `detailOf(alert)` adds the clicks query. |
| `common/error/BadRequestException.java` (new), `common/error/GlobalExceptionHandler.java` | 400 for cross-field problems, 400 for `HandlerMethodValidationException`. |
| `alert/dev/DevAlertController.java`, `alert/dev/FakeAlertFactory.java` (new) | The generator. |
| `pom.xml` | `spring-boot-testcontainers`, `testcontainers-postgresql` (test scope). |
| `test/.../TestcontainersConfiguration.java` (new) + every DB test | Shared PostgreSQL container via `@ServiceConnection`. |
| `test/.../alert/AlertChildrenIntegrationTest.java` | Click round trip and ordering; click naming an unknown recipient → 400; detail = **4** queries with 1 or 25 of each child (25 clicks too). |
| `test/.../alert/dev/FakeAlertFactoryTest.java`, `DevAlertControllerIntegrationTest.java` (new) | Generated data is valid and fake-only; endpoint works with `dev` (5 alerts, recipients, playbook steps), `count=500` → 400, 404 without the profile (`@Nested` classes, one context each). |
| `docs/postman/havilor.postman_collection.json` (new) | The collection above. |
| `README.md` | Slice 1 complete, full endpoint table, how to run tests/app/generator/Postman. |

## How to verify

1. Docker Desktop running → `./mvnw test` → **86 tests, 0 failures** (no `docker compose` needed). Surefire's console
   summary under-counts the two `@Nested` classes; the XML reports list all 86.
2. `docker compose up -d`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`, then
   `curl -X POST "localhost:8080/api/v1/dev/generate-alerts?count=20"` and `curl "localhost:8080/api/v1/alerts?size=5"`.
3. `npx newman run docs/postman/havilor.postman_collection.json` → 0 failed (or use Postman's Collection Runner).

## New concepts introduced

- **Spring profiles** (`@Profile("dev")`) to keep dev-only code out of other environments.
- **Testcontainers + `@ServiceConnection`**: real database in tests, no shared state with the dev DB, a singleton container across contexts.
- **Cross-field validation** in code (`BadRequestException`) vs per-field Bean Validation.
- **Referencing not-yet-persisted children by natural key** (address) inside one request.
- **Method parameter validation** (`@Min/@Max` on `@RequestParam`) and `HandlerMethodValidationException`.
- **Seeded randomness in tests** for repeatable property-style checks.
- **API tests in Postman/Newman** with chained variables and a request loop (`setNextRequest`).

## Known limits / next

- The generator's `count` runs in one request, so 100 alerts take about 1–2 s. That's fine for demos; a real load
  generator would batch.
- The dev database now holds a few fake `INC-PM-` / `INC-GEN-` alerts from the Postman run. They're harmless, and
  `docker compose down -v` resets everything.
- Slice 1 is complete. Next batch: Slice 2 (automatic checks, Critical rule, findings, teaching text).
