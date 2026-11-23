package co.edu.unicesar.sierap.controller;

import java.time.LocalDate;

import co.edu.unicesar.sierap.dto.response.AuditLogPageResponse;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de la bitácora de auditoría (ADR-023). Es de solo lectura: la bitácora no
 * tiene endpoints de edición ni de borrado.
 */
@RestController
@RequestMapping("/api/audit-log")
public class AuditLogController {

    private final AuditService auditService;

    /**
     * Crea el controller.
     *
     * @param auditService servicio de la bitácora
     */
    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Consulta la bitácora, del hecho más reciente al más antiguo.
     *
     * @param actor  usuario autenticado (super_admin: todo; admin: su facultad)
     * @param page   página, desde 0
     * @param size   tamaño de página (50 por defecto, máximo 200)
     * @param action tipo de hecho
     * @param from   fecha inicial inclusive
     * @param to     fecha final inclusive
     * @param q      texto a buscar en el resumen y el nombre del actor
     * @return página de registros
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public AuditLogPageResponse search(@AuthenticationPrincipal AuthenticatedUser actor,
                                       @RequestParam(required = false) Integer page,
                                       @RequestParam(required = false) Integer size,
                                       @RequestParam(required = false) AuditAction action,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) String q) {
        return auditService.search(actor, page, size, action, from, to, q);
    }
}
