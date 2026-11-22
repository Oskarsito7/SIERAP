package co.edu.unicesar.sierap.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

/** Pruebas de los reportes en PDF: se extrae el texto y se revisa lo esencial. */
class ReportPdfWriterTest {

    private final ReportPdfWriter writer = new ReportPdfWriter();

    @Test
    @DisplayName("El informe del momento lleva el membrete, los indicadores, los RAP y los grupos")
    void momentReport() throws IOException {
        byte[] pdf = writer.moment(ExportFixtures.moment(true), List.of(ExportFixtures.group(true)),
                ExportFixtures.SCALE, "Laura Mendoza");

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        String text = text(pdf);
        assertThat(text).contains("Informe del momento", "Ingeniería de Sistemas", "2026-2", "Programación II",
                "Notas definitivas", "Promedio individual", "3,55", "RAP1", "80,0 %", "Grupo 1", "Laura Mendoza");
        assertThat(text).doesNotContain("VISTA PREVIA");
    }

    @Test
    @DisplayName("Mientras la calificación sigue abierta, el PDF lleva la marca de vista previa")
    void previewWatermark() throws IOException {
        byte[] pdf = writer.moment(ExportFixtures.moment(false), List.of(), ExportFixtures.SCALE, "Laura Mendoza");

        assertThat(text(pdf)).contains("VISTA PREVIA", "Vista previa (calificación abierta)",
                "No hay grupos para mostrar.");
    }

    @Test
    @DisplayName("El acta del grupo lista a cada integrante con su nota, su estado y la firma del docente")
    void groupMinutes() throws IOException {
        String text = text(writer.group(ExportFixtures.group(true), ExportFixtures.SCALE, "Marta Díaz"));

        assertThat(text).contains("Acta de evaluación del grupo", "SIERAP", "Ana Gómez", "4,30", "Aprobado",
                "Luis Martínez *", "Reprobado", "Sin nota", "Docente del grupo");
    }

    @Test
    @DisplayName("El boletín muestra la nota, si aprobó y el desempeño por RAP del estudiante")
    void individualBulletin() throws IOException {
        String text = text(writer.individual(ExportFixtures.individual(true), ExportFixtures.SCALE, "Ana Gómez"));

        assertThat(text).contains("Boletín de desempeño por RAP", "Ana Gómez", "2026001", "4,30", "Aprobado",
                "Para aprobar: 3,00", "Diseña algoritmos", "86,7 %");
    }

    private static String text(byte[] pdf) throws IOException {
        PdfReader reader = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder builder = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                builder.append(extractor.getTextFromPage(page)).append('\n');
            }
            return builder.toString();
        } finally {
            reader.close();
        }
    }
}
