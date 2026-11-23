package co.edu.unicesar.sierap.service.impl;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.SetGlobalParameterRequest;
import co.edu.unicesar.sierap.dto.response.GlobalParameterResponse;
import co.edu.unicesar.sierap.entity.GlobalParameter;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.repository.GlobalParameterRepository;
import co.edu.unicesar.sierap.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link GlobalParameterServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class GlobalParameterServiceImplTest {

    @Mock
    private GlobalParameterRepository globalParameterRepository;

    @Mock
    private AuditService auditService;

    private GlobalParameterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new GlobalParameterServiceImpl(globalParameterRepository, auditService);
    }

    @Test
    void listAllAndSetCreatesOrUpdates() {
        GlobalParameter existing = new GlobalParameter("min_passing_grade", "3.00", "Nota mínima");
        when(globalParameterRepository.findAllByOrderByKeyAsc()).thenReturn(List.of(existing));

        List<GlobalParameterResponse> list = service.listAll();
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().key()).isEqualTo("min_passing_grade");

        when(globalParameterRepository.findByKey("min_passing_grade")).thenReturn(Optional.of(existing));
        GlobalParameterResponse updated = service.set(
                "min_passing_grade", new SetGlobalParameterRequest("3.50", null));
        assertThat(updated.value()).isEqualTo("3.50");
        assertThat(updated.description()).isEqualTo("Nota mínima");

        when(globalParameterRepository.findByKey("new_key")).thenReturn(Optional.empty());
        when(globalParameterRepository.save(any(GlobalParameter.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GlobalParameterResponse created = service.set(
                "new_key", new SetGlobalParameterRequest("10", "Nuevo parámetro"));
        assertThat(created.key()).isEqualTo("new_key");
        assertThat(created.value()).isEqualTo("10");
        assertThat(created.description()).isEqualTo("Nuevo parámetro");

        verify(auditService).record(eq(AuditAction.GLOBAL_PARAMETER_CHANGED), eq("GlobalParameter"), any(), eq(null),
                eq("Cambió el parámetro min_passing_grade de \"3.00\" a \"3.50\"."));
        verify(auditService).record(eq(AuditAction.GLOBAL_PARAMETER_CREATED), eq("GlobalParameter"), any(), eq(null),
                eq("Creó el parámetro new_key con el valor \"10\"."));
    }
}
