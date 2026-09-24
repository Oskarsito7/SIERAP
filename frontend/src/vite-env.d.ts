/// <reference types="vite/client" />

/** SIERAP environment variables (defined in .env, see .env.example). */
interface ImportMetaEnv {
  /** Base URL of the API. Defaults to `/api`, which Vite forwards to the backend in development. */
  readonly VITE_API_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
