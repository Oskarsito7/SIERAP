package co.edu.unicesar.sierap.controller;

import co.edu.unicesar.sierap.dto.response.SystemStatusResponse;
import co.edu.unicesar.sierap.service.SystemStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el estado general de la API en {@code GET /api/status}.
 *
 * <p>Sirve como verificación rápida de que el backend levantó y está conectado a
 * PostgreSQL, y como ejemplo mínimo del recorrido completo por capas
 * ({@code controller → service → dto}).</p>
 */
@RestController
@RequestMapping("/api/status")
public class SystemStatusController {

    private final SystemStatusService systemStatusService;

    /**
     * Crea el controller con el servicio que resuelve la consulta.
     *
     * @param systemStatusService servicio de estado del sistema
     */
    public SystemStatusController(SystemStatusService systemStatusService) {
        this.systemStatusService = systemStatusService;
    }

    /**
     * Devuelve el estado actual de la API.
     *
     * @return versión, perfiles activos, conexión con la base de datos y hora del servidor
     */
    @GetMapping
    public SystemStatusResponse getStatus() {
        return systemStatusService.getStatus();
    }
}
