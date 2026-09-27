package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.RegistrationCode;
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
     * Lista todos los códigos, del más reciente al más antiguo.
     *
     * @return todos los códigos
     */
    List<RegistrationCode> findAllByOrderByCreatedAtDesc();
}
