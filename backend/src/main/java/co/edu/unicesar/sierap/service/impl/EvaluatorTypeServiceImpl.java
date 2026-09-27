package co.edu.unicesar.sierap.service.impl;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.service.EvaluatorTypeService;
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

    /**
     * Crea el servicio.
     *
     * @param evaluatorTypeRepository repositorio del catálogo
     */
    public EvaluatorTypeServiceImpl(EvaluatorTypeRepository evaluatorTypeRepository) {
        this.evaluatorTypeRepository = evaluatorTypeRepository;
    }

    @Override
    public List<EvaluatorTypeResponse> listAll() {
        return evaluatorTypeRepository.findAll(Sort.by("name")).stream()
                .map(type -> new EvaluatorTypeResponse(type.getId(), type.getName(), type.getDescription(),
                        type.isActive()))
                .toList();
    }
}
