package co.edu.unicesar.sierap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la API REST de SIERAP (Sistema Integral Evaluador de
 * Resultados de Aprendizajes por Programa).
 *
 * <p>La aplicación sigue una arquitectura por capas:
 * {@code controller → service → repository → entity}, con DTOs en el borde de la
 * API y componentes transversales en {@code security}, {@code exception} y
 * {@code config}. Ver {@code docs/diagramas/diagrama_capas.md}.</p>
 */
@SpringBootApplication
public class SierapApplication {

    /**
     * Arranca el contexto de Spring Boot.
     *
     * @param args argumentos de línea de comandos (p. ej. {@code --spring.profiles.active=prod})
     */
    public static void main(String[] args) {
        SpringApplication.run(SierapApplication.class, args);
    }
}
