package co.edu.unicesar.sierap.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Academic semester (e.g. {@code 2026-2}) with the evaluation deadline set by the
 * UPC.
 *
 * <p>The assessment configuration is versioned per period (ADR-010): each
 * {@link Moment} belongs to a period, which allows comparing results across
 * semesters and years (RF 12) and checking that grading ends no later than the UPC
 * deadline (RF 9).</p>
 */
@Entity
@Table(name = "academic_period")
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Code of the period, unique in the system (e.g. {@code 2026-2}). */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** First day of the period. */
    @Column(nullable = false)
    private LocalDate startDate;

    /** Last day of the period. */
    @Column(nullable = false)
    private LocalDate endDate;

    /** Last day grading is allowed in the period, according to the UPC. */
    @Column(nullable = false)
    private LocalDate upcEvaluationDeadline;

    /** Constructor required by JPA. */
    protected AcademicPeriod() {
    }

    /**
     * Creates an academic period.
     *
     * @param code                  code of the period, e.g. {@code 2026-2}
     * @param startDate             first day of the period
     * @param endDate               last day of the period
     * @param upcEvaluationDeadline last day to grade, set by the UPC
     */
    public AcademicPeriod(String code, LocalDate startDate, LocalDate endDate,
                          LocalDate upcEvaluationDeadline) {
        this.code = code;
        this.startDate = startDate;
        this.endDate = endDate;
        this.upcEvaluationDeadline = upcEvaluationDeadline;
    }

    /**
     * Tells whether a date falls inside the period, first and last day included.
     *
     * @param date date to check (the current date comes from the {@code Clock}
     *             injected in the service, never from here)
     * @return {@code true} if {@code startDate <= date <= endDate}
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** @return identifier of the period */
    public Long getId() {
        return id;
    }

    /** @return code of the period */
    public String getCode() {
        return code;
    }

    /** @param code new code of the period */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return first day of the period */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** @param startDate new first day of the period */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /** @return last day of the period */
    public LocalDate getEndDate() {
        return endDate;
    }

    /** @param endDate new last day of the period */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /** @return last day to grade, set by the UPC */
    public LocalDate getUpcEvaluationDeadline() {
        return upcEvaluationDeadline;
    }

    /** @param upcEvaluationDeadline new UPC evaluation deadline */
    public void setUpcEvaluationDeadline(LocalDate upcEvaluationDeadline) {
        this.upcEvaluationDeadline = upcEvaluationDeadline;
    }
}
