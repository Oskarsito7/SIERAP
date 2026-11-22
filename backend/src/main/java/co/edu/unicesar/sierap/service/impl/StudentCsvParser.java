package co.edu.unicesar.sierap.service.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import co.edu.unicesar.sierap.exception.BusinessRuleException;

/**
 * Lector del archivo CSV de la carga masiva de estudiantes.
 *
 * <p>Formato: texto ya decodificado en UTF-8 (se quita el BOM si lo trae), separador
 * {@code ,} o {@code ;} (se detecta en la cabecera, porque Excel en español usa
 * {@code ;}), campos opcionalmente entre comillas dobles con {@code ""} para una comilla
 * literal (dentro de las comillas caben separadores y saltos de línea). La cabecera es
 * obligatoria, sin importar mayúsculas, tildes ni orden de las columnas; las filas
 * vacías se ignoran. Esta clase solo <b>lee</b>: validar el contenido de cada fila es
 * trabajo de {@link StudentImportValidator}.</p>
 */
public final class StudentCsvParser {

    /** Columnas obligatorias de la cabecera, en el orden de la plantilla. */
    public static final List<String> COLUMNS = List.of("nombres", "apellidos", "tipo_documento",
            "numero_documento", "correo", "codigo_estudiantil", "semestre");

    /** Máximo de estudiantes por archivo. */
    public static final int MAX_ROWS = 500;

    private StudentCsvParser() {
    }

    /**
     * Fila del archivo tal como vino (sin interpretar).
     *
     * @param row            número de línea del archivo donde empieza la fila (la cabecera es la 1)
     * @param firstNames     nombres
     * @param lastNames      apellidos
     * @param documentType   tipo de documento
     * @param documentNumber número de documento
     * @param email          correo
     * @param studentCode    código estudiantil
     * @param semester       semestre
     */
    public record RawRow(int row, String firstNames, String lastNames, String documentType,
                         String documentNumber, String email, String studentCode, String semester) {
    }

    /**
     * Lee el contenido del archivo.
     *
     * @param content texto del archivo
     * @return filas con datos, en orden
     * @throws BusinessRuleException si falta la cabecera o alguna columna, no hay filas,
     *                               hay más de {@link #MAX_ROWS} o hay comillas sin cerrar
     */
    public static List<RawRow> parse(String content) {
        String text = content.startsWith("﻿") ? content.substring(1) : content;
        List<CsvRecord> records = tokenize(text, detectDelimiter(text));
        if (records.isEmpty()) {
            throw new BusinessRuleException("El archivo no tiene estudiantes.");
        }
        Map<String, Integer> columns = mapHeader(records.get(0));
        List<RawRow> rows = new ArrayList<>();
        for (CsvRecord record : records.subList(1, records.size())) {
            if (record.fields().stream().allMatch(String::isBlank)) {
                continue;
            }
            rows.add(new RawRow(record.line(),
                    field(record, columns, "nombres"), field(record, columns, "apellidos"),
                    field(record, columns, "tipo_documento"), field(record, columns, "numero_documento"),
                    field(record, columns, "correo"), field(record, columns, "codigo_estudiantil"),
                    field(record, columns, "semestre")));
        }
        if (rows.isEmpty()) {
            throw new BusinessRuleException("El archivo no tiene estudiantes.");
        }
        if (rows.size() > MAX_ROWS) {
            throw new BusinessRuleException("El archivo tiene " + rows.size() + " estudiantes; el máximo es "
                    + MAX_ROWS + " por archivo.");
        }
        return rows;
    }

    /** Registro del archivo: línea donde empieza y sus campos. */
    private record CsvRecord(int line, List<String> fields) {
    }

    private static String field(CsvRecord record, Map<String, Integer> columns, String name) {
        int index = columns.get(name);
        return index < record.fields().size() ? record.fields().get(index).trim() : "";
    }

    private static Map<String, Integer> mapHeader(CsvRecord header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.fields().size(); i++) {
            columns.putIfAbsent(normalizeHeader(header.fields().get(i)), i);
        }
        List<String> missing = COLUMNS.stream().filter(name -> !columns.containsKey(name)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("Faltan columnas en la cabecera: " + String.join(", ", missing)
                    + ". La cabecera debe ser: " + String.join(",", COLUMNS) + ".");
        }
        return columns;
    }

    private static String normalizeHeader(String name) {
        String plain = Normalizer.normalize(name.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return plain.toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    /** Elige {@code ;} si la primera línea con contenido tiene más punto y coma que comas (fuera de comillas). */
    private static char detectDelimiter(String text) {
        int commas = 0;
        int semicolons = 0;
        boolean quoted = false;
        boolean hasContent = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                hasContent = true;
            } else if (!quoted && (c == '\n' || c == '\r')) {
                if (hasContent) {
                    break;
                }
            } else if (!quoted && c == ',') {
                commas++;
                hasContent = true;
            } else if (!quoted && c == ';') {
                semicolons++;
                hasContent = true;
            } else if (!Character.isWhitespace(c)) {
                hasContent = true;
            }
        }
        return semicolons > commas ? ';' : ',';
    }

    /** Separa el texto en registros y campos respetando comillas; ignora las líneas completamente vacías. */
    private static List<CsvRecord> tokenize(String text, char delimiter) {
        List<CsvRecord> records = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean fieldWasQuoted = false;
        int line = 1;
        int recordLine = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    current.append(c);
                }
            } else if (c == '"' && current.toString().isBlank()) {
                quoted = true;
                fieldWasQuoted = true;
                current.setLength(0);
            } else if (c == delimiter) {
                fields.add(current.toString());
                current.setLength(0);
                fieldWasQuoted = false;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                addRecord(records, fields, current, fieldWasQuoted, recordLine);
                fieldWasQuoted = false;
                line++;
                recordLine = line;
            } else {
                current.append(c);
            }
        }
        if (quoted) {
            throw new BusinessRuleException("El archivo tiene un campo entre comillas sin cerrar (desde la línea "
                    + recordLine + ").");
        }
        addRecord(records, fields, current, fieldWasQuoted, recordLine);
        return records;
    }

    private static void addRecord(List<CsvRecord> records, List<String> fields, StringBuilder current,
                                  boolean fieldWasQuoted, int recordLine) {
        if (fields.isEmpty() && current.toString().isBlank() && !fieldWasQuoted) {
            return;
        }
        fields.add(current.toString());
        records.add(new CsvRecord(recordLine, new ArrayList<>(fields)));
        fields.clear();
        current.setLength(0);
    }
}
