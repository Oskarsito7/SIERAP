# `bd/` — Scripts de base de datos para pgAdmin

Esta carpeta **no** hace parte del arranque de la aplicación: son los scripts para
tener la base de SIERAP en un PostgreSQL administrado con pgAdmin (el que está
instalado en el equipo), con datos de demostración listos para la sustentación.

La fuente de verdad del esquema siguen siendo las migraciones de Flyway en
`backend/src/main/resources/db/migration/`. Nada de lo que hay aquí las reemplaza.

| Archivo | Qué es |
|---|---|
| `01-schema.sql` | Todas las migraciones (V1..V9) concatenadas en orden. **Generado**, no editar a mano. |
| `generate-schema.sh` | Regenera `01-schema.sql` desde las migraciones: `bash bd/generate-schema.sh`. |
| `02-seed-dev.sql` | Datos de demostración: usuarios, grupos, entregas, evaluaciones y notas. Solo desarrollo. |
| `03-documento-ieee.md` | Documento en formato IEEE sobre el diseño de la base de datos: modelo, integridad, tipos de datos, índices y validación. |

---

## Opción A — pgAdmin contra el PostgreSQL de Docker (sin mover nada)

pgAdmin es solo un cliente: puede administrar el contenedor que ya existe sin
copiar nada. Es la opción recomendada si el equipo sigue trabajando con Docker.

1. `docker compose up -d`
2. En pgAdmin: clic derecho en **Servers → Register → Server…**
   - *General → Name*: `SIERAP (Docker)`
   - *Connection → Host*: `localhost` · *Port*: `5432`
   - *Maintenance database*: `sierap` · *Username*: `sierap` · *Password*: `sierap`
3. Guardar. La base aparece en `Servers → SIERAP (Docker) → Databases → sierap`.

Para los datos de demostración: abrir el **Query Tool** sobre `sierap`, abrir
`bd/02-seed-dev.sql` con el icono de carpeta y ejecutarlo (F5). El esquema ya lo
creó Flyway cuando arrancó el backend.

---

## Opción B — Pasar la base al PostgreSQL local (el de pgAdmin)

El equipo tiene **PostgreSQL 18** instalado en
`C:\Program Files\PostgreSQL\18`. Como usa el puerto `5432`, igual que el
contenedor, hay que decidir cuál queda ahí: lo más simple es apagar el
contenedor de Postgres (`docker compose stop postgres`) y dejar solo el local.
Mailpit puede seguir arriba.

### B.1 · Crear la base desde cero con los scripts (recomendado)

En pgAdmin, conectado al servidor local como `postgres`, abrir el Query Tool
sobre la base `postgres` y ejecutar:

```sql
CREATE ROLE sierap WITH LOGIN PASSWORD 'sierap';
CREATE DATABASE sierap OWNER sierap;
```

Desde ahí hay dos caminos:

**B.1.a — Dejar que el backend cree el esquema (más limpio)**

1. No ejecutar `01-schema.sql`.
2. Arrancar el backend: Flyway encuentra la base vacía y aplica V1..V9.
3. Ejecutar `bd/02-seed-dev.sql` en el Query Tool sobre `sierap`.

**B.1.b — Crear el esquema a mano desde pgAdmin**

1. Query Tool sobre `sierap` → abrir `bd/01-schema.sql` → ejecutar.
2. Ejecutar `bd/02-seed-dev.sql`.
3. Si después el backend va a apuntar a esta base, arrancarlo **una vez** con la
   línea base de Flyway (el esquema ya existe, pero sin la tabla de historial):

   ```bash
   cd backend && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.flyway.baseline-on-migrate=true --spring.flyway.baseline-version=9"
   ```

   Flyway crea `flyway_schema_history` con la versión 9 como línea base y no
   vuelve a aplicar V1..V9. De la V10 en adelante se aplican con normalidad y ya
   no hace falta volver a pasar esos argumentos.

### B.2 · Copiar lo que ya está en el contenedor

Si la base de Docker tiene datos que vale la pena conservar, se copia con
`pg_dump`. Se usa el `pg_dump` de **dentro** del contenedor (es PostgreSQL 17) y
el `pg_restore` local (18): un respaldo de una versión anterior se restaura sin
problema en una posterior, al contrario no.

```bash
docker compose exec -T postgres pg_dump -U sierap -d sierap -Fc -f /tmp/sierap.dump
```

```bash
docker cp sierap-postgres:/tmp/sierap.dump ./db/sierap.dump
```

Con el contenedor de Postgres apagado y la base `sierap` creada en el servidor
local (el `CREATE ROLE` / `CREATE DATABASE` de arriba):

```bash
"/c/Program Files/PostgreSQL/18/bin/pg_restore.exe" -h localhost -p 5432 -U sierap -d sierap --clean --if-exists ./db/sierap.dump
```

El respaldo incluye `flyway_schema_history`, así que el backend arranca contra
esa base sin necesidad de línea base. El archivo `.dump` es un respaldo local:
no se versiona (`bd/*.dump` está en `.gitignore`).

### Apuntar el backend a la base local

Los valores por defecto del perfil `dev` ya sirven si el servidor local quedó en
`localhost:5432` con el usuario `sierap`/`sierap`. Si cambia algo, se pasa por
entorno sin tocar los `application-*.yml`:

```bash
DB_URL=jdbc:postgresql://localhost:5432/sierap DB_USERNAME=sierap DB_PASSWORD=sierap ./mvnw spring-boot:run
```

---

## Usuarios de la semilla

Todos entran con la contraseña **`Sierap-Inicio-2026`**.

| Rol | Correo | Para probar |
|---|---|---|
| `super_admin` | `sierap@unicesar.edu.co` | Parámetros, periodos y catálogo de tipos de evaluador. Viene de la V6 y **exige cambiar la contraseña** en el primer ingreso. |
| `admin` | `luz.castro@unicesar.edu.co` | Configurar y lanzar momentos, RAP y rúbrica de la Facultad de Ingenierías. |
| `admin` | `hernan.molina@unicesar.edu.co` | Segundo admin de la misma facultad. |
| `docente` | `edwin.mendoza@unicesar.edu.co` | Dueño de los grupos A, B y C: calificar, designar evaluadores, ver estudiantes sin grupo. |
| `docente` | `claudia.onate@unicesar.edu.co` | Dueña solo del grupo D: sirve para comprobar que no ve los grupos de Edwin. |
| `estudiante` | `oscar.palomino@unicesar.edu.co` | Grupo A, calificado: ve su nota por RAP (4.50). |
| `estudiante` | `maria.ospino@unicesar.edu.co` | Grupo A con ajuste individual al alza (4.60). |
| `estudiante` | `juan.araujo@unicesar.edu.co` | Grupo A con ajuste individual a la baja (4.38). |
| `estudiante` | `natalia.fuentes@unicesar.edu.co` | Sin grupo: cerró la conformación y quedó fuera. |
| `evaluador_externo` | `jorge.daza@softcaribe.com.co` | Empresario, ya calificó el grupo A. |
| `evaluador_externo` | `yuranis.quintero@valleindustrial.co` | Empresaria con la calificación del grupo B en borrador. |
| `evaluador_externo` | `andres.ramirez@outlook.com` | Egresado (2019), ya calificó el grupo A. |
| `evaluador_externo` | `diana.perez@bancodeoccidente.co` | Egresada designada al grupo C, aún sin calificar. |

Para que el super_admin tampoco tenga que cambiar la contraseña:

```sql
UPDATE app_user SET must_change_password = FALSE WHERE email = 'sierap@unicesar.edu.co';
```

Los archivos de las entregas no existen en disco: las filas de `submission`
traen rutas con el formato real, pero la descarga responde 404 hasta que se
suban archivos desde el frontend.

---

## Consultas útiles en el Query Tool

Las dos reproducen la fórmula de `GradeCalculator` (ADR-018): se promedia dentro
de cada tipo de evaluador, se pondera con los pesos del momento y, si un tipo no
tiene evaluaciones enviadas, su peso se reparte entre los presentes (se divide
entre la suma de los pesos presentes, no entre 100).

**Nota de cada grupo**

```sql
WITH submitted AS (
    SELECT e.id, e.project_group_id, e.evaluator_id, SUM(cg.score) AS total
    FROM evaluation e
    JOIN criterion_grade cg ON cg.evaluation_id = e.id
    WHERE e.status = 'SUBMITTED'
    GROUP BY e.id
), typed AS (
    SELECT s.project_group_id,
           COALESCE(ety.name, 'Docente') AS evaluator_type,
           COALESCE(ew.weight, m.teacher_weight) AS weight,
           s.total
    FROM submitted s
    JOIN project_group g ON g.id = s.project_group_id
    JOIN moment m ON m.id = g.moment_id
    LEFT JOIN external_evaluator ee ON ee.id = s.evaluator_id
    LEFT JOIN evaluator_type ety ON ety.id = ee.evaluator_type_id
    LEFT JOIN evaluator_weight ew ON ew.moment_id = m.id
                                 AND ew.evaluator_type_id = ee.evaluator_type_id
), per_type AS (
    SELECT project_group_id, evaluator_type, weight, AVG(total) AS avg_total
    FROM typed
    GROUP BY project_group_id, evaluator_type, weight
)
SELECT g.name AS grupo,
       string_agg(pt.evaluator_type || ' ' || pt.weight || '% -> '
                  || ROUND(pt.avg_total, 2), ' | ' ORDER BY pt.evaluator_type) AS detalle,
       ROUND(SUM(pt.weight * pt.avg_total) / SUM(pt.weight), 2) AS nota_final
FROM per_type pt
JOIN project_group g ON g.id = pt.project_group_id
GROUP BY g.id, g.name
ORDER BY g.name;
```

Con la semilla devuelve `Grupo A → 4.50` y `Grupo B → 3.48`.

**Nota de cada estudiante** (aplica los ajustes individuales de `individual_grade`)

```sql
WITH student_eval AS (
    SELECT e.id AS evaluation_id, e.project_group_id, e.evaluator_id, pgm.student_id,
           SUM(COALESCE(ig.score, cg.score)) AS total
    FROM evaluation e
    JOIN project_group_member pgm ON pgm.project_group_id = e.project_group_id
    JOIN criterion_grade cg ON cg.evaluation_id = e.id
    LEFT JOIN individual_grade ig ON ig.evaluation_id = e.id
                                 AND ig.student_id = pgm.student_id
                                 AND ig.criterion_id = cg.criterion_id
    WHERE e.status = 'SUBMITTED'
    GROUP BY e.id, pgm.student_id
), typed AS (
    SELECT se.student_id, se.project_group_id,
           COALESCE(ety.name, 'Docente') AS evaluator_type,
           COALESCE(ew.weight, m.teacher_weight) AS weight, se.total
    FROM student_eval se
    JOIN project_group g ON g.id = se.project_group_id
    JOIN moment m ON m.id = g.moment_id
    LEFT JOIN external_evaluator ee ON ee.id = se.evaluator_id
    LEFT JOIN evaluator_type ety ON ety.id = ee.evaluator_type_id
    LEFT JOIN evaluator_weight ew ON ew.moment_id = m.id
                                 AND ew.evaluator_type_id = ee.evaluator_type_id
), per_type AS (
    SELECT student_id, project_group_id, evaluator_type, weight, AVG(total) AS avg_total
    FROM typed
    GROUP BY student_id, project_group_id, evaluator_type, weight
)
SELECT g.name AS grupo,
       u.first_names || ' ' || u.last_names AS estudiante,
       ROUND(SUM(pt.weight * pt.avg_total) / SUM(pt.weight), 2) AS nota_individual
FROM per_type pt
JOIN project_group g ON g.id = pt.project_group_id
JOIN app_user u ON u.id = pt.student_id
GROUP BY g.name, u.id, u.first_names, u.last_names
ORDER BY g.name, u.first_names;
```

---

## Cómo se verificó

`01-schema.sql` y `02-seed-dev.sql` se ejecutaron sobre un PostgreSQL 18 limpio
(`psql -v ON_ERROR_STOP=1`), dos veces seguidas para comprobar que el seed es
repetible, y las notas que devuelven las consultas de arriba coinciden con lo que
calcula `GradeCalculator`.
