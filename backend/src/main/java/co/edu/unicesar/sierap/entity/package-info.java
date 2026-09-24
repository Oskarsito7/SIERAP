/**
 * Entity layer: domain classes mapped with JPA.
 *
 * <p>Mirrors the class diagram in {@code docs/diagramas/diagrama_clases.md}:
 * {@code User} (abstract, JOINED inheritance) and its five roles, the academic
 * structure and the assessment configuration (Faculty, Program, AcademicPeriod,
 * Moment, LearningOutcome, EvaluationCriterion, PerformanceLevel) and the
 * operational part (ProjectGroup, Submission, Evaluation, CriterionGrade). The
 * database schema is created by the Flyway migrations in
 * {@code src/main/resources/db/migration}.</p>
 */
package co.edu.unicesar.sierap.entity;
