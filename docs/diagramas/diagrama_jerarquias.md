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
class Usuario {
  <<abstract>>
  -nombres: String
  -apellidos: String
  -correo: String
  -activo: boolean
  +getRol()* RolUsuario
}
class Evaluador {
  <<interface>>
  +calificar(proyecto) Evaluacion
  +getGruposAsignados() List~Grupo~
}
class SuperAdmin { +getRol() RolUsuario }
class Admin { +getRol() RolUsuario }
class Estudiante { +getRol() RolUsuario }
class Docente { +getRol() RolUsuario }
class Empresario { +getRol() RolUsuario }
class Egresado { +getRol() RolUsuario }

SuperAdmin --|> Usuario
Admin --|> Usuario
Estudiante --|> Usuario
Docente --|> Usuario
Empresario --|> Usuario
Egresado --|> Usuario
Docente ..|> Evaluador
Empresario ..|> Evaluador
Egresado ..|> Evaluador
```

- `Usuario` es abstracta: no existe un "usuario genérico", solo los seis roles concretos.
- `getRol()` es polimórfico: cada subclase lo sobrescribe y Spring Security decide los permisos con ese valor.
- Docente, Empresario y Egresado califican proyectos, pero no comparten un padre propio
  para eso: el comportamiento común se expresa con la interfaz `Evaluador`.
- Estudiante, Admin y SuperAdmin no evalúan, por eso no implementan `Evaluador`.
- En JPA la herencia se mapea con `@Inheritance` (estrategia a decidir en un ADR de la Etapa 1).

## 2. Jerarquías de herencia — Reportes y excepciones

Polimorfismo en la reportería y manejo centralizado de errores con `@ControllerAdvice`.

```mermaid
classDiagram
direction BT
class Reporte {
  <<abstract>>
  +generar()* byte[]
}
class ReporteIndividual {
  +generar() byte[]
}
class ReporteGrupal {
  +generar() byte[]
}
class ReporteComparativo {
  +generar() byte[]
}
ReporteIndividual --|> Reporte : por estudiante y RAP
ReporteGrupal --|> Reporte : por grupo
ReporteComparativo --|> Reporte : semestre X vs. Y
```

```mermaid
classDiagram
direction BT
class RuntimeException {
  <<java.lang>>
}
class SierapException {
  <<abstract>>
  -codigo: String
}
class GlobalExceptionHandler {
  <<ControllerAdvice>>
  +manejar(ex: SierapException)
}
class RecursoNoEncontradoException {
  HTTP 404
}
class AccesoDenegadoException {
  HTTP 403
}
class FueraDeFechaException {
  HTTP 422
}
class ReglaNegocioException {
  HTTP 400
}
SierapException --|> RuntimeException
RecursoNoEncontradoException --|> SierapException
AccesoDenegadoException --|> SierapException
FueraDeFechaException --|> SierapException
ReglaNegocioException --|> SierapException
GlobalExceptionHandler ..> SierapException : «maneja»
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
    GRP["<b>Grupo</b><br/>3 a 5 estudiantes"] --> PA["<b>Proyecto de aula</b><br/>uno por momento"]
    PA --> ENT["<b>Entregas</b><br/>v1, v2 … vn (historial)"]
    PA --> EV["<b>Evaluaciones</b><br/>docente · empresario · egresado"]
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
  class GRP,PA,ENT oper
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
  SA["<b>Super Admin</b><br/>Alcance: toda la institución<br/>Parámetros globales y facultades"]
  AD["<b>Admin (decano)</b><br/>Alcance: su facultad<br/>Momentos y RAP de sus programas"]
  DC["<b>Docente</b><br/>Alcance: sus grupos asignados<br/>Grupos, fechas y evaluadores externos"]
  ES["<b>Estudiante</b><br/>Alcance: su grupo y su proyecto<br/>Sube entregas, consulta su nota"]
  EM["<b>Empresario</b><br/>Alcance: grupos que se le asignan<br/>Califica dentro de la fecha"]
  EG["<b>Egresado</b><br/>Alcance: grupos que se le asignan<br/>Califica dentro de la fecha"]

  SA --> AD --> DC
  DC --> ES
  DC -- crea y asigna --> EM
  DC -- crea y asigna --> EG

  classDef rol fill:#DBEAFE,stroke:#3B6FB6,color:#1F2937
  class SA,AD,DC,ES,EM,EG rol
```

```mermaid
flowchart TB
  subgraph I["Institución — super_admin"]
    subgraph F["Facultad — admin"]
      subgraph D["Grupos del docente — docente"]
        subgraph G["Grupo asignado"]
          E1["Estudiante<br/>su propio grupo"]
          E2["Empresario / Egresado<br/>solo los grupos asignados"]
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
