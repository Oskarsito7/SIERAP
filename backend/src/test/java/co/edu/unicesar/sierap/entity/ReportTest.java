package co.edu.unicesar.sierap.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import co.edu.unicesar.sierap.dto.response.report.ComparativeReportData;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.dto.response.report.ReportData;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de la jerarquía polimórfica de {@link Report} (Etapa 7, RF 12, RAP2).
 * Verifica el polimorfismo mediante {@link Report#generate()} y la lógica de negocio de
 * cada subclase: individual, grupal, momento consolidado y comparativo.
 */
class ReportTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 25, 10, 0);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);

    private AcademicPeriod period1;
    private AcademicPeriod period2;
    private Program program;
    private Moment moment1;
    private Moment moment2;

    private LearningOutcome rap1;
    private LearningOutcome rap2;
    private EvaluationCriterion criterion1;
    private EvaluationCriterion criterion2;

    private Teacher teacher;
    private EvaluatorType businessType;
    private ExternalEvaluator business;

    private Student ana;
    private Student beto;
    private ProjectGroup group;

    @BeforeEach
    void setUp() {
        period1 = new AcademicPeriod("2026-1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 6, 30), LocalDate.of(2026, 6, 25));
        ReflectionTestUtils.setField(period1, "id", 1L);

        period2 = new AcademicPeriod("2026-2", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 10));
        ReflectionTestUtils.setField(period2, "id", 2L);

        Faculty faculty = new Faculty("ING", "Ingeniería y Tecnologías");
        ReflectionTestUtils.setField(faculty, "id", 10L);

        program = new Program("108", "Ingeniería de Sistemas");
        ReflectionTestUtils.setField(program, "id", 100L);
        faculty.addProgram(program);

        criterion1 = criterion("Criterio 1", "0.00", "1.00");
        criterion2 = criterion("Criterio 2", "0.50", "2.00");
        rap1 = new LearningOutcome("RAP1", "Diseño de software");
        ReflectionTestUtils.setField(rap1, "id", 11L);
        rap1.addCriterion(criterion1);

        rap2 = new LearningOutcome("RAP2", "Construcción y pruebas");
        ReflectionTestUtils.setField(rap2, "id", 12L);
        rap2.addCriterion(criterion2);

        businessType = evaluatorType(1L, "Empresario");

        // Momento periodo 2
        moment2 = new Moment();
        ReflectionTestUtils.setField(moment2, "id", 200L);
        ReflectionTestUtils.setField(moment2, "period", period2);
        ReflectionTestUtils.setField(moment2, "program", program);
        moment2.setNumber(1);
        moment2.setName("Momento 1");
        moment2.setSubject("Programación II");
        moment2.setGradingEnd(LocalDate.of(2026, 11, 20)); // Ya cerró frente a TODAY (25/11/2026)
        moment2.addLearningOutcome(rap1);
        moment2.addLearningOutcome(rap2);
        moment2.setTeacherWeight(new BigDecimal("60"));
        moment2.setEvaluatorWeight(businessType, new BigDecimal("20"));

        teacher = new Teacher("Marta", "Díaz", DocumentType.CC, "101", "marta@unicesar.edu.co", "pwd", NOW, faculty);
        ReflectionTestUtils.setField(teacher, "id", 50L);

        ana = new Student("Ana", "Alba", DocumentType.CC, "201", "ana@unicesar.edu.co", "pwd", NOW, "2026101", 3, program);
        ReflectionTestUtils.setField(ana, "id", 61L);

        beto = new Student("Beto", "Bravo", DocumentType.CC, "202", "beto@unicesar.edu.co", "pwd", NOW, "2026102", 3, program);
        ReflectionTestUtils.setField(beto, "id", 62L);

        group = new ProjectGroup(moment2, teacher, "Grupo 1");
        ReflectionTestUtils.setField(group, "id", 70L);
        group.setProjectTitle("Sistema SIERAP");
        group.addMember(ana);
        group.addMember(beto);

        business = new ExternalEvaluator("Carlos", "Empresa", DocumentType.CC, "301", "carlos@empresa.com", "pwd", NOW, businessType);
        ReflectionTestUtils.setField(business, "id", 81L);
        group.addEvaluator(business);

        // Docente: RAP1=1.00, RAP2=2.00 (con ajuste de Ana en RAP2 a 0.50). Total grupo = 3.00, Ana = 1.50
        Evaluation evTeacher = new Evaluation(teacher, NOW);
        group.addEvaluation(evTeacher);
        evTeacher.grade(level(criterion1, "1.00"));
        evTeacher.grade(level(criterion2, "2.00"));
        evTeacher.adjust(ana, level(criterion2, "0.50"));
        evTeacher.submit();

        // Empresario: RAP1=0.00, RAP2=2.00. Total = 2.00
        Evaluation evBusiness = new Evaluation(business, NOW);
        group.addEvaluation(evBusiness);
        evBusiness.grade(level(criterion1, "0.00"));
        evBusiness.grade(level(criterion2, "2.00"));
        evBusiness.submit();
        // Con 60% docente y 20% empresario (normalizado 75/25):
        // Nota grupo: 0.75 * 3.00 + 0.25 * 2.00 = 2.75
        // Nota Ana: 0.75 * 1.50 + 0.25 * 2.00 = 1.625 -> 1.63
        // Beto: 2.75
    }

    @Test
    @DisplayName("Polimorfismo: toda subclase de Report produce un ReportData válido a través de generate()")
    void polymorphismReportHierarchy() {
        Report reportInd = new IndividualReport(NOW, ana, group, TODAY);
        Report reportGrp = new GroupReport(NOW, group, TODAY);
        Report reportMom = new MomentReport(NOW, moment2, List.of(group), TODAY);

        // Momento para periodo 1 (para comparative)
        Moment mom1 = createMomentPeriod1();
        ProjectGroup grp1 = createGroupPeriod1(mom1);
        Report reportComp = new ComparativeReport(NOW, mom1, List.of(grp1), moment2, List.of(group), TODAY);

        List<Report> reports = List.of(reportInd, reportGrp, reportMom, reportComp);

        for (Report report : reports) {
            ReportData data = report.generate();
            assertThat(data).isNotNull();
            assertThat(data.generatedAt()).isEqualTo(NOW);
        }

        assertThat(reportInd.generate()).isInstanceOf(IndividualReportData.class);
        assertThat(reportGrp.generate()).isInstanceOf(GroupReportData.class);
        assertThat(reportMom.generate()).isInstanceOf(MomentReportData.class);
        assertThat(reportComp.generate()).isInstanceOf(ComparativeReportData.class);
    }

    @Test
    @DisplayName("IndividualReport calcula nota individual, ajuste, notas por RAP y estado final")
    void individualReportCalculations() {
        IndividualReport report = new IndividualReport(NOW, ana, group, TODAY);
        IndividualReportData data = report.generate();

        assertThat(data.finalResults()).isTrue();
        assertThat(data.student().studentCode()).isEqualTo("2026101");
        assertThat(data.student().fullName()).isEqualTo("Ana Alba");
        assertThat(data.group().name()).isEqualTo("Grupo 1");
        assertThat(data.group().projectTitle()).isEqualTo("Sistema SIERAP");
        assertThat(data.group().teacherName()).isEqualTo("Marta Díaz");

        assertThat(data.individualGrade()).isEqualByComparingTo("1.63");
        assertThat(data.groupGrade()).isEqualByComparingTo("2.75");
        // 1.63 / 3.00 * 100 = 54.33
        assertThat(data.percentage()).isEqualByComparingTo("54.33");

        assertThat(data.learningOutcomes()).hasSize(2);
        // RAP1: 0.75 * 1.00 + 0.25 * 0.00 = 0.75 (max 1.00 -> 75%)
        IndividualReportData.OutcomeScore o1 = data.learningOutcomes().get(0);
        assertThat(o1.code()).isEqualTo("RAP1");
        assertThat(o1.score()).isEqualByComparingTo("0.75");
        assertThat(o1.groupScore()).isEqualByComparingTo("0.75");
        assertThat(o1.percentage()).isEqualByComparingTo("75.00");

        // RAP2: Ana tiene 0.75 * 0.50 + 0.25 * 2.00 = 0.875 -> 0.88 (max 2.00 -> 44%)
        IndividualReportData.OutcomeScore o2 = data.learningOutcomes().get(1);
        assertThat(o2.code()).isEqualTo("RAP2");
        assertThat(o2.score()).isEqualByComparingTo("0.88");
        assertThat(o2.groupScore()).isEqualByComparingTo("2.00");
        assertThat(o2.percentage()).isEqualByComparingTo("44.00");

        assertThat(data.submittedEvaluations()).isEqualTo(2);
        assertThat(data.pendingEvaluations()).isEqualTo(0);
    }

    @Test
    @DisplayName("GroupReport lista miembros alfabéticamente y marca los que tienen ajuste")
    void groupReportCalculations() {
        GroupReport report = new GroupReport(NOW, group, TODAY);
        GroupReportData data = report.generate();

        assertThat(data.finalResults()).isTrue();
        assertThat(data.groupGrade()).isEqualByComparingTo("2.75");
        // 2.75 / 3.00 * 100 = 91.67
        assertThat(data.percentage()).isEqualByComparingTo("91.67");

        assertThat(data.members()).hasSize(2);
        GroupReportData.MemberSummary m0 = data.members().get(0);
        assertThat(m0.fullName()).isEqualTo("Ana Alba");
        assertThat(m0.adjusted()).isTrue();
        assertThat(m0.individualGrade()).isEqualByComparingTo("1.63");

        GroupReportData.MemberSummary m1 = data.members().get(1);
        assertThat(m1.fullName()).isEqualTo("Beto Bravo");
        assertThat(m1.adjusted()).isFalse();
        assertThat(m1.individualGrade()).isEqualByComparingTo("2.75");
    }

    @Test
    @DisplayName("MomentReport consolida promedios, porcentajes de aprobación y distribución")
    void momentReportCalculations() {
        MomentReport report = new MomentReport(NOW, moment2, List.of(group), TODAY);
        MomentReportData data = report.generate();

        assertThat(data.totalGroups()).isEqualTo(1);
        assertThat(data.gradedGroups()).isEqualTo(1);
        assertThat(data.totalStudents()).isEqualTo(2);
        assertThat(data.gradedStudents()).isEqualTo(2);

        // Promedio de estudiantes: (1.63 + 2.75) / 2 = 2.19
        assertThat(data.averageGrade()).isEqualByComparingTo("2.19");
        assertThat(data.minGrade()).isEqualByComparingTo("1.63");
        assertThat(data.maxGrade()).isEqualByComparingTo("2.75");

        // Aprobados (>= 3.00): 0 de 2 -> 0.00%
        assertThat(data.passingCount()).isEqualTo(0);
        assertThat(data.passingPercentage()).isEqualByComparingTo("0.00");

        // Distribución: 4 buckets
        assertThat(data.distribution()).hasSize(4);
        // Ambos están < 3.00 (1.63 y 2.75) -> Bajo: 2 (100%), resto 0
        assertThat(data.distribution().get(0).count()).isEqualTo(2);
        assertThat(data.distribution().get(0).percentage()).isEqualByComparingTo("100.00");
        assertThat(data.distribution().get(1).count()).isEqualTo(0);
        assertThat(data.distribution().get(2).count()).isEqualTo(0);
        assertThat(data.distribution().get(3).count()).isEqualTo(0);
    }

    @Test
    @DisplayName("MomentReport maneja momentos sin grupos calificados")
    void momentReportEmptyGroups() {
        ProjectGroup emptyGroup = new ProjectGroup(moment2, teacher, "Grupo vacío");
        MomentReport report = new MomentReport(NOW, moment2, List.of(emptyGroup), TODAY);
        MomentReportData data = report.generate();

        assertThat(data.totalGroups()).isEqualTo(1);
        assertThat(data.gradedGroups()).isEqualTo(0);
        assertThat(data.totalStudents()).isEqualTo(0);
        assertThat(data.gradedStudents()).isEqualTo(0);
        assertThat(data.averageGrade()).isNull();
        assertThat(data.passingPercentage()).isNull();
    }

    @Test
    @DisplayName("ComparativeReport compara agregados entre dos periodos sin exponer datos individuales")
    void comparativeReportCalculations() {
        Moment mom1 = createMomentPeriod1();
        ProjectGroup grp1 = createGroupPeriod1(mom1);

        ComparativeReport report = new ComparativeReport(NOW, mom1, List.of(grp1), moment2, List.of(group), TODAY);
        ComparativeReportData data = report.generate();

        assertThat(data.momentNumber()).isEqualTo(1);
        assertThat(data.periodA().period().code()).isEqualTo("2026-1");
        assertThat(data.periodB().period().code()).isEqualTo("2026-2");

        // Período A tuvo nota 3.00 (aprobado 100%), Periodo B tuvo promedio 2.19 (aprobado 0%)
        assertThat(data.periodA().averageGrade()).isEqualByComparingTo("3.00");
        assertThat(data.periodB().averageGrade()).isEqualByComparingTo("2.19");
        assertThat(data.deltaAverageGrade()).isEqualByComparingTo("-0.81");

        assertThat(data.periodA().passingPercentage()).isEqualByComparingTo("100.00");
        assertThat(data.periodB().passingPercentage()).isEqualByComparingTo("0.00");
        assertThat(data.deltaPassingPercentage()).isEqualByComparingTo("-100.00");

        // RAPs pareados
        assertThat(data.learningOutcomes()).isNotEmpty();
        for (ComparativeReportData.OutcomeComparison comp : data.learningOutcomes()) {
            assertThat(comp.code()).isIn("RAP1", "RAP2");
        }
    }

    private Moment createMomentPeriod1() {
        Moment mom = new Moment();
        ReflectionTestUtils.setField(mom, "id", 150L);
        ReflectionTestUtils.setField(mom, "period", period1);
        ReflectionTestUtils.setField(mom, "program", program);
        mom.setNumber(1);
        mom.setName("Momento 1");
        mom.setSubject("Programación II");
        mom.setGradingEnd(LocalDate.of(2026, 5, 20));
        mom.addLearningOutcome(rap1);
        mom.addLearningOutcome(rap2);
        mom.setTeacherWeight(new BigDecimal("100"));
        return mom;
    }

    private ProjectGroup createGroupPeriod1(Moment mom) {
        ProjectGroup grp = new ProjectGroup(mom, teacher, "Grupo P1");
        Student est = new Student("Carlos", "Gómez", DocumentType.CC, "501", "carlos@unicesar.edu.co", "pwd", NOW, "2026001", 3, program);
        grp.addMember(est);
        Evaluation ev = new Evaluation(teacher, NOW);
        grp.addEvaluation(ev);
        ev.grade(level(criterion1, "1.00"));
        ev.grade(level(criterion2, "2.00"));
        ev.submit();
        return grp;
    }

    private static EvaluatorType evaluatorType(Long id, String name) {
        EvaluatorType type = new EvaluatorType(name, null);
        ReflectionTestUtils.setField(type, "id", id);
        return type;
    }

    private static EvaluationCriterion criterion(String name, String low, String high) {
        EvaluationCriterion criterion = new EvaluationCriterion(name, 1);
        criterion.addLevel(new PerformanceLevel("Bajo", null, new BigDecimal(low), 1));
        criterion.addLevel(new PerformanceLevel("Alto", null, new BigDecimal(high), 2));
        return criterion;
    }

    private static PerformanceLevel level(EvaluationCriterion criterion, String score) {
        return criterion.getLevels().stream()
                .filter(level -> level.getScore().compareTo(new BigDecimal(score)) == 0)
                .findFirst()
                .orElseThrow();
    }
}
