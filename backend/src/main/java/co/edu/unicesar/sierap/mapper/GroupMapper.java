package co.edu.unicesar.sierap.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import co.edu.unicesar.sierap.dto.response.GroupResponse;
import co.edu.unicesar.sierap.dto.response.GroupResponse.EvaluatorRef;
import co.edu.unicesar.sierap.dto.response.GroupResponse.MemberResponse;
import co.edu.unicesar.sierap.dto.response.GroupResponse.PersonRef;
import co.edu.unicesar.sierap.dto.response.StudentWithoutGroupResponse;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.User;

/**
 * Convierte grupos a sus DTO de respuesta ({@code docs/07}, "Groups").
 */
public final class GroupMapper {

    private static final Comparator<User> BY_NAME =
            Comparator.comparing(User::getLastNames).thenComparing(User::getFirstNames);

    private GroupMapper() {
    }

    /**
     * Vista de un grupo según quién consulta ({@code docs/07}, "Groups"):
     * <ul>
     *   <li>su docente, sus evaluadores asignados, el admin de la facultad y el
     *       super_admin lo ven completo;</li>
     *   <li>sus integrantes lo ven completo, menos los evaluadores;</li>
     *   <li>los estudiantes de otros grupos solo ven el nombre de cada integrante.</li>
     * </ul>
     *
     * @param group  grupo
     * @param viewer usuario que consulta, para calcular {@code myGroup} y la vista
     * @return grupo con los datos que le corresponden a quien consulta
     */
    public static GroupResponse toResponse(ProjectGroup group, User viewer) {
        Moment moment = group.getMoment();
        boolean myGroup = group.getMembers().stream()
                .anyMatch(member -> Objects.equals(member.getId(), viewer.getId()));
        boolean student = viewer instanceof Student;
        Function<Student, MemberResponse> memberView = student && !myGroup
                ? GroupMapper::toMemberName
                : GroupMapper::toMember;
        List<EvaluatorRef> evaluators = student
                ? List.of()
                : group.getEvaluators().stream().sorted(BY_NAME).map(GroupMapper::toEvaluator).toList();
        return new GroupResponse(
                group.getId(),
                moment.getId(),
                moment.getName(),
                group.getName(),
                group.getProjectTitle(),
                new PersonRef(group.getTeacher().getId(), group.getTeacher().getFullName()),
                group.getMembers().size(),
                moment.getMinMembers(),
                moment.getMaxMembers(),
                group.isFull(),
                group.getMembers().stream().sorted(BY_NAME).map(memberView).toList(),
                group.getSubmissionDeadline(),
                group.getEvaluationDate(),
                evaluators,
                myGroup);
    }

    /**
     * Estudiante sin grupo en un momento.
     *
     * @param student estudiante
     * @return nombre, código y semestre
     */
    public static StudentWithoutGroupResponse toStudentWithoutGroup(Student student) {
        return new StudentWithoutGroupResponse(student.getId(), student.getFullName(), student.getStudentCode(),
                student.getCurrentSemester());
    }

    private static MemberResponse toMember(Student student) {
        return new MemberResponse(student.getId(), student.getFullName(), student.getStudentCode(),
                student.getEmail());
    }

    /** Lo que ve un estudiante de los integrantes de otro grupo: solo el nombre. */
    private static MemberResponse toMemberName(Student student) {
        return new MemberResponse(student.getId(), student.getFullName(), null, null);
    }

    private static EvaluatorRef toEvaluator(ExternalEvaluator evaluator) {
        return new EvaluatorRef(evaluator.getId(), evaluator.getFullName(), evaluator.getEvaluatorType().getName());
    }
}
