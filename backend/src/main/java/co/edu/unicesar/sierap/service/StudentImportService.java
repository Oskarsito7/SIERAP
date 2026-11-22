package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.response.StudentImportPreviewResponse;
import co.edu.unicesar.sierap.dto.response.StudentImportResultResponse;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga masiva de estudiantes desde un archivo CSV, con vista previa.
 *
 * <p>Permisos y alcance: los mismos que {@code POST /api/students} (super_admin, o admin
 * o docente de la facultad del programa).</p>
 */
public interface StudentImportService {

    /**
     * Valida el archivo y devuelve fila por fila qué se crearía y qué falla. No escribe nada.
     *
     * @param actor     usuario que carga el archivo
     * @param programId programa al que se agregarán los estudiantes
     * @param file      archivo CSV
     * @return filas con sus errores y los totales
     */
    StudentImportPreviewResponse preview(AuthenticatedUser actor, Long programId, MultipartFile file);

    /**
     * Vuelve a validar el archivo y crea solo las filas válidas; cada estudiante recibe
     * por correo el enlace para crear su contraseña.
     *
     * @param actor     usuario que carga el archivo
     * @param programId programa al que se agregan los estudiantes
     * @param file      archivo CSV
     * @return cantidad de estudiantes creados y las filas que no se crearon
     */
    StudentImportResultResponse importStudents(AuthenticatedUser actor, Long programId, MultipartFile file);
}
