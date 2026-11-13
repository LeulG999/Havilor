# Flow F28: Clearer manual intake

**Goal:** the analyst knows what to bring and gets the best draft from it: she sees which sources work best (and how
to get them), drops several files at once (e.g. the .eml plus a screenshot) with or without pasted text, and the
AI reads everything together. In the draft she sees what's missing and can add more without starting over.
**New idea:** **several inputs in one AI call** (Claude combines them), and **guidance computed from the result**
(empty fields → concrete next steps).

**Built on:** `main` · **Status:** done, 187 backend tests pass (2 new), `npm run build` passes, checked against a running
backend, pushed

## How it works

```mermaid
sequenceDiagram
    participant U as Analyst
    participant P as NewAlertPage (keeps text + files)
    participant E as ExtractStep (cards · drop zone · text · checklist)
    participant B as AlertIntakeService
    participant AI as AiClient (Claude)
    U->>E: drops .eml + screenshot, pastes headers
    E->>B: POST /api/v1/alerts/manual/extract (multipart: text + files ×≤5; .txt files join the text)
    B->>B: per file: .eml → text section · image/PDF → file block · other → 400 (before any AI call)
    B->>AI: ONE call: "Everything below is about one reported email" + sections + file blocks
    AI-->>B: ExtractedEmail → draft (nothing saved)
    B-->>P: draft
    P->>U: MissingHints ("What's missing") + DraftForm
    U->>P: "Add more and extract again" → step 1 with the same text and files
```

## Files

### Backend

| File | What it does |
|---|---|
| `common/ai/AiInput.java` | `files: List<AiFile(data, mediaType)>` instead of one file; `AiInput.files(text, files)`. |
| `common/ai/AnthropicAiClient.java` | One image/PDF block per file, all before the text, in one request. |
| `alert/service/AlertIntakeService.java` | `extract(text, files)`: text, up to 5 files, or both. Every .eml becomes a named text section, every image/PDF a file block with a line naming it. Per-file type check (400 names the file); more than 5 → 400; nothing → 400. Instructions: several pieces may describe the same email; trust the .eml/headers over a screenshot for addresses, dates and checks. |
| `alert/api/AlertIntakeController.java` | `files` (repeated multipart field) instead of `file`. |
| `application.yml` | `max-request-size: 20MB` (each file still at most 10 MB). |
| `test/.../alert/AlertIntakeIntegrationTest.java` | Text + .eml + PNG + PDF → exactly one AI call with all parts; 6 files → 400; one bad file among good ones → 400 and no AI call; existing cases on the `files` field. |
| `test/.../common/ai/AnthropicAiClientTest.java` | Two files → image block, document block, then text. |
| `test/.../FakeAiConfiguration.java` | Counts calls. |

### Front end

| File | What it does |
|---|---|
| `src/components/SourceCards.tsx` (new) | Email file (.eml, "Best", ●●●), Raw headers (●●○), Screenshot or PDF (●○○), Pasted text (●○○): what each gives; "How do I get this?" (`<details>`) for .eml and headers with the steps from the brief. |
| `src/components/DropZone.tsx` (new) | Drag and drop or click/Enter to choose several files; list with sizes and Remove; checks at most 5 and at most 10 MB each before sending. |
| `src/components/ExtractStep.tsx` | Cards, drop zone, text box, checklist (Required: subject and sender · Very helpful: headers, links, attachments, recipients), one request. |
| `src/components/MissingHints.tsx` (new) | From the draft: no subject/sender, no SPF/DKIM/DMARC ("Add the raw headers for a stronger assessment"), no recipients, no links, no attachments; "Add more and extract again". |
| `src/pages/NewAlertPage.tsx` | Keeps text and files across both steps; "Start over" clears them. |
| `src/index.css` | Source cards, quality dots, drop zone, file list, checklist, missing box. |

## Checked against a running backend

Text + an .eml + a PNG in one request → 502 "The AI assistant is not configured" (it reached the AI step; no key on
this machine); 6 files → 400 "send at most 5 files"; a zip → 400 "a.zip: must be an .eml file, an image … or a PDF".
Step 1 screenshot compared with the design. Step 2 needs a real AI answer, so the hints were checked in code only.

## Decisions

- **One AI call for everything,** so Claude can combine the pieces (e.g. links from the screenshot, checks from the
  headers) instead of merging separate drafts in code.
- **.txt files are read in the browser and join the text**, as before (they're headers or pasted text).
- **The field is now `files`** (repeated); the old single `file` field is gone, and the page was updated with it.
- **Request limit 20 MB** for several files; each file keeps its 10 MB limit and type check.
- **Hints come from empty fields only**: nothing is guessed; "Nothing obvious is missing" when all are present.
- Only .eml and headers have "How do I get this?" (the brief gives steps for those two).

## How to verify

New alert: four cards (the .eml one marked Best), open "How do I get this?"; drop two files (or click the zone) → both
listed, remove one; paste headers; "Extract the alert" (needs `ANTHROPIC_API_KEY`). In step 2: the "What's missing"
box lists what wasn't found; "Add more and extract again" → step 1 with the same files and text.

## New concepts introduced

- **Multiple files in one multipart request** (`List<MultipartFile>`, `FormData.append` several times).
- **Drag and drop** with `onDragOver` / `onDrop` and `dataTransfer.files`.
- **Native disclosure widgets** (`<details>` / `<summary>`).
- **Lifting state up** so it survives a step change.
