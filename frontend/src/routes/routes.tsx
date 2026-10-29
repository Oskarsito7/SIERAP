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
import { EvaluatorTypesPage } from '@/pages/catalogs/EvaluatorTypesPage'
import { FacultiesPage } from '@/pages/catalogs/FacultiesPage'
import { ParametersPage } from '@/pages/catalogs/ParametersPage'
import { PeriodsPage } from '@/pages/catalogs/PeriodsPage'
import { AssignedGroupPage } from '@/pages/evaluator/AssignedGroupPage'
import { AssignedGroupsPage } from '@/pages/evaluator/AssignedGroupsPage'
import { ExternalEvaluatorsPage } from '@/pages/externalEvaluators/ExternalEvaluatorsPage'
import { GroupDetailPage } from '@/pages/groups/GroupDetailPage'
import { TeacherGroupsPage } from '@/pages/groups/TeacherGroupsPage'
import { HomePage } from '@/pages/HomePage'
import { MomentDetailPage } from '@/pages/moments/MomentDetailPage'
import { MomentsPage } from '@/pages/moments/MomentsPage'
import { NewMomentPage } from '@/pages/moments/NewMomentPage'
import { NotFoundPage } from '@/pages/NotFoundPage'
import { RegistrationCodesPage } from '@/pages/registrationCodes/RegistrationCodesPage'
import { ReportsPage } from '@/pages/reports/ReportsPage'
import { MomentGroupsPage } from '@/pages/student/MomentGroupsPage'
import { MyGradesPage } from '@/pages/student/MyGradesPage'
import { MyGroupPage } from '@/pages/student/MyGroupPage'
import { StudentsPage } from '@/pages/students/StudentsPage'
import { TeachersPage } from '@/pages/teachers/TeachersPage'

import { CHANGE_PASSWORD_PATH, GuestOnly, RequireAuth } from './guards'
import { MenuPageRoute } from './MenuPageRoute'
import { RequireRole } from './RequireRole'

/** Páginas del menú que ya existen; las demás muestran "Próximamente". */
const MENU_PAGES: Record<string, ReactNode> = {
  '/groups': <TeacherGroupsPage />,
  '/students': <StudentsPage />,
  '/teachers': <TeachersPage />,
  '/moments': <MomentsPage />,
  '/faculties': <FacultiesPage />,
  '/periods': <PeriodsPage />,
  '/parameters': <ParametersPage />,
  '/evaluator-types': <EvaluatorTypesPage />,
  '/external-evaluators': <ExternalEvaluatorsPage />,
  '/registration-codes': <RegistrationCodesPage />,
  '/reports': <ReportsPage />,
  '/moment-groups': <MomentGroupsPage />,
  '/my-group': <MyGroupPage />,
  '/my-grades': <MyGradesPage />,
  '/assigned-groups': <AssignedGroupsPage />,
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
              ...(['summary', 'submissions', 'evaluation', 'results'] as const).map((tab) => ({
                path: tab === 'summary' ? 'groups/:id' : `groups/:id/${tab}`,
                element: (
                  <RequireRole roles={['TEACHER']}>
                    <GroupDetailPage tab={tab} />
                  </RequireRole>
                ),
              })),
              {
                path: 'moments/new',
                element: (
                  <RequireRole roles={['ADMIN']}>
                    <NewMomentPage />
                  </RequireRole>
                ),
              },
              ...(['data', 'rubric', 'weights', 'groups'] as const).map((tab) => ({
                path: tab === 'data' ? 'moments/:id' : `moments/:id/${tab}`,
                element: (
                  <RequireRole roles={['ADMIN']}>
                    <MomentDetailPage tab={tab} />
                  </RequireRole>
                ),
              })),
              {
                path: 'assigned-groups/:id',
                element: (
                  <RequireRole roles={['EXTERNAL_EVALUATOR']}>
                    <AssignedGroupPage />
                  </RequireRole>
                ),
              },
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
