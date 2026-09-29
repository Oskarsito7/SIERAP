package co.edu.unicesar.sierap.dto.response;

/**
 * Estudiante del programa de un momento que todavía no tiene grupo en él.
 *
 * @param id              identificador del estudiante
 * @param fullName        nombre completo
 * @param studentCode     código estudiantil
 * @param currentSemester semestre actual
 */
public record StudentWithoutGroupResponse(Long id, String fullName, String studentCode, int currentSemester) {
}
