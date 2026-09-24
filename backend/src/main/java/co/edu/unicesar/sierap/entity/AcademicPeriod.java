package co.edu.unicesar.sierap.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Periodo académico: semestre (p. ej. {@code 2026-2}) con la fecha límite de
 * evaluación que fija la UPC.
 *
 * <p>La configuración de la evaluación se versiona por periodo (ADR-010): cada
 * {@link Moment} pertenece a un periodo, lo que permite comparar resultados entre
 * semestres y años (RF 12) y validar que la calificación termine a más tardar en
 * la fecha límite de la UPC (RF 9).</p>
 */
@Entity
@Table(name = "academic_period")
public class AcademicPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código del periodo, único en el sistema (p. ej. {@code 2026-2}). */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** Primer día del periodo. */
    @Column(nullable = false)
    private LocalDate startDate;

    /** Último día del periodo. */
    @Column(nullable = false)
    private LocalDate endDate;

    /** Último día en que se puede calificar en el periodo, según la UPC. */
    @Column(nullable = false)
    private LocalDate upcEvaluationDeadline;

    /** Constructor requerido por JPA. */
    protected AcademicPeriod() {
    }

    /**
     * Crea un periodo académico.
     *
     * @param code                  código del periodo, p. ej. {@code 2026-2}
     * @param startDate             primer día del periodo
     * @param endDate               último día del periodo
     * @param upcEvaluationDeadline último día para calificar, fijado por la UPC
     */
    public AcademicPeriod(String code, LocalDate startDate, LocalDate endDate,
                          LocalDate upcEvaluationDeadline) {
        this.code = code;
        this.startDate = startDate;
        this.endDate = endDate;
        this.upcEvaluationDeadline = upcEvaluationDeadline;
    }

    /**
     * Indica si una fecha cae dentro del periodo, incluyendo el primer y el último
     * día.
     *
     * @param date fecha a comprobar (la fecha actual se obtiene del {@code Clock}
     *             inyectado en el service, nunca aquí)
     * @return {@code true} si {@code startDate <= date <= endDate}
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /** @return identificador del periodo */
    public Long getId() {
        return id;
    }

    /** @return código del periodo */
    public String getCode() {
        return code;
    }

    /** @param code nuevo código del periodo */
    public void setCode(String code) {
        this.code = code;
    }

    /** @return primer día del periodo */
    public LocalDate getStartDate() {
        return startDate;
    }

    /** @param startDate nuevo primer día del periodo */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /** @return último día del periodo */
    public LocalDate getEndDate() {
        return endDate;
    }

    /** @param endDate nuevo último día del periodo */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /** @return último día para calificar, fijado por la UPC */
    public LocalDate getUpcEvaluationDeadline() {
        return upcEvaluationDeadline;
    }

    /** @param upcEvaluationDeadline nueva fecha límite de evaluación de la UPC */
    public void setUpcEvaluationDeadline(LocalDate upcEvaluationDeadline) {
        this.upcEvaluationDeadline = upcEvaluationDeadline;
    }
}
