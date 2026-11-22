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

    /**
     * Indica si ya existe un tipo de evaluador con ese nombre (sin distinguir mayúsculas).
     *
     * @param name nombre visible
     * @return {@code true} si el nombre está en uso
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Indica si otro tipo de evaluador distinto ya usa ese nombre (sin distinguir mayúsculas).
     *
     * @param name nombre visible
     * @param id   identificador del tipo a excluir
     * @return {@code true} si otro tipo usa ese nombre
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
