# SIERAP — Sistema Integral Evaluador de Resultados de Aprendizajes por Programa

> Root document of the project. Read it first, in full, before touching code or
> continuing any conversation (AI or human) about this repository. It contains all
> the context agreed so far: problem, solution, scope, stack, decisions made and their
> rationale.

**Last update:** see the git history.
**Author / team:** Oskramsito and team — Universidad Popular del Cesar (UPC), Systems Engineering.

---

## 1. What SIERAP is

SIERAP is a web system to **manage and assess the classroom projects and the RAP
(Program Learning Outcomes, *Resultados de Aprendizaje del Programa*)** of the
Universidad Popular del Cesar. It was born as the classroom project of the course
**Programación II (SS300)**, assessed in its **Moment 1**, but its real design and
implementation scope is **institutional**: it is not a trimmed-down class exercise, it
is the complete system the team intends to build to replace the current flow.

## 2. The problem it solves

RAP are mandatory from the first semester in Systems Engineering at the UPC and are
assessed **cumulatively** at different moments of the program:

- **Moment I** — assessed in the third semester, in the course Programación de
  Computadores II, but it covers everything learned since the first semester.
- **Moment II** — assessed in Ingeniería de Software II, over a wider range of
  semesters.
- **Moment III** — assessed in an in-depth elective course.

Each moment reassesses the same 5 institutional RAP, raising the requirement level
(low → medium → high).

In SIERAP, students' projects are graded by the group's **teacher** and by **external
evaluators** (today business people and alumni; more types can be created). Students
do not self-assess. Today the process is managed through **AULAWEB**, the UPC's own
(not external) system, which **was not designed specifically for this flow**:

- The interaction is not intuitive for teachers or students.
- Organizing groups, submissions, dates and grades per RAP is cumbersome and largely
  manual.
- **It does not produce reports** that allow results to be consolidated or compared
  across moments, semesters, programs or years.
- The assessment criteria (RAP, moments, associated semesters, scores) vary by
  program and faculty, and AULAWEB does not allow each dean/program to configure them
  flexibly.

**Main consequence:** the process is exhausting for teachers and students, students
lack clear visibility of their cumulative performance per RAP, and the university
cannot make curricular decisions based on consolidated data.

See the full problem statement (causes/consequences/application area) in
[`docs/01-planteamiento-problema.md`](docs/01-planteamiento-problema.md).

## 3. The solution — what SIERAP does

Web system with **5 roles**:

| Role | What it does |
|---|---|
| `super_admin` | Configures the global parameters, the academic periods and the evaluator type catalog. |
| `admin` | One per faculty (used by the dean). Creates and configures the moments of each program (RAP, criteria, scores, dates) and launches them. |
| `teacher` | Enables groups in a moment, creates and assigns external evaluators, sets dates and grades only the groups they enabled. |
| `student` | Sees the groups of the moment, joins one, uploads versioned submissions (zip + presentation) and checks their grade per RAP. |
| `external_evaluator` | Business person, alumnus or another type from the catalog: grades the groups they were assigned to. |

It produces **individual, group and comparative reports** (across semesters, moments,
programs or years) — AULAWEB's most critical gap today.

**Differentiating factor:** unlike AULAWEB (generic), SIERAP is designed specifically
around the lifecycle of a RAP: `moment → level → program → group → submission →
multi-role evaluation → report`, with flexible configuration per faculty/program and
automatic comparative reporting.

Full description of the solution in
[`docs/02-descripcion-solucion.md`](docs/02-descripcion-solucion.md).

## 4. Classroom project scope (Moment 1) vs. real scope

It is important that anyone (or any AI) joining this project understands this
distinction:

- **The official UPC guide** for Moment 1 of Programación II asks, by default, for a
  simple console/GUI application with **file persistence**, in pairs.
- **The team decided, with the teacher's explicit authorization** (the teacher knows
  one of the members has 2+ years of web development experience), to go further: a
  **full web application with a real database**. The teacher's only non-negotiable
  requirement is that **the backend must be in Java**.
- Therefore, the system being built **is not a reduced academic mock-up**: it is the
  real start of SIERAP as a product, also assessed against the official Moment 1
  rubric.

The full official rubric (with scores) is in
[`docs/00-guia-oficial-momento1.md`](docs/00-guia-oficial-momento1.md) (in Spanish, as
a faithful transcription) — any scope decision for university deliveries must be
checked against those assessment criteria.

## 5. Technology stack

| Layer | Technology | Why |
|---|---|---|
| Backend | **Java + Spring Boot** (Spring Web, Spring Data JPA, Spring Security + JWT) | Non-negotiable teacher requirement (Java). Spring Boot is the de facto standard for web in Java; it fits the layered architecture required by the guide (controller/service/repository/entity) and the full OOP the rubric asks for (interfaces, inheritance, exception handling through `@ControllerAdvice`). |
| Database | **PostgreSQL** | More robust than MySQL for the hierarchical relationships of the domain (faculty→program→moment→RAP) and better support for reporting aggregations. MySQL is a valid alternative if the team prefers. |
| Frontend | **React + TypeScript** | The team's usual stack (2+ years of experience), already approved by the teacher. |

**Language (ADR-014):** code, documentation and commit messages are in English; screens,
API error messages, Javadoc and code comments are in Spanish.

## 6. Current design status (Moment 1 — Phases 1 and 2)

- [x] Problem statement (causes, consequences, application area)
- [x] Solution description (scope, differentiating factor, requirements)
- [x] Identification of domain entities
- [x] Preliminary class diagram in UML notation
- [x] Improved class, hierarchy and package (layer) diagrams
- [x] Functional requirements formalized by role with access control
- [x] Preliminary GUI design: medium-fidelity wireframes of every screen per role (`docs/wireframes/`)
- [ ] Code implementation (progress on the diagram classes)

Implementation progress, by stages of [`docs/06-plan-de-trabajo.md`](docs/06-plan-de-trabajo.md):

- [x] **Stage 0 — Foundations:** backend and frontend skeleton, PostgreSQL in
  `docker-compose`, CI on GitHub Actions.
- [ ] Stage 1 — Domain model and persistence (JPA entities + Flyway): V1–V4 merged
  (academic model, users, operations) and the detailed class diagram regenerated;
  pending D2's fixes from the PR #3 review (V5).
- [ ] Stage 2 — Authentication and access control: backend and login screens done
  (ADR-017, API contract in `docs/07-contrato-api.md`); the layout per role and the
  account screens are pending.

## 7. Structure of this repository

```
SIERAP/
├── README.md                          ← this file, entry point
├── docs/
│   ├── 00-guia-oficial-momento1.md    ← official UPC rubric and requirements (Spanish transcription)
│   ├── 01-planteamiento-problema.md   ← problem, causes, consequences
│   ├── 02-descripcion-solucion.md     ← scope, differentiating factor, stack
│   ├── 03-entidades-dominio.md        ← list and description of classes (+ Spanish → English names)
│   ├── 04-requerimientos-funcionales.md ← 12 requirements by role (formalized brainstorming)
│   ├── 05-decisiones-tecnicas.md      ← ADRs: why Java/Spring/Postgres/React and the rest
│   ├── 06-plan-de-trabajo.md          ← stages, split among the 3 devs, branches and tests
│   ├── 07-contrato-api.md             ← API contract (routes, DTOs, error codes)
│   ├── diagramas/                     ← class, hierarchy and layer diagrams (Mermaid + .drawio + PNG)
│   ├── wireframes/                    ← GUI wireframes per role (HTML + PNG for the delivery document)
│   └── entregas/                      ← formal delivery documents (includes presentacion-sierap.pptx)
├── backend/                           ← Java 21 + Spring Boot 3 API (Maven)
│   └── src/main/java/co/edu/unicesar/sierap/
│       ├── controller/  dto/  mapper/          ← presentation (REST API)
│       ├── service/  service/impl/             ← business logic
│       ├── repository/                         ← data (Spring Data JPA)
│       ├── entity/  entity/enums/              ← domain entities
│       └── security/  exception/  config/      ← cross-cutting
├── frontend/                          ← React + TypeScript app (Vite, Tailwind, shadcn/ui)
├── docker-compose.yml                 ← PostgreSQL and Mailpit for local development
└── .github/workflows/ci.yml           ← CI: backend and frontend build and tests
```

## 8. How to run the project locally

**Requirements:** JDK 21, Node.js 22 or later and Docker (Docker Desktop on
Windows/macOS). There is no need to install Maven: the repo ships the Maven Wrapper
(`mvnw`).

```bash
# 1. Database (PostgreSQL 17 on localhost:5432, user/password: sierap/sierap) and
#    Mailpit (test mailbox: catches the emails, web inbox on http://localhost:8025)
docker compose up -d

# 2. Backend — API on http://localhost:8080   (Windows PowerShell: .\mvnw.cmd spring-boot:run)
cd backend
./mvnw spring-boot:run

# 3. Frontend — in another terminal, app on http://localhost:5173
cd frontend
npm install
npm run dev
```

If everything is fine, the home page shows **"API conectada"** and **"Base de datos
disponible"**. It can also be checked with `curl http://localhost:8080/api/status`.

**First login (ADR-017):** the migration creates the first super_admin,
`sierap@unicesar.edu.co`, with the temporary password **`Sierap-Inicio-2026`**. On the
first login the only thing allowed is changing it (`POST /api/auth/password/change`).
From there the super_admin creates the admins, and so on down the chain. New accounts
receive an email with a link to create their password: in development those emails
land in Mailpit (http://localhost:8025). The API is described in
`docs/07-contrato-api.md`.

> **Production:** this temporary password is public (it is in this repository). Change
> it **right after the first deployment**, before announcing the URL: until then,
> whoever logs in first takes the super_admin account.

**Tests:**

```bash
cd backend && ./mvnw verify                       # unit + integration (requires Docker)
cd frontend && npm run lint && npm test && npm run build
```

**Own configuration without touching versioned files:** the backend reads `DB_URL`,
`DB_USERNAME` and `DB_PASSWORD` from the environment; you can also create a
`backend/application-local.yml` (ignored by git) and start with
`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,local`. If port 5432 is already
taken by another PostgreSQL: `DB_PORT=5433 docker compose up -d` and
`DB_URL=jdbc:postgresql://localhost:5433/sierap`.

**Environment variables for production (`prod` profile):** besides the `DB_*` ones,

| Variable | What it is |
|---|---|
| `SIERAP_JWT_SECRET` | Key that signs the access tokens: random, at least 32 characters, never committed. The API refuses to start without it. In `dev` there is a development default. |
| `SIERAP_FRONTEND_URL` | Public URL of the frontend; the links sent by email point to it. |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | UPC SMTP server (in development, Mailpit on `localhost:1025` without credentials). |
| `MAIL_FROM` | Sender of the emails (default `SIERAP <no-reply@unicesar.edu.co>`). |
| `SIERAP_STORAGE_PATH` | Folder where the submission files are stored (ADR-015): outside the application folder, on persistent storage and backed up with the database. The API refuses to start without it. In `dev` it defaults to `~/sierap-files`. |

**Migrations:** the schema is created by Flyway at startup. A migration that is already
in `develop` is never edited; if your local database has an older version of a migration
that was still in review, reset it with `docker compose down -v && docker compose up -d`.

## 9. How to use this repository with Claude

- **Claude Projects (planning, team discussion):** upload the whole content of `/docs`
  as Project Knowledge. Any team member who opens a new conversation inside the
  Project will automatically have all this context.
- **Claude Code (implementation):** clone this repo locally and work from its root.
  The `CLAUDE.md` file at the root gives Claude Code the same context automatically in
  every session, without you having to repeat it.

## 10. Team documentation rules

As agreed, the team must document:

- Why the university adopted RAP assessment (institutional context).
- Everything done with AI assistance (to present the project to the teacher without
  ambiguity about authorship/process).
- The software itself (architecture, decisions, progress).

Every relevant technical decision must be recorded in
[`docs/05-decisiones-tecnicas.md`](docs/05-decisiones-tecnicas.md) with its rationale,
following the Architecture Decision Record (ADR) format.
