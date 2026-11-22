package co.edu.unicesar.sierap.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link AcademicPeriod}.
 */
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {

    /**
     * Busca un periodo por su código, p. ej. {@code 2026-2}.
     *
     * @param code código del periodo
     * @return el periodo, si existe
     */
    Optional<AcademicPeriod> findByCode(String code);

    /**
     * Busca el periodo cuyas fechas incluyen el día indicado (el periodo actual). Si
     * hubiera dos, devuelve el que empezó más tarde.
     *
     * @param onStart el día, comparado con la fecha de inicio
     * @param onEnd   el mismo día, comparado con la fecha de fin
     * @return el periodo, si hay uno en curso ese día
     */
    Optional<AcademicPeriod> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
            LocalDate onStart, LocalDate onEnd);

    /**
     * Lista todos los periodos del más reciente al más antiguo.
     *
     * @return periodos ordenados por fecha de inicio descendente
     */
    List<AcademicPeriod> findAllByOrderByStartDateDesc();

    /**
     * Indica si ya existe un periodo con el código indicado.
     *
     * @param code código del periodo
     * @return {@code true} si el código está en uso
     */
    boolean existsByCode(String code);

    /**
     * Indica si otro periodo distinto ya usa el código indicado.
     *
     * @param code código del periodo
     * @param id   identificador del periodo a excluir
     * @return {@code true} si otro periodo usa ese código
     */
    boolean existsByCodeAndIdNot(String code, Long id);
}
