# Flow 0: Foundation

**Goal:** the app starts and connects to a real PostgreSQL database, with Flyway, Validation and Lombok ready
and the feature module packages in place. No endpoints, no tables yet.

**Branch:** `feature/foundation` · **Status:** done, tests passing, pushed

## Commits

| Commit | Message |
|---|---|
| `9f399b1` | chore(infra): run PostgreSQL 18 in Docker via compose.yaml |
| `a9cbfe2` | fix(infra): expose PostgreSQL on host port 5433 |
| `658093c` | feat(config): connect to PostgreSQL in application.yml |
| `1ed8c3e` | feat(db): add Flyway with PostgreSQL support |
| `1401ef7` | build: add Bean Validation and Lombok |
| `4dc3063` | chore: add empty feature module packages |
| `427b302` | docs(readme): add run-locally steps and mark flow 0 done |

Before the flow: `0181ae9` docs: make Claude the builder and add per-flow progress docs; `AGENTS.md` deleted
(CLAUDE.md is the only instruction file); `HELP.md` deleted.

## Files created / changed

| File | What it does |
|---|---|
| `compose.yaml` (new) | Describes one Docker service, `db`: image `postgres:18`, container `havilor-db`, creates database/user/password `havilor`, maps host port **5433** to container port 5432, keeps data in the named volume `havilor-db-data`. |
| `src/main/resources/application.yml` (renamed from `.yaml`) | Gives Spring the three facts it needs for the DataSource: `url` (`jdbc:postgresql://localhost:5433/havilor`), `username`, `password`. Also sets `ddl-auto: validate` (Hibernate checks entities against the tables but never creates or changes them; Flyway does that) and `open-in-view: false` (the DB session closes when the service call ends, not after the web response is written). |
| `pom.xml` | Added `spring-boot-starter-flyway` + `flyway-database-postgresql` (Flyway itself + its PostgreSQL support), `spring-boot-starter-validation` (Bean Validation: `@NotBlank`, `@Valid`, ...), `lombok` (optional). The `maven-compiler-plugin` runs Lombok as an annotation processor at compile time; the `spring-boot-maven-plugin` leaves Lombok out of the final jar, because it is only needed while compiling. |
| `src/main/resources/db/migration/.gitkeep` (new) | Placeholder so Git keeps the folder where Flyway looks for migrations (`V1__...sql` arrives in Flow 1). |
| `src/main/java/com/leul/havilor/{alert,playbook,user,finding,escalation,audit,standup,common}/package-info.java` (new) | One per feature module. Git does not track empty folders, so each module package gets a `package-info.java` holding a one-line description of the module's job. |
| `README.md` | Flow 0 ticked; stack updated; "Run locally" section written. |
| `CLAUDE.md` | HOW WE WORK rewritten (Claude builds, teaching is in a separate chat, progress docs, stop after each flow). |

## Commands run

```bash
git switch -c feature/foundation
docker compose up -d                                   # start PostgreSQL
docker exec havilor-db psql -U havilor -d havilor -tc "select version();"
./mvnw test                                            # after each brick
./mvnw spring-boot:run                                 # real start, then stopped
git push -u origin feature/foundation
```

## How to verify

1. `docker compose up -d`, then `docker ps` shows `havilor-db` with `0.0.0.0:5433->5432/tcp`.
2. `docker exec havilor-db psql -U havilor -d havilor -tc "select version();"` prints `PostgreSQL 18.6 ...`.
3. `./mvnw test` ends with `Tests run: 1, Failures: 0` and `BUILD SUCCESS`. The log shows Flyway lines:
   `Database: jdbc:postgresql://localhost:5433/havilor (PostgreSQL 18.6)` and
   `Schema "public" is up to date. No migration necessary.`
4. `./mvnw spring-boot:run` logs `Tomcat started on port 8080` and `Started HavilorApplication`.
   `http://localhost:8080/` returns **404**, which is correct: there are no endpoints yet.

## New concepts introduced

- **Application startup:** `SpringApplication.run` builds the application context, scans `com.leul.havilor` for
  components, then **auto-configures** whatever libraries are on the classpath (web → Tomcat, JPA + driver → DataSource).
- **DataSource / connection pool (HikariCP):** needs URL + username + password. Without a URL the app fails at startup
  with *"Failed to configure a DataSource: 'url' attribute is not specified"* and the failure cascades
  (`dataSource` → `dataSourceScriptDatabaseInitializer` → `entityManagerFactory`).
- **Docker basics:** image vs container, environment variables, port mapping (`host:container`), named volumes for data that survives restarts, `docker compose up -d` / `down`.
- **Flyway:** runs at startup *before* Hibernate, applies versioned SQL files from `db/migration` in order, and records them in a `flyway_schema_history` table. Pairs with `ddl-auto: validate`.
- **Bean Validation and Lombok:** added now and used from Flow 1 on (request validation; generated getters/constructors).
- **Annotation processor:** a tool the compiler runs while compiling (Lombok writes getters/constructors into the class files).
- **Package by feature:** one package per module; `package-info.java` documents a package (and keeps the folder in Git).

## Issues hit and how they were solved

- **`password authentication failed for user "havilor"`:** a PostgreSQL 18 Windows service
  (`postgresql-x64-18`) already listens on `0.0.0.0:5432`, so `localhost:5432` reached it instead of the container.
  Fix: map the container to host port **5433** (`"5433:5432"`) and use 5433 in the URL. The Windows service was left
  untouched. Check a port's owner with `Get-NetTCPConnection -LocalPort 5432 -State Listen`.
- **Lombok on JDK 26:** the installed JDK is 26 (the project compiles for Java 21). A temporary `@Getter` probe test
  confirmed the Lombok processor works, then the probe was deleted.

## Known warnings (harmless for now)

- Flyway: `No migrations found. Are your locations set up correctly?` Expected until Flow 1 adds `V1__...sql`.
- Maven: `Could not transfer metadata ... flyway-community-db-support ... 401`. A repository declared in Flyway's
  parent POM that needs a GitHub login; everything Havilor needs comes from Maven Central.
- Mockito: `self-attaching to enable the inline-mock-maker`. A notice from the test library about future JDKs.

## Next

Flow 1: **Save one alert and read it back** (`POST /api/v1/alerts`, `GET /api/v1/alerts/{id}`, 4 fields,
first Flyway migration). Waiting for Leul before starting.
