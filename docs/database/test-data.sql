-- =============================================================================
-- SIERAP · Datos de prueba
--
-- Llena las 26 tablas del sistema con al menos cinco filas cada una, sobre los datos
-- semilla de las migraciones (V2: facultad 04, programa 108, periodo 2026-2, Momento 1
-- con sus 5 RAP, criterios y niveles; V3: tipos Empresario y Egresado; V6: primer
-- super_admin).
--
-- NO es una migración: no está en db/migration para que nunca llegue a producción.
-- Se ejecuta a mano, una sola vez, sobre una base recién creada por Flyway:
--   * pgAdmin: base sierap → Query Tool → abrir este archivo → F5.
--   * psql:    psql -U sierap -h localhost -d sierap -f docs/database/test-data.sql
-- Va dentro de una transacción: si algo falla (por ejemplo, porque ya se ejecutó),
-- no se guarda nada. Para empezar de cero, borrar y recrear la base sierap y
-- arrancar el backend para que Flyway la reconstruya.
--
-- Fecha de referencia: 30 de octubre de 2026. El Momento 1 de Ingeniería de Sistemas
-- queda lanzado y en su ventana de calificación (19 oct – 27 nov).
--
-- Los tres casos que pide la rúbrica de datos:
--   1. Filas que se relacionan con varias: la facultad 04 tiene 4 programas y 2
--      admins; el docente Carlos Mendoza tiene 2 grupos y 2 códigos de registro; el
--      grupo Equipo Gamma tiene 5 integrantes, 2 evaluadores externos y 3
--      calificaciones; el evaluador Álvaro Castilla está designado en 2 grupos.
--   2. Filas que no se relacionan con ninguna: la Facultad de Bellas Artes (sin
--      programas ni cuentas), el programa Ingeniería Ambiental (sin momentos ni
--      estudiantes), los periodos 2025-1, 2025-2, 2026-1 y 2027-1 (sin momentos), la
--      docente Diana Castro (sin grupos), el Equipo Zeta (sin integrantes, entregas ni
--      calificaciones), el evaluador Rodrigo Villa (sin grupos), el tipo Investigador
--      (sin evaluadores) y los estudiantes Óscar Salas y Juliana Molina (sin grupo).
--   3. Datos rechazados por el servidor: ver rejection-tests.sql.
--
-- Contraseña de todas las cuentas de prueba: Sierap-Inicio-2026 (el mismo hash BCrypt
-- del super_admin inicial de V6), salvo Fabián Rincón, cuya cuenta está pendiente de
-- activar y no tiene contraseña.
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 1. Estructura académica
-- -----------------------------------------------------------------------------

-- Bellas Artes queda sin programas ni cuentas (caso "sin relaciones").
INSERT INTO faculty (code, name)
VALUES ('01', 'Facultad de Ciencias Administrativas, Contables y Económicas'),
       ('02', 'Facultad de Ciencias de la Salud'),
       ('03', 'Facultad de Derecho, Ciencias Políticas y Sociales'),
       ('05', 'Facultad de Bellas Artes');

-- La facultad 04 queda con 4 programas (caso "se relaciona con varias").
-- Ingeniería Ambiental queda sin momentos ni estudiantes.
INSERT INTO program (code, name, faculty_id)
SELECT v.code, v.name, f.id
FROM (VALUES
    ('109', 'Ingeniería Electrónica',             '04'),
    ('110', 'Ingeniería Agroindustrial',          '04'),
    ('111', 'Ingeniería Ambiental y Sanitaria',   '04'),
    ('201', 'Contaduría Pública',                 '01'),
    ('301', 'Enfermería',                         '02'),
    ('401', 'Derecho',                            '03')
) AS v (code, name, faculty_code)
JOIN faculty f ON f.code = v.faculty_code;

-- Periodos anteriores y el siguiente: ninguno tiene momentos todavía.
INSERT INTO academic_period (code, start_date, end_date, upc_evaluation_deadline)
VALUES ('2025-1', DATE '2025-02-03', DATE '2025-06-14', DATE '2025-06-07'),
       ('2025-2', DATE '2025-08-04', DATE '2025-12-06', DATE '2025-11-29'),
       ('2026-1', DATE '2026-02-02', DATE '2026-06-13', DATE '2026-06-06'),
       ('2027-1', DATE '2027-02-01', DATE '2027-06-12', DATE '2027-06-05');

INSERT INTO global_parameter (param_key, param_value, description)
VALUES ('institution.name',       'Universidad Popular del Cesar',     'Nombre de la institución en los reportes.'),
       ('institution.short_name', 'UPC',                               'Sigla de la institución.'),
       ('support.email',          'soporte.sierap@unicesar.edu.co',    'Correo de soporte que se muestra a los usuarios.'),
       ('timezone',               'America/Bogota',                    'Zona horaria de las fechas del sistema.'),
       ('reports.footer',         'Vicerrectoría Académica · SIERAP',  'Pie de página de los reportes exportados.');

-- El tipo Investigador queda sin evaluadores; Representante gremial está inactivo.
INSERT INTO evaluator_type (name, description, active)
VALUES ('Par académico',         'Docente de otra institución que evalúa proyectos de aula.',      TRUE),
       ('Investigador',          'Investigador de un grupo reconocido que evalúa proyectos de aula.', TRUE),
       ('Representante gremial', 'Representante de un gremio del sector productivo.',              FALSE);

-- El Momento 1 de Ingeniería de Sistemas (semilla de V2) se lanza: ventanas en orden
-- (conformación → entregas → calificación), calificación antes del límite UPC
-- (28 nov) y pesos que suman 100 % (docente 60 + Empresario 25 + Egresado 15).
UPDATE moment m
SET status                = 'LAUNCHED',
    group_formation_start = DATE '2026-08-10',
    group_formation_end   = DATE '2026-08-28',
    submission_start      = DATE '2026-09-01',
    submission_end        = DATE '2026-10-16',
    grading_start         = DATE '2026-10-19',
    grading_end           = DATE '2026-11-27',
    teacher_weight        = 60
FROM program p, academic_period ap
WHERE p.id = m.program_id AND p.code = '108'
  AND ap.id = m.period_id AND ap.code = '2026-2'
  AND m.number = 1;

-- Momentos todavía en configuración: sin ventanas de fechas (ADR-012).
INSERT INTO moment (program_id, period_id, number, name, subject, semester_from, semester_to,
                    requirement_level, min_members, max_members, status, teacher_weight)
SELECT p.id, ap.id, v.number, v.name, v.subject, v.semester_from, v.semester_to,
       v.requirement_level, v.min_members, v.max_members, 'CONFIGURATION', v.teacher_weight
FROM (VALUES
    ('108', 2, 'Momento 2', 'Estructuras de Datos (SS400)',     4, 6, 'Medio', 3, 5,  50),
    ('109', 1, 'Momento 1', 'Electrónica Digital (EL200)',      1, 3, 'Bajo',  2, 4,  80),
    ('201', 1, 'Momento 1', 'Contabilidad General (CP100)',     1, 3, 'Bajo',  3, 5, 100),
    ('301', 1, 'Momento 1', 'Fundamentos de Enfermería (EN100)', 1, 2, 'Bajo',  2, 4, 100)
) AS v (program_code, number, name, subject, semester_from, semester_to,
        requirement_level, min_members, max_members, teacher_weight)
JOIN program p ON p.code = v.program_code
JOIN academic_period ap ON ap.code = '2026-2';

-- Pesos de los evaluadores externos: en cada momento suman 100 con el del docente.
INSERT INTO evaluator_weight (moment_id, evaluator_type_id, weight)
SELECT m.id, et.id, v.weight
FROM (VALUES
    ('108', 1, 'Empresario', 25),
    ('108', 1, 'Egresado',   15),
    ('108', 2, 'Empresario', 30),
    ('108', 2, 'Egresado',   20),
    ('109', 1, 'Empresario', 20)
) AS v (program_code, moment_number, type_name, weight)
JOIN program p ON p.code = v.program_code
JOIN academic_period ap ON ap.code = '2026-2'
JOIN moment m ON m.program_id = p.id AND m.period_id = ap.id AND m.number = v.moment_number
JOIN evaluator_type et ON et.name = v.type_name;

-- -----------------------------------------------------------------------------
-- 2. Usuarios (herencia JOINED: primero app_user, luego la tabla de cada rol)
-- -----------------------------------------------------------------------------

INSERT INTO app_user (role, first_names, last_names, document_type, document_number, email,
                      password_hash, active, created_at, must_change_password)
SELECT v.role, v.first_names, v.last_names, v.document_type, v.document_number, v.email,
       CASE WHEN v.pending THEN NULL ELSE h.hash END, v.active, v.created_at, FALSE
FROM (SELECT '$2a$10$TyYxoblQNjR5HG3q4muPCOHfKL5fgtOdta7NLm8U3E7/WOkQrNYai'::VARCHAR AS hash) h
CROSS JOIN (VALUES
    -- Super administradores (Yolanda Cujia está desactivada)
    ('SUPER_ADMIN', 'Ana Milena',       'Durán Carrillo',    'CC', '1065000001', 'ana.duran@unicesar.edu.co',        TRUE,  FALSE, TIMESTAMP '2026-07-01 08:00'),
    ('SUPER_ADMIN', 'Ricardo Alfonso',  'Palmera Gómez',     'CC', '1065000002', 'ricardo.palmera@unicesar.edu.co',  TRUE,  FALSE, TIMESTAMP '2026-07-01 08:05'),
    ('SUPER_ADMIN', 'Yolanda Esther',   'Cujia Bolaño',      'CC', '1065000003', 'yolanda.cujia@unicesar.edu.co',    FALSE, FALSE, TIMESTAMP '2026-07-01 08:10'),
    ('SUPER_ADMIN', 'Germán Darío',     'Ustate Frías',      'CC', '1065000004', 'german.ustate@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-07-01 08:15'),
    -- Administradores de facultad (Fabián Rincón aún no activa su cuenta)
    ('ADMIN', 'Rafael Antonio',   'Maestre Cuello',    'CC', '1065000011', 'rafael.maestre@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-07-15 09:00'),
    ('ADMIN', 'Liliana Patricia', 'Ochoa Campo',       'CC', '1065000012', 'liliana.ochoa@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-07-15 09:10'),
    ('ADMIN', 'Hernando José',    'Pimienta Vega',     'CC', '1065000013', 'hernando.pimienta@unicesar.edu.co', TRUE, FALSE, TIMESTAMP '2026-07-15 09:20'),
    ('ADMIN', 'Gloria Inés',      'Baute Mendoza',     'CC', '1065000014', 'gloria.baute@unicesar.edu.co',     TRUE,  FALSE, TIMESTAMP '2026-07-15 09:30'),
    ('ADMIN', 'Fabián Enrique',   'Rincón Socarrás',   'CC', '1065000015', 'fabian.rincon@unicesar.edu.co',    TRUE,  TRUE,  TIMESTAMP '2026-10-28 10:00'),
    -- Docentes
    ('TEACHER', 'Carlos Eduardo',  'Mendoza Ríos',     'CC', '1065000021', 'carlos.mendoza@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-07-20 10:00'),
    ('TEACHER', 'Laura Patricia',  'Quintero Daza',    'CC', '1065000022', 'laura.quintero@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-07-20 10:10'),
    ('TEACHER', 'Andrés Felipe',   'Ospina Lara',      'CC', '1065000023', 'andres.ospina@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-07-20 10:20'),
    ('TEACHER', 'Diana Marcela',   'Castro Pineda',    'CC', '1065000024', 'diana.castro@unicesar.edu.co',     TRUE,  FALSE, TIMESTAMP '2026-07-20 10:30'),
    ('TEACHER', 'Jorge Luis',      'Sanjuán Ovalle',   'CC', '1065000025', 'jorge.sanjuan@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-07-21 11:00'),
    ('TEACHER', 'Martha Cecilia',  'Fragozo Lúquez',   'CC', '1065000026', 'martha.fragozo@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-07-21 11:10'),
    -- Estudiantes
    ('STUDENT', 'Juan David',      'Pérez Araújo',     'TI', '1003000001', 'juan.perez@unicesar.edu.co',       TRUE,  FALSE, TIMESTAMP '2026-08-05 14:00'),
    ('STUDENT', 'María Fernanda',  'Gutiérrez Daza',   'CC', '1003000002', 'maria.gutierrez@unicesar.edu.co',  TRUE,  FALSE, TIMESTAMP '2026-08-05 14:05'),
    ('STUDENT', 'Luis Alberto',    'Mejía Oñate',      'CC', '1003000003', 'luis.mejia@unicesar.edu.co',       TRUE,  FALSE, TIMESTAMP '2026-08-05 14:10'),
    ('STUDENT', 'Valentina',       'Rojas Maestre',    'TI', '1003000004', 'valentina.rojas@unicesar.edu.co',  TRUE,  FALSE, TIMESTAMP '2026-08-05 14:15'),
    ('STUDENT', 'Santiago',        'Herrera Fuentes',  'CC', '1003000005', 'santiago.herrera@unicesar.edu.co', TRUE,  FALSE, TIMESTAMP '2026-08-05 14:20'),
    ('STUDENT', 'Camila Andrea',   'Zuleta Baquero',   'CC', '1003000006', 'camila.zuleta@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-08-05 14:25'),
    ('STUDENT', 'Daniel Esteban',  'Quiroz Molina',    'CC', '1003000007', 'daniel.quiroz@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-08-05 14:30'),
    ('STUDENT', 'Isabella',        'Ariza Romero',     'TI', '1003000008', 'isabella.ariza@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-08-06 09:00'),
    ('STUDENT', 'Sebastián',       'Cuello Pinto',     'CC', '1003000009', 'sebastian.cuello@unicesar.edu.co', TRUE,  FALSE, TIMESTAMP '2026-08-06 09:05'),
    ('STUDENT', 'Laura Sofía',     'Montero Villazón', 'CC', '1003000010', 'laura.montero@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-08-06 09:10'),
    ('STUDENT', 'Andrés Felipe',   'Galvis Ustariz',   'CC', '1003000011', 'andres.galvis@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-08-06 09:15'),
    ('STUDENT', 'Natalia',         'Orozco Castilla',  'CC', '1003000012', 'natalia.orozco@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-08-06 09:20'),
    ('STUDENT', 'Kevin José',      'Torres Arzuaga',   'CC', '1003000013', 'kevin.torres@unicesar.edu.co',     TRUE,  FALSE, TIMESTAMP '2026-08-06 09:25'),
    ('STUDENT', 'Paula Andrea',    'Cotes Lacouture',  'CC', '1003000014', 'paula.cotes@unicesar.edu.co',      TRUE,  FALSE, TIMESTAMP '2026-08-06 09:30'),
    ('STUDENT', 'Miguel Ángel',    'Barros Celedón',   'CC', '1003000015', 'miguel.barros@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-08-06 09:35'),
    ('STUDENT', 'Sara Lucía',      'Amaya Brito',      'CC', '1003000016', 'sara.amaya@unicesar.edu.co',       TRUE,  FALSE, TIMESTAMP '2026-08-07 15:00'),
    ('STUDENT', 'Jhon Jairo',      'Peralta Mojica',   'CC', '1003000017', 'jhon.peralta@unicesar.edu.co',     TRUE,  FALSE, TIMESTAMP '2026-08-07 15:05'),
    ('STUDENT', 'Daniela',         'Mestre Arrieta',   'CC', '1003000018', 'daniela.mestre@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-08-07 15:10'),
    ('STUDENT', 'Óscar Iván',      'Salas Guerra',     'CC', '1003000019', 'oscar.salas@unicesar.edu.co',      TRUE,  FALSE, TIMESTAMP '2026-08-07 15:15'),
    ('STUDENT', 'Juliana',         'Molina Cárdenas',  'CC', '1003000020', 'juliana.molina@unicesar.edu.co',   TRUE,  FALSE, TIMESTAMP '2026-08-10 08:00'),
    ('STUDENT', 'Carlos Andrés',   'Vega Daza',        'CC', '1003000021', 'carlos.vega@unicesar.edu.co',      TRUE,  FALSE, TIMESTAMP '2026-08-10 08:10'),
    ('STUDENT', 'Mariana',         'Ortiz Calderón',   'CC', '1003000022', 'mariana.ortiz@unicesar.edu.co',    TRUE,  FALSE, TIMESTAMP '2026-08-10 08:20'),
    -- Evaluadores externos (correos de ejemplo: no son personas reales)
    ('EXTERNAL_EVALUATOR', 'Álvaro José',     'Castilla Murgas',  'CC',       '7700000001', 'alvaro.castilla@example.com', TRUE, FALSE, TIMESTAMP '2026-09-15 10:00'),
    ('EXTERNAL_EVALUATOR', 'Tatiana Lucía',   'Arrieta Oñate',    'CC',       '7700000002', 'tatiana.arrieta@example.com', TRUE, FALSE, TIMESTAMP '2026-09-15 10:10'),
    ('EXTERNAL_EVALUATOR', 'Ernesto Rafael',  'Gnecco Díaz',      'CE',       '7700000003', 'ernesto.gnecco@example.com',  TRUE, FALSE, TIMESTAMP '2026-09-16 10:00'),
    ('EXTERNAL_EVALUATOR', 'Yesenia Paola',   'Lúquez Romero',    'CC',       '7700000004', 'yesenia.luquez@example.com',  TRUE, FALSE, TIMESTAMP '2026-09-16 10:10'),
    ('EXTERNAL_EVALUATOR', 'Rodrigo Esteban', 'Villa Montes',     'PASSPORT', 'AB1234567',  'rodrigo.villa@example.com',   TRUE, FALSE, TIMESTAMP '2026-09-17 10:00')
) AS v (role, first_names, last_names, document_type, document_number, email, active, pending, created_at);

INSERT INTO super_admin (id)
SELECT u.id FROM app_user u
WHERE u.email IN ('ana.duran@unicesar.edu.co', 'ricardo.palmera@unicesar.edu.co',
                  'yolanda.cujia@unicesar.edu.co', 'german.ustate@unicesar.edu.co');

-- La facultad 04 tiene dos admins; Bellas Artes (05) ninguno.
INSERT INTO admin (id, faculty_id)
SELECT u.id, f.id
FROM (VALUES
    ('rafael.maestre@unicesar.edu.co',    '04'),
    ('liliana.ochoa@unicesar.edu.co',     '04'),
    ('hernando.pimienta@unicesar.edu.co', '01'),
    ('gloria.baute@unicesar.edu.co',      '02'),
    ('fabian.rincon@unicesar.edu.co',     '03')
) AS v (email, faculty_code)
JOIN app_user u ON u.email = v.email
JOIN faculty f ON f.code = v.faculty_code;

INSERT INTO teacher (id, faculty_id)
SELECT u.id, f.id
FROM (VALUES
    ('carlos.mendoza@unicesar.edu.co', '04'),
    ('laura.quintero@unicesar.edu.co', '04'),
    ('andres.ospina@unicesar.edu.co',  '04'),
    ('diana.castro@unicesar.edu.co',   '04'),
    ('jorge.sanjuan@unicesar.edu.co',  '01'),
    ('martha.fragozo@unicesar.edu.co', '02')
) AS v (email, faculty_code)
JOIN app_user u ON u.email = v.email
JOIN faculty f ON f.code = v.faculty_code;

-- Códigos de registro: Carlos Mendoza tiene dos; ELE-MEN-2026 sigue vigente y
-- nadie lo ha usado; SIS-CAS-2026 fue desactivado sin usarse.
INSERT INTO registration_code (code, teacher_id, program_id, expires_on, active, created_at)
SELECT v.code, u.id, p.id, v.expires_on, v.active, v.created_at
FROM (VALUES
    ('SIS-MEN-2026', 'carlos.mendoza@unicesar.edu.co', '108', DATE '2026-08-28', TRUE,  TIMESTAMP '2026-08-03 08:00'),
    ('SIS-QUI-2026', 'laura.quintero@unicesar.edu.co', '108', DATE '2026-08-28', TRUE,  TIMESTAMP '2026-08-04 08:00'),
    ('SIS-OSP-2026', 'andres.ospina@unicesar.edu.co',  '108', DATE '2026-08-28', TRUE,  TIMESTAMP '2026-08-04 09:00'),
    ('ELE-MEN-2026', 'carlos.mendoza@unicesar.edu.co', '109', DATE '2026-11-30', TRUE,  TIMESTAMP '2026-08-05 08:00'),
    ('SIS-CAS-2026', 'diana.castro@unicesar.edu.co',   '108', DATE '2026-08-28', FALSE, TIMESTAMP '2026-08-05 09:00'),
    ('CON-SAN-2026', 'jorge.sanjuan@unicesar.edu.co',  '201', DATE '2026-08-28', TRUE,  TIMESTAMP '2026-08-06 08:00')
) AS v (code, teacher_email, program_code, expires_on, active, created_at)
JOIN app_user u ON u.email = v.teacher_email
JOIN program p ON p.code = v.program_code;

-- Estudiantes. Juliana Molina (6.º semestre) y Carlos Vega los creó un admin, sin código.
INSERT INTO student (id, student_code, current_semester, program_id, registration_code_id)
SELECT u.id, v.student_code, v.semester, p.id, rc.id
FROM (VALUES
    ('juan.perez@unicesar.edu.co',       '2026108001', 1, '108', 'SIS-MEN-2026'),
    ('maria.gutierrez@unicesar.edu.co',  '2025208002', 2, '108', 'SIS-MEN-2026'),
    ('luis.mejia@unicesar.edu.co',       '2025108003', 3, '108', 'SIS-MEN-2026'),
    ('valentina.rojas@unicesar.edu.co',  '2026108004', 1, '108', 'SIS-MEN-2026'),
    ('santiago.herrera@unicesar.edu.co', '2025208005', 2, '108', 'SIS-MEN-2026'),
    ('camila.zuleta@unicesar.edu.co',    '2025208006', 2, '108', 'SIS-MEN-2026'),
    ('daniel.quiroz@unicesar.edu.co',    '2025108007', 3, '108', 'SIS-MEN-2026'),
    ('isabella.ariza@unicesar.edu.co',   '2026108008', 1, '108', 'SIS-QUI-2026'),
    ('sebastian.cuello@unicesar.edu.co', '2025208009', 2, '108', 'SIS-QUI-2026'),
    ('laura.montero@unicesar.edu.co',    '2025108010', 3, '108', 'SIS-QUI-2026'),
    ('andres.galvis@unicesar.edu.co',    '2025208011', 2, '108', 'SIS-QUI-2026'),
    ('natalia.orozco@unicesar.edu.co',   '2025108012', 3, '108', 'SIS-QUI-2026'),
    ('kevin.torres@unicesar.edu.co',     '2026108013', 1, '108', 'SIS-QUI-2026'),
    ('paula.cotes@unicesar.edu.co',      '2025208014', 2, '108', 'SIS-QUI-2026'),
    ('miguel.barros@unicesar.edu.co',    '2025108015', 3, '108', 'SIS-QUI-2026'),
    ('sara.amaya@unicesar.edu.co',       '2025208016', 2, '108', 'SIS-OSP-2026'),
    ('jhon.peralta@unicesar.edu.co',     '2025108017', 3, '108', 'SIS-OSP-2026'),
    ('daniela.mestre@unicesar.edu.co',   '2026108018', 1, '108', 'SIS-OSP-2026'),
    ('oscar.salas@unicesar.edu.co',      '2025208019', 2, '108', 'SIS-OSP-2026'),
    ('juliana.molina@unicesar.edu.co',   '2024108020', 6, '108', NULL),
    ('carlos.vega@unicesar.edu.co',      '2025209021', 2, '109', NULL),
    ('mariana.ortiz@unicesar.edu.co',    '2025220122', 3, '201', 'CON-SAN-2026')
) AS v (email, student_code, semester, program_code, registration_code)
JOIN app_user u ON u.email = v.email
JOIN program p ON p.code = v.program_code
LEFT JOIN registration_code rc ON rc.code = v.registration_code;

-- Rodrigo Villa (Par académico) no tiene grupos: su tipo no tiene peso en el Momento 1.
INSERT INTO external_evaluator (id, evaluator_type_id, organization, job_position,
                                graduation_year, graduation_program_id)
SELECT u.id, et.id, v.organization, v.job_position, v.graduation_year, p.id
FROM (VALUES
    ('alvaro.castilla@example.com', 'Empresario',    'Software del Caribe S.A.S.',     'Gerente de proyectos',    NULL::INTEGER, NULL),
    ('tatiana.arrieta@example.com', 'Egresado',      'Banco de la Costa',              'Ingeniera de desarrollo', 2019,          '108'),
    ('ernesto.gnecco@example.com',  'Empresario',    'ValleTech Soluciones',           'Director de tecnología',  NULL,          NULL),
    ('yesenia.luquez@example.com',  'Egresado',      'Consultora Digital del Cesar',   'Analista de datos',       2021,          '108'),
    ('rodrigo.villa@example.com',   'Par académico', 'Universidad de la Región Caribe', 'Docente investigador',   NULL,          NULL)
) AS v (email, type_name, organization, job_position, graduation_year, graduation_program_code)
JOIN app_user u ON u.email = v.email
JOIN evaluator_type et ON et.name = v.type_name
LEFT JOIN program p ON p.code = v.graduation_program_code;

-- Quién creó cada cuenta (app_user.created_by_id).
UPDATE app_user u
SET created_by_id = creator.id
FROM (VALUES
    ('ana.duran@unicesar.edu.co',         'sierap@unicesar.edu.co'),
    ('ricardo.palmera@unicesar.edu.co',   'sierap@unicesar.edu.co'),
    ('yolanda.cujia@unicesar.edu.co',     'sierap@unicesar.edu.co'),
    ('german.ustate@unicesar.edu.co',     'sierap@unicesar.edu.co'),
    ('rafael.maestre@unicesar.edu.co',    'sierap@unicesar.edu.co'),
    ('liliana.ochoa@unicesar.edu.co',     'sierap@unicesar.edu.co'),
    ('hernando.pimienta@unicesar.edu.co', 'ana.duran@unicesar.edu.co'),
    ('gloria.baute@unicesar.edu.co',      'ana.duran@unicesar.edu.co'),
    ('fabian.rincon@unicesar.edu.co',     'ana.duran@unicesar.edu.co'),
    ('carlos.mendoza@unicesar.edu.co',    'rafael.maestre@unicesar.edu.co'),
    ('laura.quintero@unicesar.edu.co',    'rafael.maestre@unicesar.edu.co'),
    ('andres.ospina@unicesar.edu.co',     'liliana.ochoa@unicesar.edu.co'),
    ('diana.castro@unicesar.edu.co',      'liliana.ochoa@unicesar.edu.co'),
    ('jorge.sanjuan@unicesar.edu.co',     'hernando.pimienta@unicesar.edu.co'),
    ('martha.fragozo@unicesar.edu.co',    'gloria.baute@unicesar.edu.co'),
    ('juliana.molina@unicesar.edu.co',    'rafael.maestre@unicesar.edu.co'),
    ('carlos.vega@unicesar.edu.co',       'rafael.maestre@unicesar.edu.co'),
    ('alvaro.castilla@example.com',       'carlos.mendoza@unicesar.edu.co'),
    ('tatiana.arrieta@example.com',       'carlos.mendoza@unicesar.edu.co'),
    ('ernesto.gnecco@example.com',        'laura.quintero@unicesar.edu.co'),
    ('yesenia.luquez@example.com',        'laura.quintero@unicesar.edu.co'),
    ('rodrigo.villa@example.com',         'andres.ospina@unicesar.edu.co')
) AS v (email, creator_email)
JOIN app_user creator ON creator.email = v.creator_email
WHERE u.email = v.email;

-- Los estudiantes que se registraron con un código quedan creados por su docente.
UPDATE app_user u
SET created_by_id = rc.teacher_id
FROM student s
JOIN registration_code rc ON rc.id = s.registration_code_id
WHERE s.id = u.id;

-- -----------------------------------------------------------------------------
-- 3. Operación del Momento 1 de Ingeniería de Sistemas (2026-2)
-- -----------------------------------------------------------------------------

-- Cada docente habilita sus grupos. El Equipo Zeta se habilitó, pero nadie se inscribió.
INSERT INTO project_group (moment_id, teacher_id, name, project_title,
                           submission_deadline, evaluation_date)
SELECT m.id, u.id, v.name, v.project_title, DATE '2026-10-16', v.evaluation_date
FROM (VALUES
    ('Equipo Alfa',    'carlos.mendoza@unicesar.edu.co', 'Sistema de reservas de laboratorios',             DATE '2026-10-22'),
    ('Equipo Beta',    'carlos.mendoza@unicesar.edu.co', 'Control de inventario para tiendas de barrio',    DATE '2026-10-23'),
    ('Equipo Gamma',   'laura.quintero@unicesar.edu.co', 'Plataforma de tutorías entre pares',              DATE '2026-10-26'),
    ('Equipo Delta',   'laura.quintero@unicesar.edu.co', 'Gestor de rutas de reciclaje',                    DATE '2026-10-27'),
    ('Equipo Épsilon', 'andres.ospina@unicesar.edu.co',  'Préstamo de libros de la biblioteca',             DATE '2026-10-28'),
    ('Equipo Zeta',    'andres.ospina@unicesar.edu.co',  NULL,                                              DATE '2026-11-05')
) AS v (name, teacher_email, project_title, evaluation_date)
JOIN app_user u ON u.email = v.teacher_email
JOIN program p ON p.code = '108'
JOIN academic_period ap ON ap.code = '2026-2'
JOIN moment m ON m.program_id = p.id AND m.period_id = ap.id AND m.number = 1;

-- Integrantes: entre 3 y 5 por grupo (min_members y max_members del Momento 1).
INSERT INTO project_group_member (project_group_id, student_id)
SELECT g.id, u.id
FROM (VALUES
    ('Equipo Alfa',    'juan.perez@unicesar.edu.co'),
    ('Equipo Alfa',    'maria.gutierrez@unicesar.edu.co'),
    ('Equipo Alfa',    'luis.mejia@unicesar.edu.co'),
    ('Equipo Alfa',    'valentina.rojas@unicesar.edu.co'),
    ('Equipo Beta',    'santiago.herrera@unicesar.edu.co'),
    ('Equipo Beta',    'camila.zuleta@unicesar.edu.co'),
    ('Equipo Beta',    'daniel.quiroz@unicesar.edu.co'),
    ('Equipo Gamma',   'isabella.ariza@unicesar.edu.co'),
    ('Equipo Gamma',   'sebastian.cuello@unicesar.edu.co'),
    ('Equipo Gamma',   'laura.montero@unicesar.edu.co'),
    ('Equipo Gamma',   'andres.galvis@unicesar.edu.co'),
    ('Equipo Gamma',   'natalia.orozco@unicesar.edu.co'),
    ('Equipo Delta',   'kevin.torres@unicesar.edu.co'),
    ('Equipo Delta',   'paula.cotes@unicesar.edu.co'),
    ('Equipo Delta',   'miguel.barros@unicesar.edu.co'),
    ('Equipo Épsilon', 'sara.amaya@unicesar.edu.co'),
    ('Equipo Épsilon', 'jhon.peralta@unicesar.edu.co'),
    ('Equipo Épsilon', 'daniela.mestre@unicesar.edu.co')
) AS v (group_name, student_email)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.student_email;

-- Evaluadores externos designados: solo de tipos con peso en el Momento 1.
INSERT INTO project_group_evaluator (project_group_id, external_evaluator_id)
SELECT g.id, u.id
FROM (VALUES
    ('Equipo Alfa',  'alvaro.castilla@example.com'),
    ('Equipo Alfa',  'tatiana.arrieta@example.com'),
    ('Equipo Gamma', 'alvaro.castilla@example.com'),
    ('Equipo Gamma', 'yesenia.luquez@example.com'),
    ('Equipo Delta', 'ernesto.gnecco@example.com')
) AS v (group_name, evaluator_email)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email;

-- Entregas versionadas. Las rutas siguen el formato del backend
-- (groups/<id>/v<versión>/<uuid>.<ext>), pero los archivos no existen en disco: la
-- descarga desde la aplicación fallará para estas filas.
-- Alfa sube dos versiones; Beta, sin presentación; Delta, tarde (después del 16 oct).
INSERT INTO submission (project_group_id, version_number, zip_file_path, presentation_path,
                        submitted_at, late, zip_file_name, zip_size,
                        presentation_file_name, presentation_size)
SELECT g.id, v.version_number,
       'groups/' || g.id || '/v' || v.version_number || '/' || gen_random_uuid() || '.zip',
       CASE WHEN v.presentation_file_name IS NULL THEN NULL
            ELSE 'groups/' || g.id || '/v' || v.version_number || '/' || gen_random_uuid() || '.pdf' END,
       v.submitted_at, v.late, v.zip_file_name, v.zip_size,
       v.presentation_file_name, v.presentation_size
FROM (VALUES
    ('Equipo Alfa',    1, TIMESTAMP '2026-09-20 21:14', FALSE, 'reservas-labs-v1.zip',   18432000, 'avance-1.pdf',       2150000),
    ('Equipo Alfa',    2, TIMESTAMP '2026-10-14 18:40', FALSE, 'reservas-labs-final.zip', 24117000, 'presentacion.pdf',  3480000),
    ('Equipo Beta',    1, TIMESTAMP '2026-10-10 22:05', FALSE, 'inventario.zip',         15360000, NULL,                 NULL),
    ('Equipo Gamma',   1, TIMESTAMP '2026-10-15 23:30', FALSE, 'tutorias-pares.zip',     31250000, 'sustentacion.pdf',   5120000),
    ('Equipo Delta',   1, TIMESTAMP '2026-10-17 00:12', TRUE,  'rutas-reciclaje.zip',    12800000, 'diapositivas.pdf',   2760000),
    ('Equipo Épsilon', 1, TIMESTAMP '2026-10-12 16:45', FALSE, 'biblioteca.zip',         20480000, 'presentacion.pdf',   4096000)
) AS v (group_name, version_number, submitted_at, late, zip_file_name, zip_size,
        presentation_file_name, presentation_size)
JOIN project_group g ON g.name = v.group_name;

-- -----------------------------------------------------------------------------
-- 4. Calificación (ventana del 19 oct al 27 nov; los externos, el día de su grupo)
-- -----------------------------------------------------------------------------

-- Solo las SUBMITTED cuentan para la nota; las DRAFT son borradores a medias.
INSERT INTO evaluation (project_group_id, evaluator_id, status, evaluated_at, comment)
SELECT g.id, u.id, v.status, v.evaluated_at, v.comment
FROM (VALUES
    ('Equipo Alfa',    'carlos.mendoza@unicesar.edu.co', 'SUBMITTED', TIMESTAMP '2026-10-22 11:00', 'Buen trabajo en equipo; reforzar el diagrama UML.'),
    ('Equipo Alfa',    'alvaro.castilla@example.com',    'SUBMITTED', TIMESTAMP '2026-10-22 11:30', 'Solución aplicable en una empresa real.'),
    ('Equipo Alfa',    'tatiana.arrieta@example.com',    'SUBMITTED', TIMESTAMP '2026-10-22 11:45', NULL),
    ('Equipo Beta',    'carlos.mendoza@unicesar.edu.co', 'SUBMITTED', TIMESTAMP '2026-10-23 10:00', 'Faltó separar la capa de datos.'),
    ('Equipo Gamma',   'laura.quintero@unicesar.edu.co', 'SUBMITTED', TIMESTAMP '2026-10-26 09:30', 'Proyecto sobresaliente.'),
    ('Equipo Gamma',   'alvaro.castilla@example.com',    'SUBMITTED', TIMESTAMP '2026-10-26 10:15', NULL),
    ('Equipo Gamma',   'yesenia.luquez@example.com',     'DRAFT',     TIMESTAMP '2026-10-26 10:40', NULL),
    ('Equipo Delta',   'laura.quintero@unicesar.edu.co', 'DRAFT',     TIMESTAMP '2026-10-27 15:00', 'Pendiente revisar la entrega tardía.'),
    ('Equipo Épsilon', 'andres.ospina@unicesar.edu.co',  'SUBMITTED', TIMESTAMP '2026-10-28 16:20', NULL)
) AS v (group_name, evaluator_email, status, evaluated_at, comment)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email;

-- Nivel elegido en cada criterio. Cada dígito del perfil es el display_order del nivel
-- (1 Bajo, 2 Básico, 3 Alto, 4 Sobresaliente) para los 8 criterios en orden:
-- RAP1, RAP2 (2), RAP3, RAP4 (2), RAP5 (2). Los borradores tienen menos dígitos porque
-- el evaluador aún no califica todos los criterios. El puntaje se copia del nivel.
INSERT INTO criterion_grade (evaluation_id, criterion_id, performance_level_id, score)
SELECT e.id, c.id, pl.id, pl.score
FROM (VALUES
    ('Equipo Alfa',    'carlos.mendoza@unicesar.edu.co', '44344344'),
    ('Equipo Alfa',    'alvaro.castilla@example.com',    '43344334'),
    ('Equipo Alfa',    'tatiana.arrieta@example.com',    '44434444'),
    ('Equipo Beta',    'carlos.mendoza@unicesar.edu.co', '33233323'),
    ('Equipo Gamma',   'laura.quintero@unicesar.edu.co', '44444444'),
    ('Equipo Gamma',   'alvaro.castilla@example.com',    '44434443'),
    ('Equipo Gamma',   'yesenia.luquez@example.com',     '434'),
    ('Equipo Delta',   'laura.quintero@unicesar.edu.co', '2323'),
    ('Equipo Épsilon', 'andres.ospina@unicesar.edu.co',  '32222322')
) AS v (group_name, evaluator_email, levels)
JOIN project_group g ON g.name = v.group_name
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

-- Ajustes individuales (ADR-018): el evaluador cambia el nivel de un criterio para
-- un integrante concreto, sobre todo en la sustentación (RAP5, criterio 2).
INSERT INTO individual_grade (evaluation_id, student_id, criterion_id, performance_level_id, score)
SELECT e.id, s.id, ec.id, pl.id, pl.score
FROM (VALUES
    ('Equipo Alfa',  'carlos.mendoza@unicesar.edu.co', 'luis.mejia@unicesar.edu.co',       'RAP5', 2, 2),
    ('Equipo Alfa',  'carlos.mendoza@unicesar.edu.co', 'valentina.rojas@unicesar.edu.co',  'RAP4', 1, 3),
    ('Equipo Alfa',  'alvaro.castilla@example.com',    'luis.mejia@unicesar.edu.co',       'RAP5', 2, 3),
    ('Equipo Gamma', 'laura.quintero@unicesar.edu.co', 'natalia.orozco@unicesar.edu.co',   'RAP5', 2, 1),
    ('Equipo Gamma', 'laura.quintero@unicesar.edu.co', 'sebastian.cuello@unicesar.edu.co', 'RAP5', 2, 3)
) AS v (group_name, evaluator_email, student_email, outcome_code, criterion_order, level_order)
JOIN project_group g ON g.name = v.group_name
JOIN app_user u ON u.email = v.evaluator_email
JOIN evaluation e ON e.project_group_id = g.id AND e.evaluator_id = u.id
JOIN app_user s ON s.email = v.student_email
JOIN learning_outcome lo ON lo.moment_id = g.moment_id AND lo.code = v.outcome_code
JOIN evaluation_criterion ec ON ec.learning_outcome_id = lo.id AND ec.display_order = v.criterion_order
JOIN performance_level pl ON pl.criterion_id = ec.id AND pl.display_order = v.level_order;

-- -----------------------------------------------------------------------------
-- 5. Autenticación (solo se guarda el hash SHA-256 del token, nunca el token)
-- -----------------------------------------------------------------------------

-- Sesiones: vigentes, una ya rotada (si vuelve a llegar es señal de robo) y una vencida.
INSERT INTO refresh_token (user_id, token_hash, expires_at, rotated_at, created_at)
SELECT u.id, ENCODE(SHA256(CONVERT_TO(v.token, 'UTF8')), 'hex'), v.expires_at, v.rotated_at, v.created_at
FROM (VALUES
    ('ana.duran@unicesar.edu.co',      'prueba-sesion-1', TIMESTAMP '2026-11-06 08:00', NULL::TIMESTAMP,             TIMESTAMP '2026-10-30 08:00'),
    ('carlos.mendoza@unicesar.edu.co', 'prueba-sesion-2', TIMESTAMP '2026-11-05 10:00', NULL,                        TIMESTAMP '2026-10-29 10:00'),
    ('juan.perez@unicesar.edu.co',     'prueba-sesion-3', TIMESTAMP '2026-11-04 19:00', TIMESTAMP '2026-10-29 19:30', TIMESTAMP '2026-10-28 19:00'),
    ('juan.perez@unicesar.edu.co',     'prueba-sesion-4', TIMESTAMP '2026-11-05 19:30', NULL,                        TIMESTAMP '2026-10-29 19:30'),
    ('rafael.maestre@unicesar.edu.co', 'prueba-sesion-5', TIMESTAMP '2026-10-20 09:00', NULL,                        TIMESTAMP '2026-10-13 09:00')
) AS v (email, token, expires_at, rotated_at, created_at)
JOIN app_user u ON u.email = v.email;

-- Enlaces de contraseña: pendiente, usado y vencido, de los dos propósitos.
INSERT INTO password_token (user_id, token_hash, purpose, expires_at, used_at, created_at)
SELECT u.id, ENCODE(SHA256(CONVERT_TO(v.token, 'UTF8')), 'hex'), v.purpose, v.expires_at, v.used_at, v.created_at
FROM (VALUES
    ('fabian.rincon@unicesar.edu.co',  'prueba-enlace-1', 'ACCOUNT_SETUP',  TIMESTAMP '2026-10-31 10:00', NULL::TIMESTAMP,             TIMESTAMP '2026-10-28 10:00'),
    ('rodrigo.villa@example.com',      'prueba-enlace-2', 'ACCOUNT_SETUP',  TIMESTAMP '2026-09-20 10:00', TIMESTAMP '2026-09-17 18:20', TIMESTAMP '2026-09-17 10:00'),
    ('oscar.salas@unicesar.edu.co',    'prueba-enlace-3', 'PASSWORD_RESET', TIMESTAMP '2026-10-30 23:00', NULL,                        TIMESTAMP '2026-10-30 22:00'),
    ('diana.castro@unicesar.edu.co',   'prueba-enlace-4', 'PASSWORD_RESET', TIMESTAMP '2026-09-02 12:00', NULL,                        TIMESTAMP '2026-09-02 11:00'),
    ('carlos.mendoza@unicesar.edu.co', 'prueba-enlace-5', 'PASSWORD_RESET', TIMESTAMP '2026-08-15 09:00', TIMESTAMP '2026-08-15 08:20', TIMESTAMP '2026-08-15 08:00')
) AS v (email, token, purpose, expires_at, used_at, created_at)
JOIN app_user u ON u.email = v.email;

COMMIT;

-- -----------------------------------------------------------------------------
-- Comprobación: filas por tabla (todas deben tener 5 o más)
-- -----------------------------------------------------------------------------
SELECT 'faculty' AS tabla, COUNT(*) AS filas FROM faculty
UNION ALL SELECT 'program', COUNT(*) FROM program
UNION ALL SELECT 'academic_period', COUNT(*) FROM academic_period
UNION ALL SELECT 'global_parameter', COUNT(*) FROM global_parameter
UNION ALL SELECT 'moment', COUNT(*) FROM moment
UNION ALL SELECT 'learning_outcome', COUNT(*) FROM learning_outcome
UNION ALL SELECT 'evaluation_criterion', COUNT(*) FROM evaluation_criterion
UNION ALL SELECT 'performance_level', COUNT(*) FROM performance_level
UNION ALL SELECT 'evaluator_weight', COUNT(*) FROM evaluator_weight
UNION ALL SELECT 'evaluator_type', COUNT(*) FROM evaluator_type
UNION ALL SELECT 'app_user', COUNT(*) FROM app_user
UNION ALL SELECT 'super_admin', COUNT(*) FROM super_admin
UNION ALL SELECT 'admin', COUNT(*) FROM admin
UNION ALL SELECT 'teacher', COUNT(*) FROM teacher
UNION ALL SELECT 'student', COUNT(*) FROM student
UNION ALL SELECT 'external_evaluator', COUNT(*) FROM external_evaluator
UNION ALL SELECT 'registration_code', COUNT(*) FROM registration_code
UNION ALL SELECT 'refresh_token', COUNT(*) FROM refresh_token
UNION ALL SELECT 'password_token', COUNT(*) FROM password_token
UNION ALL SELECT 'project_group', COUNT(*) FROM project_group
UNION ALL SELECT 'project_group_member', COUNT(*) FROM project_group_member
UNION ALL SELECT 'project_group_evaluator', COUNT(*) FROM project_group_evaluator
UNION ALL SELECT 'submission', COUNT(*) FROM submission
UNION ALL SELECT 'evaluation', COUNT(*) FROM evaluation
UNION ALL SELECT 'criterion_grade', COUNT(*) FROM criterion_grade
UNION ALL SELECT 'individual_grade', COUNT(*) FROM individual_grade
ORDER BY filas, tabla;
