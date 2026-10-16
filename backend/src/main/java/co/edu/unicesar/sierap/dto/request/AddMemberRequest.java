package co.edu.unicesar.sierap.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Estudiante que el docente agrega a uno de sus grupos.
 *
 * @param studentId estudiante sin grupo en el momento
 */
public record AddMemberRequest(@NotNull(message = "El estudiante es obligatorio") Long studentId) {
}
