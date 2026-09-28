-- =============================================================================
-- V5 · Restricciones CHECK del modelo académico (Etapa 1, D2)
-- Correcciones de la revisión del PR #3: la base de datos aceptaba momentos con
-- rangos o ventanas invertidas, puntajes negativos y periodos con fechas
-- incoherentes. Los services también validan estas reglas; los CHECK son una
-- defensa más en la base de datos. V1 ya está en develop, por eso van aquí.
--
-- Las ventanas del momento pueden estar vacías mientras se configura (ADR-012):
-- un CHECK con una fecha NULL no falla, así que solo se aplica cuando ambas fechas
-- existen. La regla de que la calificación termine a más tardar en el límite de la
-- UPC compara dos tablas y la valida Moment.launch().
-- =============================================================================

ALTER TABLE academic_period
    ADD CONSTRAINT ck_academic_period_dates
        CHECK (start_date <= end_date),
    ADD CONSTRAINT ck_academic_period_upc_deadline
        CHECK (upc_evaluation_deadline BETWEEN start_date AND end_date);

ALTER TABLE moment
    ADD CONSTRAINT ck_moment_number
        CHECK (number >= 1),
    ADD CONSTRAINT ck_moment_semesters
        CHECK (semester_from >= 1 AND semester_from <= semester_to),
    ADD CONSTRAINT ck_moment_members
        CHECK (min_members >= 1 AND min_members <= max_members),
    ADD CONSTRAINT ck_moment_group_formation_window
        CHECK (group_formation_start <= group_formation_end),
    ADD CONSTRAINT ck_moment_submission_window
        CHECK (submission_start <= submission_end),
    ADD CONSTRAINT ck_moment_grading_window
        CHECK (grading_start <= grading_end),
    -- Las ventanas van en orden y sin solaparse: conformación, entregas, calificación.
    ADD CONSTRAINT ck_moment_submission_after_group_formation
        CHECK (group_formation_end < submission_start),
    ADD CONSTRAINT ck_moment_grading_after_submission
        CHECK (submission_end < grading_start);

ALTER TABLE performance_level
    ADD CONSTRAINT ck_performance_level_score
        CHECK (score >= 0);
