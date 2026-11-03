# Refactor: layer sub-packages inside each module

**Goal:** inside `alert/` and `playbook/`, put each layer in its own sub-package, split `common/`, and enforce the
module border with ArchUnit. **No behaviour change**: the same endpoints, JSON, SQL and rules. 89 tests passing:
the 86 from Flow 10, minus the old import-scanning `ModuleBorderTest`, plus 4 ArchUnit rules.

**Commits:** `refactor: organize modules into layer sub-packages` (steps 1–2), then one commit each for steps 3–6 below.

## New layout

```
alert/
  api/            AlertController, AlertStepController
  api/request/    CreateAlertRequest, EmailDetailsRequest, RecipientRequest, UrlRequest, AttachmentRequest,
                  ClickRequest, UpdateAlertRequest, UpdateStepRequest, CloseAlertRequest, DecidedVerdict
  api/response/   AlertDetailResponse, AlertSummaryResponse, EmailDetailsResponse, RecipientResponse, UrlResponse,
                  AttachmentResponse, ClickResponse, AlertStepResponse
  domain/         Alert, EmailDetails, Recipient, Url, Attachment, ClickEvent, StepProgress,
                  AlertStatus, Priority, Source, Verdict, AuthResult, DeliveryStatus, StepStatus
  repository/     AlertRepository
  service/        AlertService, AlertStepService, AlertFactory, CreateAlertResult
  dev/            DevAlertController, FakeAlertFactory   (was top-level dev/, see below)
playbook/
  api/            PlaybookController
  api/response/   PlaybookSummaryResponse, PlaybookDetailResponse, PlaybookStepResponse
  domain/         Playbook, PlaybookStep, AnswerOptionsConverter
  repository/     PlaybookRepository
  service/        PlaybookService, ActivePlaybook
common/           CaseType, ClockConfiguration
  error/          ApiError, GlobalExceptionHandler, NotFoundException, ConflictException, BadRequestException
  paging/         PageResponse, Paging
```

## The border rule, as ArchUnit tests

`src/test/java/com/leul/havilor/ModuleBorderTest.java` analyses the compiled main classes (tests excluded):

| Rule | Meaning |
|---|---|
| `nobodyOutsideAlertUsesItsDomainOrRepository` | no class outside `alert..` depends on `alert.domain..` or `alert.repository..` |
| `nobodyOutsidePlaybookUsesItsDomainOrRepository` | the same for `playbook` |
| `alertIsReachedOnlyThroughServiceAndResponses` | outside classes may depend on `alert` **only** through `alert.service..` and `alert.api.response..` |
| `playbookIsReachedOnlyThroughServiceAndResponses` | the same for `playbook` |

Checked that the rules can fail: a throwaway class in `common/` that referenced `alert.domain.Alert` and
`playbook.api.PlaybookController` made 3 of the 4 rules fail with clear messages. Then it was deleted.

Today the only cross-module use is `alert.service` → `playbook.service.PlaybookService`,
`playbook.service.ActivePlaybook` and `playbook.api.response.PlaybookStepResponse`. All three are allowed.

## Decisions

- **The fake generator moved from `dev/` to `alert/dev/`.** It builds `CreateAlertRequest`s (`alert.api.request`)
  with alert enums (`alert.domain`), so as a separate module it would break the new rule. It exists only to feed the
  alert module, so it belongs inside it. It still only exists under the `dev` profile, at the same URL.
- **Only the members that now cross a package became public:**
  - `PlaybookRepository` (the interface): `playbook.service` must reach it. This is why the border moved from Java
    visibility to ArchUnit.
  - `CreateAlertRequest.toAlert()`, `AlertDetailResponse.from(...)`, `AlertSummaryResponse.from(...)`,
    `AlertStepResponse.of(...)`, `PlaybookSummaryResponse.from(...)`, `PlaybookDetailResponse.from(...)`: called by
    services.
  - `Url.addClick(...)` (called by `CreateAlertRequest`) and `Alert.clicksOfNewAlert()` (called by `AlertService`).
  
  Everything else keeps its visibility. For example `Recipient.belongTo`, `StepProgress.record`, the child
  `toX()` mappers and the other DTO `from(...)` methods are only used inside their own package.
- **`CreateAlertResult` lives in `service/`.** It is the service's return type, not an HTTP DTO.
  **`ActivePlaybook` is in `playbook/service/`** for the same reason: it is what `PlaybookService` hands other modules.
- **Request DTOs map to domain objects** (`api.request → domain`). That's allowed: it stays inside the module.
- **Tests stay in their packages,** except two that use package-private classes and so moved with them:
  `AnswerOptionsConverterTest` → `playbook/domain/`; `FakeAlertFactoryTest` and `DevAlertControllerIntegrationTest` →
  `alert/dev/`.
- **Known limit:** alert response DTOs expose alert enums (`Priority`, `AlertStatus`...) from `alert.domain`. A future
  module reading those fields would depend on `alert.domain` and trip the rule. When that happens, move the API-facing
  enums to `alert/api/` (or give the responses their own types) rather than weakening the rule.

## How it was done

1. `git mv` every class to its layer package, so history follows the files.
2. A script rewrote each `package` line, removed all project imports, and re-added exactly the ones each file uses.
   It ignored comments and strings when deciding.
3. Compiled, then made public only what the compiler named (the list above).
4. Replaced the old import-scanning `ModuleBorderTest` with ArchUnit (`com.tngtech.archunit:archunit-junit5:1.4.1`,
   test scope; not managed by Spring Boot, so the version is pinned).
5. `./mvnw test` → 89 tests, 0 failures.
6. Updated the file paths in `docs/progress/flow-*.md` and the README.

## Follow-up steps 3–6

| Step | Commit | Change |
|---|---|---|
| 3 | `refactor(alert): move alert graph building from CreateAlertRequest into AlertFactory` | New `alert/service/AlertFactory` (`@Component`, injected into `AlertService`) builds the `Alert`, its children and clicks from a `CreateAlertRequest`, including the click-matching 400s. `toAlert()` and the child `toX()` mappers are gone: request DTOs are **data-only** (fields + validation annotations, plus `CreateAlertRequest`'s null-list normalising). |
| 4 | `refactor: inject a Clock instead of calling Instant.now(); fixed clock in tests` | `common/ClockConfiguration` defines a `Clock` bean (`systemUTC`). The domain receives it as an argument (`Alert.close(verdict, clock)`, `Alert.recordStep(..., clock)` → `StepProgress.record(..., clock)`), and services, `GlobalExceptionHandler` and the fake generator inject it. `createdAt`/`updatedAt` moved from `@PrePersist`/`@PreUpdate` to Spring Data JPA auditing (`@CreatedDate`, `@LastModifiedDate`, `@EnableJpaAuditing(dateTimeProviderRef)`), whose `DateTimeProvider` reads the same `Clock`. Tests import `FixedClockConfiguration` (`@Primary` `Clock.fixed(2026-11-01T12:00:00Z)`), so `createdAt`, `updatedAt`, `closedAt`, `completedAt` and error `timestamp` are asserted **exactly**. |
| 5 | `fix(alert): refuse closing without a decided verdict with BadRequestException` | `Alert.close` throws `BadRequestException("verdict", …)` → 400, instead of `IllegalArgumentException`, which would have been a 500. The only behaviour change, and only reachable if a future code path skipped `@DecidedVerdict`. |
| 6 | `refactor(alert): remove unused AlertRepository.findByTicketNumber` | Production code uses `findIdByTicketNumber` since Flow 5; the repository test was switched to it. |

Step decisions:
- **Why pass the `Clock` into domain methods instead of injecting it:** entities are created by JPA, not Spring, so
  they can't have beans injected. A method argument keeps the dependency visible and makes unit tests trivial
  (`alert.close(MALICIOUS, fixedClock)`).
- **Why JPA auditing for the timestamps:** `@PrePersist`/`@PreUpdate` run inside the entity, where there is no
  `Clock`. Spring Data's `AuditingEntityListener` is a Spring-aware listener that takes "now" from our
  `DateTimeProvider`. `@DataJpaTest` doesn't load `@Configuration` classes, so `AlertRepositoryTest` imports
  `ClockConfiguration` explicitly.
- **Response DTOs keep their static `from(...)` factories.** Step 3 was about request-side graph building;
  read-side mapping stays a one-line factory per DTO.

## New concepts introduced

- **Package by feature, then by layer** inside the feature.
- **ArchUnit**: architecture rules as unit tests (`noClasses().that()…should().dependOnClassesThat()…`).
- **Visibility vs architecture tests**: package-private works only while caller and callee share a package; once
  layers split, a rule test takes over.
- **`java.time.Clock` as an injectable dependency**, and fixed clocks for deterministic tests.
- **Spring Data JPA auditing** (`@CreatedDate`, `@LastModifiedDate`, `DateTimeProvider`).
- **Factories in the service layer** to keep DTOs as plain data.
- **Refactoring safely**: `git mv` + mechanical rewrite + compiler-driven fixes + an unchanged test suite.
