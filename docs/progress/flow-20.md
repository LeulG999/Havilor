# Flow 20: pgvector + RAG (knowledge module)

**Goal:** Havilor remembers. Playbook steps and closed alerts are stored with an **embedding** (a vector that captures
meaning) in PostgreSQL with **pgvector**; the AI assessment adds the 5 most similar pieces to Claude's prompt
(retrieval-augmented generation) and says which ones it used.
**New idea:** vector similarity search in the database, and RAG.

**Built on:** `main` · **Status:** done, 151 tests passing, pushed

## The API (changed)

```json
POST /api/v1/alerts/42/ai/assessment
→ 200 {"summary": "...", "suggestedVerdict": "MALICIOUS", "suggestionReason": "...",
       "references": [{"sourceType": "CLOSED_ALERT", "sourceId": 17}, {"sourceType": "PLAYBOOK_STEP", "sourceId": 3}, ...]}
```

`references` are the chunks Claude was given (up to 5, most similar first): `CLOSED_ALERT` = an alert id,
`PLAYBOOK_STEP` = a playbook step id. Without `VOYAGE_API_KEY` it is `[]` and everything else works as before.

## How it works

```mermaid
sequenceDiagram
    autonumber
    participant St as KnowledgeStartup (ApplicationRunner)
    participant AS as AlertService.close (@Transactional)
    participant AI as AlertAiService.assess
    participant KS as KnowledgeService
    participant EC as EmbeddingClient → Voyage AI (/v1/embeddings)
    participant DB as knowledge_chunk (pgvector, HNSW index)
    St->>KS: indexPlaybookSteps()
    KS->>DB: which steps are missing?
    KS->>EC: embedDocuments(all missing step texts)  one request
    KS->>DB: INSERT ... embedding ?::vector
    AS->>AS: close, flush, audit
    AS->>KS: indexClosedAlert(id, "subject, sender, findings, verdict, reason")  REQUIRES_NEW, try/catch
    KS->>EC: embedDocuments([text])
    KS->>DB: INSERT (own transaction; a failure is logged, the close still commits)
    AI->>KS: search("subject, sender, findings", 5)
    KS->>EC: embedQuery(text)
    KS->>DB: ORDER BY embedding <=> ?::vector LIMIT 5   (cosine distance)
    KS-->>AI: matches (or [] on any failure)
    AI->>AI: Claude prompt JSON = alert + findings + referenceKnowledge
```

## Files

| File | What it does |
|---|---|
| `compose.yaml`, `test/.../TestcontainersConfiguration.java` | Image `pgvector/pgvector:pg18` (PostgreSQL 18 + the extension; Testcontainers treats it as a compatible `postgres`). |
| `db/migration/V16__knowledge_chunk.sql` (new) | `CREATE EXTENSION vector`; `knowledge_chunk` (source_type CHECK, source_id, content, `embedding vector(1024)`, created_at, UNIQUE(source_type, source_id)); **HNSW** index with `vector_cosine_ops`. |
| `common/ai/EmbeddingClient.java` (new) | `isConfigured`, `embedDocuments(texts)`, `embedQuery(text)`. Interface + one implementation (allowed: tests use a fake). |
| `common/ai/VoyageEmbeddingClient.java` (new) | Voyage AI through Spring `RestClient`: one request per list, `input_type` document/query, 5 s connect / 20 s read timeouts, errors → `AiException`. |
| `application.yml` | `havilor.embedding.api-key: ${VOYAGE_API_KEY:}`, `model: voyage-3.5`. |
| `knowledge/domain/enums/KnowledgeSourceType.java` (new) | `PLAYBOOK_STEP`, `CLOSED_ALERT`. |
| `knowledge/repository/KnowledgeChunkRepository.java` (new) | JdbcTemplate SQL: `exists`, `insert` (`ON CONFLICT DO NOTHING`), `findNearest` (`<=>`). |
| `knowledge/service/KnowledgeService.java` (new) | `indexPlaybookSteps`, `indexClosedAlert` (`REQUIRES_NEW`), `search` (failure → `[]`). Does nothing without a key. |
| `knowledge/service/KnowledgeStartup.java` (new) | Indexes missing playbook steps at startup; a failure is logged, the app still starts. |
| `knowledge/api/response/KnowledgeMatchResponse.java` (new) | A hit: sourceType (String), sourceId, content. |
| `alert/service/AlertService.java` | `close`: builds the closed-alert text and calls `indexClosedAlert` inside try/catch. |
| `alert/service/AiAssessment.java` (new), `alert/api/response/AiAssessmentResponse.java` | Claude's structured answer is now its own record; the API response adds `references`. |
| `alert/service/AlertAiService.java` | `assess`: search with subject + sender + finding titles, top 5 into the prompt as `referenceKnowledge` (labelled as data). |
| `test/.../FakeEmbeddingConfiguration.java` (new) | `@Primary` fake: a bag-of-words vector (texts sharing words are similar); can be told to fail. |
| `test/.../knowledge/KnowledgeIntegrationTest.java` (new) | Steps indexed once at startup; a close indexes the alert; a failing embedding service doesn't block the close; the assessment gets the similar past case first; a failing search → `references: []`. |
| `test/.../knowledge/service/KnowledgeServiceTest.java`, `repository/KnowledgeChunkRepositoryTest.java`, `common/ai/VoyageEmbeddingClientTest.java` (new) | No key → nothing happens; vector text format; Voyage's response shape parses; no key → no call. |
| `test/.../ModuleBorderTest.java` | The border rule for `knowledge`. |

## Decisions

- **Direction:** `alert → knowledge → playbook`. The alert module builds the text of a closed case itself and hands
  it over, so `knowledge` never needs alert or finding classes (no cycle).
- **JdbcTemplate instead of JPA for this table.** `vector` and `<=>` are simplest as SQL; Hibernate would need an
  extra dependency (`hibernate-vector`) for one table.
- **voyage-3.5, 1024 dimensions** (Voyage's default for that model). The number is fixed in the migration: changing
  the model to one with another size needs a new migration and re-indexing.
- **HNSW index with cosine distance** (asked for). With a few hundred chunks a plain scan would also be fine; the
  index costs nothing at this size.
- **Never block closing:** the index write runs in its own transaction (`REQUIRES_NEW`) and `AlertService` catches
  any exception, so neither an API failure nor a database error can roll back the close. The embedding call does run
  while the close transaction is open, so it has timeouts (5 s connect, 20 s read). Trade-off: if the close itself
  failed to commit after indexing (very unlikely: the update and version check already ran at flush), a chunk could
  describe an alert that isn't closed.
- **Search failure = no knowledge**, logged: the assessment still works. Indexing failure at startup: logged, retried
  at the next start (only missing steps are sent).
- **One request for all steps:** Voyage's free tier allows very few requests per minute; one list request avoids 429s.
- **References = what Claude was given**, not a claim about what it relied on.
- **Each source is indexed once** (unique key). A closed alert can't be reopened, so its chunk never goes stale.
- **No key → the app still starts**, nothing is indexed, `references` is `[]`.

## How to verify

1. `./mvnw test` → 151 tests, 0 failures (fake embeddings and fake Claude; no network).
2. With `VOYAGE_API_KEY` set: start the app → log `Indexed 8 playbook steps for knowledge search`; close an alert →
   `SELECT source_type, source_id FROM knowledge_chunk;` shows it; assess a similar alert → it appears in `references`.

## New concepts introduced

- **Embeddings** and **cosine similarity**; document vs query embeddings.
- **pgvector**: the `vector` type, the `<=>` operator, HNSW indexes.
- **RAG**: retrieve similar knowledge, put it in the prompt as data, show the sources.
- **Transaction propagation `REQUIRES_NEW`** to isolate a side effect from the main change.
