package co.edu.unicesar.sierap.controller;

import co.edu.unicesar.sierap.dto.response.EstadoSistemaResponse;
import co.edu.unicesar.sierap.service.EstadoSistemaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone el estado general de la API en {@code GET /api/estado}.
 *
 * <p>Sirve como verificación rápida de que el backend levantó y está conectado a
 * PostgreSQL, y como ejemplo mínimo del recorrido completo por capas
 * ({@code controller → service → dto}).</p>
 */
@RestController
@RequestMapping("/api/estado")
public class EstadoSistemaController {

    private final EstadoSistemaService estadoSistemaService;

    /**
     * Crea el controller con el servicio que resuelve la consulta.
     *
     * @param estadoSistemaService servicio de estado del sistema
     */
    public EstadoSistemaController(EstadoSistemaService estadoSistemaService) {
        this.estadoSistemaService = estadoSistemaService;
    }

    /**
     * Devuelve el estado actual de la API.
     *
     * @return versión, perfiles activos, conexión con la base de datos y hora del servidor
     */
    @GetMapping
    public EstadoSistemaResponse consultarEstado() {
        return estadoSistemaService.consultarEstado();
    }
}
