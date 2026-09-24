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

## ADR-006 — Frontend con Vite, Tailwind CSS y shadcn/ui

**Fecha:** 2026-09-24 (Etapa 0).
**Estado:** Aceptada.

**Contexto:** el frontend necesita formularios, tablas, diálogos y (en reportería)
gráficas para seis roles distintos, construido por un equipo con niveles de
experiencia diferentes. El ADR-004 fijó React + TypeScript pero no la herramienta de
build ni la librería de componentes.

**Decisión:** Vite como herramienta de build; Tailwind CSS v4 para estilos;
shadcn/ui (estilo `new-york`, primitivas de Radix) como librería de componentes;
React Router para el enrutamiento y axios como cliente HTTP (un único cliente en
`services/api/httpClient.ts`). Pruebas con Vitest + React Testing Library.

**Alternativas consideradas:** Material UI (componentes listos, pero con su propio
sistema de estilos y un bundle más pesado); Ant Design (muy completo para paneles
administrativos, pero pesado y costoso de adaptar a una identidad visual propia).

**Consecuencias:** el código de cada componente queda dentro del repositorio
(`frontend/src/components/ui/`), se agrega con `npx shadcn@latest add <componente>`
y se puede modificar libremente. El tema visual se centraliza en las variables CSS de
`src/index.css`. Nota de proceso: en la sesión de IA que creó el esqueleto no había
acceso al registro de shadcn/ui, así que `components.json`, el tema y los componentes
`Button`, `Card` y `Badge` se escribieron a mano con el mismo contenido que genera el
CLI. En las máquinas del equipo el CLI funciona normalmente.

---

## ADR-007 — Pruebas de integración contra PostgreSQL real (Testcontainers)

**Fecha:** 2026-09-24 (Etapa 0).
**Estado:** Aceptada.

**Contexto:** el plan de trabajo dejaba abierto "Testcontainers (o H2)" para las
pruebas de integración del backend. Las migraciones de Flyway se escriben en SQL de
PostgreSQL, y la reportería usará funciones de ventana y agregaciones propias de
PostgreSQL (ver ADR-003).

**Decisión:** las pruebas de integración (`@SpringBootTest`) corren contra un
PostgreSQL real levantado con Testcontainers, con la misma imagen que
`docker-compose.yml` (`postgres:17-alpine`), conectado con `@ServiceConnection` de
Spring Boot (`TestcontainersConfiguration`).

**Alternativas consideradas:** H2 en memoria (más rápida y sin Docker, pero con otro
dialecto SQL: una prueba puede pasar en H2 y la misma consulta fallar en PostgreSQL,
o al revés).

**Consecuencias:** correr `./mvnw verify` completo requiere Docker (Docker Desktop en
Windows/macOS). Las pruebas unitarias con Mockito y las de `@WebMvcTest` no lo
necesitan. Los runners de GitHub Actions ya traen Docker, así que el CI no requiere
configuración adicional.

---

## ADR-008 — Convenciones del backend: sin Lombok, DTOs como `record` y reloj inyectable

**Fecha:** 2026-09-24 (Etapa 0).
**Estado:** Aceptada.

**Contexto:** la rúbrica evalúa encapsulamiento, polimorfismo, manejo de excepciones y
documentación de cada clase, y la sustentación es individual sobre el código. Además,
varias reglas de negocio dependen de la fecha actual (RF 3, 6 y 9).

**Decisión:**

- **Sin Lombok.** Las entidades declaran sus getters/setters explícitamente, con
  Javadoc.
- **DTOs como `record`** de Java (inmutables, sin código repetitivo).
- **Inyección por constructor**, nunca `@Autowired` sobre atributos.
- **Reloj inyectable:** los services obtienen la fecha con
  `LocalDate.now(reloj)`, usando el `Clock` que expone `config.RelojConfig` en la zona
  `America/Bogota`, nunca con `LocalDate.now()`.
- **Errores de negocio como subclases de `SierapException`**
  (`RecursoNoEncontradoException` → 404, `ReglaNegocioException` → 422,
  `AccesoDenegadoException` → 403); cada subclase define su código HTTP por
  polimorfismo y `GlobalExceptionHandler` responde con el formato `ErrorResponse`.

**Alternativas consideradas:** Lombok (menos código, pero el encapsulamiento deja de
verse en el código que se sustenta y agrega un procesador de anotaciones que falla en
IDEs mal configurados); `LocalDate.now()` directo (impide probar reglas de fechas sin
depender del día en que se ejecuta la prueba).

**Consecuencias:** las entidades tienen más líneas (el IDE genera getters/setters).
Toda regla de fechas se puede probar con `Clock.fixed(...)`, y los controllers no
necesitan `try/catch`.

---

## ADR-009 — Herencia de `Usuario` en JPA con estrategia `JOINED`

**Fecha:** 2026-09-24 (Etapa 1).
**Estado:** Aceptada.

**Contexto:** `Usuario` es abstracta y tiene seis subclases con atributos propios
(`Admin` → facultad; `Docente` → facultad; `Estudiante` → código, semestre y
programa; `Empresario` → empresa y cargo; `Egresado` → año de graduación y programa
de egreso; `SuperAdmin` sin atributos propios). Había que elegir cómo se guarda esa
jerarquía en PostgreSQL (`@Inheritance`).

**Decisión:** `@Inheritance(strategy = InheritanceType.JOINED)`. La tabla `usuario`
guarda los datos comunes (nombres, apellidos, documento, correo, contraseña, rol,
estado, fecha de creación) y cada rol tiene su propia tabla (`super_admin`, `admin`,
`docente`, `estudiante`, `empresario`, `egresado`) con su llave primaria como llave
foránea a `usuario.id`. La columna `usuario.rol` actúa como discriminador
(`@DiscriminatorColumn`) con los valores de `RolUsuario`, para conocer el rol sin
hacer joins (útil en el login y en Spring Security).

**Alternativas consideradas:**

- `SINGLE_TABLE`: una sola tabla con todas las columnas de todos los roles. Es más
  rápida, pero obliga a que las columnas propias de cada rol acepten `NULL` (no se
  puede exigir `NOT NULL` en `codigo_estudiante`, por ejemplo) y la tabla no se
  parece al diagrama de clases.
- `TABLE_PER_CLASS`: una tabla completa por rol, sin tabla común. Complica que el
  correo sea único entre todos los usuarios y las consultas sobre "cualquier usuario".

**Consecuencias:** el esquema refleja la jerarquía del diagrama UML (útil para la
sustentación: cada clase tiene su tabla), cada rol puede tener restricciones
`NOT NULL` y llaves foráneas propias, y el correo es único en `usuario`. Leer un
usuario concreto implica un join con la tabla de su rol, costo despreciable para el
volumen de SIERAP.

---

## ADR-010 — Configuración de la evaluación por periodo académico, sin valores estándar

**Fecha:** 2026-09-24 (Etapa 1).
**Estado:** Aceptada.

**Contexto:** los RAP, los criterios de evaluación, los niveles de desempeño y los
puntajes varían por programa, por momento y en el tiempo; cada admin los crea,
modifica o elimina según su criterio. Al mismo tiempo, la reportería (RF 12) debe
comparar resultados entre semestres, así que editar la configuración de este
semestre no puede alterar las evaluaciones de semestres anteriores.

**Decisión:**

- La configuración se versiona **por periodo académico**: `Momento` pertenece a un
  `Programa` **y** a un `PeriodoAcademico`. Cada momento tiene sus propios `RAP`;
  cada RAP, sus `CriterioEvaluacion`; y cada criterio, sus `NivelDesempeno` con su
  puntaje.
- Al abrir un periodo nuevo, la configuración del periodo anterior se **copia** como
  punto de partida (momentos → RAP → criterios → niveles) y se edita. Los periodos
  pasados quedan intactos.
- **Nada estándar en el código:** no existen los enums `NivelExigencia` ni
  `NivelDesempeno`. El nivel de exigencia del momento es texto libre, y los niveles
  de desempeño (nombre, descripción, puntaje y orden) los define el admin en cada
  criterio. El RAP no tiene "peso": pesa lo que sumen los puntajes máximos de sus
  criterios.
- El evaluador elige **un nivel por criterio**; la nota es la suma de los puntajes
  elegidos, y el desempeño por RAP es la suma de los de sus criterios
  (`CalificacionCriterio` reemplaza a `CalificacionRAP`).

**Alternativas consideradas:** una sola configuración viva editable, desactivando
(sin borrar) lo que se elimina: más simple, pero la configuración de semestres
pasados se deforma con cada edición y los comparativos pierden sentido. Puntaje
directo por RAP (sin criterios ni niveles): no refleja cómo evalúa hoy la UPC (ver la
rúbrica en `00-guia-oficial-momento1.md`, donde cada ítem tiene niveles con puntos).

**Consecuencias:** hay más entidades de configuración (`CriterioEvaluacion`,
`NivelDesempeno`) y hace falta una operación de "copiar configuración del periodo
anterior" (Etapa 3). A cambio, cada evaluación queda ligada a la configuración con
la que se hizo, y los reportes comparativos entre periodos son confiables.

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
