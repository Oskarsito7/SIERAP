package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.PerformanceLevel;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link PerformanceLevel}.
 */
public interface PerformanceLevelRepository extends JpaRepository<PerformanceLevel, Long> {
}
