# 03 — Entidades del dominio

A continuación se listan las clases del dominio identificadas para la solución,
agrupadas por área funcional. Diagramas:

- **Diagrama de clases vigente (Etapa 1):** [`diagramas/diagrama_clases.md`](diagramas/diagrama_clases.md)
  (Mermaid, se ve directo en GitHub). Es el contrato de la Etapa 1.
- Diagrama de clases mejorado de la Fase 2: [PNG](diagramas/diagrama_clases_mejorado.png) ·
  [editable .drawio](diagramas/diagrama_clases_mejorado.drawio). **Desactualizado**
  frente a las decisiones del 2026-09-24: se regenera al cerrar la Etapa 1 con lo que
  quede en el código.
- **Diagrama de jerarquías:** [`diagramas/diagrama_jerarquias.md`](diagramas/diagrama_jerarquias.md).
- **Diagrama de capas (paquetes):** [`diagramas/diagrama_capas.md`](diagramas/diagrama_capas.md).
- Diagrama de clases preliminar (Fase 1): [`diagramas/diagrama_clases.drawio`](diagramas/diagrama_clases.drawio)
  (ábrelo en [app.diagrams.net](https://app.diagrams.net)).

Las decisiones que dieron forma a este modelo están en
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md) (sección
"Decisiones del equipo") y en los ADR-009 y ADR-010 de
[`05-decisiones-tecnicas.md`](05-decisiones-tecnicas.md).

## Usuarios (jerarquía de herencia)

Se persiste con `@Inheritance(JOINED)`: tabla `usuario` con los datos comunes y una
tabla por rol (ADR-009). Todos los usuarios inician sesión con su **correo**.

| Clase | Atributos | Descripción |
|---|---|---|
| `Usuario` (abstracta) | nombres, apellidos, tipoDocumento, numeroDocumento (único), correo (único, login), passwordHash, rol, activo, fechaCreacion | Datos comunes a todo usuario del sistema. De ella heredan los seis roles. |
| `SuperAdmin` | — | Permisos globales: define los parámetros generales del sistema y los periodos académicos. |
| `Admin` | facultad | Usuario asociado a una `Facultad` (rol de decano). Configura los `Momento`, `RAP`, criterios y niveles de los `Programa` de su facultad. |
| `Docente` | facultad | Pertenece a **una** `Facultad` (en SIERAP solo están los docentes con proyecto de aula, y esos dictan en su facultad). Tiene grupos asignados, define fechas de entrega y evaluación, y crea usuarios `Empresario`/`Egresado`. |
| `Estudiante` | codigoEstudiante (único), semestreActual, programa | Pertenece a un `Programa`. Tiene **un grupo por momento**; el historial de grupos se conserva para los reportes. Sube `Entrega` y consulta su desempeño por `RAP`. |
| `Empresario` | empresa, cargo | Usuario externo que evalúa los proyectos de los grupos que tiene asignados. |
| `Egresado` | anioGraduacion, programaEgreso | Graduado de un `Programa` de la UPC que evalúa los proyectos de los grupos que tiene asignados. |

**Interfaz `Evaluador`:** contrato común implementado por `Docente`, `Empresario`
y `Egresado`, ya que los tres pueden calificar proyectos y consultar sus grupos
asignados, aunque no comparten una jerarquía de herencia directa entre sí (todos
heredan de `Usuario`, pero el comportamiento de "evaluar" es transversal a roles
distintos — caso de uso natural para una interfaz en lugar de herencia). El
`Estudiante` **no** implementa `Evaluador`: en SIERAP no hay autoevaluación.

## Estructura académica y configuración de la evaluación

Toda la configuración de la evaluación se define **por periodo académico** y la
administra cada admin; nada viene fijo en el código (ADR-010). Al abrir un periodo
se copia la configuración del periodo anterior como punto de partida.

| Clase | Atributos | Descripción |
|---|---|---|
| `Facultad` | nombre | Unidad académica mayor de la universidad; agrupa uno o varios `Programa`. |
| `Programa` | codigo, nombre, facultad | Programa académico (p. ej. Ing. de Sistemas), perteneciente a una `Facultad`. |
| `PeriodoAcademico` | codigo (p. ej. 2026-2), fechaInicio, fechaFin, fechaLimiteEvaluacionUPC | Semestre académico con la fecha límite de evaluación fijada por la UPC. Permite validar fechas (RF 9) y comparar resultados entre semestres y años (RF 12). |
| `Momento` | programa, periodo, numero, nombre, nivelExigencia (texto libre), asignatura, semestreDesde, semestreHasta | Etapa de evaluación acumulativa de un `Programa` **en un periodo**, asociada a un rango de semestres y a una asignatura. El nivel de exigencia lo escribe el admin (no es una lista fija). |
| `RAP` | momento, codigo, descripcion | Resultado de Aprendizaje del Programa evaluado en un `Momento`. Cada momento tiene sus propios RAP, que el admin crea, edita o elimina. No tiene "peso" propio: pesa lo que sumen sus criterios. |
| `CriterioEvaluacion` | rap, descripcion, orden | Ítem que se evalúa dentro de un `RAP` (p. ej. "Identificación del problema" en la rúbrica de la UPC). |
| `NivelDesempeno` | criterio, nombre, descripcion, puntaje, orden | Nivel que el evaluador puede elegir en un criterio (p. ej. Bajo 0 · Básico 0.36 · Alto 0.48 · Sobresaliente 0.6). Nombres, cantidad y puntajes los define el admin en cada criterio. |
| `ParametroGlobal` | clave, valor, descripcion | Parámetro institucional clave/valor definido por el `SuperAdmin` (p. ej. mínimo y máximo de integrantes por grupo), para no hardcodear reglas (RF 11). |

## Estructura operativa

> Se detalla con el Dev 2 (D3) cuando empiece su parte de la Etapa 1; lo que sigue
> recoge solo lo ya decidido.

| Clase | Descripción |
|---|---|
| `Grupo` | Conjunto de estudiantes (mínimo y máximo configurables; la guía indica 3 a 5) que desarrollan un `ProyectoAula`. Tiene **un solo** `Docente`. Un estudiante puede estar en varios grupos a lo largo de la carrera, pero solo en uno por momento. |
| `ProyectoAula` | Proyecto desarrollado por un `Grupo` para un `Momento` específico; contiene sus `Entrega` y `Evaluacion`. |
| `Entrega` | Versión de la entrega del proyecto (archivo zip + presentación), con registro histórico de versionamiento. |

## Evaluación y reportería

| Clase | Descripción |
|---|---|
| `Evaluacion` | Calificación que un `Evaluador` (docente, empresario o egresado) realiza sobre un `ProyectoAula`; agrupa una `CalificacionCriterio` por cada criterio configurado en el momento. |
| `CalificacionCriterio` | Nivel de desempeño que el evaluador eligió en un `CriterioEvaluacion`, con su puntaje. Reemplaza a `CalificacionRAP`: el desempeño por RAP se calcula sumando los criterios de ese RAP, y la nota de la evaluación sumando todos. |
| `Reporte` (abstracta) | Informe generado a partir de las `Evaluacion` registradas, filtrable por semestre, momento, programa o año. Define `generar()` abstracto. |
| `ReporteIndividual` / `ReporteGrupal` / `ReporteComparativo` | Subclases de `Reporte`: cada una implementa su propio `generar()` (por estudiante y RAP, por grupo, o comparando dos periodos). |

## Enumeraciones

| Enum | Valores | Descripción |
|---|---|---|
| `RolUsuario` | `SUPER_ADMIN`, `ADMIN`, `DOCENTE`, `ESTUDIANTE`, `EMPRESARIO`, `EGRESADO` | Rol del usuario; es también el discriminador de la herencia JOINED y la base del control de acceso (Spring Security). |
| `TipoDocumento` | `CC`, `TI`, `CE`, `PASAPORTE` | Tipo de documento de identidad del usuario. |

Los enums `NivelExigencia` y `NivelDesempeno` de la Fase 2 **se eliminaron**: el
nivel de exigencia es texto libre del `Momento` y los niveles de desempeño son la
entidad configurable `NivelDesempeno` (ADR-010).

## Relaciones representadas en el diagrama

- **Herencia:** `SuperAdmin`, `Admin`, `Docente`, `Estudiante`, `Empresario` y
  `Egresado` heredan de `Usuario`.
- **Realización de interfaz:** `Docente`, `Empresario` y `Egresado` implementan
  `Evaluador`.
- **Composición:** `Facultad`—`Programa`; `Programa`—`Momento`; `Momento`—`RAP`;
  `RAP`—`CriterioEvaluacion`; `CriterioEvaluacion`—`NivelDesempeno`;
  `Grupo`—`ProyectoAula`; `ProyectoAula`—`Entrega`; `ProyectoAula`—`Evaluacion`;
  `Evaluacion`—`CalificacionCriterio` (el todo controla el ciclo de vida de sus
  partes).
- **Agregación:** `Grupo`—`Estudiante` (el estudiante existe sin el grupo y pasa por
  varios grupos a lo largo de la carrera, uno por momento).
- **Asociación:** `Momento`—`PeriodoAcademico`; `Admin`—`Facultad`;
  `Docente`—`Facultad`; `Docente`—`Grupo` (un docente por grupo);
  `Estudiante`—`Programa`; `Egresado`—`Programa`; `Grupo`—`Programa`;
  `ProyectoAula`—`Momento`; `CalificacionCriterio`—`CriterioEvaluacion`;
  `CalificacionCriterio`—`NivelDesempeno`.
- **Dependencia:** `Reporte` depende de `Evaluacion` (el reporte consume datos
  de evaluación, sin poseerlos).

## Pendiente de definir

- Detalle de la estructura operativa (`Grupo`, `ProyectoAula`, `Entrega`,
  `Evaluacion`) con el Dev 2, antes de que empiece su parte de la Etapa 1.
- Ver también "Pendiente de definir con el equipo" en
  [`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md).
