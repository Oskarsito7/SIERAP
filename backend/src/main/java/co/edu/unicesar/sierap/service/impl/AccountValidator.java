package co.edu.unicesar.sierap.service.impl;

import java.util.Locale;

import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.repository.StudentRepository;
import co.edu.unicesar.sierap.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Validaciones comunes al crear cualquier cuenta: correo, documento y código
 * estudiantil únicos.
 *
 * <p>La base de datos también los protege (restricciones únicas, ADR-016), pero aquí
 * se valida antes para responder un mensaje claro en español.</p>
 */
@Component
class AccountValidator {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    /**
     * Crea el validador.
     *
     * @param userRepository    repositorio de usuarios
     * @param studentRepository repositorio de estudiantes
     */
    AccountValidator(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    /**
     * Exige que el correo y el documento no estén registrados.
     *
     * @param email          correo de la cuenta nueva
     * @param documentNumber documento de la cuenta nueva
     */
    void requireNewUser(String email, String documentNumber) {
        if (userRepository.existsByEmail(normalizeEmail(email))) {
            throw new BusinessRuleException("Ya existe una cuenta con el correo " + normalizeEmail(email) + ".");
        }
        if (userRepository.existsByDocumentNumber(documentNumber.trim())) {
            throw new BusinessRuleException("Ya existe una cuenta con el documento " + documentNumber.trim() + ".");
        }
    }

    /**
     * Exige que el código estudiantil no esté registrado.
     *
     * @param studentCode código estudiantil
     */
    void requireNewStudentCode(String studentCode) {
        if (studentRepository.existsByStudentCode(studentCode.trim())) {
            throw new BusinessRuleException("Ya existe un estudiante con el código " + studentCode.trim() + ".");
        }
    }

    /**
     * Exige que el correo y el documento no los tenga otra cuenta (al editar una cuenta
     * existente). Cada dato es opcional: solo se valida el que llega.
     *
     * @param userId         cuenta que se edita, excluida de la comparación
     * @param email          correo nuevo, o {@code null} si no cambia
     * @param documentNumber documento nuevo, o {@code null} si no cambia
     */
    void requireAvailableForUser(Long userId, String email, String documentNumber) {
        if (email != null && userRepository.existsByEmailAndIdNot(normalizeEmail(email), userId)) {
            throw new BusinessRuleException("Ya existe una cuenta con el correo " + normalizeEmail(email) + ".");
        }
        if (documentNumber != null && userRepository.existsByDocumentNumberAndIdNot(documentNumber.trim(), userId)) {
            throw new BusinessRuleException("Ya existe una cuenta con el documento " + documentNumber.trim() + ".");
        }
    }

    /**
     * Exige que el código estudiantil no lo tenga otro estudiante (al editar uno existente).
     *
     * @param studentId   estudiante que se edita, excluido de la comparación
     * @param studentCode código nuevo
     */
    void requireAvailableStudentCode(Long studentId, String studentCode) {
        if (studentRepository.existsByStudentCodeAndIdNot(studentCode.trim(), studentId)) {
            throw new BusinessRuleException("Ya existe un estudiante con el código " + studentCode.trim() + ".");
        }
    }

    /**
     * Normaliza un correo igual que {@code User.setEmail}.
     *
     * @param email correo tal como llegó
     * @return correo sin espacios y en minúsculas
     */
    static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
