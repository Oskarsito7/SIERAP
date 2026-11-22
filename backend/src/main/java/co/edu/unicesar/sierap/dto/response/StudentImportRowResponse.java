package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Una fila del archivo CSV de estudiantes con el resultado de su validación.
 *
 * @param row             número de línea en el archivo (la cabecera es la 1)
 * @param firstNames      nombres
 * @param lastNames       apellidos
 * @param documentType    tipo de documento tal como vino en el archivo
 * @param documentNumber  número de documento
 * @param email           correo
 * @param studentCode     código estudiantil
 * @param currentSemester semestre tal como vino en el archivo
 * @param errors          errores de la fila en español; vacío si la fila es válida
 */
public record StudentImportRowResponse(
        int row,
        String firstNames,
        String lastNames,
        String documentType,
        String documentNumber,
        String email,
        String studentCode,
        String currentSemester,
        List<String> errors) {
}
