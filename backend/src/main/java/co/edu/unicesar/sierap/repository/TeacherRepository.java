package co.edu.unicesar.sierap.repository;

import java.util.List;

import co.edu.unicesar.sierap.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Teacher}.
 */
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    /**
     * Lista los docentes de una facultad.
     *
     * @param facultyId identificador de la facultad
     * @return docentes de la facultad
     */
    List<Teacher> findByFacultyId(Long facultyId);
}
