package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import co.edu.unicesar.sierap.entity.EmailOutbox;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.EmailStatus;
import co.edu.unicesar.sierap.entity.enums.PasswordTokenPurpose;
import co.edu.unicesar.sierap.repository.EmailOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de {@link OutboxEmailService}: encolar inserta una fila pendiente,
 * lista para enviarse, con el contenido pedido.
 */
@ExtendWith(MockitoExtension.class)
class OutboxEmailServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private EmailOutboxRepository outboxRepository;

    @Test
    void sendInsertsAPendingRowReadyToBeDispatched() {
        new OutboxEmailService(outboxRepository, CLOCK).send("a@unicesar.edu.co", "Asunto", "Cuerpo");

        EmailOutbox saved = savedRow();
        assertThat(saved.getRecipient()).isEqualTo("a@unicesar.edu.co");
        assertThat(saved.getSubject()).isEqualTo("Asunto");
        assertThat(saved.getBody()).isEqualTo("Cuerpo");
        assertThat(saved.getStatus()).isEqualTo(EmailStatus.PENDING);
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getNextAttemptAt()).isEqualTo(LocalDateTime.of(2026, 11, 22, 10, 0));
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getNextAttemptAt());
    }

    @Test
    void passwordLinkIsEnqueuedWithTheLinkAndTheValidity() {
        Teacher user = new Teacher("Ana", "Docente", DocumentType.CC, "1001", "ana@unicesar.edu.co", null,
                LocalDateTime.now(CLOCK), null);

        new OutboxEmailService(outboxRepository, CLOCK).sendPasswordLink(user, PasswordTokenPurpose.ACCOUNT_SETUP,
                "http://localhost:5173/set-password?token=abc", 72);

        EmailOutbox saved = savedRow();
        assertThat(saved.getRecipient()).isEqualTo("ana@unicesar.edu.co");
        assertThat(saved.getSubject()).isEqualTo("SIERAP: crea tu contraseña");
        assertThat(saved.getBody()).contains("Hola, Ana", "72 horas", "set-password?token=abc");
    }

    private EmailOutbox savedRow() {
        ArgumentCaptor<EmailOutbox> captor = ArgumentCaptor.forClass(EmailOutbox.class);
        verify(outboxRepository).save(captor.capture());
        return captor.getValue();
    }
}
