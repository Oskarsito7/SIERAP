package co.edu.unicesar.sierap.dto.response;

import org.springframework.core.io.Resource;

/**
 * Archivo listo para descargar. No se serializa a JSON: el controller lo convierte en
 * la respuesta con el archivo y su {@code Content-Disposition}.
 *
 * @param resource    contenido del archivo
 * @param fileName    nombre con el que se descarga (el original que subió el estudiante)
 * @param contentType tipo de contenido, p. ej. {@code application/zip}
 * @param size        tamaño en bytes
 */
public record FileDownload(Resource resource, String fileName, String contentType, long size) {
}
