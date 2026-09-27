import type {
  ChangePasswordRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RegisterStudentRequest,
  SetPasswordRequest,
  TokenResponse,
} from '@/types/auth'

import { httpClient, refreshSession, setSession } from './httpClient'

/** Inicio de sesión, sesiones y contraseñas (`/api/auth`, `docs/07-contrato-api.md`). */
export const authService = {
  /** Inicia sesión; un 401 aquí son credenciales incorrectas, no una sesión vencida. */
  async login(request: LoginRequest): Promise<TokenResponse> {
    const { data } = await httpClient.post<TokenResponse>('/auth/login', request, {
      skipAuthRefresh: true,
    })
    setSession(data)
    return data
  },

  /** Recupera la sesión al cargar la app con la cookie de renovación, si existe. */
  restoreSession(): Promise<TokenResponse> {
    return refreshSession()
  },

  /**
   * Cierra la sesión. Solo necesita la cookie, así que funciona aunque el token de
   * acceso haya vencido; la sesión local se borra aunque el backend no responda.
   */
  async logout(): Promise<void> {
    try {
      await httpClient.post('/auth/logout', undefined, { skipAuthRefresh: true })
    } finally {
      setSession(null)
    }
  },

  /**
   * Cambia la contraseña. El backend cierra las demás sesiones y responde con una
   * sesión nueva (ya sin la marca de contraseña temporal), que reemplaza a la actual.
   */
  async changePassword(request: ChangePasswordRequest): Promise<TokenResponse> {
    const { data } = await httpClient.post<TokenResponse>('/auth/password/change', request)
    setSession(data)
    return data
  },

  /** Pide el enlace de recuperación; responde igual exista o no la cuenta. */
  async forgotPassword(request: ForgotPasswordRequest): Promise<void> {
    await httpClient.post('/auth/password/forgot', request)
  },

  /**
   * Crea o recupera la contraseña con el token del enlace recibido por correo. El
   * backend cierra todas las sesiones del usuario, así que también se borra la local.
   */
  async setPassword(request: SetPasswordRequest): Promise<void> {
    await httpClient.post('/auth/password/set', request)
    setSession(null)
  },

  /** Registra a un estudiante con el código de su docente; el enlace llega por correo. */
  async register(request: RegisterStudentRequest): Promise<void> {
    await httpClient.post('/auth/register', request)
  },
}
