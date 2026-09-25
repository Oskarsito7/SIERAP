package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.repository.EvaluationRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.SubmissionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prueba de integración de la migración V4 contra PostgreSQL real
 * (Testcontainers): grupos, entregas y evaluaciones sobre el Momento 1 de la
 * semilla, las consultas de alcance por rol y las restricciones de la base.
 *
 * <p>Está en el paquete {@code entity} para poder crear usuarios con el
 * constructor {@code protected} que exige JPA; cuando existan los constructores
 * públicos del D1, basta con cambiar los métodos {@code teacher()},
 * {@code student()} y {@code externalEvaluator()} del final.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class OperationsIntegrationTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MomentRepository momentRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    private Moment moment;
    private Teacher teacher;
    private ExternalEvaluator evaluator;
    private Student member;
    private Student outsider;
    private ProjectGroup group;

    @BeforeEach
    void setUp() {
        moment = momentRepository.findByProgramCodeAndPeriodCodeAndNumber("108", "2026-2", 1).orElseThrow();
        teacher = persist(teacher());
        evaluator = persist(externalEvaluator());
        member = persist(student());
        outsider = persist(student());

        group = new ProjectGroup(moment, teacher, "Grupo 1");
        group.addMember(member);
        group.addEvaluator(evaluator);
        projectGroupRepository.save(group);
        flushAndClear();
    }

    @Test
    void findsTheGroupsThatEachRoleMaySee() {
        ProjectGroup otherTeachersGroup = new ProjectGroup(
                entityManager.find(Moment.class, moment.getId()), persist(teacher()), "Grupo 2");
        projectGroupRepository.save(otherTeachersGroup);
        flushAndClear();

        assertThat(projectGroupRepository.findByMomentIdOrderByName(moment.getId()))
                .extracting(ProjectGroup::getName).containsExactly("Grupo 1", "Grupo 2");
        assertThat(projectGroupRepository.findByTeacherIdAndMomentIdOrderByName(teacher.getId(), moment.getId()))
                .extracting(ProjectGroup::getName).containsExactly("Grupo 1");
        assertThat(projectGroupRepository.findByEvaluatorsId(evaluator.getId()))
                .extracting(ProjectGroup::getName).containsExactly("Grupo 1");
        assertThat(projectGroupRepository.findByMembersId(member.getId()))
                .extracting(ProjectGroup::getName).containsExactly("Grupo 1");
    }

    @Test
    void knowsWhetherAStudentAlreadyHasAGroupInTheMoment() {
        assertThat(projectGroupRepository.existsByMomentIdAndMembersId(moment.getId(), member.getId())).isTrue();
        assertThat(projectGroupRepository.existsByMomentIdAndMembersId(moment.getId(), outsider.getId())).isFalse();
        assertThat(projectGroupRepository.findByMomentIdAndMembersId(moment.getId(), member.getId()))
                .map(ProjectGroup::getName).contains("Grupo 1");
    }

    @Test
    void theTeacherLoadedLazilyCanStillGradeTheGroup() {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        Teacher sameTeacher = entityManager.find(Teacher.class, teacher.getId());

        assertThat(loaded.canBeGradedBy(sameTeacher)).isTrue();
        assertThat(loaded.canBeGradedBy(entityManager.find(ExternalEvaluator.class, evaluator.getId()))).isTrue();
        assertThat(loaded.canBeGradedBy(entityManager.find(Student.class, outsider.getId()))).isFalse();
    }

    @Test
    void keepsTheSubmissionHistoryAndReturnsTheLatestVersion() {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        loaded.addSubmission(submission(loaded.nextSubmissionVersion()));
        loaded.addSubmission(submission(loaded.nextSubmissionVersion()));
        flushAndClear();

        assertThat(submissionRepository.findByProjectGroupIdOrderByVersionNumberDesc(group.getId()))
                .extracting(Submission::getVersionNumber).containsExactly(2, 1);
        assertThat(submissionRepository.findFirstByProjectGroupIdOrderByVersionNumberDesc(group.getId()))
                .map(Submission::getVersionNumber).contains(2);
    }

    @Test
    void rejectsTwoSubmissionsWithTheSameVersion() {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        loaded.addSubmission(submission(1));
        loaded.addSubmission(submission(1));

        assertThatThrownBy(() -> projectGroupRepository.saveAndFlush(loaded))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theHighestLevelInEveryCriterionOfMoment1AddsUpToFivePoints() {
        gradeEverythingWithTheHighestLevel();

        Evaluation saved = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), teacher.getId()).orElseThrow();
        assertThat(saved.getGrades()).hasSize(8);
        assertThat(saved.isComplete()).isTrue();
        assertThat(saved.getTotalScore()).isEqualByComparingTo("5.00");
        LearningOutcome rap5 = saved.getProjectGroup().getMoment().getLearningOutcomes().get(4);
        assertThat(saved.getScoreFor(rap5)).isEqualByComparingTo("1.40");
    }

    @Test
    void correctingAGradedCriterionUpdatesTheSameRow() {
        gradeEverythingWithTheHighestLevel();
        Evaluation saved = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), teacher.getId()).orElseThrow();
        CriterionGrade firstGrade = saved.getGrades().get(0);
        Long gradeId = firstGrade.getId();
        PerformanceLevel lowest = lowestLevel(firstGrade.getCriterion());

        saved.grade(lowest);
        flushAndClear();

        CriterionGrade corrected = entityManager.find(CriterionGrade.class, gradeId);
        assertThat(corrected.getLevel().getId()).isEqualTo(lowest.getId());
        assertThat(corrected.getScore()).isEqualByComparingTo("0");
    }

    @Test
    void aPerformanceLevelUsedInAGradeCannotBeDeleted() {
        gradeEverythingWithTheHighestLevel();
        CriterionGrade grade = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), teacher.getId()).orElseThrow()
                .getGrades().get(0);
        PerformanceLevel used = grade.getLevel();

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("DELETE FROM performance_level WHERE id = :id")
                    .setParameter("id", used.getId())
                    .executeUpdate();
        }).isInstanceOf(PersistenceException.class);
    }

    private void gradeEverythingWithTheHighestLevel() {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        Evaluation evaluation = new Evaluation(
                entityManager.find(Teacher.class, teacher.getId()), LocalDateTime.of(2026, 11, 10, 8, 0));
        loaded.addEvaluation(evaluation);
        loaded.getMoment().getLearningOutcomes().stream()
                .flatMap(outcome -> outcome.getCriteria().stream())
                .forEach(criterion -> evaluation.grade(highestLevel(criterion)));
        flushAndClear();
    }

    private static PerformanceLevel highestLevel(EvaluationCriterion criterion) {
        return criterion.getLevels().stream().max(Comparator.comparing(PerformanceLevel::getScore)).orElseThrow();
    }

    private static PerformanceLevel lowestLevel(EvaluationCriterion criterion) {
        List<PerformanceLevel> levels = criterion.getLevels();
        return levels.stream().min(Comparator.comparing(PerformanceLevel::getScore)).orElseThrow();
    }

    private static Submission submission(int version) {
        return new Submission(version, "entregas/v" + version + ".zip",
                "entregas/v" + version + ".pdf", LocalDateTime.of(2026, 10, 15, 10, 0));
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    // Único lugar que depende de cómo el D1 construya los usuarios (V3).

    private static Teacher teacher() {
        return new Teacher();
    }

    private static Student student() {
        return new Student();
    }

    private static ExternalEvaluator externalEvaluator() {
        return new ExternalEvaluator();
    }
}
