package co.edu.unicesar.sierap.repository;

import java.util.Optional;

import co.edu.unicesar.sierap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de cualquier {@link User}, sin importar su rol (herencia JOINED).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su correo de inicio de sesión (se compara normalizado).
     *
     * @param email correo en minúsculas y sin espacios
     * @return el usuario, si existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Indica si ya existe un usuario con ese correo.
     *
     * @param email correo en minúsculas y sin espacios
     * @return {@code true} si el correo ya está registrado
     */
    boolean existsByEmail(String email);

    /**
     * Indica si ya existe un usuario con ese número de documento.
     *
     * @param documentNumber número de documento de identidad
     * @return {@code true} si el documento ya está registrado
     */
    boolean existsByDocumentNumber(String documentNumber);
}
