package co.edu.unicesar.sierap.repository;

import co.edu.unicesar.sierap.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio de la bitácora de auditoría. La consulta con filtros opcionales se
 * arma con {@code Specification} (ver {@code AuditServiceImpl}).
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
}
