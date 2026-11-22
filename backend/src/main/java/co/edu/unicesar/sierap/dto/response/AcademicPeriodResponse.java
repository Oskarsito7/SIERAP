package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDate;

/**
 * Periodo académico con su estado temporal calculado ({@code AcademicPeriod} en {@code docs/07}).
 *
 * @param id                    identificador del periodo
 * @param code                  código del periodo (p. ej. {@code 2026-2})
 * @param startDate             fecha de inicio
 * @param endDate               fecha de finalización
 * @param upcEvaluationDeadline fecha límite de evaluación de la UPC
 * @param status                estado temporal: {@code UPCOMING}, {@code CURRENT} o {@code CLOSED}
 */
public record AcademicPeriodResponse(
        Long id,
        String code,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate upcEvaluationDeadline,
        String status) {
}
