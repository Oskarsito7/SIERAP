import type { ComponentType } from 'react'

import { useAuth } from '@/auth/authContext'
import { ROLE_LABELS } from '@/components/layout/roleMenus'
import type { UserRole } from '@/types/auth'

import { AdminHome } from './AdminHome'
import { EvaluatorHome } from './EvaluatorHome'
import { QuickLinks } from './homeParts'
import { StudentHome } from './StudentHome'
import { SuperAdminHome } from './SuperAdminHome'
import { TeacherHome } from './TeacherHome'

/** Resumen de cada rol: lo pendiente y lo que pasa ahora en sus momentos. */
const ROLE_HOMES: Record<UserRole, ComponentType> = {
  SUPER_ADMIN: SuperAdminHome,
  ADMIN: AdminHome,
  TEACHER: TeacherHome,
  STUDENT: StudentHome,
  EXTERNAL_EVALUATOR: EvaluatorHome,
}

/** Frase de bienvenida según el rol. */
const ROLE_GREETINGS: Record<UserRole, string> = {
  SUPER_ADMIN: 'Este es el resumen institucional de SIERAP.',
  ADMIN: 'Así van los momentos de los programas de tu facultad.',
  TEACHER: 'Esto es lo que tienes pendiente con tus grupos.',
  STUDENT: 'Aquí tienes tu grupo, tus entregas y tus fechas.',
  EXTERNAL_EVALUATOR: 'Gracias por apoyar la evaluación de los proyectos de aula de la UPC.',
}

/**
 * Inicio de la aplicación: un saludo, el resumen propio del rol (lo pendiente y la fase
 * de sus momentos) y accesos rápidos a las páginas de su menú. Reemplaza la página
 * técnica de la Etapa 0; el estado del servidor solo se muestra al super_admin.
 */
export function HomePage() {
  const { user } = useAuth()
  if (!user) return null
  const RoleHome = ROLE_HOMES[user.role]

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-col gap-1">
        <p className="text-muted-foreground text-sm">{ROLE_LABELS[user.role]}</p>
        <h1 className="text-3xl font-bold tracking-tight">Hola, {user.firstNames}</h1>
        <p className="text-muted-foreground max-w-2xl">{ROLE_GREETINGS[user.role]}</p>
      </section>
      <RoleHome />
      <QuickLinks role={user.role} />
    </div>
  )
}
