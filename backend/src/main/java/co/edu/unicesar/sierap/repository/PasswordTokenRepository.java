package co.edu.unicesar.sierap.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.PasswordToken;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de {@link PasswordToken} (enlaces enviados por correo, ADR-017).
 */
public interface PasswordTokenRepository extends JpaRepository<PasswordToken, Long> {

    /**
     * Busca un enlace por el hash de su valor.
     *
     * @param tokenHash hash SHA-256 del valor que llegó en el enlace
     * @return el enlace, si existe
     */
    Optional<PasswordToken> findByTokenHash(String tokenHash);

    /**
     * Invalida los enlaces pendientes de un usuario para un propósito, para que solo
     * sirva el último que se envió.
     *
     * @param userId  identificador del usuario
     * @param purpose propósito de los enlaces a invalidar
     * @param now     momento de la invalidación
     * @return cantidad de enlaces invalidados
     */
    @Modifying
    @Query("""
            UPDATE PasswordToken t SET t.usedAt = :now
            WHERE t.user.id = :userId AND t.purpose = :purpose AND t.usedAt IS NULL
            """)
    int invalidatePending(@Param("userId") Long userId, @Param("purpose") PasswordTokenPurpose purpose,
                          @Param("now") LocalDateTime now);
}
