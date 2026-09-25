package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las reglas de {@link ProjectGroup}: cupo, integrantes,
 * quién puede calificar y numeración de versiones de la entrega.
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
    void onlyItsTeacherAndItsAssignedEvaluatorsCanGradeIt() {
        ExternalEvaluator assigned = new ExternalEvaluator();
        group.addEvaluator(assigned);

        assertThat(group.canBeGradedBy(teacher)).isTrue();
        assertThat(group.canBeGradedBy(assigned)).isTrue();
        assertThat(group.canBeGradedBy(new Teacher())).isFalse();
        assertThat(group.canBeGradedBy(new ExternalEvaluator())).isFalse();
        assertThat(group.canBeGradedBy(new Student())).isFalse();
    }

    @Test
    void anEvaluatorRemovedFromTheGroupCanNoLongerGradeIt() {
        ExternalEvaluator evaluator = new ExternalEvaluator();
        group.addEvaluator(evaluator);

        group.removeEvaluator(evaluator);

        assertThat(group.canBeGradedBy(evaluator)).isFalse();
    }

    @Test
    void numbersSubmissionVersionsConsecutivelyAndKnowsTheLatest() {
        assertThat(group.nextSubmissionVersion()).isEqualTo(1);
        assertThat(group.getLatestSubmission()).isNull();

        group.addSubmission(submission(group.nextSubmissionVersion()));
        Submission second = submission(group.nextSubmissionVersion());
        group.addSubmission(second);

        assertThat(second.getVersionNumber()).isEqualTo(2);
        assertThat(group.nextSubmissionVersion()).isEqualTo(3);
        assertThat(group.getLatestSubmission()).isSameAs(second);
        assertThat(second.getProjectGroup()).isSameAs(group);
    }

    private static Submission submission(int version) {
        return new Submission(version, "entregas/v" + version + ".zip",
                "entregas/v" + version + ".pdf", LocalDateTime.of(2026, 10, 15, 10, 0));
    }
}
