/// <reference types="vite/client" />

/** Variables de entorno propias de SIERAP (definidas en .env, ver .env.example). */
interface ImportMetaEnv {
  /** URL base de la API. Por defecto `/api`, que Vite reenvía al backend en desarrollo. */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
