package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.AuditAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro de la bitácora de auditoría (ADR-023): quién hizo qué, sobre qué y cuándo.
 *
 * <p>Es <b>inmutable</b>: solo tiene constructor y getters, sus columnas no son
 * actualizables y la base de datos rechaza UPDATE y DELETE mediante un trigger
 * (migración V13). El actor se guarda como id y como copia de su nombre, para que el
 * historial siga siendo legible aunque la cuenta cambie. {@code facultyId} permite que
 * un admin consulte solo los hechos de su facultad.</p>
 */
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Momento en que ocurrió el hecho. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Id del usuario que actuó; {@code null} si no había usuario autenticado. */
    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    /** Copia del nombre del actor al momento del hecho. */
    @Column(name = "actor_name", length = 250, updatable = false)
    private String actorName;

    /** Tipo de hecho. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private AuditAction action;

    /** Tipo de objeto afectado (p. ej. {@code User}, {@code Moment}). */
    @Column(name = "entity_type", nullable = false, length = 50, updatable = false)
    private String entityType;

    /** Id del objeto afectado, si aplica. */
    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    /** Facultad del objeto afectado, para acotar la consulta del admin. */
    @Column(name = "faculty_id", updatable = false)
    private Long facultyId;

    /** Descripción breve del hecho, en español. */
    @Column(nullable = false, length = 1000, updatable = false)
    private String summary;

    /** Dirección IP de la petición, si la hubo. */
    @Column(length = 64, updatable = false)
    private String ip;

    /** Constructor requerido por JPA. */
    protected AuditLog() {
    }

    /**
     * Crea un registro de auditoría.
     *
     * @param createdAt  momento del hecho
     * @param actorId    id del actor, o {@code null}
     * @param actorName  nombre del actor, o {@code null}
     * @param action     tipo de hecho
     * @param entityType tipo de objeto afectado
     * @param entityId   id del objeto afectado, o {@code null}
     * @param facultyId  facultad del objeto afectado, o {@code null}
     * @param summary    descripción breve en español
     * @param ip         dirección IP de la petición, o {@code null}
     */
    public AuditLog(LocalDateTime createdAt, Long actorId, String actorName, AuditAction action,
                    String entityType, Long entityId, Long facultyId, String summary, String ip) {
        this.createdAt = createdAt;
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.facultyId = facultyId;
        this.summary = summary;
        this.ip = ip;
    }

    /** @return identificador del registro */
    public Long getId() {
        return id;
    }

    /** @return momento del hecho */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** @return id del actor, o {@code null} */
    public Long getActorId() {
        return actorId;
    }

    /** @return nombre del actor al momento del hecho, o {@code null} */
    public String getActorName() {
        return actorName;
    }

    /** @return tipo de hecho */
    public AuditAction getAction() {
        return action;
    }

    /** @return tipo de objeto afectado */
    public String getEntityType() {
        return entityType;
    }

    /** @return id del objeto afectado, o {@code null} */
    public Long getEntityId() {
        return entityId;
    }

    /** @return facultad del objeto afectado, o {@code null} */
    public Long getFacultyId() {
        return facultyId;
    }

    /** @return descripción breve del hecho */
    public String getSummary() {
        return summary;
    }

    /** @return dirección IP de la petición, o {@code null} */
    public String getIp() {
        return ip;
    }
}
