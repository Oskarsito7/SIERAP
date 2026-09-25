package co.edu.unicesar.sierap.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.Comparator;

import co.edu.unicesar.sierap.TestcontainersConfiguration;
import co.edu.unicesar.sierap.entity.CriterionGrade;
import co.edu.unicesar.sierap.entity.Evaluation;
import co.edu.unicesar.sierap.entity.EvaluationCriterion;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.LearningOutcome;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.PerformanceLevel;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.ProjectGroup;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.Submission;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.EvaluationStatus;
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
 * semilla, las consultas de alcance por rol y las restricciones de la base de
 * datos (ADR-012, ADR-016).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class OperationsIntegrationTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 25, 8, 0);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private ProgramRepository programRepository;

    @Autowired
    private EvaluatorTypeRepository evaluatorTypeRepository;

    @Autowired
    private MomentRepository momentRepository;

    @Autowired
    private ProjectGroupRepository projectGroupRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private EvaluationRepository evaluationRepository;

    private int userSequence;
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
    void eachEvaluatorDecidesWithItsOwnRuleEvenWithLazyProxies() {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();

        assertThat(entityManager.find(Teacher.class, teacher.getId()).canEvaluate(loaded)).isTrue();
        assertThat(entityManager.find(ExternalEvaluator.class, evaluator.getId()).canEvaluate(loaded)).isTrue();
        assertThat(persist(teacher()).canEvaluate(loaded)).isFalse();
        assertThat(persist(externalEvaluator()).canEvaluate(loaded)).isFalse();
    }

    @Test
    void keepsTheSubmissionHistoryWithTheLateMark() {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        loaded.addSubmission(submission(loaded.nextSubmissionVersion(), false));
        loaded.addSubmission(submission(loaded.nextSubmissionVersion(), true));
        flushAndClear();

        assertThat(submissionRepository.findByProjectGroupIdOrderByVersionNumberDesc(group.getId()))
                .extracting(Submission::getVersionNumber, Submission::isLate)
                .containsExactly(tuple(2, true),
                        tuple(1, false));
        ProjectGroup reloaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        assertThat(reloaded.getSubmissionToGrade().getVersionNumber()).isEqualTo(1);
    }

    @Test
    void rejectsTwoSubmissionsWithTheSameVersion() {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        loaded.addSubmission(submission(1, false));
        loaded.addSubmission(submission(1, false));

        assertThatThrownBy(() -> projectGroupRepository.saveAndFlush(loaded))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theHighestLevelInEveryCriterionOfMoment1AddsUpToFivePoints() {
        gradeEverythingWithTheHighestLevel(teacher, true);

        Evaluation saved = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), teacher.getId()).orElseThrow();
        assertThat(saved.getGrades()).hasSize(8);
        assertThat(saved.isComplete()).isTrue();
        assertThat(saved.getTotalScore()).isEqualByComparingTo("5.00");
        LearningOutcome rap5 = saved.getProjectGroup().getMoment().getLearningOutcomes().get(4);
        assertThat(saved.getScoreFor(rap5)).isEqualByComparingTo("1.40");
    }

    @Test
    void onlySubmittedEvaluationsAreFoundAsSubmitted() {
        gradeEverythingWithTheHighestLevel(teacher, true);
        gradeEverythingWithTheHighestLevel(evaluator, false);

        assertThat(evaluationRepository.findByProjectGroupId(group.getId())).hasSize(2);
        assertThat(evaluationRepository.findByProjectGroupIdAndStatus(group.getId(), EvaluationStatus.SUBMITTED))
                .extracting(evaluation -> evaluation.getEvaluator().getId())
                .containsExactly(teacher.getId());
        assertThat(evaluationRepository.findByProjectGroupMomentIdAndStatus(moment.getId(), EvaluationStatus.DRAFT))
                .extracting(evaluation -> evaluation.getEvaluator().getId())
                .containsExactly(evaluator.getId());
    }

    @Test
    void correctingAGradedCriterionUpdatesTheSameRow() {
        gradeEverythingWithTheHighestLevel(teacher, true);
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
        gradeEverythingWithTheHighestLevel(teacher, true);
        PerformanceLevel used = evaluationRepository
                .findByProjectGroupIdAndEvaluatorId(group.getId(), teacher.getId()).orElseThrow()
                .getGrades().get(0).getLevel();

        assertThatThrownBy(() -> entityManager.createNativeQuery("DELETE FROM performance_level WHERE id = :id")
                .setParameter("id", used.getId())
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    private void gradeEverythingWithTheHighestLevel(User grader, boolean submit) {
        ProjectGroup loaded = projectGroupRepository.findById(group.getId()).orElseThrow();
        Evaluation evaluation = new Evaluation(
                entityManager.find(User.class, grader.getId()), LocalDateTime.of(2026, 11, 10, 8, 0));
        loaded.addEvaluation(evaluation);
        loaded.getMoment().getLearningOutcomes().stream()
                .flatMap(outcome -> outcome.getCriteria().stream())
                .forEach(criterion -> evaluation.grade(highestLevel(criterion)));
        if (submit) {
            evaluation.submit();
        }
        flushAndClear();
    }

    private static PerformanceLevel highestLevel(EvaluationCriterion criterion) {
        return criterion.getLevels().stream().max(Comparator.comparing(PerformanceLevel::getScore)).orElseThrow();
    }

    private static PerformanceLevel lowestLevel(EvaluationCriterion criterion) {
        return criterion.getLevels().stream().min(Comparator.comparing(PerformanceLevel::getScore)).orElseThrow();
    }

    private static Submission submission(int version, boolean late) {
        return new Submission(version, "entregas/v" + version + ".zip",
                "entregas/v" + version + ".pdf", LocalDateTime.of(2026, 10, 15, 10, 0), late);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private Teacher teacher() {
        int n = ++userSequence;
        return new Teacher("Docente", "Prueba " + n, DocumentType.CC, "T-" + n,
                "docente" + n + "@unicesar.edu.co", "hash", CREATED_AT, faculty());
    }

    private Student student() {
        int n = ++userSequence;
        return new Student("Estudiante", "Prueba " + n, DocumentType.TI, "S-" + n,
                "estudiante" + n + "@unicesar.edu.co", "hash", CREATED_AT, "E-" + n, 3, program());
    }

    private ExternalEvaluator externalEvaluator() {
        int n = ++userSequence;
        return new ExternalEvaluator("Evaluador", "Prueba " + n, DocumentType.CC, "X-" + n,
                "evaluador" + n + "@correo.com", "hash", CREATED_AT, evaluatorType("Empresario"));
    }

    private Faculty faculty() {
        return facultyRepository.findByCode("04").orElseThrow();
    }

    private Program program() {
        return programRepository.findByCode("108").orElseThrow();
    }

    private EvaluatorType evaluatorType(String name) {
        return evaluatorTypeRepository.findByName(name).orElseThrow();
    }
}
