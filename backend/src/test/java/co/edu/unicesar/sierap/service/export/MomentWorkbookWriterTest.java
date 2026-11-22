package co.edu.unicesar.sierap.service.export;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pruebas de la sábana de notas en Excel. */
class MomentWorkbookWriterTest {

    private final MomentWorkbookWriter writer = new MomentWorkbookWriter();

    private XSSFWorkbook write(boolean finalResults) throws IOException {
        byte[] content = writer.write(ExportFixtures.moment(finalResults), List.of(ExportFixtures.group(finalResults)),
                ExportFixtures.SCALE, "Laura Mendoza");
        return new XSSFWorkbook(new ByteArrayInputStream(content));
    }

    @Test
    @DisplayName("Tiene las hojas de resumen, estudiantes y grupos")
    void hasTheThreeSheets() throws IOException {
        try (XSSFWorkbook workbook = write(true)) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);
            assertThat(workbook.getSheetName(0)).isEqualTo("Resumen");
            assertThat(workbook.getSheetName(1)).isEqualTo("Estudiantes");
            assertThat(workbook.getSheetName(2)).isEqualTo("Grupos");
        }
    }

    @Test
    @DisplayName("Una fila por estudiante con su nota como número, su estado y su puntaje por RAP")
    void listsEachStudent() throws IOException {
        try (XSSFWorkbook workbook = write(true)) {
            Sheet students = workbook.getSheet("Estudiantes");
            Row header = students.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Código estudiantil");
            assertThat(header.getCell(9).getStringCellValue()).isEqualTo("RAP1");
            assertThat(students.getLastRowNum()).isEqualTo(3);

            Row ana = students.getRow(1);
            assertThat(ana.getCell(1).getStringCellValue()).isEqualTo("Ana Gómez");
            assertThat(ana.getCell(2).getStringCellValue()).isEqualTo("Grupo 1");
            assertThat(ana.getCell(7).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(ana.getCell(7).getNumericCellValue()).isEqualTo(4.30);
            assertThat(ana.getCell(8).getStringCellValue()).isEqualTo("Aprobado");
            assertThat(ana.getCell(9).getNumericCellValue()).isEqualTo(0.52);

            Row luis = students.getRow(2);
            assertThat(luis.getCell(6).getStringCellValue()).isEqualTo("Sí");
            assertThat(luis.getCell(8).getStringCellValue()).isEqualTo("Reprobado");
            assertThat(students.getRow(3).getCell(8).getStringCellValue()).isEqualTo("Sin nota");
            assertThat(students.getRow(3).getCell(7).getCellType()).isEqualTo(CellType.BLANK);
        }
    }

    @Test
    @DisplayName("El resumen dice si las notas son definitivas o una vista previa y quién lo generó")
    void summaryShowsTheStatus() throws IOException {
        try (XSSFWorkbook preview = write(false); XSSFWorkbook closed = write(true)) {
            assertThat(text(preview.getSheet("Resumen"))).contains("Vista previa", "Laura Mendoza",
                    "Ingeniería de Sistemas (108)", "Nota mínima para aprobar", "RAP1");
            assertThat(text(closed.getSheet("Resumen"))).contains("Notas definitivas");
        }
    }

    @Test
    @DisplayName("La hoja de grupos resume cada grupo")
    void groupsSheet() throws IOException {
        try (XSSFWorkbook workbook = write(true)) {
            Row row = workbook.getSheet("Grupos").getRow(1);
            assertThat(row.getCell(1).getStringCellValue()).isEqualTo("SIERAP");
            assertThat(row.getCell(3).getNumericCellValue()).isEqualTo(3);
            assertThat(row.getCell(4).getNumericCellValue()).isEqualTo(4.00);
        }
    }

    private static String text(Sheet sheet) {
        StringBuilder builder = new StringBuilder();
        sheet.forEach(row -> row.forEach(cell -> builder.append(cell.toString()).append(' ')));
        return builder.toString();
    }
}
