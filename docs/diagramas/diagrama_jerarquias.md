# Hierarchy diagram

Four hierarchies that structure SIERAP. GitHub renders each block directly from the
Mermaid code.

> Detailed-design version, with the same four hierarchies on one sheet:
> [`diagrama_jerarquias.png`](diagrama_jerarquias.png) (also
> [SVG](diagrama_jerarquias.svg); editable in
> [`diagrama_jerarquias.drawio`](diagrama_jerarquias.drawio)), regenerated in English
> when Stage 1 closed. When a hierarchy changes, update this Mermaid code and the
> `.drawio` together.

## 1. Inheritance hierarchy — Users

Abstraction + inheritance + interface: the basis of the project's OOP.

```mermaid
classDiagram
direction BT
class User {
  <<abstract>>
  -id: Long
  -firstNames: String
  -lastNames: String
  -email: String
  -passwordHash: String
  -active: boolean
  +getRole()* UserRole
  +getFullName() String
}
class UserRole {
  <<enumeration>>
  SUPER_ADMIN
  ADMIN
  TEACHER
  STUDENT
  EXTERNAL_EVALUATOR
}
class Evaluator {
  <<interface>>
  +canEvaluate(group: ProjectGroup) boolean
}
class EvaluatorType {
  -id: Long
  -name: String
  -description: String
  -active: boolean
}
class SuperAdmin { +getRole() UserRole }
class Admin {
  -faculty: Faculty
  +getRole() UserRole
}
class Student {
  -studentCode: String
  -currentSemester: int
  -program: Program
  +getRole() UserRole
}
class Teacher {
  -faculty: Faculty
  +getRole() UserRole
  +canEvaluate(group: ProjectGroup) boolean
}
class ExternalEvaluator {
  -evaluatorType: EvaluatorType
  -organization: String
  -graduationYear: Integer
  +getRole() UserRole
  +canEvaluate(group: ProjectGroup) boolean
}

User ..> UserRole : «use»
SuperAdmin --|> User
Admin --|> User
Student --|> User
Teacher --|> User
ExternalEvaluator --|> User
Teacher ..|> Evaluator
ExternalEvaluator ..|> Evaluator
ExternalEvaluator "*" -- "1" EvaluatorType : is of type
```

- `User` is abstract: there is no "generic user", only the five concrete roles.
- `getRole()` is polymorphic: each subclass overrides it and Spring Security decides permissions with that value.
- `Teacher` and `ExternalEvaluator` grade groups, but they do not share a parent of
  their own for that: the common behavior is expressed with the `Evaluator` interface,
  and each one implements its own `canEvaluate()` rule (the teacher who enabled the
  group, the external evaluators assigned to it). Dates are not part of the interface:
  the service checks the grading window with the injected `Clock`.
- Business person, alumnus and the types created later are not classes: they are values
  of the `EvaluatorType` catalog of an `ExternalEvaluator` (ADR-011).
- `Student`, `Admin` and `SuperAdmin` do not grade, so they do not implement `Evaluator`.
- In JPA the inheritance is mapped with `@Inheritance(JOINED)`: an `app_user` table + one table per role (ADR-009).

## 2. Inheritance hierarchies — Reports and exceptions

Polymorphism in reporting and centralized error handling with `@RestControllerAdvice`.
The reports are designed but not built yet (Stage 7).

```mermaid
classDiagram
direction BT
class Report {
  <<abstract>>
  -generatedAt: LocalDateTime
  -period: AcademicPeriod
  +generate()* ReportData
}
class IndividualReport {
  -student: Student
  -group: ProjectGroup
  +generate() ReportData
}
class GroupReport {
  -group: ProjectGroup
  +generate() ReportData
}
class MomentReport {
  -moment: Moment
  +generate() ReportData
}
class ComparativeReport {
  -momentA: Moment
  -momentB: Moment
  -periodA: AcademicPeriod
  -periodB: AcademicPeriod
  +generate() ReportData
}
IndividualReport --|> Report : per student and RAP
GroupReport --|> Report : per group
MomentReport --|> Report : consolidated per moment
ComparativeReport --|> Report : semester X vs. Y
```

```mermaid
classDiagram
direction BT
class RuntimeException {
  <<java.lang>>
}
class SierapException {
  <<abstract>>
  #SierapException(message: String)
  +getHttpStatus()* HttpStatus
}
class GlobalExceptionHandler {
  <<RestControllerAdvice>>
  +handleSierapException(ex: SierapException)
}
class ResourceNotFoundException {
  HTTP 404
}
class ForbiddenAccessException {
  HTTP 403
}
class BusinessRuleException {
  HTTP 422
}
class AuthenticationFailedException {
  HTTP 401
}
SierapException --|> RuntimeException
ResourceNotFoundException --|> SierapException
ForbiddenAccessException --|> SierapException
BusinessRuleException --|> SierapException
AuthenticationFailedException --|> SierapException
GlobalExceptionHandler ..> SierapException : «handles»
```

Each subclass returns its own HTTP status from `getHttpStatus()`, and
`GlobalExceptionHandler` answers with the same `ErrorResponse` for all of them.

## 3. Institutional and academic hierarchy

Configuration (super_admin / admin) → operation of each moment (teacher, students, evaluators).

```mermaid
flowchart TD
  UPC["<b>Universidad Popular del Cesar</b><br/>institution · configured by the super_admin"]
  FAC["<b>Faculty</b><br/>managed by an admin (dean)"]
  PROG["<b>Program</b><br/>e.g. Ingeniería de Sistemas"]
  OTROS["<b>Other programs</b><br/>with their own configuration"]
  PER["<b>Academic period</b><br/>e.g. 2026-2 · the configuration<br/>is defined per period"]
  M1["<b>Moment I</b><br/>e.g. Programación II<br/>requirement level set by the admin"]
  M2["<b>Moment II</b><br/>e.g. Ingeniería de Software II"]
  M3["<b>Moment III</b><br/>e.g. Electiva de profundización III"]
  R1["The moment's own <b>RAP</b><br/>created, edited or deleted by the admin"]
  R2["The moment's own RAP"]
  R3["The moment's own RAP"]
  CRI["<b>Criteria</b> of each RAP<br/>→ <b>levels</b> with their score"]

  UPC --> FAC --> PROG --> PER
  FAC -.-> OTROS
  PER --> M1 & M2 & M3
  M1 --> R1 --> CRI
  M2 --> R2
  M3 --> R3

  subgraph OP["Operation of a moment (e.g. Moment I)"]
    direction LR
    GRP["<b>Group = classroom project</b><br/>enabled by a teacher;<br/>students join it and<br/>write the project title"]
    GRP --> ENT["<b>Submissions</b><br/>v1, v2 … vn (history)<br/>zip + optional presentation"]
    GRP --> EV["<b>Evaluations</b><br/>teacher · external evaluators"]
    EV --> CAL["<b>Grade per criterion</b><br/>picked level and its score"]
    CAL --> ADJ["<b>Individual adjustment</b><br/>another level for one student<br/>in a criterion (ADR-018)"]
  end
  M1 --> GRP
  CAL -. against the criteria .-> CRI

  classDef acad fill:#DCFCE7,stroke:#2F855A,color:#1F2937
  classDef oper fill:#FEF3C7,stroke:#B7791F,color:#1F2937
  classDef eval fill:#EDE4FB,stroke:#7E57C2,color:#1F2937
  classDef rap fill:#FFFFFF,stroke:#2F855A,color:#1F2937
  classDef otro fill:#FFFFFF,stroke:#2F855A,color:#1F2937,stroke-dasharray:5 4
  class UPC,FAC,PROG,PER,M1,M2,M3 acad
  class R1,R2,R3,CRI rap
  class OTROS otro
  class GRP,ENT oper
  class EV,CAL,ADJ eval
```

Each moment has its own RAP, criteria and levels with scores, defined by the admin in
each academic period; nothing is fixed in the code (ADR-010). The grade of an
evaluation is the sum of the scores of the levels picked in each criterion, and the
final grade is a weighted average by kind of evaluator (the teacher and each
`EvaluatorType`), with the weights the admin sets in each moment. The group is graded
once and every member inherits its levels; an evaluator can change the level of one
student in a criterion, and that individual grade is each student's final grade
(ADR-018).

## 4. Role hierarchy and access scope

Each level sees only a subset of what the level above sees. Filtering is applied in the
service layer (`AccessScope` and the rules of each service), not in the user interface.
Students are the exception to the nesting while groups are formed: they see every group
of the moment of their semester to join one, and then only their own group.

```mermaid
flowchart TD
  SA["<b>Super Admin</b><br/>Scope: the whole institution<br/>Parameters, periods, faculties,<br/>evaluator types"]
  AD["<b>Admin (dean)</b><br/>Scope: their faculty<br/>Configures and launches the moments"]
  DC["<b>Teacher</b><br/>Scope: the groups they enabled<br/>Dates and evaluators of their groups"]
  ES["<b>Student</b><br/>Sees the groups of the moment,<br/>joins, submits, checks their grade"]
  EX["<b>External evaluator</b><br/>business person · alumnus · …<br/>Scope: assigned groups"]

  SA --> AD --> DC
  DC -. enables groups for .-> ES
  DC -- creates or reuses, assigns --> EX

  classDef rol fill:#DBEAFE,stroke:#3B6FB6,color:#1F2937
  class SA,AD,DC,ES,EX rol
```

```mermaid
flowchart TB
  subgraph I["Institution — super_admin"]
    subgraph F["Faculty — admin"]
      subgraph D["Groups the teacher enabled — teacher"]
        subgraph G["Group"]
          E1["Student<br/>their own group"]
          E2["External evaluator<br/>only the assigned groups"]
        end
      end
    end
  end
  style I fill:#EFF6FF,stroke:#3B6FB6,color:#1E3A8A
  style F fill:#DBEAFE,stroke:#3B6FB6,color:#1E3A8A
  style D fill:#BFDBFE,stroke:#3B6FB6,color:#1E3A8A
  style G fill:#93C5FD,stroke:#3B6FB6,color:#1E3A8A
  style E1 fill:#FFFFFF,stroke:#3B6FB6,color:#1F2937
  style E2 fill:#FFFFFF,stroke:#3B6FB6,color:#1F2937
```
