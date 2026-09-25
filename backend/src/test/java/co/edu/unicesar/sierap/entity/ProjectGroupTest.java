package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las reglas de {@link ProjectGroup}: cupo, integrantes,
 * quién puede calificarlo ({@link Evaluator}) y qué versión de la entrega se
 * califica.
 *
 * <p>Los usuarios se crean con el constructor sin argumentos que exige JPA (es
 * {@code protected}, pero accesible desde el mismo paquete).</p>
 */
class ProjectGroupTest {

    private Teacher teacher;
    private ProjectGroup group;

    @BeforeEach
    void setUp() {
        Moment moment = new Moment();
        moment.setMinMembers(3);
        moment.setMaxMembers(4);
        teacher = new Teacher();
        group = new ProjectGroup(moment, teacher, "Grupo 1");
    }

    @Test
    void reachesTheMinimumAndTheMaximumOfTheMoment() {
        group.addMember(new Student());
        group.addMember(new Student());
        assertThat(group.hasMinimumMembers()).isFalse();

        group.addMember(new Student());
        assertThat(group.hasMinimumMembers()).isTrue();
        assertThat(group.isFull()).isFalse();

        group.addMember(new Student());
        assertThat(group.isFull()).isTrue();
    }

    @Test
    void knowsItsMembersAndForgetsTheOnesWhoLeave() {
        Student student = new Student();
        group.addMember(student);
        assertThat(group.isMember(student)).isTrue();
        assertThat(group.isMember(new Student())).isFalse();

        group.removeMember(student);

        assertThat(group.isMember(student)).isFalse();
    }

    @Test
    void aTeacherCanOnlyEvaluateTheGroupsTheyEnabled() {
        Evaluator sameTeacher = teacher;
        Evaluator otherTeacher = new Teacher();

        assertThat(sameTeacher.canEvaluate(group)).isTrue();
        assertThat(otherTeacher.canEvaluate(group)).isFalse();
    }

    @Test
    void anExternalEvaluatorCanOnlyEvaluateTheGroupsTheyWereAssignedTo() {
        ExternalEvaluator assigned = new ExternalEvaluator();
        group.addEvaluator(assigned);
        Evaluator assignedEvaluator = assigned;
        Evaluator notAssigned = new ExternalEvaluator();

        assertThat(assignedEvaluator.canEvaluate(group)).isTrue();
        assertThat(notAssigned.canEvaluate(group)).isFalse();
    }

    @Test
    void anEvaluatorRemovedFromTheGroupCanNoLongerEvaluateIt() {
        ExternalEvaluator evaluator = new ExternalEvaluator();
        group.addEvaluator(evaluator);

        group.removeEvaluator(evaluator);

        assertThat(evaluator.canEvaluate(group)).isFalse();
    }

    @Test
    void numbersSubmissionVersionsConsecutively() {
        assertThat(group.nextSubmissionVersion()).isEqualTo(1);

        group.addSubmission(submission(group.nextSubmissionVersion(), false));
        Submission second = submission(group.nextSubmissionVersion(), false);
        group.addSubmission(second);

        assertThat(second.getVersionNumber()).isEqualTo(2);
        assertThat(group.nextSubmissionVersion()).isEqualTo(3);
        assertThat(second.getProjectGroup()).isSameAs(group);
    }

    @Test
    void withoutSubmissionsThereIsNothingToGrade() {
        assertThat(group.getSubmissionToGrade()).isNull();
    }

    @Test
    void gradesTheLastOnTimeVersionEvenIfLateOnesCameAfter() {
        group.addSubmission(submission(1, false));
        Submission lastOnTime = submission(2, false);
        group.addSubmission(lastOnTime);
        group.addSubmission(submission(3, true));

        assertThat(group.getSubmissionToGrade()).isSameAs(lastOnTime);
    }

    @Test
    void gradesTheLastLateVersionWhenNoneIsOnTime() {
        group.addSubmission(submission(1, true));
        Submission lastLate = submission(2, true);
        group.addSubmission(lastLate);

        assertThat(group.getSubmissionToGrade()).isSameAs(lastLate);
    }

    private static Submission submission(int version, boolean late) {
        return new Submission(version, "entregas/v" + version + ".zip",
                "entregas/v" + version + ".pdf", LocalDateTime.of(2026, 10, 15, 10, 0), late);
    }
}
