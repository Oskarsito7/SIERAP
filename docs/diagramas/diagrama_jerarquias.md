# Hierarchy diagram

Four hierarchies that structure SIERAP. GitHub renders each block directly from the
Mermaid code.

> Detailed-design version (the four panels on a single sheet, still with the Spanish
> names from before ADR-014): [`diagrama_jerarquias.png`](diagrama_jerarquias.png) ·
> editable in [`diagrama_jerarquias.drawio`](diagrama_jerarquias.drawio).

## 1. Inheritance hierarchy — Users

Abstraction + inheritance + interface: the basis of the project's OOP.

```mermaid
classDiagram
direction BT
class User {
  <<abstract>>
  -firstNames: String
  -lastNames: String
  -email: String
  -active: boolean
  +getRole()* UserRole
}
class Evaluator {
  <<interface>>
  +grade(group) Evaluation
  +getAssignedGroups() List~ProjectGroup~
}
class SuperAdmin { +getRole() UserRole }
class Admin { +getRole() UserRole }
class Student { +getRole() UserRole }
class Teacher { +getRole() UserRole }
class ExternalEvaluator {
  -evaluatorType: EvaluatorType
  +getRole() UserRole
}

SuperAdmin --|> User
Admin --|> User
Student --|> User
Teacher --|> User
ExternalEvaluator --|> User
Teacher ..|> Evaluator
ExternalEvaluator ..|> Evaluator
```

- `User` is abstract: there is no "generic user", only the five concrete roles.
- `getRole()` is polymorphic: each subclass overrides it and Spring Security decides permissions with that value.
- `Teacher` and `ExternalEvaluator` grade groups, but they do not share a parent of
  their own for that: the common behavior is expressed with the `Evaluator` interface.
- Business person, alumnus and the types created later are not classes: they are values
  of the `EvaluatorType` catalog of an `ExternalEvaluator` (ADR-011).
- `Student`, `Admin` and `SuperAdmin` do not grade, so they do not implement `Evaluator`.
- In JPA the inheritance is mapped with `@Inheritance(JOINED)`: an `app_user` table + one table per role (ADR-009).

## 2. Inheritance hierarchies — Reports and exceptions

Polymorphism in reporting and centralized error handling with `@ControllerAdvice`.

```mermaid
classDiagram
direction BT
class Report {
  <<abstract>>
  +generate()* byte[]
}
class IndividualReport {
  +generate() byte[]
}
class GroupReport {
  +generate() byte[]
}
class ComparativeReport {
  +generate() byte[]
}
IndividualReport --|> Report : per student and RAP
GroupReport --|> Report : per group
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
  +getHttpStatus()* HttpStatus
}
class GlobalExceptionHandler {
  <<ControllerAdvice>>
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
SierapException --|> RuntimeException
ResourceNotFoundException --|> SierapException
ForbiddenAccessException --|> SierapException
BusinessRuleException --|> SierapException
GlobalExceptionHandler ..> SierapException : «handles»
```

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
    GRP["<b>Group = classroom project</b><br/>enabled by a teacher;<br/>students join it"]
    GRP --> ENT["<b>Submissions</b><br/>v1, v2 … vn (history)"]
    GRP --> EV["<b>Evaluations</b><br/>teacher · external evaluators"]
    EV --> CAL["<b>Grade per criterion</b><br/>picked level and its score"]
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
  class EV,CAL eval
```

Each moment has its own RAP, criteria and levels with scores, defined by the admin in
each academic period; nothing is fixed in the code (ADR-010). The grade of an
evaluation is the sum of the scores of the levels picked in each criterion.

## 4. Role hierarchy and access scope

Each level sees only a subset of what the level above sees. Filtering is applied in the
service layer, not in the user interface.

```mermaid
flowchart TD
  SA["<b>Super Admin</b><br/>Scope: the whole institution<br/>Parameters, periods, faculties,<br/>evaluator types"]
  AD["<b>Admin (dean)</b><br/>Scope: their faculty<br/>Configures and launches the moments"]
  DC["<b>Teacher</b><br/>Scope: the groups they enabled<br/>Dates and evaluators of their groups"]
  ES["<b>Student</b><br/>Sees the groups of the moment,<br/>joins, submits, checks their grade"]
  EX["<b>External evaluator</b><br/>business person · alumnus · …<br/>Scope: assigned groups"]

  SA --> AD --> DC
  DC -. enables groups for .-> ES
  DC -- creates and assigns --> EX

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
