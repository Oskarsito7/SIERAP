package co.edu.unicesar.sierap.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import co.edu.unicesar.sierap.dto.response.StudentImportRowResponse;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.service.impl.StudentCsvParser.RawRow;
import org.springframework.stereotype.Component;

/**
 * Valida las filas del CSV de estudiantes y junta <b>todos</b> los errores de cada una.
 *
 * <p>Las reglas de formato son las de {@code NewStudentRequest} (obligatorios, largos
 * máximos, tipo de documento, semestre 1 o mayor), más la unicidad contra la base de
 * datos y contra las demás filas del mismo archivo (correo, documento y código).</p>
 */
@Component
class StudentImportValidator {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /**
     * Crea el validador.
     *
     * @param userRepository    repositorio de usuarios
     * @param studentRepository repositorio de estudiantes
     */
    StudentImportValidator(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Valida todas las filas.
     *
     * @param rows filas leídas del archivo
     * @return una respuesta por fila, en el mismo orden, con sus errores (vacíos si es válida)
     */
    List<StudentImportRowResponse> validate(List<RawRow> rows) {
        Map<String, Integer> emails = new HashMap<>();
        Map<String, Integer> documents = new HashMap<>();
        Map<String, Integer> codes = new HashMap<>();
        List<StudentImportRowResponse> result = new ArrayList<>();
        for (RawRow row : rows) {
            List<String> errors = new ArrayList<>();
            checkText(errors, row.firstNames(), "Los nombres", 100);
            checkText(errors, row.lastNames(), "Los apellidos", 100);
            checkDocumentType(errors, row.documentType());
            checkText(errors, row.documentNumber(), "El número de documento", 30);
            checkEmail(errors, row.email());
            checkText(errors, row.studentCode(), "El código estudiantil", 30);
            checkSemester(errors, row.semester());

            String email = row.email().toLowerCase(Locale.ROOT);
            if (!email.isEmpty() && EMAIL.matcher(email).matches()) {
                if (userRepository.existsByEmail(email)) {
                    errors.add("Ya existe una cuenta con el correo " + email + ".");
                }
                checkRepeated(errors, emails, email, row.row(), "El correo " + email);
            }
            if (!row.documentNumber().isEmpty()) {
                if (userRepository.existsByDocumentNumber(row.documentNumber())) {
                    errors.add("Ya existe una cuenta con el documento " + row.documentNumber() + ".");
                }
                checkRepeated(errors, documents, row.documentNumber(), row.row(),
                        "El documento " + row.documentNumber());
            }
            if (!row.studentCode().isEmpty()) {
                if (studentRepository.existsByStudentCode(row.studentCode())) {
                    errors.add("Ya existe un estudiante con el código " + row.studentCode() + ".");
                }
                checkRepeated(errors, codes, row.studentCode(), row.row(), "El código " + row.studentCode());
            }
            result.add(new StudentImportRowResponse(row.row(), row.firstNames(), row.lastNames(),
                    row.documentType(), row.documentNumber(), row.email(), row.studentCode(), row.semester(),
                    List.copyOf(errors)));
        }
        return result;
    }

    /**
     * Interpreta el tipo de documento de una fila ya validada.
     *
     * @param value texto del archivo (CC, TI o CE, sin importar mayúsculas)
     * @return tipo de documento
     */
    static DocumentType parseDocumentType(String value) {
        DocumentType type = DocumentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        if (type == DocumentType.PASSPORT) {
            throw new IllegalArgumentException("Tipo no permitido en la carga masiva: " + type);
        }
        return type;
    }

    private static void checkText(List<String> errors, String value, String label, int max) {
        String verb = label.startsWith("Los ") ? "son obligatorios" : "es obligatorio";
        String article = label.startsWith("Los ") ? "no pueden" : "no puede";
        if (value.isEmpty()) {
            errors.add(label + " " + verb + ".");
        } else if (value.length() > max) {
            errors.add(label + " " + article + " pasar de " + max + " caracteres.");
        }
    }

    private static void checkDocumentType(List<String> errors, String value) {
        if (value.isEmpty()) {
            errors.add("El tipo de documento es obligatorio.");
            return;
        }
        try {
            parseDocumentType(value);
        } catch (IllegalArgumentException e) {
            errors.add("El tipo de documento «" + value + "» no es válido (use CC, TI o CE).");
        }
    }

    private static void checkEmail(List<String> errors, String value) {
        if (value.isEmpty()) {
            errors.add("El correo es obligatorio.");
        } else if (!EMAIL.matcher(value).matches()) {
            errors.add("El correo no es válido.");
        } else if (value.length() > 150) {
            errors.add("El correo no puede pasar de 150 caracteres.");
        }
    }

    private static void checkSemester(List<String> errors, String value) {
        if (value.isEmpty()) {
            errors.add("El semestre es obligatorio.");
            return;
        }
        try {
            if (Integer.parseInt(value) < 1) {
                errors.add("El semestre debe ser 1 o mayor.");
            }
        } catch (NumberFormatException e) {
            errors.add("El semestre debe ser un número entero.");
        }
    }

    private static void checkRepeated(List<String> errors, Map<String, Integer> seen, String key, int row,
                                      String label) {
        Integer first = seen.putIfAbsent(key, row);
        if (first != null) {
            errors.add(label + " está repetido en el archivo (ya aparece en la fila " + first + ").");
        }
    }
}
