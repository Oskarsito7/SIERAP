-- Etapa 5 (docs/07, "Submissions"; ADR-015). La presentación es opcional en cada
-- versión y se guardan el nombre original y el tamaño de cada archivo solo para
-- mostrarlos y para la descarga: en disco el nombre lo genera el backend.
-- Usa V8 porque V7 está reservada para individual_grade (Etapa 6, ADR-018).

ALTER TABLE submission ALTER COLUMN presentation_path DROP NOT NULL;

ALTER TABLE submission
    ADD COLUMN zip_file_name          VARCHAR(255) NOT NULL DEFAULT 'proyecto.zip',
    ADD COLUMN zip_size               BIGINT       NOT NULL DEFAULT 0,
    ADD COLUMN presentation_file_name VARCHAR(255),
    ADD COLUMN presentation_size      BIGINT;

-- Los valores por defecto solo cubren filas anteriores; las nuevas siempre los traen.
ALTER TABLE submission ALTER COLUMN zip_file_name DROP DEFAULT;
ALTER TABLE submission ALTER COLUMN zip_size DROP DEFAULT;

ALTER TABLE submission
    ADD CONSTRAINT ck_submission_zip_size CHECK (zip_size >= 0),
    ADD CONSTRAINT ck_submission_presentation CHECK (
        (presentation_path IS NULL AND presentation_file_name IS NULL AND presentation_size IS NULL)
        OR (presentation_path IS NOT NULL AND presentation_file_name IS NOT NULL
            AND presentation_size IS NOT NULL AND presentation_size >= 0));
