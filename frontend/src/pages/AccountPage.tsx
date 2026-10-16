import { useAuth } from '@/auth/authContext'
import { ROLE_LABELS } from '@/components/layout/roleMenus'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ChangePasswordPage } from '@/pages/auth/ChangePasswordPage'

/**
 * "Mi cuenta" (wireframe AU-06), en el menú de todos los roles: los datos del usuario,
 * solo para ver (si hay un error los corrige quien creó la cuenta), y el cambio de
 * contraseña.
 */
export function AccountPage() {
  const { user } = useAuth()

  if (!user) {
    return null
  }

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Mi cuenta</h1>
      <div className="grid items-start gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Mis datos</CardTitle>
            <CardDescription>Si algún dato está mal, pídele a quien creó tu cuenta que lo corrija.</CardDescription>
          </CardHeader>
          <CardContent>
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
              <dt className="text-muted-foreground">Nombre</dt>
              <dd>{user.fullName}</dd>
              <dt className="text-muted-foreground">Correo</dt>
              <dd className="break-all">{user.email}</dd>
              <dt className="text-muted-foreground">Rol</dt>
              <dd>{ROLE_LABELS[user.role]}</dd>
            </dl>
          </CardContent>
        </Card>
        <ChangePasswordPage />
      </div>
    </div>
  )
}
