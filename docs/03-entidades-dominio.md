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
"Flujo de un momento" y "Decisiones del equipo") y en los ADR-009 a ADR-014 de
[`05-decisiones-tecnicas.md`](05-decisiones-tecnicas.md).

> **Idioma (ADR-014):** el código está en inglés; lo que ve el usuario final (textos
> de la interfaz, mensajes de error, datos como los nombres de los RAP) está en
> español. Por eso las clases y atributos de este documento están en inglés y sus
> descripciones en español. La columna "Concepto" da el nombre del dominio en
> español.

## Usuarios (jerarquía de herencia) · D1

Se persiste con `@Inheritance(JOINED)`: tabla `app_user` con los datos comunes y una
tabla por rol (ADR-009; `user` es palabra reservada en PostgreSQL). Todos los
usuarios inician sesión con su **correo**.

| Clase | Concepto | Atributos | Descripción |
|---|---|---|---|
| `User` (abstracta) | Usuario | firstNames, lastNames, documentType, documentNumber (único), email (único, login), passwordHash, role, active, createdAt | Datos comunes a todo usuario del sistema. De ella heredan los cinco roles. |
| `SuperAdmin` | Super administrador | — | Permisos globales: parámetros del sistema, periodos académicos, facultades y catálogo de tipos de evaluador. |
| `Admin` | Administrador (decano) | faculty | Usuario de una `Faculty` (rol de decano). Crea y configura los `Moment` de los programas de su facultad y los **lanza**. |
| `Teacher` | Docente | faculty | Pertenece a **una** `Faculty` (en SIERAP solo están los docentes con proyecto de aula, y esos dictan en su facultad). **Habilita** grupos en un momento, crea y designa evaluadores externos para ellos, fija sus fechas y califica **solo los grupos que él habilitó**. |
| `Student` | Estudiante | studentCode (único), currentSemester, program | Pertenece a un `Program`. Ve los grupos de **todos** los docentes de un momento de su programa y se inscribe en uno (**un grupo por momento**, con historial). Sube entregas y consulta su desempeño por RAP. |
| `ExternalEvaluator` | Evaluador externo | evaluatorType, organization, position, graduationYear, graduationProgram (los cuatro últimos opcionales) | Evaluador que no es docente (hoy empresarios y egresados). Lo crea un docente y califica solo los grupos para los que fue designado (ADR-011). |
| `EvaluatorType` | Tipo de evaluador | name, description, active | Catálogo de tipos de evaluador externo que mantiene el super_admin (datos semilla: "Empresario" y "Egresado"). Crear un tipo nuevo no requiere programar. |

**Interfaz `Evaluator`:** contrato común implementado por `Teacher` y
`ExternalEvaluator`: ambos califican grupos y consultan los grupos que les
corresponden, aunque no comparten un padre propio para eso (los dos heredan de
`User`, pero "evaluar" es un comportamiento transversal a roles distintos: caso
natural para una interfaz). `Student` **no** implementa `Evaluator`: en SIERAP no
hay autoevaluación.

## Estructura académica y configuración de la evaluación · D2

Toda la configuración de la evaluación se define **por periodo académico** y la
administra el admin de cada facultad; nada viene fijo en el código (ADR-010). Al
abrir un periodo se copia la configuración del periodo anterior como punto de
partida.

| Clase | Concepto | Atributos | Descripción |
|---|---|---|---|
| `Faculty` | Facultad | name | Unidad académica mayor de la universidad; agrupa uno o varios `Program`. |
| `Program` | Programa | code, name, faculty | Programa académico (p. ej. Ing. de Sistemas), perteneciente a una `Faculty`. |
| `AcademicPeriod` | Periodo académico | code (p. ej. 2026-2), startDate, endDate, upcEvaluationDeadline | Semestre académico con la fecha límite de evaluación fijada por la UPC. Permite validar fechas (RF 9) y comparar resultados entre semestres y años (RF 12). |
| `Moment` | Momento | program, period, number, name, subject, semesterFrom, semesterTo, requirementLevel (texto libre), minMembers, maxMembers, status, groupFormationStart, groupFormationEnd, submissionStart, submissionEnd, gradingStart, gradingEnd | Etapa de evaluación de un `Program` **en un periodo**. Tiene un ciclo de vida (ADR-012): en `CONFIGURATION` el admin define sus RAP, criterios, niveles, integrantes por grupo y ventanas de fechas, y los estudiantes arman grupos; al **lanzarlo** (`LAUNCHED`) la configuración y los grupos quedan bloqueados y las ventanas marcan cuándo se entrega y cuándo se califica. |
| `LearningOutcome` | RAP (Resultado de Aprendizaje del Programa) | moment, code, description | RAP evaluado en un `Moment`. Cada momento tiene sus propios RAP, que el admin crea, edita o elimina mientras el momento está en configuración. Pesa lo que sumen sus criterios. |
| `EvaluationCriterion` | Criterio de evaluación | learningOutcome, description, displayOrder | Ítem de calificación dentro de un RAP (p. ej. "Identificación del problema" en la rúbrica de la UPC). |
| `PerformanceLevel` | Nivel de desempeño | criterion, name, description, score (`BigDecimal`, ADR-013), displayOrder | Nivel que el evaluador puede elegir en un criterio (p. ej. Bajo 0 · Básico 0.36 · Alto 0.48 · Sobresaliente 0.6). Nombres, cantidad y puntajes los define el admin en cada criterio. |
| `GlobalParameter` | Parámetro global | key, value, description | Parámetro institucional clave/valor definido por el `SuperAdmin` (RF 11), para no hardcodear reglas generales. |

## Estructura operativa · D3

| Clase | Concepto | Atributos | Descripción |
|---|---|---|---|
| `ProjectGroup` | Grupo (= proyecto de aula) | moment, teacher, name, projectTitle, members, evaluators, submissionDeadline, evaluationDate | Grupo **y** proyecto de aula a la vez (cada grupo tiene un solo proyecto). Lo habilita un `Teacher` en un momento; los estudiantes se inscriben (entre el mínimo y el máximo del momento). El docente le designa evaluadores externos y fija sus fechas dentro de las ventanas del momento. Se llama `ProjectGroup` porque `group` es palabra reservada en SQL. |
| `Submission` | Entrega | projectGroup, versionNumber, zipFilePath, presentationPath, submittedAt | Versión de la entrega del grupo (archivo zip + presentación). Cada subida es una versión nueva; se conserva el historial. |

## Evaluación y reportería · D3

| Clase | Concepto | Descripción |
|---|---|---|
| `Evaluation` | Evaluación | Calificación que un `Evaluator` (el docente del grupo o un evaluador externo designado) hace sobre un `ProjectGroup`; agrupa una `CriterionGrade` por cada criterio del momento. |
| `CriterionGrade` | Calificación por criterio | Nivel de desempeño que el evaluador eligió en un `EvaluationCriterion`, con su puntaje. El desempeño por RAP se calcula sumando los criterios de ese RAP, y la nota de la evaluación, sumando todos. |
| `Report` (abstracta) | Reporte | Informe generado a partir de las `Evaluation` registradas, filtrable por semestre, momento, programa o año. Define `generate()` abstracto. |
| `IndividualReport` / `GroupReport` / `ComparativeReport` | Reporte individual / grupal / comparativo | Subclases de `Report`: cada una implementa su propio `generate()` (por estudiante y RAP, por grupo, o comparando dos periodos). |

## Enumeraciones

| Enum | Valores | Descripción |
|---|---|---|
| `UserRole` | `SUPER_ADMIN`, `ADMIN`, `TEACHER`, `STUDENT`, `EXTERNAL_EVALUATOR` | Rol del usuario; es también el discriminador de la herencia JOINED y la base del control de acceso (Spring Security). |
| `DocumentType` | `CC`, `TI`, `CE`, `PASSPORT` | Tipo de documento de identidad del usuario (cédula de ciudadanía, tarjeta de identidad, cédula de extranjería, pasaporte). |
| `MomentStatus` | `CONFIGURATION`, `LAUNCHED` | Estado del ciclo de vida del momento (ADR-012). Es flujo del sistema, no un valor de negocio de la evaluación. |

Los enums `NivelExigencia` y `NivelDesempeno` de la Fase 2 **se eliminaron** (el nivel
de exigencia es texto libre del `Moment` y los niveles de desempeño son la entidad
configurable `PerformanceLevel`, ADR-010), y también los roles `EMPRESARIO` y
`EGRESADO` (ahora son tipos de `ExternalEvaluator`, ADR-011).

## Relaciones representadas en el diagrama

- **Herencia:** `SuperAdmin`, `Admin`, `Teacher`, `Student` y `ExternalEvaluator`
  heredan de `User`.
- **Realización de interfaz:** `Teacher` y `ExternalEvaluator` implementan `Evaluator`.
- **Composición:** `Faculty`—`Program`; `Program`—`Moment`; `Moment`—`LearningOutcome`;
  `LearningOutcome`—`EvaluationCriterion`; `EvaluationCriterion`—`PerformanceLevel`;
  `Moment`—`ProjectGroup`; `ProjectGroup`—`Submission`; `ProjectGroup`—`Evaluation`;
  `Evaluation`—`CriterionGrade` (el todo controla el ciclo de vida de sus partes).
- **Agregación:** `ProjectGroup`—`Student` (el estudiante existe sin el grupo y pasa
  por varios grupos a lo largo de la carrera, uno por momento).
- **Asociación:** `Moment`—`AcademicPeriod`; `Admin`—`Faculty`; `Teacher`—`Faculty`;
  `Teacher`—`ProjectGroup` (el docente que lo habilitó);
  `ProjectGroup`—`ExternalEvaluator` (evaluadores designados);
  `ExternalEvaluator`—`EvaluatorType`; `Student`—`Program`;
  `ExternalEvaluator`—`Program` (programa de egreso, opcional);
  `CriterionGrade`—`EvaluationCriterion`; `CriterionGrade`—`PerformanceLevel`.
- **Dependencia:** `Report` depende de `Evaluation` (el reporte consume datos de
  evaluación, sin poseerlos).

## Pendiente de definir

Ver "Pendiente de definir con el equipo" en
[`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md).
