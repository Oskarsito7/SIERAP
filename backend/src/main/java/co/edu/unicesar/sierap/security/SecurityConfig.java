package co.edu.unicesar.sierap.security;

import co.edu.unicesar.sierap.config.AuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security (Etapa 2, ADR-017).
 *
 * <ul>
 *   <li>API sin estado: no hay sesión de servidor; cada petición trae su token.</li>
 *   <li>Rutas públicas: iniciar sesión, renovar y cerrar sesión, recuperar o crear
 *       la contraseña con el enlace, registrarse con código y el estado del sistema.
 *       Todo lo demás exige un token válido.</li>
 *   <li>Los roles se validan en los controllers con {@code @PreAuthorize}; el alcance
 *       (su facultad, sus grupos…) lo validan los services.</li>
 *   <li>CSRF desactivado: la API no usa cookies de sesión; la cookie de renovación es
 *       {@code SameSite=Strict} y solo viaja a {@code /api/auth}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthProperties.class)
public class SecurityConfig {

    /**
     * Codificador de contraseñas: BCrypt con su costo por defecto.
     *
     * @return codificador de contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cadena de filtros de seguridad de la API.
     *
     * @param http        constructor de la configuración HTTP
     * @param jwtService  servicio que valida los tokens de acceso
     * @param errorWriter escritor de errores 401/403 con el formato de la API
     * @return cadena de filtros
     * @throws Exception si la configuración no es válida
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   SecurityErrorWriter errorWriter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST,
                                "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/password/forgot", "/api/auth/password/set", "/api/auth/register")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/status").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                                        "Debe iniciar sesión para continuar."))
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.write(request, response, HttpStatus.FORBIDDEN,
                                        "No tiene permisos para realizar esta operación.")))
                .addFilterBefore(new JwtAuthFilter(jwtService, errorWriter), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
