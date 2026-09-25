package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Submission}.
 */
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * Historial de versiones de un grupo, de la más reciente a la más antigua.
     *
     * @param projectGroupId id del grupo
     * @return versiones entregadas
     */
    List<Submission> findByProjectGroupIdOrderByVersionNumberDesc(Long projectGroupId);

    /**
     * Última versión entregada por un grupo, sea a tiempo o tardía. La versión que
     * se califica la decide {@code ProjectGroup#getSubmissionToGrade()}.
     *
     * @param projectGroupId id del grupo
     * @return la entrega más reciente, si el grupo ya entregó
     */
    Optional<Submission> findFirstByProjectGroupIdOrderByVersionNumberDesc(Long projectGroupId);
}
