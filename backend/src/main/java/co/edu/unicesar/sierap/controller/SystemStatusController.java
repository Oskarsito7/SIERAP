package co.edu.unicesar.sierap.controller;

import co.edu.unicesar.sierap.dto.response.SystemStatusResponse;
import co.edu.unicesar.sierap.service.SystemStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the overall API status at {@code GET /api/status}.
 *
 * <p>Serves as a quick check that the backend is up and connected to PostgreSQL,
 * and as a minimal example of the full trip through the layers
 * ({@code controller → service → dto}).</p>
 */
@RestController
@RequestMapping("/api/status")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    /**
     * Creates the controller with the service that resolves the query.
     *
     * @param systemStatusService system status service
     */
    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    /**
     * Returns the current API status.
     *
     * @return version, active profiles, database connection and server time
     */
    @GetMapping
    public SystemStatusResponse getStatus() {
        return systemStatusService.getStatus();
    }
}
