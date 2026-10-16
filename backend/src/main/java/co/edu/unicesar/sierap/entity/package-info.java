/**
 * Capa de entidades: clases del dominio mapeadas con JPA.
 *
 * <p>Refleja el diagrama de clases de {@code docs/diagramas/diagrama_clases.md}:
 * {@code User} (abstracta, herencia JOINED) y sus cinco roles, la estructura
 * académica y la configuración de la evaluación (Faculty, Program,
 * AcademicPeriod, Moment, LearningOutcome, EvaluationCriterion, PerformanceLevel)
 * y la operativa (ProjectGroup, Submission, Evaluation, CriterionGrade). El
 * esquema de la base de datos lo crean las migraciones de Flyway en
 * {@code src/main/resources/db/migration}.</p>
 */
package co.edu.unicesar.sierap.entity;
