package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Student}.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Busca un estudiante por su código estudiantil.
     *
     * @param studentCode código estudiantil
     * @return el estudiante, si existe
     */
    Optional<Student> findByStudentCode(String studentCode);

    /**
     * Lista los estudiantes de un programa.
     *
     * @param programId identificador del programa
     * @return estudiantes del programa
     */
    List<Student> findByProgramId(Long programId);

    /**
     * Indica si ya existe un estudiante con ese código estudiantil.
     *
     * @param studentCode código estudiantil
     * @return {@code true} si el código ya está registrado
     */
    boolean existsByStudentCode(String studentCode);

    /**
     * Cuenta los estudiantes que se registraron con un código.
     *
     * @param registrationCodeId identificador del código de registro
     * @return cantidad de estudiantes registrados con ese código
     */
    long countByRegistrationCodeId(Long registrationCodeId);
}
