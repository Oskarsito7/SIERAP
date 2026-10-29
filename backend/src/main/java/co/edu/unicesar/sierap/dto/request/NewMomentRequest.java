package co.edu.unicesar.sierap.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un momento de evaluación en un programa y periodo académico
 * ({@code NewMoment} en {@code docs/07}).
 *
 * @param programId           identificador del programa académico
 * @param periodId            identificador del periodo académico
 * @param number              número del momento (1, 2, 3...)
 * @param name                nombre visible, p. ej. "Momento 1"
 * @param subject             asignatura en la que se evalúa
 * @param semesterFrom        primer semestre que abarca
 * @param semesterTo          último semestre que abarca
 * @param requirementLevel    nivel de exigencia en texto libre (ADR-010), p. ej. "Bajo"
 * @param minMembers          mínimo de integrantes por grupo
 * @param maxMembers          máximo de integrantes por grupo
 * @param groupFormationStart primer día de conformación de grupos, o {@code null}
 * @param groupFormationEnd   último día de conformación de grupos, o {@code null}
 * @param submissionStart     primer día de entregas, o {@code null}
 * @param submissionEnd       último día de entregas, o {@code null}
 * @param gradingStart        primer día de calificación, o {@code null}
 * @param gradingEnd          último día de calificación, o {@code null}
 */
public record NewMomentRequest(
        @NotNull(message = "El programa es obligatorio")
        Long programId,

        @NotNull(message = "El periodo académico es obligatorio")
        Long periodId,

        @NotNull(message = "El número del momento es obligatorio")
        @Min(value = 1, message = "El número del momento debe ser al menos 1")
        Integer number,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String name,

        @NotBlank(message = "La asignatura es obligatoria")
        @Size(max = 150, message = "La asignatura admite hasta 150 caracteres")
        String subject,

        @NotNull(message = "El semestre inicial es obligatorio")
        @Min(value = 1, message = "El semestre inicial debe ser al menos 1")
        Integer semesterFrom,

        @NotNull(message = "El semestre final es obligatorio")
        @Min(value = 1, message = "El semestre final debe ser al menos 1")
        Integer semesterTo,

        @NotBlank(message = "El nivel de exigencia es obligatorio")
        @Size(max = 100, message = "El nivel de exigencia admite hasta 100 caracteres")
        String requirementLevel,

        @NotNull(message = "El mínimo de integrantes es obligatorio")
        @Min(value = 1, message = "El mínimo de integrantes debe ser al menos 1")
        Integer minMembers,

        @NotNull(message = "El máximo de integrantes es obligatorio")
        @Min(value = 1, message = "El máximo de integrantes debe ser al menos 1")
        Integer maxMembers,

        LocalDate groupFormationStart,
        LocalDate groupFormationEnd,
        LocalDate submissionStart,
        LocalDate submissionEnd,
        LocalDate gradingStart,
        LocalDate gradingEnd) {
}
