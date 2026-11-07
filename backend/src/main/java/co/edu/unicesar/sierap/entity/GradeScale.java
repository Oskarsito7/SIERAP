package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.exception.BusinessRuleException;

/**
 * Escala de calificación configurable a partir de parámetros globales del sistema (Etapa 7 / RF 12).
 *
 * <p>Define el porcentaje requerido para aprobar ({@code report.passing-percentage}) y los rangos
 * de la distribución de notas ({@code report.grade-bands}), calculando los puntajes mínimos y máximos
 * en función del puntaje máximo del momento ({@link Moment#getMaxScore()}).</p>
 */
public class GradeScale {

    private final BigDecimal passingPercentage;
    private final List<GradeBand> bands;

    /**
     * Construye una escala de calificación con el porcentaje de aprobación y sus rangos ordenados.
     *
     * @param passingPercentage porcentaje mínimo para aprobar (0–100)
     * @param bands             lista ordenada de niveles de desempeño
     */
    public GradeScale(BigDecimal passingPercentage, List<GradeBand> bands) {
        this.passingPercentage = Objects.requireNonNull(passingPercentage, "passingPercentage no puede ser nulo");
        this.bands = List.copyOf(Objects.requireNonNull(bands, "bands no puede ser nulo"));
    }

    /**
     * Nivel o rango de calificación con su nombre y porcentaje de inicio.
     *
     * @param name          nombre del rango (ej. "Bajo", "Básico", "Alto", "Sobresaliente")
     * @param minPercentage porcentaje mínimo para ingresar al rango (0–100)
     */
    public record GradeBand(String name, BigDecimal minPercentage) {
        public GradeBand {
            Objects.requireNonNull(name, "name no puede ser nulo");
            Objects.requireNonNull(minPercentage, "minPercentage no puede ser nulo");
        }
    }

    /**
     * Construye una instancia de {@link GradeScale} parseando los valores de los parámetros globales.
     *
     * @param passingPercentageStr valor en texto de {@code report.passing-percentage} (ej. "60")
     * @param gradeBandsStr        valor en texto de {@code report.grade-bands} (ej. "Bajo:0;Básico:60;Alto:76;Sobresaliente:90")
     * @return escala de calificación configurada
     * @throws BusinessRuleException si falta algún parámetro o su formato es inválido
     */
    public static GradeScale of(String passingPercentageStr, String gradeBandsStr) {
        if (passingPercentageStr == null || passingPercentageStr.isBlank()) {
            throw new BusinessRuleException("Falta configurar el parámetro global 'report.passing-percentage'");
        }
        if (gradeBandsStr == null || gradeBandsStr.isBlank()) {
            throw new BusinessRuleException("Falta configurar el parámetro global 'report.grade-bands'");
        }

        BigDecimal passingPercentage;
        try {
            passingPercentage = new BigDecimal(passingPercentageStr.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("El parámetro global 'report.passing-percentage' debe ser un número válido: "
                    + passingPercentageStr);
        }

        if (passingPercentage.compareTo(BigDecimal.ZERO) < 0 || passingPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessRuleException("El parámetro global 'report.passing-percentage' debe estar entre 0 y 100: "
                    + passingPercentageStr);
        }

        String[] tokens = gradeBandsStr.split(";");
        List<GradeBand> bands = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colonIndex = trimmed.indexOf(':');
            if (colonIndex <= 0 || colonIndex >= trimmed.length() - 1) {
                throw new BusinessRuleException("El parámetro global 'report.grade-bands' tiene un formato inválido: "
                        + gradeBandsStr);
            }
            String name = trimmed.substring(0, colonIndex).trim();
            String pctStr = trimmed.substring(colonIndex + 1).trim();
            BigDecimal pct;
            try {
                pct = new BigDecimal(pctStr);
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("El parámetro global 'report.grade-bands' contiene un porcentaje inválido '"
                        + pctStr + "': " + gradeBandsStr);
            }
            if (pct.compareTo(BigDecimal.ZERO) < 0 || pct.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BusinessRuleException("Los porcentajes de 'report.grade-bands' deben estar entre 0 y 100: "
                        + gradeBandsStr);
            }
            bands.add(new GradeBand(name, pct));
        }

        if (bands.isEmpty()) {
            throw new BusinessRuleException("El parámetro global 'report.grade-bands' no contiene rangos válidos: "
                    + gradeBandsStr);
        }

        bands.sort(Comparator.comparing(GradeBand::minPercentage));

        return new GradeScale(passingPercentage, bands);
    }

    /**
     * Porcentaje mínimo sobre el puntaje máximo necesario para aprobar.
     *
     * @return porcentaje (0–100)
     */
    public BigDecimal getPassingPercentage() {
        return passingPercentage;
    }

    /**
     * Niveles de desempeño configurados, ordenados por porcentaje ascendente.
     *
     * @return lista inmutable de niveles
     */
    public List<GradeBand> getBands() {
        return bands;
    }

    /**
     * Calcula la nota mínima requerida para aprobar en función del puntaje máximo del momento.
     *
     * @param maxScore puntaje máximo del momento
     * @return nota mínima de aprobación con 2 decimales
     */
    public BigDecimal getPassingScore(BigDecimal maxScore) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return maxScore.multiply(passingPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Determina si una calificación dada supera o iguala la nota mínima de aprobación.
     *
     * @param grade    calificación individual del estudiante
     * @param maxScore puntaje máximo del momento
     * @return {@code true} si la nota aprueba
     */
    public boolean isPassing(BigDecimal grade, BigDecimal maxScore) {
        if (grade == null) {
            return false;
        }
        return grade.compareTo(getPassingScore(maxScore)) >= 0;
    }

    /**
     * Distribuye las notas individuales de los estudiantes en los rangos configurados,
     * calculando los límites dinámicamente con base en el puntaje máximo del momento.
     *
     * @param grades      calificaciones de los estudiantes evaluados
     * @param totalGraded total de estudiantes calificados
     * @param maxScore    puntaje máximo del momento
     * @return lista de cubos de distribución con conteo y porcentaje
     */
    public List<MomentReportData.DistributionBucket> calculateDistribution(
            List<BigDecimal> grades,
            int totalGraded,
            BigDecimal maxScore) {

        BigDecimal effectiveMax = (maxScore != null && maxScore.compareTo(BigDecimal.ZERO) > 0)
                ? maxScore
                : BigDecimal.ZERO;

        int n = bands.size();
        List<MomentReportData.DistributionBucket> result = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            GradeBand band = bands.get(i);
            BigDecimal min = effectiveMax.multiply(band.minPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal max;
            if (i == n - 1) {
                max = effectiveMax.setScale(2, RoundingMode.HALF_UP);
            } else {
                BigDecimal nextMin = effectiveMax.multiply(bands.get(i + 1).minPercentage())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                max = nextMin.subtract(new BigDecimal("0.01"));
                if (max.compareTo(min) < 0) {
                    max = min;
                }
            }

            String range = String.format(Locale.ROOT, "%.2f - %.2f", min, max);

            int count = 0;
            for (BigDecimal g : grades) {
                boolean match = g.compareTo(min) >= 0 && g.compareTo(max) <= 0;
                if (i == 0 && g.compareTo(min) < 0) {
                    match = true;
                }
                if (i == n - 1 && g.compareTo(max) > 0) {
                    match = true;
                }
                if (match) {
                    count++;
                }
            }

            BigDecimal pct = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalGraded > 0) {
                pct = BigDecimal.valueOf(count)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalGraded), 2, RoundingMode.HALF_UP);
            }

            result.add(new MomentReportData.DistributionBucket(range, band.name(), min, max, count, pct));
        }

        return result;
    }
}
