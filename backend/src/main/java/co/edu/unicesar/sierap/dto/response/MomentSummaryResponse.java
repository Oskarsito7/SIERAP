package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDate;

import co.edu.unicesar.sierap.entity.enums.MomentStatus;

/**
 * Resumen de un momento ({@code MomentSummary} en {@code docs/07}).
 *
 * <p>Los indicadores {@code groupFormationOpen}, {@code submissionOpen} y
 * {@code gradingOpen} se calculan con la fecha del {@code Clock} inyectado: el
 * frontend los usa para habilitar o deshabilitar acciones, y el backend vuelve a
 * validarlos en cada operación (ADR-012).</p>
 *
 * @param id                  identificador
 * @param number              número del momento dentro del programa y el periodo
 * @param name                nombre visible, p. ej. "Momento 1"
 * @param subject             asignatura en la que se evalúa
 * @param programId           programa del momento
 * @param programName         nombre del programa
 * @param periodId            periodo académico
 * @param periodCode          código del periodo, p. ej. {@code 2026-2}
 * @param semesterFrom        primer semestre que abarca
 * @param semesterTo          último semestre que abarca
 * @param requirementLevel    nivel de exigencia en texto libre (ADR-010), p. ej. "Bajo"
 * @param minMembers          mínimo de estudiantes por grupo
 * @param maxMembers          máximo de estudiantes por grupo
 * @param groupCount          cantidad de grupos habilitados en el momento
 * @param status              estado del ciclo de vida
 * @param groupFormationStart primer día de la conformación de grupos, o {@code null}
 * @param groupFormationEnd   último día de la conformación de grupos, o {@code null}
 * @param submissionStart     primer día de entregas, o {@code null}
 * @param submissionEnd       último día de entregas, o {@code null}
 * @param gradingStart        primer día de calificación, o {@code null}
 * @param gradingEnd          último día de calificación, o {@code null}
 * @param groupFormationOpen  {@code true} si hoy se pueden conformar grupos
 * @param submissionOpen      {@code true} si hoy se pueden subir entregas
 * @param gradingOpen         {@code true} si hoy se puede calificar
 */
public record MomentSummaryResponse(
        Long id,
        int number,
        String name,
        String subject,
        Long programId,
        String programName,
        Long periodId,
        String periodCode,
        int semesterFrom,
        int semesterTo,
        String requirementLevel,
        int minMembers,
        int maxMembers,
        int groupCount,
        MomentStatus status,
        LocalDate groupFormationStart,
        LocalDate groupFormationEnd,
        LocalDate submissionStart,
        LocalDate submissionEnd,
        LocalDate gradingStart,
        LocalDate gradingEnd,
        boolean groupFormationOpen,
        boolean submissionOpen,
        boolean gradingOpen) {
}
