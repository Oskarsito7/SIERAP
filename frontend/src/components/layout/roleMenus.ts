import {
  Building2,
  CalendarRange,
  ChartColumn,
  ClipboardCheck,
  FolderKanban,
  GraduationCap,
  KeyRound,
  Layers,
  ShieldCheck,
  SlidersHorizontal,
  Tags,
  UserCheck,
  Users,
  UsersRound,
  type LucideIcon,
} from 'lucide-react'

import type { UserRole } from '@/types/auth'

/** Una opción del menú lateral. */
export interface MenuItem {
  /** Ruta de la página (en inglés, como el resto de rutas del frontend). */
  path: string
  /** Texto del menú y título de la página, en español. */
  label: string
  icon: LucideIcon
  /** Qué hará la página; se muestra en "Próximamente" mientras no exista. */
  description: string
  /** Etapa del plan de trabajo (`docs/06`) en la que llega la página. */
  stage: number
}

/** Nombre de cada rol tal como lo ve el usuario. */
export const ROLE_LABELS: Record<UserRole, string> = {
  SUPER_ADMIN: 'Super administrador',
  ADMIN: 'Administrador',
  TEACHER: 'Docente',
  STUDENT: 'Estudiante',
  EXTERNAL_EVALUATOR: 'Evaluador externo',
}

const registrationCodes: Omit<MenuItem, 'description'> = {
  path: '/registration-codes',
  label: 'Códigos de registro',
  icon: KeyRound,
  stage: 2,
}

const reports: Omit<MenuItem, 'description'> = {
  path: '/reports',
  label: 'Reportes',
  icon: ChartColumn,
  stage: 7,
}

/**
 * Menú de cada rol, aprobado por el líder el 2026-09-27 (`docs/06`, Etapa 2). "Mi
 * cuenta" y "Cerrar sesión" son comunes a todos y los agrega el layout.
 */
export const ROLE_MENUS: Record<UserRole, MenuItem[]> = {
  SUPER_ADMIN: [
    {
      path: '/faculties',
      label: 'Facultades y programas',
      icon: Building2,
      description: 'La gestión de facultades y programas',
      stage: 3,
    },
    {
      path: '/periods',
      label: 'Periodos académicos',
      icon: CalendarRange,
      description: 'La gestión de periodos académicos y del límite de evaluación de la UPC',
      stage: 3,
    },
    {
      path: '/parameters',
      label: 'Parámetros globales',
      icon: SlidersHorizontal,
      description: 'Los parámetros globales del sistema',
      stage: 3,
    },
    {
      path: '/evaluator-types',
      label: 'Tipos de evaluador',
      icon: Tags,
      description: 'El catálogo de tipos de evaluador externo',
      stage: 3,
    },
    {
      path: '/admins',
      label: 'Administradores',
      icon: ShieldCheck,
      description: 'La gestión de las cuentas de administradores',
      stage: 2,
    },
  ],
  ADMIN: [
    {
      path: '/moments',
      label: 'Momentos',
      icon: Layers,
      description: 'La configuración y el lanzamiento de los momentos',
      stage: 3,
    },
    {
      path: '/teachers',
      label: 'Docentes',
      icon: Users,
      description: 'La gestión de las cuentas de docentes',
      stage: 2,
    },
    {
      ...registrationCodes,
      description: 'Los códigos de registro de los docentes de la facultad',
    },
    {
      ...reports,
      description: 'Los reportes individuales, grupales y comparativos',
    },
  ],
  TEACHER: [
    {
      path: '/groups',
      label: 'Mis grupos',
      icon: FolderKanban,
      description: 'La gestión de tus grupos, sus fechas y sus evaluadores',
      stage: 4,
    },
    {
      path: '/students',
      label: 'Mis estudiantes',
      icon: GraduationCap,
      description: 'La lista de tus estudiantes',
      stage: 2,
    },
    {
      path: '/external-evaluators',
      label: 'Evaluadores externos',
      icon: UserCheck,
      description: 'La gestión de los evaluadores externos de tus grupos',
      stage: 4,
    },
    {
      ...registrationCodes,
      description: 'Los códigos con los que tus estudiantes se registran',
    },
    {
      ...reports,
      description: 'Los reportes de tus grupos',
    },
  ],
  STUDENT: [
    {
      path: '/moment-groups',
      label: 'Grupos del momento',
      icon: UsersRound,
      description: 'Los grupos del momento y la inscripción en uno de ellos',
      stage: 5,
    },
    {
      path: '/my-group',
      label: 'Mi grupo y entregas',
      icon: FolderKanban,
      description: 'Tu grupo y las versiones de tus entregas',
      stage: 5,
    },
    {
      path: '/my-grades',
      label: 'Mis notas',
      icon: ChartColumn,
      description: 'Tus notas por RAP',
      stage: 6,
    },
  ],
  EXTERNAL_EVALUATOR: [
    {
      path: '/assigned-groups',
      label: 'Grupos asignados',
      icon: ClipboardCheck,
      description: 'Los grupos que te asignaron y su calificación',
      stage: 6,
    },
  ],
}

/** Ruta de "Mi cuenta", común a todos los roles. */
export const ACCOUNT_PATH = '/account'

/** Una página del menú con los roles que pueden abrirla. */
export interface MenuPage {
  path: string
  roles: UserRole[]
  /** Opción de menú de cada rol que la ve (el texto puede cambiar por rol). */
  items: Partial<Record<UserRole, MenuItem>>
}

/**
 * Páginas del menú agrupadas por ruta: "Códigos de registro" y "Reportes" son la misma
 * página para el admin y el docente, cada uno con su alcance.
 */
export function menuPages(): MenuPage[] {
  const pages = new Map<string, MenuPage>()
  for (const [role, items] of Object.entries(ROLE_MENUS) as [UserRole, MenuItem[]][]) {
    for (const item of items) {
      const page = pages.get(item.path) ?? { path: item.path, roles: [], items: {} }
      page.roles.push(role)
      page.items[role] = item
      pages.set(item.path, page)
    }
  }
  return [...pages.values()]
}
