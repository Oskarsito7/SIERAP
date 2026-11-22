package co.edu.unicesar.sierap.repository;

import java.util.List;
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

    /**
     * Lista todas las facultades ordenadas por nombre.
     *
     * @return facultades ordenadas alfabéticamente
     */
    List<Faculty> findAllByOrderByNameAsc();

    /**
     * Indica si ya existe una facultad con el código indicado.
     *
     * @param code código institucional
     * @return {@code true} si el código está en uso
     */
    boolean existsByCode(String code);

    /**
     * Indica si otra facultad distinta ya usa el código indicado.
     *
     * @param code código institucional
     * @param id   identificador de la facultad a excluir
     * @return {@code true} si otra facultad usa ese código
     */
    boolean existsByCodeAndIdNot(String code, Long id);
}
