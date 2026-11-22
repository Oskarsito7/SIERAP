package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.response.FileDownload;
import co.edu.unicesar.sierap.security.AuthenticatedUser;

/**
 * Exportación de los reportes a archivos (Momento 2, ADR-020): la sábana de notas de un
 * momento en Excel y el informe del momento, el acta del grupo y el boletín individual
 * en PDF. Cada archivo sale del mismo reporte que se ve en pantalla, con las mismas
 * reglas de alcance de {@link ReportService}.
 */
public interface ReportExportService {

    /**
     * Sábana de notas del momento ({@code .xlsx}). El detalle por estudiante y por grupo
     * incluye solo los grupos que el usuario puede ver: todos para el super_admin y el
     * admin de la facultad, y los suyos para un docente.
     *
     * @param actor    super_admin, admin o docente de la facultad del momento
     * @param momentId momento
     * @return el archivo
     */
    FileDownload momentWorkbook(AuthenticatedUser actor, Long momentId);

    /**
     * Informe del momento en PDF, con el mismo alcance que la sábana.
     *
     * @param actor    super_admin, admin o docente de la facultad del momento
     * @param momentId momento
     * @return el archivo
     */
    FileDownload momentPdf(AuthenticatedUser actor, Long momentId);

    /**
     * Acta de evaluación de un grupo en PDF.
     *
     * @param actor   docente del grupo, admin de la facultad o super_admin
     * @param groupId grupo
     * @return el archivo
     */
    FileDownload groupPdf(AuthenticatedUser actor, Long groupId);

    /**
     * Boletín individual de un estudiante en PDF. El estudiante solo descarga el suyo y
     * solo cuando cerró la calificación del momento.
     *
     * @param actor     estudiante, o super_admin, admin o docente con acceso al estudiante
     * @param momentId  momento
     * @param studentId estudiante (opcional si el actor es el estudiante)
     * @return el archivo
     */
    FileDownload individualPdf(AuthenticatedUser actor, Long momentId, Long studentId);
}
