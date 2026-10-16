package co.edu.unicesar.sierap.service.impl;

import java.util.List;

import co.edu.unicesar.sierap.dto.request.NewEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.ExternalEvaluatorRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.service.EvaluatorTypeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link EvaluatorTypeService}.
 */
@Service
@Transactional(readOnly = true)
public class EvaluatorTypeServiceImpl implements EvaluatorTypeService {

    private final EvaluatorTypeRepository evaluatorTypeRepository;
    private final ExternalEvaluatorRepository externalEvaluatorRepository;
    private final MomentRepository momentRepository;

    /**
     * Crea el servicio.
     *
     * @param evaluatorTypeRepository     repositorio del catálogo
     * @param externalEvaluatorRepository repositorio de evaluadores externos
     * @param momentRepository            repositorio de momentos
     */
    public EvaluatorTypeServiceImpl(EvaluatorTypeRepository evaluatorTypeRepository,
                                    ExternalEvaluatorRepository externalEvaluatorRepository,
                                    MomentRepository momentRepository) {
        this.evaluatorTypeRepository = evaluatorTypeRepository;
        this.externalEvaluatorRepository = externalEvaluatorRepository;
        this.momentRepository = momentRepository;
    }

    @Override
    public List<EvaluatorTypeResponse> listAll() {
        return evaluatorTypeRepository.findAll(Sort.by("name")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EvaluatorTypeResponse create(NewEvaluatorTypeRequest request) {
        String name = request.name().trim();
        if (evaluatorTypeRepository.existsByNameIgnoreCase(name)) {
            throw new DataIntegrityViolationException(
                    "Ya existe un tipo de evaluador con el nombre '" + name + "'.");
        }
        String description = request.description() != null ? request.description().trim() : null;
        EvaluatorType type = new EvaluatorType(name, description);
        EvaluatorType saved = evaluatorTypeRepository.save(type);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public EvaluatorTypeResponse update(Long id, UpdateEvaluatorTypeRequest request) {
        EvaluatorType type = evaluatorTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evaluador", id));
        String name = request.name().trim();
        if (evaluatorTypeRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DataIntegrityViolationException(
                    "Ya existe otro tipo de evaluador con el nombre '" + name + "'.");
        }
        type.setName(name);
        type.setDescription(request.description() != null ? request.description().trim() : null);
        type.setActive(request.active());
        return toResponse(type);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        EvaluatorType type = evaluatorTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evaluador", id));
        if (externalEvaluatorRepository.existsByEvaluatorTypeId(id)
                || momentRepository.existsByEvaluatorWeightsEvaluatorTypeId(id)) {
            throw new DataIntegrityViolationException(
                    "No se puede eliminar el tipo de evaluador porque está siendo utilizado por evaluadores o momentos.");
        }
        evaluatorTypeRepository.delete(type);
    }

    private EvaluatorTypeResponse toResponse(EvaluatorType type) {
        int evaluatorCount = type.getId() != null
                ? externalEvaluatorRepository.countByEvaluatorTypeId(type.getId())
                : 0;
        return new EvaluatorTypeResponse(
                type.getId(),
                type.getName(),
                type.getDescription(),
                type.isActive(),
                evaluatorCount);
    }
}
