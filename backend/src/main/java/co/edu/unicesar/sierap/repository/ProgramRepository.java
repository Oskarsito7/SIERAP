package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Program}.
 */
public interface ProgramRepository extends JpaRepository<Program, Long> {

    /**
     * Finds a program by its institutional code.
     *
     * @param code code of the program
     * @return the program, if it exists
     */
    Optional<Program> findByCode(String code);
}
