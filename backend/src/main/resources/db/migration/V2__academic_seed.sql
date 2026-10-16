-- =============================================================================
-- V2 · Datos semilla de la estructura académica (Etapa 1, D2)
-- Facultad, Ingeniería de Sistemas, periodo actual y el Momento 1 en
-- CONFIGURATION con los 5 RAP, criterios y niveles de la rúbrica oficial
-- (docs/00-guia-oficial-momento1.md). Todo es editable por el admin: son datos,
-- no valores fijos del código (ADR-010). Los textos los ve el usuario final, por
-- eso van en español.
-- Puntaje máximo del Momento 1: 7 criterios x 0.60 + Sustentación 0.80 = 5.00
-- =============================================================================

INSERT INTO faculty (code, name)
VALUES ('04', 'Facultad de Ingenierías y Tecnologías');

INSERT INTO program (code, name, faculty_id)
SELECT '108', 'Ingeniería de Sistemas', f.id
FROM faculty f
WHERE f.code = '04';

-- PROVISIONAL: reemplazar con las fechas oficiales mediante una migración nueva
INSERT INTO academic_period (code, start_date, end_date, upc_evaluation_deadline)
VALUES ('2026-2', DATE '2026-08-03', DATE '2026-12-05', DATE '2026-11-28');

-- Momento 1: hasta tercer semestre, grupos de 3 a 5 integrantes. Sin ventanas de fechas todavía.
INSERT INTO moment (program_id, period_id, number, name, subject,
                    semester_from, semester_to, requirement_level,
                    min_members, max_members, status)
SELECT p.id, ap.id, 1, 'Momento 1', 'Programación II (SS300)',
       1, 3, 'Bajo',
       3, 5, 'CONFIGURATION'
FROM program p, academic_period ap
WHERE p.code = '108' AND ap.code = '2026-2';

INSERT INTO learning_outcome (moment_id, code, description)
SELECT m.id, v.code, v.description
FROM moment m
JOIN program p ON p.id = m.program_id AND p.code = '108'
JOIN academic_period ap ON ap.id = m.period_id AND ap.code = '2026-2'
CROSS JOIN (VALUES
    ('RAP1', 'INTEGRA las ciencias básicas y las ciencias básicas de la ingeniería para la resolución de problemas reales, promoviendo el desarrollo sostenible.'),
    ('RAP2', 'DESARROLLA productos de software que resuelven problemas reales, aplicando buenas prácticas y estándares de calidad.'),
    ('RAP3', 'CONSTRUYE modelos computacionales basados en métodos analíticos y experimentales para el desarrollo de soluciones de software.'),
    ('RAP4', 'GESTIONA proyectos de Tecnologías de la Información, para satisfacer las necesidades del entorno global.'),
    ('RAP5', 'EMPRENDE aplicando ciencia, tecnología e innovación desde su profesión, con pensamiento crítico, responsabilidad ética, liderazgo y colaboración.')
) AS v (code, description)
WHERE m.number = 1;

INSERT INTO evaluation_criterion (learning_outcome_id, description, display_order)
SELECT lo.id, v.description, v.display_order
FROM learning_outcome lo
JOIN moment m ON m.id = lo.moment_id AND m.number = 1
JOIN program p ON p.id = m.program_id AND p.code = '108'
JOIN academic_period ap ON ap.id = m.period_id AND ap.code = '2026-2'
JOIN (VALUES
    ('RAP1', 1, 'Identificación del problema'),
    ('RAP2', 1, 'Implementación de arquitectura por capas'),
    ('RAP2', 2, 'Uso de técnicas generales de POO'),
    ('RAP3', 1, 'Análisis y diseño (diagrama de clases UML)'),
    ('RAP4', 1, 'Funcionalidad de la solución desarrollada'),
    ('RAP4', 2, 'Uso de buenas prácticas en escritura de código'),
    ('RAP5', 1, 'Descripción de la solución'),
    ('RAP5', 2, 'Sustentación y/o nivel de dominio')
) AS v (outcome_code, display_order, description) ON v.outcome_code = lo.code;

INSERT INTO performance_level (criterion_id, name, description, score, display_order)
SELECT c.id, v.name, v.description, v.score, v.display_order
FROM evaluation_criterion c
JOIN learning_outcome lo ON lo.id = c.learning_outcome_id
JOIN moment m ON m.id = lo.moment_id AND m.number = 1
JOIN program p ON p.id = m.program_id AND p.code = '108'
JOIN academic_period ap ON ap.id = m.period_id AND ap.code = '2026-2'
JOIN (VALUES
    -- RAP1 · Identificación del problema
    ('RAP1', 1, 'Bajo',          0.00, 1, 'No identifica una problemática real, ni sus causas ni sus consecuencias.'),
    ('RAP1', 1, 'Básico',        0.36, 2, 'Identifica una problemática real, sin causas ni consecuencias.'),
    ('RAP1', 1, 'Alto',          0.48, 3, 'Identifica la problemática y presenta sus causas o sus consecuencias, pero no ambas.'),
    ('RAP1', 1, 'Sobresaliente', 0.60, 4, 'Identifica y describe el problema central, sus causas y sus consecuencias.'),
    -- RAP2 · Implementación de arquitectura por capas
    ('RAP2', 1, 'Bajo',          0.00, 1, 'No aplica arquitectura por capas.'),
    ('RAP2', 1, 'Básico',        0.36, 2, 'Implementa solo una capa.'),
    ('RAP2', 1, 'Alto',          0.48, 3, 'Implementa entre dos y tres capas.'),
    ('RAP2', 1, 'Sobresaliente', 0.60, 4, 'Implementa en Java bajo arquitectura por capas completa (datos, lógica, presentación y entidades).'),
    -- RAP2 · Uso de técnicas generales de POO
    ('RAP2', 2, 'Bajo',          0.00, 1, 'No incluye mecanismos ni principios de POO.'),
    ('RAP2', 2, 'Básico',        0.36, 2, 'Usa encapsulamiento y sobrecarga de métodos.'),
    ('RAP2', 2, 'Alto',          0.48, 3, 'Usa encapsulamiento, herencia y polimorfismo o interfaces.'),
    ('RAP2', 2, 'Sobresaliente', 0.60, 4, 'Usa encapsulamiento, herencia, polimorfismo o interfaces y gestión de excepciones.'),
    -- RAP3 · Análisis y diseño (diagrama de clases UML)
    ('RAP3', 1, 'Bajo',          0.00, 1, 'No presenta diagrama de clases UML.'),
    ('RAP3', 1, 'Básico',        0.36, 2, 'Identifica clases y atributos y elabora el diagrama, pero sin notación UML adecuada.'),
    ('RAP3', 1, 'Alto',          0.48, 3, 'Identifica clases y atributos y elabora el diagrama con notación UML adecuada.'),
    ('RAP3', 1, 'Sobresaliente', 0.60, 4, 'Identifica clases, atributos y relaciones (asociación, agregación, composición y herencia) y elabora el diagrama con notación UML adecuada.'),
    -- RAP4 · Funcionalidad de la solución desarrollada
    ('RAP4', 1, 'Bajo',          0.00, 1, 'No cumple los requerimientos funcionales.'),
    ('RAP4', 1, 'Básico',        0.36, 2, 'Soluciona mínimamente la problemática.'),
    ('RAP4', 1, 'Alto',          0.48, 3, 'Soluciona medianamente la problemática.'),
    ('RAP4', 1, 'Sobresaliente', 0.60, 4, 'Soluciona completamente la problemática identificada.'),
    -- RAP4 · Uso de buenas prácticas en escritura de código
    ('RAP4', 2, 'Bajo',          0.00, 1, 'No aplica buenas prácticas.'),
    ('RAP4', 2, 'Básico',        0.36, 2, 'El código está debidamente indentado.'),
    ('RAP4', 2, 'Alto',          0.48, 3, 'El código está debidamente indentado y usa notación camelCase para clases, métodos y variables.'),
    ('RAP4', 2, 'Sobresaliente', 0.60, 4, 'El código está debidamente indentado, usa notación camelCase para clases, métodos y variables, aplica estándares de nombres de paquetes y está documentado con comentarios.'),
    -- RAP5 · Descripción de la solución
    ('RAP5', 1, 'Bajo',          0.00, 1, 'No describe la solución, el alcance, el factor diferenciador ni los requerimientos.'),
    ('RAP5', 1, 'Básico',        0.36, 2, 'Describe la solución, sin alcance, factor diferenciador ni requerimientos.'),
    ('RAP5', 1, 'Alto',          0.48, 3, 'Describe la solución, el alcance y los requerimientos, sin factor diferenciador.'),
    ('RAP5', 1, 'Sobresaliente', 0.60, 4, 'Describe la solución, el alcance, el factor diferenciador y los requerimientos funcionales.'),
    -- RAP5 · Sustentación y/o nivel de dominio (calificación individual)
    ('RAP5', 2, 'Bajo',          0.00, 1, 'No realiza la sustentación.'),
    ('RAP5', 2, 'Básico',        0.48, 2, 'Demuestra dominio del problema propuesto.'),
    ('RAP5', 2, 'Alto',          0.64, 3, 'Demuestra dominio del problema propuesto y de la solución diseñada.'),
    ('RAP5', 2, 'Sobresaliente', 0.80, 4, 'Demuestra dominio del problema propuesto, de la solución diseñada y del código desarrollado.')
) AS v (outcome_code, criterion_order, name, score, display_order, description)
    ON v.outcome_code = lo.code AND v.criterion_order = c.display_order;
