package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.Moment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de {@link Moment}.
 *
 * <p>Las consultas de listado traen el programa y el periodo en la misma consulta
 * ({@link EntityGraph}), porque el resumen del momento muestra sus nombres.</p>
 */
public interface MomentRepository extends JpaRepository<Moment, Long> {

    /**
     * Busca un momento por su clave natural: programa, periodo y número.
     *
     * @param programCode código del programa
     * @param periodCode  código del periodo académico
     * @param number      número del momento
     * @return el momento, si existe
     */
    Optional<Moment> findByProgramCodeAndPeriodCodeAndNumber(
            String programCode, String periodCode, int number);

    /**
     * Momentos de un periodo en todos los programas (super_admin).
     *
     * @param periodId periodo académico
     * @return momentos ordenados por programa y número
     */
    @EntityGraph(attributePaths = {"program", "period"})
    List<Moment> findByPeriodIdOrderByProgramNameAscNumberAsc(Long periodId);

    /**
     * Momentos de un periodo en los programas de una facultad (admin y docente).
     *
     * @param periodId  periodo académico
     * @param facultyId facultad de los programas
     * @return momentos ordenados por programa y número
     */
    @EntityGraph(attributePaths = {"program", "period"})
    List<Moment> findByPeriodIdAndProgramFacultyIdOrderByProgramNameAscNumberAsc(Long periodId, Long facultyId);

    /**
     * Momentos de un periodo en un programa específico ordenados por número.
     *
     * @param periodId  periodo académico
     * @param programId programa académico
     * @return momentos del programa en ese periodo ordenados por número
     */
    @EntityGraph(attributePaths = {"program", "period"})
    List<Moment> findByPeriodIdAndProgramIdOrderByNumberAsc(Long periodId, Long programId);

    /**
     * Momentos de un periodo en un programa cuyo rango de semestres incluye el
     * semestre indicado (estudiante), o momentos en los que el estudiante ya tiene un
     * grupo (incluso si su semestre está fuera del rango).
     *
     * @param periodId  periodo académico
     * @param programId programa del estudiante
     * @param semester  semestre actual del estudiante
     * @param studentId id del estudiante
     * @return momentos ordenados por número
     */
    @EntityGraph(attributePaths = {"program", "period"})
    @Query("""
            SELECT m FROM Moment m
            WHERE m.period.id = :periodId
              AND ((m.program.id = :programId AND :semester BETWEEN m.semesterFrom AND m.semesterTo)
                   OR EXISTS (
                       SELECT 1 FROM ProjectGroup g
                       JOIN g.members mem
                       WHERE g.moment = m AND mem.id = :studentId
                   ))
            ORDER BY m.number""")
    List<Moment> findForStudent(@Param("periodId") Long periodId, @Param("programId") Long programId,
                                @Param("semester") int semester, @Param("studentId") Long studentId);

    /**
     * Indica si ya existe un momento con ese número en el programa y periodo.
     *
     * @param programId id del programa
     * @param periodId  id del periodo académico
     * @param number    número del momento
     * @return {@code true} si ya existe
     */
    boolean existsByProgramIdAndPeriodIdAndNumber(Long programId, Long periodId, int number);

    /**
     * Indica si ya existe otro momento diferente con ese número en el programa y periodo.
     *
     * @param programId id del programa
     * @param periodId  id del periodo académico
     * @param number    número del momento
     * @param id        id del momento a excluir
     * @return {@code true} si otro momento ya tiene ese número
     */
    boolean existsByProgramIdAndPeriodIdAndNumberAndIdNot(Long programId, Long periodId, int number, Long id);
}
