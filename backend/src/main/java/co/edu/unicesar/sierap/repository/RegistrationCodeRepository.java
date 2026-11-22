package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.RegistrationCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link RegistrationCode} (ADR-017).
 */
public interface RegistrationCodeRepository extends JpaRepository<RegistrationCode, Long> {

    /**
     * Busca un código por el texto que escribe el estudiante.
     *
     * @param code código de registro
     * @return el código, si existe
     */
    Optional<RegistrationCode> findByCode(String code);

    /**
     * Indica si un código ya está en uso (para generar uno nuevo sin repetir).
     *
     * @param code código candidato
     * @return {@code true} si ya existe
     */
    boolean existsByCode(String code);

    /**
     * Lista los códigos de un docente, del más reciente al más antiguo.
     *
     * @param teacherId identificador del docente
     * @return códigos del docente
     */
    List<RegistrationCode> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    /**
     * Lista los códigos de los docentes de una facultad, del más reciente al más antiguo.
     *
     * @param facultyId identificador de la facultad
     * @return códigos de la facultad
     */
    List<RegistrationCode> findByTeacherFacultyIdOrderByCreatedAtDesc(Long facultyId);

    /**
     * Lista una página de los códigos de un docente (ADR-024). El orden lo trae el
     * {@code Pageable}; con {@code Pageable.unpaged(sort)} devuelve todos.
     *
     * @param teacherId identificador del docente
     * @param pageable  página y orden pedidos
     * @return página de códigos del docente
     */
    Page<RegistrationCode> findByTeacherId(Long teacherId, Pageable pageable);

    /**
     * Lista una página de los códigos de los docentes de una facultad (ADR-024).
     *
     * @param facultyId identificador de la facultad
     * @param pageable  página y orden pedidos
     * @return página de códigos de la facultad
     */
    Page<RegistrationCode> findByTeacherFacultyId(Long facultyId, Pageable pageable);

    /**
     * Lista todos los códigos, del más reciente al más antiguo.
     *
     * @return todos los códigos
     */
    List<RegistrationCode> findAllByOrderByCreatedAtDesc();
}
