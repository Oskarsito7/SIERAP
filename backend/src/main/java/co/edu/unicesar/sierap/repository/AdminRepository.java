package co.edu.unicesar.sierap.repository;

import java.util.List;

import co.edu.unicesar.sierap.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de {@link Admin}.
 */
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * Lista los administradores de una facultad.
     *
     * @param facultyId identificador de la facultad
     * @return administradores de la facultad
     */
    List<Admin> findByFacultyId(Long facultyId);

    /**
     * Indica si existen administradores adscritos a una facultad.
     *
     * @param facultyId identificador de la facultad
     * @return {@code true} si hay al menos un administrador en la facultad
     */
    boolean existsByFacultyId(Long facultyId);
}
