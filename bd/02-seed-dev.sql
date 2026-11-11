-- =============================================================================
-- SIERAP · 02-seed-dev.sql — Datos semilla de demostración
-- =============================================================================
-- SOLO PARA DESARROLLO Y SUSTENTACIÓN. Nunca ejecutar en producción: crea
-- usuarios con una contraseña conocida y borra los usuarios existentes.
--
-- Requisito: las migraciones V1..V9 ya aplicadas (por el backend al arrancar, o
-- ejecutando bd/01-schema.sql). Este archivo NO crea tablas, solo datos.
--
-- Qué deja en la base (el periodo 2026-2 visto a 2026-10-30, con la ventana de
-- calificación abierta):
--
--   * Momento 1 LANZADO con sus tres ventanas y los pesos 60 / 25 / 15
--     (docente / Empresario / Egresado), y Momento 2 en CONFIGURACIÓN con sus
--     RAP pero sin criterios todavía.
--   * 2 admins de facultad, 2 docentes, 15 estudiantes (13 en grupo, 2 sin
--     grupo) y 4 evaluadores externos (2 empresarios, 2 egresados).
--   * 4 grupos en cuatro estados distintos:
--       - Grupo A  entregó 2 versiones y está calificado por los 3 evaluadores
--                  → nota del grupo 4.50, y con los ajustes individuales de
--                  sustentación: 4.60 María, 4.38 Juan, 4.50 Oscar y Laura
--       - Grupo B  entregó, el docente ya calificó y el empresario va en
--                  borrador (no cuenta: el peso ausente se reparte, así que la
--                  nota queda en 3.48, la del docente)
--       - Grupo C  entregó tarde y aún no se califica (su fecha de evaluación
--                  es 2026-11-06, todavía no llega)
--       - Grupo D  es de la otra docente, está conformado y sin entrega
--     Los grupos A, B y C son del docente Edwin y el D de la docente Claudia,
--     para poder comprobar que cada docente solo ve los grupos que habilitó.
--
-- CONTRASEÑA de todos los usuarios creados aquí: Sierap-Inicio-2026
-- (el mismo hash bcrypt del super_admin de la V6; estos sí entran directo,
-- quedan con must_change_password = FALSE).
--
-- Los archivos de las entregas no existen en disco: las filas de submission
-- traen rutas válidas, pero al descargarlas la API responde 404. Para probar la
-- descarga hay que subir los archivos desde el frontend.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 0 · Limpieza: hace el script repetible
-- -----------------------------------------------------------------------------
-- OJO: borra TODOS los usuarios menos el super_admin de la V6 y todo lo que
-- cuelgue de ellos (grupos, entregas, evaluaciones y notas). Si en la base hay
-- datos que se quieren conservar, comentar esta sección completa.

DELETE FROM project_group
WHERE moment_id IN (SELECT id FROM moment);

UPDATE student SET registration_code_id = NULL;
DELETE FROM registration_code;

DELETE FROM app_user
WHERE email <> 'sierap@unicesar.edu.co';

DELETE FROM moment WHERE number <> 1;
DELETE FROM evaluator_weight;

-- -----------------------------------------------------------------------------
-- 1 · Parámetros globales (RF 11: reglas institucionales fuera del código)
-- -----------------------------------------------------------------------------
INSERT INTO global_parameter (param_key, param_value, description)
VALUES ('institution.name', 'Universidad Popular del Cesar',
        'Nombre de la institución que se muestra en las pantallas y los reportes.'),
       ('institution.city', 'Valledupar',
        'Ciudad de la sede principal.'),
       ('grading.max_score', '5.00',
        'Nota máxima de la escala institucional.'),
       ('grading.passing_score', '3.00',
        'Nota mínima aprobatoria.'),
       ('grading.decimals', '2',
        'Decimales con los que se muestran las notas.'),
       ('submission.max_versions', '5',
        'Versiones que un grupo puede subir por momento.')
ON CONFLICT (param_key) DO UPDATE
    SET param_value = EXCLUDED.param_value,
        description = EXCLUDED.description;

-- -----------------------------------------------------------------------------
-- 2 · Momento 1: se lanza con sus ventanas y sus pesos
-- -----------------------------------------------------------------------------
-- Ventanas en orden y sin solaparse (V5), y la calificación termina antes del
-- límite de la UPC del periodo (2026-11-28).
UPDATE moment m
SET status                = 'LAUNCHED',
    teacher_weight        = 60,
    group_formation_start = DATE '2026-08-10',
    group_formation_end   = DATE '2026-08-28',
    submission_start      = DATE '2026-09-01',
    submission_end        = DATE '2026-10-16',
    grading_start         = DATE '2026-10-19',
    grading_end           = DATE '2026-11-20'
FROM program p, academic_period ap
WHERE m.program_id = p.id AND p.code = '108'
  AND m.period_id = ap.id AND ap.code = '2026-2'
  AND m.number = 1;

-- Pesos por tipo de evaluador: 60 (docente) + 25 + 15 = 100 (ADR-018).
INSERT INTO evaluator_weight (moment_id, evaluator_type_id, weight)
SELECT m.id, et.id, v.weight
FROM (VALUES ('Empresario', 25.00), ('Egresado', 15.00)) AS v (type_name, weight)
JOIN evaluator_type et ON et.name = v.type_name
JOIN moment m ON m.number = 1
JOIN program p ON p.id = m.program_id AND p.code = '108'
JOIN academic_period ap ON ap.id = m.period_id AND ap.code = '2026-2';

-- -----------------------------------------------------------------------------
-- 3 · Momento 2: recién creado, en configuración
-- -----------------------------------------------------------------------------
-- Muestra el otro estado del ciclo de vida (ADR-012): sin ventanas, sin pesos
-- completos y con los RAP cargados pero sin criterios todavía.
INSERT INTO moment (program_id, period_id, number, name, subject,
                    semester_from, semester_to, requirement_level,
                    min_members, max_members, status, teacher_weight)
SELECT p.id, ap.id, 2, 'Momento 2', 'Estructuras de Datos (SS301)',
       4, 6, 'Medio',
       3, 5, 'CONFIGURATION', 70
FROM program p, academic_period ap
WHERE p.code = '108' AND ap.code = '2026-2';

INSERT INTO evaluator_weight (moment_id, evaluator_type_id, weight)
SELECT m2.id, et.id, 30.00
FROM moment m2
JOIN program p ON p.id = m2.program_id AND p.code = '108'
JOIN evaluator_type et ON et.name = 'Empresario'
WHERE m2.number = 2;

-- Los RAP del programa son los mismos; los criterios y niveles del Momento 2
-- los define el admin más adelante.
INSERT INTO learning_outcome (moment_id, code, description)
SELECT m2.id, lo.code, lo.description
FROM learning_outcome lo
JOIN moment m1 ON m1.id = lo.moment_id AND m1.number = 1
JOIN program p ON p.id = m1.program_id AND p.code = '108'
JOIN moment m2 ON m2.program_id = m1.program_id AND m2.period_id = m1.period_id
              AND m2.number = 2;

-- -----------------------------------------------------------------------------
-- 4 · Usuarios
-- -----------------------------------------------------------------------------
-- Todos con el hash bcrypt de "Sierap-Inicio-2026" y created_by apuntando a
-- quien los habría creado en el flujo real (super_admin → admin → docente).

-- 4.1 Admins de la Facultad de Ingenierías y Tecnologías.
INSERT INTO app_user (role, first_names, last_names, document_type, document_number,
                      email, password_hash, active, created_at,
                      must_change_password, created_by_id)
SELECT 'ADMIN', v.first_names, v.last_names, 'CC', v.document_number, v.email,
       '$2a$10$TyYxoblQNjR5HG3q4muPCOHfKL5fgtOdta7NLm8U3E7/WOkQrNYai',
       TRUE, TIMESTAMP '2026-07-20 09:00:00', FALSE, sa.id
FROM (VALUES
    ('Luz Marina',   'Castro Ospino',  '1065812340', 'luz.castro@unicesar.edu.co'),
    ('Hernán Darío', 'Molina Araújo',  '0077185401', 'hernan.molina@unicesar.edu.co')
) AS v (first_names, last_names, document_number, email)
CROSS JOIN app_user sa
WHERE sa.email = 'sierap@unicesar.edu.co';

INSERT INTO admin (id, faculty_id)
SELECT u.id, f.id
FROM app_user u
CROSS JOIN faculty f
WHERE u.email IN ('luz.castro@unicesar.edu.co', 'hernan.molina@unicesar.edu.co')
  AND f.code = '04';

-- 4.2 Docentes. Edwin habilita los grupos A, B y C; Claudia el grupo D.
INSERT INTO app_user (role, first_names, last_names, document_type, document_number,
                      email, password_hash, active, created_at,
                      must_change_password, created_by_id)
SELECT 'TEACHER', v.first_names, v.last_names, 'CC', v.document_number, v.email,
       '$2a$10$TyYxoblQNjR5HG3q4muPCOHfKL5fgtOdta7NLm8U3E7/WOkQrNYai',
       TRUE, TIMESTAMP '2026-07-22 10:30:00', FALSE, a.id
FROM (VALUES
    ('Edwin Alberto',    'Mendoza Daza', '0077185430', 'edwin.mendoza@unicesar.edu.co'),
    ('Claudia Patricia', 'Oñate Zuleta', '0049765012', 'claudia.onate@unicesar.edu.co')
) AS v (first_names, last_names, document_number, email)
CROSS JOIN app_user a
WHERE a.email = 'luz.castro@unicesar.edu.co';

INSERT INTO teacher (id, faculty_id)
SELECT u.id, f.id
FROM app_user u
CROSS JOIN faculty f
WHERE u.email IN ('edwin.mendoza@unicesar.edu.co', 'claudia.onate@unicesar.edu.co')
  AND f.code = '04';

-- 4.3 Códigos de registro: con ellos los estudiantes se inscriben solos
-- (vencen al cerrar la conformación de grupos, ADR-017).
INSERT INTO registration_code (code, teacher_id, program_id, expires_on, active, created_at)
SELECT v.code, t.id, p.id, DATE '2026-08-28', TRUE, TIMESTAMP '2026-08-05 08:00:00'
FROM (VALUES
    ('PRG2-2026-EM', 'edwin.mendoza@unicesar.edu.co'),
    ('PRG2-2026-CO', 'claudia.onate@unicesar.edu.co')
) AS v (code, teacher_email)
JOIN app_user u ON u.email = v.teacher_email
JOIN teacher t ON t.id = u.id
CROSS JOIN program p
WHERE p.code = '108';

-- 4.4 Estudiantes de Ingeniería de Sistemas, entre 1.º y 3.º semestre (el rango
-- del Momento 1).
INSERT INTO app_user (role, first_names, last_names, document_type, document_number,
                      email, password_hash, active, created_at,
                      must_change_password, created_by_id)
SELECT 'STUDENT', v.first_names, v.last_names, v.document_type, v.document_number, v.email,
       '$2a$10$TyYxoblQNjR5HG3q4muPCOHfKL5fgtOdta7NLm8U3E7/WOkQrNYai',
       TRUE, v.created_at, FALSE, u.id
FROM (VALUES
    ('Oscar Daniel',     'Palomino Fontalvo',  'CC', '1065001001', 'oscar.palomino@unicesar.edu.co',    TIMESTAMP '2026-08-07 08:12:00', 'edwin.mendoza@unicesar.edu.co'),
    ('María Fernanda',   'Ospino Daza',        'CC', '1065001002', 'maria.ospino@unicesar.edu.co',      TIMESTAMP '2026-08-07 08:25:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Juan Sebastián',   'Araújo Mejía',       'CC', '1065001003', 'juan.araujo@unicesar.edu.co',       TIMESTAMP '2026-08-07 09:03:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Laura Valentina',  'Cuello Baute',       'CC', '1065001004', 'laura.cuello@unicesar.edu.co',      TIMESTAMP '2026-08-07 09:40:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Camilo Andrés',    'Zuleta Pineda',      'CC', '1065001005', 'camilo.zuleta@unicesar.edu.co',     TIMESTAMP '2026-08-08 10:15:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Daniela Sofía',    'Maestre Gutiérrez',  'CC', '1065001006', 'daniela.maestre@unicesar.edu.co',   TIMESTAMP '2026-08-08 10:48:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Kevin Alexis',     'Lobo Núñez',         'CC', '1065001007', 'kevin.lobo@unicesar.edu.co',        TIMESTAMP '2026-08-08 11:20:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Andrea Carolina',  'Quintero Solano',    'CC', '1065001008', 'andrea.quintero@unicesar.edu.co',   TIMESTAMP '2026-08-09 14:05:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Jesús David',      'Montero Calderón',   'CC', '1065001009', 'jesus.montero@unicesar.edu.co',     TIMESTAMP '2026-08-09 14:33:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Valeria Isabel',   'Rois Angarita',      'CC', '1065001010', 'valeria.rois@unicesar.edu.co',      TIMESTAMP '2026-08-09 15:10:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Santiago José',    'Becerra Hinojosa',   'TI', '1065001011', 'santiago.becerra@unicesar.edu.co',  TIMESTAMP '2026-08-10 08:30:00', 'claudia.onate@unicesar.edu.co'),
    ('Paula Andrea',     'Villazón Mindiola',  'CC', '1065001012', 'paula.villazon@unicesar.edu.co',    TIMESTAMP '2026-08-10 09:12:00', 'claudia.onate@unicesar.edu.co'),
    ('Brayan Stiven',    'Guerra Carrillo',    'CC', '1065001013', 'brayan.guerra@unicesar.edu.co',     TIMESTAMP '2026-08-10 09:55:00', 'claudia.onate@unicesar.edu.co'),
    ('Natalia Esther',   'Fuentes Ramírez',    'CC', '1065001014', 'natalia.fuentes@unicesar.edu.co',   TIMESTAMP '2026-08-12 16:20:00', 'edwin.mendoza@unicesar.edu.co'),
    ('Miguel Ángel',     'Sarmiento Pavajeau', 'CC', '1065001015', 'miguel.sarmiento@unicesar.edu.co',  TIMESTAMP '2026-08-12 16:45:00', 'edwin.mendoza@unicesar.edu.co')
) AS v (first_names, last_names, document_type, document_number, email, created_at, creator_email)
JOIN app_user u ON u.email = v.creator_email;

INSERT INTO student (id, student_code, current_semester, program_id, registration_code_id)
SELECT u.id, v.student_code, v.current_semester, p.id, rc.id
FROM (VALUES
    ('oscar.palomino@unicesar.edu.co',    '2021115001', 3, 'PRG2-2026-EM'),
    ('maria.ospino@unicesar.edu.co',      '2021115002', 3, 'PRG2-2026-EM'),
    ('juan.araujo@unicesar.edu.co',       '2021115003', 3, 'PRG2-2026-EM'),
    ('laura.cuello@unicesar.edu.co',      '2021115004', 3, 'PRG2-2026-EM'),
    ('camilo.zuleta@unicesar.edu.co',     '2022115005', 2, 'PRG2-2026-EM'),
    ('daniela.maestre@unicesar.edu.co',   '2022115006', 2, 'PRG2-2026-EM'),
    ('kevin.lobo@unicesar.edu.co',        '2022115007', 2, 'PRG2-2026-EM'),
    ('andrea.quintero@unicesar.edu.co',   '2021115008', 3, 'PRG2-2026-EM'),
    ('jesus.montero@unicesar.edu.co',     '2021115009', 3, 'PRG2-2026-EM'),
    ('valeria.rois@unicesar.edu.co',      '2021115010', 3, 'PRG2-2026-EM'),
    ('santiago.becerra@unicesar.edu.co',  '2023115011', 1, 'PRG2-2026-CO'),
    ('paula.villazon@unicesar.edu.co',    '2023115012', 1, 'PRG2-2026-CO'),
    ('brayan.guerra@unicesar.edu.co',     '2023115013', 1, 'PRG2-2026-CO'),
    ('natalia.fuentes@unicesar.edu.co',   '2022115014', 2, 'PRG2-2026-EM'),
    ('miguel.sarmiento@unicesar.edu.co',  '2021115015', 3, 'PRG2-2026-EM')
) AS v (email, student_code, current_semester, code)
JOIN app_user u ON u.email = v.email
JOIN registration_code rc ON rc.code = v.code
CROSS JOIN program p
WHERE p.code = '108';

-- 4.5 Evaluadores externos: dos empresarios y dos egresados del programa
-- (ADR-011: el tipo sale del catálogo, no de un enum).
INSERT INTO app_user (role, first_names, last_names, document_type, document_number,
                      email, password_hash, active, created_at,
                      must_change_password, created_by_id)
SELECT 'EXTERNAL_EVALUATOR', v.first_names, v.last_names, 'CC', v.document_number, v.email,
       '$2a$10$TyYxoblQNjR5HG3q4muPCOHfKL5fgtOdta7NLm8U3E7/WOkQrNYai',
       TRUE, TIMESTAMP '2026-09-15 11:00:00', FALSE, u.id
FROM (VALUES
    ('Jorge Enrique',  'Daza Pineda',    '0012543876', 'jorge.daza@softcaribe.com.co',        'edwin.mendoza@unicesar.edu.co'),
    ('Yuranis',        'Quintero Mejía', '1065990123', 'yuranis.quintero@valleindustrial.co', 'edwin.mendoza@unicesar.edu.co'),
    ('Andrés Felipe',  'Ramírez Ovalle', '1065334455', 'andres.ramirez@outlook.com',          'edwin.mendoza@unicesar.edu.co'),
    ('Diana Carolina', 'Pérez Fuentes',  '1065667788', 'diana.perez@bancodeoccidente.co',     'claudia.onate@unicesar.edu.co')
) AS v (first_names, last_names, document_number, email, creator_email)
JOIN app_user u ON u.email = v.creator_email;

INSERT INTO external_evaluator (id, evaluator_type_id, organization, job_position,
                                graduation_year, graduation_program_id)
SELECT u.id, et.id, v.organization, v.job_position, v.graduation_year,
       CASE WHEN v.graduation_year IS NULL THEN NULL ELSE p.id END
FROM (VALUES
    ('jorge.daza@softcaribe.com.co',        'Empresario', 'SoftCaribe S.A.S.',            'Gerente de Tecnología',  NULL::INTEGER),
    ('yuranis.quintero@valleindustrial.co', 'Empresario', 'Valle Industrial del Cesar',   'Jefe de Sistemas',       NULL::INTEGER),
    ('andres.ramirez@outlook.com',          'Egresado',   'Globant Colombia',             'Desarrollador Senior',   2019),
    ('diana.perez@bancodeoccidente.co',     'Egresado',   'Banco de Occidente',           'Analista de Datos',      2021)
) AS v (email, type_name, organization, job_position, graduation_year)
JOIN app_user u ON u.email = v.email
JOIN evaluator_type et ON et.name = v.type_name
CROSS JOIN program p
WHERE p.code = '108';

-- -----------------------------------------------------------------------------
-- 5 · Grupos del Momento 1 (grupo = proyecto de aula)
-- -----------------------------------------------------------------------------
-- Las fechas de cada grupo caen dentro de las ventanas del momento: la entrega
-- a más tardar el 2026-10-16 y la evaluación entre el 10-19 y el 11-20.
INSERT INTO project_group (moment_id, teacher_id, name, project_title,
                           submission_deadline, evaluation_date)
SELECT m.id, t.id, v.name, v.project_title, v.submission_deadline, v.evaluation_date
FROM (VALUES
    ('edwin.mendoza@unicesar.edu.co',  'Grupo A',
     'SIERAP: evaluador de resultados de aprendizaje por programa',
     DATE '2026-10-16', DATE '2026-10-26'),
    ('edwin.mendoza@unicesar.edu.co',  'Grupo B',
     'GestorBiblioUPC: préstamo de libros de la biblioteca',
     DATE '2026-10-16', DATE '2026-10-28'),
    ('edwin.mendoza@unicesar.edu.co',  'Grupo C',
     'TurnoSalud: gestión de turnos en la IPS universitaria',
     DATE '2026-10-16', DATE '2026-11-06'),
    ('claudia.onate@unicesar.edu.co',  'Grupo D',
     NULL, DATE '2026-10-16', NULL)
) AS v (teacher_email, name, project_title, submission_deadline, evaluation_date)
JOIN app_user u ON u.email = v.teacher_email
JOIN teacher t ON t.id = u.id
JOIN moment m ON m.number = 1
JOIN program p ON p.id = m.program_id AND p.code = '108'
JOIN academic_period ap ON ap.id = m.period_id AND ap.code = '2026-2';

-- Integrantes: 13 de los 15 estudiantes. Natalia y Miguel quedan sin grupo a
-- propósito, para el filtro de estudiantes sin grupo del docente.
INSERT INTO project_group_member (project_group_id, student_id)
SELECT g.id, s.id
FROM (VALUES
    ('Grupo A', 'oscar.palomino@unicesar.edu.co'),
    ('Grupo A', 'maria.ospino@unicesar.edu.co'),
    ('Grupo A', 'juan.araujo@unicesar.edu.co'),
    ('Grupo A', 'laura.cuello@unicesar.edu.co'),
    ('Grupo B', 'camilo.zuleta@unicesar.edu.co'),
    ('Grupo B', 'daniela.maestre@unicesar.edu.co'),
    ('Grupo B', 'kevin.lobo@unicesar.edu.co'),
    ('Grupo C', 'andrea.quintero@unicesar.edu.co'),
    ('Grupo C', 'jesus.montero@unicesar.edu.co'),
    ('Grupo C', 'valeria.rois@unicesar.edu.co'),
    ('Grupo D', 'santiago.becerra@unicesar.edu.co'),
    ('Grupo D', 'paula.villazon@unicesar.edu.co'),
    ('Grupo D', 'brayan.guerra@unicesar.edu.co')
) AS v (group_name, student_email)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.student_email
JOIN student s ON s.id = u.id;

-- Evaluadores externos designados por el docente para cada grupo.
INSERT INTO project_group_evaluator (project_group_id, external_evaluator_id)
SELECT g.id, e.id
FROM (VALUES
    ('Grupo A', 'jorge.daza@softcaribe.com.co'),
    ('Grupo A', 'andres.ramirez@outlook.com'),
    ('Grupo B', 'yuranis.quintero@valleindustrial.co'),
    ('Grupo C', 'diana.perez@bancodeoccidente.co')
) AS v (group_name, evaluator_email)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email
JOIN external_evaluator e ON e.id = u.id;

-- -----------------------------------------------------------------------------
-- 6 · Entregas versionadas
-- -----------------------------------------------------------------------------
-- Las rutas siguen el formato que genera el backend:
-- groups/{id del grupo}/v{versión}/{uuid}.{ext} (ADR-015).
INSERT INTO submission (project_group_id, version_number,
                        zip_file_path, zip_file_name, zip_size,
                        presentation_path, presentation_file_name, presentation_size,
                        submitted_at, late)
SELECT g.id, v.version_number,
       'groups/' || g.id || '/v' || v.version_number || '/' || v.zip_uuid || '.zip',
       v.zip_file_name, v.zip_size,
       CASE WHEN v.presentation_uuid IS NULL THEN NULL
            ELSE 'groups/' || g.id || '/v' || v.version_number || '/' || v.presentation_uuid || '.pdf' END,
       v.presentation_file_name, v.presentation_size,
       v.submitted_at, v.late
FROM (VALUES
    -- Grupo A: primera versión sin presentación y segunda completa.
    ('Grupo A', 1, '3f6a1c84-5d2b-4e71-9a30-7c1b8e45d201', 'sierap-momento1-v1.zip', 8421376,
     NULL, NULL, NULL::BIGINT, TIMESTAMP '2026-10-05 12:14:00', FALSE),
    ('Grupo A', 2, 'b1d9e2f7-0a46-4c58-8b22-61f3a9d7c402', 'sierap-momento1-final.zip', 11238912,
     'd47c8a19-2e65-4f03-9d81-55b2c6e10a93', 'sierap-sustentacion.pdf', 2457600,
     TIMESTAMP '2026-10-15 22:40:00', FALSE),
    -- Grupo B: una sola versión, justo el último día de la ventana.
    ('Grupo B', 1, '7c2b45e0-91af-4d36-a5c7-0e8d31f6b215', 'gestorbiblioupc.zip', 6291456,
     'a08f5d63-77c1-41be-92e4-3b6a0c9f8d17', 'gestorbiblioupc-presentacion.pdf', 1835008,
     TIMESTAMP '2026-10-16 18:05:00', FALSE),
    -- Grupo C: entregó un día después de su fecha límite → late = TRUE.
    ('Grupo C', 1, 'e5901b27-3c48-4a6d-b7f1-82e40d5c9a36', 'turnosalud.zip', 4718592,
     NULL, NULL, NULL::BIGINT, TIMESTAMP '2026-10-17 09:30:00', TRUE)
    -- Grupo D: sin entregas.
) AS v (group_name, version_number, zip_uuid, zip_file_name, zip_size,
        presentation_uuid, presentation_file_name, presentation_size,
        submitted_at, late)
JOIN project_group g ON g.name = v.group_name;

-- -----------------------------------------------------------------------------
-- 7 · Evaluaciones
-- -----------------------------------------------------------------------------
-- Solo las SUBMITTED cuentan para la nota; la de Yuranis queda en DRAFT para
-- ver una calificación a medio camino.
INSERT INTO evaluation (project_group_id, evaluator_id, status, evaluated_at, comment)
SELECT g.id, u.id, v.status, v.evaluated_at, v.comment
FROM (VALUES
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'SUBMITTED', TIMESTAMP '2026-10-26 15:10:00',
     'Excelente trabajo. La arquitectura por capas está bien separada y la documentación del código es consistente. Mejorar las pruebas de la capa de servicios.'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'SUBMITTED', TIMESTAMP '2026-10-26 15:45:00',
     'La solución es aplicable en una empresa real. Faltó profundizar en el manejo de errores de cara al usuario.'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'SUBMITTED', TIMESTAMP '2026-10-26 16:20:00',
     'Muy buen nivel para tercer semestre. El diagrama de clases refleja fielmente el código entregado.'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'SUBMITTED', TIMESTAMP '2026-10-28 14:00:00',
     'La funcionalidad cumple lo mínimo. La capa de datos y la de lógica están mezcladas y el diagrama UML no está completo.'),
    ('Grupo B', 'yuranis.quintero@valleindustrial.co', 'DRAFT', TIMESTAMP '2026-10-28 14:30:00',
     NULL)
) AS v (group_name, evaluator_email, status, evaluated_at, comment)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email;

-- 7.1 Calificación de cada criterio. El puntaje se copia del nivel elegido.
-- Puntaje máximo del momento: 7 criterios x 0.60 + sustentación 0.80 = 5.00.
INSERT INTO criterion_grade (evaluation_id, criterion_id, performance_level_id, score)
SELECT e.id, c.id, pl.id, pl.score
FROM (VALUES
    -- Grupo A · docente Edwin → 4.60
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP1', 1, 'Sobresaliente'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP2', 1, 'Sobresaliente'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP2', 2, 'Alto'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP3', 1, 'Sobresaliente'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP4', 1, 'Alto'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP4', 2, 'Sobresaliente'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP5', 1, 'Sobresaliente'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'RAP5', 2, 'Alto'),
    -- Grupo A · empresario Jorge → 4.12
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP1', 1, 'Alto'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP2', 1, 'Alto'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP2', 2, 'Alto'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP3', 1, 'Alto'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP4', 1, 'Sobresaliente'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP4', 2, 'Alto'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP5', 1, 'Alto'),
    ('Grupo A', 'jorge.daza@softcaribe.com.co', 'RAP5', 2, 'Alto'),
    -- Grupo A · egresado Andrés → 4.76
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP1', 1, 'Sobresaliente'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP2', 1, 'Sobresaliente'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP2', 2, 'Sobresaliente'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP3', 1, 'Alto'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP4', 1, 'Sobresaliente'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP4', 2, 'Sobresaliente'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP5', 1, 'Alto'),
    ('Grupo A', 'andres.ramirez@outlook.com', 'RAP5', 2, 'Sobresaliente'),
    -- Grupo B · docente Edwin → 3.48
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP1', 1, 'Alto'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP2', 1, 'Básico'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP2', 2, 'Básico'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP3', 1, 'Alto'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP4', 1, 'Básico'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP4', 2, 'Alto'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP5', 1, 'Alto'),
    ('Grupo B', 'edwin.mendoza@unicesar.edu.co', 'RAP5', 2, 'Básico'),
    -- Grupo B · empresaria Yuranis: borrador, solo lleva 4 de los 8 criterios
    ('Grupo B', 'yuranis.quintero@valleindustrial.co', 'RAP1', 1, 'Alto'),
    ('Grupo B', 'yuranis.quintero@valleindustrial.co', 'RAP2', 1, 'Básico'),
    ('Grupo B', 'yuranis.quintero@valleindustrial.co', 'RAP2', 2, 'Alto'),
    ('Grupo B', 'yuranis.quintero@valleindustrial.co', 'RAP3', 1, 'Básico')
) AS v (group_name, evaluator_email, outcome_code, criterion_order, level_name)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email
JOIN evaluation e ON e.project_group_id = g.id AND e.evaluator_id = u.id
JOIN moment m ON m.id = g.moment_id
JOIN learning_outcome lo ON lo.moment_id = m.id AND lo.code = v.outcome_code
JOIN evaluation_criterion c ON c.learning_outcome_id = lo.id
                          AND c.display_order = v.criterion_order
JOIN performance_level pl ON pl.criterion_id = c.id AND pl.name = v.level_name;

-- 7.2 Ajustes individuales (ADR-018): el evaluador cambia el nivel de un
-- criterio para un estudiante concreto. Aquí, la sustentación (RAP5, criterio 2)
-- del Grupo A: el grupo quedó en "Alto" (0.64) y el docente sube a María
-- Fernanda y baja a Juan Sebastián. Oscar y Laura conservan la nota del grupo.
INSERT INTO individual_grade (evaluation_id, student_id, criterion_id, performance_level_id, score)
SELECT e.id, s.id, c.id, pl.id, pl.score
FROM (VALUES
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'maria.ospino@unicesar.edu.co', 'RAP5', 2, 'Sobresaliente'),
    ('Grupo A', 'edwin.mendoza@unicesar.edu.co', 'juan.araujo@unicesar.edu.co',  'RAP5', 2, 'Básico'),
    ('Grupo A', 'andres.ramirez@outlook.com',    'juan.araujo@unicesar.edu.co',  'RAP5', 2, 'Alto')
) AS v (group_name, evaluator_email, student_email, outcome_code, criterion_order, level_name)
JOIN project_group g ON g.name = v.group_name
JOIN app_user eu ON eu.email = v.evaluator_email
JOIN evaluation e ON e.project_group_id = g.id AND e.evaluator_id = eu.id
JOIN app_user su ON su.email = v.student_email
JOIN student s ON s.id = su.id
JOIN moment m ON m.id = g.moment_id
JOIN learning_outcome lo ON lo.moment_id = m.id AND lo.code = v.outcome_code
JOIN evaluation_criterion c ON c.learning_outcome_id = lo.id
                          AND c.display_order = v.criterion_order
JOIN performance_level pl ON pl.criterion_id = c.id AND pl.name = v.level_name;

COMMIT;

-- -----------------------------------------------------------------------------
-- Verificación: lo que debería haber quedado
-- -----------------------------------------------------------------------------
SELECT 'faculty'                AS tabla, COUNT(*) AS filas, 1  AS esperadas FROM faculty
UNION ALL SELECT 'program',                COUNT(*), 1  FROM program
UNION ALL SELECT 'academic_period',        COUNT(*), 1  FROM academic_period
UNION ALL SELECT 'global_parameter',       COUNT(*), 6  FROM global_parameter
UNION ALL SELECT 'moment',                 COUNT(*), 2  FROM moment
UNION ALL SELECT 'evaluator_weight',       COUNT(*), 3  FROM evaluator_weight
UNION ALL SELECT 'learning_outcome',       COUNT(*), 10 FROM learning_outcome
UNION ALL SELECT 'evaluation_criterion',   COUNT(*), 8  FROM evaluation_criterion
UNION ALL SELECT 'performance_level',      COUNT(*), 32 FROM performance_level
UNION ALL SELECT 'evaluator_type',         COUNT(*), 2  FROM evaluator_type
UNION ALL SELECT 'app_user',               COUNT(*), 24 FROM app_user
UNION ALL SELECT 'super_admin',            COUNT(*), 1  FROM super_admin
UNION ALL SELECT 'admin',                  COUNT(*), 2  FROM admin
UNION ALL SELECT 'teacher',                COUNT(*), 2  FROM teacher
UNION ALL SELECT 'student',                COUNT(*), 15 FROM student
UNION ALL SELECT 'external_evaluator',     COUNT(*), 4  FROM external_evaluator
UNION ALL SELECT 'registration_code',      COUNT(*), 2  FROM registration_code
UNION ALL SELECT 'project_group',          COUNT(*), 4  FROM project_group
UNION ALL SELECT 'project_group_member',   COUNT(*), 13 FROM project_group_member
UNION ALL SELECT 'project_group_evaluator',COUNT(*), 4  FROM project_group_evaluator
UNION ALL SELECT 'submission',             COUNT(*), 4  FROM submission
UNION ALL SELECT 'evaluation',             COUNT(*), 5  FROM evaluation
UNION ALL SELECT 'criterion_grade',        COUNT(*), 36 FROM criterion_grade
UNION ALL SELECT 'individual_grade',       COUNT(*), 3  FROM individual_grade
ORDER BY tabla;
