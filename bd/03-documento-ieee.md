# Diseño e implementación de la base de datos relacional de SIERAP: un sistema de evaluación de proyectos de aula por Resultados de Aprendizaje del Programa

**Oscar Palomino Fontalvo**, *[Integrante 2]*, *[Integrante 3]*

*Programa de Ingeniería de Sistemas, Facultad de Ingenierías y Tecnologías*
*Universidad Popular del Cesar — Valledupar, Cesar, Colombia*

*Asignatura: Programación II (SS300) — Proyecto de aula, Momento 1*

---

## Resumen

La Universidad Popular del Cesar (UPC) evalúa los proyectos de aula de sus
programas mediante Resultados de Aprendizaje del Programa (RAP), y actualmente
gestiona ese proceso en AULAWEB, una plataforma lenta, sin reportería y poco
flexible. Este trabajo presenta el diseño y la implementación de la base de datos
relacional de SIERAP (Sistema Integral Evaluador de Resultados de Aprendizaje por
Programa), la pieza que soporta todo el proceso: configuración de la rúbrica,
conformación de grupos, entregas versionadas, calificación por múltiples
evaluadores y cálculo de notas. El esquema consta de 26 tablas desplegadas
mediante nueve migraciones versionadas sobre PostgreSQL. Se describen tres
decisiones de diseño que determinan su estructura: (i) la configuración de la
evaluación se versiona por periodo académico, de modo que ninguna edición
posterior altera el significado de notas pasadas; (ii) la integridad se defiende
en dos niveles, con reglas de negocio en el modelo de dominio y restricciones
declarativas en el motor; y (iii) los puntajes se almacenan como decimales
exactos, no como punto flotante. Se reporta además un conjunto de datos semilla
de demostración y su validación empírica sobre PostgreSQL 18, en la que las notas
obtenidas por consulta SQL coinciden exactamente con las que calcula la capa de
dominio.

**Palabras clave** — base de datos relacional, PostgreSQL, integridad
referencial, migraciones de esquema, resultados de aprendizaje, evaluación por
rúbrica, normalización.

---

## I. Introducción

La evaluación por Resultados de Aprendizaje del Programa exige registrar no solo
una nota final, sino la evidencia completa que la sustenta: qué RAP se evaluó,
con qué criterios, en qué nivel de desempeño se ubicó cada criterio y quién lo
calificó. Esa trazabilidad es precisamente lo que la herramienta actual de la UPC
no ofrece de forma consultable [1].

El problema no es, por tanto, principalmente de interfaz: es un problema de
**modelo de datos**. Una base de datos que guarde únicamente la nota final hace
imposible todo reporte por RAP; una que guarde la rúbrica como texto hace
imposible agregar resultados; y una que permita editar la rúbrica en el sitio
invalida retroactivamente las notas ya asignadas.

Este documento describe cómo se resolvió ese problema en SIERAP. La Sección II
sitúa el contexto. La Sección III expone la metodología de despliegue del
esquema. La Sección IV, central, detalla el diseño: los cuatro módulos del
esquema, la política de integridad referencial, la elección de tipos de datos y
la estrategia de indexación. La Sección V presenta el conjunto de datos semilla.
La Sección VI reporta la validación experimental. Las Secciones VII y VIII
recogen limitaciones y conclusiones.

## II. Contexto y antecedentes

El modelo relacional [9] es el marco natural para este dominio: la rúbrica es
intrínsecamente jerárquica (momento → RAP → criterio → nivel) y las consultas de
interés son agregaciones sobre esa jerarquía.

El referente operativo es AULAWEB, la plataforma institucional vigente. Sus
limitaciones reportadas por los docentes —lentitud, ausencia de reportes por RAP
y rigidez de la rúbrica— se tradujeron en tres requisitos no funcionales que
condicionaron el diseño:

1. **Configurabilidad total.** Ningún elemento de la rúbrica puede estar fijo en
   el código: ni los nombres de los RAP, ni los niveles de desempeño, ni los
   puntajes, ni los rangos de semestre.
2. **Comparabilidad histórica.** Los reportes deben comparar resultados entre
   semestres y entre años sin que las ediciones del semestre en curso alteren los
   anteriores.
3. **Separación estricta de visibilidad.** Un docente solo accede a los grupos
   que él habilitó; un administrador, solo a los programas de su facultad; un
   evaluador externo, solo a los grupos a los que fue designado.

La rúbrica oficial del Momento 1 [1] sirvió como caso de prueba del modelo: cinco
RAP, ocho criterios de evaluación y cuatro niveles de desempeño por criterio, con
una nota máxima de 5.00.

## III. Metodología

### A. Plataforma

El motor seleccionado es PostgreSQL [2], por su soporte completo de restricciones
declarativas (`CHECK`, claves foráneas con acciones referenciales, índices sobre
expresiones) y su adherencia al estándar SQL [3]. La aplicación es una API REST
en Java 21 con Spring Boot y Spring Data JPA [7], organizada en capas
(`controller → service → repository → entity`), con Hibernate [6] como proveedor
de persistencia.

### B. Versionado del esquema

El esquema no se genera desde las entidades de la aplicación. Se declara en
scripts SQL versionados y lo aplica Flyway [4]; Hibernate se configura en modo
`validate`, de forma que arranca únicamente si las entidades coinciden con las
tablas existentes. Esta inversión es deliberada: la base de datos es un artefacto
con ciclo de vida propio, y su evolución debe ser reproducible en los entornos de
los tres desarrolladores, en integración continua y en producción.

La regla operativa derivada es que **una migración ya publicada nunca se
modifica**. Flyway almacena una suma de verificación de cada script aplicado; una
edición posterior provoca un fallo de validación en todos los entornos que ya la
ejecutaron. Toda corrección se publica como migración nueva. La Tabla I resume
las nueve migraciones.

**TABLA I. MIGRACIONES DEL ESQUEMA**

| Migración | Contenido | Tablas |
|---|---|---|
| V1 | Estructura académica y configuración de la evaluación | 8 |
| V2 | Datos semilla de la rúbrica oficial del Momento 1 | — |
| V3 | Usuarios, roles y catálogo de tipos de evaluador | 7 |
| V4 | Operación: grupos, entregas, evaluaciones y calificaciones | 6 |
| V5 | Restricciones `CHECK` del modelo académico | — |
| V6 | Autenticación, sesiones y alta de cuentas | 3 |
| V7 | Ajustes individuales de calificación | 1 |
| V8 | Metadatos de los archivos de entrega | — |
| V9 | Pesos por tipo de evaluador | 1 |
| | **Total** | **26** |

Las migraciones V2, V5 y V8 no crean tablas: insertan datos de configuración,
agregan restricciones y amplían columnas existentes, respectivamente.

## IV. Diseño de la base de datos

### A. Visión general

El esquema se organiza en cuatro módulos funcionales, cuyo diagrama de clases
completo se encuentra en `docs/diagramas/`:

1. **Estructura académica y configuración** (8 tablas): define *qué* se evalúa.
2. **Usuarios y roles** (7 tablas): define *quién* participa.
3. **Operación y evaluación** (8 tablas): registra *qué ocurrió*.
4. **Autenticación** (3 tablas): sesiones y alta de cuentas.

### B. Módulo de configuración: versionado por periodo

El núcleo del diseño es la entidad `moment`, que representa una etapa de
evaluación de un programa **en un periodo académico determinado**. Su restricción
de unicidad lo expresa:

```sql
CONSTRAINT uk_moment_program_period_number
    UNIQUE (program_id, period_id, number)
```

De esta clave compuesta se desprende toda la arquitectura del módulo: el
"Momento 1" del periodo 2026-2 es una fila **distinta** del "Momento 1" del
periodo 2026-1, y cada una posee su propia jerarquía de RAP, criterios y niveles,
colgada mediante claves foráneas con `ON DELETE CASCADE`:

```
faculty → program → moment → learning_outcome
                               → evaluation_criterion
                                   → performance_level
```

La consecuencia es que los RAP se **repiten** en cada momento en lugar de
referenciar un catálogo único del programa. Esta redundancia aparente es la
decisión que habilita el requisito de comparabilidad histórica: si los RAP
pertenecieran al programa, reformular la redacción de un RAP alteraría el
significado de todas las evaluaciones históricas que lo referencian. Al
vincularlos al momento, cada evaluación queda ligada de forma permanente a la
configuración con la que fue realizada. Al abrir un periodo nuevo, la
configuración del anterior se copia como punto de partida.

Obsérvese también que `learning_outcome` **no** posee columna de peso: el peso de
un RAP es la suma de los puntajes máximos de sus criterios. Introducir una
columna de peso habría creado un dato derivado susceptible de quedar
desincronizado con los niveles.

Finalmente, el nivel de exigencia del momento (`requirement_level`) es texto
libre y los nombres de los niveles de desempeño (`performance_level.name`) son
`VARCHAR`, no enumeraciones. Un tipo enumerado habría impuesto una migración de
esquema cada vez que un programa quisiera nombrar distinto sus niveles, lo que
contradice el requisito de configurabilidad.

### C. Módulo de usuarios: herencia JOINED

Los cinco roles del sistema (`super_admin`, `admin`, `teacher`, `student`,
`external_evaluator`) comparten atributos de identidad y autenticación, pero
difieren en sus atributos propios: un estudiante tiene código y semestre; un
evaluador externo, organización y cargo.

Se adoptó la estrategia de herencia **JOINED** [5], [6]: una tabla `app_user` con
los atributos comunes y una tabla por rol que comparte su clave primaria con
ella. Las alternativas presentaban inconvenientes mayores: una tabla única por
jerarquía habría obligado a declarar como anulables todos los atributos
específicos, renunciando a la integridad; una tabla por clase concreta habría
duplicado la restricción de unicidad del correo electrónico en cinco tablas, sin
poder garantizarla globalmente.

Los tipos de evaluador externo (empresario, egresado y los que la universidad
defina) **no** son subclases, sino filas de un catálogo `evaluator_type`. Crear
un tipo nuevo no requiere programación.

### D. Integridad referencial: cascada y restricción

La política de borrado distingue dos naturalezas de relación, y las expresa
directamente en el DDL:

**TABLA II. ACCIONES REFERENCIALES SEGÚN EL TIPO DE RELACIÓN**

| Relación | Semántica | Acción | Efecto |
|---|---|---|---|
| `program → faculty` | Composición | `ON DELETE CASCADE` | La parte se elimina con el todo |
| `moment → program` | Composición | `ON DELETE CASCADE` | Ídem |
| `learning_outcome → moment` | Composición | `ON DELETE CASCADE` | Ídem |
| `evaluation_criterion → learning_outcome` | Composición | `ON DELETE CASCADE` | Ídem |
| `performance_level → evaluation_criterion` | Composición | `ON DELETE CASCADE` | Ídem |
| `moment → academic_period` | Asociación | *(sin acción)* = `RESTRICT` | El borrado se rechaza |
| `criterion_grade → evaluation_criterion` | Dependencia operativa | `RESTRICT` | El borrado se rechaza |
| `student → program` | Asociación | `RESTRICT` | El borrado se rechaza |

La correspondencia entre el diagrama de clases y el esquema físico es directa:
donde el modelo UML presenta composición, la clave foránea declara
`ON DELETE CASCADE`; donde presenta asociación, la clave foránea omite la acción
y PostgreSQL aplica `RESTRICT`.

El comportamiento resultante es el buscado: **la configuración que aún no se ha
usado se elimina con facilidad; la que ya tiene calificaciones no se elimina
nunca**. Si un administrador intenta borrar un criterio ya calificado, PostgreSQL
rechaza la operación completa —incluidas las cascadas que habría disparado— y la
capa de aplicación traduce la violación de integridad en una respuesta HTTP 409
Conflict con un mensaje en español.

### E. Validación en dos niveles

Las reglas de negocio se verifican en la capa de dominio, que emite mensajes
precisos, y se refuerzan con restricciones declarativas en el motor. No se trata
de duplicación: son dos niveles con alcances distintos.

Las restricciones `CHECK` de la migración V5 cubren lo que puede evaluarse dentro
de una sola fila:

```sql
ck_moment_semesters  CHECK (semester_from >= 1 AND semester_from <= semester_to)
ck_moment_members    CHECK (min_members  >= 1 AND min_members  <= max_members)
ck_moment_submission_after_group_formation
                     CHECK (group_formation_end < submission_start)
ck_moment_grading_after_submission
                     CHECK (submission_end < grading_start)
```

Estas cuatro restricciones garantizan que las tres ventanas de fechas del momento
—conformación de grupos, entregas y calificación— estén ordenadas y no se
solapen.

Un aspecto sutil merece mención. Las seis columnas de fecha del momento admiten
`NULL`, porque durante la configuración las ventanas aún no se conocen. Esto no
entra en conflicto con las restricciones anteriores: en SQL, una restricción
`CHECK` rechaza una fila únicamente cuando la condición se evalúa como **falsa**;
con un operando nulo, la lógica trivaluada del estándar [3] produce *desconocido*,
y la fila se acepta. La restricción se activa, por tanto, exactamente cuando
ambas fechas existen. El comportamiento deseado —ventanas opcionales mientras se
configura, obligatorias y coherentes al lanzar— se obtiene sin cláusulas
adicionales.

Las reglas que **no** pueden expresarse como `CHECK`, por involucrar más de una
fila o más de una tabla, se validan en el método `Moment.launch()` de la capa de
dominio:

- que la ventana de calificación termine no después del plazo institucional
  (`academic_period.upc_evaluation_deadline`): compara dos tablas;
- que los pesos del docente y de los tipos de evaluador sumen exactamente 100 %:
  compara la columna `moment.teacher_weight` con varias filas de
  `evaluator_weight`;
- que todo RAP tenga criterios y todo criterio tenga niveles.

### F. Representación numérica

Los puntajes (`performance_level.score`), las calificaciones y los pesos se
almacenan como `NUMERIC(5,2)` y se manipulan como `BigDecimal` en Java; en ningún
punto se emplea punto flotante.

La justificación es verificable con la rúbrica del Momento 1, cuyos puntajes son
0.36, 0.48, 0.60 y 0.80. Ninguno admite representación binaria exacta. La nota
máxima del momento es

> 7 criterios × 0.60 + 1 criterio × 0.80 = 4.20 + 0.80 = **5.00**

Calculada en aritmética de punto flotante de doble precisión, esa suma puede
arrojar 4.999999…, valor que se muestra incorrectamente, se compara
incorrectamente contra el umbral de aprobación y propaga el error a los promedios
de los reportes. `NUMERIC` es aritmética decimal exacta y elimina la clase
completa de errores.

Cabe destacar la regularidad de la rúbrica oficial, que el modelo reproduce sin
codificarla: los cuatro niveles de todo criterio valen 0 %, 60 %, 80 % y 100 % de
su puntaje máximo (Tabla III).

**TABLA III. NIVELES DE DESEMPEÑO Y PUNTAJES**

| Nivel | Criterio estándar | Criterio de sustentación | Proporción |
|---|---|---|---|
| Bajo | 0.00 | 0.00 | 0 % |
| Básico | 0.36 | 0.48 | 60 % |
| Alto | 0.48 | 0.64 | 80 % |
| Sobresaliente | 0.60 | 0.80 | 100 % |

### G. Indexación

PostgreSQL crea índices automáticamente para las claves primarias y para cada
restricción `UNIQUE`, pero **no** para las claves foráneas [2]. Dado que las
claves foráneas se recorren tanto en las consultas de lectura como en la
verificación de acciones referenciales al borrar, se declararon índices
explícitos sobre ellas.

La declaración no es uniforme, y la diferencia es intencionada. La columna
`moment.program_id` no requiere índice propio porque ya es la primera columna de
`uk_moment_program_period_number (program_id, period_id, number)`, y un índice
compuesto atiende las búsquedas por cualquier prefijo izquierdo. La columna
`period_id` no es prefijo de ese índice, por lo que sí se declaró
`ix_moment_period`. El mismo razonamiento aplica a `learning_outcome`, cuya clave
`uk (moment_id, code)` cubre las búsquedas por momento. En cambio,
`evaluation_criterion` y `performance_level` no poseen restricciones `UNIQUE`, y
sus claves foráneas requirieron índices explícitos.

Un caso particular es el correo electrónico del usuario. El requisito es que sea
único **sin distinguir mayúsculas**, y se implementó mediante un índice único
sobre una expresión:

```sql
CREATE UNIQUE INDEX uk_app_user_email ON app_user (LOWER(email));
```

complementado con una restricción que obliga a almacenarlo ya normalizado:

```sql
CONSTRAINT ck_app_user_email_normalized CHECK (email = LOWER(TRIM(email)))
```

### H. Modelo de calificación

La calificación de un grupo es un promedio ponderado por **tipo de evaluador**.
El peso del docente es una columna del momento (`teacher_weight`), porque todo
momento tiene docente; el peso de cada tipo de evaluador externo es una fila de
`evaluator_weight`, porque su presencia es variable. Un tipo sin fila carece de
peso en el momento.

El procedimiento de cálculo es: promediar los puntajes dentro de cada tipo de
evaluador, ponderar los promedios con los pesos del momento y dividir entre la
**suma de los pesos presentes**, no entre 100. Esta última precisión implementa
una redistribución proporcional: si un evaluador externo no envía su
calificación, su peso se reparte entre los que sí la enviaron, en lugar de
penalizar al grupo.

Sobre ese resultado grupal, la tabla `individual_grade` permite que un evaluador
ajuste el nivel de criterios específicos para un estudiante concreto —típicamente
el criterio de sustentación—, conservando una copia del puntaje del nivel elegido
y respetando la unicidad por evaluación, estudiante y criterio.

## V. Datos semilla

Se construyeron dos conjuntos de datos con propósitos distintos.

El **primero** forma parte de la migración V2 y constituye configuración real de
producción: la facultad, el programa de Ingeniería de Sistemas, el periodo
académico y el Momento 1 con la rúbrica oficial completa —5 RAP, 8 criterios y 32
niveles de desempeño [1]—. Se insertó mediante sentencias `INSERT ... SELECT` que
localizan las filas padre por su clave natural (`code = '108'`), y no por
identificadores literales, dado que estos los genera la base de datos y el script
no los conoce de antemano. Esto hace el script reproducible en cualquier
instalación.

El **segundo** es un conjunto de demostración, externo a las migraciones
(`02-seed-dev.sql`), que recorre el ciclo completo de un momento lanzado. Su
contenido se resume en la Tabla IV.

**TABLA IV. CONJUNTO DE DATOS DE DEMOSTRACIÓN**

| Entidad | Filas | Observación |
|---|---|---|
| Usuarios | 24 | 1 super admin, 2 admins, 2 docentes, 15 estudiantes, 4 evaluadores externos |
| Momentos | 2 | Uno lanzado, uno en configuración |
| Grupos | 4 | Repartidos entre dos docentes |
| Entregas | 4 | Incluye una versión múltiple y una entrega extemporánea |
| Evaluaciones | 5 | Cuatro enviadas y una en borrador |
| Calificaciones por criterio | 36 | |
| Ajustes individuales | 3 | |

Los cuatro grupos se diseñaron para cubrir estados distintos del proceso: un
grupo completamente calificado por los tres tipos de evaluador, uno con la
calificación del evaluador externo aún en borrador, uno con entrega extemporánea
y pendiente de calificar, y uno conformado sin entrega. Dos estudiantes quedan
deliberadamente sin grupo, y los grupos se reparten entre dos docentes, lo que
permite verificar las reglas de visibilidad enunciadas en la Sección II.

Este segundo conjunto se mantiene **fuera** de las migraciones de Flyway de forma
deliberada: incorporarlo como migración implicaría crear usuarios con contraseña
conocida en el entorno productivo.

## VI. Validación y resultados

El esquema y los datos de demostración se ejecutaron sobre una instancia limpia
de PostgreSQL 18 con `psql -v ON_ERROR_STOP=1`, que aborta ante el primer error.
Ambos scripts se aplicaron sin violar ninguna restricción. El script de datos se
ejecutó dos veces consecutivas para comprobar su idempotencia, con el mismo
resultado.

La validación sustantiva consistió en comparar las notas obtenidas mediante
consulta SQL directa contra las que produce la capa de dominio. La Tabla V recoge
los resultados.

**TABLA V. NOTAS OBTENIDAS POR CONSULTA SQL**

| Sujeto | Composición | Nota |
|---|---|---|
| Grupo A | Docente 60 % → 4.60; Empresario 25 % → 4.12; Egresado 15 % → 4.76 | **4.50** |
| Grupo B | Docente 60 % → 3.48 (único presente) | **3.48** |
| Estudiante con ajuste al alza | Grupo A, sustentación elevada a Sobresaliente | **4.60** |
| Estudiante con ajuste a la baja | Grupo A, sustentación reducida a Básico | **4.38** |
| Estudiantes sin ajuste | Grupo A | **4.50** |

Los valores coinciden exactamente con los esperados, incluida la redistribución
proporcional del peso del evaluador cuya calificación permanece en borrador
(Grupo B), que confirma el comportamiento descrito en la Sección IV-H.

## VII. Limitaciones y trabajo futuro

Se identifican tres limitaciones del diseño actual.

**Ausencia de índice en `program.faculty_id`.** Esta clave foránea carece de
índice explícito. El impacto es despreciable en el volumen previsto —decenas de
programas—, pero la declaración debería añadirse si la cardinalidad creciera.

**Parámetros globales como texto.** La tabla `global_parameter` implementa un
diccionario clave-valor, de modo que el administrador puede crear parámetros sin
modificar el esquema. El costo es que todo valor se almacena como cadena y su
conversión e interpretación quedan en la capa de aplicación.

**Copia de configuración entre periodos.** La estrategia de versionado por
periodo exige una operación de copia al abrir un periodo nuevo. Dicha operación
está especificada pero su implementación corresponde a una etapa posterior.

Como trabajo futuro se contempla el almacenamiento de los archivos de entrega en
un servicio de objetos en lugar del disco del servidor, y la incorporación de
vistas materializadas para los reportes comparativos entre periodos, cuyo costo
de cómputo crecerá con el histórico acumulado.

## VIII. Conclusiones

El diseño presentado demuestra que los tres requisitos no funcionales que
motivaron el proyecto se satisfacen mediante decisiones localizadas en el modelo
de datos, no mediante lógica de aplicación.

La configurabilidad total se obtiene al representar la rúbrica como datos —filas
de `learning_outcome`, `evaluation_criterion` y `performance_level`— en lugar de
como tipos enumerados. La comparabilidad histórica se obtiene al vincular esa
rúbrica al momento mediante la clave compuesta `(program_id, period_id, number)`,
de modo que toda evaluación queda ligada de forma permanente a la configuración
con la que se realizó. La integridad se obtiene al hacer corresponder las
acciones referenciales con la semántica del modelo conceptual: cascada donde hay
composición, restricción donde hay asociación o dependencia operativa.

La exactitud numérica, finalmente, no es un detalle de implementación: una
rúbrica cuya nota máxima debe ser exactamente 5.00 exige aritmética decimal, y la
elección del tipo `NUMERIC` elimina por construcción una clase completa de
errores de cálculo.

La validación empírica confirma que la base de datos, por sí sola, reproduce las
notas que calcula la aplicación, lo que acredita que el modelo contiene toda la
información necesaria para la evaluación por resultados de aprendizaje.

---

## Referencias

[1] Universidad Popular del Cesar, *Guía oficial del Momento 1 — Proyecto de aula*, documento institucional. Transcripción en `docs/00-guia-oficial-momento1.md`.

[2] PostgreSQL Global Development Group, *PostgreSQL 17 Documentation*. [En línea]. Disponible: https://www.postgresql.org/docs/17/

[3] ISO/IEC 9075-2:2023, *Information technology — Database languages SQL — Part 2: Foundation (SQL/Foundation)*. Ginebra, Suiza: ISO, 2023.

[4] Redgate Software, *Flyway Documentation*. [En línea]. Disponible: https://documentation.red-gate.com/flyway

[5] M. Fowler, *Patterns of Enterprise Application Architecture*. Boston, MA, EE. UU.: Addison-Wesley, 2003.

[6] Red Hat, *Hibernate ORM User Guide*. [En línea]. Disponible: https://hibernate.org/orm/documentation/

[7] Broadcom, *Spring Boot Reference Documentation*. [En línea]. Disponible: https://docs.spring.io/spring-boot/

[8] IEEE Std 1016-2009, *IEEE Standard for Information Technology — Systems Design — Software Design Descriptions*. Nueva York, NY, EE. UU.: IEEE, 2009.

[9] E. F. Codd, "A relational model of data for large shared data banks," *Communications of the ACM*, vol. 13, n.º 6, pp. 377–387, jun. 1970.

[10] Equipo SIERAP, *05 — Decisiones técnicas (registro de ADR)*, documento interno del proyecto. `docs/05-decisiones-tecnicas.md`.
