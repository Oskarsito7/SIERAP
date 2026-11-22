-- =============================================================================
-- SIERAP · Pruebas de rechazo
--
-- Cada sentencia intenta guardar un dato que rompe una regla del negocio y el
-- servidor la rechaza. Sirven para el caso 3 de los datos de prueba y para la
-- demostración de la sustentación. Requieren haber ejecutado antes test-data.sql.
--
-- Cómo ejecutarlas:
--   * pgAdmin: seleccionar UNA sentencia con el mouse y pulsar F5 (si se ejecuta todo
--     el archivo, pgAdmin se detiene en el primer error).
--   * psql:    psql -U sierap -h localhost -d sierap -f docs/database/rejection-tests.sql
--     (psql sigue con la siguiente sentencia después de cada error).
-- Ninguna guarda nada: todas fallan. La prueba 12 muestra un borrado en cascada dentro
-- de una transacción que se deshace con ROLLBACK.
--
-- Debajo de cada sentencia está el mensaje que devuelve un servidor configurado en
-- español (el texto exacto puede variar según la versión de PostgreSQL).
-- =============================================================================

-- 1. CHECK · Un periodo no puede terminar antes de empezar.
INSERT INTO academic_period (code, start_date, end_date, upc_evaluation_deadline)
VALUES ('2027-2', DATE '2027-12-04', DATE '2027-08-02', DATE '2027-11-27');
-- ERROR: el nuevo registro para la relación «academic_period» viola la restricción
--        «check» «ck_academic_period_dates»

-- 2. CHECK · El límite de calificación de la UPC debe caer dentro del semestre.
INSERT INTO academic_period (code, start_date, end_date, upc_evaluation_deadline)
VALUES ('2027-2', DATE '2027-08-02', DATE '2027-12-04', DATE '2028-01-15');
-- ERROR: ... viola la restricción «check» «ck_academic_period_upc_deadline»

-- 3. CHECK · Las entregas no pueden empezar antes de que termine la conformación de grupos.
UPDATE moment
SET submission_start = DATE '2026-08-20'
WHERE name = 'Momento 1' AND status = 'LAUNCHED';
-- ERROR: ... viola la restricción «check» «ck_moment_submission_after_group_formation»

-- 4. CHECK · Un momento solo puede estar en configuración o lanzado.
UPDATE moment SET status = 'CLOSED' WHERE name = 'Momento 2';
-- ERROR: ... viola la restricción «check» «ck_moment_status»

-- 5. CHECK · Un grupo no puede tener un mínimo de integrantes mayor que el máximo.
UPDATE moment SET min_members = 6 WHERE name = 'Momento 2';
-- ERROR: ... viola la restricción «check» «ck_moment_members»

-- 6. UNIQUE · Un correo, una cuenta (sin importar mayúsculas: índice sobre LOWER(email)).
INSERT INTO app_user (role, first_names, last_names, document_type, document_number,
                      email, active, created_at)
VALUES ('TEACHER', 'Carlos', 'Mendoza Duplicado', 'CC', '1065999999',
        'carlos.mendoza@unicesar.edu.co', TRUE, TIMESTAMP '2026-10-30 10:00');
-- ERROR: llave duplicada viola restricción de unicidad «uk_app_user_email»

-- 7. CHECK · El correo se guarda en minúsculas.
INSERT INTO app_user (role, first_names, last_names, document_type, document_number,
                      email, active, created_at)
VALUES ('TEACHER', 'Nuevo', 'Docente', 'CC', '1065999998',
        'Nuevo.Docente@unicesar.edu.co', TRUE, TIMESTAMP '2026-10-30 10:00');
-- ERROR: ... viola la restricción «check» «ck_app_user_email_normalized»

-- 8. UNIQUE · Un evaluador califica una sola vez cada grupo.
INSERT INTO evaluation (project_group_id, evaluator_id, status, evaluated_at)
SELECT g.id, u.id, 'DRAFT', TIMESTAMP '2026-10-30 09:00'
FROM project_group g, app_user u
WHERE g.name = 'Equipo Alfa' AND u.email = 'carlos.mendoza@unicesar.edu.co';
-- ERROR: llave duplicada viola restricción de unicidad «uk_evaluation_group_evaluator»

-- 9. FOREIGN KEY · No se puede crear un programa de una facultad que no existe.
INSERT INTO program (code, name, faculty_id)
VALUES ('999', 'Programa sin facultad', 99999);
-- ERROR: inserción o actualización en la tabla «program» viola la llave foránea
--        «fk_program_faculty»

-- 10. FOREIGN KEY (RESTRICT) · No se puede borrar un nivel de desempeño que ya se usó
--     para calificar: se perderían notas puestas.
DELETE FROM performance_level
WHERE id = (SELECT performance_level_id FROM criterion_grade ORDER BY id LIMIT 1);
-- ERROR: update o delete en «performance_level» viola la llave foránea
--        «fk_criterion_grade_level» en la tabla «criterion_grade»

-- 11. FOREIGN KEY (RESTRICT) · No se puede borrar un docente que tiene grupos.
DELETE FROM teacher
WHERE id = (SELECT id FROM app_user WHERE email = 'carlos.mendoza@unicesar.edu.co');
-- ERROR: ... viola la llave foránea «fk_project_group_teacher» en la tabla «project_group»
--        (o «fk_registration_code_teacher», según cuál revise primero el servidor)

-- 12. CASCADE (no es un rechazo) · Al borrar un grupo se borran sus integrantes,
--     evaluadores, entregas y calificaciones. Se deshace con ROLLBACK.
BEGIN;
DELETE FROM project_group WHERE name = 'Equipo Alfa';
SELECT (SELECT COUNT(*) FROM project_group_member) AS integrantes,
       (SELECT COUNT(*) FROM submission)           AS entregas,
       (SELECT COUNT(*) FROM evaluation)           AS evaluaciones,
       (SELECT COUNT(*) FROM criterion_grade)      AS notas_por_criterio;
-- Antes: 18 integrantes, 6 entregas, 9 evaluaciones y 63 notas.
-- Después: 14, 4, 6 y 39.
ROLLBACK;
