/** Roles del sistema (espejo de `UserRole` del backend). */
export type UserRole = 'SUPER_ADMIN' | 'ADMIN' | 'TEACHER' | 'STUDENT' | 'EXTERNAL_EVALUATOR'

/** Tipos de documento de identidad (espejo de `DocumentType` del backend). */
export type DocumentType = 'CC' | 'TI' | 'CE' | 'PASSPORT'

/** Resumen del usuario autenticado (espejo de `UserSummaryResponse`). */
export interface UserSummary {
  id: number
  firstNames: string
  lastNames: string
  fullName: string
  email: string
  role: UserRole
  active: boolean
  /** Si es `true`, la única pantalla permitida es "Cambiar contraseña". */
  mustChangePassword: boolean
}

/** Sesión emitida por login, refresh y cambio de contraseña (espejo de `TokenResponse`). */
export interface TokenResponse {
  accessToken: string
  /** Vigencia del token de acceso, en segundos. */
  expiresIn: number
  user: UserSummary
}

/** Cuerpo de `POST /api/auth/login`. */
export interface LoginRequest {
  email: string
  password: string
}

/** Cuerpo de `POST /api/auth/password/change`. */
export interface ChangePasswordRequest {
  currentPassword: string
  newPassword: string
}

/** Cuerpo de `POST /api/auth/password/forgot`. */
export interface ForgotPasswordRequest {
  email: string
}

/** Cuerpo de `POST /api/auth/password/set` (enlace recibido por correo). */
export interface SetPasswordRequest {
  token: string
  newPassword: string
}

/** Cuerpo de `POST /api/auth/register` (registro de estudiante con código). */
export interface RegisterStudentRequest {
  registrationCode: string
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
  studentCode: string
  currentSemester: number
}
