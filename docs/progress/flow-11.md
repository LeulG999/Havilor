# Flow 11: Generic message for database conflicts

**Goal:** the 409 for a database constraint violation no longer talks about alerts.
**New idea:** an error message in a shared handler must fit every caller, not just the first one.

**Built on:** `main` · **Status:** done, 88 tests passing, pushed

## What changed

| File | Change |
|---|---|
| `common/error/GlobalExceptionHandler.java` | `DataIntegrityViolationException` → **409** `The request conflicts with existing data` (was `Alert with this ticket number already exists`). |
| `test/.../common/ErrorHandlingIntegrationTest.java` | `databaseConstraintGives409` expects the new message (and still checks that the constraint name never leaks). |
| `test/.../alert/AlertControllerIntegrationTest.java` | The concurrent-create test expects the new message on its 409s. |

## Decision

- **Why generic:** `GlobalExceptionHandler` lives in `common/` and catches violations from every module. From Batch 2
  on, more tables have constraints (unique user email, foreign keys), so an alert-specific message would be wrong.
  The handler still never returns the database's own text (it names tables and constraints).

## How to verify

`./mvnw test` → 88 tests, 0 failures. Two simultaneous POSTs of the same new ticket: one 201, the other 200 or
409 `The request conflicts with existing data`.
