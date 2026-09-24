import type { RouteObject } from 'react-router'

import { MainLayout } from '@/components/layout/MainLayout'
import { InicioPage } from '@/pages/InicioPage'
import { NotFoundPage } from '@/pages/NotFoundPage'

/**
 * Tabla de rutas de la aplicación. En la Etapa 2 se agregan las rutas protegidas
 * por rol (una rama por cada `RolUsuario`, envuelta en una guarda de autenticación).
 */
export const rutas: RouteObject[] = [
  {
    path: '/',
    element: <MainLayout />,
    children: [
      { index: true, element: <InicioPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
