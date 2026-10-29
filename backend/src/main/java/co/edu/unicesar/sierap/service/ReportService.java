package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.response.report.ComparativeReportData;
import co.edu.unicesar.sierap.dto.response.report.GroupReportData;
import co.edu.unicesar.sierap.dto.response.report.IndividualReportData;
import co.edu.unicesar.sierap.dto.response.report.MomentReportData;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Servicio de generación de reportes académicos (Etapa 7, RF 12).
 */
public interface ReportService {

    /**
     * Reporte individual de un estudiante en un momento.
     *
     * @param actor     usuario autenticado
     * @param momentId  momento evaluativo
     * @param studentId estudiante consultado (opcional si el actor es estudiante)
     * @return datos estructurados del reporte individual
     */
    IndividualReportData getIndividualReport(AuthenticatedUser actor, Long momentId, Long studentId);

    /**
     * Reporte por grupo de proyecto de aula.
     *
     * @param actor   usuario autenticado
     * @param groupId identificador del grupo
     * @return datos estructurados del reporte grupal
     */
    GroupReportData getGroupReport(AuthenticatedUser actor, Long groupId);

    /**
     * Reporte consolidado de un momento y programa académico.
     *
     * @param actor    usuario autenticado
     * @param momentId identificador del momento
     * @return datos estructurados del reporte consolidado
     */
    MomentReportData getMomentReport(AuthenticatedUser actor, Long momentId);

    /**
     * Reporte comparativo entre dos periodos para un mismo programa y número de momento.
     *
     * @param actor        usuario autenticado
     * @param programId    programa académico
     * @param momentNumber número de momento (p. ej. 1)
     * @param periodAId    periodo base A
     * @param periodBId    periodo objetivo B
     * @return datos estructurados del reporte comparativo
     */
    ComparativeReportData getComparativeReport(
            AuthenticatedUser actor,
            Long programId,
            int momentNumber,
            Long periodAId,
            Long periodBId);
}
