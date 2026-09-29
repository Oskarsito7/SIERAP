import type { ReactNode } from 'react'
import type { RouteObject } from 'react-router'

import { AppLayout } from '@/components/layout/AppLayout'
import { MainLayout } from '@/components/layout/MainLayout'
import { ACCOUNT_PATH, menuPages } from '@/components/layout/roleMenus'
import { AccountPage } from '@/pages/AccountPage'
import { ChangePasswordPage } from '@/pages/auth/ChangePasswordPage'
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage'
import { LoginPage } from '@/pages/auth/LoginPage'
import { RegisterPage } from '@/pages/auth/RegisterPage'
import { SetPasswordPage } from '@/pages/auth/SetPasswordPage'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { RegistrationCodesPage } from '@/pages/registrationCodes/RegistrationCodesPage'

import { CHANGE_PASSWORD_PATH, GuestOnly, RequireAuth } from './guards'
import { MenuPageRoute } from './MenuPageRoute'
import { RequireRole } from './RequireRole'

/** Páginas del menú que ya existen; las demás muestran "Próximamente". */
const MENU_PAGES: Record<string, ReactNode> = {
  '/registration-codes': <RegistrationCodesPage />,
}

/**
 * Tabla de rutas de la aplicación. Las páginas de acceso son públicas (login y
 * registro solo sin sesión) y usan `MainLayout`; el resto exige sesión y usa el layout
 * por rol (`AppLayout`). Cada opción del menú tiene su ruta, protegida por rol; las
 * que todavía no tienen página muestran "Próximamente".
 */
export const routes: RouteObject[] = [
  {
    path: '/',
    children: [
      {
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
          { path: '*', element: <NotFoundPage /> },
        ],
      },
      {
        element: <RequireAuth />,
        children: [
          {
            element: <AppLayout />,
            children: [
              { index: true, element: <HomePage /> },
              { path: CHANGE_PASSWORD_PATH.slice(1), element: <ChangePasswordPage /> },
              { path: ACCOUNT_PATH.slice(1), element: <AccountPage /> },
              ...menuPages().map((page) => ({
                path: page.path.slice(1),
                element: MENU_PAGES[page.path] ? (
                  <RequireRole roles={page.roles}>{MENU_PAGES[page.path]}</RequireRole>
                ) : (
                  <MenuPageRoute page={page} />
                ),
              })),
            ],
          },
        ],
      },
    ],
  },
]
