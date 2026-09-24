# 04 — Requerimientos funcionales

> Formalización de los 12 requerimientos originales de la lluvia de ideas del
> equipo, organizados por rol, con las reglas de configuración transversales que
> aplican a todos. Ajustados el 2026-09-24 al flujo de un momento definido por el
> responsable del proyecto (ver "Flujo de un momento" y "Decisiones del equipo").

## Estudiante

1. Puede iniciar sesión en el sistema (con su correo).
2. Puede ver los grupos que habilitaron **todos** los docentes en un momento de su
   programa e inscribirse en el que haya acordado con sus compañeros, aunque sea de
   un docente distinto al suyo. Solo puede estar en un grupo por momento, y el
   grupo respeta el mínimo y el máximo de integrantes que definió el admin para ese
   momento.
3. Puede hacer entregas del proyecto de aula de su grupo (archivo `.zip`), con
   versionamiento/registro histórico de todas las entregas realizadas (dentro de la
   ventana de entregas y antes de la fecha límite de su grupo), y subir también su
   presentación (`.pdf` o `.docx`).
4. Puede conocer su calificación final y su desempeño individual por cada RAP.

## Evaluadores externos (hoy empresarios y egresados)

5. Pueden iniciar sesión y ver todos los grupos para los que fueron designados.
6. Pueden calificar esos grupos, únicamente en la fecha de evaluación que fijó el
   docente, dentro de la ventana de calificación del momento.

Los tipos de evaluador externo ("Empresario", "Egresado" y los que se creen después)
son un catálogo configurable, no roles fijos (ADR-011).

## Docente

7. Puede iniciar sesión, **habilitar los grupos** de un momento en los que se van a
   inscribir los estudiantes y fijar la fecha límite de entrega de cada uno (dentro
   de la ventana de entregas). **El docente solo ve, gestiona y califica los grupos
   que él habilitó.**
8. Puede crear evaluadores externos (de cualquier tipo del catálogo) y designarlos
   **solo** a los grupos que él habilitó.
9. Puede definir la fecha en que se evalúa cada uno de sus grupos, **dentro de la
   ventana de calificación del momento**, que a su vez termina a más tardar en la
   fecha límite que fija la UPC para el periodo.

## Admin (facultad / decano)

10. Crea y configura los momentos de cada programa de su facultad, en cada periodo:
    RAP, criterios de evaluación, niveles de desempeño con sus puntajes, mínimo y
    máximo de integrantes por grupo y ventanas de fechas (conformación, entregas y
    calificación). Cuando los grupos están armados, **lanza** el momento.

## Super Admin

11. Inicia sesión y define los parámetros globales del sistema, las facultades y
    programas, los periodos académicos (con la fecha límite de evaluación de la
    UPC) y el catálogo de tipos de evaluador externo.

## Transversal — Reportería

12. Se pueden generar reportes/informes individuales, grupales y completos,
    incluyendo comparativos (comparar semestre X con semestre Y, comparar por
    años, etc.).

## Flujo de un momento

Así se ejecuta el Momento 1, y exactamente igual el Momento 2, el Momento 3 y los de
cualquier programa (ADR-012):

| # | Fase | Quién | Qué hace |
|---|---|---|---|
| 1 | Configuración | Admin | Crea el momento (p. ej. "Momento 1") en el periodo y configura sus RAP, criterios (ítems de calificación), niveles con puntaje, integrantes por grupo y ventanas de fechas. |
| 2 | Configuración | Docente | Habilita sus grupos en el momento, crea y designa a sus evaluadores externos y fija las fechas de entrega y evaluación de sus grupos. |
| 3 | Conformación de grupos (ventana) | Estudiante | Entra a SIERAP, ve los grupos de todos los docentes del momento y se inscribe en el que acordó con sus compañeros ("yo voy con fulano y sutano"). |
| 4 | Lanzamiento | Admin | Lanza el momento: se bloquean la configuración y la composición de los grupos. |
| 5 | Entregas (ventana) | Estudiante | Su grupo sube versiones de la entrega (zip + presentación). |
| 6 | Calificación (ventana) | Docente y evaluadores externos | Califican solo los grupos que les corresponden, eligiendo un nivel por criterio. |
| 7 | Cierre | — | Pasada la ventana de calificación, las notas quedan definitivas y alimentan los reportes. |

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
  los crea, modifica o elimina mientras el momento está en configuración. Esa
  configuración se define por periodo académico (al empezar un semestre se copia la
  del anterior y se ajusta), para que los semestres pasados no cambien y se puedan
  comparar (ADR-010).

## Control de acceso — resumen por rol

| Rol | Puede ver | Puede modificar |
|---|---|---|
| `super_admin` | Todo | Parámetros globales, facultades, programas, periodos, catálogo de tipos de evaluador |
| `admin` | Su facultad completa | Momentos de los programas de su facultad (configuración y lanzamiento) |
| `docente` | Solo los grupos que él habilitó | Sus grupos, sus fechas, los evaluadores externos de sus grupos, sus evaluaciones |
| `estudiante` | Los grupos de los momentos de su programa; su grupo y sus notas | Su inscripción (durante la conformación) y las entregas de su grupo |
| `evaluador_externo` | Solo los grupos para los que fue designado | Sus propias evaluaciones (en la fecha estipulada) |

## Decisiones del equipo

Definidas por el responsable del proyecto el 2026-09-24.

| Tema | Decisión |
|---|---|
| Inicio de sesión | Todos los usuarios inician sesión con su **correo** (institucional, o personal en el caso de los evaluadores externos). Además se guarda tipo y número de documento de identidad. |
| Estudiante y grupos | Un estudiante tiene **un grupo por momento**; puede pasar por varios grupos a lo largo de la carrera y se conserva el historial. |
| Quién arma los grupos | El docente **habilita** grupos; los estudiantes del programa ven los grupos de **todos** los docentes del momento y se **inscriben** ellos mismos, aunque el grupo sea de otro docente. No hay lista previa de estudiantes por momento. |
| Grupo y proyecto | Cada grupo tiene **un solo** proyecto de aula: `Grupo` y `ProyectoAula` son la misma entidad. |
| Integrantes por grupo | El mínimo y el máximo los define el admin **en cada momento**. |
| Autoevaluación | El estudiante **no** se autoevalúa. Evalúan el docente del grupo y los evaluadores externos designados. |
| Tipos de evaluador | Hoy son empresario y egresado, pero se pueden crear más con otros nombres: catálogo de `TipoEvaluador` que mantiene el super_admin (ADR-011). |
| Docentes por grupo | Cada grupo tiene **un solo** docente: el que lo habilitó, y solo él lo califica (junto con los evaluadores externos que designe). |
| Docente y facultad | Cada docente pertenece a **una** facultad. Los docentes de materias de relleno que dictan en varias facultades no usan SIERAP. |
| Estructura de la evaluación | Momento → RAP → criterios → niveles de desempeño con puntaje. El evaluador elige un nivel por criterio y la nota es la suma de los puntajes. |
| Niveles | El nivel de exigencia del momento y los niveles de desempeño son configurables; no hay listas fijas. |
| Configuración en el tiempo | Se define por periodo académico; al abrir un periodo se copia la del anterior (ADR-010). |
| Ciclo del momento | Configuración (incluye conformación de grupos) → **Lanzar** (admin, con los grupos armados; bloquea configuración y grupos) → entregas → calificación → cierre (ADR-012). |
| Fechas | El admin fija las ventanas del momento (conformación, entregas, calificación) dentro del límite de la UPC; cada docente fija la fecha de entrega y de evaluación de sus grupos dentro de esas ventanas. |
| Herencia de `Usuario` | `JOINED`: tabla común `usuario` + una tabla por rol (ADR-009). |
| Datos semilla | Facultad, Ing. de Sistemas, periodo actual y el Momento 1 completo (5 RAP con los criterios y niveles de la rúbrica oficial, `docs/00`), en estado de configuración y editable. El Momento 1 evalúa del semestre 1 al 3, en parejas (mínimo y máximo 2 integrantes), sin ventanas de fechas todavía. |
| Puntajes | Se manejan como decimales exactos (`BigDecimal`), para que el Momento 1 sume exactamente 5.0 (ADR-013). |
| Idioma del código | El código va en inglés; lo que ve el usuario final (interfaz, mensajes, datos) va en español (ADR-014). |

## Pendiente de definir con el equipo

- ¿Qué pasa al lanzar un momento si hay grupos que no alcanzan el mínimo de
  integrantes, o estudiantes que no se inscribieron?
- ¿Un estudiante puede cambiarse de grupo durante la conformación?
- ¿Cómo se combinan las evaluaciones del docente y de los evaluadores externos en la
  nota final del grupo (promedio, pesos por tipo de evaluador configurables, otra
  regla)?
- ¿Quién crea las cuentas de docentes y estudiantes (super_admin, admin de
  facultad, carga masiva, autorregistro)?
- ¿Qué pasa si un docente necesita reasignar un grupo a otro docente a mitad de
  momento?
- ¿Los reportes comparativos requieren anonimizar datos de estudiantes o se
  manejan siempre a nivel agregado?
