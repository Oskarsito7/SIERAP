package co.edu.unicesar.sierap.service.impl;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.StudentImportRowResponse;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.service.impl.StudentCsvParser.RawRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link StudentImportValidator}: errores de campo, unicidad contra la
 * base y repetidos dentro del mismo archivo.
 */
@ExtendWith(MockitoExtension.class)
class StudentImportValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    private StudentImportValidator validator;

    @BeforeEach
    void setUp() {
        validator = new StudentImportValidator(userRepository, studentRepository);
    }

    private static RawRow row(int line, String code, String email, String document) {
        return new RawRow(line, "Ana", "Pérez", "CC", document, email, code, "3");
    }

    @Test
    void aCompleteRowHasNoErrors() {
        List<StudentImportRowResponse> result = validator.validate(
                List.of(row(2, "A1", "Ana@UniCesar.edu.co", "1001")));

        assertThat(result).singleElement().satisfies(r -> {
            assertThat(r.errors()).isEmpty();
            assertThat(r.row()).isEqualTo(2);
            assertThat(r.email()).isEqualTo("Ana@UniCesar.edu.co");
        });
    }

    @Test
    void reportsEveryFieldErrorOfTheSameRow() {
        RawRow bad = new RawRow(5, "", "", "XX", "", "no-es-correo", "", "tres");

        List<String> errors = validator.validate(List.of(bad)).get(0).errors();

        assertThat(errors).containsExactly(
                "Los nombres son obligatorios.",
                "Los apellidos son obligatorios.",
                "El tipo de documento «XX» no es válido (use CC, TI o CE).",
                "El número de documento es obligatorio.",
                "El correo no es válido.",
                "El código estudiantil es obligatorio.",
                "El semestre debe ser un número entero.");
    }

    @Test
    void rejectsSemesterBelowOneAndTooLongValues() {
        RawRow bad = new RawRow(2, "N".repeat(101), "Pérez", "TI", "1".repeat(31), "a@b.co", "C".repeat(31), "0");

        assertThat(validator.validate(List.of(bad)).get(0).errors()).containsExactly(
                "Los nombres no pueden pasar de 100 caracteres.",
                "El número de documento no puede pasar de 30 caracteres.",
                "El código estudiantil no puede pasar de 30 caracteres.",
                "El semestre debe ser 1 o mayor.");
    }

    @Test
    void passportIsNotAllowedInTheBulkUpload() {
        RawRow passport = new RawRow(2, "Ana", "Pérez", "PASSPORT", "1001", "a@b.co", "A1", "1");

        assertThat(validator.validate(List.of(passport)).get(0).errors())
                .containsExactly("El tipo de documento «PASSPORT» no es válido (use CC, TI o CE).");
        assertThatThrownBy(() -> StudentImportValidator.parseDocumentType("PASSPORT"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(StudentImportValidator.parseDocumentType(" ce ")).isEqualTo(DocumentType.CE);
    }

    @Test
    void reportsEmailDocumentAndCodeAlreadyInTheDatabase() {
        when(userRepository.existsByEmail("ana@unicesar.edu.co")).thenReturn(true);
        when(userRepository.existsByDocumentNumber("1001")).thenReturn(true);
        when(studentRepository.existsByStudentCode("A1")).thenReturn(true);

        List<String> errors = validator.validate(List.of(row(2, "A1", "Ana@unicesar.edu.co", "1001"))).get(0).errors();

        assertThat(errors).containsExactly(
                "Ya existe una cuenta con el correo ana@unicesar.edu.co.",
                "Ya existe una cuenta con el documento 1001.",
                "Ya existe un estudiante con el código A1.");
    }

    @Test
    void flagsRepeatedValuesInsideTheFileOnTheLaterRowsOnly() {
        List<StudentImportRowResponse> result = validator.validate(List.of(
                row(2, "A1", "ana@unicesar.edu.co", "1001"),
                row(3, "A1", "ANA@unicesar.edu.co", "1002"),
                row(4, "A3", "luis@unicesar.edu.co", "1001")));

        assertThat(result.get(0).errors()).isEmpty();
        assertThat(result.get(1).errors()).containsExactly(
                "El correo ana@unicesar.edu.co está repetido en el archivo (ya aparece en la fila 2).",
                "El código A1 está repetido en el archivo (ya aparece en la fila 2).");
        assertThat(result.get(2).errors()).containsExactly(
                "El documento 1001 está repetido en el archivo (ya aparece en la fila 2).");
    }
}
