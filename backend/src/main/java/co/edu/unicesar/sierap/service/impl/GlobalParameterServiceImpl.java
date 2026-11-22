package co.edu.unicesar.sierap.service.impl;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.SetGlobalParameterRequest;
import co.edu.unicesar.sierap.dto.response.GlobalParameterResponse;
import co.edu.unicesar.sierap.entity.GlobalParameter;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.repository.GlobalParameterRepository;
import co.edu.unicesar.sierap.service.GlobalParameterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link GlobalParameterService}.
 */
@Service
@Transactional(readOnly = true)
public class GlobalParameterServiceImpl implements GlobalParameterService {

    private final GlobalParameterRepository globalParameterRepository;

    /**
     * Crea el servicio.
     *
     * @param globalParameterRepository repositorio de parámetros globales
     */
    public GlobalParameterServiceImpl(GlobalParameterRepository globalParameterRepository) {
        this.globalParameterRepository = globalParameterRepository;
    }

    @Override
    public List<GlobalParameterResponse> listAll() {
        return globalParameterRepository.findAllByOrderByKeyAsc().stream()
                .map(GlobalParameterServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GlobalParameterResponse set(String key, SetGlobalParameterRequest request) {
        if (key == null || key.isBlank()) {
            throw new BusinessRuleException("La clave del parámetro global es obligatoria.");
        }
        String cleanKey = key.trim();
        if (cleanKey.length() > 100) {
            throw new BusinessRuleException("La clave del parámetro no puede superar 100 caracteres.");
        }
        String value = request.value().trim();
        String description = request.description() != null ? request.description().trim() : null;

        Optional<GlobalParameter> existing = globalParameterRepository.findByKey(cleanKey);
        if (existing.isPresent()) {
            GlobalParameter param = existing.get();
            param.setValue(value);
            if (description != null) {
                param.setDescription(description);
            }
            return toResponse(param);
        }

        GlobalParameter created = new GlobalParameter(cleanKey, value, description);
        GlobalParameter saved = globalParameterRepository.save(created);
        return toResponse(saved);
    }

    private static GlobalParameterResponse toResponse(GlobalParameter parameter) {
        return new GlobalParameterResponse(
                parameter.getKey(),
                parameter.getValue(),
                parameter.getDescription());
    }
}
