package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.response.MomentDetailResponse;
import co.edu.unicesar.sierap.dto.response.MomentSummaryResponse;
import co.edu.unicesar.sierap.entity.AcademicPeriod;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Moment;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.enums.DocumentType;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.repository.AcademicPeriodRepository;
import co.edu.unicesar.sierap.repository.MomentRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pruebas unitarias de {@link MomentServiceImpl}, verificando el alcance de consulta
 * para estudiantes dentro y fuera del rango de semestres de un momento.
 */
@ExtendWith(MockitoExtension.class)
class MomentServiceImplTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T14:30:00Z"), ZoneId.of("America/Bogota"));

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private AcademicPeriodRepository academicPeriodRepository;

    @Mock
    private ProjectGroupRepository projectGroupRepository;

    @Mock
    private AccessScope accessScope;

    private MomentServiceImpl momentService;

    private AcademicPeriod period;
    private Faculty faculty;
    private Program program;
    private Student student;
    private AuthenticatedUser studentActor;

    @BeforeEach
    void setUp() {
        momentService = new MomentServiceImpl(momentRepository, academicPeriodRepository,
                projectGroupRepository, accessScope, FIXED_CLOCK);

        period = new AcademicPeriod("2026-2", LocalDate.of(2026, 8, 3), LocalDate.of(2026, 12, 5),
                LocalDate.of(2026, 11, 28));
        ReflectionTestUtils.setField(period, "id", 1L);

        faculty = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(faculty, "id", 10L);

        program = new Program("108", "Sistemas");
        ReflectionTestUtils.setField(program, "id", 100L);
        faculty.addProgram(program);

        student = new Student("Juan", "Pérez", DocumentType.CC, "12345", "juan@unicesar.edu.co",
                "hash", LocalDateTime.now(FIXED_CLOCK), "20261001", 2, program);
        ReflectionTestUtils.setField(student, "id", 50L);

        studentActor = new AuthenticatedUser(50L, "juan@unicesar.edu.co", UserRole.STUDENT, false);
    }

    @Test
    void listPassesStudentIdToFindForStudent() {
        when(academicPeriodRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                any(LocalDate.class), any(LocalDate.class))).thenReturn(Optional.of(period));
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findForStudent(1L, 100L, 2, 50L)).thenReturn(List.of(moment));

        List<MomentSummaryResponse> result = momentService.list(studentActor, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(200L);
        verify(momentRepository).findForStudent(1L, 100L, 2, 50L);
    }

    @Test
    void studentCanViewMomentInTheirSemesterRange() {
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        Moment moment = new Moment(period, 1, "Momento 1", "Asignatura", 1, 3, "Bajo", 3, 5);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 200L);

        when(momentRepository.findById(200L)).thenReturn(Optional.of(moment));

        MomentDetailResponse response = momentService.get(studentActor, 200L);

        assertThat(response.summary().id()).isEqualTo(200L);
    }

    @Test
    void studentCanViewMomentOutsideSemesterRangeIfTheyHaveAGroup() {
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        // Momento para semestres 4 a 6; el estudiante está en 2do semestre
        Moment moment = new Moment(period, 2, "Momento 2", "Asignatura 2", 4, 6, "Medio", 2, 4);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 201L);

        when(momentRepository.findById(201L)).thenReturn(Optional.of(moment));
        when(projectGroupRepository.existsByMomentIdAndMembersId(201L, 50L)).thenReturn(true);

        MomentDetailResponse response = momentService.get(studentActor, 201L);

        assertThat(response.summary().id()).isEqualTo(201L);
        verify(projectGroupRepository).existsByMomentIdAndMembersId(201L, 50L);
    }

    @Test
    void studentCannotViewMomentOutsideSemesterRangeWithoutGroup() {
        when(accessScope.loadActor(studentActor)).thenReturn(student);

        Moment moment = new Moment(period, 2, "Momento 2", "Asignatura 2", 4, 6, "Medio", 2, 4);
        program.addMoment(moment);
        ReflectionTestUtils.setField(moment, "id", 201L);

        when(momentRepository.findById(201L)).thenReturn(Optional.of(moment));
        when(projectGroupRepository.existsByMomentIdAndMembersId(201L, 50L)).thenReturn(false);

        assertThatThrownBy(() -> momentService.get(studentActor, 201L))
                .isInstanceOf(ForbiddenAccessException.class)
                .hasMessage("No tiene acceso a este momento.");
    }
}
