package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Vista previa de la carga masiva de estudiantes: no se escribió nada.
 *
 * @param rows    filas del archivo con sus errores
 * @param valid   filas sin errores (las que se crearían)
 * @param invalid filas con al menos un error
 */
public record StudentImportPreviewResponse(List<StudentImportRowResponse> rows, int valid, int invalid) {
}
