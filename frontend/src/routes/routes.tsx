import type { RouteObject } from 'react-router'

import { MainLayout } from '@/components/layout/MainLayout'
import { ChangePasswordPage } from '@/pages/auth/ChangePasswordPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { SetPasswordPage } from '@/pages/auth/SetPasswordPage'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'

import { CHANGE_PASSWORD_PATH, GuestOnly, RequireAuth } from './guards'

/**
 * Tabla de rutas de la aplicación. Las páginas de acceso son públicas (login y
 * registro solo sin sesión); el resto exige sesión. Las ramas por rol (una por cada
 * `UserRole`) se agregan con el layout por rol.
 */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <MainLayout />,
    children: [
      {
        element: <GuestOnly />,
        children: [
          { path: 'login', element: <LoginPage /> },
          { path: 'register', element: <RegisterPage /> },
        ],
      },
      // Los enlaces del correo funcionan con o sin sesión abierta.
      { path: 'forgot-password', element: <ForgotPasswordPage /> },
      { path: 'set-password', element: <SetPasswordPage /> },
      {
        element: <RequireAuth />,
        children: [
          { index: true, element: <HomePage /> },
          { path: CHANGE_PASSWORD_PATH.slice(1), element: <ChangePasswordPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]
