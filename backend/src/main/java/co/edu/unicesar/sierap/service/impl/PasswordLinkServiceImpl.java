package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.PasswordToken;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.repository.PasswordTokenRepository;
import co.edu.unicesar.sierap.security.OpaqueTokens;
import co.edu.unicesar.sierap.service.EmailService;
import co.edu.unicesar.sierap.service.PasswordLinkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Implementación de {@link PasswordLinkService}.
 *
 * <p>Cada enlace lleva un token aleatorio de un solo uso; en la base solo queda su
 * hash. El enlace apunta a la página {@code /set-password} del frontend.</p>
 */
@Service
@Transactional
public class PasswordLinkServiceImpl implements PasswordLinkService {

    private final PasswordTokenRepository passwordTokenRepository;
    private final EmailService emailService;
    private final AuthProperties properties;
    private final Clock clock;

    /**
     * Crea el servicio.
     *
     * @param passwordTokenRepository repositorio de enlaces
     * @param emailService            envío de correos
     * @param properties              parámetros de autenticación (vigencias, URL del frontend)
     * @param clock                   reloj de la aplicación
     */
    public PasswordLinkServiceImpl(PasswordTokenRepository passwordTokenRepository, EmailService emailService,
                                   AuthProperties properties, Clock clock) {
        this.passwordTokenRepository = passwordTokenRepository;
        this.emailService = emailService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void sendAccountSetupLink(User user) {
        issue(user, PasswordTokenPurpose.ACCOUNT_SETUP, properties.setupLinkHours());
    }

    @Override
    public void sendPasswordResetLink(User user) {
        // Pedir la recuperación varias veces seguidas no inunda el correo de la cuenta:
        // mientras el último enlace siga pendiente y sea reciente, no se envía otro.
        LocalDateTime since = LocalDateTime.now(clock).minusMinutes(properties.resetLinkCooldownMinutes());
        if (passwordTokenRepository.existsByUserIdAndPurposeAndUsedAtIsNullAndCreatedAtAfter(user.getId(),
                PasswordTokenPurpose.PASSWORD_RESET, since)) {
            return;
        }
        issue(user, PasswordTokenPurpose.PASSWORD_RESET, properties.resetLinkHours());
    }

    @Override
    public User consume(String token) {
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordToken passwordToken = passwordTokenRepository.findByTokenHash(OpaqueTokens.hash(token))
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(() -> new BusinessRuleException(
                        "El enlace no es válido o ya venció. Solicite uno nuevo."));
        passwordToken.markUsed(now);
        return passwordToken.getUser();
    }

    @Override
    public void invalidateAllLinks(User user) {
        passwordTokenRepository.invalidateAllPending(user.getId(), LocalDateTime.now(clock));
    }

    private void issue(User user, PasswordTokenPurpose purpose, int validHours) {
        LocalDateTime now = LocalDateTime.now(clock);
        passwordTokenRepository.invalidatePending(user.getId(), purpose, now);
        String token = OpaqueTokens.newToken();
        passwordTokenRepository.save(new PasswordToken(user, OpaqueTokens.hash(token), purpose,
                now.plusHours(validHours), now));
        String link = properties.frontendUrl() + "/set-password?token=" + token;
        afterCommit(() -> emailService.sendPasswordLink(user, purpose, link, validHours));
    }

    /**
     * Ejecuta la acción cuando la transacción actual se confirma (o de inmediato si no
     * hay transacción). El correo se envía así fuera de la transacción: la conexión a la
     * base de datos no queda retenida mientras responde el servidor SMTP, y no se envía
     * un enlace que luego se deshaga.
     */
    private static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
