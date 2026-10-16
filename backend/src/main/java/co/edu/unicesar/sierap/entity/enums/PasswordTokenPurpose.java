package co.edu.unicesar.sierap.entity.enums;

/**
 * Para qué sirve un enlace enviado por correo (ADR-017).
 */
public enum PasswordTokenPurpose {

    /** Crear la contraseña de una cuenta nueva (vence en 72 horas). */
    ACCOUNT_SETUP,

    /** Recuperar una contraseña olvidada (vence en 1 hora). */
    PASSWORD_RESET
}
