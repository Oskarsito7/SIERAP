# Diagrama de jerarquías

Cuatro jerarquías que estructuran SIERAP. GitHub renderiza cada bloque
directamente a partir del código Mermaid.

> Versión con diseño detallado (los cuatro paneles en una sola lámina):
> [`diagrama_jerarquias.png`](diagrama_jerarquias.png) · editable en
> [`diagrama_jerarquias.drawio`](diagrama_jerarquias.drawio).

## 1. Jerarquía de herencia — Usuarios

Abstracción + herencia + interfaz: la base de la POO del proyecto.

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

- `User` es abstracta: no existe un "usuario genérico", solo los cinco roles concretos.
- `getRole()` es polimórfico: cada subclase lo sobrescribe y Spring Security decide los permisos con ese valor.
- `Teacher` (docente) y `ExternalEvaluator` (evaluador externo) califican grupos, pero
  no comparten un padre propio para eso: el comportamiento común se expresa con la
  interfaz `Evaluator`.
- Empresario, egresado y los tipos que se creen después no son clases: son valores del
  catálogo `EvaluatorType` de un `ExternalEvaluator` (ADR-011).
- `Student`, `Admin` y `SuperAdmin` no evalúan, por eso no implementan `Evaluator`.
- En JPA la herencia se mapea con `@Inheritance(JOINED)`: tabla `app_user` + una tabla por rol (ADR-009).
- Clases en inglés, textos para el usuario en español (ADR-014).

## 2. Jerarquías de herencia — Reportes y excepciones

Polimorfismo en la reportería y manejo centralizado de errores con `@ControllerAdvice`.

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
IndividualReport --|> Report : por estudiante y RAP
GroupReport --|> Report : por grupo
ComparativeReport --|> Report : semestre X vs. Y
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

## 3. Jerarquía institucional y académica

Configuración (super_admin / admin) → operación de cada momento (docente, estudiantes, evaluadores).

```mermaid
flowchart TD
  UPC["<b>Universidad Popular del Cesar</b><br/>institución · la configura el super_admin"]
  FAC["<b>Facultad</b><br/>la administra un admin (decano)"]
  PROG["<b>Programa</b><br/>ej. Ingeniería de Sistemas"]
  OTROS["<b>Otros programas</b><br/>con su propia configuración"]
  PER["<b>Periodo académico</b><br/>ej. 2026-2 · la configuración<br/>se define por periodo"]
  M1["<b>Momento I</b><br/>ej. Programación II<br/>exigencia: la define el admin"]
  M2["<b>Momento II</b><br/>ej. Ingeniería de Software II"]
  M3["<b>Momento III</b><br/>ej. Electiva de profundización III"]
  R1["<b>RAP</b> propios del momento<br/>el admin los crea, edita o elimina"]
  R2["RAP propios del momento"]
  R3["RAP propios del momento"]
  CRI["<b>Criterios</b> de cada RAP<br/>→ <b>niveles</b> con su puntaje"]

  UPC --> FAC --> PROG --> PER
  FAC -.-> OTROS
  PER --> M1 & M2 & M3
  M1 --> R1 --> CRI
  M2 --> R2
  M3 --> R3

  subgraph OP["Operación de un momento (ej. Momento I)"]
    direction LR
    GRP["<b>Grupo = proyecto de aula</b><br/>lo habilita un docente;<br/>los estudiantes se inscriben"]
    GRP --> ENT["<b>Entregas</b><br/>v1, v2 … vn (historial)"]
    GRP --> EV["<b>Evaluaciones</b><br/>docente · evaluadores externos"]
    EV --> CAL["<b>Calificación por criterio</b><br/>nivel elegido y su puntaje"]
  end
  M1 --> GRP
  CAL -. contra los criterios .-> CRI

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

Cada momento tiene sus propios RAP, criterios y niveles con puntaje, definidos por el
admin en cada periodo académico; nada viene fijo en el código (ADR-010). La nota de una
evaluación es la suma de los puntajes de los niveles elegidos en cada criterio.

## 4. Jerarquía de roles y alcance de acceso

Cada nivel ve solo un subconjunto de lo que ve el nivel superior. El filtrado se aplica
en la capa de servicio, no en la interfaz.

```mermaid
flowchart TD
  SA["<b>Super Admin</b><br/>Alcance: toda la institución<br/>Parámetros, periodos, facultades,<br/>tipos de evaluador"]
  AD["<b>Admin (decano)</b><br/>Alcance: su facultad<br/>Configura y lanza los momentos"]
  DC["<b>Docente</b><br/>Alcance: los grupos que habilitó<br/>Fechas y evaluadores de sus grupos"]
  ES["<b>Estudiante</b><br/>Ve los grupos del momento,<br/>se inscribe, entrega, consulta su nota"]
  EX["<b>Evaluador externo</b><br/>empresario · egresado · …<br/>Alcance: grupos designados"]

  SA --> AD --> DC
  DC -. habilita grupos para .-> ES
  DC -- crea y designa --> EX

  classDef rol fill:#DBEAFE,stroke:#3B6FB6,color:#1F2937
  class SA,AD,DC,ES,EX rol
```

```mermaid
flowchart TB
  subgraph I["Institución — super_admin"]
    subgraph F["Facultad — admin"]
      subgraph D["Grupos que habilitó el docente — docente"]
        subgraph G["Grupo"]
          E1["Estudiante<br/>su propio grupo"]
          E2["Evaluador externo<br/>solo los grupos designados"]
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
