import { createBrowserRouter, RouterProvider } from 'react-router'

import { routes } from './routes'

const router = createBrowserRouter(routes)

/** Entry point of the application routing. */
export function AppRouter() {
  return <RouterProvider router={router} />
}
