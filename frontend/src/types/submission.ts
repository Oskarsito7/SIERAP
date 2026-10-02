/** Versión de la entrega de un grupo (espejo de `SubmissionResponse`, `Submission` en `docs/07`). */
export interface Submission {
  id: number
  groupId: number
  versionNumber: number
  /** Fecha y hora de la carga, `YYYY-MM-DDTHH:mm:ss`. */
  submittedAt: string
  late: boolean
  /** Nombre original del `.zip`, solo para mostrarlo y como nombre de la descarga. */
  zipFileName: string
  zipSize: number
  /** `null` si esta versión no trae presentación. */
  presentationFileName: string | null
  presentationSize: number | null
  /** La versión que califican los evaluadores. */
  toGrade: boolean
}

/** Historial de entregas de un grupo (`GET /api/groups/{id}/submissions`). */
export interface SubmissionHistory {
  /** De la más reciente a la más antigua. */
  versions: Submission[]
  /** La versión más reciente que trae presentación, o `null`. */
  currentPresentation: Submission | null
}
