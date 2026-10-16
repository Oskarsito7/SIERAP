package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.EvaluatorType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos del catálogo {@link EvaluatorType} (ADR-011).
 */
public interface EvaluatorTypeRepository extends JpaRepository<EvaluatorType, Long> {

    /**
     * Busca un tipo de evaluador por su nombre.
     *
     * @param name nombre visible del tipo
     * @return el tipo, si existe
     */
    Optional<EvaluatorType> findByName(String name);

    /**
     * Lista los tipos que se pueden asignar a evaluadores nuevos, en orden alfabético.
     *
     * @return tipos activos del catálogo
     */
    List<EvaluatorType> findByActiveTrueOrderByNameAsc();
}
