package co.edu.unicesar.sierap.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de {@link RefreshToken} (sesiones, ADR-017).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca un token por el hash de su valor.
     *
     * @param tokenHash hash SHA-256 del valor que llegó en la cookie
     * @return el token, si existe
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Cierra todas las sesiones de un usuario borrando sus tokens (cambio de
     * contraseña, desactivación de la cuenta o token robado).
     *
     * @param userId identificador del usuario
     * @return cantidad de tokens borrados
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /**
     * Borra los tokens vencidos de un usuario, para que la tabla no crezca con cada
     * renovación.
     *
     * @param userId identificador del usuario
     * @param now    momento actual
     * @return cantidad de tokens borrados
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId AND t.expiresAt <= :now")
    int deleteExpiredByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
