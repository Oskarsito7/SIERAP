import { createBrowserRouter, RouterProvider } from 'react-router'

import { routes } from './routes'

const router = createBrowserRouter(routes)

/** Punto de entrada del enrutamiento de la aplicación. */
export function AppRouter() {
  return <RouterProvider router={router} />
}
