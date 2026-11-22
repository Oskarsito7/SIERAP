package co.edu.unicesar.sierap.controller;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.SetGlobalParameterRequest;
import co.edu.unicesar.sierap.dto.response.GlobalParameterResponse;
import co.edu.unicesar.sierap.service.GlobalParameterService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de gestión de parámetros globales del sistema (RF 11).
 */
@RestController
@RequestMapping("/api/global-parameters")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class GlobalParameterController {

    private final GlobalParameterService globalParameterService;

    /**
     * Crea el controller.
     *
     * @param globalParameterService servicio de parámetros globales
     */
    public GlobalParameterController(GlobalParameterService globalParameterService) {
        this.globalParameterService = globalParameterService;
    }

    /**
     * Lista todos los parámetros globales ordenados por clave.
     *
     * @return parámetros globales
     */
    @GetMapping
    public List<GlobalParameterResponse> listAll() {
        return globalParameterService.listAll();
    }

    /**
     * Crea o actualiza un parámetro global por su clave.
     *
     * @param key     clave del parámetro
     * @param request valor y descripción opcional
     * @return parámetro actualizado o creado
     */
    @PutMapping("/{key}")
    public GlobalParameterResponse set(@PathVariable String key,
                                       @Valid @RequestBody SetGlobalParameterRequest request) {
        return globalParameterService.set(key, request);
    }
}
