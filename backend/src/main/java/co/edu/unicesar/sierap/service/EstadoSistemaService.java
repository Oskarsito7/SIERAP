package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.response.EstadoSistemaResponse;

/**
 * Consulta el estado general de la API (versión, perfil activo y conexión con la
 * base de datos).
 */
public interface EstadoSistemaService {

    /**
     * Obtiene el estado actual del sistema.
     *
     * @return estado de la API y de la conexión con la base de datos
     */
    EstadoSistemaResponse consultarEstado();
}
