# SIERAP — Frontend

Cliente web de SIERAP: **React + TypeScript** con **Vite**, **Tailwind CSS v4** y
componentes **shadcn/ui** (ver ADR-006 en `docs/05-decisiones-tecnicas.md`).

## Comandos

```bash
npm install          # instalar dependencias (la primera vez)
npm run dev          # servidor de desarrollo en http://localhost:5173
npm run lint         # ESLint
npm test             # pruebas (Vitest + React Testing Library)
npm run test:watch   # pruebas en modo observador
npm run build        # verificación de tipos + build de producción en dist/
```

En desarrollo, Vite reenvía todo lo que empiece por `/api` a `http://localhost:8080`
(el backend), así que el backend debe estar corriendo. Ver `vite.config.ts`.

## Estructura

```
src/
├── components/
│   ├── ui/          ← componentes de shadcn/ui (se agregan con el CLI, no a mano)
│   └── layout/      ← estructura común de las páginas
├── lib/             ← utilidades (cn de shadcn/ui)
├── pages/           ← una página por vista; las de cada rol llegan en las etapas 2 a 7
├── routes/          ← tabla de rutas (routes.tsx) y el router
├── services/api/    ← cliente HTTP (axios) y un servicio por recurso del backend
├── types/           ← tipos que reflejan los DTO del backend
└── test/            ← configuración común de las pruebas
```

- Los servicios de `services/api` son la **única** capa que habla con el backend;
  las páginas nunca llaman a axios directamente.
- Los tipos de `types/` deben coincidir con los DTO del backend (`dto.response`).

## Agregar componentes de shadcn/ui

```bash
npx shadcn@latest add input label table dialog
```

El CLI copia el código del componente en `src/components/ui/` usando la configuración
de `components.json`. Catálogo: <https://ui.shadcn.com/docs/components>.
