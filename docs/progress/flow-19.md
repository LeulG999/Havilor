# Flow 19: Manual intake

**Goal:** an analyst has a suspicious email that no source system sent in. She gives Havilor what she has (pasted
text, raw headers, an `.eml` file, a screenshot or a PDF); Claude extracts a **draft** alert; she reviews it and sends
it to the normal `POST /api/v1/alerts` with source `MANUAL`.
**New idea:** **multipart uploads** and Claude's **vision** (images and PDFs), feeding the existing create flow
instead of a second one.

**Built on:** `main` · **Status:** done, 140 tests passing, pushed

## The API

```bash
# one part only: "text" (pasted email / raw headers) OR "file" (.eml, PNG/JPEG/GIF/WebP, PDF; max 10 MB)
curl -X POST http://localhost:8080/api/v1/alerts/manual/extract -H "Authorization: Bearer $TOKEN" -F "file=@reported.eml"
```

```json
→ 200 (a CreateAlertRequest, nothing saved)
{"ticketNumber": null, "source": "MANUAL", "caseType": "USER_REPORTED_EMAIL", "priority": "MEDIUM",
 "receivedAt": "2026-10-31T09:15:00Z",
 "email": {"subject": "Invoice overdue", "fromAddress": "billing@invoices.example.net", "spfResult": "FAIL", ...},
 "recipients": [{"address": "alex@corp.example.com", "delivery": "DELIVERED"}],
 "urls": [{"address": "https://login.fake-bank.example.net/verify", ...}], "attachments": [...], "clicks": []}

POST /api/v1/alerts   (the draft, edited or not)   → 201, ticketNumber "MAN-20261108-1" (then -2, -3 ...)
```

**400:** no part, both parts, an unsupported file type, text over 200,000 characters; a non-MANUAL alert without a
ticket number. **502:** the assistant can't answer.

```mermaid
sequenceDiagram
    participant An as Analyst
    participant IC as AlertIntakeController
    participant IS as AlertIntakeService
    participant AI as AiClient (Claude, vision)
    participant AC as AlertController → AlertService.create (unchanged flow)
    An->>IC: POST /alerts/manual/extract  (text | .eml | image | PDF)
    IC->>IS: extract(text, file)
    IS->>IS: exactly one part? which kind?  → AiInput (text, or file + media type)
    IS->>AI: structured(instructions, input, ExtractedEmail.class)
    AI-->>IS: ExtractedEmail (Optional = "not shown")
    IS-->>An: draft CreateAlertRequest (source MANUAL, no ticket)
    An->>An: review / edit
    An->>AC: POST /alerts (draft)
    AC->>AC: no ticket + MANUAL → MAN-<yyyyMMdd>-<n>; then the normal create (checks, findings, playbook)
```

## Files

| File | What it does |
|---|---|
| `db/migration/V15__manual_source.sql` (new) | Adds `MANUAL` to the `ck_alert_source` CHECK. |
| `alert/domain/enums/Source.java` | `MANUAL`. |
| `alert/api/request/CreateAlertRequest.java` | `ticketNumber` no longer `@NotBlank` (still `@Size(max = 50)`): it may be missing for MANUAL. |
| `alert/service/AlertService.java` | `ticketNumberFor`: the sent ticket, or `MAN-<yyyyMMdd>-<count+1>` for MANUAL, or **400** `ticketNumber: must not be blank`. |
| `alert/service/AlertFactory.java` | `create(request, ticketNumber)`. |
| `alert/repository/AlertRepository.java` | `countByTicketNumberStartingWith`. |
| `alert/service/ExtractedEmail.java` (new) | The structured-output type for extraction. `Optional` fields → `"type or null"` in the schema. |
| `alert/service/AlertIntakeService.java` (new) | Picks the input kind (text / `.eml` as text / image / PDF), the extraction prompt, and maps `ExtractedEmail` → draft `CreateAlertRequest`. |
| `alert/api/AlertIntakeController.java` (new) | `POST /api/v1/alerts/manual/extract`, `multipart/form-data`. |
| `application.yml` | `spring.servlet.multipart` max 10 MB (the 1 MB default is too small for screenshots and PDFs). |
| `test/.../alert/AlertIntakeIntegrationTest.java` (new) | Text → draft → POST twice → `MAN-20261101-1`, `-2`; `.eml` read as text; PNG and PDF passed as files; 400s; non-MANUAL without ticket → 400. |
| `test/.../common/ai/AnthropicAiClientTest.java` | The `ExtractedEmail` schema builds, with nullable fields. |
| `test/.../common/ErrorHandlingIntegrationTest.java` | `ticketNumber` is no longer a Bean Validation error (8 fields instead of 9). |

## Decisions

- **One create flow.** Extraction returns a `CreateAlertRequest` and saves nothing; the analyst posts it to the
  existing endpoint, so manual alerts get the same validation, automatic findings, critical rule and playbook.
- **A small extraction type instead of `CreateAlertRequest` itself.** The schema derived from `CreateAlertRequest`
  makes every field required and non-null, which would push Claude to invent values (scores, headers, dates).
  `ExtractedEmail` holds only what an email shows, with `Optional` for "not shown"; the service maps it.
- **Fixed defaults for the rest:** `source = MANUAL`, `caseType = USER_REPORTED_EMAIL`, `priority = MEDIUM`,
  `delivery = DELIVERED`, no ticket, no clicks. The analyst can change them in the draft.
- **`.eml` is sent as text** (it is text); images and PDFs go as base64 blocks for vision. The type is taken from
  the part's content type (and the `.eml` extension).
- **Ticket numbers:** `MAN-<date>-<n>` where `n` = existing MAN tickets of that day + 1 (date from the app clock, UTC).
  Two manual alerts created at the same instant would collide on the unique ticket number: the second gets **409**
  and can be sent again. Simple, and correct (no duplicate rows).
- **"Ticket required" moved from Bean Validation to the service**, because it depends on another field (`source`).
- **Prompt injection:** the extraction prompt also says the input is data, never instructions.

## How to verify

1. `./mvnw test` → 140 tests, 0 failures (fake AiClient).
2. With `ANTHROPIC_API_KEY` set: upload a fake `.eml` or a screenshot of a fake email to `/api/v1/alerts/manual/extract`,
   then POST the draft to `/api/v1/alerts` → 201 with a `MAN-...` ticket and `source: MANUAL`.

## New concepts introduced

- **Multipart file uploads** in Spring MVC (`MultipartFile`, size limits).
- **Vision input**: images and PDFs as base64 content blocks.
- **Schema design for extraction**: nullable fields so the model can say "unknown" instead of guessing.
- **Cross-field validation** in the service when one field's rule depends on another.
