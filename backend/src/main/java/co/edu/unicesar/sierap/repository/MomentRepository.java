package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.Moment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Moment}.
 */
public interface MomentRepository extends JpaRepository<Moment, Long> {

    /**
     * Finds a moment by its natural key: program, period and number.
     *
     * @param programCode code of the program
     * @param periodCode  code of the academic period
     * @param number      number of the moment
     * @return the moment, if it exists
     */
    Optional<Moment> findByProgramCodeAndPeriodCodeAndNumber(
            String programCode, String periodCode, int number);
}
