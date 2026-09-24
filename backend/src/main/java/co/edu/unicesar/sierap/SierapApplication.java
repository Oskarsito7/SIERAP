package co.edu.unicesar.sierap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the SIERAP REST API (Sistema Integral Evaluador de Resultados de
 * Aprendizajes por Programa).
 *
 * <p>The application follows a layered architecture:
 * {@code controller → service → repository → entity}, with DTOs at the API
 * boundary and cross-cutting components in {@code security}, {@code exception} and
 * {@code config}. See {@code docs/diagramas/diagrama_capas.md}.</p>
 */
@SpringBootApplication
public class SierapApplication {

    /**
     * Starts the Spring Boot context.
     *
     * @param args command-line arguments (e.g. {@code --spring.profiles.active=prod})
     */
    public static void main(String[] args) {
        SpringApplication.run(SierapApplication.class, args);
    }
}
