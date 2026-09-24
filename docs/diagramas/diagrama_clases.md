# Class diagram (current · Stage 1)

SIERAP domain model with inheritance, abstract classes, interfaces, enumerations and
composition, aggregation, association and dependency relationships. GitHub renders
this diagram directly from the Mermaid code.

> **This is the current diagram** and the Stage 1 contract: it includes the
> 2026-09-24 decisions (see `docs/04`, "Team decisions" section, and ADR-009 to
> ADR-014). Class and attribute names are in English (ADR-014); the Spanish → English
> name table is in [`../03-entidades-dominio.md`](../03-entidades-dominio.md). The
> detailed-design version ([`diagrama_clases_mejorado.png`](diagrama_clases_mejorado.png)
> · editable in [`diagrama_clases_mejorado.drawio`](diagrama_clases_mejorado.drawio))
> belongs to Phase 2 and is regenerated when Stage 1 closes. The Phase 1 preliminary
> diagram is still in [`diagrama_clases.drawio`](diagrama_clases.drawio).

```mermaid
classDiagram
direction TB

%% ================= Users and roles (D1) =================
class User {
  <<abstract>>
  -id: Long
  -firstNames: String
  -lastNames: String
  -documentType: DocumentType
  -documentNumber: String
  -email: String
  -passwordHash: String
  -active: boolean
  -createdAt: LocalDateTime
  +getRole()* UserRole
  +getFullName() String
  +isActive() boolean
}
class Evaluator {
  <<interface>>
  +grade(group: ProjectGroup) Evaluation
  +getAssignedGroups() List~ProjectGroup~
  +canEvaluate(group: ProjectGroup, today: LocalDate) boolean
}
class UserRole {
  <<enumeration>>
  SUPER_ADMIN
  ADMIN
  TEACHER
  STUDENT
  EXTERNAL_EVALUATOR
}
class DocumentType {
  <<enumeration>>
  CC
  TI
  CE
  PASSPORT
}
class SuperAdmin {
  +getRole() UserRole
}
class Admin {
  -faculty: Faculty
  +getRole() UserRole
}
class Teacher {
  -faculty: Faculty
  +getRole() UserRole
}
class Student {
  -studentCode: String
  -currentSemester: int
  -program: Program
  +getRole() UserRole
}
class ExternalEvaluator {
  -evaluatorType: EvaluatorType
  -organization: String
  -position: String
  -graduationYear: Integer
  -graduationProgram: Program
  +getRole() UserRole
}
class EvaluatorType {
  -id: Long
  -name: String
  -description: String
  -active: boolean
}

%% ================= Academic structure and configuration (D2) =================
class GlobalParameter {
  -key: String
  -value: String
  -description: String
}
class Faculty {
  -id: Long
  -code: String
  -name: String
  -programs: List~Program~
}
class Program {
  -id: Long
  -code: String
  -name: String
  -faculty: Faculty
  -moments: List~Moment~
}
class AcademicPeriod {
  -id: Long
  -code: String
  -startDate: LocalDate
  -endDate: LocalDate
  -upcEvaluationDeadline: LocalDate
  +contains(date: LocalDate) boolean
}
class Moment {
  -id: Long
  -number: int
  -name: String
  -subject: String
  -semesterFrom: int
  -semesterTo: int
  -requirementLevel: String
  -minMembers: int
  -maxMembers: int
  -status: MomentStatus
  -groupFormationStart: LocalDate
  -groupFormationEnd: LocalDate
  -submissionStart: LocalDate
  -submissionEnd: LocalDate
  -gradingStart: LocalDate
  -gradingEnd: LocalDate
  -program: Program
  -period: AcademicPeriod
  -learningOutcomes: List~LearningOutcome~
  +launch() void
  +isInGroupFormation(today: LocalDate) boolean
  +isInSubmission(today: LocalDate) boolean
  +isInGrading(today: LocalDate) boolean
  +getMaxScore() BigDecimal
}
class MomentStatus {
  <<enumeration>>
  CONFIGURATION
  LAUNCHED
}
class LearningOutcome {
  -id: Long
  -code: String
  -description: String
  -criteria: List~EvaluationCriterion~
  +getMaxScore() BigDecimal
}
class EvaluationCriterion {
  -id: Long
  -description: String
  -displayOrder: int
  -levels: List~PerformanceLevel~
  +getMaxScore() BigDecimal
}
class PerformanceLevel {
  -id: Long
  -name: String
  -description: String
  -score: BigDecimal
  -displayOrder: int
}

%% ================= Operations (D3) =================
class ProjectGroup {
  -id: Long
  -name: String
  -projectTitle: String
  -moment: Moment
  -teacher: Teacher
  -members: List~Student~
  -evaluators: List~ExternalEvaluator~
  -submissionDeadline: LocalDate
  -evaluationDate: LocalDate
  +enroll(s: Student) void
  +isFull() boolean
  +getLatestSubmission() Submission
  +getFinalGrade() BigDecimal
}
class Submission {
  -id: Long
  -versionNumber: int
  -zipFilePath: String
  -presentationPath: String
  -submittedAt: LocalDateTime
  +wasOnTime(deadline: LocalDate) boolean
}

%% ================= Evaluation and reporting (D3) =================
class Evaluation {
  -id: Long
  -date: LocalDateTime
  -comment: String
  -grades: List~CriterionGrade~
  +calculateTotalGrade() BigDecimal
  +calculateGradeByOutcome(outcome: LearningOutcome) BigDecimal
}
class CriterionGrade {
  -id: Long
  -criterion: EvaluationCriterion
  -selectedLevel: PerformanceLevel
  -score: BigDecimal
}
class Report {
  <<abstract>>
  -generatedAt: LocalDateTime
  -period: AcademicPeriod
  +generate()* byte[]
}
class IndividualReport {
  -student: Student
  +generate() byte[]
}
class GroupReport {
  -group: ProjectGroup
  +generate() byte[]
}
class ComparativeReport {
  -periodA: AcademicPeriod
  -periodB: AcademicPeriod
  +generate() byte[]
}

%% ================= Relationships =================
User <|-- SuperAdmin
User <|-- Admin
User <|-- Teacher
User <|-- Student
User <|-- ExternalEvaluator
Evaluator <|.. Teacher
Evaluator <|.. ExternalEvaluator
User ..> UserRole : «use»
User ..> DocumentType : «use»

SuperAdmin ..> GlobalParameter : «manages»
SuperAdmin ..> AcademicPeriod : «manages»
SuperAdmin ..> EvaluatorType : «manages»
Admin "1" -- "1" Faculty : administers
Teacher "*" -- "1" Faculty : belongs to
Student "*" -- "1" Program : studies
ExternalEvaluator "*" -- "1" EvaluatorType : is of type
ExternalEvaluator "*" -- "0..1" Program : graduated from

Faculty "1" *-- "1..*" Program
Program "1" *-- "*" Moment
Moment "*" -- "1" AcademicPeriod : configured in
Moment ..> MomentStatus : «use»
Moment "1" *-- "1..*" LearningOutcome
LearningOutcome "1" *-- "1..*" EvaluationCriterion
EvaluationCriterion "1" *-- "1..*" PerformanceLevel

Moment "1" *-- "*" ProjectGroup
Teacher "1" -- "*" ProjectGroup : enables
ProjectGroup "*" o-- "min..max" Student : one group per moment
ProjectGroup "*" -- "*" ExternalEvaluator : assigned
ProjectGroup "1" *-- "0..*" Submission

ProjectGroup "1" *-- "0..*" Evaluation
Evaluation "*" -- "1" Evaluator : made by
Evaluation "1" *-- "1..*" CriterionGrade
CriterionGrade "*" -- "1" EvaluationCriterion : grades
CriterionGrade "*" -- "1" PerformanceLevel : selected level

Report <|-- IndividualReport
Report <|-- GroupReport
Report <|-- ComparativeReport
Report ..> Evaluation : «use»
```

## Stage 1 changes (2026-09-24 decisions)

| Change | Reason |
|---|---|
| Every class, attribute and method moves to English (`Facultad` → `Faculty`, `Momento` → `Moment`, `RAP` → `LearningOutcome`, `CriterioEvaluacion` → `EvaluationCriterion`, `NivelDesempeno` → `PerformanceLevel`, `Grupo` → `ProjectGroup`…); the full table is in `docs/03` | Code and documentation in English; screens, error messages and Javadoc in Spanish (ADR-014). |
| `Faculty` gains `code` (unique, required) | Institutional code of the faculty (e.g. `04`), like `Program.code` (e.g. `108`). |
| Scores (`score`, `getMaxScore()`, grades) as `BigDecimal` instead of `double` | Exact sums: Moment 1 must add up to 5.00, not 4.9999… (ADR-013). |
| `User`: `name` → `firstNames` + `lastNames`; new `documentType` and `documentNumber`; `DocumentType` enum | Store the identity document besides the email, which is the login field. |
| `Empresario` and `Egresado` → a single `ExternalEvaluator` subclass with an `EvaluatorType` (catalog) | More evaluator types must be creatable without programming (ADR-011). |
| `Teacher` belongs to a `Faculty` | Only teachers with a classroom project use SIERAP, and they teach in their faculty. |
| `Student` loses `group` and gains `program` | A student has one group per moment with history; the relationship lives on the `ProjectGroup` side. |
| `Moment` belongs to an `AcademicPeriod`; `requirementLevel` becomes free text | The configuration is versioned per period and there are no standard levels (ADR-010). |
| `Moment` gains `status` (`MomentStatus`), min./max. members and three date windows | Moment lifecycle: configuration → launch → submissions → grading (ADR-012). |
| `LearningOutcome` (RAP) belongs to a `Moment` (composition) and loses its evaluative weight | Each moment has its own RAP; the weight comes from the scores of its criteria. |
| New `EvaluationCriterion` and `PerformanceLevel` (entity) | Structure of the UPC rubric: each RAP has criteria and each criterion levels with scores, all configurable. |
| Group and classroom project merge into `ProjectGroup`, which belongs to a `Moment` | Each group has a single project; the teacher enables groups within a moment. |
| Per-RAP grade → `CriterionGrade` | The evaluator picks one level per criterion; the performance per RAP is a sum. |
| The `NivelExigencia` and `NivelDesempeno` enums are removed | Nothing standard in the code (ADR-010). |
| No student self-assessment | `Student` does not implement `Evaluator`. |

## Changes compared with the preliminary diagram (Phase 1)

| Change | Reason |
|---|---|
| `AcademicPeriod` (new) | RF 9 requires validating that the evaluation date falls within the UPC range, and RF 12 compares results across semesters and years: both need the period as an entity. |
| `GlobalParameter` (new) | RF 11: the super_admin defines global parameters without hard-coding them. |
| `Report` becomes abstract with 3 subclasses | Each report type implements its own `generate()`: polymorphism instead of a `switch` by type. Resolves the Phase 1 open item about reporting. |
| Abstract `User.getRole()` | Each role overrides it; login leaves the domain and moves to `AuthService` (service layer). |
| Multiplicity `min..max` on `ProjectGroup`—`Student` | The number of members is configured per moment (Moment 1: 3 to 5). |
