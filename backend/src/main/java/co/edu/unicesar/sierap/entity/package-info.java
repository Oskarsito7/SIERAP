/**
 * Capa de entidades: clases del dominio mapeadas con JPA.
 *
 * <p>Refleja el diagrama de clases de {@code docs/diagramas/diagrama_clases.md}:
 * {@code Usuario} (abstracta, herencia JOINED) y sus cinco roles, la estructura
 * académica y la configuración de la evaluación (Facultad, Programa,
 * PeriodoAcademico, Momento, RAP, CriterioEvaluacion, NivelDesempeno) y la
 * operativa (Grupo, Entrega, Evaluacion, CalificacionCriterio). El esquema de la
 * base de datos lo crean las migraciones de Flyway en
 * {@code src/main/resources/db/migration}.</p>
 */
package co.edu.unicesar.sierap.entity;
