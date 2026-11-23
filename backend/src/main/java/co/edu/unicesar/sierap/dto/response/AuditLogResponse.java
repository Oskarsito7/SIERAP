package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDateTime;

import co.edu.unicesar.sierap.entity.enums.AuditAction;

/**
 * Registro de la bitácora de auditoría.
 *
 * @param id         identificador del registro
 * @param createdAt  momento del hecho
 * @param actorId    id del actor, o {@code null}
 * @param actorName  nombre del actor, o {@code null}
 * @param action     tipo de hecho
 * @param entityType tipo de objeto afectado
 * @param entityId   id del objeto afectado, o {@code null}
 * @param facultyId  facultad del objeto afectado, o {@code null}
 * @param summary    descripción del hecho, en español
 * @param ip         dirección IP de la petición, o {@code null}
 */
public record AuditLogResponse(Long id, LocalDateTime createdAt, Long actorId, String actorName,
                               AuditAction action, String entityType, Long entityId, Long facultyId,
                               String summary, String ip) {
}
