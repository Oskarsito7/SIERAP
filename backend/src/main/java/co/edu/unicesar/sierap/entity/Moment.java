package co.edu.unicesar.sierap.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.enums.MomentStatus;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Momento: etapa de evaluación de un {@link Program} en un {@link AcademicPeriod}
 * (p. ej. "Momento 1" de Ingeniería de Sistemas en 2026-2).
 *
 * <p>Tiene un ciclo de vida (ADR-012): en {@link MomentStatus#CONFIGURATION} el
 * admin define sus RAP ({@link LearningOutcome}), criterios, niveles, integrantes
 * por grupo, ventanas de fechas y el peso del docente y de cada tipo de evaluador
 * en la nota final ({@link EvaluatorWeight}), y los estudiantes se inscriben en grupos; al
 * lanzarlo ({@link MomentStatus#LAUNCHED}) la configuración queda bloqueada y las
 * ventanas indican cuándo se entrega y cuándo se califica. Las ventanas pueden
 * estar vacías mientras el momento se configura.</p>
 *
 * <p>El estado solo cambia con {@link #launch()}, que valida que la configuración
 * esté completa; después de lanzar, los setters de configuración y los métodos
 * para agregar o quitar RAP lanzan {@link BusinessRuleException}.</p>
 *
 * <p>Es dueño de sus RAP y de sus pesos de evaluador (composición) y se asocia a un
 * periodo, que existe independientemente del momento.</p>
 */
@Entity
@Table(name = "moment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"program_id", "period_id", "number"}))
public class Moment {

    /** Suma que deben dar los pesos del momento. */
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Programa dueño del momento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    /** Periodo académico en el que se configura el momento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private AcademicPeriod period;

    /** Número del momento dentro del programa y el periodo (1, 2, 3...). */
    @Column(nullable = false)
    private int number;

    /** Nombre visible del momento, p. ej. "Momento 1". */
    @Column(nullable = false, length = 100)
    private String name;

    /** Asignatura en la que se evalúa el momento. */
    @Column(nullable = false, length = 150)
    private String subject;

    /** Primer semestre que abarca el momento. */
    @Column(nullable = false)
    private int semesterFrom;

    /** Último semestre que abarca el momento. */
    @Column(nullable = false)
    private int semesterTo;

    /** Nivel de exigencia en texto libre (ADR-010), p. ej. "Bajo". */
    @Column(nullable = false, length = 100)
    private String requirementLevel;

    /** Mínimo de estudiantes por grupo. */
    @Column(nullable = false)
    private int minMembers;

    /** Máximo de estudiantes por grupo. */
    @Column(nullable = false)
    private int maxMembers;

    /** Estado del ciclo de vida; todo momento nuevo empieza en configuración. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MomentStatus status = MomentStatus.CONFIGURATION;

    /** Primer día de la ventana de conformación de grupos. */
    private LocalDate groupFormationStart;

    /** Último día de la ventana de conformación de grupos. */
    private LocalDate groupFormationEnd;

    /** Primer día de la ventana de entregas. */
    private LocalDate submissionStart;

    /** Último día de la ventana de entregas. */
    private LocalDate submissionEnd;

    /** Primer día de la ventana de calificación. */
    private LocalDate gradingStart;

    /** Último día de la ventana de calificación. */
    private LocalDate gradingEnd;

    /** RAP evaluados en el momento. */
    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("code")
    private List<LearningOutcome> learningOutcomes = new ArrayList<>();

    /**
     * Porcentaje de la nota final que aporta el docente (ADR-018); un momento nuevo
     * empieza con el docente al 100 %.
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal teacherWeight = ONE_HUNDRED;

    /** Pesos de los tipos de evaluador externo que participan en el momento. */
    @OneToMany(mappedBy = "moment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EvaluatorWeight> evaluatorWeights = new ArrayList<>();

    /** Constructor requerido por JPA. */
    protected Moment() {
    }

    /**
     * Crea un momento en estado {@link MomentStatus#CONFIGURATION}, sin RAP ni
     * ventanas de fechas. Se asocia a su programa con {@link Program#addMoment(Moment)}.
     *
     * @param period           periodo académico en el que se configura
     * @param number           número del momento dentro del programa y el periodo
     * @param name             nombre visible, p. ej. "Momento 1"
     * @param subject          asignatura en la que se evalúa
     * @param semesterFrom     primer semestre que abarca
     * @param semesterTo       último semestre que abarca
     * @param requirementLevel nivel de exigencia en texto libre
     * @param minMembers       mínimo de estudiantes por grupo
     * @param maxMembers       máximo de estudiantes por grupo
     */
    public Moment(AcademicPeriod period, int number, String name, String subject,
                  int semesterFrom, int semesterTo, String requirementLevel,
                  int minMembers, int maxMembers) {
        this.period = period;
        this.number = number;
        this.name = name;
        this.subject = subject;
        this.semesterFrom = semesterFrom;
        this.semesterTo = semesterTo;
        this.requirementLevel = requirementLevel;
        this.minMembers = minMembers;
        this.maxMembers = maxMembers;
    }

    /**
     * Lanza el momento (ADR-012): valida que la configuración esté completa y pasa a
     * {@link MomentStatus#LAUNCHED}, con lo que la configuración queda bloqueada.
     *
     * <p>Exige las tres ventanas de fechas completas, cada una con inicio antes o
     * igual a su fin, ordenadas y sin solaparse (conformación, luego entregas, luego
     * calificación), la de calificación terminando a más tardar en el límite de
     * evaluación de la UPC del periodo, al menos un RAP en el que cada criterio
     * tenga niveles de desempeño, y que el peso del docente más los de los tipos de
     * evaluador sumen exactamente 100 % (ADR-018). Se puede lanzar antes de que termine la ventana de
     * conformación: lanzar cierra la conformación de grupos.</p>
     *
     * @throws BusinessRuleException si el momento ya fue lanzado o su configuración
     *                               está incompleta o es inválida
     */
    public void launch() {
        requireConfiguration();
        if (groupFormationStart == null || groupFormationEnd == null
                || submissionStart == null || submissionEnd == null
                || gradingStart == null || gradingEnd == null) {
            throw new BusinessRuleException(
                    "Para lanzar el momento se deben definir las tres ventanas de fechas.");
        }
        requireWindow("conformación de grupos", groupFormationStart, groupFormationEnd);
        requireWindow("entregas", submissionStart, submissionEnd);
        requireWindow("calificación", gradingStart, gradingEnd);
        if (!groupFormationEnd.isBefore(submissionStart)) {
            throw new BusinessRuleException(
                    "La ventana de entregas debe empezar después de la de conformación de grupos.");
        }
        if (!submissionEnd.isBefore(gradingStart)) {
            throw new BusinessRuleException(
                    "La ventana de calificación debe empezar después de la de entregas.");
        }
        LocalDate upcDeadline = period.getUpcEvaluationDeadline();
        if (gradingEnd.isAfter(upcDeadline)) {
            throw new BusinessRuleException("La ventana de calificación debe terminar a más tardar el "
                    + upcDeadline + ", límite de evaluación de la UPC.");
        }
        if (learningOutcomes.isEmpty()) {
            throw new BusinessRuleException("El momento debe tener al menos un RAP.");
        }
        for (LearningOutcome outcome : learningOutcomes) {
            if (outcome.getCriteria().isEmpty()) {
                throw new BusinessRuleException(
                        "El " + outcome.getCode() + " debe tener al menos un criterio.");
            }
            for (EvaluationCriterion criterion : outcome.getCriteria()) {
                if (criterion.getLevels().isEmpty()) {
                    throw new BusinessRuleException("El criterio \"" + criterion.getDescription()
                            + "\" del " + outcome.getCode() + " debe tener niveles de desempeño.");
                }
            }
        }
        BigDecimal totalWeight = getTotalWeight();
        if (totalWeight.compareTo(ONE_HUNDRED) != 0) {
            throw new BusinessRuleException("Los pesos de los evaluadores deben sumar 100 % (hoy suman "
                    + totalWeight.stripTrailingZeros().toPlainString() + " %).");
        }
        status = MomentStatus.LAUNCHED;
    }

    /**
     * Indica si hoy se pueden conformar grupos: el momento sigue en configuración y
     * la fecha cae dentro de la ventana de conformación.
     *
     * @param today fecha actual, obtenida del {@code Clock} inyectado en el service
     * @return {@code true} si el momento está en configuración y dentro de la ventana
     */
    public boolean isInGroupFormation(LocalDate today) {
        return status == MomentStatus.CONFIGURATION
                && isWithin(groupFormationStart, groupFormationEnd, today);
    }

    /**
     * Indica si hoy se pueden subir entregas: el momento está lanzado y la fecha cae
     * dentro de la ventana de entregas.
     *
     * @param today fecha actual, obtenida del {@code Clock} inyectado en el service
     * @return {@code true} si el momento está lanzado y dentro de la ventana
     */
    public boolean isInSubmission(LocalDate today) {
        return status == MomentStatus.LAUNCHED
                && isWithin(submissionStart, submissionEnd, today);
    }

    /**
     * Indica si hoy se puede calificar: el momento está lanzado y la fecha cae dentro
     * de la ventana de calificación.
     *
     * @param today fecha actual, obtenida del {@code Clock} inyectado en el service
     * @return {@code true} si el momento está lanzado y dentro de la ventana
     */
    public boolean isInGrading(LocalDate today) {
        return status == MomentStatus.LAUNCHED
                && isWithin(gradingStart, gradingEnd, today);
    }

    /**
     * Puntaje máximo que se puede obtener en el momento: la suma de los puntajes
     * máximos de sus RAP.
     *
     * @return suma de {@link LearningOutcome#getMaxScore()}; cero si no tiene RAP
     */
    public BigDecimal getMaxScore() {
        return learningOutcomes.stream()
                .map(LearningOutcome::getMaxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Agrega un RAP al momento y fija el momento del RAP.
     *
     * @param learningOutcome RAP a agregar
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void addLearningOutcome(LearningOutcome learningOutcome) {
        requireConfiguration();
        learningOutcomes.add(learningOutcome);
        learningOutcome.setMoment(this);
    }

    /**
     * Quita un RAP del momento; al guardar, el RAP se elimina con sus criterios.
     *
     * @param learningOutcome RAP a quitar
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void removeLearningOutcome(LearningOutcome learningOutcome) {
        requireConfiguration();
        learningOutcomes.remove(learningOutcome);
        learningOutcome.setMoment(null);
    }

    /**
     * Rechaza cambios de configuración si el momento ya fue lanzado (ADR-012).
     *
     * @throws BusinessRuleException si el momento está en {@link MomentStatus#LAUNCHED}
     */
    private void requireConfiguration() {
        if (status == MomentStatus.LAUNCHED) {
            throw new BusinessRuleException(
                    "El momento ya fue lanzado: su configuración no se puede modificar.");
        }
    }

    /**
     * Verifica que una ventana de fechas empiece antes o el mismo día en que termina.
     *
     * @throws BusinessRuleException si el inicio es posterior al fin
     */
    private static void requireWindow(String window, LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BusinessRuleException(
                    "La ventana de " + window + " no puede terminar antes de empezar.");
        }
    }

    /**
     * Indica si una fecha cae dentro de una ventana, incluyendo sus extremos. Una
     * ventana sin fechas definidas no contiene ninguna fecha.
     */
    private static boolean isWithin(LocalDate start, LocalDate end, LocalDate date) {
        return start != null && end != null
                && !date.isBefore(start) && !date.isAfter(end);
    }

    /** @return identificador del momento */
    public Long getId() {
        return id;
    }

    /** @return programa dueño del momento */
    public Program getProgram() {
        return program;
    }

    /**
     * Fija el programa del momento. Usar {@link Program#addMoment(Moment)} para
     * mantener sincronizados los dos lados de la relación.
     *
     * @param program programa dueño del momento
     */
    void setProgram(Program program) {
        this.program = program;
    }

    /** @return periodo académico del momento */
    public AcademicPeriod getPeriod() {
        return period;
    }

    /**
     * @param period periodo académico en el que se configura el momento
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setPeriod(AcademicPeriod period) {
        requireConfiguration();
        this.period = period;
    }

    /** @return número del momento */
    public int getNumber() {
        return number;
    }

    /**
     * @param number nuevo número del momento
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setNumber(int number) {
        requireConfiguration();
        this.number = number;
    }

    /** @return nombre visible del momento */
    public String getName() {
        return name;
    }

    /**
     * @param name nuevo nombre visible
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setName(String name) {
        requireConfiguration();
        this.name = name;
    }

    /** @return asignatura en la que se evalúa */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject nueva asignatura
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setSubject(String subject) {
        requireConfiguration();
        this.subject = subject;
    }

    /** @return primer semestre que abarca */
    public int getSemesterFrom() {
        return semesterFrom;
    }

    /**
     * @param semesterFrom nuevo primer semestre
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setSemesterFrom(int semesterFrom) {
        requireConfiguration();
        this.semesterFrom = semesterFrom;
    }

    /** @return último semestre que abarca */
    public int getSemesterTo() {
        return semesterTo;
    }

    /**
     * @param semesterTo nuevo último semestre
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setSemesterTo(int semesterTo) {
        requireConfiguration();
        this.semesterTo = semesterTo;
    }

    /** @return nivel de exigencia en texto libre */
    public String getRequirementLevel() {
        return requirementLevel;
    }

    /**
     * @param requirementLevel nuevo nivel de exigencia
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setRequirementLevel(String requirementLevel) {
        requireConfiguration();
        this.requirementLevel = requirementLevel;
    }

    /** @return mínimo de estudiantes por grupo */
    public int getMinMembers() {
        return minMembers;
    }

    /**
     * @param minMembers nuevo mínimo de estudiantes por grupo
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setMinMembers(int minMembers) {
        requireConfiguration();
        this.minMembers = minMembers;
    }

    /** @return máximo de estudiantes por grupo */
    public int getMaxMembers() {
        return maxMembers;
    }

    /**
     * @param maxMembers nuevo máximo de estudiantes por grupo
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setMaxMembers(int maxMembers) {
        requireConfiguration();
        this.maxMembers = maxMembers;
    }

    /** @return estado del ciclo de vida */
    public MomentStatus getStatus() {
        return status;
    }

    /** @return primer día de la ventana de conformación, o {@code null} */
    public LocalDate getGroupFormationStart() {
        return groupFormationStart;
    }

    /**
     * @param groupFormationStart primer día de la ventana de conformación
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setGroupFormationStart(LocalDate groupFormationStart) {
        requireConfiguration();
        this.groupFormationStart = groupFormationStart;
    }

    /** @return último día de la ventana de conformación, o {@code null} */
    public LocalDate getGroupFormationEnd() {
        return groupFormationEnd;
    }

    /**
     * @param groupFormationEnd último día de la ventana de conformación
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setGroupFormationEnd(LocalDate groupFormationEnd) {
        requireConfiguration();
        this.groupFormationEnd = groupFormationEnd;
    }

    /** @return primer día de la ventana de entregas, o {@code null} */
    public LocalDate getSubmissionStart() {
        return submissionStart;
    }

    /**
     * @param submissionStart primer día de la ventana de entregas
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setSubmissionStart(LocalDate submissionStart) {
        requireConfiguration();
        this.submissionStart = submissionStart;
    }

    /** @return último día de la ventana de entregas, o {@code null} */
    public LocalDate getSubmissionEnd() {
        return submissionEnd;
    }

    /**
     * @param submissionEnd último día de la ventana de entregas
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setSubmissionEnd(LocalDate submissionEnd) {
        requireConfiguration();
        this.submissionEnd = submissionEnd;
    }

    /** @return primer día de la ventana de calificación, o {@code null} */
    public LocalDate getGradingStart() {
        return gradingStart;
    }

    /**
     * @param gradingStart primer día de la ventana de calificación
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setGradingStart(LocalDate gradingStart) {
        requireConfiguration();
        this.gradingStart = gradingStart;
    }

    /** @return último día de la ventana de calificación, o {@code null} */
    public LocalDate getGradingEnd() {
        return gradingEnd;
    }

    /**
     * @param gradingEnd último día de la ventana de calificación
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void setGradingEnd(LocalDate gradingEnd) {
        requireConfiguration();
        this.gradingEnd = gradingEnd;
    }

    /** @return RAP del momento, en solo lectura (usar add/remove) */
    public List<LearningOutcome> getLearningOutcomes() {
        return Collections.unmodifiableList(learningOutcomes);
    }

    /** @return porcentaje de la nota final que aporta el docente */
    public BigDecimal getTeacherWeight() {
        return teacherWeight;
    }

    /**
     * @param teacherWeight porcentaje del docente: mayor que cero y hasta 100, con
     *                      dos decimales como máximo
     * @throws BusinessRuleException si el momento ya fue lanzado o el peso es inválido
     */
    public void setTeacherWeight(BigDecimal teacherWeight) {
        requireConfiguration();
        requireValidWeight(teacherWeight);
        this.teacherWeight = teacherWeight;
    }

    /** @return pesos de los tipos de evaluador externo en el momento, solo lectura */
    public List<EvaluatorWeight> getEvaluatorWeights() {
        return Collections.unmodifiableList(evaluatorWeights);
    }

    /**
     * Peso de un tipo de evaluador en el momento.
     *
     * @param evaluatorType tipo de evaluador
     * @return su porcentaje, o cero si el tipo no tiene peso en el momento
     */
    public BigDecimal getWeightFor(EvaluatorType evaluatorType) {
        return findEvaluatorWeight(evaluatorType)
                .map(EvaluatorWeight::getWeight)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Indica si un tipo de evaluador tiene peso en el momento. Si no lo tiene, los
     * evaluadores de ese tipo pueden ser designados pero su calificación no cuenta
     * en la nota final (decisión del 16/10/2026).
     *
     * @param evaluatorType tipo de evaluador
     * @return {@code true} si el tipo tiene peso en el momento
     */
    public boolean hasWeightFor(EvaluatorType evaluatorType) {
        return findEvaluatorWeight(evaluatorType).isPresent();
    }

    /**
     * Asigna o cambia el peso de un tipo de evaluador en el momento.
     *
     * @param evaluatorType tipo de evaluador
     * @param weight        porcentaje: mayor que cero y hasta 100, con dos
     *                      decimales como máximo
     * @throws BusinessRuleException si el momento ya fue lanzado o el peso es inválido
     */
    public void setEvaluatorWeight(EvaluatorType evaluatorType, BigDecimal weight) {
        requireConfiguration();
        requireValidWeight(weight);
        findEvaluatorWeight(evaluatorType).ifPresentOrElse(
                existing -> existing.setWeight(weight),
                () -> evaluatorWeights.add(new EvaluatorWeight(this, evaluatorType, weight)));
    }

    /**
     * Quita el peso de un tipo de evaluador; el tipo deja de participar en el momento.
     *
     * @param evaluatorType tipo de evaluador
     * @throws BusinessRuleException si el momento ya fue lanzado
     */
    public void removeEvaluatorWeight(EvaluatorType evaluatorType) {
        requireConfiguration();
        findEvaluatorWeight(evaluatorType).ifPresent(evaluatorWeights::remove);
    }

    /**
     * Suma de los pesos del momento: el del docente más los de los tipos de evaluador.
     *
     * @return suma de los porcentajes; debe ser 100 para lanzar el momento
     */
    public BigDecimal getTotalWeight() {
        return evaluatorWeights.stream()
                .map(EvaluatorWeight::getWeight)
                .reduce(teacherWeight, BigDecimal::add);
    }

    /** Busca el peso de un tipo de evaluador, comparando por identificador si lo tiene. */
    private Optional<EvaluatorWeight> findEvaluatorWeight(EvaluatorType evaluatorType) {
        return evaluatorWeights.stream()
                .filter(w -> isSameType(w.getEvaluatorType(), evaluatorType))
                .findFirst();
    }

    private static boolean isSameType(EvaluatorType a, EvaluatorType b) {
        return a == b || (a.getId() != null && a.getId().equals(b.getId()));
    }

    /**
     * Verifica que un peso sea un porcentaje mayor que cero y hasta 100, con dos
     * decimales como máximo (ADR-013).
     *
     * @throws BusinessRuleException si el peso es inválido
     */
    private static void requireValidWeight(BigDecimal weight) {
        if (weight == null || weight.signum() <= 0 || weight.compareTo(ONE_HUNDRED) > 0
                || weight.stripTrailingZeros().scale() > 2) {
            throw new BusinessRuleException(
                    "Cada peso debe ser un porcentaje mayor que 0 y hasta 100, con dos decimales como máximo.");
        }
    }
}
