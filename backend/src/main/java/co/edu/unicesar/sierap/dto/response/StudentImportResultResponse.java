package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Resultado de la carga masiva de estudiantes.
 *
 * @param created estudiantes creados
 * @param errors  filas que no se crearon, con el motivo
 */
public record StudentImportResultResponse(int created, List<RowError> errors) {

    /**
     * Error de una fila del archivo.
     *
     * @param row     número de línea en el archivo
     * @param message errores de la fila, separados por punto y coma
     */
    public record RowError(int row, String message) {
    }
}
