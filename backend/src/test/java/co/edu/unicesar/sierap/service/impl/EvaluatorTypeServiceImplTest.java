package co.edu.unicesar.sierap.service.impl;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.NewEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.request.UpdateEvaluatorTypeRequest;
import co.edu.unicesar.sierap.dto.response.EvaluatorTypeResponse;
import co.edu.unicesar.sierap.entity.EvaluatorType;
import co.edu.unicesar.sierap.repository.EvaluatorTypeRepository;
import co.edu.unicesar.sierap.repository.ExternalEvaluatorRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link EvaluatorTypeServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class EvaluatorTypeServiceImplTest {

    @Mock
    private EvaluatorTypeRepository evaluatorTypeRepository;

    @Mock
    private ExternalEvaluatorRepository externalEvaluatorRepository;

    @Mock
    private MomentRepository momentRepository;

    private EvaluatorTypeServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EvaluatorTypeServiceImpl(
                evaluatorTypeRepository,
                externalEvaluatorRepository,
                momentRepository);
    }

    @Test
    void listAllIncludesEvaluatorCount() {
        EvaluatorType t = new EvaluatorType("Empresario", "Representante empresarial");
        ReflectionTestUtils.setField(t, "id", 1L);
        when(evaluatorTypeRepository.findAll(Sort.by("name"))).thenReturn(List.of(t));
        when(externalEvaluatorRepository.countByEvaluatorTypeId(1L)).thenReturn(7);

        List<EvaluatorTypeResponse> result = service.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().evaluatorCount()).isEqualTo(7);
    }

    @Test
    void createUpdateAndDeleteEvaluatorType() {
        when(evaluatorTypeRepository.existsByNameIgnoreCase("Sector Productivo")).thenReturn(false);
        when(evaluatorTypeRepository.save(any(EvaluatorType.class))).thenAnswer(inv -> {
            EvaluatorType t = inv.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 3L);
            return t;
        });

        EvaluatorTypeResponse created = service.create(
                new NewEvaluatorTypeRequest("Sector Productivo", "Empresas del sector"));
        assertThat(created.id()).isEqualTo(3L);
        assertThat(created.active()).isTrue();

        EvaluatorType existing = new EvaluatorType("Sector Productivo", "Empresas del sector");
        ReflectionTestUtils.setField(existing, "id", 3L);
        when(evaluatorTypeRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(evaluatorTypeRepository.existsByNameIgnoreCaseAndIdNot("Aliado Externo", 3L)).thenReturn(false);

        EvaluatorTypeResponse updated = service.update(
                3L, new UpdateEvaluatorTypeRequest("Aliado Externo", "Nueva desc", false));
        assertThat(updated.name()).isEqualTo("Aliado Externo");
        assertThat(updated.active()).isFalse();

        when(externalEvaluatorRepository.existsByEvaluatorTypeId(3L)).thenReturn(false);
        when(momentRepository.existsByEvaluatorWeightsEvaluatorTypeId(3L)).thenReturn(false);

        service.delete(3L);
        verify(evaluatorTypeRepository).delete(existing);

        when(momentRepository.existsByEvaluatorWeightsEvaluatorTypeId(3L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
