package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDate;

/**
 * Código de registro de un docente.
 *
 * @param id              identificador
 * @param code            código que escriben los estudiantes
 * @param programId       programa en el que quedan los estudiantes
 * @param programName     nombre del programa
 * @param teacherId       docente dueño del código
 * @param teacherName     nombre del docente
 * @param expiresOn       último día en que sirve
 * @param active          {@code false} si fue desactivado
 * @param registeredCount estudiantes que se registraron con él
 */
public record RegistrationCodeResponse(
        Long id,
        String code,
        Long programId,
        String programName,
        Long teacherId,
        String teacherName,
        LocalDate expiresOn,
        boolean active,
        long registeredCount) {
}
