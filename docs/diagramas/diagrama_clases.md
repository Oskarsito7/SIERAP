# Diagrama de clases (versión mejorada · Fase 2)

Modelo de dominio de SIERAP con herencia, clases abstractas, interfaces,
enumeraciones y relaciones de composición, agregación, asociación y dependencia.
GitHub renderiza este diagrama directamente a partir del código Mermaid.

> Versión con diseño detallado y colores por área:
> [`diagrama_clases_mejorado.png`](diagrama_clases_mejorado.png) · editable en
> [`diagrama_clases_mejorado.drawio`](diagrama_clases_mejorado.drawio)
> (abrir en [app.diagrams.net](https://app.diagrams.net)). El diagrama preliminar
> de la Fase 1 sigue en [`diagrama_clases.drawio`](diagrama_clases.drawio).

```mermaid
classDiagram
direction TB

%% ================= Usuarios y roles =================
class Usuario {
  <<abstract>>
  -id: Long
  -nombre: String
  -correo: String
  -passwordHash: String
  -activo: boolean
  -fechaCreacion: LocalDateTime
  +getRol()* RolUsuario
  +cambiarPassword(actual, nueva) void
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
class SuperAdmin {
  +getRol() RolUsuario
  +crearFacultad(nombre: String) Facultad
  +definirParametro(clave, valor) void
}
class Admin {
  -facultad: Facultad
  +getRol() RolUsuario
  +definirMomento(p: Programa, m: Momento) void
  +definirRAP(m: Momento, rap: RAP) void
}
class Estudiante {
  -codigoEstudiante: String
  -semestreActual: int
  -grupo: Grupo
  +getRol() RolUsuario
  +unirseAGrupo(grupo: Grupo) void
  +subirEntrega(rutaZip, rutaPresentacion) Entrega
  +consultarDesempenoPorRAP() List~CalificacionRAP~
}
class Docente {
  -gruposAsignados: List~Grupo~
  +getRol() RolUsuario
  +crearGrupo(nombre, periodo) Grupo
  +definirFechaEntrega(p: ProyectoAula, f: LocalDate) void
  +registrarEvaluador(e: Evaluador, g: Grupo) void
}
class Empresario {
  -empresa: String
  -cargo: String
  -gruposAsignados: List~Grupo~
  +getRol() RolUsuario
}
class Egresado {
  -anioGraduacion: int
  -programaEgreso: Programa
  -gruposAsignados: List~Grupo~
  +getRol() RolUsuario
}

%% ================= Estructura académica =================
class ParametroGlobal {
  -clave: String
  -valor: String
  -descripcion: String
}
class Facultad {
  -id: Long
  -nombre: String
  -programas: List~Programa~
  +agregarPrograma(p: Programa) void
}
class Programa {
  -id: Long
  -codigo: String
  -nombre: String
  -momentos: List~Momento~
  +agregarMomento(m: Momento) void
  +getMomentoPorSemestre(sem: int) Momento
}
class Momento {
  -id: Long
  -numero: int
  -nivel: NivelExigencia
  -semestreDesde: int
  -semestreHasta: int
  -asignatura: String
  +incluyeSemestre(semestre: int) boolean
}
class NivelExigencia {
  <<enumeration>>
  BAJO
  MEDIO
  ALTO
}
class RAP {
  -id: Long
  -codigo: String
  -descripcion: String
  -pesoEvaluativo: double
}
class PeriodoAcademico {
  -id: Long
  -codigo: String
  -fechaInicio: LocalDate
  -fechaFin: LocalDate
  -limiteEvaluacionUPC: LocalDate
  +contiene(fecha: LocalDate) boolean
}

%% ================= Estructura operativa =================
class Grupo {
  -id: Long
  -nombre: String
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
  -calificaciones: List~CalificacionRAP~
  +calcularNotaTotal() double
}
class CalificacionRAP {
  -id: Long
  -puntaje: double
  -nivel: NivelDesempeno
}
class NivelDesempeno {
  <<enumeration>>
  BAJO
  BASICO
  ALTO
  SOBRESALIENTE
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

SuperAdmin ..> ParametroGlobal : «gestiona»
Admin "1" -- "1" Facultad : administra
Facultad "1" *-- "1..*" Programa
Programa "1" *-- "1..*" Momento
Momento "*" -- "1..*" RAP : evalúa
Momento ..> NivelExigencia : «use»

Grupo "0..1" o-- "3..5" Estudiante
Docente "1" -- "*" Grupo : dicta
Grupo "*" -- "1" Programa
Grupo "*" -- "1" PeriodoAcademico
Grupo "1" *-- "1..*" ProyectoAula
ProyectoAula "*" -- "1" Momento
ProyectoAula "1" *-- "0..*" Entrega

ProyectoAula "1" *-- "0..*" Evaluacion
Evaluacion "*" -- "1" Evaluador : realizada por
Evaluacion "1" *-- "1..*" CalificacionRAP
CalificacionRAP "*" -- "1" RAP : califica un
CalificacionRAP ..> NivelDesempeno : «use»

Reporte <|-- ReporteIndividual
Reporte <|-- ReporteGrupal
Reporte <|-- ReporteComparativo
Reporte ..> Evaluacion : «use»
```

## Cambios frente al diagrama preliminar (Fase 1)

| Cambio | Motivo |
|---|---|
| `PeriodoAcademico` (nueva) | RF 9 exige validar que la fecha de evaluación caiga en el rango de la UPC, y RF 12 compara resultados entre semestres y años: ambos necesitan el periodo como entidad. |
| `ParametroGlobal` (nueva) | RF 11: el super_admin define parámetros globales (p. ej. integrantes por grupo) sin hardcodearlos. |
| `Reporte` pasa a ser abstracta con 3 subclases | Cada tipo de reporte implementa su propio `generar()`: polimorfismo en lugar de un `switch` por tipo. Resuelve el pendiente de la Fase 1 sobre reportería. |
| Enums `NivelExigencia` y `NivelDesempeno` | Reemplazan `String` sueltos (`nivelAlcanzado`) por tipos cerrados. |
| `Usuario.getRol()` abstracto | Cada rol lo sobrescribe; `iniciarSesion()` sale del dominio y pasa a `AuthService` (capa de servicio). |
| Multiplicidad `3..5` en `Grupo`—`Estudiante` | Confirma la cardinalidad de la guía oficial que estaba pendiente. |
