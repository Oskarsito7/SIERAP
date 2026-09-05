# 03 — Entidades del dominio

A continuación se listan las clases del dominio identificadas para la solución,
agrupadas por área funcional. El diagrama de clases UML correspondiente está en
[`diagramas/diagrama_clases.drawio`](diagramas/diagrama_clases.drawio) (ábrelo en
[app.diagrams.net](https://app.diagrams.net)).

## Usuarios (jerarquía de herencia)

| Clase | Descripción |
|---|---|
| `Usuario` (abstracta) | Clase base con los datos comunes a todo usuario del sistema: identificación, credenciales y estado de la cuenta. De ella heredan los seis roles del sistema. |
| `SuperAdmin` | Usuario con permisos globales: define los parámetros generales del sistema. |
| `Admin` | Usuario asociado a una `Facultad` (rol de decano). Define los `Momento` y `RAP` de cada `Programa` de su facultad. |
| `Docente` | Usuario asociado a uno o varios `Grupo`. Define grupos, fechas de entrega y evaluación, y crea usuarios `Empresario`/`Egresado`. |
| `Estudiante` | Usuario que pertenece a un `Grupo`, sube `Entrega` de su `ProyectoAula` y consulta su calificación por `RAP`. |
| `Empresario` | Usuario externo que evalúa los `ProyectoAula` de los grupos que tiene asignados. |
| `Egresado` | Usuario externo (graduado) que evalúa los `ProyectoAula` de los grupos que tiene asignados. |

**Interfaz `Evaluador`:** contrato común implementado por `Docente`, `Empresario`
y `Egresado`, ya que los tres pueden calificar proyectos y consultar sus grupos
asignados, aunque no comparten una jerarquía de herencia directa entre sí (todos
heredan de `Usuario`, pero el comportamiento de "evaluar" es transversal a roles
distintos — caso de uso natural para una interfaz en lugar de herencia).

## Estructura académica

| Clase | Descripción |
|---|---|
| `Facultad` | Unidad académica mayor de la universidad; agrupa uno o varios `Programa`. |
| `Programa` | Programa académico (p. ej. Ing. de Sistemas), perteneciente a una `Facultad`. Define sus propios `Momento` y `RAP`. |
| `Momento` | Etapa de evaluación acumulativa dentro de un `Programa` (Momento I, II, III), asociada a un rango de semestres y a una asignatura. |
| `RAP` | Resultado de Aprendizaje del Programa evaluado dentro de un `Momento`, con su descripción y peso evaluativo. |

## Estructura operativa

| Clase | Descripción |
|---|---|
| `Grupo` | Conjunto de 3 a 5 `Estudiante` que se organizan para desarrollar un `ProyectoAula`, dentro de un `Programa` y asignados a un `Docente`. |
| `ProyectoAula` | Proyecto desarrollado por un `Grupo` para un `Momento` específico; contiene sus `Entrega` y `Evaluacion`. |
| `Entrega` | Versión de la entrega del proyecto (archivo zip + presentación), con registro histórico de versionamiento. |

## Evaluación y reportería

| Clase | Descripción |
|---|---|
| `Evaluacion` | Calificación que un `Evaluador` (docente, empresario o egresado) realiza sobre un `ProyectoAula`; agrupa una o varias `CalificacionRAP`. |
| `CalificacionRAP` | Puntaje individual asignado a un `RAP` específico dentro de una `Evaluacion`. |
| `Reporte` | Informe generado (individual, grupal o comparativo) a partir de las `Evaluacion` registradas, filtrable por semestre, momento, programa o año. |

## Enumeración

| Enum | Descripción |
|---|---|
| `RolUsuario` | `SUPER_ADMIN`, `ADMIN`, `DOCENTE`, `ESTUDIANTE`, `EMPRESARIO`, `EGRESADO` — usado para el control de acceso por rol (Spring Security). |

## Relaciones representadas en el diagrama

- **Herencia:** `SuperAdmin`, `Admin`, `Docente`, `Estudiante`, `Empresario` y
  `Egresado` heredan de `Usuario`.
- **Realización de interfaz:** `Docente`, `Empresario` y `Egresado` implementan
  `Evaluador`.
- **Composición:** `Facultad`—`Programa`; `Programa`—`Momento`;
  `Grupo`—`ProyectoAula`; `ProyectoAula`—`Entrega`; `ProyectoAula`—`Evaluacion`;
  `Evaluacion`—`CalificacionRAP` (el todo controla el ciclo de vida de sus
  partes).
- **Agregación:** `Grupo`—`Estudiante` (un estudiante puede existir sin depender
  exclusivamente del grupo).
- **Asociación:** `Momento`—`RAP`; `Docente`—`Grupo`; `Grupo`—`Programa`;
  `CalificacionRAP`—`RAP`; `Admin`—`Facultad`.
- **Dependencia:** `Reporte` depende de `Evaluacion` (el reporte consume datos
  de evaluación, sin poseerlos).

## Pendiente para la Fase 2 (diagrama de clases mejorado)

- Definir si `Usuario` debe ser clase abstracta o si se modela con
  single-table/joined inheritance en JPA (`@Inheritance`).
- Evaluar si se necesitan clases abstractas adicionales o interfaces para
  estrategias de reportería (`Reporte` podría beneficiarse de un patrón
  Strategy si los tipos de reporte crecen).
- Confirmar cardinalidades exactas (p. ej. mínimo/máximo de integrantes por
  `Grupo`: la guía indica entre 3 y 5).
