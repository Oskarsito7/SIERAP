package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.AcademicPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link AcademicPeriod}.
 */
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriod, Long> {

    /**
     * Finds a period by its code, e.g. {@code 2026-2}.
     *
     * @param code code of the period
     * @return the period, if it exists
     */
    Optional<AcademicPeriod> findByCode(String code);
}
