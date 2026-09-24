import { createBrowserRouter, RouterProvider } from 'react-router'

import { rutas } from './rutas'

const router = createBrowserRouter(rutas)

/** Punto de entrada del enrutamiento de la aplicación. */
export function AppRouter() {
  return <RouterProvider router={router} />
}
