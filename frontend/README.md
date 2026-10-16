# SIERAP — Frontend

SIERAP web client: **React + TypeScript** with **Vite**, **Tailwind CSS v4** and
**shadcn/ui** components (see ADR-006 in `docs/05-decisiones-tecnicas.md`).

## Commands

```bash
npm install          # install dependencies (the first time)
npm run dev          # development server on http://localhost:5173
npm run lint         # ESLint
npm test             # tests (Vitest + React Testing Library)
npm run test:watch   # tests in watch mode
npm run build        # type checking + production build in dist/
```

In development, Vite forwards everything starting with `/api` to `http://localhost:8080`
(the backend), so the backend must be running. See `vite.config.ts`.

## Structure

```
src/
├── components/
│   ├── ui/          ← shadcn/ui components (added with the CLI, not by hand)
│   └── layout/      ← common page structure
├── lib/             ← utilities (shadcn/ui's cn)
├── pages/           ← one page per view; each role's pages arrive in stages 2 to 7
├── routes/          ← route table (routes.tsx) and the router
├── services/api/    ← HTTP client (axios) and one service per backend resource
├── types/           ← types that mirror the backend DTOs
└── test/            ← common test setup
```

- The `services/api` services are the **only** layer that talks to the backend; pages
  never call axios directly.
- The `types/` types must match the backend DTOs (`dto.response`): same routes and
  same JSON field names.
- Language (ADR-014): identifiers in English; texts on screen and code comments in
  Spanish.

## Adding shadcn/ui components

```bash
npx shadcn@latest add input label table dialog
```

The CLI copies the component code into `src/components/ui/` using the `components.json`
configuration. Catalog: <https://ui.shadcn.com/docs/components>.
