-- Parámetros globales para reportes académicos (Etapa 7 / RF 12, decisión 2026-10-29).
-- report.passing-percentage: porcentaje del puntaje máximo del momento necesario para aprobar.
-- report.grade-bands: niveles de desempeño y porcentaje mínimo de cada uno, separados por punto y coma.

INSERT INTO global_parameter (param_key, param_value, description)
VALUES
    ('report.passing-percentage', '60', 'Porcentaje mínimo sobre el puntaje máximo del momento para aprobar (RF 12)'),
    ('report.grade-bands', 'Bajo:0;Básico:60;Alto:76;Sobresaliente:90', 'Rangos de la distribución de calificaciones por porcentaje del puntaje máximo (RF 12)');
