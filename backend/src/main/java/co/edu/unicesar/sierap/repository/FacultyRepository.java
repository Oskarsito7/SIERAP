package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Faculty}.
 */
public interface FacultyRepository extends JpaRepository<Faculty, Long> {
}
