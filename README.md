# SIERAP — Sistema Integral Evaluador de Resultados de Aprendizajes por Programa

> Documento raíz del proyecto. Léelo primero, completo, antes de tocar código o de
> continuar cualquier conversación (IA o humana) sobre este repositorio. Contiene
> todo el contexto acordado hasta la fecha: problema, solución, alcance, stack,
> decisiones tomadas y su justificación.

**Última actualización:** ver historial de git.
**Autor / equipo:** Oskramsito y equipo — Universidad Popular del Cesar (UPC), Ing. de Sistemas.

---

## 1. Qué es SIERAP

SIERAP es un sistema web para la **gestión y evaluación de los proyectos de aula y
los RAP (Resultados de Aprendizaje del Programa)** de la Universidad Popular del
Cesar. Nace como el proyecto de aula de la asignatura **Programación II (SS300)**,
evaluado en su **Momento 1**, pero su alcance real de diseño e implementación es
**institucional**: no es un ejercicio de clase recortado, es el sistema completo que
el equipo pretende construir para reemplazar el flujo actual.

## 2. El problema que resuelve

Los RAP son obligatorios desde primer semestre en Ing. de Sistemas de la UPC y se
evalúan de forma **acumulativa** en distintos momentos del programa:

- **Momento I** — se evalúa en tercer semestre, en la asignatura Programación de
  Computadores II, pero valora todo lo aprendido desde primer semestre.
- **Momento II** — se evalúa en Ingeniería de Software II, sobre un rango de
  semestres mayor.
- **Momento III** — se evalúa en una Electiva/Optativa de profundización.

Cada momento reevalúa los mismos 5 RAP institucionales, subiendo el nivel de
exigencia (bajo → medio → alto).

En SIERAP los proyectos de los estudiantes los califican **tres tipos de
evaluadores**: docentes, empresarios y egresados (el estudiante no se autoevalúa).
Hoy el proceso se gestiona a través de **AULAWEB**, un
sistema propio (no externo) de la UPC que **no fue diseñado específicamente para
este flujo**:

- La interacción es poco intuitiva tanto para docentes como estudiantes.
- La organización de grupos, entregas, fechas y calificaciones por RAP es
  dispendiosa y en buena parte manual.
- **No genera reportes ni informes** que permitan consolidar o comparar resultados
  entre momentos, semestres, programas o años.
- Los criterios de evaluación (RAP, momentos, semestres asociados, puntuaciones)
  varían por programa y facultad, y AULAWEB no permite configurarlos de forma
  flexible por cada decano/programa.

**Consecuencia principal:** el proceso es agotador para docentes y estudiantes, los
estudiantes no tienen visibilidad clara de su desempeño acumulado por RAP, y la
universidad no puede tomar decisiones curriculares basadas en datos consolidados.

Ver el planteamiento completo (causas/consecuencias/área de aplicación) en
[`docs/01-planteamiento-problema.md`](docs/01-planteamiento-problema.md).

## 3. La solución — qué hace SIERAP

Sistema web con **6 roles**:

| Rol | Qué hace |
|---|---|
| `super_admin` | Configura los parámetros globales del sistema. |
| `admin` | Uno por facultad (lo usa el decano). Define los momentos y RAP de cada programa de su facultad. |
| `docente` | Define grupos, fechas de entrega y evaluación; crea usuarios empresario/egresado; solo ve los grupos que tiene asignados. |
| `estudiante` | Se organiza en grupos, sube entregas versionadas (zip + presentación), consulta su calificación por RAP. |
| `empresario` | Evalúa los proyectos de los grupos que tiene asignados. |
| `egresado` | Evalúa los proyectos de los grupos que tiene asignados. |

Genera **reportes individuales, grupales y comparativos** (entre semestres, momentos,
programas o años) — la carencia más crítica de AULAWEB hoy.

**Factor diferenciador:** a diferencia de AULAWEB (genérico), SIERAP está diseñado
específicamente alrededor del ciclo de vida de un RAP: `momento → nivel → programa →
grupo → entrega → evaluación multi-rol → reporte`, con configuración flexible por
facultad/programa y reportería comparativa automática.

Descripción completa de la solución en
[`docs/02-descripcion-solucion.md`](docs/02-descripcion-solucion.md).

## 4. Alcance del proyecto de aula (Momento 1) vs. alcance real

Es importante que cualquier persona (o IA) que entre a este proyecto entienda esta
distinción:

- **La guía oficial de la UPC** para el Momento 1 de Programación II pide, por
  defecto, una aplicación de consola/GUI simple con **persistencia en archivos**,
  en parejas.
- **El equipo decidió, con autorización explícita del docente** (que conoce la
  experiencia previa de +2 años en desarrollo web de uno de los integrantes), ir
  más allá: **aplicación web completa, con base de datos real**. El único requisito
  no negociable del docente es que **el backend esté en Java**.
- Por tanto, el sistema que se está construyendo **no es una maqueta académica
  reducida**: es el arranque real de SIERAP como producto, evaluado además contra
  la rúbrica oficial del Momento 1.

La rúbrica oficial completa (con puntajes) está en
[`docs/00-guia-oficial-momento1.md`](docs/00-guia-oficial-momento1.md) — cualquier
decisión de alcance para las entregas de universidad debe revisarse contra esos
criterios de evaluación.

## 5. Stack tecnológico

| Capa | Tecnología | Por qué |
|---|---|---|
| Backend | **Java + Spring Boot** (Spring Web, Spring Data JPA, Spring Security + JWT) | Requisito no negociable del docente (Java). Spring Boot es el estándar de facto para web en Java; encaja con la arquitectura por capas exigida por la guía (controller/service/repository/entity) y con la POO completa que pide la rúbrica (interfaces, herencia, manejo de excepciones vía `@ControllerAdvice`). |
| Base de datos | **PostgreSQL** | Más robusto que MySQL para las relaciones jerárquicas del dominio (facultad→programa→momento→RAP) y mejor soporte de agregaciones para reportería. MySQL es alternativa válida si el equipo prefiere. |
| Frontend | **React + TypeScript** | Stack habitual del equipo (+2 años de experiencia), ya aprobado por el docente. |

## 6. Estado actual del diseño (Momento 1 — Fase 1 y 2)

- [x] Planteamiento del problema (causas, consecuencias, área de aplicación)
- [x] Descripción de la solución (alcance, factor diferenciador, requerimientos)
- [x] Identificación de entidades del dominio
- [x] Diagrama de clases preliminar en notación UML
- [x] Diagrama de clases mejorado, de jerarquías y de paquetes (capas)
- [x] Requerimientos funcionales formalizados por rol con control de acceso
- [ ] Diseño preliminar de GUI (mockup/wireframe por rol)
- [ ] Implementación de código (avance de clases del diagrama)

Avance de la implementación, por etapas de [`docs/06-plan-de-trabajo.md`](docs/06-plan-de-trabajo.md):

- [x] **Etapa 0 — Cimientos:** esqueleto backend y frontend, PostgreSQL en
  `docker-compose`, CI en GitHub Actions.
- [ ] Etapa 1 — Modelo de dominio y persistencia (entidades JPA + Flyway).
- [ ] Etapa 2 — Autenticación y control de acceso.

## 7. Estructura de este repositorio

```
SIERAP/
├── README.md                          ← este archivo, punto de entrada
├── docs/
│   ├── 00-guia-oficial-momento1.md    ← rúbrica y requisitos oficiales de la UPC
│   ├── 01-planteamiento-problema.md   ← problema, causas, consecuencias
│   ├── 02-descripcion-solucion.md     ← alcance, factor diferenciador, stack
│   ├── 03-entidades-dominio.md        ← listado y descripción de clases
│   ├── 04-requerimientos-funcionales.md ← 12 requerimientos por rol (lluvia de ideas formalizada)
│   ├── 05-decisiones-tecnicas.md      ← ADRs: por qué Java/Spring/Postgres/React
│   ├── 06-plan-de-trabajo.md          ← etapas, reparto entre los 3 devs, ramas y pruebas
│   ├── diagramas/                     ← clases, jerarquías y capas (Mermaid + .drawio + PNG)
│   └── entregas/                      ← documentos formales de entrega (incluye presentacion-sierap.pptx)
├── backend/                           ← API Java 21 + Spring Boot 3 (Maven)
│   └── src/main/java/co/edu/unicesar/sierap/
│       ├── controller/  dto/  mapper/          ← presentación (API REST)
│       ├── service/  service/impl/             ← lógica de negocio
│       ├── repository/                         ← datos (Spring Data JPA)
│       ├── entity/  entity/enums/              ← entidades del dominio
│       └── security/  exception/  config/      ← transversales
├── frontend/                          ← app React + TypeScript (Vite, Tailwind, shadcn/ui)
├── docker-compose.yml                 ← PostgreSQL para desarrollo local
└── .github/workflows/ci.yml           ← CI: build y pruebas de backend y frontend
```

## 8. Cómo levantar el proyecto en local

**Requisitos:** JDK 21, Node.js 22 o superior y Docker (Docker Desktop en
Windows/macOS). No hace falta instalar Maven: el repo trae el Maven Wrapper
(`mvnw`).

```bash
# 1. Base de datos (PostgreSQL 17 en localhost:5432, usuario/clave: sierap/sierap)
docker compose up -d

# 2. Backend — API en http://localhost:8080   (en Windows: mvnw.cmd spring-boot:run)
cd backend
./mvnw spring-boot:run

# 3. Frontend — en otra terminal, app en http://localhost:5173
cd frontend
npm install
npm run dev
```

Si todo está bien, la página de inicio muestra **"API conectada"** y **"Base de datos
disponible"**. También se puede verificar con `curl http://localhost:8080/api/estado`.

**Pruebas:**

```bash
cd backend && ./mvnw verify                       # unitarias + integración (requiere Docker)
cd frontend && npm run lint && npm test && npm run build
```

**Configuración propia sin tocar archivos versionados:** el backend lee
`DB_URL`, `DB_USERNAME` y `DB_PASSWORD` del entorno; también se puede crear un
`backend/application-local.yml` (ignorado por git) y arrancar con
`./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,local`. Si el puerto 5432 ya
está ocupado por otro PostgreSQL: `DB_PORT=5433 docker compose up -d` y
`DB_URL=jdbc:postgresql://localhost:5433/sierap`.

## 9. Cómo usar este repositorio con Claude

- **Claude Projects (planeación, discusión con el equipo):** sube todo el contenido
  de `/docs` como Project Knowledge. Cualquier miembro del equipo que abra una
  conversación nueva dentro del Project tendrá automáticamente todo este contexto.
- **Claude Code (implementación):** clona este repo localmente y trabaja desde su
  raíz. El archivo `CLAUDE.md` en la raíz (ver siguiente) le da a Claude Code el
  mismo contexto de forma automática en cada sesión, sin que tengas que repetirlo.

## 10. Reglas de documentación del equipo

Según lo acordado, el equipo debe documentar:

- El porqué la universidad se acoge a evaluar los RAP (contexto institucional).
- Todo lo que se haga con asistencia de IA (para sustentar el proyecto ante el
  docente sin ambigüedad sobre autoría/proceso).
- El software en sí (arquitectura, decisiones, avance).

Cada decisión técnica relevante debe quedar registrada en
[`docs/05-decisiones-tecnicas.md`](docs/05-decisiones-tecnicas.md) con su
justificación, siguiendo el formato de Architecture Decision Record (ADR).
