package co.edu.unicesar.sierap.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import co.edu.unicesar.sierap.exception.FileTooLargeException;
import co.edu.unicesar.sierap.exception.InvalidFileException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Valida los archivos de una entrega antes de guardarlos (ADR-015): extensión, tipo de
 * contenido declarado, firma de los primeros bytes y tamaño.
 *
 * <ul>
 *   <li>Proyecto: {@code .zip}, hasta 100 MB.</li>
 *   <li>Presentación: {@code .pdf}, {@code .docx} o {@code .pptx}, hasta 30 MB.</li>
 * </ul>
 *
 * <p>La firma evita que un archivo cualquiera pase solo por tener la extensión
 * correcta: un {@code .zip}, un {@code .docx} y un {@code .pptx} empiezan con
 * {@code PK} y un {@code .pdf} con {@code %PDF}. Los navegadores no siempre declaran el
 * mismo tipo de contenido, así que además del oficial se acepta
 * {@code application/octet-stream}.</p>
 */
final class SubmissionFileValidator {

    /** Tamaño máximo del {@code .zip} del proyecto: 100 MB. */
    static final long MAX_ZIP_SIZE = 100L * 1024 * 1024;

    /** Tamaño máximo de la presentación: 30 MB. */
    static final long MAX_PRESENTATION_SIZE = 30L * 1024 * 1024;

    private static final String OCTET_STREAM = "application/octet-stream";
    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 3, 4};
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F'};

    private static final Set<String> ZIP_TYPES = Set.of("application/zip", "application/x-zip-compressed",
            "application/x-zip", "multipart/x-zip", OCTET_STREAM);

    /** Tipos de contenido aceptados para cada extensión de la presentación. */
    private static final Map<String, Set<String>> PRESENTATION_TYPES = Map.of(
            ".pdf", Set.of("application/pdf", "application/x-pdf", OCTET_STREAM),
            ".docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    OCTET_STREAM),
            ".pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    OCTET_STREAM));

    private SubmissionFileValidator() {
    }

    /**
     * Valida el {@code .zip} del proyecto.
     *
     * @param zip archivo subido; obligatorio
     * @return la extensión con la que se guarda
     */
    static String validateZip(MultipartFile zip) {
        if (zip == null || zip.isEmpty()) {
            throw new InvalidFileException("Adjunte el archivo .zip con el proyecto.");
        }
        if (!".zip".equals(extensionOf(zip)) || !acceptsType(zip, ZIP_TYPES) || !startsWith(zip, ZIP_SIGNATURE)) {
            throw new InvalidFileException("El proyecto debe ser un archivo .zip válido.");
        }
        if (zip.getSize() > MAX_ZIP_SIZE) {
            throw new FileTooLargeException("El archivo .zip supera el máximo de 100 MB.");
        }
        return ".zip";
    }

    /**
     * Valida la presentación.
     *
     * @param presentation archivo subido; ya se sabe que no es {@code null} ni vacío
     * @return la extensión con la que se guarda
     */
    static String validatePresentation(MultipartFile presentation) {
        String extension = extensionOf(presentation);
        Set<String> types = PRESENTATION_TYPES.get(extension);
        byte[] signature = ".pdf".equals(extension) ? PDF_SIGNATURE : ZIP_SIGNATURE;
        if (types == null || !acceptsType(presentation, types) || !startsWith(presentation, signature)) {
            throw new InvalidFileException("La presentación debe ser un archivo .pdf, .docx o .pptx válido.");
        }
        if (presentation.getSize() > MAX_PRESENTATION_SIZE) {
            throw new FileTooLargeException("La presentación supera el máximo de 30 MB.");
        }
        return extension;
    }

    /** Extensión del nombre original en minúsculas, con punto; vacía si no tiene. */
    private static String extensionOf(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private static boolean acceptsType(MultipartFile file, Set<String> types) {
        String type = file.getContentType();
        return type != null && types.contains(type.toLowerCase(Locale.ROOT));
    }

    private static boolean startsWith(MultipartFile file, byte[] signature) {
        try (InputStream input = file.getInputStream()) {
            return Arrays.equals(input.readNBytes(signature.length), signature);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read the uploaded file", exception);
        }
    }
}
