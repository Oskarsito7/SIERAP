package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateRegistrationCodeRequest;
import co.edu.unicesar.sierap.dto.response.PagedResult;
import co.edu.unicesar.sierap.dto.response.RegistrationCodeResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

/**
 * Códigos de registro de los docentes (ADR-017).
 */
public interface RegistrationCodeService {

    /**
     * Genera un código para un programa de la facultad del docente.
     *
     * @param actor   docente, o admin/super_admin que lo genera por un docente
     * @param request programa, vencimiento y (si no es el docente) el docente dueño
     * @return código generado
     */
    RegistrationCodeResponse create(AuthenticatedUser actor, NewRegistrationCodeRequest request);

    /**
     * Lista los códigos dentro del alcance del usuario: los propios para un docente,
     * los de su facultad para un admin, todos para el super_admin.
     *
     * @param actor usuario que consulta
     * @return códigos de registro
     */
    List<RegistrationCodeResponse> list(AuthenticatedUser actor);

    /**
     * Igual que {@link #list(AuthenticatedUser)} pero con paginación opcional (ADR-024).
     *
     * @param actor    usuario que consulta
     * @param pageable página pedida, o {@code Pageable.unpaged()} para traer todos
     * @return códigos del más reciente al más antiguo, y el total sin paginar
     */
    PagedResult<RegistrationCodeResponse> list(AuthenticatedUser actor, Pageable pageable);

    /**
     * Desactiva un código (no se puede reactivar).
     *
     * @param actor   usuario que lo pide
     * @param id      código
     * @param request nuevo estado
     * @return código actualizado
     */
    RegistrationCodeResponse update(AuthenticatedUser actor, Long id, UpdateRegistrationCodeRequest request);
}
