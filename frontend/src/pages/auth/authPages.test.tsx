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

vi.mock('@/services/api/authService', () => ({
  authService: { login: vi.fn(), changePassword: vi.fn(), register: vi.fn(), logout: vi.fn() },
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
function renderPage(path: string, page: ReactNode) {
  const router = createMemoryRouter(
    [
      { path, element: page },
      { path: '/', element: <p>Inicio</p> },
    ],
    { initialEntries: [path] },
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
  })

  async function fillAndSubmit(email: string) {
    await userEvent.type(screen.getByLabelText('Código de registro'), 'ab12cd34')
    await userEvent.type(screen.getByLabelText('Nombres'), 'Ana')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Pérez')
    await userEvent.selectOptions(screen.getByLabelText('Tipo de documento'), 'TI')
    await userEvent.type(screen.getByLabelText('Número de documento'), '1065000111')
    await userEvent.type(screen.getByLabelText('Correo institucional'), email)
    await userEvent.type(screen.getByLabelText('Código estudiantil'), '20261001')
    await userEvent.type(screen.getByLabelText('Semestre actual'), '3')
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
    })
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
