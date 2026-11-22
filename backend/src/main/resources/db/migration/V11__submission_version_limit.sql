-- Tope de versiones de entrega por grupo (auditoría de seguridad, run-1).
-- Cada entrega se guarda como una versión nueva y nunca se sobrescribe (ADR-015); sin
-- tope, un integrante podía llenar el volumen compartido subiendo versiones sin fin.
-- El valor es configurable por el super_admin como los demás parámetros (ADR-010).

INSERT INTO global_parameter (param_key, param_value, description)
VALUES ('submission.max-versions', '20', 'Número máximo de versiones de entrega que puede subir un grupo en un momento (ADR-015)');
