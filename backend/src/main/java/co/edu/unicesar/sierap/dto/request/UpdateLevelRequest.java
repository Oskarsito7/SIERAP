package co.edu.unicesar.sierap.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Modificación parcial de un nivel de desempeño.
 *
 * @param name         nuevo nombre del nivel, o {@code null} si se omite
 * @param description  nueva descripción del desempeño esperado, o {@code null} si se omite
 * @param score        nuevo puntaje del nivel, o {@code null} si se omite
 * @param displayOrder nuevo orden de presentación dentro del criterio, o {@code null} si se omite
 */
public record UpdateLevelRequest(
        @Size(max = 50, message = "El nombre admite hasta 50 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción admite hasta 1000 caracteres")
        String description,

        @DecimalMin(value = "0.0", message = "El puntaje no puede ser negativo")
        BigDecimal score,

        @Min(value = 1, message = "El orden de presentación debe ser al menos 1")
        Integer displayOrder) {
}
