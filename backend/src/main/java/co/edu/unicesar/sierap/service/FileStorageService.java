package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.entity.Submission.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Archivos de las entregas en el disco del servidor (ADR-015).
 *
 * <p>Los archivos viven bajo una carpeta base que se configura por perfil
 * ({@code sierap.storage.base-path}) y la base de datos guarda solo la ruta relativa a
 * ella. El nombre en disco lo genera el backend; el nombre original solo se conserva
 * para mostrarlo.</p>
 */
public interface FileStorageService {

    /**
     * Guarda un archivo ya validado con un nombre generado por el backend.
     *
     * @param file      archivo subido
     * @param directory carpeta relativa donde se guarda, p. ej. {@code groups/7/v3}
     * @param extension extensión con punto, p. ej. {@code .zip}
     * @return ruta relativa, nombre original y tamaño del archivo guardado
     */
    StoredFile store(MultipartFile file, String directory, String extension);

    /**
     * Abre un archivo guardado para descargarlo.
     *
     * @param path ruta relativa guardada en la base de datos
     * @return el archivo
     */
    Resource load(String path);

    /**
     * Borra un archivo guardado. Se usa cuando la transacción que lo registraba se
     * revierte, para no dejar archivos huérfanos. Si no existe, no hace nada.
     *
     * @param path ruta relativa
     */
    void delete(String path);
}
