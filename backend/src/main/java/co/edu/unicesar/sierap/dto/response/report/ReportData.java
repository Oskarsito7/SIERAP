package co.edu.unicesar.sierap.dto.response.report;

import java.time.LocalDateTime;

/**
 * Interfaz sellada para los datos estructurados generados por los reportes del sistema (Etapa 7).
 * Cada subclase de {@code Report} produce de forma polimórfica su implementación concreta.
 */
public sealed interface ReportData permits
        IndividualReportData,
        GroupReportData,
        MomentReportData,
        ComparativeReportData {

    /**
     * Marca de tiempo de generación del reporte.
     *
     * @return fecha y hora en que se computó el reporte
     */
    LocalDateTime generatedAt();
}
