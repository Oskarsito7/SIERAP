# CLAUDE.md — Contexto para Claude Code

Este archivo se carga automáticamente al iniciar una sesión de Claude Code en esta
raíz. Contiene lo esencial para trabajar en SIERAP sin tener que re-explicarlo.

## Resumen del proyecto

SIERAP (Sistema Integral Evaluador de Resultados de Aprendizajes por Programa) es un
sistema web para que la Universidad Popular del Cesar (UPC) gestione y evalúe los
proyectos de aula y los RAP (Resultados de Aprendizaje del Programa) de sus
estudiantes, reemplazando el flujo actual gestionado en AULAWEB (lento, sin
reportería, poco flexible). Contexto completo en [`README.md`](README.md) y en
`/docs`.

**Este es también el proyecto de aula (Momento 1) de la asignatura Programación II
de uno de los desarrolladores** — cualquier trabajo aquí debe poder sustentarse
también contra la rúbrica oficial de la UPC en
`docs/00-guia-oficial-momento1.md`.

## Regla de trabajo: preguntar antes de decidir

El responsable del proyecto conoce las necesidades de la universidad y del proyecto.
**No tomar decisiones por cuenta propia**: ante cualquier duda de dominio, de
alcance, de modelo de datos o de organización del equipo, preguntar primero (con
opciones concretas y una recomendación cuando la haya) y solo después implementar.
Las decisiones ya tomadas quedan registradas en `docs/04` (sección de decisiones) y
en los ADR de `docs/05`; no volver a preguntarlas.

## Stack (no negociable salvo discusión explícita)

- **Backend:** Java 21 + Spring Boot 3.x — Spring Web, Spring Data JPA, Spring
  Security + JWT. Arquitectura por capas: `controller → service → repository →
  entity`, con DTOs en el borde de la API.
- **Base de datos:** PostgreSQL.
- **Frontend:** React + TypeScript con Vite, Tailwind CSS v4 + shadcn/ui, React
  Router y axios (ADR-006).
- **Build backend:** Maven, vía Maven Wrapper (`backend/mvnw`). Paquete base
  `co.edu.unicesar.sierap`.

## Roles del sistema (fundamentales para cualquier feature)

`super_admin` (parámetros, periodos, catálogo de tipos de evaluador), `admin` (por
facultad: crea, configura y lanza los momentos de sus programas), `docente` (habilita
grupos en un momento, designa evaluadores externos, fija fechas), `estudiante` (se
inscribe en un grupo del momento, sube entregas versionadas, ve su calificación por
RAP) y `evaluador_externo` (empresario, egresado u otro tipo del catálogo
`EvaluatorType`, ADR-011). `Teacher` y `ExternalEvaluator` implementan la interfaz
`Evaluator`; el estudiante **no** se autoevalúa. Todos inician sesión con su correo.
El flujo de un momento (configuración → conformación de grupos → lanzar → entregas →
calificación) está en `docs/04` y en el ADR-012. Ver `docs/03-entidades-dominio.md`
para el modelo completo y `docs/diagramas/diagrama_clases.md` para el UML vigente (el
`.drawio` se regenera al cerrar la Etapa 1).

## Reglas de control de acceso a tener siempre presentes

- Un docente **solo** debe poder ver/gestionar/calificar los grupos que él
  habilitó — nunca todos los grupos del sistema. Los estudiantes, en cambio, ven los
  grupos de todos los docentes del momento para poder inscribirse.
- Un admin de facultad solo gestiona programas/momentos/RAP de **su** facultad.
- Los evaluadores externos solo ven y califican los grupos para los que fueron
  designados, y solo en la fecha de evaluación estipulada (dentro de la ventana de
  calificación del momento, que termina a más tardar en el límite de la UPC).
- Cada operación valida el estado y las ventanas del momento (ADR-012): la
  configuración solo se edita antes de lanzar; se entrega y se califica solo dentro
  de sus ventanas.
- Todo lo de RAP/momentos/criterios/niveles/puntuación es **configurable** por
  admin/super_admin y se versiona **por periodo académico** (ADR-010) — nada de
  valores estándar en el código: ni nombres de RAP, ni niveles, ni puntajes, ni
  rangos de semestre, ni enums de niveles.

## Convenciones de código

- **Idioma (ADR-014, aprobado por el líder):**
  - **En inglés:** el código (clases, atributos, métodos, tablas, columnas, rutas y
    campos JSON de la API, en backend y frontend), la documentación (`docs/`,
    README, ADR) y los mensajes de commit.
  - **En español:** todo texto que ve el usuario (pantallas del frontend y mensajes
    de error de la API), el **Javadoc y los comentarios del código** (son para la
    sustentación), `docs/00` (transcripción de la guía oficial) y `docs/entregas`.
  - Equivalencias español → inglés del modelo: tabla en `docs/03-entidades-dominio.md`.
- Nomenclatura de paquetes/clases/métodos/atributos siguiendo convención estándar
  de Java (paquetes en minúsculas, clases en PascalCase, métodos/atributos en
  camelCase) — esto es parte de la rúbrica de evaluación de la universidad.
- Cada clase debe llevar comentarios/documentación (Javadoc mínimo en clases de
  dominio) — también es parte de la rúbrica.
- Manejo de excepciones centralizado vía `@ControllerAdvice`, no try/catch
  disperso en controllers: los services lanzan subclases de `SierapException`
  (`ResourceNotFoundException` 404, `BusinessRuleException` 422,
  `ForbiddenAccessException` 403) y `GlobalExceptionHandler` responde con
  `ErrorResponse`.
- Sin Lombok; DTOs como `record`; inyección por constructor (ADR-008).
- `config/`, `security/`, `exception/` y `pom.xml` son del D1: preguntar antes de
  cambiarlos.
- Una migración de Flyway que ya está en `develop` nunca se edita: cualquier
  corrección va en una migración nueva.
- Puntajes y notas como `BigDecimal`, nunca `double` (ADR-013).
- Fechas: usar siempre el `Clock` inyectado (`LocalDate.now(clock)`), nunca
  `LocalDate.now()`, para poder probar reglas de fechas con `Clock.fixed`.
- Frontend: las páginas nunca llaman a axios directo, siempre vía
  `src/services/api/*`; los tipos de `src/types/` reflejan los DTO del backend.

## Dónde está cada cosa

- `docs/00-guia-oficial-momento1.md` — rúbrica oficial y requisitos de la UPC,
  úsala para verificar que cualquier entregable de universidad cumpla criterios.
- `docs/01-planteamiento-problema.md` y `docs/02-descripcion-solucion.md` — el
  porqué del proyecto, no deberían necesitar cambios frecuentes.
- `docs/03-entidades-dominio.md` — el modelo de dominio; si se agregan/cambian
  entidades, actualizar este archivo Y el diagrama en `docs/diagramas/`.
- `docs/04-requerimientos-funcionales.md` — los 12 requerimientos funcionales
  base por rol; cualquier feature nueva debería trazarse a uno de estos o
  justificar por qué es una extensión.
- `docs/05-decisiones-tecnicas.md` — registro de decisiones técnicas (ADRs).
  Añadir una entrada nueva cuando se tome una decisión de arquitectura no trivial
  (elección de librería, patrón, estructura de módulos, etc.).
- `docs/06-plan-de-trabajo.md` — etapas del desarrollo, reparto entre los tres
  integrantes, estrategia de ramas (`main`/`develop`/`feature/*`), convención de
  commits, checklist de PR y estrategia de pruebas. Consúltalo antes de abrir una
  rama o de proponer en qué orden implementar algo.
- `backend/` — API Spring Boot; `frontend/` — app React (ver `frontend/README.md`);
  `docker-compose.yml` — PostgreSQL local; `.github/workflows/ci.yml` — CI.

## Comandos

```bash
docker compose up -d                                  # PostgreSQL local (sierap/sierap)
cd backend && ./mvnw spring-boot:run                  # API en :8080 (perfil dev por defecto)
cd backend && ./mvnw verify                           # pruebas; las de integración usan Testcontainers (Docker)
cd frontend && npm run dev                            # app en :5173, proxy /api -> :8080
cd frontend && npm run lint && npm test && npm run build
```

## Al iniciar cualquier feature nueva

1. Revisar si el requerimiento ya está cubierto en
   `docs/04-requerimientos-funcionales.md`; si no, señalarlo antes de implementar.
2. Confirmar el rol/control de acceso correspondiente.
3. Si implica una entidad nueva o cambio de relación, actualizar
   `docs/03-entidades-dominio.md` y el `.drawio`.
4. Si es una decisión técnica no trivial, añadir un ADR en
   `docs/05-decisiones-tecnicas.md`.
