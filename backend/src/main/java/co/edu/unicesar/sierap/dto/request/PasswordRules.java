package co.edu.unicesar.sierap.dto.request;

/**
 * Reglas de las contraseñas (decisión del líder, ADR-017): al menos 8 caracteres,
 * con al menos una letra y un número. Son constantes para poder usarlas en
 * {@code @Pattern} de los DTO.
 */
public final class PasswordRules {

    /** Expresión regular de una contraseña válida. */
    public static final String REGEX = "^(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\\d).{8,}$";

    /** Mensaje de validación que ve el usuario. */
    public static final String MESSAGE =
            "La contraseña debe tener al menos 8 caracteres, con al menos una letra y un número.";

    private PasswordRules() {
    }
}
