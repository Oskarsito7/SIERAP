package co.edu.unicesar.sierap.dto.response;

/**
 * Facultad académica con el conteo de sus programas ({@code Faculty} en {@code docs/07}).
 *
 * @param id           identificador de la facultad
 * @param code         código institucional
 * @param name         nombre oficial de la facultad
 * @param programCount cantidad de programas adscritos a la facultad
 */
public record FacultyResponse(
        Long id,
        String code,
        String name,
        int programCount) {
}
