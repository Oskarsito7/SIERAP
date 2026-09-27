package co.edu.unicesar.sierap.service.impl;

import java.util.Objects;

import co.edu.unicesar.sierap.entity.Admin;
import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import co.edu.unicesar.sierap.entity.Student;
import co.edu.unicesar.sierap.entity.SuperAdmin;
import co.edu.unicesar.sierap.entity.Teacher;
import co.edu.unicesar.sierap.entity.User;
import co.edu.unicesar.sierap.exception.ForbiddenAccessException;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.repository.UserRepository;
import co.edu.unicesar.sierap.security.AuthenticatedUser;
import org.springframework.stereotype.Component;

/**
 * Reglas de alcance de las cuentas (ADR-017): qué facultad gestiona cada usuario y
 * sobre qué cuentas puede actuar.
 *
 * <ul>
 *   <li>El super_admin gestiona todo.</li>
 *   <li>Un admin gestiona su facultad: sus docentes, los estudiantes de sus programas
 *       y los evaluadores externos que crearon él o los docentes de su facultad.</li>
 *   <li>Un docente gestiona a sus estudiantes (los que creó o se registraron con su
 *       código) y a los evaluadores externos que creó.</li>
 * </ul>
 *
 * <p>Las comparaciones se hacen por id, porque las relaciones LAZY pueden ser
 * proxies de Hibernate.</p>
 */
@Component
class AccessScope {

    private final UserRepository userRepository;

    /**
     * Crea el componente.
     *
     * @param userRepository repositorio de usuarios
     */
    AccessScope(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Carga la entidad del usuario autenticado.
     *
     * @param actor usuario del token
     * @return entidad del usuario, con su subclase concreta
     */
    User loadActor(AuthenticatedUser actor) {
        return userRepository.findById(actor.id())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", actor.id()));
    }

    /**
     * Exige que el usuario pueda gestionar la facultad indicada.
     *
     * @param actor     usuario que actúa
     * @param facultyId facultad destino
     * @throws ForbiddenAccessException si la facultad no es la suya
     */
    void requireFaculty(User actor, Long facultyId) {
        if (actor instanceof SuperAdmin) {
            return;
        }
        Long ownFaculty = facultyOf(actor);
        if (ownFaculty == null || !ownFaculty.equals(facultyId)) {
            throw new ForbiddenAccessException("Solo puede gestionar información de su facultad.");
        }
    }

    /**
     * Facultad del usuario (admins y docentes).
     *
     * @param user usuario
     * @return id de su facultad, o {@code null} si su rol no pertenece a una facultad
     */
    Long facultyOf(User user) {
        if (user instanceof Admin admin) {
            return admin.getFaculty().getId();
        }
        if (user instanceof Teacher teacher) {
            return teacher.getFaculty().getId();
        }
        return null;
    }

    /**
     * Indica si el usuario puede actuar sobre otra cuenta (reenviar su enlace o
     * cambiar su estado).
     *
     * @param actor  usuario que actúa
     * @param target cuenta destino
     * @return {@code true} si la cuenta está dentro de su alcance
     */
    boolean canManage(User actor, User target) {
        if (actor instanceof SuperAdmin) {
            return true;
        }
        if (actor instanceof Admin admin) {
            Long faculty = admin.getFaculty().getId();
            if (target instanceof Teacher teacher) {
                return faculty.equals(teacher.getFaculty().getId());
            }
            if (target instanceof Student student) {
                return faculty.equals(student.getProgram().getFaculty().getId());
            }
            if (target instanceof ExternalEvaluator evaluator) {
                User creator = evaluator.getCreatedBy();
                return creator != null && (sameUser(creator, actor) || faculty.equals(facultyOf(creator)));
            }
            return false;
        }
        if (actor instanceof Teacher) {
            boolean ownStudentOrEvaluator = target instanceof Student || target instanceof ExternalEvaluator;
            return ownStudentOrEvaluator && target.getCreatedBy() != null && sameUser(target.getCreatedBy(), actor);
        }
        return false;
    }

    private static boolean sameUser(User first, User second) {
        return Objects.equals(first.getId(), second.getId());
    }
}
