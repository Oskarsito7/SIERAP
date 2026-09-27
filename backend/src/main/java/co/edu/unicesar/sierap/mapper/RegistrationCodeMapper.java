package co.edu.unicesar.sierap.mapper;

import co.edu.unicesar.sierap.dto.response.RegistrationCodeResponse;
import co.edu.unicesar.sierap.entity.RegistrationCode;

/**
 * Convierte códigos de registro en su DTO de salida.
 */
public final class RegistrationCodeMapper {

    private RegistrationCodeMapper() {
    }

    /**
     * Convierte un código de registro en su DTO.
     *
     * @param code            código de registro
     * @param registeredCount estudiantes que se registraron con él
     * @return DTO del código
     */
    public static RegistrationCodeResponse toResponse(RegistrationCode code, long registeredCount) {
        return new RegistrationCodeResponse(code.getId(), code.getCode(), code.getProgram().getId(),
                code.getProgram().getName(), code.getTeacher().getId(), code.getTeacher().getFullName(),
                code.getExpiresOn(), code.isActive(), registeredCount);
    }
}
