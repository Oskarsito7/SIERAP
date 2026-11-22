package co.edu.unicesar.sierap.security;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.config.RateLimitProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
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
 *   <li>Endurecimiento de producción (ADR-021): límite de intentos por IP en las rutas de
 *       autenticación, cabeceras de seguridad, CORS por entorno y solo las sondas
 *       {@code /actuator/health/**} públicas.</li>
 *   <li>CSRF desactivado: la API no usa cookies de sesión; la cookie de renovación es
 *       {@code SameSite=Strict} y solo viaja a {@code /api/auth}.</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({AuthProperties.class, RateLimitProperties.class})
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
     * @param rateLimit   límites de intentos por IP
     * @param clock       reloj de la aplicación
     * @return cadena de filtros
     * @throws Exception si la configuración no es válida
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                   SecurityErrorWriter errorWriter,
                                                   RateLimitProperties rateLimit, Clock clock) throws Exception {
        return http
                // Toma el bean "corsConfigurationSource" por nombre: inyectarlo por tipo es
                // ambiguo porque HandlerMappingIntrospector de Spring MVC también lo implementa.
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=()"))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true).maxAgeInSeconds(31536000)))
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
                        .requestMatchers(HttpMethod.GET, "/api/status", "/api/auth/privacy-policy").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) ->
                                errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                                        "Debe iniciar sesión para continuar."))
                        .accessDeniedHandler((request, response, exception) ->
                                errorWriter.write(request, response, HttpStatus.FORBIDDEN,
                                        "No tiene permisos para realizar esta operación.")))
                .addFilterBefore(new RateLimitFilter(rateLimit, errorWriter, clock),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthFilter(jwtService, errorWriter), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Orígenes permitidos por CORS ({@code sierap.security.allowed-origins}, ADR-021).
     * Vacío significa sin CORS: el frontend y la API comparten origen.
     *
     * @param allowedOrigins orígenes separados por comas
     * @return fuente de configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${sierap.security.allowed-origins:}") String allowedOrigins) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim).filter(origin -> !origin.isEmpty()).toList();
        if (origins.isEmpty()) {
            return source;
        }
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setAllowCredentials(true);
        cors.setExposedHeaders(List.of("Content-Disposition", "X-Total-Count"));
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
