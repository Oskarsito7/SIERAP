package co.edu.unicesar.sierap.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import co.edu.unicesar.sierap.config.AuthProperties;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

/**
 * Emite y valida los tokens de acceso (JWT firmados con HS256, ADR-017).
 *
 * <p>El token lleva el id del usuario ({@code sub}), su correo, su rol y si debe
 * cambiar la contraseña. Dura pocos minutos; para seguir en sesión el frontend usa
 * el token de renovación. Las fechas salen del {@link Clock} inyectado, así que las
 * pruebas pueden simular el paso del tiempo.</p>
 */
@Component
public class JwtService {

    /**
     * HS256 exige una clave de al menos 256 bits. El algoritmo se fija al firmar:
     * si no, jjwt elegiría HS384 o HS512 según el largo de la clave.
     */
    private static final int MIN_SECRET_BYTES = 32;

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_MUST_CHANGE_PASSWORD = "mustChangePassword";

    private final SecretKey key;
    private final Duration accessTokenDuration;
    private final Clock clock;

    /**
     * Crea el servicio. Falla al arrancar si la clave no existe o es demasiado corta,
     * para no terminar firmando tokens con una clave débil.
     *
     * @param properties parámetros de autenticación
     * @param clock      reloj de la aplicación
     */
    public JwtService(AuthProperties properties, Clock clock) {
        String secret = properties.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "sierap.auth.jwt-secret (SIERAP_JWT_SECRET) debe tener al menos " + MIN_SECRET_BYTES + " bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenDuration = Duration.ofMinutes(properties.accessTokenMinutes());
        this.clock = clock;
    }

    /**
     * Emite un token de acceso para el usuario.
     *
     * @param user usuario que inició sesión
     * @return JWT firmado
     */
    public String createAccessToken(User user) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_EMAIL, user.getEmail())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_MUST_CHANGE_PASSWORD, user.isMustChangePassword())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenDuration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Valida la firma y la vigencia de un token de acceso.
     *
     * @param token JWT recibido en el encabezado {@code Authorization}
     * @return el usuario del token, o vacío si es inválido o está vencido
     */
    public Optional<AuthenticatedUser> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(new AuthenticatedUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get(CLAIM_EMAIL, String.class),
                    UserRole.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    Boolean.TRUE.equals(claims.get(CLAIM_MUST_CHANGE_PASSWORD, Boolean.class))));
        } catch (JwtException | IllegalArgumentException exception) {
            // Token mal formado, con firma inválida, vencido o con datos inesperados:
            // se trata como si no hubiera token (la petición queda sin autenticar).
            return Optional.empty();
        }
    }

    /** @return vigencia del token de acceso, en segundos */
    public long getAccessTokenSeconds() {
        return accessTokenDuration.toSeconds();
    }
}
