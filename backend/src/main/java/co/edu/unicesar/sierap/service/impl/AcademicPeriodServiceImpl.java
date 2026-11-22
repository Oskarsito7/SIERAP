package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.request.UpdateAcademicPeriodRequest;
import co.edu.unicesar.sierap.dto.response.AcademicPeriodResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.service.AcademicPeriodService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link AcademicPeriodService}.
 */
@Service
@Transactional(readOnly = true)
public class AcademicPeriodServiceImpl implements AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final MomentRepository momentRepository;
    private final Clock clock;

    /**
     * Crea el servicio.
     *
     * @param academicPeriodRepository repositorio de periodos académicos
     * @param momentRepository         repositorio de momentos
     * @param clock                    reloj del sistema
     */
    public AcademicPeriodServiceImpl(AcademicPeriodRepository academicPeriodRepository,
                                     MomentRepository momentRepository,
                                     Clock clock) {
        this.academicPeriodRepository = academicPeriodRepository;
        this.momentRepository = momentRepository;
        this.clock = clock;
    }

    @Override
    public List<AcademicPeriodResponse> listAll() {
        LocalDate today = LocalDate.now(clock);
        return academicPeriodRepository.findAllByOrderByStartDateDesc().stream()
                .map(period -> toResponse(period, today))
                .toList();
    }

    @Override
    @Transactional
    public AcademicPeriodResponse create(NewAcademicPeriodRequest request) {
        validateDates(request.startDate(), request.endDate(), request.upcEvaluationDeadline());
        String code = request.code().trim();
        if (academicPeriodRepository.existsByCode(code)) {
            throw new DataIntegrityViolationException(
                    "Ya existe un periodo académico con el código '" + code + "'.");
        }
        AcademicPeriod period = new AcademicPeriod(
                code,
                request.startDate(),
                request.endDate(),
                request.upcEvaluationDeadline());
        AcademicPeriod saved = academicPeriodRepository.save(period);
        return toResponse(saved, LocalDate.now(clock));
    }

    @Override
    @Transactional
    public AcademicPeriodResponse update(Long id, UpdateAcademicPeriodRequest request) {
        AcademicPeriod period = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Periodo académico", id));
        validateDates(request.startDate(), request.endDate(), request.upcEvaluationDeadline());

        List<Moment> moments = momentRepository.findByPeriodId(id);
        for (Moment moment : moments) {
            if (moment.getGradingEnd() != null
                    && request.upcEvaluationDeadline().isBefore(moment.getGradingEnd())) {
                throw new BusinessRuleException(
                        "La fecha límite de evaluación UPC no puede ser anterior al cierre de calificación del momento "
                                + moment.getNumber() + " (" + moment.getGradingEnd() + ").");
            }
        }

        String code = request.code().trim();
        if (academicPeriodRepository.existsByCodeAndIdNot(code, id)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otro periodo académico con el código '" + code + "'.");
        }

        period.setCode(code);
        period.setStartDate(request.startDate());
        period.setEndDate(request.endDate());
        period.setUpcEvaluationDeadline(request.upcEvaluationDeadline());
        return toResponse(period, LocalDate.now(clock));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AcademicPeriod period = academicPeriodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Periodo académico", id));
        if (momentRepository.existsByPeriodId(id)) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar el periodo académico porque tiene momentos configurados.");
        }
        academicPeriodRepository.delete(period);
    }

    private static void validateDates(LocalDate startDate, LocalDate endDate, LocalDate upcEvaluationDeadline) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException(
                    "La fecha de fin del periodo no puede ser anterior a la fecha de inicio.");
        }
        if (upcEvaluationDeadline.isBefore(startDate) || upcEvaluationDeadline.isAfter(endDate)) {
            throw new BusinessRuleException(
                    "La fecha límite de evaluación UPC debe estar entre la fecha de inicio y la fecha de fin del periodo.");
        }
    }

    private static AcademicPeriodResponse toResponse(AcademicPeriod period, LocalDate today) {
        String status;
        if (today.isBefore(period.getStartDate())) {
            status = "UPCOMING";
        } else if (today.isAfter(period.getEndDate())) {
            status = "CLOSED";
        } else {
            status = "CURRENT";
        }
        return new AcademicPeriodResponse(
                period.getId(),
                period.getCode(),
                period.getStartDate(),
                period.getEndDate(),
                period.getUpcEvaluationDeadline(),
                status);
    }
}
