package co.edu.unicesar.sierap.entity;

/**
 * Quien puede calificar un {@link ProjectGroup}: hoy el {@link Teacher} que habilitó
 * el grupo y los {@link ExternalEvaluator} designados para él.
 *
 * <p>"Evaluar" es un comportamiento que atraviesa roles distintos que no comparten
 * un padre propio (los dos heredan de {@link User}), por eso es una interfaz. Cada
 * clase implementa su propia regla de {@link #canEvaluate(ProjectGroup)}
 * (polimorfismo), y el {@code Student} no la implementa: en SIERAP no hay
 * autoevaluación.</p>
 *
 * <p>Las fechas no son parte de la interfaz: la ventana de calificación y la fecha
 * de evaluación del grupo las valida el service con el {@code Clock} inyectado.</p>
 */
public interface Evaluator {

    /**
     * Indica si este evaluador puede calificar el grupo, sin considerar fechas.
     *
     * @param group grupo que se quiere calificar
     * @return {@code true} si el grupo le corresponde a este evaluador
     */
    boolean canEvaluate(ProjectGroup group);
}
