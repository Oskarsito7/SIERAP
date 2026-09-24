# 06 — Plan de trabajo: etapas, equipo y ramas

> Cómo se organiza el desarrollo de SIERAP entre **3 personas**: en qué etapas se
> divide el trabajo, quién es responsable de qué, y qué convención de ramas se usa
> para no pisarse. Trazabilidad: cada etapa referencia los requerimientos
> funcionales de [`04-requerimientos-funcionales.md`](04-requerimientos-funcionales.md)
> y los criterios de la rúbrica de [`00-guia-oficial-momento1.md`](00-guia-oficial-momento1.md).

---

## 1. El equipo — 3 roles, 3 verticales

La división **no es por capas** (uno backend, uno frontend, uno base de datos):
eso genera bloqueos constantes ("no puedo avanzar hasta que el otro termine el
endpoint"). Se divide por **vertical funcional**: cada quien lleva su feature de
punta a punta (entidad → repository → service → controller → pantalla React →
pruebas), sobre una base común que se construye entre los tres al principio.

| # | Rol | Responsable | Dueño de |
|---|---|---|---|
| **D1** | Arquitecto / plataforma | Líder del proyecto (con Claude Code) | Esqueleto del proyecto, seguridad y JWT, control de acceso por rol, manejo de excepciones, CI, revisión de PRs, integridad del diagrama de clases |
| **D2** | Dominio académico y configuración | Desarrollador 1 | Facultad, Programa, PeriodoAcademico, Momento (ciclo de vida y ventanas), RAP, criterios y niveles con puntaje; pantallas de `admin` y `super_admin` |
| **D3** | Operación y evaluación | Desarrollador 2 | Grupos (proyecto de aula), inscripción, entregas versionadas, evaluación, reportería; pantallas de `docente`, `estudiante` y `evaluador_externo` |

**Reglas de convivencia:**

- **D1 es el único que toca** `config/`, `security/`, `exception/` y el `pom.xml`.
  Si D2 o D3 necesitan un cambio ahí, lo piden en el PR — no lo hacen por su cuenta.
- Las **entidades JPA se crean en la Etapa 1, en secuencia D2 → D1 → D3** (cada una
  depende de las anteriores), para que después nadie tenga que modificar el archivo
  de otro y generar conflictos.
- **Contrato antes que código:** antes de implementar una vertical se acuerda el
  contrato de la API (rutas, DTO de entrada/salida, códigos de error) y se anota en
  el PR. Así D3 puede escribir el frontend contra un contrato acordado sin esperar
  a que el backend esté listo.
- **Nadie mergea su propio PR.** Cada PR lo aprueba al menos otra persona.
- La **sustentación es individual** (rúbrica UPC): los tres deben poder explicar
  todo el sistema, no solo su vertical. Por eso las revisiones de PR son
  obligatorias — leer el código del otro es parte de estudiar para la sustentación.

---

## 2. Etapas

Duración en semanas relativas; ajustar a la fecha real de entrega del Momento 1.
Las etapas 0–2 son **secuenciales y bloqueantes**; de la 3 en adelante hay
paralelismo real.

### Etapa 0 — Cimientos (Semana 1) · los 3 juntos

Objetivo: que cualquiera pueda clonar el repo y levantar todo con un comando.

| Tarea | Responsable |
|---|---|
| Crear `develop`, proteger `main` y `develop` en GitHub | D1 |
| Esqueleto backend: Maven, Spring Boot 3.x, Java 21, estructura de paquetes por capas | D1 |
| `docker-compose.yml` con PostgreSQL + perfiles `application-dev` / `application-prod` | D1 |
| Esqueleto frontend: Vite + React + TypeScript, router, cliente HTTP, estructura de carpetas | D3 |
| GitHub Actions: build backend + tests, build frontend + lint, en cada PR | D1 |
| Diagrama de paquetes (pendiente de la Fase 2 de la rúbrica) | D2 |
| `docs/entregas/` — armar el esqueleto del documento formal de entrega | D2 |

**Definition of done:** `docker compose up` + `mvn spring-boot:run` + `npm run dev`
levantan los tres servicios en cualquier máquina del equipo; el CI pasa en verde.

**Rúbrica que cubre:** RAP2 (arquitectura por capas), RAP4 (buenas prácticas).

---

### Etapa 1 — Modelo de dominio y persistencia (Semana 2) · en secuencia D2 → D1 → D3

Objetivo: traducir `docs/03-entidades-dominio.md` a entidades JPA reales. Va **en
secuencia** porque cada parte referencia a la anterior (`Admin` apunta a `Facultad`,
`Grupo` apunta a `Docente` y `Estudiante`): cada quien empieza cuando se mergea la
parte anterior. Cada migración tiene su número reservado para que nadie choque.

| Orden | Tarea | Responsable | Migraciones |
|---|---|---|---|
| 1 | `Faculty`, `Program`, `AcademicPeriod`, `GlobalParameter`, `Moment` (+ enum `MomentStatus`), `LearningOutcome`, `EvaluationCriterion`, `PerformanceLevel` + repositorios | D2 | `V1__academic_schema.sql` |
| 1 | Datos semilla: facultad, Ing. de Sistemas, periodo actual y el Momento 1 completo (5 RAP, criterios y niveles de la rúbrica de `docs/00`) | D2 | `V2__academic_seed.sql` |
| 2 | `User` abstracta (JOINED, ADR-009) + `SuperAdmin`, `Admin`, `Teacher`, `Student`, `ExternalEvaluator` + `EvaluatorType` + enums `UserRole` y `DocumentType` + repositorios | D1 | `V3__users.sql` |
| 3 | `ProjectGroup` (grupo = proyecto de aula), `Submission`, `Evaluation`, `CriterionGrade` + repositorios, e interfaz `Evaluator` con sus implementaciones | D3 (+ D1 en `Evaluator`) | `V4__operations.sql` |
| — | Javadoc en todas las clases de dominio (exigido por rúbrica) | cada quien en lo suyo | — |

**Definition of done:** el esquema se crea solo al arrancar, hay datos semilla, y
las relaciones (herencia, composición, agregación, asociación) del diagrama UML
están reflejadas en el código.

**Rúbrica que cubre:** RAP3 (diagrama de clases), RAP2 (POO: herencia, interfaces,
encapsulamiento), RAP4 (documentación por comentarios).

---

### Etapa 2 — Autenticación y control de acceso (Semana 3) · D1 lidera

Objetivo: que el resto de etapas ya pueda asumir "hay un usuario autenticado con un rol".

| Tarea | Responsable |
|---|---|
| Spring Security + JWT: login, emisión y validación de token | D1 |
| Autorización por rol (`@PreAuthorize` / `SecurityFilterChain`) para los 5 roles | D1 |
| **Reglas de alcance**: docente solo ve sus grupos, admin solo su facultad, evaluador solo lo asignado | D1 |
| `@ControllerAdvice` global + jerarquía de excepciones propias del dominio | D1 |
| Login + guardas de ruta + persistencia de sesión en el frontend | D3 |
| Layout base por rol (menú distinto según `RolUsuario`) | D3 |

**Definition of done:** existen pruebas que demuestran que un docente **no puede**
leer un grupo ajeno y que un admin **no puede** tocar otra facultad (403, no un 200
con lista vacía).

**Cubre RF:** 1, 5, 7, 11 (la parte de inicio de sesión).
**Rúbrica:** RAP2 (manejo de excepciones), RAP5 (requerimientos de control de acceso).

---

### Etapa 3 — Configuración académica (Semanas 4–5) · D2

Objetivo: que **nada de negocio esté hardcodeado** — es el factor diferenciador del
proyecto frente a AULAWEB.

- CRUD de `Facultad` y `Programa` (`super_admin`).
- CRUD de `PeriodoAcademico` (`super_admin`) y catálogo de `TipoEvaluador`.
- CRUD de `Momento` por programa y periodo: asignatura, semestres, nivel de exigencia,
  integrantes mín./máx. y ventanas de fechas (conformación, entregas, calificación).
- CRUD de `RAP`, `CriterioEvaluacion` y `NivelDesempeno` con su puntaje, solo mientras
  el momento está en `CONFIGURACION`.
- **Lanzar** el momento (valida configuración completa y la bloquea) y **copiar la
  configuración** del periodo anterior (ADR-010, ADR-012).
- Validaciones: ventanas ordenadas y dentro del límite UPC, rangos de semestre coherentes.
- Pantallas de `admin` y `super_admin`.

**Cubre RF:** 10, 11 y las reglas transversales de configuración.

---

### Etapa 4 — Operación docente (Semanas 4–5, en paralelo con la 3) · D3

- El docente **habilita** grupos en un momento (durante la configuración); **solo ve
  los suyos**.
- Definición de la fecha límite de entrega de cada grupo, dentro de la ventana de entregas.
- Definición de la fecha de evaluación, **dentro de la ventana de calificación** (y por
  tanto del límite UPC).
- Creación de evaluadores externos (tipo del catálogo) y designación **solo a sus grupos**.

**Cubre RF:** 7, 8, 9.

---

### Etapa 5 — Estudiante y entregas versionadas (Semana 6) · D3, apoyo D2

- Ver los grupos de todos los docentes del momento e **inscribirse** en uno (un grupo
  por momento; mín./máx. de integrantes del momento; solo en la ventana de conformación).
- Subida de entrega `.zip` + presentación `.pdf` / `.docx`.
- **Versionamiento**: cada subida es una `Entrega` nueva, no reemplaza la anterior;
  historial consultable.
- Rechazo de entregas fuera de la fecha límite.
- Almacenamiento de archivos (definir en un ADR: sistema de archivos vs. columna binaria).

**Cubre RF:** 2, 3.

---

### Etapa 6 — Evaluación multi-rol (Semana 7) · D2 + D3

- Implementación real de la interfaz `Evaluador` para `Docente` y `EvaluadorExterno`
  — es el punto donde el **polimorfismo** que pide la rúbrica se hace visible.
- `Evaluacion` con sus `CalificacionCriterio` (un nivel elegido por cada criterio del momento).
- Ventana temporal: solo se puede calificar dentro de la fecha estipulada.
- Cálculo de nota consolidada por grupo y desempeño individual por RAP.
- Pantalla de calificación (evaluadores) y de resultados (estudiante).

**Cubre RF:** 4, 6. **Rúbrica:** RAP2 (polimorfismo/interfaces), RAP4 (funcionalidad
de la solución).

---

### Etapa 7 — Reportería (Semana 8) · D2, apoyo D3 en gráficas

La carencia principal de AULAWEB, y por tanto lo que más peso tiene en la sustentación.

- Reporte individual (desempeño de un estudiante por RAP).
- Reporte grupal.
- Reporte consolidado por programa/momento.
- **Comparativos**: semestre X vs. semestre Y, por años, por programa.
- Exportación (PDF/Excel — decidir alcance real según el tiempo restante).

**Cubre RF:** 12.

---

### Etapa 8 — Estabilización y entrega (Semana 9) · los 3

- Congelar features (`feature/*` deja de mergear a `develop`; se abre `release/*`).
- Pruebas de extremo a extremo de los 12 RF.
- Revisión de nomenclatura Java, indentación y Javadoc **contra la rúbrica**.
- Actualizar `03-entidades-dominio.md` y el `.drawio` con lo que quedó realmente.
- Documento formal de entrega en `docs/entregas/` + mockups + diagrama de paquetes.
- Ensayo de sustentación: **cada quien debe poder explicar las tres verticales**.
- Merge `release/*` → `main` + tag `v1.0.0-momento1`.

---

## 3. Estrategia de ramas

### 3.1 Ramas permanentes

| Rama | Propósito | Reglas |
|---|---|---|
| `main` | **Producción.** Solo código presentado/entregado y estable. | Protegida. Nadie hace push directo. Solo recibe merges de `release/*` y `hotfix/*`. Cada merge lleva un tag (`v1.0.0-momento1`). |
| `develop` | **Integración.** El estado actual del desarrollo; siempre debe compilar y pasar el CI. | Protegida. Solo recibe merges vía Pull Request aprobado. |

### 3.2 Ramas temporales

Se crean **desde `develop`** y se mergean **hacia `develop`** (excepto `hotfix/*`).
Se borran después del merge.

| Prefijo | Para qué | Ejemplo |
|---|---|---|
| `feature/` | Funcionalidad nueva | `feature/d2-crud-rap` |
| `fix/` | Corrección de un bug en desarrollo | `fix/d3-validacion-fecha-entrega` |
| `test/` | Pruebas que se agregan de forma independiente a la feature | `test/d1-control-acceso-docente` |
| `refactor/` | Reorganización sin cambio de comportamiento | `refactor/d1-extraer-service-evaluacion` |
| `docs/` | Solo documentación o diagramas | `docs/d2-diagrama-paquetes` |
| `chore/` | Configuración, dependencias, CI | `chore/d1-github-actions` |
| `release/` | Congelamiento previo a una entrega | `release/momento1` |
| `hotfix/` | Arreglo urgente sobre algo ya entregado | `hotfix/login-roto` |

**Convención de nombre:** `<prefijo>/<dev>-<descripcion-en-kebab-case>`.
El `<dev>` (`d1` / `d2` / `d3`, o las iniciales) hace obvio de un vistazo quién
trabaja en qué.

### 3.3 Reglas de release y hotfix

- `release/momento1` sale de `develop` cuando se congelan las features. Ahí solo
  entran correcciones. Al terminar se mergea a **`main` y de vuelta a `develop`**.
- `hotfix/*` sale de **`main`** (no de `develop`) y se mergea a **`main` y a
  `develop`**. Es la única rama que puede saltarse `develop`.

### 3.4 Flujo diario de cada quien

```bash
git checkout develop
git pull origin develop
git checkout -b feature/d2-crud-rap
# ... trabajar, commits pequeños y frecuentes ...
git push -u origin feature/d2-crud-rap
# abrir PR hacia develop y pedir revisión a otro del equipo
```

Si la rama lleva varios días abierta, **traer `develop` seguido** para evitar
conflictos grandes al final:

```bash
git checkout feature/d2-crud-rap
git merge origin/develop
```

### 3.5 Convención de commits

Formato Conventional Commits — hace el historial legible y sirve como evidencia de
proceso ante el docente:

```
feat(rap): CRUD de RAP configurable por momento
fix(entrega): rechazar subidas fuera de la fecha limite
test(seguridad): docente no accede a grupos ajenos
docs(entidades): actualizar diagrama con Evaluacion
chore(ci): agregar workflow de build del backend
refactor(evaluacion): extraer calculo de nota a EvaluacionService
```

Ámbitos sugeridos: `auth`, `usuario`, `facultad`, `programa`, `momento`, `rap`,
`grupo`, `entrega`, `evaluacion`, `reporte`, `ui`, `ci`.

### 3.6 Checklist de Pull Request

Un PR no se aprueba si no cumple:

- [ ] Compila y el CI está en verde.
- [ ] Tiene pruebas de lo que agrega (ver sección 4).
- [ ] Respeta la arquitectura por capas: nada de lógica de negocio en el controller,
      ninguna entidad JPA cruzando el borde de la API (siempre DTO).
- [ ] Nomenclatura Java estándar: paquetes en minúscula, clases en `PascalCase`,
      métodos y atributos en `camelCase` — **es criterio de la rúbrica**.
- [ ] Clases nuevas con Javadoc — **también es criterio de la rúbrica**.
- [ ] Sin `try/catch` disperso: las excepciones se manejan en el `@ControllerAdvice`.
- [ ] Sin valores de negocio hardcodeados (RAP, pesos y rangos de semestre van en BD).
- [ ] Si cambió el dominio: `docs/03-entidades-dominio.md` y el `.drawio` actualizados.
- [ ] Si fue una decisión técnica no trivial: ADR nuevo en `docs/05-decisiones-tecnicas.md`.

### 3.7 Protecciones a configurar en GitHub

En `Settings → Branches`, para `main` y `develop`:

- Require a pull request before merging (mínimo **1 aprobación**).
- Require status checks to pass (el workflow de CI).
- Require branches to be up to date before merging.
- Bloquear force-push y borrado de la rama.

---

## 4. Pruebas — quién prueba qué

Las pruebas **van en la misma rama de la feature**, no se dejan para el final. Una
rama `test/*` aparte solo se usa cuando se agregan pruebas a algo ya mergeado.

| Nivel | Herramienta | Qué se prueba | Responsable |
|---|---|---|---|
| Unitarias backend | JUnit 5 + Mockito | Lógica de los `Service`: cálculo de notas, validación de fechas, regla de 3–5 integrantes | cada quien en su vertical |
| Integración backend | `@SpringBootTest` + Testcontainers (o H2) | Repositorios, migraciones, consultas de reportería | D2 |
| Seguridad | `spring-security-test` (`@WithMockUser`) | **Que cada rol solo vea lo suyo** — la prueba más importante del proyecto | D1 |
| Frontend | Vitest + React Testing Library | Componentes de formulario, guardas de ruta por rol | D3 |
| Extremo a extremo | Manual guiado por los 12 RF (o Playwright si sobra tiempo) | Flujo completo por rol antes de cada entrega | los 3, en la Etapa 8 |

**Mínimo exigible por PR:** toda regla de negocio y toda regla de control de acceso
que se agregue debe tener al menos una prueba que falle si se rompe la regla.

---

## 5. Cadencia de trabajo

- **Lunes — planeación (30 min):** qué rama abre cada quien esta semana.
- **A mitad de semana — sincronización (15 min):** bloqueos y cambios de contrato de API.
- **Viernes — integración:** todo lo terminado se mergea a `develop` y se verifica
  que `develop` levanta completo. Nada se queda sin mergear el fin de semana.
- **Fin de etapa:** demo interna de 10 min (ensayo directo de la sustentación).

---

## 6. Riesgos y mitigación

| Riesgo | Mitigación |
|---|---|
| La única persona con experiencia web se vuelve cuello de botella | D1 no implementa verticales completas: construye la base y revisa. Las verticales las hacen D2 y D3. |
| Conflictos constantes en las entidades JPA | Se crean todas de una vez en la Etapa 1 y después casi no se tocan. |
| El alcance real (institucional) no cabe en el plazo del Momento 1 | Las Etapas 0–6 son el mínimo entregable. La 7 (reportería) se recorta a reportes básicos si el tiempo aprieta, dejando los comparativos para el Momento 2. |
| Frontend bloqueado esperando endpoints | Contrato de API acordado antes de implementar; D3 trabaja contra datos simulados mientras tanto. |
| Llegar a la sustentación sabiendo solo la parte propia | Revisión cruzada obligatoria de PRs + demo interna al cerrar cada etapa. |
