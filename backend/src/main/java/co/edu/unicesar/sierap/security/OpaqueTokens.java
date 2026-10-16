package co.edu.unicesar.sierap.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Utilidades para los tokens opacos (renovación de sesión y enlaces por correo).
 *
 * <p>El valor aleatorio viaja al usuario (cookie o enlace) y en la base de datos solo
 * se guarda su hash SHA-256: aunque alguien lea la base, no puede reconstruir el
 * token (ADR-017).</p>
 */
public final class OpaqueTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Bytes aleatorios de cada token (256 bits). */
    private static final int TOKEN_BYTES = 32;

    private OpaqueTokens() {
    }

    /**
     * Genera un token aleatorio apto para URL y cookies.
     *
     * @return token en Base64 URL-safe, sin relleno
     */
    public static String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Calcula el hash que se guarda en la base de datos.
     *
     * @param token valor del token
     * @return hash SHA-256 en hexadecimal (64 caracteres)
     */
    public static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            // SHA-256 es obligatorio en toda JVM; si falta, la plataforma está rota.
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }
}
