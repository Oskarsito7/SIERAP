package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.Program;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Program}.
 */
public interface ProgramRepository extends JpaRepository<Program, Long> {

    /**
     * Busca un programa por su código institucional.
     *
     * @param code código del programa
     * @return el programa, si existe
     */
    Optional<Program> findByCode(String code);

    /**
     * Lista todos los programas ordenados por nombre.
     *
     * @return programas ordenados alfabéticamente
     */
    @EntityGraph(attributePaths = "faculty")
    List<Program> findAllByOrderByNameAsc();

    /**
     * Lista los programas de una facultad ordenados por nombre.
     *
     * @param facultyId identificador de la facultad
     * @return programas de la facultad ordenados alfabéticamente
     */
    @EntityGraph(attributePaths = "faculty")
    List<Program> findByFacultyIdOrderByNameAsc(Long facultyId);

    /**
     * Indica si ya existe un programa con el código indicado.
     *
     * @param code código institucional
     * @return {@code true} si el código está en uso
     */
    boolean existsByCode(String code);

    /**
     * Indica si otro programa distinto ya usa el código indicado.
     *
     * @param code código institucional
     * @param id   identificador del programa a excluir
     * @return {@code true} si otro programa usa ese código
     */
    boolean existsByCodeAndIdNot(String code, Long id);
}
