package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.SetGlobalParameterRequest;
import co.edu.unicesar.sierap.dto.response.GlobalParameterResponse;

/**
 * Gestión de parámetros globales del sistema (RF 11).
 */
public interface GlobalParameterService {

    /**
     * Lista todos los parámetros globales ordenados por clave.
     *
     * @return parámetros globales
     */
    List<GlobalParameterResponse> listAll();

    /**
     * Crea o actualiza un parámetro global por su clave.
     *
     * @param key     clave única del parámetro
     * @param request valor y descripción opcional
     * @return parámetro creado o actualizado
     */
    GlobalParameterResponse set(String key, SetGlobalParameterRequest request);
}
