package co.edu.unicesar.sierap.repository;

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
}
