/**
 * Capa de entidades: clases del dominio mapeadas con JPA.
 *
 * <p>Refleja el diagrama de clases de {@code docs/diagramas/diagrama_clases.md}:
 * {@code Usuario} (abstracta) y sus seis roles, la estructura académica
 * (Facultad, Programa, Momento, RAP) y la operativa (Grupo, ProyectoAula, Entrega,
 * Evaluacion, CalificacionRAP). El esquema de la base de datos lo crean las
 * migraciones de Flyway en {@code src/main/resources/db/migration}.</p>
 */
package co.edu.unicesar.sierap.entity;
