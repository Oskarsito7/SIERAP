import type { RouteObject } from 'react-router'

import { MainLayout } from '@/components/layout/MainLayout'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'

/**
 * Tabla de rutas de la aplicación. En la Etapa 2 se agregan las rutas protegidas
 * por rol (una rama por cada `UserRole`, envuelta en una guarda de autenticación).
 */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
