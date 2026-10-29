# 05 — Technical decisions (ADRs)

Log of relevant architecture decisions, in a short Architecture Decision Record
format. Add a new entry (do not edit the previous ones) when the team makes a
non-trivial technical decision. The whole log was translated to English on
2026-09-24 (ADR-014); the content of each decision did not change.

---

## ADR-001 — Web application with a database instead of console + files

**Date:** defined before implementation started.
**Status:** Accepted.

**Context:** the official UPC guide for Moment 1 of Programación II asks, by default,
for a simple console/GUI application with file persistence.

**Decision:** a full web application with a real relational database will be built,
since the teacher explicitly authorized this scope extension, knowing that one of the
team members has 2+ years of web development experience.

**Constraint set by the teacher:** the backend must be in Java (non-negotiable).

**Consequences:** the project must still meet the same underlying rubric criteria
(full OOP, layered architecture, good practices), only implemented on a web stack
instead of a console. See `docs/00-guia-oficial-momento1.md` for the exact rubric.

---

## ADR-002 — Backend in Java + Spring Boot

**Date:** defined before implementation started.
**Status:** Accepted.

**Context:** the backend must be in Java (teacher constraint). A framework is needed
that supports a full REST API, with a layered architecture, role-based security and
persistence through an ORM.

**Decision:** Spring Boot (Spring Web + Spring Data JPA + Spring Security with JWT).

**Alternatives considered:** plain Java EE/Jakarta EE (discarded because of its more
verbose configuration for a small team with tight classroom-project deadlines).

**Consequences:** the layered architecture required by the guide maps naturally to
`controller → service → repository → entity`. Exception handling is centralized with
`@ControllerAdvice`. Role-based access control is implemented with Spring Security +
a `UserRole` enum.

---

## ADR-003 — PostgreSQL as the database

**Date:** defined before implementation started.
**Status:** Accepted (MySQL remains a valid alternative if the team changes its mind).

**Context:** the domain has a deep relational hierarchy
(faculty→program→moment→RAP→evaluation→grade) and needs aggregations for
comparative reporting (by semester, moment, program, year).

**Decision:** PostgreSQL.

**Alternatives considered:** MySQL — also valid, and the team is more familiar with
it, but PostgreSQL offers better support for window functions and complex
aggregations, which matters here because of the reporting module (the main gap that
motivates the project compared with AULAWEB).

---

## ADR-004 — Frontend in React + TypeScript

**Date:** defined before implementation started.
**Status:** Accepted.

**Context:** an SPA is needed with views specific to each role, complex forms
(RAP/moment configuration, versioned file uploads) and consumption of a REST API.

**Decision:** React + TypeScript, as the stack the team has the most prior experience
with (2+ years), already known and approved by the teacher.

---

## ADR-005 — Simplified Git Flow branching (`main` / `develop` / `feature/*`)

**Date:** at the start of implementation, before creating `backend/` and `frontend/`.
**Status:** Accepted.

**Context:** the team is three people working in parallel on the same repository,
with different levels of experience, and with university deliveries on fixed dates.
Everyone working directly on `main` would make it impossible to have a guaranteed
stable state on presentation day, and would leave no evidence of the process (the
guide asks to document how the project was developed).

**Decision:** simplified Git Flow — `main` as the production branch (only delivered,
stable states, with a tag per delivery), `develop` as the integration branch, and
temporary `feature/`, `fix/`, `test/`, `refactor/`, `docs/`, `chore/`, `release/` and
`hotfix/` branches. Everything reaches `develop` through a Pull Request with at least
one approval from another team member. Full detail in
[`06-plan-de-trabajo.md`](06-plan-de-trabajo.md).

**Alternatives considered:** trunk-based with direct pushes to `main` (discarded:
without a stable branch, any broken commit the night before puts the presentation at
risk); one branch per person instead of one per feature (discarded: long-lived
branches pile up huge conflicts and do not allow review per unit of work).

**Consequences:** no change reaches `main` without going through `develop` and a
review. Mandatory cross-review also prepares the team for the presentation, which
according to the rubric is individual and assesses mastery of the whole code, not
only the part each person wrote. It requires configuring branch protection on GitHub
and a CI workflow that validates every PR.

---

## ADR-006 — Frontend with Vite, Tailwind CSS and shadcn/ui

**Date:** 2026-09-24 (Stage 0).
**Status:** Accepted.

**Context:** the frontend needs forms, tables, dialogs and (in reporting) charts for
five different roles, built by a team with different levels of experience. ADR-004
set React + TypeScript but not the build tool or the component library.

**Decision:** Vite as the build tool; Tailwind CSS v4 for styles; shadcn/ui
(`new-york` style, Radix primitives) as the component library; React Router for
routing and axios as the HTTP client (a single client in
`services/api/httpClient.ts`). Tests with Vitest + React Testing Library.

**Alternatives considered:** Material UI (ready-made components, but with its own
styling system and a heavier bundle); Ant Design (very complete for admin panels, but
heavy and costly to adapt to a custom visual identity).

**Consequences:** the code of each component lives inside the repository
(`frontend/src/components/ui/`), is added with `npx shadcn@latest add <component>`
and can be freely modified. The visual theme is centralized in the CSS variables of
`src/index.css`. Process note: the AI session that created the skeleton had no access
to the shadcn/ui registry, so `components.json`, the theme and the `Button`, `Card`
and `Badge` components were written by hand with the same content the CLI generates.
On the team's machines the CLI works normally.

---

## ADR-007 — Integration tests against a real PostgreSQL (Testcontainers)

**Date:** 2026-09-24 (Stage 0).
**Status:** Accepted.

**Context:** the work plan left "Testcontainers (or H2)" open for the backend
integration tests. Flyway migrations are written in PostgreSQL SQL, and reporting
will use PostgreSQL-specific window functions and aggregations (see ADR-003).

**Decision:** integration tests (`@SpringBootTest`) run against a real PostgreSQL
started with Testcontainers, with the same image as `docker-compose.yml`
(`postgres:17-alpine`), connected with Spring Boot's `@ServiceConnection`
(`TestcontainersConfiguration`).

**Alternatives considered:** in-memory H2 (faster and without Docker, but with a
different SQL dialect: a test can pass on H2 and the same query fail on PostgreSQL,
or the other way around).

**Consequences:** running the full `./mvnw verify` requires Docker (Docker Desktop on
Windows/macOS). Unit tests with Mockito and `@WebMvcTest` tests do not need it.
GitHub Actions runners already ship with Docker, so CI needs no extra configuration.

---

## ADR-008 — Backend conventions: no Lombok, DTOs as `record` and an injectable clock

**Date:** 2026-09-24 (Stage 0).
**Status:** Accepted.

**Context:** the rubric assesses encapsulation, polymorphism, exception handling and
documentation of every class, and the presentation is individual and about the code.
In addition, several business rules depend on the current date (RF 3, 6 and 9).

**Decision:**

- **No Lombok.** Entities declare their getters/setters explicitly, with Javadoc.
- **DTOs as Java `record`s** (immutable, no boilerplate).
- **Constructor injection**, never `@Autowired` on fields.
- **Injectable clock:** services get the date with `LocalDate.now(clock)`, using the
  `Clock` exposed by `config.ClockConfig` in the `America/Bogota` zone, never with
  `LocalDate.now()`.
- **Business errors as subclasses of `SierapException`**
  (`ResourceNotFoundException` → 404, `BusinessRuleException` → 422,
  `ForbiddenAccessException` → 403; English names since ADR-014); each subclass
  defines its HTTP status through polymorphism and `GlobalExceptionHandler` responds
  with the `ErrorResponse` format.

**Alternatives considered:** Lombok (less code, but encapsulation is no longer
visible in the code being presented, and it adds an annotation processor that breaks
in misconfigured IDEs); calling `LocalDate.now()` directly (makes it impossible to
test date rules without depending on the day the test runs).

**Consequences:** entities have more lines (the IDE generates getters/setters). Every
date rule can be tested with `Clock.fixed(...)`, and controllers need no
`try/catch`.

---

## ADR-009 — JPA inheritance of `User` with the `JOINED` strategy

**Date:** 2026-09-24 (Stage 1).
**Status:** Accepted.

**Context:** `User` is abstract and has five subclasses with their own attributes
(`Admin` → faculty; `Teacher` → faculty; `Student` → code, semester and program;
`ExternalEvaluator` → evaluator type and optional data, see ADR-011; `SuperAdmin`
without attributes of its own). We had to choose how this hierarchy is stored in
PostgreSQL (`@Inheritance`).

**Decision:** `@Inheritance(strategy = InheritanceType.JOINED)`. The `app_user` table
stores the common data (names, document, email, password, role, status, creation
date) and each role has its own table (`super_admin`, `admin`, `teacher`, `student`,
`external_evaluator`) whose primary key is a foreign key to `app_user.id`. The
`app_user.role` column acts as the discriminator (`@DiscriminatorColumn`) with the
values of `UserRole`, to know the role without joins (useful at login and in Spring
Security). The table is called `app_user` because `user` is a reserved word in
PostgreSQL (ADR-014).

**Alternatives considered:**

- `SINGLE_TABLE`: a single table with all the columns of all the roles. It is faster,
  but forces the role-specific columns to accept `NULL` (`student_code` cannot be
  `NOT NULL`, for example) and the table does not resemble the class diagram.
- `TABLE_PER_CLASS`: a full table per role, with no common table. It makes it hard for
  the email to be unique across all users and to query "any user".

**Consequences:** the schema mirrors the hierarchy of the UML diagram (useful for the
presentation: each class has its table), each role can have its own `NOT NULL`
constraints and foreign keys, and the email is unique in `app_user`. Reading a
specific user means a join with its role table, a negligible cost for SIERAP's
volume.

---

## ADR-010 — Assessment configuration per academic period, with no standard values

**Date:** 2026-09-24 (Stage 1).
**Status:** Accepted.

**Context:** RAP, evaluation criteria, performance levels and scores vary by program,
by moment and over time; each admin creates, modifies or deletes them at their
discretion. At the same time, reporting (RF 12) must compare results across
semesters, so editing this semester's configuration cannot alter past semesters'
evaluations.

**Decision:**

- The configuration is versioned **per academic period**: `Moment` belongs to a
  `Program` **and** an `AcademicPeriod`. Each moment has its own RAP
  (`LearningOutcome`); each RAP, its `EvaluationCriterion`s; and each criterion, its
  `PerformanceLevel`s with their score.
- When a new period opens, the previous period's configuration is **copied** as a
  starting point (moments → RAP → criteria → levels) and edited. Past periods stay
  intact.
- **Nothing standard in the code:** there are no `NivelExigencia` or `NivelDesempeno`
  enums. The requirement level of the moment is free text, and the performance
  levels (name, description, score and order) are defined by the admin in each
  criterion. The RAP has no "weight": it weighs the sum of the maximum scores of its
  criteria.
- The evaluator picks **one level per criterion**; the grade is the sum of the
  picked scores, and the performance per RAP is the sum of its criteria
  (`CriterionGrade` replaces the old per-RAP grade).

**Alternatives considered:** a single live, editable configuration, deactivating
(not deleting) what is removed: simpler, but past semesters' configuration gets
distorted with every edit and comparisons lose meaning. A direct score per RAP (no
criteria or levels): does not reflect how the UPC assesses today (see the rubric in
`00-guia-oficial-momento1.md`, where each item has levels with points).

**Consequences:** there are more configuration entities (`EvaluationCriterion`,
`PerformanceLevel`) and a "copy configuration from the previous period" operation is
needed (Stage 3). In exchange, each evaluation stays tied to the configuration it was
made with, and comparative reports across periods are reliable.

---

## ADR-011 — Configurable evaluator types (`ExternalEvaluator` + catalog)

**Date:** 2026-09-24 (Stage 1).
**Status:** Accepted.

**Context:** today the teacher, the business person and the alumnus grade, but the
project lead indicated that more evaluator types with other names may appear, and
that they must be creatable without programming. With one `User` subclass per type
(`Empresario`, `Egresado`), each new type would require code, a table and a
migration.

**Decision:**

- `Teacher` stays a role of its own, because besides grading it enables groups,
  assigns evaluators and sets dates.
- Every evaluator who is not a teacher is a single subclass, **`ExternalEvaluator`**
  (role `EXTERNAL_EVALUATOR`), with an `EvaluatorType` taken from a **catalog**
  maintained by the super_admin. The catalog seed data are "Empresario" and
  "Egresado"; adding a type is adding a row.
- Type-specific data is optional in `ExternalEvaluator`: organization, position,
  graduation year and graduation program.
- The `Evaluator` interface is implemented by `Teacher` and `ExternalEvaluator`.

**Alternatives considered:** keeping `Empresario` and `Egresado` as subclasses and
adding a generic "Other evaluator" (two different ways of modeling the same thing);
one subclass per type (not configurable from the application).

**Consequences:** `UserRole` is left with `SUPER_ADMIN`, `ADMIN`, `TEACHER`,
`STUDENT` and `EXTERNAL_EVALUATOR`. Permissions are granted by role, regardless of
the type; the type is used for display, for reports and, if decided, to weight the
grade by evaluator type (pending in `docs/04`). The polymorphism required by the
rubric is kept: `getRole()` in each subclass and the `Evaluator` interface with two
implementations.

---

## ADR-012 — Moment lifecycle: configuration, group formation, launch and windows

**Date:** 2026-09-24 (Stage 1).
**Status:** Accepted.

**Context:** the project lead described how a moment runs, the same for any moment
and program:

1. The admin creates the moment (e.g. "Momento 1") and configures its RAP, criteria
   and levels with scores.
2. Teachers enable groups and assign their external evaluators.
3. Students enter SIERAP, see the groups of **all** the teachers of the moment and
   join the one they agreed on.
4. The admin **launches** the moment; work is submitted and graded.

**Decision:**

- `Moment.status` (enum `MomentStatus`): `CONFIGURATION` → `LAUNCHED`.
- The admin defines on the moment the minimum and maximum members per group and three
  **date windows**: group formation, submissions and grading. They must be ordered and
  must not overlap, and the grading window must end no later than the period's
  `upcEvaluationDeadline`. They may be empty while configuring; they are required to
  launch.
- **In `CONFIGURATION`:** the admin edits everything. Within the group formation
  window, teachers enable groups and the program's students join them (one group per
  moment).
- **"Launch"** is done by the admin **after** group formation: it validates that the
  configuration is complete and **locks** the RAP, criteria, levels, scores and group
  membership.
- **In `LAUNCHED`**, what is allowed depends on the dates, evaluated with the injected
  `Clock`: work is submitted within the submission window, graded within the grading
  window, and after grading the moment is closed. Outside the windows nothing is
  submitted or graded.
- Each teacher sets the submission deadline and evaluation date of **their** groups,
  within the moment windows (RF 7 and 9).

**Alternatives considered:** advancing every phase with buttons (more manual work and
easy to forget); launching when configuration ends, before group formation (the lead
preferred launching with the groups already formed); activating the moment by date
only, with no button.

**Consequences:** `MomentStatus` is a **system flow** enum, not an assessment business
value, so it does not contradict ADR-010. Each operation validates status and date
(e.g. joining only in `CONFIGURATION` and within the group formation window;
submitting only in `LAUNCHED` and within the submission window). What happens when
launching with groups below the minimum number of members is still pending.

---

## ADR-013 — Scores as `BigDecimal`

**Date:** 2026-09-24 (Stage 1).
**Status:** Accepted.

**Context:** the class diagram had performance level scores as `double`. The Moment 1
grade is a sum of 0.36, 0.48, 0.6 and 0.8, values with no exact binary
representation: with `double` the sum can give `4.999999…` instead of `5.0`, and such
a grade is displayed wrongly, compares wrongly against a threshold and throws reports
off.

**Decision:** scores (`PerformanceLevel.score`, the `getMaxScore()` of
`EvaluationCriterion`, `LearningOutcome` and `Moment`, and the grades computed in
Stage 6) use `java.math.BigDecimal`, stored in PostgreSQL as `NUMERIC(5,2)`. Tests
compare with `isEqualByComparingTo` (scale does not matter: `5.0` = `5.00`).

**Alternatives considered:** `double` with rounding on display (the error accumulates
in averages and comparisons, and rounding has to be remembered everywhere); storing
hundredths as integers (exact, but forces conversions across the whole application
and makes the code less readable in the presentation).

**Consequences:** sums are exact. Adding is written with `add` instead of `+`, and DTOs
expose the score as a decimal number in the JSON.

---

## ADR-014 — Language: English for code and documentation, Spanish for the user and for code comments

**Date:** 2026-09-24 (Stage 1).
**Status:** Accepted (approved by the project lead).

**Context:** the Stage 0 code, the Stage 1 model and the documentation were written in
Spanish. The project lead decided which parts of the project are written in English
and which stay in Spanish.

**Decision:**

- **In English:**
  - The code: classes, interfaces, enums and their values, attributes, methods,
    variables, tables and columns, API routes (`/api/status`) and JSON fields of the
    DTOs, on the backend and the frontend.
  - The documentation: `docs/` (except what is listed below), the READMEs and the
    ADRs.
  - Commit messages.
- **In Spanish:**
  - Every text the user sees: frontend screens and API error messages
    (`ErrorResponse.message`, validation messages), and the business data shown on
    screen (names of faculties, programs, RAP, criteria and levels in the seed data).
  - Javadoc and code comments, because the presentation to the university is in
    Spanish.
  - `docs/00` (transcription of the official guide) and `docs/entregas`.
- Name mapping: see the Spanish → English table in
  [`03-entidades-dominio.md`](03-entidades-dominio.md).
- SQL reserved words are avoided as table or column names: `app_user` instead of
  `user`, `ProjectGroup` instead of `Group`, `display_order` instead of `order`,
  `param_key`/`param_value` instead of `key`/`value`.

**Alternatives considered:** everything in Spanish (the previous state); English only
in new code (the backend would be mixed, with Stage 0 classes in Spanish and Stage 1
classes in English); Javadoc in English too (discarded by the lead: the presentation
is in Spanish).

**Consequences:** the Stage 0 code was renamed in the same PR as the academic model
(`EstadoSistema*` → `SystemStatus*`, `RelojConfig` → `ClockConfig`, the exceptions,
the `/api/estado` → `/api/status` endpoint and the JSON fields, with the frontend
adjusted). D1 and D3 branches must use the names in the `docs/03` table. The standard
Java naming convention required by the rubric stays the same; only the language
changes.

---

## ADR-015 — Submission files stored on the server disk

**Date:** 2026-09-25 (Stage 1).
**Status:** Accepted (decided by the project lead).

**Context:** each submission has a `.zip` file and a presentation, with version
history (RF 3). The files have to live somewhere the backend can store and serve
them from.

**Decision:**

- Files are stored on the **server disk**, under a base folder set by configuration
  (`sierap.storage.base-path`, different per profile) and outside the application
  folder. The database stores only the path **relative** to that base, never the
  file contents.
- Accepted formats and sizes: `.zip` up to **100 MB**; presentation `.pdf`, `.docx` or
  `.pptx` up to **30 MB**. The backend validates extension, content type and size
  before saving, and the file name on disk is generated by the backend (the original
  name is not trusted).
- A submission is never overwritten: each upload is a new version with its own files.

**Alternatives considered:** object storage such as S3 or MinIO (scales better, but
adds one more service to `docker-compose` and to the deployment); binary columns in
PostgreSQL (everything in one place, but the database grows fast and backups become
heavy).

**Consequences:** the simplest deployment. The upload folder must be backed up together
with the database, and in production it must be on persistent storage (a Docker
volume if the backend runs in a container). The multipart limits of Spring
(`spring.servlet.multipart.max-file-size` / `max-request-size`) must match these
sizes. Implemented in Stage 5 (D3):

- The base folder comes from `SIERAP_STORAGE_PATH` (in `dev`, `~/sierap-files`; the API
  refuses to start without it in `prod`). Each version goes to
  `groups/{groupId}/v{version}/` with a random name (`UUID` + validated extension).
- Besides the extension and the declared content type, the backend checks the first
  bytes of each file (`PK` for `.zip`, `.docx` and `.pptx`; `%PDF` for `.pdf`), because
  browsers do not always declare the same type (`application/octet-stream` is accepted).
  Wrong type → `400`; too large → `413` (Spring's multipart limits are 100 MB per file
  and 135 MB per request; the 30 MB of the presentation is checked by the service).
- Migration V8 makes the presentation optional and adds the original name and the size
  of each file, used only to show them and as the download name. V7 stays reserved for
  `individual_grade` (ADR-018).
- If the transaction that registers a version is rolled back, the files it already
  wrote are deleted, so no orphan files are left.

---

## ADR-016 — Delete policy: cascade within configuration, blocked once there are groups or grades

**Date:** 2026-09-25 (Stage 1).
**Status:** Accepted (decided by the project lead).

**Context:** V1 deletes in cascade faculty → program → moment → RAP → criteria →
levels. The D3 draft blocks (`RESTRICT`) deleting moments, teachers, criteria or
levels that already have groups or grades, so a configuration change never erases
past grades (ADR-010). Both rules had to be reconciled.

**Decision:** both rules stay, and they are compatible:

- **Configuration without operational data** (no groups, no grades) is deleted in
  cascade, as V1 does.
- **Once there are groups, submissions or grades**, the foreign keys that point to the
  configuration from operational data are `RESTRICT`: PostgreSQL rejects the whole
  delete, including the cascades it would have triggered.
- What belongs to a group (members, assigned evaluators, submissions, evaluations,
  grades) is deleted with the group.
- Users are never deleted in cascade: the foreign keys from users to faculty and
  program are `RESTRICT`.
- When the database rejects an operation, `GlobalExceptionHandler` turns the
  `DataIntegrityViolationException` into a **409 Conflict** with a message in
  Spanish, instead of a 500. The services should still check the business rules
  first and throw a `BusinessRuleException` with a precise message; the 409 is the
  last line of defense.

**Alternatives considered:** never deleting and only deactivating (needs an `active`
column and filtering in every query); deleting only when empty, without cascades
(safer against accidental deletions, but it forces deleting level by level).

**Consequences:** deleting configuration that has not been used is easy, and grades
can never be lost by deleting configuration. The frontend must show the 409 message
to the user.

---

## ADR-017 — Authentication, sessions and account onboarding

**Date:** 2026-09-27 (Stage 2).
**Status:** Accepted (decisions of the project lead; technical details proposed by D1).

**Context:** Stage 2 needs login, sessions and a way to create accounts for the five
roles. The lead decided who creates whom, how people get their password and how the
first super_admin exists.

**Decision:**

- **Stateless Spring Security with JWT.** Passwords are stored with BCrypt.
- **Sessions: short access token + refresh token.**
  - Access token: JWT signed with HS256, valid for **15 minutes**, with the user id,
    role and email as claims. The frontend keeps it **in memory** (never in
    `localStorage`) and sends it as `Authorization: Bearer`.
  - Refresh token: an opaque random value valid for **7 days**, sent in an
    **httpOnly, Secure, SameSite=Strict** cookie limited to `/api/auth`. The database
    stores only its hash (`refresh_token` table), so it can be revoked. Each refresh
    rotates it: the old one is marked as rotated, and if it is presented again (a sign
    it was stolen) every session of the user is closed. Logout, a password change and
    deactivating the account delete the sessions instead, so a stale cookie in another
    tab only gets a `401`.
  - The JWT signing key and the lifetimes come from configuration
    (`SIERAP_JWT_SECRET`, never in the repository).
- **Who creates accounts:** super_admin → faculty admins; admin → teachers; teacher →
  their students (one by one or by bulk upload) and their external evaluators. Upper
  roles can also create lower users **within their scope** (the super_admin anyone;
  an admin, teachers, students and external evaluators of their faculty).
- **How a created user gets a password:** SIERAP sends an **email link** to set it.
  The link carries a one-time random token (stored hashed, `password_token` table)
  that expires in 72 hours; until the password is set, the account cannot log in.
  Passwords need at least 8 characters, with at least one letter and one number.
- **Self-registration with a code:** a teacher generates a **registration code**
  linked to **the teacher and one program** of their faculty, reusable until its
  expiry date, and they can deactivate it earlier. A student registers with the code
  and their data, lands in that program, appears in the teacher's student list, and
  receives the same email link to set their password (which also proves they own the
  email). Only institutional emails (`@unicesar.edu.co`, configurable) are accepted.
- **Forgot password:** same email link mechanism, with a link that expires in 1 hour.
  The request always answers the same way, whether the email exists or not, so
  accounts cannot be discovered.
- **First super_admin:** created by a migration with a **temporary password** that
  must be changed at the first login (`must_change_password`). While that flag is on,
  the API only allows the `/api/auth` calls (change the password, profile, logout). The temporary password is written in the
  repository, so it must be changed as soon as each environment is deployed.
- **Email:** Spring Mail over SMTP, configured by environment variables. In
  development, Mailpit in `docker-compose` catches every email (web inbox at
  <http://localhost:8025>). In production, an **institutional UPC account**.
- **Authorization:** role checks with `@PreAuthorize`; scope checks (own faculty,
  own groups…) in the services, which throw `ForbiddenAccessException`. Missing or
  invalid tokens answer 401 and denied access 403, both with the uniform
  `ErrorResponse`.

**Alternatives considered:** a single 8-hour token in `localStorage` (simpler, but a
stolen token stays valid for hours and `localStorage` is readable by any injected
script); temporary passwords handed over by the creator (no mail server needed, but
the password travels outside the system); creating the first super_admin from
environment variables at startup (keeps the password out of the repository; the lead
preferred the migration).

**Consequences:** the API contract is in [`07-contrato-api.md`](07-contrato-api.md).
New tables: `refresh_token`, `password_token` and `registration_code`, plus
`app_user.must_change_password`, `app_user.created_by_id` (who created the account; it
sets who can manage it) and `student.registration_code_id`. `password_hash` becomes nullable (accounts waiting for
their password). The UPC must provide the SMTP credentials of an institutional account
before deployment.

---

## ADR-018 — Group grade with individual adjustments

**Date:** 2026-09-28 (API contract of Stages 4 to 6).
**Status:** Accepted (decided by the project lead).

**Context:** evaluators grade the group once (one level per criterion), but the grade
is individual: the official UPC guide says the presentation is graded per student
(`docs/00`), and a teacher may want to recognize that a member did better or worse
than the rest.

**Decision:**

- The group is graded **once**: one `CriterionGrade` per criterion in each
  `Evaluation`. Every member **inherits** those levels.
- Any evaluator (the teacher and the assigned external evaluators) can open one
  student and change the level of the criteria where that student did better or
  worse. Each change is an `IndividualGrade` (evaluation, student, criterion, level and
  a copy of its score); a criterion without an adjustment keeps the group's level.
  Adjustments pick configured levels, never free numbers.
- Two grades come out of it: the **group grade** (group levels only) and each member's
  **individual grade** (group levels replaced by their adjustments), which is the
  student's final grade. Both use the same formula: weighted by kind of evaluator,
  averaging several evaluators of the same kind and redistributing the weight of a
  kind without submitted evaluations.
- Adjustments follow the rules of the evaluation they belong to: they are made in the
  grading period of the group and only count once the evaluation is submitted.
  Setting the same level as the group's removes the adjustment.

**Alternatives considered:** marking each criterion as group or individual and
grading every student in the individual ones (more work per evaluation, even when
nobody stands out); a free individual grade set by the teacher and combined with the
group grade by weights (loses the link to RAP and levels); grading every criterion
twice, for the group and for each student (multiplies the grading work).

**Consequences:** a new entity `IndividualGrade` (table `individual_grade`, migration
V7 in Stage 6) composed by `Evaluation`, which gains the methods to compute the score
of one student in total and per RAP. Completeness for submitting still refers to the
group levels only. Reports can compare group and individual grades.

---

## ADR-019 — Whole system runnable with Docker Compose

**Date:** 2026-10-29
**Status:** Accepted (requested by the project team).

**Context:** testing SIERAP required JDK 21, Node 22 and Docker installed and three
terminals (database, backend, frontend). People who only want to try the system (for
example, entering the admin panel) need a single command.

**Decision:**

- `backend/Dockerfile`: multi-stage build, the jar is packaged with the Maven Wrapper on
  `eclipse-temurin:21-jdk` (tests skipped: they run in CI and with `./mvnw verify`) and
  runs on `eclipse-temurin:21-jre`.
- `frontend/Dockerfile`: the app is built with Vite on `node:22-alpine` and served by
  `nginx`, which forwards `/api` to the backend container (same as the Vite proxy in
  development) and falls back to `index.html` for React Router routes.
- Both services are in `docker-compose.yml` under the Compose profile `app`, so
  `docker compose up -d` still starts only PostgreSQL and Mailpit for developers, and
  `docker compose --profile app up -d --build` starts everything. The backend runs with
  the `dev` profile (development JWT key, refresh cookie without https), sends emails to
  Mailpit and stores submission files in the `sierap-files` volume (ADR-015). The app is
  served on `http://localhost:5173`, the same URL the email links use in development.

**Alternatives considered:** a separate compose file for the full system (two files to
keep in sync); running the backend with `prod` (requires real SMTP and secrets, not
meant for local trials).

**Consequences:** these images are for local trials and demos, not for production: a
production deployment must use the `prod` profile with its environment variables
(section 8 of the README).

---

## Template for new entries

```
## ADR-00X — <short title of the decision>

**Date:** <date>
**Status:** Proposed / Accepted / Superseded by ADR-00Y

**Context:** <what problem motivates this decision>

**Decision:** <what was decided>

**Alternatives considered:** <what other options were evaluated and why not>

**Consequences:** <what this decision implies going forward>
```
