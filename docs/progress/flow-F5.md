# Flow F5: Realistic DMARC in the fake generator (backend fix)

**Goal:** fake alerts never show an impossible combination: DMARC passing while both SPF and DKIM failed.
**New idea:** fake data should follow the real rules, or it teaches the wrong thing.

**Built on:** `main` · **Status:** done, 162 tests passing, pushed

## The rule

DMARC builds on SPF and DKIM: it passes only if at least one of them passed (and matches the From domain). So:

| SPF | DKIM | Generated DMARC |
|---|---|---|
| PASS | any | random (PASS, FAIL or NONE): DMARC can still fail if the passing domain doesn't match |
| any | PASS | random (same reason) |
| not PASS | not PASS | **always FAIL** |

## Files

| File | What it does |
|---|---|
| `alert/dev/FakeAlertFactory.java` | SPF and DKIM are picked first; `dmarcResult(spf, dkim)` returns FAIL when neither passed, otherwise a random result. |
| `test/.../alert/dev/FakeAlertFactoryTest.java` | `dmarcPassesOnlyWhenSpfOrDkimPassed`: over 200 seeded alerts, DMARC is FAIL whenever neither SPF nor DKIM passed; it also checks both cases really occur, so the test can't pass by accident. |

## How to verify

`./mvnw test` → 162 tests, 0 failures. In the dev profile, `POST /api/v1/dev/generate-alerts?count=50`: no alert has
`dmarcResult: PASS` with both `spfResult` and `dkimResult` not `PASS`.
