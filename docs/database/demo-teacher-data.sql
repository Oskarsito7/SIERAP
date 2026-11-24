-- =============================================================================
-- SIERAP · Datos de demostración del panel docente
--
-- Crea una docente de demostración con todo lo necesario para recorrer su panel:
-- grupos en distintos estados, estudiantes, evaluadores externos, códigos de
-- registro, entregas con archivos descargables y calificaciones (enviadas, en
-- borrador y pendientes).
--
-- Requisitos: una base creada por Flyway y con docs/database/test-data.sql ya
-- cargado (usa el Momento 1 de Ingeniería de Sistemas, lanzado y con la calificación
-- abierta del 19 oct al 27 nov de 2026, y el Momento 2 en configuración).
--
-- NO es una migración: nunca debe llegar a producción. Se ejecuta a mano:
--   * psql:   psql -U sierap -h localhost -d sierap -f docs/database/demo-teacher-data.sql
--   * Docker: docker compose exec -T postgres psql -U sierap -d sierap < docs/database/demo-teacher-data.sql
--   * pgAdmin: base sierap → Query Tool → abrir este archivo → F5.
--
-- Se puede ejecutar varias veces: primero borra la demo anterior (grupos, entregas,
-- calificaciones, códigos y cuentas de demostración, incluidas las que se hayan creado
-- desde la aplicación durante un ensayo) y la vuelve a crear igual. Las cuentas con
-- registros en la bitácora de auditoría no se pueden borrar (audit_log es inmutable):
-- esas se conservan y se dejan como al principio.
--
-- Archivos de las entregas: copiar docs/database/demo-files/ a <carpeta personal>/
-- sierap-files/demo/ (ruta por defecto de sierap.storage.base-path en el perfil dev)
-- para que las descargas funcionen. Sin copiarlos, todo lo demás funciona igual.
--
-- Cuentas (contraseña de todas: Sierap-Inicio-2026, la misma de test-data.sql):
--   * Docente:     docente.demo@unicesar.edu.co
--   * Estudiantes: *.demo@unicesar.edu.co   (p. ej. andrea.lopez.demo@unicesar.edu.co)
--   * Evaluadores: *.demo@example.com       (p. ej. hernan.pumarejo.demo@example.com)
--
-- Qué se ve en el panel (fecha de referencia: 24 de noviembre de 2026):
--   * Equipo Sirius: 4 integrantes, 2 versiones, docente y 2 externos con la
--     evaluación enviada → pestaña Resultados completa, con ajustes individuales.
--   * Equipo Vega: 3 integrantes, entrega tardía, evaluación de la docente en
--     borrador (4 de 8 criterios) → aparece en "Por calificar" para terminarla.
--   * Equipo Altair: 5 integrantes (el máximo), externo en borrador, sin evaluación
--     de la docente → "Por calificar" para calificar desde cero.
--   * Equipo Rigel: 3 integrantes, sin entregas, sin fecha de evaluación y sin
--     evaluadores → "Requieren fechas"; sirve para fijar la fecha y asignar a
--     wilmer.daza.demo@example.com buscándolo por correo.
--   * Equipo Polaris (Momento 2, en configuración): sin integrantes → se puede
--     eliminar, y en ese momento se ve el formulario "Habilitar grupo".
--   * 3 estudiantes de la docente sin grupo (más Óscar Salas de test-data.sql) para
--     "Agregar estudiante sin grupo"; 1 estudiante (Ana Lucía Cujia, también sin grupo)
--     y 1 evaluador con la cuenta pendiente de activar para "Reenviar enlace".
--   * Códigos: SIS-DEMO-2026 (activo, usado), ELE-DEMO-2026 (activo, sin usar),
--     SIS-DEMO-AGO (vencido) y SIS-DEMO-OFF (desactivado).
--
-- Después del 27 de noviembre de 2026 la calificación del Momento 1 cierra y los
-- grupos quedan bloqueados (solo lectura), como en producción.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. Comprobación: test-data.sql debe estar cargado
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM moment m
                   JOIN program p ON p.id = m.program_id AND p.code = '108'
                   JOIN academic_period ap ON ap.id = m.period_id AND ap.code = '2026-2'
                   WHERE m.number = 1 AND m.status = 'LAUNCHED')
       OR NOT EXISTS (SELECT 1
                      FROM moment m
                      JOIN program p ON p.id = m.program_id AND p.code = '108'
                      JOIN academic_period ap ON ap.id = m.period_id AND ap.code = '2026-2'
                      WHERE m.number = 2) THEN
        RAISE EXCEPTION 'Primero cargue docs/database/test-data.sql: falta el Momento 1 lanzado o el Momento 2 de Ingeniería de Sistemas (2026-2).';
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1. Limpieza de la demo anterior
-- -----------------------------------------------------------------------------

-- Cuentas de la demo: la docente, todo lo que ella creó (también desde la aplicación),
-- los estudiantes que se registraron con sus códigos y las cuentas fijas de este script.
CREATE TEMP TABLE demo_old_user ON COMMIT DROP AS
WITH teacher AS (SELECT id FROM app_user WHERE email = 'docente.demo@unicesar.edu.co')
SELECT u.id
FROM app_user u
WHERE u.email = 'docente.demo@unicesar.edu.co'
   OR u.email LIKE '%.demo@unicesar.edu.co'
   OR u.email LIKE '%.demo@example.com'
   OR u.created_by_id IN (SELECT id FROM teacher)
   OR u.id IN (SELECT s.id
               FROM student s
               JOIN registration_code rc ON rc.id = s.registration_code_id
               WHERE rc.teacher_id IN (SELECT id FROM teacher));

-- Los grupos de la docente arrastran integrantes, evaluadores, entregas y calificaciones.
DELETE FROM project_group
WHERE teacher_id IN (SELECT id FROM app_user WHERE email = 'docente.demo@unicesar.edu.co');

-- Rastros de las cuentas de demo en grupos de otros docentes (si se usaron en un ensayo).
DELETE FROM individual_grade WHERE student_id IN (SELECT id FROM demo_old_user);
DELETE FROM project_group_member WHERE student_id IN (SELECT id FROM demo_old_user);
DELETE FROM project_group_evaluator WHERE external_evaluator_id IN (SELECT id FROM demo_old_user);
DELETE FROM evaluation WHERE evaluator_id IN (SELECT id FROM demo_old_user);

-- Códigos de la docente: primero se sueltan de los estudiantes que los usaron.
UPDATE student SET registration_code_id = NULL
WHERE registration_code_id IN (SELECT rc.id
                               FROM registration_code rc
                               JOIN app_user u ON u.id = rc.teacher_id
                               WHERE u.email = 'docente.demo@unicesar.edu.co');
DELETE FROM registration_code
WHERE teacher_id IN (SELECT id FROM app_user WHERE email = 'docente.demo@unicesar.edu.co');

-- Se borran las cuentas sin registros de auditoría (las tablas de cada rol, sesiones,
-- enlaces y recordatorios caen en cascada); las demás se reutilizan más abajo.
DELETE FROM app_user u
WHERE u.id IN (SELECT id FROM demo_old_user)
  AND NOT EXISTS (SELECT 1 FROM audit_log a WHERE a.actor_id = u.id);

-- -----------------------------------------------------------------------------
-- 2. Cuentas
-- -----------------------------------------------------------------------------

-- pending = cuenta pendiente de activar (sin contraseña ni consentimiento de datos).
CREATE TEMP TABLE demo_user ON COMMIT DROP AS
SELECT *
FROM (VALUES
    ('TEACHER',            'Patricia Elena',   'Martínez Oñate',    'CC', '1099100000', 'docente.demo@unicesar.edu.co',          FALSE, TIMESTAMP '2026-07-22 09:00'),
    -- Equipo Sirius
    ('STUDENT',            'Andrea Carolina',  'López Fragozo',     'CC', '1099100001', 'andrea.lopez.demo@unicesar.edu.co',     FALSE, TIMESTAMP '2026-08-04 10:00'),
    ('STUDENT',            'Julián Andrés',    'Maestre Araújo',    'CC', '1099100002', 'julian.maestre.demo@unicesar.edu.co',   FALSE, TIMESTAMP '2026-08-04 10:05'),
    ('STUDENT',            'Sofía Alejandra',  'Daza Castro',       'TI', '1099100003', 'sofia.daza.demo@unicesar.edu.co',       FALSE, TIMESTAMP '2026-08-04 10:10'),
    ('STUDENT',            'Mateo',            'Ospino Gutiérrez',  'CC', '1099100004', 'mateo.ospino.demo@unicesar.edu.co',     FALSE, TIMESTAMP '2026-08-04 10:15'),
    -- Equipo Vega
    ('STUDENT',            'Gabriela',         'Pinto Mendoza',     'CC', '1099100005', 'gabriela.pinto.demo@unicesar.edu.co',   FALSE, TIMESTAMP '2026-08-04 11:00'),
    ('STUDENT',            'Samuel David',     'Arias Quintero',    'CC', '1099100006', 'samuel.arias.demo@unicesar.edu.co',     FALSE, TIMESTAMP '2026-08-04 11:05'),
    ('STUDENT',            'Valeria',          'Hinojosa Romero',   'TI', '1099100007', 'valeria.hinojosa.demo@unicesar.edu.co', FALSE, TIMESTAMP '2026-08-04 11:10'),
    -- Equipo Altair
    ('STUDENT',            'Nicolás',          'Baquero Vega',      'CC', '1099100008', 'nicolas.baquero.demo@unicesar.edu.co',  FALSE, TIMESTAMP '2026-08-05 09:00'),
    ('STUDENT',            'Mariana Isabel',   'Celedón Brito',     'CC', '1099100009', 'mariana.celedon.demo@unicesar.edu.co',  FALSE, TIMESTAMP '2026-08-05 09:05'),
    ('STUDENT',            'Tomás Eduardo',    'Villazón Pérez',    'CC', '1099100010', 'tomas.villazon.demo@unicesar.edu.co',   FALSE, TIMESTAMP '2026-08-05 09:10'),
    ('STUDENT',            'Luciana',          'Mojica Ustariz',    'CC', '1099100011', 'luciana.mojica.demo@unicesar.edu.co',   FALSE, TIMESTAMP '2026-08-05 09:15'),
    ('STUDENT',            'Emiliano',         'Rangel Socarrás',   'TI', '1099100012', 'emiliano.rangel.demo@unicesar.edu.co',  FALSE, TIMESTAMP '2026-08-05 09:20'),
    -- Equipo Rigel
    ('STUDENT',            'Daniela Sofía',    'Ovalle Lacouture',  'CC', '1099100013', 'daniela.ovalle.demo@unicesar.edu.co',   FALSE, TIMESTAMP '2026-08-06 14:00'),
    ('STUDENT',            'Alejandro',        'Fuentes Cotes',     'CC', '1099100014', 'alejandro.fuentes.demo@unicesar.edu.co', FALSE, TIMESTAMP '2026-08-06 14:05'),
    ('STUDENT',            'Isabel Cristina',  'Murgas Amaya',      'CC', '1099100015', 'isabel.murgas.demo@unicesar.edu.co',    FALSE, TIMESTAMP '2026-08-06 14:10'),
    -- Sin grupo en el Momento 1 (para agregarlos a mano)
    ('STUDENT',            'Felipe',           'Calderón Arzuaga',  'CC', '1099100016', 'felipe.calderon.demo@unicesar.edu.co',  FALSE, TIMESTAMP '2026-09-02 08:00'),
    ('STUDENT',            'Antonella',        'Guerra Lúquez',     'TI', '1099100017', 'antonella.guerra.demo@unicesar.edu.co', FALSE, TIMESTAMP '2026-09-02 08:05'),
    -- 5.º semestre (fuera del rango del Momento 1) y uno pendiente de activar
    ('STUDENT',            'Ricardo José',     'Bolaño Pimienta',   'CC', '1099100018', 'ricardo.bolano.demo@unicesar.edu.co',   FALSE, TIMESTAMP '2026-08-10 10:00'),
    ('STUDENT',            'Ana Lucía',        'Cujia Gnecco',      'CC', '1099100019', 'ana.cujia.demo@unicesar.edu.co',        TRUE,  TIMESTAMP '2026-11-20 16:00'),
    -- Evaluadores externos (correos de ejemplo: no son personas reales)
    ('EXTERNAL_EVALUATOR', 'Hernán Darío',     'Pumarejo Castro',   'CC', '7799100001', 'hernan.pumarejo.demo@example.com',      FALSE, TIMESTAMP '2026-09-20 09:00'),
    ('EXTERNAL_EVALUATOR', 'Karen Lorena',     'Bolaño Ríos',       'CC', '7799100002', 'karen.bolano.demo@example.com',         FALSE, TIMESTAMP '2026-09-20 09:10'),
    ('EXTERNAL_EVALUATOR', 'Wilmer Andrés',    'Daza Mendoza',      'CC', '7799100003', 'wilmer.daza.demo@example.com',          FALSE, TIMESTAMP '2026-09-21 10:00'),
    ('EXTERNAL_EVALUATOR', 'Rosa Elvira',      'Cuello Arias',      'CC', '7799100004', 'rosa.cuello.demo@example.com',          TRUE,  TIMESTAMP '2026-11-21 11:00')
) AS v (role, first_names, last_names, document_type, document_number, email, pending, created_at);

-- Contraseña Sierap-Inicio-2026 (mismo hash BCrypt que test-data.sql).
CREATE TEMP TABLE demo_hash ON COMMIT DROP AS
SELECT '$2a$10$TyYxoblQNjR5HG3q4muPCOHfKL5fgtOdta7NLm8U3E7/WOkQrNYai'::VARCHAR AS hash;

INSERT INTO app_user (role, first_names, last_names, document_type, document_number, email,
                      password_hash, active, created_at, must_change_password, privacy_accepted_at)
SELECT d.role, d.first_names, d.last_names, d.document_type, d.document_number, d.email,
       CASE WHEN d.pending THEN NULL ELSE h.hash END, TRUE, d.created_at, FALSE,
       CASE WHEN d.pending THEN NULL ELSE d.created_at + INTERVAL '1 hour' END
FROM demo_user d
CROSS JOIN demo_hash h
WHERE NOT EXISTS (SELECT 1 FROM app_user u WHERE u.email = d.email);

-- Las cuentas que sobrevivieron a la limpieza vuelven a su estado inicial.
UPDATE app_user u
SET first_names          = d.first_names,
    last_names           = d.last_names,
    document_type        = d.document_type,
    document_number      = d.document_number,
    password_hash        = CASE WHEN d.pending THEN NULL ELSE h.hash END,
    active               = TRUE,
    must_change_password = FALSE,
    privacy_accepted_at  = CASE WHEN d.pending THEN NULL ELSE d.created_at + INTERVAL '1 hour' END
FROM demo_user d
CROSS JOIN demo_hash h
WHERE u.email = d.email;

INSERT INTO teacher (id, faculty_id)
SELECT u.id, f.id
FROM app_user u
JOIN faculty f ON f.code = '04'
WHERE u.email = 'docente.demo@unicesar.edu.co'
  AND NOT EXISTS (SELECT 1 FROM teacher t WHERE t.id = u.id);

-- La docente la creó el admin de la facultad 04; todo lo demás, la docente.
UPDATE app_user u
SET created_by_id = (SELECT id FROM app_user WHERE email = 'rafael.maestre@unicesar.edu.co')
WHERE u.email = 'docente.demo@unicesar.edu.co';

UPDATE app_user u
SET created_by_id = (SELECT id FROM app_user WHERE email = 'docente.demo@unicesar.edu.co')
FROM demo_user d
WHERE u.email = d.email AND d.role <> 'TEACHER';

-- Códigos de registro de la docente.
INSERT INTO registration_code (code, teacher_id, program_id, expires_on, active, created_at)
SELECT v.code, t.id, p.id, v.expires_on, v.active, v.created_at
FROM (VALUES
    ('SIS-DEMO-AGO',  '108', DATE '2026-08-28', TRUE,  TIMESTAMP '2026-07-25 08:00'),
    ('SIS-DEMO-2026', '108', DATE '2026-12-15', TRUE,  TIMESTAMP '2026-08-01 08:00'),
    ('ELE-DEMO-2026', '109', DATE '2026-12-15', TRUE,  TIMESTAMP '2026-09-01 08:00'),
    ('SIS-DEMO-OFF',  '108', DATE '2026-12-31', FALSE, TIMESTAMP '2026-09-10 08:00')
) AS v (code, program_code, expires_on, active, created_at)
JOIN app_user t ON t.email = 'docente.demo@unicesar.edu.co'
JOIN program p ON p.code = v.program_code;

-- Estudiantes de Ingeniería de Sistemas: unos los creó la docente (sin código) y
-- otros se registraron con sus códigos.
CREATE TEMP TABLE demo_student ON COMMIT DROP AS
SELECT *
FROM (VALUES
    ('andrea.lopez.demo@unicesar.edu.co',      '2026108501', 1, 'SIS-DEMO-AGO'),
    ('julian.maestre.demo@unicesar.edu.co',    '2025208502', 2, 'SIS-DEMO-AGO'),
    ('sofia.daza.demo@unicesar.edu.co',        '2026108503', 1, 'SIS-DEMO-AGO'),
    ('mateo.ospino.demo@unicesar.edu.co',      '2025108504', 3, NULL),
    ('gabriela.pinto.demo@unicesar.edu.co',    '2025208505', 2, 'SIS-DEMO-2026'),
    ('samuel.arias.demo@unicesar.edu.co',      '2025108506', 3, 'SIS-DEMO-2026'),
    ('valeria.hinojosa.demo@unicesar.edu.co',  '2026108507', 1, NULL),
    ('nicolas.baquero.demo@unicesar.edu.co',   '2025208508', 2, 'SIS-DEMO-2026'),
    ('mariana.celedon.demo@unicesar.edu.co',   '2025208509', 2, 'SIS-DEMO-2026'),
    ('tomas.villazon.demo@unicesar.edu.co',    '2025108510', 3, 'SIS-DEMO-2026'),
    ('luciana.mojica.demo@unicesar.edu.co',    '2026108511', 1, NULL),
    ('emiliano.rangel.demo@unicesar.edu.co',   '2026108512', 1, NULL),
    ('daniela.ovalle.demo@unicesar.edu.co',    '2025208513', 2, 'SIS-DEMO-2026'),
    ('alejandro.fuentes.demo@unicesar.edu.co', '2025108514', 3, 'SIS-DEMO-2026'),
    ('isabel.murgas.demo@unicesar.edu.co',     '2025208515', 2, NULL),
    ('felipe.calderon.demo@unicesar.edu.co',   '2026108516', 1, 'SIS-DEMO-2026'),
    ('antonella.guerra.demo@unicesar.edu.co',  '2025208517', 2, NULL),
    ('ricardo.bolano.demo@unicesar.edu.co',    '2024208518', 5, NULL),
    ('ana.cujia.demo@unicesar.edu.co',         '2026108519', 1, NULL)
) AS v (email, student_code, semester, registration_code);

INSERT INTO student (id, student_code, current_semester, program_id, registration_code_id)
SELECT u.id, ds.student_code, ds.semester, p.id, rc.id
FROM demo_student ds
JOIN app_user u ON u.email = ds.email
JOIN program p ON p.code = '108'
LEFT JOIN registration_code rc ON rc.code = ds.registration_code
WHERE NOT EXISTS (SELECT 1 FROM student s WHERE s.id = u.id);

UPDATE student s
SET student_code         = ds.student_code,
    current_semester     = ds.semester,
    program_id           = (SELECT id FROM program WHERE code = '108'),
    registration_code_id = (SELECT id FROM registration_code WHERE code = ds.registration_code)
FROM demo_student ds
JOIN app_user u ON u.email = ds.email
WHERE s.id = u.id;

-- Evaluadores externos que creó la docente (solo de tipos con peso en el Momento 1).
CREATE TEMP TABLE demo_evaluator ON COMMIT DROP AS
SELECT *
FROM (VALUES
    ('hernan.pumarejo.demo@example.com', 'Empresario', 'Agrotech del Cesar S.A.S.',  'Gerente general',          NULL::INTEGER, NULL),
    ('karen.bolano.demo@example.com',    'Egresado',   'Fintech Caribe',             'Desarrolladora senior',    2018,          '108'),
    ('wilmer.daza.demo@example.com',     'Empresario', 'Logística Valledupar Ltda.', 'Jefe de sistemas',         NULL,          NULL),
    ('rosa.cuello.demo@example.com',     'Egresado',   'Gobernación del Cesar',      'Profesional de proyectos', 2016,          '108')
) AS v (email, type_name, organization, job_position, graduation_year, graduation_program_code);

INSERT INTO external_evaluator (id, evaluator_type_id, organization, job_position,
                                graduation_year, graduation_program_id)
SELECT u.id, et.id, de.organization, de.job_position, de.graduation_year, p.id
FROM demo_evaluator de
JOIN app_user u ON u.email = de.email
JOIN evaluator_type et ON et.name = de.type_name
LEFT JOIN program p ON p.code = de.graduation_program_code
WHERE NOT EXISTS (SELECT 1 FROM external_evaluator e WHERE e.id = u.id);

-- -----------------------------------------------------------------------------
-- 3. Grupos de la docente
-- -----------------------------------------------------------------------------

INSERT INTO project_group (moment_id, teacher_id, name, project_title,
                           submission_deadline, evaluation_date)
SELECT m.id, t.id, v.name, v.project_title, v.submission_deadline, v.evaluation_date
FROM (VALUES
    (1, 'Equipo Sirius',  'App de turnos para el comedor universitario',   DATE '2026-10-16', DATE '2026-11-10'),
    (1, 'Equipo Vega',    'Sistema de citas para el consultorio jurídico', DATE '2026-10-16', DATE '2026-11-18'),
    (1, 'Equipo Altair',  'Control de asistencia con código QR',           DATE '2026-10-16', DATE '2026-11-23'),
    (1, 'Equipo Rigel',   'Bolsa de empleo para egresados',                DATE '2026-10-16', NULL),
    (2, 'Equipo Polaris', NULL,                                            NULL,              NULL)
) AS v (moment_number, name, project_title, submission_deadline, evaluation_date)
JOIN app_user t ON t.email = 'docente.demo@unicesar.edu.co'
JOIN program p ON p.code = '108'
JOIN academic_period ap ON ap.code = '2026-2'
JOIN moment m ON m.program_id = p.id AND m.period_id = ap.id AND m.number = v.moment_number;

-- Grupo de la docente por nombre (los nombres solo se repiten entre docentes distintos).
CREATE TEMP VIEW demo_group AS
SELECT g.*
FROM project_group g
JOIN app_user t ON t.id = g.teacher_id AND t.email = 'docente.demo@unicesar.edu.co';

-- Integrantes: entre 3 y 5 por grupo (mínimo y máximo del Momento 1).
INSERT INTO project_group_member (project_group_id, student_id)
SELECT g.id, u.id
FROM (VALUES
    ('Equipo Sirius', 'andrea.lopez.demo@unicesar.edu.co'),
    ('Equipo Sirius', 'julian.maestre.demo@unicesar.edu.co'),
    ('Equipo Sirius', 'sofia.daza.demo@unicesar.edu.co'),
    ('Equipo Sirius', 'mateo.ospino.demo@unicesar.edu.co'),
    ('Equipo Vega',   'gabriela.pinto.demo@unicesar.edu.co'),
    ('Equipo Vega',   'samuel.arias.demo@unicesar.edu.co'),
    ('Equipo Vega',   'valeria.hinojosa.demo@unicesar.edu.co'),
    ('Equipo Altair', 'nicolas.baquero.demo@unicesar.edu.co'),
    ('Equipo Altair', 'mariana.celedon.demo@unicesar.edu.co'),
    ('Equipo Altair', 'tomas.villazon.demo@unicesar.edu.co'),
    ('Equipo Altair', 'luciana.mojica.demo@unicesar.edu.co'),
    ('Equipo Altair', 'emiliano.rangel.demo@unicesar.edu.co'),
    ('Equipo Rigel',  'daniela.ovalle.demo@unicesar.edu.co'),
    ('Equipo Rigel',  'alejandro.fuentes.demo@unicesar.edu.co'),
    ('Equipo Rigel',  'isabel.murgas.demo@unicesar.edu.co')
) AS v (group_name, student_email)
JOIN demo_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.student_email;

-- Evaluadores designados. Rigel queda sin evaluadores para asignarlos en la demo.
INSERT INTO project_group_evaluator (project_group_id, external_evaluator_id)
SELECT g.id, u.id
FROM (VALUES
    ('Equipo Sirius', 'hernan.pumarejo.demo@example.com'),
    ('Equipo Sirius', 'karen.bolano.demo@example.com'),
    ('Equipo Vega',   'hernan.pumarejo.demo@example.com'),
    ('Equipo Altair', 'karen.bolano.demo@example.com')
) AS v (group_name, evaluator_email)
JOIN demo_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email;

-- Entregas. Los archivos están en docs/database/demo-files/ (ver encabezado).
-- Vega entregó tarde: después del 16 de octubre, su fecha límite.
INSERT INTO submission (project_group_id, version_number, zip_file_path, presentation_path,
                        submitted_at, late, zip_file_name, zip_size,
                        presentation_file_name, presentation_size)
SELECT g.id, v.version_number, v.zip_path, v.presentation_path, v.submitted_at, v.late,
       v.zip_file_name, v.zip_size, v.presentation_file_name, v.presentation_size
FROM (VALUES
    ('Equipo Sirius', 1, 'demo/sirius-v1.zip',  'demo/sirius-v1-avance.pdf',       TIMESTAMP '2026-09-22 20:10', FALSE, 'turnos-comedor-v1.zip',    553, 'avance-1.pdf',     720),
    ('Equipo Sirius', 2, 'demo/sirius-v2.zip',  'demo/sirius-v2-presentacion.pdf', TIMESTAMP '2026-10-15 22:45', FALSE, 'turnos-comedor-final.zip', 553, 'presentacion.pdf', 730),
    ('Equipo Vega',   1, 'demo/vega-v1.zip',    NULL,                              TIMESTAMP '2026-10-17 08:30', TRUE,  'citas-consultorio.zip',    552, NULL,               NULL),
    ('Equipo Altair', 1, 'demo/altair-v1.zip',  'demo/altair-v1-sustentacion.pdf', TIMESTAMP '2026-10-14 19:05', FALSE, 'asistencia-qr.zip',        543, 'sustentacion.pdf', 716)
) AS v (group_name, version_number, zip_path, presentation_path, submitted_at, late,
        zip_file_name, zip_size, presentation_file_name, presentation_size)
JOIN demo_group g ON g.name = v.group_name;

-- -----------------------------------------------------------------------------
-- 4. Calificaciones
-- -----------------------------------------------------------------------------

-- Solo las SUBMITTED cuentan para la nota; las DRAFT son borradores a medias.
INSERT INTO evaluation (project_group_id, evaluator_id, status, evaluated_at, comment)
SELECT g.id, u.id, v.status, v.evaluated_at, v.comment
FROM (VALUES
    ('Equipo Sirius', 'docente.demo@unicesar.edu.co',     'SUBMITTED', TIMESTAMP '2026-11-10 10:30', 'Muy buena arquitectura por capas; documentar mejor los casos de uso.'),
    ('Equipo Sirius', 'hernan.pumarejo.demo@example.com', 'SUBMITTED', TIMESTAMP '2026-11-10 11:00', 'La solución es viable para un comedor real; falta un plan de despliegue.'),
    ('Equipo Sirius', 'karen.bolano.demo@example.com',    'SUBMITTED', TIMESTAMP '2026-11-10 11:20', 'Buen uso de POO y buenas prácticas de código.'),
    ('Equipo Vega',   'docente.demo@unicesar.edu.co',     'DRAFT',     TIMESTAMP '2026-11-18 16:40', 'Revisar la entrega tardía antes de terminar.'),
    ('Equipo Vega',   'hernan.pumarejo.demo@example.com', 'SUBMITTED', TIMESTAMP '2026-11-18 17:15', 'Interfaz clara; el modelo de datos necesita ajustes.'),
    ('Equipo Altair', 'karen.bolano.demo@example.com',    'DRAFT',     TIMESTAMP '2026-11-23 15:00', NULL)
) AS v (group_name, evaluator_email, status, evaluated_at, comment)
JOIN demo_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email;

-- Nivel por criterio: cada dígito es el display_order del nivel (1 Bajo, 2 Básico,
-- 3 Alto, 4 Sobresaliente) para los 8 criterios en orden: RAP1, RAP2 (2), RAP3,
-- RAP4 (2), RAP5 (2). Los borradores tienen menos dígitos. El puntaje se copia del nivel.
INSERT INTO criterion_grade (evaluation_id, criterion_id, performance_level_id, score)
SELECT e.id, c.id, pl.id, pl.score
FROM (VALUES
    ('Equipo Sirius', 'docente.demo@unicesar.edu.co',     '43443434'),
    ('Equipo Sirius', 'hernan.pumarejo.demo@example.com', '33443334'),
    ('Equipo Sirius', 'karen.bolano.demo@example.com',    '44433444'),
    ('Equipo Vega',   'docente.demo@unicesar.edu.co',     '3232'),
    ('Equipo Vega',   'hernan.pumarejo.demo@example.com', '32323332'),
    ('Equipo Altair', 'karen.bolano.demo@example.com',    '443')
) AS v (group_name, evaluator_email, levels)
JOIN demo_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email
JOIN evaluation e ON e.project_group_id = g.id AND e.evaluator_id = u.id
JOIN (SELECT ec.id, lo.moment_id,
             ROW_NUMBER() OVER (PARTITION BY lo.moment_id ORDER BY lo.code, ec.display_order) AS position
      FROM evaluation_criterion ec
      JOIN learning_outcome lo ON lo.id = ec.learning_outcome_id) c
    ON c.moment_id = g.moment_id AND c.position <= LENGTH(v.levels)
JOIN performance_level pl
    ON pl.criterion_id = c.id
   AND pl.display_order = SUBSTRING(v.levels FROM c.position::INTEGER FOR 1)::INTEGER;

-- Ajustes individuales (ADR-018): la docente baja la sustentación de Mateo y sube a
-- Sofía en la arquitectura por capas; el empresario sube a Julián en la gestión del
-- proyecto.
INSERT INTO individual_grade (evaluation_id, student_id, criterion_id, performance_level_id, score)
SELECT e.id, s.id, ec.id, pl.id, pl.score
FROM (VALUES
    ('Equipo Sirius', 'docente.demo@unicesar.edu.co',     'mateo.ospino.demo@unicesar.edu.co',   'RAP5', 2, 2),
    ('Equipo Sirius', 'docente.demo@unicesar.edu.co',     'sofia.daza.demo@unicesar.edu.co',     'RAP2', 1, 4),
    ('Equipo Sirius', 'hernan.pumarejo.demo@example.com', 'julian.maestre.demo@unicesar.edu.co', 'RAP4', 1, 4)
) AS v (group_name, evaluator_email, student_email, outcome_code, criterion_order, level_order)
JOIN demo_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email
JOIN evaluation e ON e.project_group_id = g.id AND e.evaluator_id = u.id
JOIN app_user s ON s.email = v.student_email
JOIN learning_outcome lo ON lo.moment_id = g.moment_id AND lo.code = v.outcome_code
JOIN evaluation_criterion ec ON ec.learning_outcome_id = lo.id AND ec.display_order = v.criterion_order
JOIN performance_level pl ON pl.criterion_id = ec.id AND pl.display_order = v.level_order;

DROP VIEW demo_group;

COMMIT;

-- -----------------------------------------------------------------------------
-- Comprobación: grupos de la docente de demostración
-- -----------------------------------------------------------------------------
SELECT g.name                                                         AS grupo,
       m.name                                                         AS momento,
       (SELECT COUNT(*) FROM project_group_member pm WHERE pm.project_group_id = g.id)    AS integrantes,
       (SELECT COUNT(*) FROM project_group_evaluator pe WHERE pe.project_group_id = g.id) AS evaluadores,
       (SELECT COUNT(*) FROM submission su WHERE su.project_group_id = g.id)              AS entregas,
       (SELECT STRING_AGG(e.status, ', ' ORDER BY e.status) FROM evaluation e
         WHERE e.project_group_id = g.id)                             AS evaluaciones,
       g.evaluation_date                                              AS fecha_evaluacion
FROM project_group g
JOIN moment m ON m.id = g.moment_id
JOIN app_user t ON t.id = g.teacher_id AND t.email = 'docente.demo@unicesar.edu.co'
ORDER BY m.number, g.name;
