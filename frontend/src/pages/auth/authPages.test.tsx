import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import type { ReactNode } from 'react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { authService } from '@/services/api/authService'
import type { ApiError } from '@/types/api'
import type { TokenResponse, UserSummary } from '@/types/auth'

import { ChangePasswordPage } from './ChangePasswordPage'
import { LoginPage } from './LoginPage'
import { RegisterPage } from './RegisterPage'
import { SetPasswordPage } from './SetPasswordPage'

vi.mock('@/services/api/authService', () => ({
  authService: {
    login: vi.fn(),
    changePassword: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    setPassword: vi.fn(),
    getPrivacyPolicyUrl: vi.fn(),
  },
}))

const user: UserSummary = {
  id: 1,
  firstNames: 'Administrador',
  lastNames: 'SIERAP',
  fullName: 'Administrador SIERAP',
  email: 'sierap@unicesar.edu.co',
  role: 'SUPER_ADMIN',
  active: true,
  mustChangePassword: true,
}

function apiError(status: number, message: string, fieldErrors: Record<string, string> = {}): AxiosError {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors }
  return new AxiosError(message, 'ERR_BAD_REQUEST', undefined, null, { status, data } as AxiosResponse)
}

/** Renderiza la página en `path` con una ruta "/" de destino para ver la redirección. */
function renderPage(path: string, page: ReactNode, entry: string = path) {
  const router = createMemoryRouter(
    [
      { path, element: page },
      { path: '/', element: <p>Inicio</p> },
    ],
    { initialEntries: [entry] },
  )
  render(
    <AuthContext value={{ state: { status: 'authenticated', user }, user, logout: vi.fn() }}>
      <RouterProvider router={router} />
    </AuthContext>,
  )
  return router
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.mocked(authService.login).mockReset()
  })

  it('signs in and goes to the home page', async () => {
    vi.mocked(authService.login).mockResolvedValue({} as TokenResponse)
    renderPage('/login', <LoginPage />)

    await userEvent.type(screen.getByLabelText('Correo'), ' sierap@unicesar.edu.co ')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'Sierap-Inicio-2026')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByText('Inicio')).toBeInTheDocument()
    expect(authService.login).toHaveBeenCalledWith({
      email: 'sierap@unicesar.edu.co',
      password: 'Sierap-Inicio-2026',
    })
  })

  it('shows the message of wrong credentials', async () => {
    vi.mocked(authService.login).mockRejectedValue(
      apiError(401, 'Correo o contraseña incorrectos.'),
    )
    renderPage('/login', <LoginPage />)

    await userEvent.type(screen.getByLabelText('Correo'), 'sierap@unicesar.edu.co')
    await userEvent.type(screen.getByLabelText('Contraseña'), 'mala')
    await userEvent.click(screen.getByRole('button', { name: 'Ingresar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Correo o contraseña incorrectos.')
  })
})

describe('ChangePasswordPage', () => {
  beforeEach(() => {
    vi.mocked(authService.changePassword).mockReset()
  })

  async function fill(newPassword: string, confirmPassword = newPassword) {
    await userEvent.type(screen.getByLabelText('Contraseña actual'), 'Sierap-Inicio-2026')
    await userEvent.type(screen.getByLabelText('Contraseña nueva'), newPassword)
    await userEvent.type(screen.getByLabelText('Confirmar contraseña nueva'), confirmPassword)
    await userEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
  }

  it('checks the password rules before sending', async () => {
    renderPage('/change-password', <ChangePasswordPage />)

    await fill('corta', 'otra')

    expect(screen.getByText(/al menos 8 caracteres/, { selector: 'p[id]' })).toBeInTheDocument()
    expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument()
    expect(authService.changePassword).not.toHaveBeenCalled()
  })

  it('changes the password and continues to the home page', async () => {
    vi.mocked(authService.changePassword).mockResolvedValue({} as TokenResponse)
    renderPage('/change-password', <ChangePasswordPage />)

    await fill('Nueva-Clave-2026')

    expect(await screen.findByText('Inicio')).toBeInTheDocument()
    expect(authService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'Sierap-Inicio-2026',
      newPassword: 'Nueva-Clave-2026',
    })
  })

  it('shows the 422 message when the current password is wrong', async () => {
    vi.mocked(authService.changePassword).mockRejectedValue(
      apiError(422, 'La contraseña actual no es correcta.'),
    )
    renderPage('/change-password', <ChangePasswordPage />)

    await fill('Nueva-Clave-2026')

    expect(await screen.findByRole('alert')).toHaveTextContent('La contraseña actual no es correcta.')
  })
})

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.mocked(authService.register).mockReset()
    vi.mocked(authService.getPrivacyPolicyUrl).mockReset()
    vi.mocked(authService.getPrivacyPolicyUrl).mockResolvedValue(null)
  })

  async function fillAndSubmit(email: string, accept = true) {
    await userEvent.type(screen.getByLabelText('Código de registro'), 'ab12cd34')
    await userEvent.type(screen.getByLabelText('Nombres'), 'Ana')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Pérez')
    await userEvent.selectOptions(screen.getByLabelText('Tipo de documento'), 'TI')
    await userEvent.type(screen.getByLabelText('Número de documento'), '1065000111')
    await userEvent.type(screen.getByLabelText('Correo institucional'), email)
    await userEvent.type(screen.getByLabelText('Código estudiantil'), '20261001')
    await userEvent.type(screen.getByLabelText('Semestre actual'), '3')
    if (accept) await userEvent.click(screen.getByRole('checkbox', { name: /Autorizo a la Universidad Popular del Cesar/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Registrarme' }))
  }

  it('registers the student and asks to check the email', async () => {
    vi.mocked(authService.register).mockResolvedValue()
    renderPage('/register', <RegisterPage />)

    await fillAndSubmit('ana@unicesar.edu.co')

    expect(await screen.findByRole('heading', { name: 'Revisa tu correo' })).toBeInTheDocument()
    expect(authService.register).toHaveBeenCalledWith({
      registrationCode: 'ab12cd34',
      firstNames: 'Ana',
      lastNames: 'Pérez',
      documentType: 'TI',
      documentNumber: '1065000111',
      email: 'ana@unicesar.edu.co',
      studentCode: '20261001',
      currentSemester: 3,
      privacyAccepted: true,
    })
  })

  it('does not register without the data authorization and says why', async () => {
    renderPage('/register', <RegisterPage />)

    await fillAndSubmit('ana@unicesar.edu.co', false)

    expect(
      await screen.findByText('Debes autorizar el tratamiento de tus datos personales para registrarte.'),
    ).toBeInTheDocument()
    expect(authService.register).not.toHaveBeenCalled()
  })

  it('links the privacy policy in a new tab when the URL is configured', async () => {
    vi.mocked(authService.getPrivacyPolicyUrl).mockResolvedValue('https://www.unicesar.edu.co/datos')
    renderPage('/register', <RegisterPage />)

    const link = await screen.findByRole('link', { name: 'Política de tratamiento de datos' })
    expect(link).toHaveAttribute('href', 'https://www.unicesar.edu.co/datos')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer')
  })

  it('shows the authorization text without a link when there is no policy URL', async () => {
    renderPage('/register', <RegisterPage />)

    expect(
      await screen.findByLabelText(/Autorizo a la Universidad Popular del Cesar el tratamiento de mis datos personales/),
    ).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Política de tratamiento de datos' })).not.toBeInTheDocument()
  })

  it('shows the 422 message of a non institutional email', async () => {
    vi.mocked(authService.register).mockRejectedValue(
      apiError(422, 'Para registrarse debe usar su correo institucional (@unicesar.edu.co).'),
    )
    renderPage('/register', <RegisterPage />)

    await fillAndSubmit('ana@gmail.com')

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Para registrarse debe usar su correo institucional (@unicesar.edu.co).',
    )
  })
})

describe('SetPasswordPage', () => {
  beforeEach(() => {
    vi.mocked(authService.setPassword).mockReset()
    vi.mocked(authService.getPrivacyPolicyUrl).mockReset()
    vi.mocked(authService.getPrivacyPolicyUrl).mockResolvedValue('https://www.unicesar.edu.co/datos')
  })

  async function fill(accept: boolean) {
    await userEvent.type(screen.getByLabelText('Contraseña nueva'), 'Clave-segura-1')
    await userEvent.type(screen.getByLabelText('Confirmar contraseña nueva'), 'Clave-segura-1')
    if (accept) await userEvent.click(screen.getByRole('checkbox', { name: /Autorizo a la Universidad Popular del Cesar/ }))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar contraseña' }))
  }

  it('requires the data authorization before saving the password', async () => {
    renderPage('/set-password', <SetPasswordPage />, '/set-password?token=abc')

    await fill(false)

    expect(
      await screen.findByText('Debes autorizar el tratamiento de tus datos personales para crear tu contraseña.'),
    ).toBeInTheDocument()
    expect(authService.setPassword).not.toHaveBeenCalled()
  })

  it('sends the authorization with the new password', async () => {
    vi.mocked(authService.setPassword).mockResolvedValue()
    renderPage('/set-password', <SetPasswordPage />, '/set-password?token=abc')

    expect(await screen.findByRole('link', { name: 'Política de tratamiento de datos' })).toHaveAttribute(
      'href',
      'https://www.unicesar.edu.co/datos',
    )
    await fill(true)

    expect(await screen.findByText('Tu contraseña quedó guardada. Ya puedes iniciar sesión.')).toBeInTheDocument()
    expect(authService.setPassword).toHaveBeenCalledWith({
      token: 'abc',
      newPassword: 'Clave-segura-1',
      privacyAccepted: true,
    })
  })

  it('shows the 422 message of the API', async () => {
    vi.mocked(authService.setPassword).mockRejectedValue(
      apiError(422, 'El enlace no es válido o ya venció. Solicite uno nuevo.'),
    )
    renderPage('/set-password', <SetPasswordPage />, '/set-password?token=abc')

    await fill(true)

    expect(await screen.findByRole('alert')).toHaveTextContent('El enlace no es válido o ya venció.')
  })
})
