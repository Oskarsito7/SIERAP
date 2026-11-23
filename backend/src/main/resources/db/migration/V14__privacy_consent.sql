-- Habeas Data (Ley 1581 de 2012, ADR-024): fecha en que la persona autorizó el
-- tratamiento de sus datos personales. Es NULL para las cuentas que aún no lo han
-- hecho (las anteriores a esta migración y las creadas por otros hasta que crean su
-- contraseña por primera vez).
-- El parámetro global opcional 'privacy.policy-url' (URL de la política de tratamiento
-- de datos) NO se siembra aquí: lo crea el super_admin desde Parámetros.

ALTER TABLE app_user ADD COLUMN privacy_accepted_at TIMESTAMP;
