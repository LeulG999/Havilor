# Flow 22: Performance review

**Goal:** check every list/detail endpoint of finding, action, escalation, audit and standup for N+1 queries and
missing indexes, and fix only the real problems found, with evidence.
**New idea:** measure first (SQL statement counts, query plans), then change.

**Built on:** `main` · **Status:** done, 159 tests passing, pushed

## How it was checked

1. **N+1:** `QueryCountIntegrationTest` (new, stays as a regression guard) creates one alert with **5** findings,
   actions, escalations, audit lines and standup notes, then counts the SQL statements of each endpoint with
   Hibernate statistics. An N+1 shows as a count that grows with the rows.
2. **Indexes:** every repository query was listed with its `WHERE`, then compared with the indexes in the migrations.
   The cost of a missing index was measured in a throwaway `pgvector/pgvector:pg18` container with 300,000 synthetic
   rows (50,000 alerts × 6), with `EXPLAIN (ANALYZE)`.

## What was found

| Endpoint | Statements (5 rows) | Query filter | Index before | Verdict |
|---|---|---|---|---|
| `GET /alerts/{id}/findings` | 1 | `finding.alert_id = ?` | **none** | missing index |
| `GET /alerts/{id}/actions` | 2 (alert + list) | `action.alert_id = ?` | **none** | missing index |
| `GET /alerts/{id}/escalations` | 2 (exists + list) | `escalation.alert_id = ?` | **none** | missing index |
| `GET /alerts/{id}/audit` | 1 | `entity_type = ? AND entity_id = ?` | **none** | missing index |
| `GET /audit` (manager, paged) | 2 (page + count) | `ORDER BY id DESC` | primary key | OK |
| `GET /standup/notes`, `/standup/team` | **7** (page + count + 1 per note) | `author_id = ?` / `status = ?` | none | **N+1**; indexes not needed (see below) |
| alert list / detail / steps (re-checked) | 1–2 / 5 / 2 | — | yes (Flows 2, 5, 8) | OK |

Query plan for `SELECT * FROM finding WHERE alert_id = 4242 ORDER BY id` on 300,000 rows (the audit lookup looked the same):

```
no index:   Parallel Seq Scan on finding   Rows Removed by Filter: 149997 (x2)   Buffers: 2703   Execution Time: 10.353 ms
with index: Index Scan using idx_finding_alert_id   Index Cond: (alert_id = 4242)   Buffers: 12   Execution Time: 0.074 ms
```

A full scan reads every page of the table, and those tables grow with every alert (the audit log fastest), so the cost
grows forever; the index lookup stays the same size.

## What was changed (and why)

| Change | Why |
|---|---|
| `db/migration/V17__lookup_indexes.sql` (new): `idx_finding_alert_id`, `idx_action_alert_id`, `idx_escalation_alert_id`, `idx_audit_entry_entity (entity_type, entity_id)` | The measured full scans. Same rule the project already follows for alert children (V4, V7, V8): index the foreign key you filter on. They also make `ON DELETE CASCADE` from `alert` cheap. |
| `standup/domain/StandupNote.java`: `@BatchSize(size = 100)` on `alertIds` | The N+1: building each `NoteResponse` loaded that note's mentioned alerts with its own query. Now the whole page loads them in one `WHERE note_id IN (...)` query: **7 → 3** statements, and 3 for any page size (100 = the page maximum). |
| `test/.../QueryCountIntegrationTest.java` (new) | Locks in the counts above, so a future N+1 fails the build. |

## What was checked and deliberately not changed

- **`standup_note (author_id)` / `(status)`:** a few notes per analyst per day means thousands of rows after years;
  a scan of that is trivial, and `status` has only two values. No index until there is a measured need.
- **`standup_note_alert (alert_id)`:** only used when an alert is deleted (cascade), which the app never does.
- **`alert.assignee_id`:** nothing filters by it yet.
- **`countByTicketNumberStartingWith('MAN-…')`:** a `LIKE 'prefix%'` can't use the default unique index, but manual
  alerts are rare and the count runs once per manual alert.
- **Audit/manager page `COUNT(*)`:** a full count per page request; acceptable for a manager-only screen today.

## How to verify

1. `./mvnw test` → 159 tests, 0 failures (includes `QueryCountIntegrationTest`).
2. `docker exec havilor-db psql -U havilor -d havilor -c "\d finding"` lists `idx_finding_alert_id`.

## New concepts introduced

- **The N+1 query problem** and batch fetching (`@BatchSize`) as the fix for lazy collections on paged lists
  (a fetch join would make Hibernate page in memory).
- **Reading `EXPLAIN (ANALYZE)`**: sequential vs index scans, buffers, execution time.
- **Indexing foreign keys** in PostgreSQL (it does not do it automatically).
- **Measure, then optimise:** keep the evidence and a regression test.
