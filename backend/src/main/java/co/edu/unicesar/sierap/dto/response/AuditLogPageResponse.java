package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Página de la bitácora de auditoría.
 *
 * @param items registros de la página, del más reciente al más antiguo
 * @param total total de registros que cumplen el filtro
 * @param page  página devuelta (desde 0)
 * @param size  tamaño de página aplicado
 */
public record AuditLogPageResponse(List<AuditLogResponse> items, long total, int page, int size) {
}
