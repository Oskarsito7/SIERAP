package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.Moment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Moment}.
 */
public interface MomentRepository extends JpaRepository<Moment, Long> {

    /**
     * Busca un momento por su clave natural: programa, periodo y número.
     *
     * @param programCode código del programa
     * @param periodCode  código del periodo académico
     * @param number      número del momento
     * @return el momento, si existe
     */
    Optional<Moment> findByProgramCodeAndPeriodCodeAndNumber(
            String programCode, String periodCode, int number);
}
