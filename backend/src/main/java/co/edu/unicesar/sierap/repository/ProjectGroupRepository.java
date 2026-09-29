package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.ProjectGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de {@link ProjectGroup}.
 *
 * <p>Cada consulta corresponde a una regla de alcance por rol: el estudiante ve los
 * grupos de su momento, el docente solo los suyos y el evaluador externo solo los
 * que se le asignaron. Ningún service debe usar {@code findAll()} para listar
 * grupos a un usuario.</p>
 */
public interface ProjectGroupRepository extends JpaRepository<ProjectGroup, Long> {

    /**
     * Grupos habilitados en un momento, de todos los docentes (lo que ve el
     * estudiante durante la conformación).
     *
     * @param momentId id del momento
     * @return grupos del momento ordenados por nombre
     */
    List<ProjectGroup> findByMomentIdOrderByName(Long momentId);

    /**
     * Grupos que habilitó un docente en un momento.
     *
     * @param teacherId id del docente
     * @param momentId  id del momento
     * @return grupos del docente en ese momento, ordenados por nombre
     */
    List<ProjectGroup> findByTeacherIdAndMomentIdOrderByName(Long teacherId, Long momentId);

    /**
     * Grupos asignados a un evaluador externo.
     *
     * @param externalEvaluatorId id del evaluador externo
     * @return grupos que debe calificar
     */
    List<ProjectGroup> findByEvaluatorsId(Long externalEvaluatorId);

    /**
     * Grupo de un estudiante en un momento (tiene como máximo uno).
     *
     * @param momentId  id del momento
     * @param studentId id del estudiante
     * @return el grupo, si ya se inscribió en alguno
     */
    Optional<ProjectGroup> findByMomentIdAndMembersId(Long momentId, Long studentId);

    /**
     * Indica si un estudiante ya está inscrito en algún grupo del momento; el
     * service lo consulta antes de inscribirlo (un grupo por momento).
     *
     * @param momentId  id del momento
     * @param studentId id del estudiante
     * @return {@code true} si ya tiene grupo en ese momento
     */
    boolean existsByMomentIdAndMembersId(Long momentId, Long studentId);

    /**
     * Historial de grupos de un estudiante en todos los momentos.
     *
     * @param studentId id del estudiante
     * @return grupos en los que ha estado
     */
    List<ProjectGroup> findByMembersId(Long studentId);

    /**
     * Grupos que habilitó un docente en todos sus momentos.
     *
     * @param teacherId id del docente
     * @return grupos ordenados por nombre
     */
    @EntityGraph(attributePaths = {"moment", "teacher"})
    List<ProjectGroup> findByTeacherIdOrderByName(Long teacherId);

    /**
     * Grupos asignados a un evaluador externo, ordenados por nombre.
     *
     * @param externalEvaluatorId id del evaluador externo
     * @return grupos que debe calificar
     */
    @EntityGraph(attributePaths = {"moment", "teacher"})
    List<ProjectGroup> findByEvaluatorsIdOrderByName(Long externalEvaluatorId);

    /**
     * Grupos asignados a un evaluador externo en un momento.
     *
     * @param externalEvaluatorId id del evaluador externo
     * @param momentId            id del momento
     * @return grupos ordenados por nombre
     */
    @EntityGraph(attributePaths = {"moment", "teacher"})
    List<ProjectGroup> findByEvaluatorsIdAndMomentIdOrderByName(Long externalEvaluatorId, Long momentId);

    /**
     * Indica si un docente ya tiene un grupo con ese nombre en el momento (la base de
     * datos lo impide con {@code uk_project_group_moment_teacher_name}).
     *
     * @param momentId  id del momento
     * @param teacherId id del docente
     * @param name      nombre del grupo
     * @return {@code true} si el nombre ya está en uso
     */
    boolean existsByMomentIdAndTeacherIdAndNameIgnoreCase(Long momentId, Long teacherId, String name);

    /**
     * Carga un grupo bloqueando su fila hasta que termine la transacción. Se usa al
     * cambiar sus integrantes, para que dos inscripciones simultáneas no pasen a la vez
     * la validación del cupo.
     *
     * @param id id del grupo
     * @return el grupo, si existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from ProjectGroup g where g.id = :id")
    Optional<ProjectGroup> findByIdForUpdate(@Param("id") Long id);
}
