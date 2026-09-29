/** Código de registro de estudiantes (espejo de `RegistrationCodeResponse` del backend). */
export interface RegistrationCode {
  id: number
  /** 8 caracteres, sin 0/O ni 1/I. */
  code: string
  programId: number
  programName: string
  teacherId: number
  teacherName: string
  /** Fecha `YYYY-MM-DD` hasta la que sirve (incluida). */
  expiresOn: string
  active: boolean
  /** Estudiantes que se registraron con él. */
  registeredCount: number
}

/** Cuerpo de `POST /api/registration-codes`; `teacherId` solo lo envían el admin y el super_admin. */
export interface NewRegistrationCodeRequest {
  programId: number
  expiresOn: string
  teacherId?: number
}
