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

    private void issue(User user, PasswordTokenPurpose purpose, int validHours) {
        LocalDateTime now = LocalDateTime.now(clock);
        passwordTokenRepository.invalidatePending(user.getId(), purpose, now);
        String token = OpaqueTokens.newToken();
        passwordTokenRepository.save(new PasswordToken(user, OpaqueTokens.hash(token), purpose,
                now.plusHours(validHours), now));
        String link = properties.frontendUrl() + "/set-password?token=" + token;
        emailService.sendPasswordLink(user, purpose, link, validHours);
    }
}
