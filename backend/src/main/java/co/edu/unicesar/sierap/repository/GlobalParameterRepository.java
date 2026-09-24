package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.GlobalParameter;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link GlobalParameter}.
 */
public interface GlobalParameterRepository extends JpaRepository<GlobalParameter, Long> {

    /**
     * Finds a parameter by its key.
     *
     * @param key unique name of the parameter
     * @return the parameter, if it exists
     */
    Optional<GlobalParameter> findByKey(String key);
}
