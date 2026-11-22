package co.edu.unicesar.sierap.service.impl;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.dto.request.NewFacultyRequest;
import co.edu.unicesar.sierap.dto.request.UpdateFacultyRequest;
import co.edu.unicesar.sierap.dto.response.FacultyResponse;
import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.Faculty;
import co.edu.unicesar.sierap.entity.Program;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.enums.UserRole;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.AdminRepository;
import co.edu.unicesar.sierap.repository.FacultyRepository;
import co.edu.unicesar.sierap.repository.ProjectGroupRepository;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.TeacherRepository;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link FacultyServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class FacultyServiceImplTest {

    @Mock
    private FacultyRepository facultyRepository;

    @Mock
    private ProjectGroupRepository projectGroupRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private AccessScope accessScope;

    private FacultyServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FacultyServiceImpl(
                facultyRepository,
                projectGroupRepository,
                studentRepository,
                adminRepository,
                teacherRepository,
                accessScope);
    }

    @Test
    void listForUserSuperAdminReturnsAll() {
        AuthenticatedUser actor = new AuthenticatedUser(1L, "sa@unicesar.edu.co", UserRole.SUPER_ADMIN, false);
        SuperAdmin sa = mock(SuperAdmin.class);
        when(accessScope.loadActor(actor)).thenReturn(sa);

        Faculty f1 = new Faculty("04", "Ingenierías y Tecnológicas");
        ReflectionTestUtils.setField(f1, "id", 1L);
        f1.addProgram(new Program("108", "Ingeniería de Sistemas"));
        when(facultyRepository.findAllByOrderByNameAsc()).thenReturn(List.of(f1));

        List<FacultyResponse> result = service.listForUser(actor);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("04");
        assertThat(result.getFirst().programCount()).isEqualTo(1);
    }

    @Test
    void listForUserAdminReturnsOnlyOwnFaculty() {
        AuthenticatedUser actor = new AuthenticatedUser(2L, "admin@unicesar.edu.co", UserRole.ADMIN, false);
        Admin admin = mock(Admin.class);
        when(accessScope.loadActor(actor)).thenReturn(admin);
        when(accessScope.facultyOf(admin)).thenReturn(1L);

        Faculty f1 = new Faculty("04", "Ingenierías y Tecnológicas");
        ReflectionTestUtils.setField(f1, "id", 1L);
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(f1));

        List<FacultyResponse> result = service.listForUser(actor);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(1L);
    }

    @Test
    void createSuccessAndDuplicateCodeThrowsConflict() {
        when(facultyRepository.existsByCode("04")).thenReturn(false);
        when(facultyRepository.save(any(Faculty.class))).thenAnswer(inv -> {
            Faculty f = inv.getArgument(0);
            ReflectionTestUtils.setField(f, "id", 10L);
            return f;
        });

        FacultyResponse created = service.create(new NewFacultyRequest(" 04 ", " Ingenierías "));
        assertThat(created.id()).isEqualTo(10L);
        assertThat(created.code()).isEqualTo("04");
        assertThat(created.name()).isEqualTo("Ingenierías");

        when(facultyRepository.existsByCode("04")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new NewFacultyRequest("04", "Otra")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updateSuccessAndDuplicateCodeThrowsConflict() {
        Faculty f = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(f, "id", 1L);
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(f));
        when(facultyRepository.existsByCodeAndIdNot("05", 1L)).thenReturn(false);

        FacultyResponse updated = service.update(1L, new UpdateFacultyRequest("05", "Ingenierías y Tecnológicas"));
        assertThat(updated.code()).isEqualTo("05");
        assertThat(updated.name()).isEqualTo("Ingenierías y Tecnológicas");

        when(facultyRepository.existsByCodeAndIdNot("06", 1L)).thenReturn(true);
        assertThatThrownBy(() -> service.update(1L, new UpdateFacultyRequest("06", "Otra")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteSuccessAndWithDependenciesThrowsConflict() {
        Faculty f = new Faculty("04", "Ingenierías");
        ReflectionTestUtils.setField(f, "id", 1L);
        when(facultyRepository.findById(1L)).thenReturn(Optional.of(f));
        when(projectGroupRepository.existsByMomentProgramFacultyId(1L)).thenReturn(false);
        when(studentRepository.existsByProgramFacultyId(1L)).thenReturn(false);
        when(adminRepository.existsByFacultyId(1L)).thenReturn(false);
        when(teacherRepository.existsByFacultyId(1L)).thenReturn(false);

        service.delete(1L);
        verify(facultyRepository).delete(f);

        when(teacherRepository.existsByFacultyId(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deleteNotFoundThrowsResourceNotFound() {
        when(facultyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
