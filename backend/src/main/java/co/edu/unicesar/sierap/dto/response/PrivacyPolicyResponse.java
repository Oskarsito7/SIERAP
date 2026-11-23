package co.edu.unicesar.sierap.dto.response;

/**
 * Enlace a la política de tratamiento de datos personales (Ley 1581 de 2012).
 *
 * @param url dirección {@code https://} de la política, o {@code null} si el
 *            super_admin aún no configuró el parámetro {@code privacy.policy-url}
 */
public record PrivacyPolicyResponse(String url) {
}
