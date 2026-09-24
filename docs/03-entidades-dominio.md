# 03 — Domain entities

The domain classes identified for the solution are listed below, grouped by
functional area. Diagrams:

- **Current class diagram (Stage 1):** [`diagramas/diagrama_clases.md`](diagramas/diagrama_clases.md)
  (Mermaid, rendered directly on GitHub). It is the Stage 1 contract.
- Improved class diagram from Phase 2: [PNG](diagramas/diagrama_clases_mejorado.png) ·
  [editable .drawio](diagramas/diagrama_clases_mejorado.drawio). **Outdated**
  relative to the 2026-09-24 decisions: it is regenerated when Stage 1 closes, from
  what ends up in the code.
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
| TipoDocumento | `DocumentType` | — |
| Grupo (= proyecto de aula) | `ProjectGroup` | `project_group` (`group` is reserved in SQL) |
| Entrega | `Submission` | `submission` |
| Evaluacion | `Evaluation` | `evaluation` |
| CalificacionCriterio | `CriterionGrade` | `criterion_grade` |
| Reporte (Individual / Grupal / Comparativo) | `Report` (`IndividualReport` / `GroupReport` / `ComparativeReport`) | — |

The D2 names are already in the code. The D1 and D3 names are the proposal for their
branches and should be confirmed by whoever implements them.

## Users (inheritance hierarchy) · D1

Persisted with `@Inheritance(JOINED)`: an `app_user` table with the common data and
one table per role (ADR-009). Every user logs in with their **email**.

| Class | Spanish name | Attributes | Description |
|---|---|---|---|
| `User` (abstract) | Usuario | firstNames, lastNames, documentType, documentNumber (unique), email (unique, login), passwordHash, role, active, createdAt | Data common to every user of the system. The five roles inherit from it. |
| `SuperAdmin` | Super administrador | — | Global permissions: system parameters, academic periods, faculties and the evaluator type catalog. |
| `Admin` | Administrador (decano) | faculty | User of a `Faculty` (dean role). Creates and configures the `Moment`s of the programs of their faculty and **launches** them. |
| `Teacher` | Docente | faculty | Belongs to **one** `Faculty` (SIERAP only includes teachers with a classroom project, and they teach in their faculty). **Enables** groups in a moment, creates and assigns external evaluators to them, sets their dates and grades **only the groups they enabled**. |
| `Student` | Estudiante | studentCode (unique), currentSemester, program | Belongs to a `Program`. Sees the groups of **all** the teachers of a moment of their program and joins one (**one group per moment**, with history). Uploads submissions and checks their performance per RAP. |
| `ExternalEvaluator` | Evaluador externo | evaluatorType, organization, position, graduationYear, graduationProgram (the last four optional) | Evaluator who is not a teacher (today business people and alumni). Created by a teacher; grades only the groups they were assigned to (ADR-011). |
| `EvaluatorType` | Tipo de evaluador | name, description, active | Catalog of external evaluator types maintained by the super_admin (seed data: "Empresario" and "Egresado"). Creating a new type requires no programming. |

**`Evaluator` interface:** common contract implemented by `Teacher` and
`ExternalEvaluator`: both grade groups and query the groups assigned to them, even
though they do not share a parent of their own for that (both inherit from `User`,
but "evaluating" is a behavior that cuts across different roles: a natural case for
an interface). `Student` does **not** implement `Evaluator`: there is no
self-assessment in SIERAP.

## Academic structure and assessment configuration · D2

The whole assessment configuration is defined **per academic period** and managed by
the admin of each faculty; nothing is fixed in the code (ADR-010). When a period is
opened, the configuration of the previous period is copied as a starting point.

| Class | Spanish name | Attributes | Description |
|---|---|---|---|
| `Faculty` | Facultad | code (unique, e.g. `04`), name | Top-level academic unit of the university; groups one or more `Program`s. |
| `Program` | Programa | code (unique, e.g. `108`), name, faculty | Academic program (e.g. Ingeniería de Sistemas) that belongs to a `Faculty`. |
| `AcademicPeriod` | Periodo académico | code (e.g. 2026-2), startDate, endDate, upcEvaluationDeadline | Academic semester with the evaluation deadline set by the UPC. Allows validating dates (RF 9) and comparing results across semesters and years (RF 12). |
| `Moment` | Momento | program, period, number, name, subject, semesterFrom, semesterTo, requirementLevel (free text), minMembers, maxMembers, status, groupFormationStart, groupFormationEnd, submissionStart, submissionEnd, gradingStart, gradingEnd | Assessment stage of a `Program` **in a period**. It has a lifecycle (ADR-012): in `CONFIGURATION` the admin defines its RAP, criteria, levels, members per group and date windows, and students form groups; once **launched** (`LAUNCHED`) the configuration and groups are locked and the windows mark when work is submitted and when it is graded. |
| `LearningOutcome` | RAP (Resultado de Aprendizaje del Programa) | moment, code, description | RAP assessed in a `Moment`. Each moment has its own RAP, which the admin creates, edits or deletes while the moment is in configuration. It weighs the sum of its criteria. |
| `EvaluationCriterion` | Criterio de evaluación | learningOutcome, description, displayOrder | Grading item within a RAP (e.g. "Identificación del problema" in the UPC rubric). |
| `PerformanceLevel` | Nivel de desempeño | criterion, name, description, score (`BigDecimal`, ADR-013), displayOrder | Level the evaluator can pick in a criterion (e.g. Bajo 0 · Básico 0.36 · Alto 0.48 · Sobresaliente 0.6). Names, number of levels and scores are defined by the admin for each criterion. |
| `GlobalParameter` | Parámetro global | key, value, description | Institutional key/value parameter defined by the `SuperAdmin` (RF 11), so general rules are not hard-coded. |

## Operations · D3

| Class | Spanish name | Attributes | Description |
|---|---|---|---|
| `ProjectGroup` | Grupo (= proyecto de aula) | moment, teacher, name, projectTitle, members, evaluators, submissionDeadline, evaluationDate | Group **and** classroom project at the same time (each group has a single project). A `Teacher` enables it in a moment; students join it (between the minimum and maximum of the moment). The teacher assigns external evaluators to it and sets its dates within the moment windows. |
| `Submission` | Entrega | projectGroup, versionNumber, zipFilePath, presentationPath, submittedAt | Version of the group's submission (zip file + presentation). Each upload is a new version; history is kept. |

## Evaluation and reporting · D3

| Class | Spanish name | Description |
|---|---|---|
| `Evaluation` | Evaluación | Grade that an `Evaluator` (the group's teacher or an assigned external evaluator) gives a `ProjectGroup`; groups one `CriterionGrade` per criterion of the moment. |
| `CriterionGrade` | Calificación por criterio | Performance level the evaluator picked in an `EvaluationCriterion`, with its score. Performance per RAP is the sum of that RAP's criteria, and the evaluation grade is the sum of all of them. |
| `Report` (abstract) | Reporte | Report generated from the recorded `Evaluation`s, filterable by semester, moment, program or year. Defines an abstract `generate()`. |
| `IndividualReport` / `GroupReport` / `ComparativeReport` | Reporte individual / grupal / comparativo | Subclasses of `Report`: each implements its own `generate()` (per student and RAP, per group, or comparing two periods). |

## Enumerations

| Enum | Values | Description |
|---|---|---|
| `UserRole` | `SUPER_ADMIN`, `ADMIN`, `TEACHER`, `STUDENT`, `EXTERNAL_EVALUATOR` | Role of the user; it is also the discriminator of the JOINED inheritance and the basis of access control (Spring Security). |
| `DocumentType` | `CC`, `TI`, `CE`, `PASSPORT` | Identity document type of the user (citizenship ID, identity card, foreigner ID, passport). |
| `MomentStatus` | `CONFIGURATION`, `LAUNCHED` | Lifecycle status of the moment (ADR-012). It is system flow, not an assessment business value. |

The Phase 2 enums `NivelExigencia` and `NivelDesempeno` **were removed** (the
requirement level is free text in `Moment` and performance levels are the
configurable `PerformanceLevel` entity, ADR-010), and so were the `EMPRESARIO` and
`EGRESADO` roles (they are now types of `ExternalEvaluator`, ADR-011).

## Relationships shown in the diagram

- **Inheritance:** `SuperAdmin`, `Admin`, `Teacher`, `Student` and `ExternalEvaluator`
  inherit from `User`.
- **Interface realization:** `Teacher` and `ExternalEvaluator` implement `Evaluator`.
- **Composition:** `Faculty`—`Program`; `Program`—`Moment`; `Moment`—`LearningOutcome`;
  `LearningOutcome`—`EvaluationCriterion`; `EvaluationCriterion`—`PerformanceLevel`;
  `Moment`—`ProjectGroup`; `ProjectGroup`—`Submission`; `ProjectGroup`—`Evaluation`;
  `Evaluation`—`CriterionGrade` (the whole controls the lifecycle of its parts).
- **Aggregation:** `ProjectGroup`—`Student` (the student exists without the group
  and goes through several groups during the program, one per moment).
- **Association:** `Moment`—`AcademicPeriod`; `Admin`—`Faculty`; `Teacher`—`Faculty`;
  `Teacher`—`ProjectGroup` (the teacher who enabled it);
  `ProjectGroup`—`ExternalEvaluator` (assigned evaluators);
  `ExternalEvaluator`—`EvaluatorType`; `Student`—`Program`;
  `ExternalEvaluator`—`Program` (graduation program, optional);
  `CriterionGrade`—`EvaluationCriterion`; `CriterionGrade`—`PerformanceLevel`.
- **Dependency:** `Report` depends on `Evaluation` (the report consumes evaluation
  data without owning it).

## Still to be defined

See "Still to be defined with the team" in
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md).
