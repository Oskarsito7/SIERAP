package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.Program;
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
}
