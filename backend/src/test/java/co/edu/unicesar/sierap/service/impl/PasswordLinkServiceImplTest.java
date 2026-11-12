package co.edu.unicesar.sierap.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import co.edu.unicesar.sierap.repository.PasswordTokenRepository;
import co.edu.unicesar.sierap.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Pruebas unitarias de {@link PasswordLinkServiceImpl} (auditoría de seguridad, run-1):
 * el correo se envía solo cuando la transacción se confirma, y una recuperación pedida
 * de nuevo mientras el último enlace sigue pendiente y es reciente no envía otro.
 */
@ExtendWith(MockitoExtension.class)
class PasswordLinkServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private PasswordTokenRepository passwordTokenRepository;

    @Mock
    private EmailService emailService;

    private PasswordLinkServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties("una-clave-de-pruebas-de-al-menos-32-bytes", 15, 7, false,
                72, 1, 5, "http://localhost:5173", "unicesar.edu.co", "SIERAP <no-reply@unicesar.edu.co>");
        service = new PasswordLinkServiceImpl(passwordTokenRepository, emailService, properties, CLOCK);
        user = new Teacher("Docente", "Prueba", DocumentType.CC, "1001", "docente@unicesar.edu.co", null,
                LocalDateTime.now(CLOCK), null);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void theEmailIsSentOnlyAfterTheTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();

        service.sendAccountSetupLink(user);

        verify(emailService, never()).sendPasswordLink(any(), any(), anyString(), anyInt());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(emailService).sendPasswordLink(eq(user), eq(PasswordTokenPurpose.ACCOUNT_SETUP), anyString(), eq(72));
    }

    @Test
    void aRecentPendingResetLinkIsNotSentAgain() {
        when(passwordTokenRepository.existsByUserIdAndPurposeAndUsedAtIsNullAndCreatedAtAfter(
                user.getId(), PasswordTokenPurpose.PASSWORD_RESET, LocalDateTime.now(CLOCK).minusMinutes(5)))
                .thenReturn(true);

        service.sendPasswordResetLink(user);

        verify(passwordTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordLink(any(), any(), anyString(), anyInt());
    }
}
