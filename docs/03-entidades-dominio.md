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
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md) (secciones
"Flujo de un momento" y "Decisiones del equipo") y en los ADR-009 a ADR-012 de
[`05-decisiones-tecnicas.md`](05-decisiones-tecnicas.md).

## Usuarios (jerarquía de herencia) · D1

Se persiste con `@Inheritance(JOINED)`: tabla `usuario` con los datos comunes y una
tabla por rol (ADR-009). Todos los usuarios inician sesión con su **correo**.

| Clase | Atributos | Descripción |
|---|---|---|
| `Usuario` (abstracta) | nombres, apellidos, tipoDocumento, numeroDocumento (único), correo (único, login), passwordHash, rol, activo, fechaCreacion | Datos comunes a todo usuario del sistema. De ella heredan los cinco roles. |
| `SuperAdmin` | — | Permisos globales: parámetros del sistema, periodos académicos, facultades y catálogo de tipos de evaluador. |
| `Admin` | facultad | Usuario de una `Facultad` (rol de decano). Crea y configura los `Momento` de los programas de su facultad y los **lanza**. |
| `Docente` | facultad | Pertenece a **una** `Facultad` (en SIERAP solo están los docentes con proyecto de aula, y esos dictan en su facultad). **Habilita** grupos en un momento, crea y designa evaluadores externos para ellos, fija sus fechas y califica **solo los grupos que él habilitó**. |
| `Estudiante` | codigoEstudiante (único), semestreActual, programa | Pertenece a un `Programa`. Ve los grupos de **todos** los docentes de un momento de su programa y se inscribe en uno (**un grupo por momento**, con historial). Sube entregas y consulta su desempeño por `RAP`. |
| `EvaluadorExterno` | tipoEvaluador, organizacion, cargo, anioGraduacion, programaEgreso (los cuatro últimos opcionales) | Evaluador que no es docente (hoy empresarios y egresados). Lo crea un docente y califica solo los grupos para los que fue designado (ADR-011). |
| `TipoEvaluador` | nombre, descripcion, activo | Catálogo de tipos de evaluador externo que mantiene el super_admin (datos semilla: "Empresario" y "Egresado"). Crear un tipo nuevo no requiere programar. |

**Interfaz `Evaluador`:** contrato común implementado por `Docente` y
`EvaluadorExterno`: ambos califican grupos y consultan los grupos que les
corresponden, aunque no comparten un padre propio para eso (los dos heredan de
`Usuario`, pero "evaluar" es un comportamiento transversal a roles distintos: caso
natural para una interfaz). El `Estudiante` **no** implementa `Evaluador`: en SIERAP
no hay autoevaluación.

## Estructura académica y configuración de la evaluación · D2

Toda la configuración de la evaluación se define **por periodo académico** y la
administra el admin de cada facultad; nada viene fijo en el código (ADR-010). Al
abrir un periodo se copia la configuración del periodo anterior como punto de
partida.

| Clase | Atributos | Descripción |
|---|---|---|
| `Facultad` | nombre | Unidad académica mayor de la universidad; agrupa uno o varios `Programa`. |
| `Programa` | codigo, nombre, facultad | Programa académico (p. ej. Ing. de Sistemas), perteneciente a una `Facultad`. |
| `PeriodoAcademico` | codigo (p. ej. 2026-2), fechaInicio, fechaFin, fechaLimiteEvaluacionUPC | Semestre académico con la fecha límite de evaluación fijada por la UPC. Permite validar fechas (RF 9) y comparar resultados entre semestres y años (RF 12). |
| `Momento` | programa, periodo, numero, nombre, asignatura, semestreDesde, semestreHasta, nivelExigencia (texto libre), minIntegrantes, maxIntegrantes, estado, inicioConformacion, finConformacion, inicioEntregas, finEntregas, inicioCalificacion, finCalificacion | Etapa de evaluación de un `Programa` **en un periodo**. Tiene un ciclo de vida (ADR-012): en `CONFIGURACION` el admin define sus RAP, criterios, niveles, integrantes por grupo y ventanas de fechas, y los estudiantes arman grupos; al **lanzarlo** (`LANZADO`) la configuración y los grupos quedan bloqueados y las ventanas marcan cuándo se entrega y cuándo se califica. |
| `RAP` | momento, codigo, descripcion | Resultado de Aprendizaje del Programa evaluado en un `Momento`. Cada momento tiene sus propios RAP, que el admin crea, edita o elimina mientras el momento está en configuración. Pesa lo que sumen sus criterios. |
| `CriterioEvaluacion` | rap, descripcion, orden | Ítem de calificación dentro de un `RAP` (p. ej. "Identificación del problema" en la rúbrica de la UPC). |
| `NivelDesempeno` | criterio, nombre, descripcion, puntaje, orden | Nivel que el evaluador puede elegir en un criterio (p. ej. Bajo 0 · Básico 0.36 · Alto 0.48 · Sobresaliente 0.6). Nombres, cantidad y puntajes los define el admin en cada criterio. |
| `ParametroGlobal` | clave, valor, descripcion | Parámetro institucional clave/valor definido por el `SuperAdmin` (RF 11), para no hardcodear reglas generales. |

## Estructura operativa · D3

| Clase | Atributos | Descripción |
|---|---|---|
| `Grupo` | momento, docente, nombre, tituloProyecto, integrantes, evaluadores, fechaLimiteEntrega, fechaEvaluacion | Grupo **y** proyecto de aula a la vez (cada grupo tiene un solo proyecto). Lo habilita un `Docente` en un momento; los estudiantes se inscriben (entre el mínimo y el máximo del momento). El docente le designa evaluadores externos y fija sus fechas dentro de las ventanas del momento. |
| `Entrega` | grupo, numeroVersion, rutaArchivoZip, rutaPresentacion, fechaSubida | Versión de la entrega del grupo (archivo zip + presentación). Cada subida es una versión nueva; se conserva el historial. |

## Evaluación y reportería · D3

| Clase | Descripción |
|---|---|
| `Evaluacion` | Calificación que un `Evaluador` (el docente del grupo o un evaluador externo designado) hace sobre un `Grupo`; agrupa una `CalificacionCriterio` por cada criterio del momento. |
| `CalificacionCriterio` | Nivel de desempeño que el evaluador eligió en un `CriterioEvaluacion`, con su puntaje. El desempeño por RAP se calcula sumando los criterios de ese RAP, y la nota de la evaluación, sumando todos. |
| `Reporte` (abstracta) | Informe generado a partir de las `Evaluacion` registradas, filtrable por semestre, momento, programa o año. Define `generar()` abstracto. |
| `ReporteIndividual` / `ReporteGrupal` / `ReporteComparativo` | Subclases de `Reporte`: cada una implementa su propio `generar()` (por estudiante y RAP, por grupo, o comparando dos periodos). |

## Enumeraciones

| Enum | Valores | Descripción |
|---|---|---|
| `RolUsuario` | `SUPER_ADMIN`, `ADMIN`, `DOCENTE`, `ESTUDIANTE`, `EVALUADOR_EXTERNO` | Rol del usuario; es también el discriminador de la herencia JOINED y la base del control de acceso (Spring Security). |
| `TipoDocumento` | `CC`, `TI`, `CE`, `PASAPORTE` | Tipo de documento de identidad del usuario. |
| `EstadoMomento` | `CONFIGURACION`, `LANZADO` | Estado del ciclo de vida del momento (ADR-012). Es flujo del sistema, no un valor de negocio de la evaluación. |

Los enums `NivelExigencia` y `NivelDesempeno` de la Fase 2 **se eliminaron** (el nivel
de exigencia es texto libre del `Momento` y los niveles de desempeño son la entidad
configurable `NivelDesempeno`, ADR-010), y también los roles `EMPRESARIO` y
`EGRESADO` (ahora son tipos de `EvaluadorExterno`, ADR-011).

## Relaciones representadas en el diagrama

- **Herencia:** `SuperAdmin`, `Admin`, `Docente`, `Estudiante` y `EvaluadorExterno`
  heredan de `Usuario`.
- **Realización de interfaz:** `Docente` y `EvaluadorExterno` implementan `Evaluador`.
- **Composición:** `Facultad`—`Programa`; `Programa`—`Momento`; `Momento`—`RAP`;
  `RAP`—`CriterioEvaluacion`; `CriterioEvaluacion`—`NivelDesempeno`;
  `Momento`—`Grupo`; `Grupo`—`Entrega`; `Grupo`—`Evaluacion`;
  `Evaluacion`—`CalificacionCriterio` (el todo controla el ciclo de vida de sus
  partes).
- **Agregación:** `Grupo`—`Estudiante` (el estudiante existe sin el grupo y pasa por
  varios grupos a lo largo de la carrera, uno por momento).
- **Asociación:** `Momento`—`PeriodoAcademico`; `Admin`—`Facultad`;
  `Docente`—`Facultad`; `Docente`—`Grupo` (el docente que lo habilitó);
  `Grupo`—`EvaluadorExterno` (evaluadores designados); `EvaluadorExterno`—`TipoEvaluador`;
  `Estudiante`—`Programa`; `EvaluadorExterno`—`Programa` (programa de egreso,
  opcional); `CalificacionCriterio`—`CriterioEvaluacion`;
  `CalificacionCriterio`—`NivelDesempeno`.
- **Dependencia:** `Reporte` depende de `Evaluacion` (el reporte consume datos
  de evaluación, sin poseerlos).

## Pendiente de definir

Ver "Pendiente de definir con el equipo" en
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md).
