package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link AcademicPeriod#contains(LocalDate)}.
 */
class AcademicPeriodTest {

    private final AcademicPeriod period = new AcademicPeriod("2026-2",
            LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5), LocalDate.of(2026, 11, 28));

    @Test
    void containsADateInTheMiddle() {
        assertThat(period.contains(LocalDate.of(2026, 9, 24))).isTrue();
    }

    @Test
    void includesTheFirstAndTheLastDay() {
        assertThat(period.contains(LocalDate.of(2026, 8, 3))).isTrue();
        assertThat(period.contains(LocalDate.of(2026, 12, 5))).isTrue();
    }

    @Test
    void doesNotContainDatesOutsideThePeriod() {
        assertThat(period.contains(LocalDate.of(2026, 8, 2))).isFalse();
        assertThat(period.contains(LocalDate.of(2026, 12, 6))).isFalse();
    }
}
