package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.CriterionGrade;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link co.edu.unicesar.sierap.entity.CriterionGrade}. Las
 * calificaciones se guardan en cascada con su {@code Evaluation}; este repository
 * queda para consultas de reportes.
 */
public interface CriterionGradeRepository extends JpaRepository<CriterionGrade, Long> {
}
