package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import co.edu.unicesar.sierap.config.MailOutboxProperties;
import co.edu.unicesar.sierap.entity.EmailOutbox;
import co.edu.unicesar.sierap.entity.enums.EmailStatus;
import co.edu.unicesar.sierap.repository.EmailOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

/**
 * Pruebas unitarias de {@link EmailOutboxDispatcher}: toma el lote de pendientes vencidos
 * y un fallo con un correo no frena a los demás.
 */
@ExtendWith(MockitoExtension.class)
class EmailOutboxDispatcherTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private EmailOutboxRepository outboxRepository;

    @Mock
    private EmailOutboxProcessor processor;

    private EmailOutboxDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new EmailOutboxDispatcher(outboxRepository, processor,
                new MailOutboxProperties(true, Duration.ofSeconds(15), 5, 20), CLOCK);
    }

    @Test
    void asksForTheDuePendingEmailsOfOneBatch() {
        when(outboxRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                any(), any(), any())).thenReturn(List.of());

        assertThat(dispatcher.dispatch()).isZero();

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(outboxRepository).findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                eq(EmailStatus.PENDING), eq(LocalDateTime.of(2026, 11, 22, 10, 0)), page.capture());
        assertThat(page.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void deliversEveryEmailOfTheBatch() {
        List<EmailOutbox> batch = List.of(email(1L), email(2L), email(3L));
        when(outboxRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                any(), any(), any())).thenReturn(batch);

        assertThat(dispatcher.dispatch()).isEqualTo(3);

        verify(processor).deliver(1L);
        verify(processor).deliver(2L);
        verify(processor).deliver(3L);
    }

    @Test
    void aFailureWithOneEmailDoesNotStopTheRestOfTheBatch() {
        List<EmailOutbox> batch = List.of(email(1L), email(2L), email(3L));
        when(outboxRepository.findByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAscIdAsc(
                any(), any(), any())).thenReturn(batch);
        lenient().doThrow(new IllegalStateException("fallo inesperado")).when(processor).deliver(2L);

        dispatcher.dispatch();

        verify(processor).deliver(1L);
        verify(processor).deliver(2L);
        verify(processor).deliver(3L);
    }

    private static EmailOutbox email(long id) {
        EmailOutbox email = mock(EmailOutbox.class);
        when(email.getId()).thenReturn(id);
        return email;
    }
}
