package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Faculty}.
 */
public interface FacultyRepository extends JpaRepository<Faculty, Long> {

    /**
     * Busca una facultad por su código institucional.
     *
     * @param code código de la facultad
     * @return la facultad, si existe
     */
    Optional<Faculty> findByCode(String code);
}
