package co.edu.unicesar.sierap.service.impl;

import co.edu.unicesar.sierap.entity.GlobalParameter;
import co.edu.unicesar.sierap.entity.GradeScale;
import co.edu.unicesar.sierap.exception.BusinessRuleException;
import co.edu.unicesar.sierap.repository.GlobalParameterRepository;
import org.springframework.stereotype.Component;

/**
 * Carga la escala de notas de los reportes desde los parámetros globales
 * {@value #PARAM_PASSING_PERCENTAGE} y {@value #PARAM_GRADE_BANDS} (V10). La comparten el
 * servicio de reportes y el de exportación, para que la pantalla y los archivos
 * descargados usen siempre el mismo umbral de aprobación y los mismos niveles.
 */
@Component
public class GradeScaleLoader {

    /** Porcentaje del puntaje máximo del momento necesario para aprobar. */
    public static final String PARAM_PASSING_PERCENTAGE = "report.passing-percentage";

    /** Niveles de desempeño de la distribución de notas. */
    public static final String PARAM_GRADE_BANDS = "report.grade-bands";

    private final GlobalParameterRepository globalParameterRepository;

    /**
     * Crea el cargador.
     *
     * @param globalParameterRepository parámetros globales
     */
    public GradeScaleLoader(GlobalParameterRepository globalParameterRepository) {
        this.globalParameterRepository = globalParameterRepository;
    }

    /**
     * Escala de notas vigente.
     *
     * @return umbral de aprobación y niveles de desempeño
     * @throws BusinessRuleException si falta un parámetro o su formato no es válido
     */
    public GradeScale load() {
        GlobalParameter passing = globalParameterRepository.findByKey(PARAM_PASSING_PERCENTAGE)
                .orElseThrow(() -> new BusinessRuleException(
                        "Falta configurar el parámetro global '" + PARAM_PASSING_PERCENTAGE + "'"));
        GlobalParameter bands = globalParameterRepository.findByKey(PARAM_GRADE_BANDS)
                .orElseThrow(() -> new BusinessRuleException(
                        "Falta configurar el parámetro global '" + PARAM_GRADE_BANDS + "'"));
        return GradeScale.of(passing.getValue(), bands.getValue());
    }
}
