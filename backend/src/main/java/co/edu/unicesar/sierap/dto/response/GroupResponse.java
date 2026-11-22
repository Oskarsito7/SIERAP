package co.edu.unicesar.sierap.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Grupo (= proyecto de aula), {@code Group} en {@code docs/07}.
 *
 * <p>Los integrantes y los evaluadores se incluyen completos para el docente del
 * grupo, sus evaluadores asignados, el admin de la facultad y el super_admin. La vista
 * reducida que ven los estudiantes (Etapa 5) se arma en el mapper.</p>
 *
 * @param id                 identificador
 * @param momentId           momento en el que se habilitó
 * @param momentName         nombre del momento, p. ej. "Momento 1"
 * @param name               nombre del grupo
 * @param projectTitle       título del proyecto; {@code null} hasta que lo escriban
 * @param teacher            docente que lo habilitó
 * @param memberCount        número de integrantes
 * @param minMembers         mínimo de integrantes del momento
 * @param maxMembers         máximo de integrantes del momento
 * @param full               {@code true} si ya tiene el máximo de integrantes
 * @param members            integrantes
 * @param submissionDeadline fecha límite de entrega; {@code null} hasta que el docente la fije
 * @param evaluationDate     fecha de evaluación; {@code null} hasta que el docente la fije
 * @param evaluators         evaluadores externos asignados; vacío para los estudiantes
 * @param myGroup            {@code true} si quien consulta es integrante
 */
public record GroupResponse(
        Long id,
        Long momentId,
        String momentName,
        String name,
        String projectTitle,
        PersonRef teacher,
        int memberCount,
        int minMembers,
        int maxMembers,
        boolean full,
        List<MemberResponse> members,
        LocalDate submissionDeadline,
        LocalDate evaluationDate,
        List<EvaluatorRef> evaluators,
        boolean myGroup) {

    /**
     * Referencia corta a una persona.
     *
     * @param id       identificador del usuario
     * @param fullName nombre completo
     */
    public record PersonRef(Long id, String fullName) {
    }

    /**
     * Integrante del grupo. Los estudiantes de otros grupos solo ven {@code fullName}
     * (Etapa 5); los demás campos llegan en {@code null}.
     *
     * @param id          identificador del estudiante
     * @param fullName    nombre completo
     * @param studentCode código estudiantil
     * @param email       correo
     */
    public record MemberResponse(Long id, String fullName, String studentCode, String email) {
    }

    /**
     * Evaluador externo asignado.
     *
     * @param id            identificador del usuario
     * @param fullName      nombre completo
     * @param evaluatorType nombre del tipo de evaluador, p. ej. "Empresario"
     * @param countsInGrade {@code true} si su tipo tiene peso en el momento; si no, su
     *                      evaluación no cuenta en la nota (decisión del 16/10, docs/04)
     */
    public record EvaluatorRef(Long id, String fullName, String evaluatorType, boolean countsInGrade) {
    }
}
