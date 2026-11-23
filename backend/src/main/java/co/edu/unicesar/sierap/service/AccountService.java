package co.edu.unicesar.sierap.service;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewAdminRequest;
import co.edu.unicesar.sierap.dto.request.NewExternalEvaluatorRequest;
import co.edu.unicesar.sierap.dto.request.NewStudentRequest;
import co.edu.unicesar.sierap.dto.request.NewTeacherRequest;
import co.edu.unicesar.sierap.dto.request.SetSemesterRequest;
import co.edu.unicesar.sierap.dto.request.UpdateUserRequest;
import co.edu.unicesar.sierap.dto.response.PagedResult;
import co.edu.unicesar.sierap.dto.response.StudentResponse;
import co.edu.unicesar.sierap.dto.response.UpdatedCountResponse;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.springframework.data.domain.Pageable;

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
     * @return estudiante creado
     */
    StudentResponse createStudent(AuthenticatedUser actor, NewStudentRequest request);

    /**
     * Crea un estudiante como parte de una importación CSV: igual que
     * {@link #createStudent}, pero sin registrar un evento por cada fila (la importación
     * deja un único evento con la cantidad, ADR-023).
     *
     * @param actor   usuario que importa
     * @param request datos del estudiante
     * @return estudiante creado
     */
    StudentResponse createImportedStudent(AuthenticatedUser actor, NewStudentRequest request);

    /**
     * Crea un evaluador externo de un tipo activo del catálogo.
     *
     * @param actor   usuario que crea la cuenta
     * @param request datos del evaluador
     * @return usuario creado
     */
    UserSummaryResponse createExternalEvaluator(AuthenticatedUser actor, NewExternalEvaluatorRequest request);

    /**
     * Lista los administradores de una facultad (solo el super_admin, SA-05).
     *
     * @param facultyId facultad
     * @return administradores de la facultad
     */
    List<UserSummaryResponse> listAdmins(Long facultyId);

    /**
     * Lista los docentes de una facultad dentro del alcance del usuario.
     *
     * @param actor     usuario que consulta
     * @param facultyId facultad
     * @return docentes de la facultad
     */
    List<UserSummaryResponse> listTeachers(AuthenticatedUser actor, Long facultyId);

    /**
     * Lista los estudiantes de un programa dentro del alcance del usuario (DO-08), con la
     * marca de los que puede gestionar.
     *
     * @param actor                usuario que consulta
     * @param programId            programa
     * @param withoutGroupInMoment si no es {@code null}, solo los activos del programa, con
     *                             su semestre en el rango del momento, que no tienen grupo
     *                             en él (lo mismo que {@code students-without-group})
     * @return estudiantes ordenados por apellidos y nombres
     */
    List<StudentResponse> listStudents(AuthenticatedUser actor, Long programId, Long withoutGroupInMoment);

    /**
     * Lista, con paginación opcional, los estudiantes de un programa dentro del alcance del
     * usuario, con búsqueda opcional (ADR-024).
     *
     * @param actor     usuario que consulta
     * @param programId programa
     * @param query     texto a buscar en nombres, apellidos, código y correo, sin distinguir
     *                  mayúsculas ni tildes; {@code null} o vacío para no filtrar
     * @param pageable  página pedida, o {@code Pageable.unpaged()} para traer todos
     * @return estudiantes ordenados por apellidos y nombres, y el total sin paginar
     */
    PagedResult<StudentResponse> searchStudents(AuthenticatedUser actor, Long programId, String query,
                                                Pageable pageable);

    /**
     * Lista, con paginación opcional, los evaluadores externos dentro del alcance del
     * usuario (ADR-024).
     *
     * @param actor    usuario que consulta
     * @param pageable página pedida, o {@code Pageable.unpaged()} para traer todos
     * @return evaluadores externos ordenados por apellidos, y el total sin paginar
     */
    PagedResult<UserSummaryResponse> listExternalEvaluators(AuthenticatedUser actor, Pageable pageable);

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
     * Edita los datos de una cuenta (todos los campos son opcionales) o la activa o
     * desactiva. Desactivarla, o cambiarle el correo, cierra sus sesiones.
     *
     * @param actor   usuario que lo pide
     * @param userId  cuenta destino
     * @param request cambios; debe traer al menos un campo
     * @return usuario actualizado
     */
    UserSummaryResponse updateUser(AuthenticatedUser actor, Long userId, UpdateUserRequest request);

    /**
     * Fija el mismo semestre a varios estudiantes. Es todo o nada: si alguno no existe,
     * no es estudiante o está fuera del alcance del usuario, no se actualiza ninguno.
     *
     * @param actor   usuario que lo pide
     * @param request estudiantes y semestre
     * @return cantidad de estudiantes actualizados
     */
    UpdatedCountResponse setSemester(AuthenticatedUser actor, SetSemesterRequest request);
}
