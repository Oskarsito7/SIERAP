package co.edu.unicesar.sierap.dto.request;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Modificación parcial de un momento de evaluación en estado de configuración.
 *
 * <p>Solo se aplican los campos enviados. Las ventanas de fechas se actualizan
 * enviando sus dos fechas, o ambas en {@code null} para limpiarlas (ADR-012).</p>
 *
 * @param periodId                   nuevo periodo académico, o {@code null} si se omite
 * @param number                     nuevo número del momento, o {@code null} si se omite
 * @param name                       nuevo nombre, o {@code null} si se omite
 * @param subject                    nueva asignatura, o {@code null} si se omite
 * @param semesterFrom               nuevo semestre inicial, o {@code null} si se omite
 * @param semesterTo                 nuevo semestre final, o {@code null} si se omite
 * @param requirementLevel           nuevo nivel de exigencia, o {@code null} si se omite
 * @param minMembers                 nuevo mínimo de integrantes, o {@code null} si se omite
 * @param maxMembers                 nuevo máximo de integrantes, o {@code null} si se omite
 * @param groupFormationStart        inicio de conformación de grupos
 * @param groupFormationEnd          fin de conformación de grupos
 * @param submissionStart            inicio de entregas
 * @param submissionEnd              fin de entregas
 * @param gradingStart               inicio de calificación
 * @param gradingEnd                 fin de calificación
 * @param updateGroupFormationWindow {@code true} si la ventana de conformación vino en la petición
 * @param updateSubmissionWindow     {@code true} si la ventana de entregas vino en la petición
 * @param updateGradingWindow        {@code true} si la ventana de calificación vino en la petición
 */
@JsonDeserialize(using = UpdateMomentRequest.Deserializer.class)
public record UpdateMomentRequest(
        Long periodId,

        @Min(value = 1, message = "El número del momento debe ser al menos 1")
        Integer number,

        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String name,

        @Size(max = 150, message = "La asignatura admite hasta 150 caracteres")
        String subject,

        @Min(value = 1, message = "El semestre inicial debe ser al menos 1")
        Integer semesterFrom,

        @Min(value = 1, message = "El semestre final debe ser al menos 1")
        Integer semesterTo,

        @Size(max = 100, message = "El nivel de exigencia admite hasta 100 caracteres")
        String requirementLevel,

        @Min(value = 1, message = "El mínimo de integrantes debe ser al menos 1")
        Integer minMembers,

        @Min(value = 1, message = "El máximo de integrantes debe ser al menos 1")
        Integer maxMembers,

        LocalDate groupFormationStart,
        LocalDate groupFormationEnd,
        LocalDate submissionStart,
        LocalDate submissionEnd,
        LocalDate gradingStart,
        LocalDate gradingEnd,
        boolean updateGroupFormationWindow,
        boolean updateSubmissionWindow,
        boolean updateGradingWindow) {

    /**
     * Constructor de conveniencia cuando no se requiere controlar explícitamente las
     * banderas de actualización de ventanas.
     */
    public UpdateMomentRequest(
            Long periodId,
            Integer number,
            String name,
            String subject,
            Integer semesterFrom,
            Integer semesterTo,
            String requirementLevel,
            Integer minMembers,
            Integer maxMembers,
            LocalDate groupFormationStart,
            LocalDate groupFormationEnd,
            LocalDate submissionStart,
            LocalDate submissionEnd,
            LocalDate gradingStart,
            LocalDate gradingEnd) {
        this(periodId, number, name, subject, semesterFrom, semesterTo,
                requirementLevel, minMembers, maxMembers,
                groupFormationStart, groupFormationEnd,
                submissionStart, submissionEnd,
                gradingStart, gradingEnd,
                groupFormationStart != null || groupFormationEnd != null,
                submissionStart != null || submissionEnd != null,
                gradingStart != null || gradingEnd != null);
    }

    /**
     * Deserializador personalizado que detecta qué ventanas de fechas fueron enviadas
     * explícitamente en el JSON (incluso con valor {@code null} para limpiarlas).
     */
    public static class Deserializer extends JsonDeserializer<UpdateMomentRequest> {

        @Override
        public UpdateMomentRequest deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);

            Long periodId = node.has("periodId") && !node.get("periodId").isNull() ? node.get("periodId").asLong() : null;
            Integer number = node.has("number") && !node.get("number").isNull() ? node.get("number").asInt() : null;
            String name = node.has("name") && !node.get("name").isNull() ? node.get("name").asText() : null;
            String subject = node.has("subject") && !node.get("subject").isNull() ? node.get("subject").asText() : null;
            Integer semesterFrom = node.has("semesterFrom") && !node.get("semesterFrom").isNull() ? node.get("semesterFrom").asInt() : null;
            Integer semesterTo = node.has("semesterTo") && !node.get("semesterTo").isNull() ? node.get("semesterTo").asInt() : null;
            String requirementLevel = node.has("requirementLevel") && !node.get("requirementLevel").isNull() ? node.get("requirementLevel").asText() : null;
            Integer minMembers = node.has("minMembers") && !node.get("minMembers").isNull() ? node.get("minMembers").asInt() : null;
            Integer maxMembers = node.has("maxMembers") && !node.get("maxMembers").isNull() ? node.get("maxMembers").asInt() : null;

            boolean updateGroupFormationWindow = node.has("groupFormationStart") || node.has("groupFormationEnd");
            LocalDate groupFormationStart = parseDate(node, "groupFormationStart");
            LocalDate groupFormationEnd = parseDate(node, "groupFormationEnd");

            boolean updateSubmissionWindow = node.has("submissionStart") || node.has("submissionEnd");
            LocalDate submissionStart = parseDate(node, "submissionStart");
            LocalDate submissionEnd = parseDate(node, "submissionEnd");

            boolean updateGradingWindow = node.has("gradingStart") || node.has("gradingEnd");
            LocalDate gradingStart = parseDate(node, "gradingStart");
            LocalDate gradingEnd = parseDate(node, "gradingEnd");

            return new UpdateMomentRequest(
                    periodId, number, name, subject, semesterFrom, semesterTo,
                    requirementLevel, minMembers, maxMembers,
                    groupFormationStart, groupFormationEnd,
                    submissionStart, submissionEnd,
                    gradingStart, gradingEnd,
                    updateGroupFormationWindow, updateSubmissionWindow, updateGradingWindow);
        }

        private static LocalDate parseDate(JsonNode node, String field) {
            if (!node.has(field) || node.get(field).isNull()) {
                return null;
            }
            return LocalDate.parse(node.get(field).asText());
        }
    }
}
