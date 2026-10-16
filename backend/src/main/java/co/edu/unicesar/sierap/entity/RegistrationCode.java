package co.edu.unicesar.sierap.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Código de registro que un docente comparte con sus estudiantes para que creen su
 * cuenta solos (ADR-017).
 *
 * <p>Queda ligado al docente y a un programa de su facultad: quien se registra con él
 * queda en ese programa y aparece en la lista de estudiantes del docente. Sirve para
 * varios registros hasta la fecha de vencimiento, y el docente puede desactivarlo
 * antes.</p>
 */
@Entity
@Table(name = "registration_code")
public class RegistrationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código que escribe el estudiante, único en el sistema. */
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    /** Docente dueño del código. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    /** Programa en el que quedan los estudiantes que se registran con el código. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** Último día en que el código se puede usar. */
    @Column(name = "expires_on", nullable = false)
    private LocalDate expiresOn;

    /** {@code false} si el docente lo desactivó antes de vencer. */
    @Column(nullable = false)
    private boolean active = true;

    /** Fecha y hora de creación. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Constructor requerido por JPA. */
    protected RegistrationCode() {
    }

    /**
     * Crea un código activo.
     *
     * @param code      código que escribirán los estudiantes
     * @param teacher   docente dueño del código
     * @param program   programa en el que quedarán los estudiantes
     * @param expiresOn último día de uso
     * @param createdAt fecha y hora de creación, obtenida del {@code Clock} inyectado
     */
    public RegistrationCode(String code, Teacher teacher, Program program, LocalDate expiresOn,
                            LocalDateTime createdAt) {
        this.code = code;
        this.teacher = teacher;
        this.program = program;
        this.expiresOn = expiresOn;
        this.createdAt = createdAt;
    }

    /**
     * Indica si un estudiante puede registrarse hoy con este código.
     *
     * @param today fecha actual, obtenida del {@code Clock} inyectado
     * @return {@code true} si está activo y no ha vencido
     */
    public boolean isUsableOn(LocalDate today) {
        return active && !today.isAfter(expiresOn);
    }

    /** @return identificador del código */
    public Long getId() {
        return id;
    }

    /** @return código que escriben los estudiantes */
    public String getCode() {
        return code;
    }

    /** @return docente dueño del código */
    public Teacher getTeacher() {
        return teacher;
    }

    /** @return programa en el que quedan los estudiantes */
    public Program getProgram() {
        return program;
    }

    /** @return último día de uso */
    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    /** @return {@code true} si no fue desactivado */
    public boolean isActive() {
        return active;
    }

    /** Desactiva el código: deja de servir de inmediato. */
    public void deactivate() {
        this.active = false;
    }

    /** @return fecha y hora de creación */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
