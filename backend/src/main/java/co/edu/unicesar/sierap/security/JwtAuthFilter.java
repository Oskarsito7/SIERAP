package co.edu.unicesar.sierap.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro que autentica cada petición con el token de acceso del encabezado
 * {@code Authorization: Bearer ...} (ADR-017).
 *
 * <ol>
 *   <li>Si no hay token, o es inválido o está vencido, la petición sigue sin
 *       autenticar: las rutas públicas funcionan y las protegidas responden 401.</li>
 *   <li>Si el token es válido, registra al usuario y su rol ({@code ROLE_...}) en el
 *       contexto de seguridad, para {@code @PreAuthorize} y los controllers.</li>
 *   <li>Si el usuario debe cambiar su contraseña temporal, solo deja pasar las rutas
 *       de {@code /api/auth} (cambio de contraseña, perfil, renovar y cerrar sesión) y
 *       el estado del sistema; todo lo demás responde 403.</li>
 * </ol>
 *
 * <p>No es un {@code @Component} a propósito: lo crea {@link SecurityConfig} dentro de
 * la cadena de seguridad, para que Spring Boot no lo registre dos veces como filtro
 * del servidor.</p>
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Prefijo de las rutas permitidas mientras el usuario tenga una contraseña
     * temporal. Todas las de {@code /api/auth} son públicas o sirven justo para salir
     * de ese estado (cambiar la contraseña, ver el perfil, cerrar sesión).
     */
    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    /** Ruta pública del estado del sistema, que tampoco se bloquea. */
    private static final String STATUS_PATH = "/api/status";

    private final JwtService jwtService;
    private final SecurityErrorWriter errorWriter;

    /**
     * Crea el filtro.
     *
     * @param jwtService  servicio que valida los tokens
     * @param errorWriter escritor de errores con el formato de la API
     */
    public JwtAuthFilter(JwtService jwtService, SecurityErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<AuthenticatedUser> user = extractToken(request).flatMap(jwtService::parseAccessToken);
        if (user.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }
        AuthenticatedUser authenticatedUser = user.get();
        if (authenticatedUser.mustChangePassword() && !isAllowedWhilePasswordChangeRequired(request)) {
            errorWriter.write(request, response, HttpStatus.FORBIDDEN,
                    "Debe cambiar su contraseña temporal antes de continuar.");
            return;
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                authenticatedUser, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + authenticatedUser.role().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private static boolean isAllowedWhilePasswordChangeRequired(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith(AUTH_PATH_PREFIX) || path.equals(STATUS_PATH);
    }

    private static Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
    }
}
