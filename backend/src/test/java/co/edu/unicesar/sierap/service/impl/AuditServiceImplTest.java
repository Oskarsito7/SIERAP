package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.response.AuditLogPageResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.AuditLog;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.enums.AuditAction;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.repository.AuditLogRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Pruebas unitarias de {@link AuditServiceImpl}: el actor y la IP son opcionales, el
 * resumen se recorta al ancho de la columna y la consulta aplica alcance y paginación.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T14:30:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccessScope accessScope;

    private AuditServiceImpl service;
    private Admin admin;

    @BeforeEach
    void setUp() {
        service = new AuditServiceImpl(auditLogRepository, userRepository, accessScope, FIXED_CLOCK);
        Faculty faculty = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(faculty, "id", 10L);
        admin = new Admin("Carlos", "Ramos", DocumentType.CC, "67890", "admin@unicesar.edu.co", "hash",
                LocalDateTime.now(FIXED_CLOCK), faculty);
        ReflectionTestUtils.setField(admin, "id", 60L);
    }

    @AfterEach
    void clearContexts() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsWithoutActorNorRequest() {
        service.record(AuditAction.MOMENT_LAUNCHED, "Moment", 7L, 10L, "Lanzó el momento.");

        AuditLog saved = captureSaved();
        assertThat(saved.getActorId()).isNull();
        assertThat(saved.getActorName()).isNull();
        assertThat(saved.getIp()).isNull();
        assertThat(saved.getAction()).isEqualTo(AuditAction.MOMENT_LAUNCHED);
        assertThat(saved.getEntityType()).isEqualTo("Moment");
        assertThat(saved.getEntityId()).isEqualTo(7L);
        assertThat(saved.getFacultyId()).isEqualTo(10L);
        assertThat(saved.getSummary()).isEqualTo("Lanzó el momento.");
        assertThat(saved.getCreatedAt()).isEqualTo(LocalDateTime.now(FIXED_CLOCK));
    }

    @Test
    void takesTheAuthenticatedActorAndTheRequestIp() {
        AuthenticatedUser principal = new AuthenticatedUser(60L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.1.2.3");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(userRepository.findById(60L)).thenReturn(Optional.of(admin));

        service.record(AuditAction.ACCOUNT_UPDATED, "User", 5L, null, "Cambió el correo.");

        AuditLog saved = captureSaved();
        assertThat(saved.getActorId()).isEqualTo(60L);
        assertThat(saved.getActorName()).isEqualTo("Carlos Ramos");
        assertThat(saved.getIp()).isEqualTo("10.1.2.3");
        assertThat(saved.getFacultyId()).isNull();
    }

    @Test
    void fallsBackToTheEmailWhenTheActorAccountCannotBeLoaded() {
        AuthenticatedUser principal = new AuthenticatedUser(61L, "fantasma@unicesar.edu.co", UserRole.ADMIN, false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(userRepository.findById(61L)).thenReturn(Optional.empty());

        service.record(AuditAction.ACCOUNT_UPDATED, "User", 5L, null, "Cambió el correo.");

        assertThat(captureSaved().getActorName()).isEqualTo("fantasma@unicesar.edu.co");
    }

    @Test
    void aTooLongSummaryIsTrimmedToTheColumnWidth() {
        service.record(AuditAction.ACCOUNT_UPDATED, "User", 5L, null, "x".repeat(1500));

        assertThat(captureSaved().getSummary()).hasSize(1000).endsWith("…");
    }

    @Test
    void searchAppliesDefaultsAndClampsTheSize() {
        AuthenticatedUser actor = new AuthenticatedUser(60L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
        when(accessScope.loadActor(actor)).thenReturn(admin);
        when(auditLogRepository.findAll(anySpec(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 50), 0));

        AuditLogPageResponse defaults = service.search(actor, null, null, null, null, null, null);
        assertThat(defaults.page()).isZero();
        assertThat(defaults.size()).isEqualTo(50);

        AuditLogPageResponse clamped = service.search(actor, -3, 5000, null, null, null, "  ");
        assertThat(clamped.page()).isZero();
        assertThat(clamped.size()).isEqualTo(200);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository, org.mockito.Mockito.times(2)).findAll(anySpec(), pageable.capture());
        assertThat(pageable.getAllValues().get(1).getPageSize()).isEqualTo(200);
        assertThat(pageable.getAllValues().get(1).getSort().toString()).contains("createdAt: DESC");
    }

    @Test
    void searchRejectsAnInvertedDateRange() {
        AuthenticatedUser actor = new AuthenticatedUser(60L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
        when(accessScope.loadActor(actor)).thenReturn(admin);

        assertThatThrownBy(() -> service.search(actor, 0, 10, null, LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 1), null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void searchIsForbiddenForRolesOtherThanAdmins() {
        Faculty faculty = new Faculty("04", "Ingenierías");
        Teacher teacher = new Teacher("Ana", "Pérez", DocumentType.CC, "111", "ana@unicesar.edu.co", "hash",
                LocalDateTime.now(FIXED_CLOCK), faculty);
        AuthenticatedUser actor = new AuthenticatedUser(70L, "ana@unicesar.edu.co", UserRole.TEACHER, false);
        when(accessScope.loadActor(actor)).thenReturn(teacher);

        assertThatThrownBy(() -> service.search(actor, 0, 10, null, null, null, null))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    private AuditLog captureSaved() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static Specification<AuditLog> anySpec() {
        return any(Specification.class);
    }
}
