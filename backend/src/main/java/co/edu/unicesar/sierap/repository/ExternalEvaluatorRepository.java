package co.edu.unicesar.sierap.repository;

import java.util.List;
import java.util.Optional;

import co.edu.unicesar.sierap.entity.ExternalEvaluator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Acceso a datos de {@link ExternalEvaluator}.
 */
public interface ExternalEvaluatorRepository extends JpaRepository<ExternalEvaluator, Long> {

    /**
     * Lista los evaluadores externos de un tipo del catálogo.
     *
     * @param evaluatorTypeId identificador del tipo de evaluador
     * @return evaluadores externos de ese tipo
     */
    List<ExternalEvaluator> findByEvaluatorTypeId(Long evaluatorTypeId);

    /**
     * Lista los evaluadores externos que creó un usuario.
     *
     * @param createdById identificador de quien los creó
     * @return evaluadores externos creados por ese usuario
     */
    List<ExternalEvaluator> findByCreatedByIdOrderByLastNamesAsc(Long createdById);

    /**
     * Busca un evaluador externo activo por coincidencia exacta de correo o de número
     * de documento. No busca por partes, para que las cuentas no se puedan recorrer.
     *
     * @param email    correo normalizado (sin espacios y en minúsculas)
     * @param document número de documento tal como se escribió
     * @return el evaluador, si hay uno activo con ese correo o documento
     */
    @Query("""
            select e from ExternalEvaluator e
            where e.active = true and (e.email = :email or e.documentNumber = :document)
            """)
    Optional<ExternalEvaluator> findActiveByEmailOrDocument(@Param("email") String email,
                                                           @Param("document") String document);
}
