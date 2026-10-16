package co.edu.unicesar.sierap.dto.response;

import java.util.List;

/**
 * Diagnóstico previo al lanzamiento de un momento ({@code LaunchCheck} en {@code docs/07}).
 *
 * @param momentId                    identificador del momento
 * @param canLaunch                   {@code true} si no hay problemas bloqueantes y sigue en configuración
 * @param problems                    lista de todos los problemas bloqueantes para lanzar
 * @param minMembers                  mínimo de integrantes por grupo requerido
 * @param groupsBelowMinimum          grupos habilitados que no alcanzan el mínimo de integrantes
 * @param evaluatorTypesWithoutWeight tipos de evaluador asignados a grupos del momento que no tienen peso asignado
 */
public record LaunchCheckResponse(
        Long momentId,
        boolean canLaunch,
        List<String> problems,
        int minMembers,
        List<GroupBelowMinimumResponse> groupsBelowMinimum,
        List<EvaluatorTypeWithoutWeightResponse> evaluatorTypesWithoutWeight) {

    /**
     * Grupo que no alcanza el mínimo de integrantes.
     *
     * @param id          identificador del grupo
     * @param name        nombre del grupo
     * @param teacher     docente responsable del grupo
     * @param memberCount cantidad actual de integrantes
     */
    public record GroupBelowMinimumResponse(
            Long id,
            String name,
            TeacherSummary teacher,
            int memberCount) {
    }

    /**
     * Resumen del docente responsable del grupo.
     *
     * @param id       identificador del docente
     * @param fullName nombre completo del docente
     */
    public record TeacherSummary(
            Long id,
            String fullName) {
    }

    /**
     * Tipo de evaluador que tiene evaluadores asignados a grupos del momento pero sin peso en la calificación.
     *
     * @param evaluatorTypeId   identificador del tipo de evaluador
     * @param evaluatorTypeName nombre del tipo de evaluador
     * @param evaluatorCount    cantidad de evaluadores de este tipo asignados en el momento
     */
    public record EvaluatorTypeWithoutWeightResponse(
            Long evaluatorTypeId,
            String evaluatorTypeName,
            int evaluatorCount) {
    }
}
