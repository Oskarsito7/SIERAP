# 04 — Requerimientos funcionales

> Formalización de los 12 requerimientos originales de la lluvia de ideas del
> equipo, organizados por rol, con las reglas de configuración transversales que
> aplican a todos.

## Estudiante

1. Puede iniciar sesión en el sistema.
2. Puede escoger o crear su grupo, que sirve como identificador del grupo dentro
   del sistema (mín. 3, máx. 5 integrantes, según la guía oficial).
3. Puede hacer entregas del proyecto de aula (archivo `.zip`), con
   versionamiento/registro histórico de todas las entregas realizadas (respetando
   la fecha estipulada), y subir también su presentación (`.pdf` o `.docx`).
4. Puede conocer su calificación final y su desempeño individual por cada RAP.

## Empresario / Egresado

5. Pueden iniciar sesión y ver todos los grupos que tienen asignados para
   calificar.
6. Pueden calificar los proyectos de aula, únicamente dentro de la fecha
   estipulada por el docente.

## Docente

7. Puede iniciar sesión, definir los grupos que van a escoger los estudiantes,
   y definir las fechas de entrega del proyecto de aula. **Al docente solo le
   deben aparecer los grupos que tiene asignados para dar clases.**
8. Puede crear los usuarios/roles `Empresario` y `Egresado`. Igual que el punto
   anterior, solo debe poder asignarlos a los grupos que él mismo tiene
   asignados.
9. Puede definir la fecha o el día en que se va a evaluar el proyecto de aula
   (para que empresario y egresado puedan evaluar a los grupos), **con la
   condición de que esa fecha esté dentro del rango estipulado por la UPC.**

## Admin (facultad / decano)

10. Define los momentos y los RAP por cada programa de su facultad.

## Super Admin

11. Inicia sesión y define los parámetros globales del sistema (RAP, profesores
    por grupo, etc. a nivel institucional).

## Transversal — Reportería

12. Se pueden generar reportes/informes individuales, grupales y completos,
    incluyendo comparativos (comparar semestre X con semestre Y, comparar por
    años, etc.).

## Reglas de configuración transversales (aplican a todos los roles anteriores)

- Los criterios de evaluación son diferentes por cada programa; incluso sus RAP
  pueden ser distintos. El admin/super_admin de cada facultad y programa debe
  poder definir sus propios RAP dentro del sistema.
- Cada momento evalúa hasta un semestre distinto para un mismo programa (el
  Momento 1 evalúa hasta cierto semestre con sus RAP; el Momento 2 hasta otro
  semestre; y así hasta completar todos los momentos).
- **Todo debe ser configurable:** los criterios de evaluación, la puntuación,
  los momentos (desde/hasta qué semestre evalúa cada uno) y los RAP. Evitar
  hardcodear estos valores de negocio en el código — deben vivir en base de
  datos y ser editables desde el rol correspondiente.
- **Nada estándar, y por periodo:** cada momento tiene sus propios RAP; cada RAP,
  sus criterios; cada criterio, sus niveles de desempeño con su puntaje. El admin
  los crea, modifica o elimina. Esa configuración se define por periodo académico
  (al empezar un semestre se copia la del anterior y se ajusta), para que los
  semestres pasados no cambien y se puedan comparar (ADR-010).

## Control de acceso — resumen por rol

| Rol | Puede ver | Puede modificar |
|---|---|---|
| `super_admin` | Todo | Parámetros globales, facultades |
| `admin` | Su facultad completa | Momentos y RAP de los programas de su facultad |
| `docente` | Solo sus grupos asignados | Grupos, fechas de entrega/evaluación, usuarios empresario/egresado de sus grupos |
| `estudiante` | Su propio grupo y proyecto | Sus entregas, su grupo (mientras esté abierto) |
| `empresario` / `egresado` | Solo los grupos que se le asignaron | Sus propias evaluaciones (dentro de la fecha estipulada) |

## Decisiones del equipo

Definidas por el responsable del proyecto el 2026-09-24.

| Tema | Decisión |
|---|---|
| Inicio de sesión | Todos los usuarios inician sesión con su **correo** (institucional o personal en el caso de empresarios y egresados). Además se guarda tipo y número de documento de identidad. |
| Estudiante y grupos | Un estudiante tiene **un grupo por momento**; puede pasar por varios grupos a lo largo de la carrera y se conserva el historial. |
| Autoevaluación | El estudiante **no** se autoevalúa. Evalúan docente, empresario y egresado. |
| Docentes por grupo | Cada grupo tiene **un solo** docente. |
| Docente y facultad | Cada docente pertenece a **una** facultad. Los docentes de materias de relleno que dictan en varias facultades no usan SIERAP. |
| Estructura de la evaluación | Momento → RAP → criterios → niveles de desempeño con puntaje. El evaluador elige un nivel por criterio y la nota es la suma de los puntajes. |
| Niveles | El nivel de exigencia del momento y los niveles de desempeño son configurables; no hay listas fijas. |
| Configuración en el tiempo | Se define por periodo académico; al abrir un periodo se copia la del anterior (ADR-010). |
| Herencia de `Usuario` | `JOINED`: tabla común `usuario` + una tabla por rol (ADR-009). |

## Pendiente de definir con el equipo

- ¿Qué pasa si un docente necesita reasignar un grupo a otro docente a mitad de
  momento?
- ¿Los reportes comparativos requieren anonimizar datos de estudiantes o se
  manejan siempre a nivel agregado?
- ¿Cómo se combinan las evaluaciones de docente, empresario y egresado en la nota
  final del proyecto (promedio, pesos por tipo de evaluador configurables, otra
  regla)?
- ¿Quién crea las cuentas de docentes y estudiantes (super_admin, admin de
  facultad, carga masiva, autorregistro)?
