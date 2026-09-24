package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.GlobalParameter;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link GlobalParameter}.
 */
public interface GlobalParameterRepository extends JpaRepository<GlobalParameter, Long> {

    /**
     * Busca un parámetro por su clave.
     *
     * @param key nombre único del parámetro
     * @return el parámetro, si existe
     */
    Optional<GlobalParameter> findByKey(String key);
}
