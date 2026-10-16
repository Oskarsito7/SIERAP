package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.NewAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.request.UpdateAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.response.AcademicPeriodResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AcademicPeriodServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class AcademicPeriodServiceImplTest {

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private MomentRepository momentRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    private AcademicPeriodServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AcademicPeriodServiceImpl(academicPeriodRepository, momentRepository, clock);
    }

    @Test
    void listAllComputesStatusesCorrectly() {
        AcademicPeriod upcoming = new AcademicPeriod("2027-1",
                LocalDate.of(2027, 2, 1), LocalDate.of(2027, 6, 15), LocalDate.of(2027, 6, 10));
        ReflectionTestUtils.setField(upcoming, "id", 3L);

        AcademicPeriod current = new AcademicPeriod("2026-2",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 10));
        ReflectionTestUtils.setField(current, "id", 2L);

        AcademicPeriod closed = new AcademicPeriod("2026-1",
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 10));
        ReflectionTestUtils.setField(closed, "id", 1L);

        when(academicPeriodRepository.findAllByOrderByStartDateDesc())
                .thenReturn(List.of(upcoming, current, closed));

        List<AcademicPeriodResponse> result = service.listAll();

        assertThat(result).extracting(AcademicPeriodResponse::status)
                .containsExactly("UPCOMING", "CURRENT", "CLOSED");
    }

    @Test
    void createValidatesDatesAndUniqueCode() {
        assertThatThrownBy(() -> service.create(new NewAcademicPeriodRequest(
                "2027-1",
                LocalDate.of(2027, 6, 15),
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 6, 10))))
                .isInstanceOf(BusinessRuleException.class);

        assertThatThrownBy(() -> service.create(new NewAcademicPeriodRequest(
                "2027-1",
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 6, 15),
                LocalDate.of(2027, 6, 20))))
                .isInstanceOf(BusinessRuleException.class);

        when(academicPeriodRepository.existsByCode("2027-1")).thenReturn(false);
        when(academicPeriodRepository.save(any(AcademicPeriod.class))).thenAnswer(inv -> {
            AcademicPeriod p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 10L);
            return p;
        });

        AcademicPeriodResponse created = service.create(new NewAcademicPeriodRequest(
                "2027-1",
                LocalDate.of(2027, 2, 1),
                LocalDate.of(2027, 6, 15),
                LocalDate.of(2027, 6, 10)));
        assertThat(created.id()).isEqualTo(10L);
        assertThat(created.status()).isEqualTo("UPCOMING");
    }

    @Test
    void updateRejectsDeadlineBeforeMomentGradingEnd() {
        AcademicPeriod period = new AcademicPeriod("2026-2",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 10));
        ReflectionTestUtils.setField(period, "id", 2L);
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period));

        Moment moment = mock(Moment.class);
        when(moment.getGradingEnd()).thenReturn(LocalDate.of(2026, 11, 25));
        when(moment.getNumber()).thenReturn(1);
        when(momentRepository.findByPeriodId(2L)).thenReturn(List.of(moment));

        assertThatThrownBy(() -> service.update(2L, new UpdateAcademicPeriodRequest(
                "2026-2",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 12, 15),
                LocalDate.of(2026, 11, 20))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cierre de calificación");
    }

    @Test
    void deleteSuccessAndWithMomentsThrowsConflict() {
        AcademicPeriod period = new AcademicPeriod("2026-2",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 10));
        ReflectionTestUtils.setField(period, "id", 2L);
        when(academicPeriodRepository.findById(2L)).thenReturn(Optional.of(period));
        when(momentRepository.existsByPeriodId(2L)).thenReturn(false);

        service.delete(2L);
        verify(academicPeriodRepository).delete(period);

        when(momentRepository.existsByPeriodId(2L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(2L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
