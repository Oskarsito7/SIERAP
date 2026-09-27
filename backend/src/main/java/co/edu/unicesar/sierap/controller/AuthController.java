package co.edu.unicesar.sierap.controller;

import co.edu.unicesar.sierap.dto.request.ChangePasswordRequest;
import co.edu.unicesar.sierap.dto.request.ForgotPasswordRequest;
import co.edu.unicesar.sierap.dto.request.LoginRequest;
import co.edu.unicesar.sierap.dto.request.RegisterStudentRequest;
import co.edu.unicesar.sierap.dto.request.SetPasswordRequest;
import co.edu.unicesar.sierap.dto.response.TokenResponse;
import co.edu.unicesar.sierap.dto.response.UserSummaryResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import co.edu.unicesar.sierap.security.RefreshCookie;
import co.edu.unicesar.sierap.service.AuthService;
import co.edu.unicesar.sierap.service.AuthSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio de sesión, sesiones y contraseñas en {@code /api/auth} (Etapa 2, ADR-017;
 * contrato en {@code docs/07-contrato-api.md}).
 *
 * <p>El token de acceso va en el cuerpo de la respuesta; el de renovación, en la
 * cookie {@code sierap_refresh}, que el navegador guarda y reenvía solo a
 * {@code /api/auth}.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookie refreshCookie;

    /**
     * Crea el controller.
     *
     * @param authService   servicio de autenticación
     * @param refreshCookie fábrica de la cookie de renovación
     */
    public AuthController(AuthService authService, RefreshCookie refreshCookie) {
        this.authService = authService;
        this.refreshCookie = refreshCookie;
    }

    /**
     * Inicia sesión con correo y contraseña.
     *
     * @param request credenciales
     * @return token de acceso y usuario, con la cookie de renovación
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return withCookie(authService.login(request));
    }

    /**
     * Renueva la sesión con la cookie de renovación (y la rota).
     *
     * @param refreshToken valor de la cookie, si llegó
     * @return nuevo token de acceso, con la cookie rotada
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        return withCookie(authService.refresh(refreshToken));
    }

    /**
     * Cierra la sesión: borra el token de renovación y la cookie. Es pública
     * para poder cerrar sesión aunque el token de acceso ya haya vencido.
     *
     * @param refreshToken valor de la cookie, si llegó
     * @return 204 con la cookie borrada
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshCookie.NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString())
                .build();
    }

    /**
     * Devuelve el usuario de la sesión.
     *
     * @param user usuario autenticado
     * @return resumen del usuario
     */
    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return authService.getCurrentUser(user.id());
    }

    /**
     * Cambia la contraseña del usuario autenticado. Cierra sus otras sesiones y
     * devuelve una nueva, ya sin la marca de contraseña temporal.
     *
     * @param user    usuario autenticado
     * @param request contraseña actual y nueva
     * @return nuevo token de acceso, con una cookie de renovación nueva
     */
    @PostMapping("/password/change")
    public ResponseEntity<TokenResponse> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                                        @Valid @RequestBody ChangePasswordRequest request) {
        return withCookie(authService.changePassword(user.id(), request));
    }

    /**
     * Pide el enlace para recuperar la contraseña. Responde igual exista o no la
     * cuenta, para no revelar qué correos están registrados.
     *
     * @param request correo de la cuenta
     */
    @PostMapping("/password/forgot")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
    }

    /**
     * Crea o recupera la contraseña con el enlace recibido por correo.
     *
     * @param request valor del enlace y nueva contraseña
     */
    @PostMapping("/password/set")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setPassword(@Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(request);
    }

    /**
     * Registra a un estudiante con el código de su docente. La cuenta queda creada y
     * el estudiante recibe por correo el enlace para crear su contraseña.
     *
     * @param request datos del estudiante y código de registro
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void register(@Valid @RequestBody RegisterStudentRequest request) {
        authService.registerStudent(request);
    }

    private ResponseEntity<TokenResponse> withCookie(AuthSession session) {
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.create(session.refreshToken()).toString())
                .body(session.token());
    }
}
