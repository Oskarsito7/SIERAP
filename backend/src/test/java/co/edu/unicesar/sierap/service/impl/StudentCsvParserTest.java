package co.edu.unicesar.sierap.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.service.impl.StudentCsvParser.RawRow;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas unitarias de {@link StudentCsvParser}: separadores, comillas, BOM, cabecera y límites.
 */
class StudentCsvParserTest {

    private static final String HEADER = "nombres,apellidos,tipo_documento,numero_documento,correo,"
            + "codigo_estudiantil,semestre";

    @Test
    void readsRowsWithCommaSeparatorAndTracksLineNumbers() {
        List<RawRow> rows = StudentCsvParser.parse(HEADER + "\n"
                + "Ana María,Pérez Ruiz,CC,1001,ana@unicesar.edu.co,A1,3\n"
                + "Luis,Gómez,TI,1002,luis@unicesar.edu.co,A2,1\n");

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).isEqualTo(new RawRow(2, "Ana María", "Pérez Ruiz", "CC", "1001",
                "ana@unicesar.edu.co", "A1", "3"));
        assertThat(rows.get(1).row()).isEqualTo(3);
    }

    @Test
    void detectsSemicolonSeparatorAndSupportsCrLf() {
        List<RawRow> rows = StudentCsvParser.parse(HEADER.replace(',', ';') + "\r\n"
                + "Ana;Pérez;CC;1001;ana@unicesar.edu.co;A1;3\r\n");

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.firstNames()).isEqualTo("Ana");
            assertThat(row.semester()).isEqualTo("3");
        });
    }

    @Test
    void stripsBomAndIgnoresCaseAccentsAndColumnOrder() {
        String content = "﻿Semestre,CORREO,Código_Estudiantil,numero_documento,tipo_documento,apellidos,Nombres\n"
                + "4,ana@unicesar.edu.co,A1,1001,cc,Pérez,Ana\n";

        assertThat(StudentCsvParser.parse(content)).singleElement().isEqualTo(
                new RawRow(2, "Ana", "Pérez", "cc", "1001", "ana@unicesar.edu.co", "A1", "4"));
    }

    @Test
    void handlesQuotedFieldsWithSeparatorsEscapedQuotesAndLineBreaks() {
        List<RawRow> rows = StudentCsvParser.parse(HEADER + "\n"
                + "\"Ana, la \"\"grande\"\"\",\"Pérez\nRuiz\",CC,1001,ana@unicesar.edu.co,A1,3\n"
                + "Luis,Gómez,TI,1002,luis@unicesar.edu.co,A2,1\n");

        assertThat(rows.get(0).firstNames()).isEqualTo("Ana, la \"grande\"");
        assertThat(rows.get(0).lastNames()).isEqualTo("Pérez\nRuiz");
        // La segunda fila empieza en la línea 4 porque la primera ocupa dos líneas.
        assertThat(rows.get(1).row()).isEqualTo(4);
    }

    @Test
    void ignoresEmptyRowsButKeepsTheRealLineNumber() {
        List<RawRow> rows = StudentCsvParser.parse(HEADER + "\n\n,,,,,,\n"
                + "Ana,Pérez,CC,1001,ana@unicesar.edu.co,A1,3");

        assertThat(rows).singleElement().satisfies(row -> assertThat(row.row()).isEqualTo(4));
    }

    @Test
    void shortRowsGetEmptyMissingFields() {
        List<RawRow> rows = StudentCsvParser.parse(HEADER + "\nAna,Pérez\n");

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.documentType()).isEmpty();
            assertThat(row.semester()).isEmpty();
        });
    }

    @Test
    void rejectsAFileWithoutStudents() {
        assertThatThrownBy(() -> StudentCsvParser.parse(HEADER + "\n\n"))
                .isInstanceOf(BusinessRuleException.class).hasMessage("El archivo no tiene estudiantes.");
        assertThatThrownBy(() -> StudentCsvParser.parse(""))
                .isInstanceOf(BusinessRuleException.class).hasMessage("El archivo no tiene estudiantes.");
    }

    @Test
    void rejectsAnIncompleteHeaderNamingTheMissingColumns() {
        assertThatThrownBy(() -> StudentCsvParser.parse("nombres,apellidos,correo\nAna,Pérez,a@b.co\n"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Faltan columnas en la cabecera: tipo_documento, numero_documento, "
                        + "codigo_estudiantil, semestre");
    }

    @Test
    void rejectsMoreThanTheMaximumOfRows() {
        String body = IntStream.rangeClosed(1, StudentCsvParser.MAX_ROWS + 1)
                .mapToObj(i -> "Ana,Pérez,CC," + i + ",a" + i + "@unicesar.edu.co,C" + i + ",1")
                .collect(Collectors.joining("\n"));

        assertThatThrownBy(() -> StudentCsvParser.parse(HEADER + "\n" + body))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("el máximo es 500");
        assertThat(StudentCsvParser.parse(HEADER + "\n" + body.substring(0, body.lastIndexOf('\n'))))
                .hasSize(StudentCsvParser.MAX_ROWS);
    }

    @Test
    void rejectsAnUnclosedQuote() {
        assertThatThrownBy(() -> StudentCsvParser.parse(HEADER + "\n\"Ana,Pérez,CC,1001,a@b.co,A1,3\n"))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("sin cerrar");
    }
}
