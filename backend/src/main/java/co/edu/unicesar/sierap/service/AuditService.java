package co.edu.unicesar.sierap.service;

import java.time.LocalDate;

import co.edu.unicesar.sierap.dto.response.AuditLogPageResponse;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Bitácora de auditoría inmutable (ADR-023): registro de hechos sensibles y consulta.
 */
public interface AuditService {

    /**
     * Registra un hecho en la misma transacción de la operación que lo origina (si esa
     * operación hace rollback, el registro tampoco queda). Toma el actor autenticado y
     * la IP de la petición en curso cuando existen.
     *
     * @param action     tipo de hecho
     * @param entityType tipo de objeto afectado
     * @param entityId   id del objeto afectado, o {@code null}
     * @param facultyId  facultad del objeto afectado, o {@code null}
     * @param summary    descripción breve y concreta, en español
     */
    void record(AuditAction action, String entityType, Long entityId, Long facultyId, String summary);

    /**
     * Consulta paginada de la bitácora, del hecho más reciente al más antiguo.
     *
     * @param actor  usuario que consulta (super_admin: todo; admin: solo su facultad)
     * @param page   página (desde 0), o {@code null} para la primera
     * @param size   tamaño de página (por defecto 50, máximo 200)
     * @param action filtro por tipo de hecho, o {@code null}
     * @param from   fecha inicial inclusive, o {@code null}
     * @param to     fecha final inclusive, o {@code null}
     * @param q      texto a buscar en el resumen y el nombre del actor, o {@code null}
     * @return página de registros
     */
    AuditLogPageResponse search(AuthenticatedUser actor, Integer page, Integer size, AuditAction action,
                                LocalDate from, LocalDate to, String q);
}
