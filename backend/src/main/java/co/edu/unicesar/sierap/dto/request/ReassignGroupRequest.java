package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Docente al que el admin pasa un grupo a mitad del momento.
 *
 * @param teacherId docente activo de la facultad del programa
 */
public record ReassignGroupRequest(@NotNull(message = "El docente es obligatorio") Long teacherId) {
}
