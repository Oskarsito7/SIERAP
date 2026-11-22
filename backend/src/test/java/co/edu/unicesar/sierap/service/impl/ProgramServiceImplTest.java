package co.edu.unicesar.sierap.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.NewProgramRequest;
import co.edu.unicesar.sierap.dto.request.UpdateProgramRequest;
import co.edu.unicesar.sierap.dto.response.ProgramResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.ExternalEvaluatorRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProgramRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ProgramServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class ProgramServiceImplTest {

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ProjectGroupRepository projectGroupRepository;

    @Mock
    private ExternalEvaluatorRepository externalEvaluatorRepository;

    @Mock
    private AccessScope accessScope;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-16T12:00:00Z"), ZoneOffset.UTC);

    private ProgramServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProgramServiceImpl(
                programRepository,
                facultyRepository,
                academicPeriodRepository,
                momentRepository,
                studentRepository,
                projectGroupRepository,
                externalEvaluatorRepository,
                accessScope,
                clock);
    }

    @Test
    void listForUserSuperAdminAndCounts() {
        AuthenticatedUser actor = new AuthenticatedUser(1L, "sa@unicesar.edu.co", UserRole.SUPER_ADMIN, false);
        when(accessScope.loadActor(actor)).thenReturn(mock(SuperAdmin.class));

        Faculty f = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(f, "id", 1L);
        Program p = new Program("108", "Ingeniería de Sistemas");
        ReflectionTestUtils.setField(p, "id", 10L);
        f.addProgram(p);

        when(programRepository.findAllByOrderByNameAsc()).thenReturn(List.of(p));

        AcademicPeriod current = new AcademicPeriod("2026-2",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15), LocalDate.of(2026, 12, 10));
        ReflectionTestUtils.setField(current, "id", 5L);
        when(academicPeriodRepository
                .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        LocalDate.of(2026, 10, 16), LocalDate.of(2026, 10, 16)))
                .thenReturn(Optional.of(current));
        when(momentRepository.countByProgramIdAndPeriodId(10L, 5L)).thenReturn(2);
        when(studentRepository.countByProgramId(10L)).thenReturn(45);

        List<ProgramResponse> result = service.listForUser(actor, null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().currentPeriodMomentCount()).isEqualTo(2);
        assertThat(result.getFirst().studentCount()).isEqualTo(45);
    }

    @Test
    void listForUserAdminOtherFacultyThrowsForbidden() {
        AuthenticatedUser actor = new AuthenticatedUser(2L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
        Admin admin = mock(Admin.class);
        when(accessScope.loadActor(actor)).thenReturn(admin);
        when(accessScope.facultyOf(admin)).thenReturn(1L);
        doThrow(new ForbiddenAccessException("Solo puede gestionar información de su facultad."))
                .when(accessScope).requireFaculty(admin, 2L);

        assertThatThrownBy(() -> service.listForUser(actor, 2L))
                .isInstanceOf(ForbiddenAccessException.class);
    }

    @Test
    void createUpdateAndDeleteProgram() {
        Faculty f = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(f, "id", 1L);
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(f));
        when(programRepository.existsByCode("109")).thenReturn(false);
        when(programRepository.save(any(Program.class))).thenAnswer(inv -> {
            Program p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 20L);
            return p;
        });

        ProgramResponse created = service.create(new NewProgramRequest(1L, "109", "Ingeniería Ambiental"));
        assertThat(created.id()).isEqualTo(20L);
        assertThat(created.code()).isEqualTo("109");

        Program existing = f.getPrograms().getFirst();
        when(programRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(programRepository.existsByCodeAndIdNot("110", 20L)).thenReturn(false);

        ProgramResponse updated = service.update(20L, new UpdateProgramRequest("110", "Ingeniería Ambiental y Sanitaria"));
        assertThat(updated.code()).isEqualTo("110");

        when(projectGroupRepository.existsByMomentProgramId(20L)).thenReturn(false);
        when(studentRepository.existsByProgramId(20L)).thenReturn(false);
        when(externalEvaluatorRepository.existsByGraduationProgramId(20L)).thenReturn(false);

        service.delete(20L);
        verify(programRepository).delete(existing);

        when(studentRepository.existsByProgramId(20L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(20L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
