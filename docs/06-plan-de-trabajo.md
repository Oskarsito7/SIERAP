# 06 — Work plan: stages, team and branches

> How SIERAP development is organized among **3 people**: which stages the work is
> split into, who is responsible for what, and which branching convention is used to
> avoid stepping on each other. Traceability: each stage references the functional
> requirements in [`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md)
> and the rubric criteria in [`00-guia-oficial-momento1.md`](00-guia-oficial-momento1.md).

---

## 1. The team — 3 roles, 3 verticals

The split is **not by layers** (one backend, one frontend, one database): that causes
constant blocking ("I can't move until the other one finishes the endpoint"). It is
split by **functional vertical**: each person takes their feature end to end (entity
→ repository → service → controller → React screen → tests), on top of a common base
the three build together at the beginning.

| # | Role | Owner | Owns |
|---|---|---|---|
| **D1** | Architect / platform | Project lead (with Claude Code) | Project skeleton, security and JWT, role-based access control, exception handling, CI, PR reviews, integrity of the class diagram |
| **D2** | Academic domain and configuration | Developer 1 | Faculty, Program, AcademicPeriod, Moment (lifecycle and windows), LearningOutcome (RAP), criteria and levels with scores; `admin` and `super_admin` screens |
| **D3** | Operations and evaluation | Developer 2 | Groups (classroom project), enrollment, versioned submissions, evaluation, reporting; `teacher`, `student` and `external_evaluator` screens |

**Working rules:**

- **Only D1 touches** `config/`, `security/`, `exception/` and `pom.xml`. If D2 or D3
  need a change there, they ask in the PR — they don't do it on their own.
- **JPA entities are created in Stage 1, in sequence D2 → D1 → D3** (each depends on
  the previous ones), so that afterwards nobody has to modify someone else's file and
  cause conflicts.
- **Flyway migrations already in `develop` are never edited:** any fix goes in a new
  migration.
- **Contract before code:** before implementing a vertical, the API contract (routes,
  input/output DTOs, error codes) is agreed and written down in the PR. That way D3
  can write the frontend against an agreed contract without waiting for the backend.
- **Nobody merges their own PR.** Each PR is approved by at least one other person.
- The **presentation is individual** (UPC rubric): all three must be able to explain
  the whole system, not only their vertical. That is why PR reviews are mandatory —
  reading the others' code is part of studying for the presentation.

---

## 2. Stages

Duration in relative weeks; adjust to the real Moment 1 delivery date. Stages 0–2 are
**sequential and blocking**; from stage 3 onward there is real parallelism.

### Stage 0 — Foundations (Week 1) · all 3 together

Goal: anyone can clone the repo and start everything with one command.

| Task | Owner |
|---|---|
| Create `develop`, protect `main` and `develop` on GitHub | D1 |
| Backend skeleton: Maven, Spring Boot 3.x, Java 21, layered package structure | D1 |
| `docker-compose.yml` with PostgreSQL + `application-dev` / `application-prod` profiles | D1 |
| Frontend skeleton: Vite + React + TypeScript, router, HTTP client, folder structure | D3 |
| GitHub Actions: backend build + tests, frontend build + lint, on every PR | D1 |
| Package diagram (pending from Phase 2 of the rubric) | D2 |
| `docs/entregas/` — skeleton of the formal delivery document | D2 |

**Definition of done:** `docker compose up` + `mvn spring-boot:run` + `npm run dev`
start the three services on any team machine; CI is green.

**Rubric covered:** RAP2 (layered architecture), RAP4 (good practices).

---

### Stage 1 — Domain model and persistence (Week 2) · in sequence D2 → D1 → D3

Goal: turn `docs/03-entidades-dominio.md` into real JPA entities. It goes **in
sequence** because each part references the previous one (`Admin` points to
`Faculty`, `ProjectGroup` points to `Teacher` and `Student`): each person starts when
the previous part is merged. Each migration has its number reserved so nobody
collides.

| Order | Task | Owner | Migrations |
|---|---|---|---|
| 1 | `Faculty`, `Program`, `AcademicPeriod`, `GlobalParameter`, `Moment` (+ enum `MomentStatus`), `LearningOutcome`, `EvaluationCriterion`, `PerformanceLevel` + repositories | D2 | `V1__academic_schema.sql` |
| 1 | Seed data: faculty, Ingeniería de Sistemas, current period and the complete Moment 1 (5 RAP, criteria and levels of the `docs/00` rubric) | D2 | `V2__academic_seed.sql` |
| 2 | Abstract `User` (JOINED, ADR-009) + `SuperAdmin`, `Admin`, `Teacher`, `Student`, `ExternalEvaluator` + `EvaluatorType` + enums `UserRole` and `DocumentType` + repositories | D1 | `V3__users.sql` |
| 3 | `ProjectGroup` (group = classroom project), `Submission`, `Evaluation`, `CriterionGrade` + repositories, and the `Evaluator` interface with its implementations | D3 (+ D1 on `Evaluator`) | `V4__operations.sql` |
| — | Javadoc (in Spanish, ADR-014) on every domain class (required by the rubric) | each person on their part | — |

**Definition of done:** the schema is created on startup, there is seed data, and
the relationships (inheritance, composition, aggregation, association) of the UML
diagram are reflected in the code.

**Rubric covered:** RAP3 (class diagram), RAP2 (OOP: inheritance, interfaces,
encapsulation), RAP4 (documentation through comments).

---

### Stage 2 — Authentication and access control (Week 3) · led by D1

Goal: the remaining stages can assume "there is an authenticated user with a role".

| Task | Owner |
|---|---|
| Spring Security + JWT: login, token issuing and validation | D1 |
| Role-based authorization (`@PreAuthorize` / `SecurityFilterChain`) for the 5 roles | D1 |
| **Scope rules**: a teacher only sees their groups, an admin only their faculty, an evaluator only what was assigned | D1 |
| Global `@ControllerAdvice` + hierarchy of domain exceptions | D1 |
| Login + route guards + session persistence on the frontend | D3 |
| Base layout per role (different menu for each `UserRole`) | D3 |

**Definition of done:** there are tests proving that a teacher **cannot** read
someone else's group and an admin **cannot** touch another faculty (403, not a 200
with an empty list).

**Covers RF:** 1, 5, 7, 11 (the login part).
**Rubric:** RAP2 (exception handling), RAP5 (access control requirements).

---

### Stage 3 — Academic configuration (Weeks 4–5) · D2

Goal: **no business value hard-coded** — this is the project's differentiating
factor compared with AULAWEB.

- CRUD of `Faculty` and `Program` (`super_admin`).
- CRUD of `AcademicPeriod` (`super_admin`) and the `EvaluatorType` catalog.
- CRUD of `Moment` per program and period: subject, semesters, requirement level,
  min./max. members and date windows (group formation, submissions, grading).
- CRUD of `LearningOutcome`, `EvaluationCriterion` and `PerformanceLevel` with its
  score, only while the moment is in `CONFIGURATION`.
- **Launch** the moment (validates the configuration is complete and locks it) and
  **copy the configuration** from the previous period (ADR-010, ADR-012).
- Validations: ordered windows within the UPC limit, consistent semester ranges.
- `admin` and `super_admin` screens.

**Covers RF:** 10, 11 and the cross-cutting configuration rules.

---

### Stage 4 — Teacher operations (Weeks 4–5, in parallel with 3) · D3

- The teacher **enables** groups in a moment (during configuration); **they only see
  their own**.
- Setting the submission deadline of each group, within the submission window.
- Setting the evaluation date, **within the grading window** (and therefore the UPC
  limit).
- Creating external evaluators (type from the catalog) and assigning them **only to
  their groups**.

**Covers RF:** 7, 8, 9.

---

### Stage 5 — Student and versioned submissions (Week 6) · D3, with D2 support

- See the groups of all the teachers of the moment and **join** one (one group per
  moment; the moment's min./max. members; only within the group formation window).
- Upload of a `.zip` submission + `.pdf` / `.docx` presentation.
- **Versioning**: each upload is a new `Submission`, it does not replace the previous
  one; the history can be queried.
- Rejection of submissions after the deadline.
- File storage (decide in an ADR: file system vs. binary column).

**Covers RF:** 2, 3.

---

### Stage 6 — Multi-role evaluation (Week 7) · D2 + D3

- Actual implementation of the `Evaluator` interface for `Teacher` and
  `ExternalEvaluator` — this is where the **polymorphism** required by the rubric
  becomes visible.
- `Evaluation` with its `CriterionGrade`s (one picked level per criterion of the
  moment).
- Time window: grading is only possible within the set date.
- Calculation of the consolidated group grade and individual performance per RAP.
- Grading screen (evaluators) and results screen (student).

**Covers RF:** 4, 6. **Rubric:** RAP2 (polymorphism/interfaces), RAP4 (functionality
of the solution).

---

### Stage 7 — Reporting (Week 8) · D2, with D3 support on charts

AULAWEB's main gap, and therefore what weighs the most in the presentation.

- Individual report (a student's performance per RAP).
- Group report.
- Consolidated report per program/moment.
- **Comparisons**: semester X vs. semester Y, by year, by program.
- Export (PDF/Excel — decide the real scope based on the remaining time).

**Covers RF:** 12.

---

### Stage 8 — Stabilization and delivery (Week 9) · all 3

- Feature freeze (`feature/*` stops merging into `develop`; `release/*` is opened).
- End-to-end tests of the 12 RF.
- Review of Java naming, indentation and Javadoc **against the rubric**.
- Update `03-entidades-dominio.md` and the `.drawio` with what was actually built.
- Formal delivery document in `docs/entregas/` + mockups + package diagram.
- Presentation rehearsal: **each person must be able to explain the three verticals**.
- Merge `release/*` → `main` + tag `v1.0.0-momento1`.

---

## 3. Branching strategy

### 3.1 Permanent branches

| Branch | Purpose | Rules |
|---|---|---|
| `main` | **Production.** Only presented/delivered and stable code. | Protected. Nobody pushes directly. Only receives merges from `release/*` and `hotfix/*`. Each merge gets a tag (`v1.0.0-momento1`). |
| `develop` | **Integration.** The current state of development; it must always compile and pass CI. | Protected. Only receives merges through an approved Pull Request. |

### 3.2 Temporary branches

Created **from `develop`** and merged **into `develop`** (except `hotfix/*`). Deleted
after the merge.

| Prefix | What for | Example |
|---|---|---|
| `feature/` | New functionality | `feature/d2-rap-crud` |
| `fix/` | Fix of a bug in development | `fix/d3-submission-deadline-validation` |
| `test/` | Tests added independently from the feature | `test/d1-teacher-access-control` |
| `refactor/` | Reorganization without behavior change | `refactor/d1-extract-evaluation-service` |
| `docs/` | Documentation or diagrams only | `docs/d2-package-diagram` |
| `chore/` | Configuration, dependencies, CI | `chore/d1-github-actions` |
| `release/` | Freeze before a delivery | `release/momento1` |
| `hotfix/` | Urgent fix on something already delivered | `hotfix/broken-login` |

**Naming convention:** `<prefix>/<dev>-<kebab-case-description>`. The `<dev>` (`d1` /
`d2` / `d3`, or initials) makes it obvious at a glance who is working on what.

### 3.3 Release and hotfix rules

- `release/momento1` branches off `develop` when features are frozen. Only fixes go
  in there. When finished it is merged into **`main` and back into `develop`**.
- `hotfix/*` branches off **`main`** (not `develop`) and is merged into **`main` and
  `develop`**. It is the only branch that may skip `develop`.

### 3.4 Everyone's daily flow

```bash
git checkout develop
git pull origin develop
git checkout -b feature/d2-rap-crud
# ... work, small and frequent commits ...
git push -u origin feature/d2-rap-crud
# open a PR into develop and ask another team member for review
```

If the branch has been open for several days, **bring `develop` in often** to avoid
big conflicts at the end:

```bash
git checkout feature/d2-rap-crud
git merge origin/develop
```

### 3.5 Commit convention

Conventional Commits format, in English (ADR-014) — it keeps the history readable and
serves as evidence of the process for the teacher:

```
feat(rap): configurable RAP CRUD per moment
fix(submission): reject uploads after the deadline
test(security): teacher cannot access other teachers' groups
docs(entities): update diagram with Evaluation
chore(ci): add backend build workflow
refactor(evaluation): extract grade calculation to EvaluationService
```

Suggested scopes: `auth`, `user`, `faculty`, `program`, `moment`, `rap`, `group`,
`submission`, `evaluation`, `report`, `ui`, `ci`.

### 3.6 Pull Request checklist

A PR is not approved unless it meets:

- [ ] It compiles and CI is green.
- [ ] It has tests for what it adds (see section 4).
- [ ] It respects the layered architecture: no business logic in the controller, no
      JPA entity crossing the API boundary (always DTOs).
- [ ] Standard Java naming: lowercase packages, `PascalCase` classes, `camelCase`
      methods and attributes — **it is a rubric criterion**.
- [ ] New classes with Javadoc (in Spanish) — **also a rubric criterion**.
- [ ] Language according to ADR-014: code in English; screens, error messages,
      Javadoc and comments in Spanish.
- [ ] No scattered `try/catch`: exceptions are handled in the `@ControllerAdvice`.
- [ ] No hard-coded business values (RAP, weights and semester ranges go in the DB).
- [ ] No edits to migrations already in `develop`.
- [ ] If the domain changed: `docs/03-entidades-dominio.md` and the `.drawio` updated.
- [ ] If it was a non-trivial technical decision: new ADR in `docs/05-decisiones-tecnicas.md`.

### 3.7 Protections to configure on GitHub

In `Settings → Branches`, for `main` and `develop`:

- Require a pull request before merging (at least **1 approval**).
- Require status checks to pass (the CI workflow).
- Require branches to be up to date before merging.
- Block force-push and branch deletion.

---

## 4. Tests — who tests what

Tests **go in the same branch as the feature**, they are not left for the end. A
separate `test/*` branch is only used when adding tests to something already merged.

| Level | Tool | What is tested | Owner |
|---|---|---|---|
| Backend unit | JUnit 5 + Mockito | `Service` logic: grade calculation, date validation, members-per-group rule | each person on their vertical |
| Backend integration | `@SpringBootTest` + Testcontainers (ADR-007) | Repositories, migrations, reporting queries | D2 |
| Security | `spring-security-test` (`@WithMockUser`) | **That each role only sees its own** — the most important test of the project | D1 |
| Frontend | Vitest + React Testing Library | Form components, route guards per role | D3 |
| End to end | Manual, guided by the 12 RF (or Playwright if time allows) | Full flow per role before each delivery | all 3, in Stage 8 |

**Minimum required per PR:** every business rule and every access control rule added
must have at least one test that fails if the rule is broken.

---

## 5. Work cadence

- **Monday — planning (30 min):** which branch each person opens this week.
- **Mid-week — sync (15 min):** blockers and API contract changes.
- **Friday — integration:** everything finished is merged into `develop` and it is
  checked that `develop` starts completely. Nothing stays unmerged over the weekend.
- **End of stage:** 10-min internal demo (a direct rehearsal of the presentation).

---

## 6. Risks and mitigation

| Risk | Mitigation |
|---|---|
| The only person with web experience becomes a bottleneck | D1 does not implement full verticals: builds the base and reviews. D2 and D3 build the verticals. |
| Constant conflicts in the JPA entities | They are all created at once in Stage 1 and barely touched afterwards. |
| The real (institutional) scope does not fit in the Moment 1 deadline | Stages 0–6 are the minimum deliverable. Stage 7 (reporting) is cut down to basic reports if time runs short, leaving comparisons for Moment 2. |
| Frontend blocked waiting for endpoints | API contract agreed before implementing; D3 works against mocked data meanwhile. |
| Reaching the presentation knowing only one's own part | Mandatory cross-review of PRs + internal demo at the end of each stage. |
