package co.edu.unicesar.sierap.service.export;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData.MemberOutcomeScore;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData.MemberSummary;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData.OutcomeScore;
import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData.DistributionBucket;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData.OutcomeSummary;
import co.edu.unicesar.sierap.entity.GradeScale;
import org.openpdf.text.Document;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfGState;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPCellEvent;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfPageEventHelper;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

/**
 * Reportes en PDF con el membrete de la UPC (OpenPDF, ADR-020): el informe de un
 * momento, el acta de evaluación de un grupo y el boletín individual de un estudiante.
 *
 * <p>Todos comparten el encabezado (logo, programa, periodo, momento y quién lo generó),
 * el pie con el número de página y, si la calificación del momento sigue abierta, una
 * marca de agua "VISTA PREVIA" en cada página. Las barras de logro por RAP se dibujan
 * como vectores, así que se ven nítidas al imprimir. Solo transforma datos ya
 * calculados por los reportes: no aplica reglas de negocio ni de acceso.</p>
 */
@Component
public class ReportPdfWriter {

    private static final Color UPC_GREEN = new Color(0x10, 0x7B, 0x42);
    private static final Color UPC_GREEN_LIGHT = new Color(0xF3, 0xFA, 0xEF);
    private static final Color GRAY = new Color(0x77, 0x77, 0x77);
    private static final Color BAR_TRACK = new Color(0xEC, 0xEC, 0xEC);

    private static final Font TITLE = new Font(Font.HELVETICA, 16, Font.BOLD, UPC_GREEN);
    private static final Font SUBTITLE = new Font(Font.HELVETICA, 10, Font.NORMAL, GRAY);
    private static final Font SECTION = new Font(Font.HELVETICA, 12, Font.BOLD, UPC_GREEN);
    private static final Font LABEL = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font HEADER = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY);
    private static final Font BIG = new Font(Font.HELVETICA, 18, Font.BOLD, UPC_GREEN);

    private final byte[] logo;

    /** Carga el logo de la UPC que va en el membrete. */
    public ReportPdfWriter() {
        try (InputStream in = ReportPdfWriter.class.getResourceAsStream("/report/upc-logo.png")) {
            if (in == null) {
                throw new IllegalStateException("Falta el logo de la UPC en /report/upc-logo.png");
            }
            this.logo = in.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo leer el logo de la UPC", exception);
        }
    }

    // ------------------------------------------------------------------ documentos

    /**
     * Informe ejecutivo de un momento: indicadores, logro por RAP, distribución de notas
     * y resumen por grupo.
     *
     * @param data        reporte consolidado del momento
     * @param groups      reportes de los grupos que el usuario puede ver
     * @param scale       escala de notas: umbral de aprobación
     * @param generatedBy nombre de quien lo descarga
     * @return contenido del PDF
     */
    public byte[] moment(MomentReportData data, List<GroupReportData> groups, GradeScale scale, String generatedBy) {
        Header header = new Header("Informe del momento", data.program().name(), data.period().code(),
                data.moment().momentNumber() + " · " + data.moment().name(), data.moment().subject(),
                data.generatedAt(), generatedBy, data.finalResults());
        return render(header, document -> {
            section(document, "Indicadores");
            PdfPTable tiles = table(new float[] {1, 1, 1, 1});
            tile(tiles, "Promedio individual", ExportFormat.score(data.averageGrade()),
                    "de " + ExportFormat.score(data.moment().maxScore()));
            tile(tiles, "Aprobados", String.valueOf(data.passingCount()),
                    ExportFormat.percentage(data.passingPercentage()) + " de los calificados");
            tile(tiles, "Estudiantes calificados", data.gradedStudents() + " de " + data.totalStudents(),
                    "Grupos: " + data.gradedGroups() + " de " + data.totalGroups());
            tile(tiles, "Rango de notas", ExportFormat.score(data.minGrade()) + " – "
                    + ExportFormat.score(data.maxGrade()), "Para aprobar: "
                    + ExportFormat.score(scale.getPassingScore(data.moment().maxScore())));
            document.add(tiles);

            section(document, "Logro por RAP");
            PdfPTable outcomes = table(new float[] {1.1f, 5, 1.3f, 3.2f});
            headerRow(outcomes, "RAP", "Descripción", "Promedio", "% de logro");
            for (OutcomeSummary outcome : data.learningOutcomes()) {
                cell(outcomes, outcome.code(), LABEL);
                cell(outcomes, outcome.description(), BODY);
                number(outcomes, ExportFormat.score(outcome.averageScore()) + " / "
                        + ExportFormat.score(outcome.maxScore()));
                bar(outcomes, outcome.percentage());
            }
            document.add(outcomes);

            section(document, "Distribución de notas");
            PdfPTable distribution = table(new float[] {2, 2, 1.3f, 3.2f});
            headerRow(distribution, "Nivel", "Rango", "Estudiantes", "% de los calificados");
            for (DistributionBucket bucket : data.distribution()) {
                cell(distribution, bucket.label(), LABEL);
                cell(distribution, bucket.range(), BODY);
                number(distribution, String.valueOf(bucket.count()));
                bar(distribution, bucket.percentage());
            }
            document.add(distribution);

            section(document, "Grupos");
            if (groups.size() < data.totalGroups()) {
                note(document, (groups.size() == 1 ? "Solo se lista 1 grupo" : "Solo se listan " + groups.size()
                        + " grupos") + " de los " + data.totalGroups()
                        + " del momento: los que usted puede ver. Los indicadores incluyen todos.");
            }
            PdfPTable groupTable = table(new float[] {1.4f, 3.4f, 2.6f, 1.2f, 1.2f, 1.6f});
            headerRow(groupTable, "Grupo", "Proyecto", "Docente", "Nota", "% logro", "Evaluaciones");
            for (GroupReportData group : groups) {
                cell(groupTable, group.group().name(), LABEL);
                cell(groupTable, orDash(group.group().projectTitle()), BODY);
                cell(groupTable, group.group().teacherName(), BODY);
                number(groupTable, ExportFormat.score(group.groupGrade()));
                number(groupTable, ExportFormat.percentage(group.percentage()));
                number(groupTable, group.submittedEvaluations() + " de "
                        + (group.submittedEvaluations() + group.pendingEvaluations()));
            }
            if (groups.isEmpty()) {
                empty(groupTable, 6, "No hay grupos para mostrar.");
            }
            document.add(groupTable);
        });
    }

    /**
     * Acta de evaluación de un grupo: proyecto, nota del grupo, logro por RAP y la nota
     * individual de cada integrante, con espacio para la firma del docente.
     *
     * @param data        reporte del grupo
     * @param scale       escala de notas: umbral de aprobación
     * @param generatedBy nombre de quien lo descarga
     * @return contenido del PDF
     */
    public byte[] group(GroupReportData data, GradeScale scale, String generatedBy) {
        Header header = new Header("Acta de evaluación del grupo", data.program().name(), data.period().code(),
                data.moment().momentNumber() + " · " + data.moment().name(), data.moment().subject(),
                data.generatedAt(), generatedBy, data.finalResults());
        BigDecimal maxScore = data.moment().maxScore();
        return render(header, document -> {
            PdfPTable info = table(new float[] {1.4f, 4});
            pair(info, "Grupo", data.group().name());
            pair(info, "Proyecto", orDash(data.group().projectTitle()));
            pair(info, "Docente", data.group().teacherName());
            pair(info, "Evaluaciones", data.submittedEvaluations() + " enviadas, "
                    + data.pendingEvaluations() + " pendientes");
            document.add(info);

            PdfPTable tiles = table(new float[] {1, 1});
            tile(tiles, "Nota del grupo", ExportFormat.score(data.groupGrade()), "de " + ExportFormat.score(maxScore));
            tile(tiles, "Logro del grupo", ExportFormat.percentage(data.percentage()), "sobre el puntaje máximo");
            document.add(tiles);

            section(document, "Logro del grupo por RAP");
            PdfPTable outcomes = table(new float[] {1.1f, 5, 1.3f, 3.2f});
            headerRow(outcomes, "RAP", "Descripción", "Puntaje", "% de logro");
            for (OutcomeScore outcome : data.learningOutcomes()) {
                cell(outcomes, outcome.code(), LABEL);
                cell(outcomes, outcome.description(), BODY);
                number(outcomes, ExportFormat.score(outcome.score()) + " / " + ExportFormat.score(outcome.maxScore()));
                bar(outcomes, outcome.percentage());
            }
            document.add(outcomes);

            section(document, "Notas individuales");
            List<OutcomeScore> outcomeList = data.learningOutcomes();
            float[] widths = new float[4 + outcomeList.size()];
            widths[0] = 1.6f;
            widths[1] = 3.6f;
            for (int i = 0; i < outcomeList.size(); i++) {
                widths[2 + i] = 1;
            }
            widths[2 + outcomeList.size()] = 1.2f;
            widths[3 + outcomeList.size()] = 1.6f;
            PdfPTable members = table(widths);
            String[] headers = new String[widths.length];
            headers[0] = "Código";
            headers[1] = "Estudiante";
            for (int i = 0; i < outcomeList.size(); i++) {
                headers[2 + i] = outcomeList.get(i).code();
            }
            headers[2 + outcomeList.size()] = "Nota";
            headers[3 + outcomeList.size()] = "Estado";
            headerRow(members, headers);
            for (MemberSummary member : data.members()) {
                cell(members, orDash(member.studentCode()), BODY);
                cell(members, member.fullName() + (member.adjusted() ? " *" : ""), BODY);
                for (OutcomeScore outcome : outcomeList) {
                    number(members, ExportFormat.score(memberScore(member, outcome.id())));
                }
                number(members, ExportFormat.score(member.individualGrade()));
                cell(members, MomentWorkbookWriter.status(member.individualGrade(), maxScore, scale), BODY);
            }
            if (data.members().isEmpty()) {
                empty(members, widths.length, "El grupo no tiene integrantes.");
            }
            document.add(members);
            note(document, "* Un evaluador ajustó la nota de este estudiante en algún criterio.");

            signature(document, data.group().teacherName(), "Docente del grupo");
        });
    }

    /**
     * Boletín individual de desempeño por RAP de un estudiante en un momento.
     *
     * @param data        reporte individual
     * @param scale       escala de notas: umbral de aprobación
     * @param generatedBy nombre de quien lo descarga
     * @return contenido del PDF
     */
    public byte[] individual(IndividualReportData data, GradeScale scale, String generatedBy) {
        Header header = new Header("Boletín de desempeño por RAP", data.program().name(), data.period().code(),
                data.moment().momentNumber() + " · " + data.moment().name(), data.moment().subject(),
                data.generatedAt(), generatedBy, data.finalResults());
        BigDecimal maxScore = data.moment().maxScore();
        return render(header, document -> {
            PdfPTable info = table(new float[] {1.4f, 4});
            pair(info, "Estudiante", data.student().fullName());
            pair(info, "Código estudiantil", orDash(data.student().studentCode()));
            pair(info, "Semestre", String.valueOf(data.student().currentSemester()));
            pair(info, "Grupo", data.group().name() + (data.group().projectTitle() == null ? ""
                    : " · " + data.group().projectTitle()));
            pair(info, "Docente", data.group().teacherName());
            document.add(info);

            PdfPTable tiles = table(new float[] {1, 1, 1});
            tile(tiles, "Nota individual", ExportFormat.score(data.individualGrade()),
                    "de " + ExportFormat.score(maxScore));
            tile(tiles, "Estado", MomentWorkbookWriter.status(data.individualGrade(), maxScore, scale),
                    "Para aprobar: " + ExportFormat.score(scale.getPassingScore(maxScore)));
            tile(tiles, "Logro general", ExportFormat.percentage(data.percentage()),
                    "Nota del grupo: " + ExportFormat.score(data.groupGrade()));
            document.add(tiles);

            section(document, "Desempeño por RAP");
            PdfPTable outcomes = table(new float[] {1.1f, 4.6f, 1.3f, 1.3f, 3});
            headerRow(outcomes, "RAP", "Descripción", "Tu puntaje", "Grupo", "% de logro");
            for (IndividualReportData.OutcomeScore outcome : data.learningOutcomes()) {
                cell(outcomes, outcome.code(), LABEL);
                cell(outcomes, outcome.description(), BODY);
                number(outcomes, ExportFormat.score(outcome.score()) + " / " + ExportFormat.score(outcome.maxScore()));
                number(outcomes, ExportFormat.score(outcome.groupScore()));
                bar(outcomes, outcome.percentage());
            }
            document.add(outcomes);
            note(document, "Evaluaciones que cuentan en la nota: " + data.submittedEvaluations() + " enviadas, "
                    + data.pendingEvaluations() + " pendientes.");
        });
    }

    // ------------------------------------------------------------------ armado del documento

    /** Datos del encabezado común a los tres documentos. */
    private record Header(String title, String program, String period, String moment, String subject,
                          java.time.LocalDateTime generatedAt, String generatedBy, boolean finalResults) {
    }

    private byte[] render(Header header, Consumer<Document> body) {
        Document document = new Document(PageSize.A4, 40, 40, 40, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = PdfWriter.getInstance(document, out);
        writer.setPageEvent(new PageDecorator(header));
        document.addTitle(header.title() + " · " + header.moment());
        document.addCreator("SIERAP · Universidad Popular del Cesar");
        document.open();
        letterhead(document, header);
        body.accept(document);
        document.close();
        return out.toByteArray();
    }

    private void letterhead(Document document, Header header) {
        PdfPTable top = table(new float[] {1.3f, 2});
        PdfPCell logoCell;
        try {
            Image image = Image.getInstance(logo);
            image.scaleToFit(150, 53);
            logoCell = new PdfPCell(image, false);
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo cargar el logo de la UPC", exception);
        }
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        top.addCell(logoCell);

        Paragraph right = new Paragraph();
        right.add(new Phrase(header.title() + "\n", TITLE));
        right.add(new Phrase("SIERAP · Evaluación de resultados de aprendizaje", SUBTITLE));
        PdfPCell rightCell = new PdfPCell(right);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        top.addCell(rightCell);
        document.add(top);

        PdfPTable meta = table(new float[] {1, 2.2f, 1, 2.2f});
        meta.setSpacingBefore(10);
        metaPair(meta, "Programa", header.program());
        metaPair(meta, "Periodo", header.period());
        metaPair(meta, "Momento", header.moment());
        metaPair(meta, "Asignatura", header.subject());
        metaPair(meta, "Estado", header.finalResults() ? "Notas definitivas" : "Vista previa (calificación abierta)");
        metaPair(meta, "Generado", ExportFormat.dateTime(header.generatedAt()));
        document.add(meta);
    }

    /** Pie con la página y, mientras la calificación siga abierta, la marca de agua. */
    private static final class PageDecorator extends PdfPageEventHelper {
        private final Header header;

        PageDecorator(Header header) {
            this.header = header;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Rectangle page = document.getPageSize();
            String footer = "SIERAP · Universidad Popular del Cesar · Generado el "
                    + ExportFormat.dateTime(header.generatedAt()) + " por " + header.generatedBy();
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, new Phrase(footer, SMALL),
                    document.left(), 25, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), SMALL), document.right(), 25, 0);

            if (!header.finalResults()) {
                PdfContentByte under = writer.getDirectContentUnder();
                under.saveState();
                PdfGState transparent = new PdfGState();
                transparent.setFillOpacity(0.12f);
                under.setGState(transparent);
                ColumnText.showTextAligned(under, Element.ALIGN_CENTER,
                        new Phrase("VISTA PREVIA", new Font(Font.HELVETICA, 72, Font.BOLD, UPC_GREEN)),
                        page.getWidth() / 2, page.getHeight() / 2 + 30, 45);
                ColumnText.showTextAligned(under, Element.ALIGN_CENTER,
                        new Phrase("CALIFICACIÓN ABIERTA", new Font(Font.HELVETICA, 28, Font.BOLD, UPC_GREEN)),
                        page.getWidth() / 2 + 40, page.getHeight() / 2 - 40, 45);
                under.restoreState();
            }
        }
    }

    // ------------------------------------------------------------------ piezas

    private static PdfPTable table(float[] widths) {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        return table;
    }

    private static void section(Document document, String title) {
        Paragraph paragraph = new Paragraph(title, SECTION);
        paragraph.setSpacingBefore(14);
        paragraph.setSpacingAfter(2);
        document.add(paragraph);
    }

    private static void note(Document document, String text) {
        Paragraph paragraph = new Paragraph(text, SMALL);
        paragraph.setSpacingBefore(4);
        document.add(paragraph);
    }

    private static void headerRow(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER));
            cell.setBackgroundColor(UPC_GREEN);
            cell.setBorderColor(UPC_GREEN);
            cell.setPadding(5);
            table.addCell(cell);
        }
        table.setHeaderRows(1);
    }

    private static void cell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BAR_TRACK);
        table.addCell(cell);
    }

    private static void number(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY));
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BAR_TRACK);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cell);
    }

    private static void empty(PdfPTable table, int columns, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, SMALL));
        cell.setColspan(columns);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        table.addCell(cell);
    }

    private static void pair(PdfPTable table, String label, String value) {
        cell(table, label, LABEL);
        cell(table, value, BODY);
    }

    private static void metaPair(PdfPTable table, String label, String value) {
        PdfPCell name = new PdfPCell(new Phrase(label, LABEL));
        name.setBackgroundColor(UPC_GREEN_LIGHT);
        name.setBorder(Rectangle.NO_BORDER);
        name.setPadding(4);
        table.addCell(name);
        PdfPCell content = new PdfPCell(new Phrase(value, BODY));
        content.setBackgroundColor(UPC_GREEN_LIGHT);
        content.setBorder(Rectangle.NO_BORDER);
        content.setPadding(4);
        table.addCell(content);
    }

    /** Cifra destacada: nombre, valor grande y un detalle. */
    private static void tile(PdfPTable table, String label, String value, String detail) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(new Paragraph(11, label, SMALL));
        // Interlineado propio para que la cifra grande no se monte sobre el detalle.
        cell.addElement(new Paragraph(24, value, BIG));
        cell.addElement(new Paragraph(11, detail, SMALL));
        cell.setBorderColor(BAR_TRACK);
        cell.setPadding(8);
        cell.setPaddingTop(2);
        table.addCell(cell);
    }

    /** Celda con una barra horizontal proporcional al porcentaje y su valor al lado. */
    private static void bar(PdfPTable table, BigDecimal percentage) {
        PdfPCell cell = new PdfPCell(new Phrase(ExportFormat.percentage(percentage), BODY));
        cell.setPadding(5);
        cell.setPaddingLeft(5);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BAR_TRACK);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setCellEvent(new BarEvent(percentage));
        table.addCell(cell);
    }

    /** Dibuja la barra de una celda: el riel gris y la parte lograda en verde. */
    private record BarEvent(BigDecimal percentage) implements PdfPCellEvent {
        @Override
        public void cellLayout(PdfPCell cell, Rectangle position, PdfContentByte[] canvases) {
            PdfContentByte canvas = canvases[PdfPTable.BACKGROUNDCANVAS];
            float left = position.getLeft() + 5;
            float width = (position.getWidth() - 10) * 0.62f;
            float height = 7;
            float bottom = position.getBottom() + (position.getHeight() - height) / 2;
            canvas.saveState();
            canvas.setColorFill(BAR_TRACK);
            canvas.rectangle(left, bottom, width, height);
            canvas.fill();
            if (percentage != null && percentage.signum() > 0) {
                float filled = width * Math.min(1f, percentage.floatValue() / 100f);
                canvas.setColorFill(UPC_GREEN);
                canvas.rectangle(left, bottom, filled, height);
                canvas.fill();
            }
            canvas.restoreState();
        }
    }

    private static void signature(Document document, String name, String role) {
        PdfPTable table = table(new float[] {1, 1});
        table.setSpacingBefore(46);
        PdfPCell line = new PdfPCell(new Phrase(name + "\n" + role, BODY));
        line.setBorder(Rectangle.TOP);
        line.setBorderColor(GRAY);
        line.setPaddingTop(4);
        table.addCell(line);
        PdfPCell blank = new PdfPCell(new Phrase(""));
        blank.setBorder(Rectangle.NO_BORDER);
        table.addCell(blank);
        document.add(table);
    }

    private static BigDecimal memberScore(MemberSummary member, Long outcomeId) {
        return member.learningOutcomes().stream()
                .filter(score -> score.id().equals(outcomeId))
                .map(MemberOutcomeScore::score)
                .findFirst()
                .orElse(null);
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? ExportFormat.EMPTY : value;
    }
}
