# Diagrama de clases (vigente · Etapa 1)

Modelo de dominio de SIERAP con herencia, clases abstractas, interfaces,
enumeraciones y relaciones de composición, agregación, asociación y dependencia.
GitHub renderiza este diagrama directamente a partir del código Mermaid.

> **Este es el diagrama vigente** y el contrato de la Etapa 1: incorpora las
> decisiones del 2026-09-24 (ver `docs/04`, sección "Decisiones del equipo", y los
> ADR-009 y ADR-010). La versión con diseño detallado
> ([`diagrama_clases_mejorado.png`](diagrama_clases_mejorado.png) · editable en
> [`diagrama_clases_mejorado.drawio`](diagrama_clases_mejorado.drawio)) corresponde
> a la Fase 2 y se regenera al cerrar la Etapa 1. El diagrama preliminar de la
> Fase 1 sigue en [`diagrama_clases.drawio`](diagrama_clases.drawio).

```mermaid
classDiagram
direction TB

%% ================= Usuarios y roles =================
class Usuario {
  <<abstract>>
  -id: Long
  -nombres: String
  -apellidos: String
  -tipoDocumento: TipoDocumento
  -numeroDocumento: String
  -correo: String
  -passwordHash: String
  -activo: boolean
  -fechaCreacion: LocalDateTime
  +getRol()* RolUsuario
  +getNombreCompleto() String
  +estaActivo() boolean
}
class Evaluador {
  <<interface>>
  +calificar(proyecto: ProyectoAula) Evaluacion
  +getGruposAsignados() List~Grupo~
  +puedeEvaluar(p: ProyectoAula, hoy: LocalDate) boolean
}
class RolUsuario {
  <<enumeration>>
  SUPER_ADMIN
  ADMIN
  DOCENTE
  ESTUDIANTE
  EMPRESARIO
  EGRESADO
}
class TipoDocumento {
  <<enumeration>>
  CC
  TI
  CE
  PASAPORTE
}
class SuperAdmin {
  +getRol() RolUsuario
}
class Admin {
  -facultad: Facultad
  +getRol() RolUsuario
}
class Docente {
  -facultad: Facultad
  +getRol() RolUsuario
}
class Estudiante {
  -codigoEstudiante: String
  -semestreActual: int
  -programa: Programa
  +getRol() RolUsuario
}
class Empresario {
  -empresa: String
  -cargo: String
  +getRol() RolUsuario
}
class Egresado {
  -anioGraduacion: int
  -programaEgreso: Programa
  +getRol() RolUsuario
}

%% ================= Estructura académica y configuración =================
class ParametroGlobal {
  -clave: String
  -valor: String
  -descripcion: String
}
class Facultad {
  -id: Long
  -nombre: String
  -programas: List~Programa~
}
class Programa {
  -id: Long
  -codigo: String
  -nombre: String
  -facultad: Facultad
}
class PeriodoAcademico {
  -id: Long
  -codigo: String
  -fechaInicio: LocalDate
  -fechaFin: LocalDate
  -fechaLimiteEvaluacionUPC: LocalDate
  +contiene(fecha: LocalDate) boolean
}
class Momento {
  -id: Long
  -numero: int
  -nombre: String
  -nivelExigencia: String
  -asignatura: String
  -semestreDesde: int
  -semestreHasta: int
  -programa: Programa
  -periodo: PeriodoAcademico
  -raps: List~RAP~
  +incluyeSemestre(semestre: int) boolean
}
class RAP {
  -id: Long
  -codigo: String
  -descripcion: String
  -criterios: List~CriterioEvaluacion~
  +getPuntajeMaximo() double
}
class CriterioEvaluacion {
  -id: Long
  -descripcion: String
  -orden: int
  -niveles: List~NivelDesempeno~
  +getPuntajeMaximo() double
}
class NivelDesempeno {
  -id: Long
  -nombre: String
  -descripcion: String
  -puntaje: double
  -orden: int
}

%% ================= Estructura operativa (se detalla con D3) =================
class Grupo {
  -id: Long
  -nombre: String
  -docente: Docente
  -integrantes: List~Estudiante~
  -abierto: boolean
  +agregarIntegrante(e: Estudiante) void
  +estaCompleto() boolean
}
class ProyectoAula {
  -id: Long
  -titulo: String
  -fechaLimiteEntrega: LocalDate
  -fechaEvaluacion: LocalDate
  +registrarEntrega(e: Entrega) void
  +getUltimaEntrega() Entrega
  +getNotaFinal() double
}
class Entrega {
  -id: Long
  -numeroVersion: int
  -rutaArchivoZip: String
  -rutaPresentacion: String
  -fechaSubida: LocalDateTime
  +fueATiempo(limite: LocalDate) boolean
}

%% ================= Evaluación y reportería =================
class Evaluacion {
  -id: Long
  -fecha: LocalDateTime
  -comentario: String
  -calificaciones: List~CalificacionCriterio~
  +calcularNotaTotal() double
  +calcularNotaPorRAP(rap: RAP) double
}
class CalificacionCriterio {
  -id: Long
  -criterio: CriterioEvaluacion
  -nivelElegido: NivelDesempeno
  -puntaje: double
}
class Reporte {
  <<abstract>>
  -fechaGeneracion: LocalDateTime
  -periodo: PeriodoAcademico
  +generar()* byte[]
}
class ReporteIndividual {
  -estudiante: Estudiante
  +generar() byte[]
}
class ReporteGrupal {
  -grupo: Grupo
  +generar() byte[]
}
class ReporteComparativo {
  -periodoA: PeriodoAcademico
  -periodoB: PeriodoAcademico
  +generar() byte[]
}

%% ================= Relaciones =================
Usuario <|-- SuperAdmin
Usuario <|-- Admin
Usuario <|-- Estudiante
Usuario <|-- Docente
Usuario <|-- Empresario
Usuario <|-- Egresado
Evaluador <|.. Docente
Evaluador <|.. Empresario
Evaluador <|.. Egresado
Usuario ..> RolUsuario : «use»
Usuario ..> TipoDocumento : «use»

SuperAdmin ..> ParametroGlobal : «gestiona»
SuperAdmin ..> PeriodoAcademico : «gestiona»
Admin "1" -- "1" Facultad : administra
Docente "*" -- "1" Facultad : pertenece a
Estudiante "*" -- "1" Programa : cursa
Egresado "*" -- "1" Programa : egresó de

Facultad "1" *-- "1..*" Programa
Programa "1" *-- "*" Momento
Momento "*" -- "1" PeriodoAcademico : se configura en
Momento "1" *-- "1..*" RAP
RAP "1" *-- "1..*" CriterioEvaluacion
CriterioEvaluacion "1" *-- "1..*" NivelDesempeno

Grupo "*" o-- "3..5" Estudiante : un grupo por momento
Docente "1" -- "*" Grupo : dicta
Grupo "*" -- "1" Programa
Grupo "1" *-- "1..*" ProyectoAula
ProyectoAula "*" -- "1" Momento
ProyectoAula "1" *-- "0..*" Entrega

ProyectoAula "1" *-- "0..*" Evaluacion
Evaluacion "*" -- "1" Evaluador : realizada por
Evaluacion "1" *-- "1..*" CalificacionCriterio
CalificacionCriterio "*" -- "1" CriterioEvaluacion : califica
CalificacionCriterio "*" -- "1" NivelDesempeno : nivel elegido

Reporte <|-- ReporteIndividual
Reporte <|-- ReporteGrupal
Reporte <|-- ReporteComparativo
Reporte ..> Evaluacion : «use»
```

## Cambios de la Etapa 1 (decisiones del 2026-09-24)

| Cambio | Motivo |
|---|---|
| `Usuario`: `nombre` → `nombres` + `apellidos`; nuevos `tipoDocumento` y `numeroDocumento`; enum `TipoDocumento` | Decisión del equipo: guardar el documento de identidad además del correo, que es el dato de inicio de sesión. |
| `Docente` pertenece a una `Facultad` | Solo usan SIERAP los docentes con proyecto de aula, que dictan en su facultad; así el admin gestiona los docentes de la suya. |
| `Estudiante` pierde `grupo` y gana `programa` | Un estudiante tiene un grupo por momento con historial; la relación queda del lado de `Grupo`. |
| `Momento` pertenece a un `PeriodoAcademico`; `nivelExigencia` pasa a texto libre | La configuración se versiona por periodo y no hay niveles estándar (ADR-010). |
| `RAP` pertenece a un `Momento` (composición) y pierde `pesoEvaluativo` | Cada momento tiene sus propios RAP; el peso lo dan los puntajes de sus criterios. |
| Nuevas `CriterioEvaluacion` y `NivelDesempeno` (entidad) | Estructura de la rúbrica de la UPC: cada RAP tiene criterios y cada criterio niveles con puntaje, todo configurable. |
| `CalificacionRAP` → `CalificacionCriterio` | El evaluador elige un nivel por criterio; el desempeño por RAP se calcula sumando. |
| Se eliminan los enums `NivelExigencia` y `NivelDesempeno` | Nada estándar en el código (ADR-010). |
| `Grupo`—`Docente` "1", sin autoevaluación del estudiante | Decisiones del equipo: un docente por grupo; `Estudiante` no implementa `Evaluador`. |

## Cambios frente al diagrama preliminar (Fase 1)

| Cambio | Motivo |
|---|---|
| `PeriodoAcademico` (nueva) | RF 9 exige validar que la fecha de evaluación caiga en el rango de la UPC, y RF 12 compara resultados entre semestres y años: ambos necesitan el periodo como entidad. |
| `ParametroGlobal` (nueva) | RF 11: el super_admin define parámetros globales (p. ej. integrantes por grupo) sin hardcodearlos. |
| `Reporte` pasa a ser abstracta con 3 subclases | Cada tipo de reporte implementa su propio `generar()`: polimorfismo en lugar de un `switch` por tipo. Resuelve el pendiente de la Fase 1 sobre reportería. |
| `Usuario.getRol()` abstracto | Cada rol lo sobrescribe; `iniciarSesion()` sale del dominio y pasa a `AuthService` (capa de servicio). |
| Multiplicidad `3..5` en `Grupo`—`Estudiante` | Confirma la cardinalidad de la guía oficial que estaba pendiente. |
