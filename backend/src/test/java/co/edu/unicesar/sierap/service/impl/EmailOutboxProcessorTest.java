package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import co.edu.unicesar.sierap.config.MailOutboxProperties;
import co.edu.unicesar.sierap.entity.EmailOutbox;
import co.edu.unicesar.sierap.entity.enums.EmailStatus;
import co.edu.unicesar.sierap.repository.EmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

/**
 * Pruebas unitarias de {@link EmailOutboxProcessor}: éxito, reintento con espera
 * exponencial y paso a {@code FAILED} al agotar los intentos.
 */
@ExtendWith(MockitoExtension.class)
class EmailOutboxProcessorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 22, 10, 0);

    @Mock
    private EmailOutboxRepository outboxRepository;

    @Mock
    private SmtpMailSender mailSender;

    private EmailOutboxProcessor processor;
    private EmailOutbox email;

    @BeforeEach
    void setUp() {
        processor = new EmailOutboxProcessor(outboxRepository, mailSender,
                new MailOutboxProperties(true, Duration.ofSeconds(15), 5, 20), CLOCK);
        email = new EmailOutbox("a@unicesar.edu.co", "Asunto", "Cuerpo", NOW.minusMinutes(5));
        when(outboxRepository.findById(1L)).thenReturn(Optional.of(email));
    }

    @Test
    void successMarksTheEmailAsSent() {
        processor.deliver(1L);

        verify(mailSender).send("a@unicesar.edu.co", "Asunto", "Cuerpo");
        assertThat(email.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(email.getSentAt()).isEqualTo(NOW);
        assertThat(email.getAttempts()).isZero();
        // El cuerpo puede llevar un enlace con token de un solo uso: no se conserva.
        assertThat(email.getBody()).isEqualTo(EmailOutbox.REDACTED_BODY);
        verify(outboxRepository).save(email);
    }

    @Test
    void failureCountsTheAttemptAndSchedulesARetryAfterOneMinute() {
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(anyString(), anyString(), anyString());

        processor.deliver(1L);

        assertThat(email.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(email.getAttempts()).isEqualTo(1);
        assertThat(email.getLastError()).contains("SMTP caído");
        assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(1));
        assertThat(email.getSentAt()).isNull();
        // Mientras quede un reintento, el cuerpo se conserva para poder enviarlo.
        assertThat(email.getBody()).isNotEqualTo(EmailOutbox.REDACTED_BODY);
    }

    @Test
    void theWaitDoublesWithEachFailedAttempt() {
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(anyString(), anyString(), anyString());

        processor.deliver(1L);
        assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(1));
        processor.deliver(1L);
        assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(2));
        processor.deliver(1L);
        assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(4));
        processor.deliver(1L);
        assertThat(email.getNextAttemptAt()).isEqualTo(NOW.plusMinutes(8));
        assertThat(email.getStatus()).isEqualTo(EmailStatus.PENDING);
    }

    @Test
    void reachingTheMaximumAttemptsMovesTheEmailToFailed() {
        doThrow(new MailSendException("SMTP caído")).when(mailSender).send(anyString(), anyString(), anyString());

        for (int i = 0; i < 5; i++) {
            processor.deliver(1L);
        }

        assertThat(email.getAttempts()).isEqualTo(5);
        assertThat(email.getStatus()).isEqualTo(EmailStatus.FAILED);
        assertThat(email.getBody()).isEqualTo(EmailOutbox.REDACTED_BODY);
        // Ya no se reintenta: un sexto intento no toca el servidor SMTP.
        processor.deliver(1L);
        verify(mailSender, org.mockito.Mockito.times(5)).send(anyString(), anyString(), anyString());
    }

    @Test
    void anEmailThatIsNoLongerPendingIsSkipped() {
        email.markSent(NOW);

        processor.deliver(1L);

        verify(mailSender, never()).send(anyString(), anyString(), anyString());
        verify(outboxRepository, never()).save(email);
    }

    @Test
    void aVeryLongErrorMessageIsTruncatedToFitTheColumn() {
        doThrow(new MailSendException("x".repeat(3000))).when(mailSender).send(anyString(), anyString(), anyString());

        processor.deliver(1L);

        assertThat(email.getLastError()).hasSizeLessThanOrEqualTo(1000);
    }
}
