package co.edu.unicesar.sierap.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;

import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData.MemberOutcomeScore;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData.MemberSummary;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData.DistributionBucket;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData.OutcomeSummary;
import co.edu.unicesar.sierap.entity.GradeScale;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Sábana de notas de un momento en Excel ({@code .xlsx}, Apache POI, ADR-020), para
 * cargar las notas en la plataforma académica y para los informes de autoevaluación.
 * Tiene tres hojas:
 *
 * <ol>
 *   <li><b>Resumen:</b> datos del momento, indicadores, promedio por RAP y distribución.</li>
 *   <li><b>Estudiantes:</b> una fila por estudiante con su grupo, la nota del grupo, si tuvo
 *       ajuste individual, su nota, si aprobó y su puntaje en cada RAP.</li>
 *   <li><b>Grupos:</b> una fila por grupo con su nota y el estado de sus evaluaciones.</li>
 * </ol>
 *
 * <p>Las notas van como números (con formato {@code 0.00}) para que se puedan ordenar y
 * calcular en Excel. Solo transforma datos ya calculados por los reportes: no aplica
 * reglas de negocio ni de acceso.</p>
 */
@Component
public class MomentWorkbookWriter {

    private static final XSSFColor UPC_GREEN = new XSSFColor(new byte[] {0x10, 0x7B, 0x42});

    /**
     * Genera la sábana de notas.
     *
     * @param summary     reporte consolidado del momento (todas las cifras del momento)
     * @param groups      reportes de los grupos que el usuario puede ver, en orden
     * @param scale       escala de notas: umbral de aprobación
     * @param generatedBy nombre de quien descarga el archivo
     * @return contenido del archivo {@code .xlsx}
     */
    public byte[] write(MomentReportData summary, List<GroupReportData> groups, GradeScale scale,
                        String generatedBy) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            writeSummary(workbook.createSheet("Resumen"), styles, summary, groups.size(), scale, generatedBy);
            writeStudents(workbook.createSheet("Estudiantes"), styles, summary, groups, scale);
            writeGroups(workbook.createSheet("Grupos"), styles, groups);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo generar la sábana de notas", exception);
        }
    }

    // ------------------------------------------------------------------ hojas

    private void writeSummary(Sheet sheet, Styles styles, MomentReportData data, int detailedGroups,
                              GradeScale scale, String generatedBy) {
        Writer w = new Writer(sheet, styles);
        w.title("Universidad Popular del Cesar · SIERAP");
        w.subtitle("Sábana de notas del momento");
        w.blank();
        w.pair("Programa", data.program().name() + " (" + data.program().code() + ")");
        w.pair("Periodo académico", data.period().code());
        w.pair("Momento", data.moment().momentNumber() + " · " + data.moment().name());
        w.pair("Asignatura", data.moment().subject());
        w.pairNumber("Puntaje máximo", data.moment().maxScore());
        w.pairNumber("Nota mínima para aprobar", scale.getPassingScore(data.moment().maxScore()));
        w.pair("Estado", data.finalResults()
                ? "Notas definitivas"
                : "Vista previa: la calificación sigue abierta y las cifras pueden cambiar");
        w.pair("Generado", ExportFormat.dateTime(data.generatedAt()) + " por " + generatedBy);
        w.pair("Grupos con detalle", detailedGroups + " de " + data.totalGroups()
                + (detailedGroups < data.totalGroups() ? " (solo los grupos que usted puede ver)" : ""));
        w.blank();

        w.section("Indicadores");
        w.pair("Grupos calificados", data.gradedGroups() + " de " + data.totalGroups());
        w.pair("Estudiantes calificados", data.gradedStudents() + " de " + data.totalStudents());
        w.pairNumber("Promedio de los grupos", data.averageGroupGrade());
        w.pairNumber("Promedio individual", data.averageGrade());
        w.pairNumber("Nota más baja", data.minGrade());
        w.pairNumber("Nota más alta", data.maxGrade());
        w.pair("Aprobados", data.passingCount() + " (" + ExportFormat.percentage(data.passingPercentage()) + ")");
        w.blank();

        w.section("Logro por RAP");
        w.header("RAP", "Descripción", "Promedio", "Máximo", "% de logro");
        for (OutcomeSummary outcome : data.learningOutcomes()) {
            Row row = w.row();
            w.text(row, 0, outcome.code());
            w.text(row, 1, outcome.description());
            w.number(row, 2, outcome.averageScore());
            w.number(row, 3, outcome.maxScore());
            w.number(row, 4, outcome.percentage());
        }
        w.blank();

        w.section("Distribución de notas");
        w.header("Nivel", "Rango", "Estudiantes", "% de los calificados");
        for (DistributionBucket bucket : data.distribution()) {
            Row row = w.row();
            w.text(row, 0, bucket.label());
            w.text(row, 1, bucket.range());
            w.integer(row, 2, bucket.count());
            w.number(row, 3, bucket.percentage());
        }

        sheet.setColumnWidth(0, 28 * 256);
        sheet.setColumnWidth(1, 70 * 256);
        for (int column = 2; column <= 4; column++) {
            sheet.setColumnWidth(column, 18 * 256);
        }
    }

    private void writeStudents(Sheet sheet, Styles styles, MomentReportData summary, List<GroupReportData> groups,
                               GradeScale scale) {
        Writer w = new Writer(sheet, styles);
        List<OutcomeSummary> outcomes = summary.learningOutcomes();
        String[] fixed = {"Código estudiantil", "Estudiante", "Grupo", "Proyecto", "Docente", "Nota del grupo",
            "Ajuste individual", "Nota individual", "Estado"};
        String[] headers = new String[fixed.length + outcomes.size()];
        System.arraycopy(fixed, 0, headers, 0, fixed.length);
        for (int i = 0; i < outcomes.size(); i++) {
            headers[fixed.length + i] = outcomes.get(i).code();
        }
        w.header(headers);
        BigDecimal maxScore = summary.moment().maxScore();

        for (GroupReportData group : groups) {
            for (MemberSummary member : group.members()) {
                Row row = w.row();
                w.text(row, 0, member.studentCode());
                w.text(row, 1, member.fullName());
                w.text(row, 2, group.group().name());
                w.text(row, 3, group.group().projectTitle());
                w.text(row, 4, group.group().teacherName());
                w.number(row, 5, group.groupGrade());
                w.text(row, 6, member.adjusted() ? "Sí" : "No");
                w.number(row, 7, member.individualGrade());
                w.text(row, 8, status(member.individualGrade(), maxScore, scale));
                for (int i = 0; i < outcomes.size(); i++) {
                    w.number(row, fixed.length + i, scoreOf(member, outcomes.get(i).id()));
                }
            }
        }
        w.finishTable(headers.length);
        int[] widths = {18, 32, 14, 32, 26, 14, 16, 15, 12};
        for (int column = 0; column < headers.length; column++) {
            sheet.setColumnWidth(column, (column < widths.length ? widths[column] : 10) * 256);
        }
    }

    private void writeGroups(Sheet sheet, Styles styles, List<GroupReportData> groups) {
        Writer w = new Writer(sheet, styles);
        String[] headers = {"Grupo", "Proyecto", "Docente", "Integrantes", "Nota del grupo", "% de logro",
            "Evaluaciones enviadas", "Evaluaciones pendientes"};
        w.header(headers);
        for (GroupReportData group : groups) {
            Row row = w.row();
            w.text(row, 0, group.group().name());
            w.text(row, 1, group.group().projectTitle());
            w.text(row, 2, group.group().teacherName());
            w.integer(row, 3, group.members().size());
            w.number(row, 4, group.groupGrade());
            w.number(row, 5, group.percentage());
            w.integer(row, 6, group.submittedEvaluations());
            w.integer(row, 7, group.pendingEvaluations());
        }
        w.finishTable(headers.length);
        int[] widths = {14, 36, 26, 12, 15, 12, 22, 24};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, widths[column] * 256);
        }
    }

    /** "Aprobado", "Reprobado" o "Sin nota", con el umbral de los parámetros globales. */
    static String status(BigDecimal grade, BigDecimal maxScore, GradeScale scale) {
        if (grade == null) {
            return "Sin nota";
        }
        return scale.isPassing(grade, maxScore) ? "Aprobado" : "Reprobado";
    }

    private static BigDecimal scoreOf(MemberSummary member, Long outcomeId) {
        return member.learningOutcomes().stream()
                .filter(score -> score.id().equals(outcomeId))
                .map(MemberOutcomeScore::score)
                .findFirst()
                .orElse(null);
    }

    // ------------------------------------------------------------------ utilidades

    /** Estilos de celda compartidos por las tres hojas (POI limita cuántos se crean). */
    private static final class Styles {
        private final CellStyle title;
        private final CellStyle subtitle;
        private final CellStyle section;
        private final CellStyle label;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle number;

        Styles(XSSFWorkbook workbook) {
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            title = workbook.createCellStyle();
            title.setFont(titleFont);

            Font subtitleFont = workbook.createFont();
            subtitleFont.setFontHeightInPoints((short) 12);
            subtitle = workbook.createCellStyle();
            subtitle.setFont(subtitleFont);

            Font bold = workbook.createFont();
            bold.setBold(true);
            section = workbook.createCellStyle();
            section.setFont(bold);
            label = workbook.createCellStyle();
            label.setFont(bold);

            Font white = workbook.createFont();
            white.setBold(true);
            white.setColor(IndexedColors.WHITE.getIndex());
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(white);
            headerStyle.setFillForegroundColor(UPC_GREEN);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setWrapText(true);
            header = headerStyle;

            text = workbook.createCellStyle();
            text.setBorderBottom(BorderStyle.HAIR);

            number = workbook.createCellStyle();
            number.setBorderBottom(BorderStyle.HAIR);
            number.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        }
    }

    /** Escribe una hoja fila por fila, recordando dónde empieza la tabla. */
    private static final class Writer {
        private final Sheet sheet;
        private final Styles styles;
        private int next;
        private int headerRow = -1;

        Writer(Sheet sheet, Styles styles) {
            this.sheet = sheet;
            this.styles = styles;
            // Al imprimir: hoja horizontal y todas las columnas en el ancho de una página.
            sheet.getPrintSetup().setLandscape(true);
            sheet.setFitToPage(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.getPrintSetup().setFitHeight((short) 0);
        }

        Row row() {
            return sheet.createRow(next++);
        }

        void blank() {
            next++;
        }

        void title(String value) {
            styled(row(), 0, value, styles.title);
        }

        void subtitle(String value) {
            styled(row(), 0, value, styles.subtitle);
        }

        void section(String value) {
            styled(row(), 0, value, styles.section);
        }

        void pair(String name, String value) {
            Row row = row();
            styled(row, 0, name, styles.label);
            text(row, 1, value);
        }

        void pairNumber(String name, BigDecimal value) {
            Row row = row();
            styled(row, 0, name, styles.label);
            number(row, 1, value);
        }

        void header(String... values) {
            headerRow = next;
            Row row = row();
            for (int i = 0; i < values.length; i++) {
                styled(row, i, values[i], styles.header);
            }
        }

        /** Congela el encabezado y le pone filtros a la tabla de la hoja. */
        void finishTable(int columns) {
            sheet.createFreezePane(0, headerRow + 1);
            int last = Math.max(next - 1, headerRow);
            sheet.setAutoFilter(new CellRangeAddress(headerRow, last, 0, columns - 1));
        }

        void text(Row row, int column, String value) {
            styled(row, column, value == null ? "" : value, styles.text);
        }

        void number(Row row, int column, BigDecimal value) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(styles.number);
            if (value != null) {
                // Excel solo guarda números en coma flotante: la nota ya viene redondeada a
                // dos decimales (BigDecimal, ADR-013) y el formato "0.00" la muestra igual.
                cell.setCellValue(value.doubleValue());
            }
        }

        void integer(Row row, int column, int value) {
            Cell cell = row.createCell(column);
            cell.setCellStyle(styles.text);
            cell.setCellValue(value);
        }

        private static void styled(Row row, int column, String value, CellStyle style) {
            Cell cell = row.createCell(column);
            cell.setCellValue(value);
            cell.setCellStyle(style);
        }
    }
}
