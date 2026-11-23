package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import co.edu.unicesar.sierap.dto.response.AuditLogPageResponse;
import co.edu.unicesar.sierap.dto.response.AuditLogResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.AuditLog;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.repository.AuditLogRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.service.AuditService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Implementación de {@link AuditService} (ADR-023).
 *
 * <p>{@link #record} exige una transacción en curso ({@link Propagation#MANDATORY}):
 * así el registro siempre viaja con la operación que lo origina y se descarta si esta
 * hace rollback. El actor y la IP son opcionales (jobs o pruebas sin sesión).</p>
 */
@Service
public class AuditServiceImpl implements AuditService {

    /** Tamaño de página por defecto de la consulta. */
    static final int DEFAULT_SIZE = 50;

    /** Tamaño de página máximo de la consulta. */
    static final int MAX_SIZE = 200;

    /** Largo máximo del resumen (ancho de la columna). */
    static final int MAX_SUMMARY = 1000;

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final AccessScope accessScope;
    private final Clock clock;

    /**
     * Crea el servicio con sus dependencias.
     *
     * @param auditLogRepository repositorio de la bitácora
     * @param userRepository     repositorio de usuarios (para copiar el nombre del actor)
     * @param accessScope        reglas de alcance
     * @param clock              reloj de la aplicación
     */
    public AuditServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository,
                            AccessScope accessScope, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.accessScope = accessScope;
        this.clock = clock;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AuditAction action, String entityType, Long entityId, Long facultyId, String summary) {
        Long actorId = null;
        String actorName = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser actor) {
            actorId = actor.id();
            actorName = userRepository.findById(actor.id()).map(User::getFullName).orElse(actor.email());
        }
        String ip = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            ip = attributes.getRequest().getRemoteAddr();
        }
        auditLogRepository.save(new AuditLog(LocalDateTime.now(clock), actorId, actorName, action, entityType,
                entityId, facultyId, fit(summary), ip));
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogPageResponse search(AuthenticatedUser actor, Integer page, Integer size, AuditAction action,
                                       LocalDate from, LocalDate to, String q) {
        User viewer = accessScope.loadActor(actor);
        Long facultyScope = null;
        if (viewer instanceof Admin admin) {
            facultyScope = admin.getFaculty().getId();
        } else if (!(viewer instanceof SuperAdmin)) {
            throw new ForbiddenAccessException("No tiene acceso a la bitácora de auditoría.");
        }
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessRuleException("La fecha final no puede ser anterior a la inicial.");
        }
        int pageNumber = page == null ? 0 : Math.max(page, 0);
        int pageSize = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        Page<AuditLog> result = auditLogRepository.findAll(
                filter(facultyScope, action, from, to, q),
                PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        List<AuditLogResponse> items = result.getContent().stream().map(AuditServiceImpl::toResponse).toList();
        return new AuditLogPageResponse(items, result.getTotalElements(), pageNumber, pageSize);
    }

    /** Arma los filtros opcionales; el texto busca sin distinguir mayúsculas en resumen y actor. */
    private static Specification<AuditLog> filter(Long facultyId, AuditAction action, LocalDate from,
                                                  LocalDate to, String q) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (facultyId != null) {
                predicates.add(builder.equal(root.get("facultyId"), facultyId));
            }
            if (action != null) {
                predicates.add(builder.equal(root.get("action"), action));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("summary")), pattern, '\\'),
                        builder.like(builder.lower(root.get("actorName")), pattern, '\\')));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Recorta el resumen al ancho de la columna para que un texto largo nunca tumbe la operación. */
    private static String fit(String summary) {
        return summary.length() <= MAX_SUMMARY ? summary : summary.substring(0, MAX_SUMMARY - 1) + "…";
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(log.getId(), log.getCreatedAt(), log.getActorId(), log.getActorName(),
                log.getAction(), log.getEntityType(), log.getEntityId(), log.getFacultyId(), log.getSummary(),
                log.getIp());
    }
}
