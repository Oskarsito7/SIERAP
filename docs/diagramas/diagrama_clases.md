# Diagrama de clases (vigente · Etapa 1)

Modelo de dominio de SIERAP con herencia, clases abstractas, interfaces,
enumeraciones y relaciones de composición, agregación, asociación y dependencia.
GitHub renderiza este diagrama directamente a partir del código Mermaid.

> **Este es el diagrama vigente** y el contrato de la Etapa 1: incorpora las
> decisiones del 2026-09-24 (ver `docs/04`, sección "Decisiones del equipo", y los
> ADR-009 a ADR-014). Clases y atributos en inglés (ADR-014). La versión con diseño detallado
> ([`diagrama_clases_mejorado.png`](diagrama_clases_mejorado.png) · editable en
> [`diagrama_clases_mejorado.drawio`](diagrama_clases_mejorado.drawio)) corresponde
> a la Fase 2 y se regenera al cerrar la Etapa 1. El diagrama preliminar de la
> Fase 1 sigue en [`diagrama_clases.drawio`](diagrama_clases.drawio).

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

## Cambios de la Etapa 1 (decisiones del 2026-09-24)

| Cambio | Motivo |
|---|---|
| Todas las clases, atributos y métodos pasan a inglés (`Facultad` → `Faculty`, `Momento` → `Moment`, `RAP` → `LearningOutcome`, `CriterioEvaluacion` → `EvaluationCriterion`, `NivelDesempeno` → `PerformanceLevel`, `Grupo` → `ProjectGroup`…); la tabla de equivalencias está en `docs/03` | El código se escribe en inglés y solo lo que ve el usuario final queda en español (ADR-014). |
| Puntajes (`score`, `getMaxScore()`, notas) como `BigDecimal` en lugar de `double` | Sumas exactas: el Momento 1 debe dar 5.00, no 4.9999… (ADR-013). |
| `Usuario`: `nombre` → `nombres` + `apellidos`; nuevos `tipoDocumento` y `numeroDocumento`; enum `TipoDocumento` | Guardar el documento de identidad además del correo, que es el dato de inicio de sesión. |
| `Empresario` y `Egresado` → una sola subclase `EvaluadorExterno` con `TipoEvaluador` (catálogo) | Se deben poder crear más tipos de evaluador sin programar (ADR-011). |
| `Docente` pertenece a una `Facultad` | Solo usan SIERAP los docentes con proyecto de aula, que dictan en su facultad. |
| `Estudiante` pierde `grupo` y gana `programa` | Un estudiante tiene un grupo por momento con historial; la relación queda del lado de `Grupo`. |
| `Momento` pertenece a un `PeriodoAcademico`; `nivelExigencia` pasa a texto libre | La configuración se versiona por periodo y no hay niveles estándar (ADR-010). |
| `Momento` gana `estado` (`EstadoMomento`), integrantes mín./máx. y tres ventanas de fechas | Ciclo de vida del momento: configuración → lanzar → entregas → calificación (ADR-012). |
| `RAP` pertenece a un `Momento` (composición) y pierde `pesoEvaluativo` | Cada momento tiene sus propios RAP; el peso lo dan los puntajes de sus criterios. |
| Nuevas `CriterioEvaluacion` y `NivelDesempeno` (entidad) | Estructura de la rúbrica de la UPC: cada RAP tiene criterios y cada criterio niveles con puntaje, todo configurable. |
| `Grupo` y `ProyectoAula` se fusionan; `Grupo` pertenece a un `Momento` | Cada grupo tiene un solo proyecto; el docente habilita grupos dentro de un momento. |
| `CalificacionRAP` → `CalificacionCriterio` | El evaluador elige un nivel por criterio; el desempeño por RAP se calcula sumando. |
| Se eliminan los enums `NivelExigencia` y `NivelDesempeno` | Nada estándar en el código (ADR-010). |
| Sin autoevaluación del estudiante | `Estudiante` no implementa `Evaluador`. |

## Cambios frente al diagrama preliminar (Fase 1)

| Cambio | Motivo |
|---|---|
| `PeriodoAcademico` (nueva) | RF 9 exige validar que la fecha de evaluación caiga en el rango de la UPC, y RF 12 compara resultados entre semestres y años: ambos necesitan el periodo como entidad. |
| `ParametroGlobal` (nueva) | RF 11: el super_admin define parámetros globales (p. ej. integrantes por grupo) sin hardcodearlos. |
| `Reporte` pasa a ser abstracta con 3 subclases | Cada tipo de reporte implementa su propio `generar()`: polimorfismo en lugar de un `switch` por tipo. Resuelve el pendiente de la Fase 1 sobre reportería. |
| `Usuario.getRol()` abstracto | Cada rol lo sobrescribe; `iniciarSesion()` sale del dominio y pasa a `AuthService` (capa de servicio). |
| Multiplicidad `3..5` en `Grupo`—`Estudiante` | Confirma la cardinalidad de la guía oficial que estaba pendiente. |
