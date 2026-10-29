package co.edu.unicesar.sierap.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.response.report.ReportData;

/**
 * Reporte académico generado a partir de las calificaciones y evaluaciones del sistema (Etapa 7).
 *
 * <p>Clase base abstracta de la jerarquía polimórfica de reportes. En el Momento 1 los reportes
 * se renderizan en pantalla con gráficas y tablas, por lo que {@link #generate()} devuelve una
 * estructura de datos estructurada {@link ReportData} en lugar de un binario {@code byte[]}.</p>
 */
public abstract class Report {

    private final LocalDateTime generatedAt;
    private final AcademicPeriod period;

    /**
     * Construye un reporte con su marca de tiempo y periodo académico base.
     *
     * @param generatedAt fecha y hora de generación
     * @param period      periodo académico del reporte
     */
    protected Report(LocalDateTime generatedAt, AcademicPeriod period) {
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt no puede ser nulo");
        this.period = Objects.requireNonNull(period, "period no puede ser nulo");
    }

    /**
     * Fecha y hora en que se generó el reporte.
     *
     * @return fecha y hora
     */
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    /**
     * Periodo académico asociado al reporte.
     *
     * @return periodo académico
     */
    public AcademicPeriod getPeriod() {
        return period;
    }

    /**
     * Genera de forma polimórfica los datos estructurados del reporte a partir del modelo de dominio.
     *
     * @return estructura de datos del reporte
     */
    public abstract ReportData generate();
}
