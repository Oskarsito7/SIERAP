package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.request.ChangePasswordRequest;
import co.edu.unicesar.sierap.dto.request.ForgotPasswordRequest;
import co.edu.unicesar.sierap.dto.request.LoginRequest;
import co.edu.unicesar.sierap.dto.request.RegisterStudentRequest;
import co.edu.unicesar.sierap.dto.request.SetPasswordRequest;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;

/**
 * Inicio de sesión, sesiones y contraseñas (Etapa 2, ADR-017).
 */
public interface AuthService {

    /**
     * Inicia sesión con correo y contraseña.
     *
     * @param request credenciales
     * @return token de acceso, usuario y token de renovación
     * @throws co.edu.unicesar.sierap.exception.AuthenticationFailedException si las
     *         credenciales no son correctas (mismo mensaje exista o no el correo)
     */
    AuthSession login(LoginRequest request);

    /**
     * Renueva la sesión con el token de renovación de la cookie. El token usado se
     * marca como rotado y se entrega uno nuevo (rotación).
     *
     * @param refreshToken valor de la cookie, o {@code null} si no llegó
     * @return nueva sesión
     * @throws co.edu.unicesar.sierap.exception.AuthenticationFailedException si el
     *         token falta, venció, ya se rotó o la sesión se cerró
     */
    AuthSession refresh(String refreshToken);

    /**
     * Cierra la sesión borrando el token de renovación, si llegó.
     *
     * @param refreshToken valor de la cookie, o {@code null}
     */
    void logout(String refreshToken);

    /**
     * Devuelve el usuario de la sesión.
     *
     * @param userId identificador del usuario autenticado
     * @return resumen del usuario
     */
    UserSummaryResponse getCurrentUser(Long userId);

    /**
     * Cambia la contraseña de un usuario autenticado. Revoca sus otras sesiones y
     * devuelve una sesión nueva, sin la marca de contraseña temporal.
     *
     * @param userId  identificador del usuario autenticado
     * @param request contraseña actual y nueva
     * @return nueva sesión
     */
    AuthSession changePassword(Long userId, ChangePasswordRequest request);

    /**
     * Envía el enlace para recuperar la contraseña, si la cuenta existe y está activa.
     * No informa si el correo existe.
     *
     * @param request correo de la cuenta
     */
    void forgotPassword(ForgotPasswordRequest request);

    /**
     * Crea o recupera la contraseña con el enlace enviado por correo.
     *
     * @param request valor del enlace y nueva contraseña
     */
    void setPassword(SetPasswordRequest request);

    /**
     * Registra a un estudiante con el código de su docente y le envía el enlace para
     * crear su contraseña.
     *
     * @param request datos del estudiante y código de registro
     */
    void registerStudent(RegisterStudentRequest request);
}
