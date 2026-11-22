package co.edu.unicesar.sierap.dto.response;

/**
 * Programa académico con su facultad y conteos ({@code Program} en {@code docs/07}).
 *
 * @param id                       identificador del programa
 * @param code                     código institucional
 * @param name                     nombre del programa
 * @param facultyId                identificador de la facultad
 * @param facultyName              nombre de la facultad
 * @param currentPeriodMomentCount cantidad de momentos configurados en el periodo actual
 * @param studentCount             cantidad de estudiantes pertenecientes al programa
 */
public record ProgramResponse(
        Long id,
        String code,
        String name,
        Long facultyId,
        String facultyName,
        int currentPeriodMomentCount,
        int studentCount) {
}
