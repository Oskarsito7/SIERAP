package co.edu.unicesar.sierap.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import co.edu.unicesar.sierap.entity.Submission.StoredFile;
import co.edu.unicesar.sierap.exception.ResourceNotFoundException;
import co.edu.unicesar.sierap.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementación de {@link FileStorageService} sobre el disco del servidor (ADR-015).
 *
 * <p>Cada archivo se guarda con un nombre aleatorio ({@link UUID}) y la extensión ya
 * validada, nunca con el nombre que envió el usuario. Toda ruta se resuelve dentro de
 * la carpeta base: una ruta que intente salir de ella (p. ej. con {@code ..}) se
 * rechaza.</p>
 */
@Service
public class DiskFileStorageService implements FileStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskFileStorageService.class);

    /** Largo máximo del nombre original que se guarda (columna de 255 caracteres). */
    private static final int MAX_NAME_LENGTH = 255;

    private final Path basePath;

    /**
     * Crea el servicio con la carpeta base configurada.
     *
     * @param basePath carpeta base de almacenamiento ({@code sierap.storage.base-path})
     */
    public DiskFileStorageService(@Value("${sierap.storage.base-path}") String basePath) {
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("sierap.storage.base-path must be configured (SIERAP_STORAGE_PATH)");
        }
        this.basePath = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(MultipartFile file, String directory, String extension) {
        String relativePath = directory + "/" + UUID.randomUUID() + extension;
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not store " + relativePath, exception);
        }
        return new StoredFile(relativePath, originalName(file, extension), file.getSize());
    }

    @Override
    public Resource load(String path) {
        Path file = resolve(path);
        if (!Files.isRegularFile(file)) {
            throw new ResourceNotFoundException("El archivo de la entrega ya no está disponible.");
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException exception) {
            LOGGER.warn("Could not delete orphan file {}", path, exception);
        }
    }

    /** Ruta absoluta dentro de la carpeta base; rechaza cualquier ruta que salga de ella. */
    private Path resolve(String relativePath) {
        Path resolved = basePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new IllegalArgumentException("Path outside the storage folder: " + relativePath);
        }
        return resolved;
    }

    /**
     * Nombre original sin carpetas (algunos navegadores envían la ruta completa) y
     * recortado al largo de la columna; si no llega, uno genérico con la extensión.
     */
    private static String originalName(MultipartFile file, String extension) {
        String name = file.getOriginalFilename();
        if (name != null) {
            name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1).strip();
        }
        if (name == null || name.isEmpty()) {
            return "archivo" + extension;
        }
        return name.length() <= MAX_NAME_LENGTH ? name : name.substring(name.length() - MAX_NAME_LENGTH);
    }
}
