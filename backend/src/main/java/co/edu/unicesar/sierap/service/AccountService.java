package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewAdminRequest;
import co.edu.unicesar.sierap.dto.request.NewExternalEvaluatorRequest;
import co.edu.unicesar.sierap.dto.request.NewStudentRequest;
import co.edu.unicesar.sierap.dto.request.NewTeacherRequest;
import co.edu.unicesar.sierap.dto.request.UpdateUserRequest;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Creación y gestión de cuentas (Etapa 2, ADR-017).
 *
 * <p>Cadena de creación: super_admin → admin → docente → estudiante y evaluador
 * externo; los roles de arriba también pueden crear usuarios de abajo, dentro de su
 * alcance (su facultad). Las cuentas nuevas no tienen contraseña: reciben un enlace
 * por correo para crearla.</p>
 */
public interface AccountService {

    /**
     * Crea el administrador de una facultad (solo el super_admin).
     *
     * @param actor   usuario que crea la cuenta
     * @param request datos del administrador
     * @return usuario creado
     */
    UserSummaryResponse createAdmin(AuthenticatedUser actor, NewAdminRequest request);

    /**
     * Crea un docente (super_admin, o admin de la facultad).
     *
     * @param actor   usuario que crea la cuenta
     * @param request datos del docente
     * @return usuario creado
     */
    UserSummaryResponse createTeacher(AuthenticatedUser actor, NewTeacherRequest request);

    /**
     * Crea un estudiante (super_admin, o admin o docente de la facultad del programa).
     *
     * @param actor   usuario que crea la cuenta
     * @param request datos del estudiante
     * @return usuario creado
     */
    UserSummaryResponse createStudent(AuthenticatedUser actor, NewStudentRequest request);

    /**
     * Crea un evaluador externo de un tipo activo del catálogo.
     *
     * @param actor   usuario que crea la cuenta
     * @param request datos del evaluador
     * @return usuario creado
     */
    UserSummaryResponse createExternalEvaluator(AuthenticatedUser actor, NewExternalEvaluatorRequest request);

    /**
     * Lista los docentes de una facultad dentro del alcance del usuario.
     *
     * @param actor     usuario que consulta
     * @param facultyId facultad
     * @return docentes de la facultad
     */
    List<UserSummaryResponse> listTeachers(AuthenticatedUser actor, Long facultyId);

    /**
     * Lista los estudiantes de un programa dentro del alcance del usuario.
     *
     * @param actor     usuario que consulta
     * @param programId programa
     * @return estudiantes del programa
     */
    List<UserSummaryResponse> listStudents(AuthenticatedUser actor, Long programId);

    /**
     * Lista los evaluadores externos: todos para el super_admin; los que creó el
     * usuario para admins y docentes.
     *
     * @param actor usuario que consulta
     * @return evaluadores externos
     */
    List<UserSummaryResponse> listExternalEvaluators(AuthenticatedUser actor);

    /**
     * Reenvía el enlace de contraseña: para crearla si la cuenta aún no tiene, o
     * para recuperarla si ya tiene.
     *
     * @param actor  usuario que lo pide
     * @param userId cuenta destino
     */
    void resendPasswordLink(AuthenticatedUser actor, Long userId);

    /**
     * Activa o desactiva una cuenta. Desactivarla cierra sus sesiones.
     *
     * @param actor   usuario que lo pide
     * @param userId  cuenta destino
     * @param request nuevo estado
     * @return usuario actualizado
     */
    UserSummaryResponse updateUser(AuthenticatedUser actor, Long userId, UpdateUserRequest request);
}
