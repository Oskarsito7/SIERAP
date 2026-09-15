# 05 — Decisiones técnicas (ADRs)

Registro de decisiones de arquitectura relevantes, en formato breve de
Architecture Decision Record. Añadir una entrada nueva (no editar las
anteriores) cuando el equipo tome una decisión técnica no trivial.

---

## ADR-001 — Aplicación web con base de datos, en lugar de consola + archivos

**Fecha:** definida antes del inicio de la implementación.
**Estado:** Aceptada.

**Contexto:** la guía oficial de la UPC para el Momento 1 de Programación II pide,
por defecto, una aplicación de consola/GUI simple con persistencia en archivos.

**Decisión:** se desarrollará una aplicación web completa con base de datos
relacional real, dado que el docente autorizó explícitamente esta ampliación de
alcance, conociendo la experiencia previa de +2 años en desarrollo web de uno de
los integrantes del equipo.

**Restricción impuesta por el docente:** el backend debe estar en Java (no
negociable).

**Consecuencias:** el proyecto debe seguir cumpliendo los mismos criterios de
fondo de la rúbrica (POO completa, arquitectura por capas, buenas prácticas),
solo que implementados sobre un stack web en lugar de consola. Ver
`docs/00-guia-oficial-momento1.md` para la rúbrica exacta.

---

## ADR-002 — Backend en Java + Spring Boot

**Fecha:** definida antes del inicio de la implementación.
**Estado:** Aceptada.

**Contexto:** el backend debe estar en Java (restricción del docente). Se
necesita un framework que soporte una API REST completa, con arquitectura por
capas, seguridad por rol, y persistencia con un ORM.

**Decisión:** Spring Boot (Spring Web + Spring Data JPA + Spring Security con
JWT).

**Alternativas consideradas:** Java EE/Jakarta EE puro (descartado por mayor
verbosidad de configuración para un equipo pequeño y con plazos ajustados de
proyecto de aula).

**Consecuencias:** la arquitectura por capas exigida por la guía se traduce
naturalmente en `controller → service → repository → entity`. El manejo de
excepciones se centraliza con `@ControllerAdvice`. El control de acceso por rol
(6 roles) se implementa con Spring Security + un enum `RolUsuario`.

---

## ADR-003 — PostgreSQL como base de datos

**Fecha:** definida antes del inicio de la implementación.
**Estado:** Aceptada (MySQL queda como alternativa válida si el equipo cambia
de opinión).

**Contexto:** el dominio tiene una jerarquía relacional profunda
(facultad→programa→momento→RAP→evaluación→calificación) y se necesitan
agregaciones para reportería comparativa (por semestre, momento, programa, año).

**Decisión:** PostgreSQL.

**Alternativas consideradas:** MySQL — también válido, el equipo tiene más
familiaridad previa con él, pero PostgreSQL ofrece mejor soporte de
funciones de ventana y agregaciones complejas, más relevante aquí por el
módulo de reportería (la carencia principal que motiva el proyecto frente a
AULAWEB).

---

## ADR-004 — Frontend en React + TypeScript

**Fecha:** definida antes del inicio de la implementación.
**Estado:** Aceptada.

**Contexto:** se necesita una SPA con vistas diferenciadas por rol (6 roles),
formularios complejos (configuración de RAP/momentos, subida de archivos
versionada) y consumo de una API REST.

**Decisión:** React + TypeScript, por ser el stack de mayor experiencia previa
del equipo (+2 años), ya conocido y aprobado por el docente.

---

## ADR-005 — Flujo de ramas Git Flow simplificado (`main` / `develop` / `feature/*`)

**Fecha:** al inicio de la implementación, antes de crear `backend/` y `frontend/`.
**Estado:** Aceptada.

**Contexto:** el equipo son tres personas trabajando en paralelo sobre el mismo
repositorio, con niveles de experiencia distintos, y con entregas a la universidad
en fechas fijas. Trabajar todos directamente sobre `main` haría imposible tener un
estado estable garantizado el día de la sustentación, y no dejaría evidencia de
proceso (la guía pide documentar cómo se desarrolló el proyecto).

**Decisión:** Git Flow simplificado — `main` como rama de producción (solo estados
entregados y estables, con tag por entrega), `develop` como rama de integración, y
ramas temporales `feature/`, `fix/`, `test/`, `refactor/`, `docs/`, `chore/`,
`release/` y `hotfix/`. Todo entra a `develop` vía Pull Request con al menos una
aprobación de otro integrante. Detalle completo en
[`06-plan-de-trabajo.md`](06-plan-de-trabajo.md).

**Alternativas consideradas:** *trunk-based* con push directo a `main` (descartado:
sin una rama estable, cualquier commit roto la noche anterior compromete la
sustentación); una rama por persona en lugar de una por funcionalidad (descartado:
las ramas de larga vida acumulan conflictos enormes y no permiten revisión por
unidad de trabajo).

**Consecuencias:** ningún cambio llega a `main` sin pasar por `develop` y por una
revisión. La revisión cruzada obligatoria además sirve como preparación para la
sustentación, que según la rúbrica es individual y evalúa el dominio del código
completo, no solo de la parte que cada quien escribió. Requiere configurar
protección de ramas en GitHub y un workflow de CI que valide cada PR.

---

## Plantilla para nuevas entradas

```
## ADR-00X — <título corto de la decisión>

**Fecha:** <fecha>
**Estado:** Propuesta / Aceptada / Reemplazada por ADR-00Y

**Contexto:** <qué problema motiva esta decisión>

**Decisión:** <qué se decidió>

**Alternativas consideradas:** <qué otras opciones se evaluaron y por qué no>

**Consecuencias:** <qué implica esta decisión hacia adelante>
```
