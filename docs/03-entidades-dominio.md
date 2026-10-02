# 03 — Domain entities

The domain classes identified for the solution are listed below, grouped by
functional area. Diagrams:

- **Current class diagram (Stage 1):** [`diagramas/diagrama_clases.md`](diagramas/diagrama_clases.md)
  (Mermaid, rendered directly on GitHub). It is the Stage 1 contract.
- **Detailed class diagram** (same model, on one sheet, for the delivery document):
  [PNG](diagramas/diagrama_clases_mejorado.png) · [SVG](diagramas/diagrama_clases_mejorado.svg) ·
  [editable .drawio](diagramas/diagrama_clases_mejorado.drawio). Regenerated when Stage 1
  closed (2026-09-28) from the code, plus what is designed but not built yet (marked
  with a dashed border).
- **Hierarchy diagram:** [`diagramas/diagrama_jerarquias.md`](diagramas/diagrama_jerarquias.md).
- **Layer (package) diagram:** [`diagramas/diagrama_capas.md`](diagramas/diagrama_capas.md).
- Preliminary class diagram (Phase 1): [`diagramas/diagrama_clases.drawio`](diagramas/diagrama_clases.drawio)
  (open it in [app.diagrams.net](https://app.diagrams.net)).

The decisions that shaped this model are in
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md) ("Flow of a
moment" and "Team decisions" sections) and in ADR-009 to ADR-014 of
[`05-decisiones-tecnicas.md`](05-decisiones-tecnicas.md).

> **Language (ADR-014):** code and documentation are in English; what the end user
> sees (screens, API error messages, data such as RAP names) and the Javadoc and code
> comments are in Spanish. The "Spanish name" column gives the name of each concept
> in the domain language of the university.

## Spanish → English names

| Spanish (domain) | English (code) | Table |
|---|---|---|
| Facultad | `Faculty` | `faculty` |
| Programa | `Program` | `program` |
| PeriodoAcademico | `AcademicPeriod` | `academic_period` |
| ParametroGlobal | `GlobalParameter` | `global_parameter` |
| Momento | `Moment` | `moment` |
| EstadoMomento (CONFIGURACION, LANZADO) | `MomentStatus` (`CONFIGURATION`, `LAUNCHED`) | column `moment.status` |
| PesoEvaluador | `EvaluatorWeight` | `evaluator_weight` (the teacher's weight is the column `moment.teacher_weight`) |
| RAP | `LearningOutcome` | `learning_outcome` |
| CriterioEvaluacion | `EvaluationCriterion` | `evaluation_criterion` |
| NivelDesempeno | `PerformanceLevel` | `performance_level` |
| Usuario | `User` | `app_user` (`user` is reserved in PostgreSQL) |
| SuperAdmin / Admin | `SuperAdmin` / `Admin` | `super_admin` / `admin` |
| Docente | `Teacher` | `teacher` |
| Estudiante | `Student` | `student` |
| EvaluadorExterno | `ExternalEvaluator` | `external_evaluator` |
| TipoEvaluador | `EvaluatorType` | `evaluator_type` |
| Evaluador (interfaz) | `Evaluator` (interface) | — |
| RolUsuario | `UserRole` | column `app_user.role` |
| CodigoRegistro | `RegistrationCode` | `registration_code` |
| TokenRenovacion (sesión) | `RefreshToken` | `refresh_token` |
| EnlaceContrasena | `PasswordToken` | `password_token` |
| PropositoEnlace (CREAR_CUENTA, RECUPERAR) | `PasswordTokenPurpose` (`ACCOUNT_SETUP`, `PASSWORD_RESET`) | column `password_token.purpose` |
| TipoDocumento | `DocumentType` | — |
| Grupo (= proyecto de aula) | `ProjectGroup` | `project_group` (`group` is reserved in SQL) |
| Entrega | `Submission` | `submission` |
| Evaluacion | `Evaluation` | `evaluation` |
| CalificacionCriterio | `CriterionGrade` | `criterion_grade` |
| AjusteIndividual | `IndividualGrade` | `individual_grade` |
| Reporte (Individual / Grupal / Comparativo) | `Report` (`IndividualReport` / `GroupReport` / `ComparativeReport`) | — |

The D1 and D2 names are already in the code. The D3 names are the proposal for its
branch and should be confirmed by whoever implements it. The `ExternalEvaluator`
attribute `position` is stored in the column `job_position`, because `position` is a
reserved word in standard SQL (ADR-014).

## Users (inheritance hierarchy) · D1

Persisted with `@Inheritance(JOINED)`: an `app_user` table with the common data and
one table per role (ADR-009). Every user logs in with their **email**.

| Class | Spanish name | Attributes | Description |
|---|---|---|---|
| `User` (abstract) | Usuario | firstNames, lastNames, documentType, documentNumber (unique), email (unique, login), passwordHash, role, active, mustChangePassword, createdBy (optional), createdAt | Data common to every user of the system. The five roles inherit from it. `passwordHash` is empty until the person creates their password with the emailed link; `mustChangePassword` forces a temporary password to be changed; `createdBy` is the user who created the account, and it sets who can manage it (ADR-017). |
| `SuperAdmin` | Super administrador | — | Global permissions: system parameters, academic periods, faculties and the evaluator type catalog. |
| `Admin` | Administrador (decano) | faculty | User of a `Faculty` (dean role); a faculty can have **several** admins, all with the same permissions. Creates and configures the `Moment`s of the programs of their faculty and **launches** them. |
| `Teacher` | Docente | faculty | Belongs to **one** `Faculty` (SIERAP only includes teachers with a classroom project, and they teach in their faculty). **Enables** groups in a moment, creates and assigns external evaluators to them, sets their dates and grades **only the groups they enabled**. |
| `Student` | Estudiante | studentCode (unique), currentSemester, program, registrationCode (optional) | Belongs to a `Program`. Created by a teacher (or a role above), or self-registered with a teacher's `RegistrationCode`, which is kept to know how they joined. Sees the groups of **all** the teachers of a moment of their program and joins one (**one group per moment**, with history). Uploads submissions and checks their performance per RAP. |
| `ExternalEvaluator` | Evaluador externo | evaluatorType, organization, position, graduationYear, graduationProgram (the last four optional) | Evaluator who is not a teacher (today business people and alumni). Created by a teacher; grades only the groups they were assigned to (ADR-011). |
| `EvaluatorType` | Tipo de evaluador | name, description, active | Catalog of external evaluator types maintained by the super_admin (seed data: "Empresario" and "Egresado"). Creating a new type requires no programming. |

**`Evaluator` interface:** common contract implemented by `Teacher` and
`ExternalEvaluator`: both grade groups, even though they do not share a parent of
their own for that (both inherit from `User`, but "evaluating" is a behavior that cuts
across different roles: a natural case for an interface). `Student` does **not**
implement `Evaluator`: there is no self-assessment in SIERAP.

- `canEvaluate(ProjectGroup)` is **abstract** and each class implements its own rule
  (polymorphism): a `Teacher` can evaluate the groups they enabled; an
  `ExternalEvaluator`, the groups they were assigned to. Entities are compared by id,
  because lazy relations may be Hibernate proxies.
- Dates are not part of the interface: the service checks the grading window and the
  evaluation date with the injected `Clock`.
- `Evaluation.evaluator` is typed as `User`, because JPA cannot map an interface; the
  service checks that the user is an `Evaluator` that can evaluate the group.

## Authentication and accounts · D1 (Stage 2, ADR-017)

| Class | Spanish name | Attributes | Description |
|---|---|---|---|
| `RegistrationCode` | Código de registro | code (unique, 8 characters), teacher, program, expiresOn, active, createdAt | Code a teacher shares with their students so they register themselves in a `Program` of the teacher's faculty. Reusable until it expires or is deactivated; a deactivated code cannot be reactivated. |
| `RefreshToken` | Token de renovación (sesión) | user, tokenHash (unique), expiresAt, rotatedAt, createdAt | One login session. Only the SHA-256 hash of the cookie value is stored. Each renewal marks it as rotated and creates a new one; if a rotated token comes back, it was probably stolen and every session of the user is closed. Logging out, changing the password or deactivating the account delete the rows. |
| `PasswordToken` | Enlace de contraseña | user, tokenHash (unique), purpose, expiresAt, usedAt, createdAt | Single-use link sent by email to create the password of a new account (72 h) or recover a forgotten one (1 h). Issuing a new link invalidates the pending ones of the same purpose. |

The first `SuperAdmin` (`sierap@unicesar.edu.co`) is created by the migration with a
temporary password that must be changed on the first login.

## Academic structure and assessment configuration · D2

The whole assessment configuration is defined **per academic period** and managed by
the admin of each faculty; nothing is fixed in the code (ADR-010). When a period is
opened, the configuration of the previous period is copied as a starting point.

| Class | Spanish name | Attributes | Description |
|---|---|---|---|
| `Faculty` | Facultad | code (unique, e.g. `04`), name | Top-level academic unit of the university; groups one or more `Program`s. |
| `Program` | Programa | code (unique, e.g. `108`), name, faculty | Academic program (e.g. Ingeniería de Sistemas) that belongs to a `Faculty`. |
| `AcademicPeriod` | Periodo académico | code (e.g. 2026-2), startDate, endDate, upcEvaluationDeadline | Academic semester with the evaluation deadline set by the UPC. Allows validating dates (RF 9) and comparing results across semesters and years (RF 12). |
| `Moment` | Momento | program, period, number, name, subject, semesterFrom, semesterTo, requirementLevel (free text), minMembers, maxMembers, status, groupFormationStart, groupFormationEnd, submissionStart, submissionEnd, gradingStart, gradingEnd, teacherWeight, evaluatorWeights | Assessment stage of a `Program` **in a period**. It has a lifecycle (ADR-012): in `CONFIGURATION` the admin defines its RAP, criteria, levels, members per group and date windows, and students form groups; once **launched** (`LAUNCHED`) the configuration and groups are locked and the windows mark when work is submitted and when it is graded. The status only changes through `launch()`, which checks that the configuration is complete (the three windows ordered without overlapping, grading ending no later than the UPC deadline, at least one RAP whose criteria all have levels, and the weights of the teacher and of the evaluator types adding up to exactly 100 %); after it, the configuration setters reject changes. The database backs these rules with `CHECK` constraints (V5). |
| `EvaluatorWeight` | Peso de un tipo de evaluador | moment, evaluatorType, weight | Weight of an `EvaluatorType` in the final grade of a `Moment` (ADR-018), as a percentage greater than 0 and up to 100 with two decimals. The teacher's weight is `Moment.teacherWeight` (greater than 0, 100 by default) because every moment has a teacher. A type without weight in the moment takes no part in it: no external evaluators of that type can be assigned to its groups. The grade calculation reads `getTeacherWeight()` and `getWeightFor(type)` (zero if the type has no weight). Composed by `Moment` (V9). |
| `LearningOutcome` | RAP (Resultado de Aprendizaje del Programa) | moment, code, description | RAP assessed in a `Moment`. Each moment has its own RAP, which the admin creates, edits or deletes while the moment is in configuration. It weighs the sum of its criteria. |
| `EvaluationCriterion` | Criterio de evaluación | learningOutcome, description, displayOrder | Grading item within a RAP (e.g. "Identificación del problema" in the UPC rubric). |
| `PerformanceLevel` | Nivel de desempeño | criterion, name, description, score (`BigDecimal`, ADR-013), displayOrder | Level the evaluator can pick in a criterion (e.g. Bajo 0 · Básico 0.36 · Alto 0.48 · Sobresaliente 0.6). Names, number of levels and scores are defined by the admin for each criterion. |
| `GlobalParameter` | Parámetro global | key, value, description | Institutional key/value parameter defined by the `SuperAdmin` (RF 11), so general rules are not hard-coded. |

## Operations · D3

| Class | Spanish name | Attributes | Description |
|---|---|---|---|
| `ProjectGroup` | Grupo (= proyecto de aula) | moment, teacher, name, projectTitle, members, evaluators, submissionDeadline, evaluationDate | Group **and** classroom project at the same time (each group has a single project). A `Teacher` enables it in a moment with a `name`; students of the program whose current semester is within the moment's range join it (between the minimum and maximum of the moment). The members write the `projectTitle` (the teacher can edit it too) until the submission window ends. The teacher assigns external evaluators to it and sets its dates within the moment windows; after the launch they can still change the dates and the evaluators while the grading window is open. |
| `Submission` | Entrega | projectGroup, versionNumber, zipFilePath, zipFileName, zipSize, presentationPath, presentationFileName, presentationSize (the three optional), submittedAt, late | Version of the group's submission: the `.zip` is required and the presentation optional (when a version has none, the latest presentation of an earlier version is shown). Each upload is a new version; history is kept. Uploads are accepted while the submission window is open; those after the group's deadline are marked `late`. Files are stored on the server disk with a name generated by the backend; the original name and the size are kept only to show them and for the download (ADR-015, V8). |

## Evaluation and reporting · D3

| Class | Spanish name | Description |
|---|---|---|
| `Evaluation` | Evaluación | Grade that an `Evaluator` (the group's teacher or an assigned external evaluator) gives a `ProjectGroup`; groups one `CriterionGrade` per criterion of the moment. It has a `status` (`DRAFT` → `SUBMITTED`): the evaluator can save it half done and submit it later; only submitted evaluations count, and they can be corrected while the grading window is open. One evaluation per evaluator per group. Attributes: projectGroup, evaluator (`User`: JPA cannot map the `Evaluator` interface), status, evaluatedAt, comment (optional). |
| `CriterionGrade` | Calificación por criterio | Performance level the evaluator picked in an `EvaluationCriterion` for the **whole group**, with a **copy of its score** taken when grading. Performance per RAP is the sum of that RAP's criteria, and the evaluation grade is the sum of all of them. Attributes: evaluation, criterion, level, score. One grade per criterion in each evaluation. |
| `IndividualGrade` | Ajuste individual | Level an evaluator gives **one student** in one criterion when it differs from the group's level (e.g. that student did better in the presentation). By default every member inherits the group's levels; an `IndividualGrade` replaces the group's level only for that student and that criterion, so their individual grade differs from the others' (ADR-018). Attributes: evaluation, student (a member of the group), criterion, level, score (copy). At most one per student and criterion in each evaluation. |
| `Report` (abstract) | Reporte | Report generated from the recorded `Evaluation`s, filterable by semester, moment, program or year. Defines an abstract `generate()`. |
| `IndividualReport` / `GroupReport` / `ComparativeReport` | Reporte individual / grupal / comparativo | Subclasses of `Report`: each implements its own `generate()` (per student and RAP, per group, or comparing two periods). |

**Group grade and individual grade (ADR-018):** the group is graded once. The
**group grade** uses the group's levels only; the **individual grade** of each member
uses the same levels except the criteria where an evaluator adjusted that student
(`IndividualGrade`). Without adjustments both are equal. The individual grade is the
one each student sees as their final grade; both feed the reports.

**Final grade formula** (applies to the group grade and to each individual grade):
weighted average by kind of evaluator (the teacher and each
`EvaluatorType`), with weights the admin sets in each moment (to be modeled by D2 in
Stage 3, together with the rest of the moment configuration). Several evaluators of
the same kind are averaged within that kind; if a kind has no submitted evaluation,
its weight is redistributed among the present kinds in proportion to their weights.
The graded submission is the last on-time version, or the last late one if there is
none on time.

## Enumerations

| Enum | Values | Description |
|---|---|---|
| `UserRole` | `SUPER_ADMIN`, `ADMIN`, `TEACHER`, `STUDENT`, `EXTERNAL_EVALUATOR` | Role of the user; it is also the discriminator of the JOINED inheritance and the basis of access control (Spring Security). |
| `DocumentType` | `CC`, `TI`, `CE`, `PASSPORT` | Identity document type of the user (citizenship ID, identity card, foreigner ID, passport). |
| `MomentStatus` | `CONFIGURATION`, `LAUNCHED` | Lifecycle status of the moment (ADR-012). It is system flow, not an assessment business value. |
| `EvaluationStatus` | `DRAFT`, `SUBMITTED` | Status of an evaluation: saved half done or submitted. System flow, like `MomentStatus`. |
| `PasswordTokenPurpose` | `ACCOUNT_SETUP`, `PASSWORD_RESET` | Why a password link was sent: to create the password of a new account or to recover a forgotten one. |

The Phase 2 enums `NivelExigencia` and `NivelDesempeno` **were removed** (the
requirement level is free text in `Moment` and performance levels are the
configurable `PerformanceLevel` entity, ADR-010), and so were the `EMPRESARIO` and
`EGRESADO` roles (they are now types of `ExternalEvaluator`, ADR-011).

## Relationships shown in the diagram

- **Inheritance:** `SuperAdmin`, `Admin`, `Teacher`, `Student` and `ExternalEvaluator`
  inherit from `User`.
- **Interface realization:** `Teacher` and `ExternalEvaluator` implement `Evaluator`.
- **Composition:** `Faculty`—`Program`; `Program`—`Moment`; `Moment`—`LearningOutcome`; `Moment`—`EvaluatorWeight`;
  `LearningOutcome`—`EvaluationCriterion`; `EvaluationCriterion`—`PerformanceLevel`;
  `Moment`—`ProjectGroup`; `ProjectGroup`—`Submission`; `ProjectGroup`—`Evaluation`;
  `Evaluation`—`CriterionGrade`; `Evaluation`—`IndividualGrade`; `User`—`RefreshToken`;
  `User`—`PasswordToken` (the whole controls the lifecycle of its parts).
- **Aggregation:** `ProjectGroup`—`Student` (the student exists without the group
  and goes through several groups during the program, one per moment).
- **Association:** `Moment`—`AcademicPeriod`; `Admin`—`Faculty` (several admins per faculty); `Teacher`—`Faculty`;
  `Teacher`—`ProjectGroup` (the teacher who enabled it);
  `ProjectGroup`—`ExternalEvaluator` (assigned evaluators);
  `ExternalEvaluator`—`EvaluatorType`; `Student`—`Program`;
  `ExternalEvaluator`—`Program` (graduation program, optional);
  `CriterionGrade`—`EvaluationCriterion`; `CriterionGrade`—`PerformanceLevel`;
  `IndividualGrade`—`Student`; `IndividualGrade`—`EvaluationCriterion`;
  `IndividualGrade`—`PerformanceLevel`;
  `User`—`User` (`createdBy`); `RegistrationCode`—`Teacher`; `RegistrationCode`—`Program`;
  `Student`—`RegistrationCode` (optional).
- **Dependency:** `Report` depends on `Evaluation` (the report consumes evaluation
  data without owning it).

## Still to be defined

See "Still to be defined with the team" in
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md).
