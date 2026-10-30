#!/usr/bin/env bash
# Regenera bd/01-schema.sql concatenando las migraciones de Flyway en orden de
# version. Ejecutar desde la raiz del repositorio:  bash bd/generate-schema.sh
# Cada vez que se agregue una migracion nueva (V10, V11...), volver a correrlo.
set -euo pipefail

MIGRATIONS="backend/src/main/resources/db/migration"
OUT="bd/01-schema.sql"

if [ ! -d "$MIGRATIONS" ]; then
  echo "Ejecutar desde la raiz del repositorio (no se encontro $MIGRATIONS)" >&2
  exit 1
fi

# Conserva el encabezado del archivo (todo lo anterior al primer bloque de migracion).
awk '/^-- ####/ {exit} {print}' "$OUT" > "$OUT.tmp"

# Orden numerico por version: V2 antes de V10.
find "$MIGRATIONS" -name 'V*.sql' | sort -t'V' -k2 -n | while read -r f; do
  {
    printf -- '-- #############################################################################\n'
    printf -- '-- %s\n' "$(basename "$f")"
    printf -- '-- #############################################################################\n\n'
    cat "$f"
    printf '\n'
  } >> "$OUT.tmp"
done

mv "$OUT.tmp" "$OUT"
echo "Generado $OUT ($(wc -l < "$OUT") lineas)"
