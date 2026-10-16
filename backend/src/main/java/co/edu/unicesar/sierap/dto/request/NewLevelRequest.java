package co.edu.unicesar.sierap.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un nivel de desempeño en un criterio de evaluación.
 *
 * @param name         nombre del nivel (p. ej. "Alto")
 * @param description  descripción del desempeño esperado (opcional)
 * @param score        puntaje otorgado por el nivel (mayor o igual a 0, hasta 2 decimales)
 * @param displayOrder orden de presentación dentro del criterio (opcional; si se omite, se ubica al final)
 */
public record NewLevelRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre admite hasta 50 caracteres")
        String name,

        @Size(max = 1000, message = "La descripción admite hasta 1000 caracteres")
        String description,

        @NotNull(message = "El puntaje es obligatorio")
        @DecimalMin(value = "0.0", message = "El puntaje no puede ser negativo")
        BigDecimal score,

        @Min(value = 1, message = "El orden de presentación debe ser al menos 1")
        Integer displayOrder) {
}
