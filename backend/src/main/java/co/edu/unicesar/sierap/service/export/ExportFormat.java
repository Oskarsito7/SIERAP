package co.edu.unicesar.sierap.service.export;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formato de las cifras y fechas en los reportes exportados, igual al de la pantalla:
 * notas con dos decimales y coma ("4,12"), porcentajes con coma y fechas
 * {@code dd/MM/yyyy HH:mm}. Un valor que no existe todavía (sin nota) se muestra como
 * "—".
 */
public final class ExportFormat {

    /** Texto de una cifra que todavía no existe (p. ej. un estudiante sin nota). */
    public static final String EMPTY = "—";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ExportFormat() {
    }

    /**
     * Nota o puntaje con dos decimales y coma decimal.
     *
     * @param value nota, o {@code null}
     * @return p. ej. "4,12", o {@link #EMPTY}
     */
    public static String score(BigDecimal value) {
        if (value == null) {
            return EMPTY;
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /**
     * Porcentaje con un decimal y coma decimal.
     *
     * @param value porcentaje de 0 a 100, o {@code null}
     * @return p. ej. "82,4 %", o {@link #EMPTY}
     */
    public static String percentage(BigDecimal value) {
        if (value == null) {
            return EMPTY;
        }
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + " %";
    }

    /**
     * Fecha y hora de generación.
     *
     * @param value fecha y hora
     * @return p. ej. "22/11/2026 14:05"
     */
    public static String dateTime(LocalDateTime value) {
        return DATE_TIME.format(value);
    }

    /**
     * Parte de un nombre de archivo a partir de un texto: sin tildes, en minúsculas y
     * con guiones, p. ej. "Grupo 1" → "grupo-1".
     *
     * @param text texto libre
     * @return texto seguro para un nombre de archivo
     */
    public static String slug(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "reporte" : slug;
    }

    /**
     * Nombre de archivo seguro: el nombre pasa por {@link #slug(String)} y se conserva la
     * extensión, p. ej. "Sábana 108 2026-2.xlsx" → "sabana-108-2026-2.xlsx".
     *
     * @param fileName nombre con extensión
     * @return nombre sin espacios, tildes ni símbolos
     */
    public static String slugFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return slug(fileName);
        }
        return slug(fileName.substring(0, dot)) + fileName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
