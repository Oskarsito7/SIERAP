package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * Estudiantes activos de un programa, con su semestre en el rango indicado, que no
     * tienen grupo en el momento.
     *
     * @param programId    programa del momento
     * @param semesterFrom primer semestre del momento
     * @param semesterTo   último semestre del momento
     * @param momentId     momento
     * @return estudiantes ordenados por apellidos y nombres
     */
    @Query("""
            select s from Student s
            where s.program.id = :programId
              and s.active = true
              and s.currentSemester between :semesterFrom and :semesterTo
              and not exists (
                  select 1 from ProjectGroup g join g.members m
                  where g.moment.id = :momentId and m.id = s.id)
            order by s.lastNames, s.firstNames
            """)
    List<Student> findWithoutGroup(@Param("programId") Long programId, @Param("semesterFrom") int semesterFrom,
                                   @Param("semesterTo") int semesterTo, @Param("momentId") Long momentId);
}
