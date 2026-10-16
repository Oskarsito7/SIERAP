/** Referencia corta a una persona. */
export interface PersonRef {
  id: number
  fullName: string
}

/** Integrante de un grupo. Los estudiantes de otros grupos solo ven `fullName`. */
export interface GroupMember {
  id: number
  fullName: string
  studentCode: string | null
  email: string | null
}

/** Evaluador externo asignado a un grupo. */
export interface GroupEvaluator {
  id: number
  fullName: string
  /** Nombre del tipo de evaluador, p. ej. "Empresario". */
  evaluatorType: string
  /** `false` si su tipo no tiene peso en el momento: su evaluación no cuenta en la nota. */
  countsInGrade: boolean
}

/** Grupo (= proyecto de aula), espejo de `GroupResponse` (`Group` en `docs/07`). */
export interface Group {
  id: number
  momentId: number
  momentName: string
  name: string
  projectTitle: string | null
  teacher: PersonRef
  memberCount: number
  minMembers: number
  maxMembers: number
  full: boolean
  members: GroupMember[]
  /** `YYYY-MM-DD`; `null` hasta que el docente la fije. */
  submissionDeadline: string | null
  /** `YYYY-MM-DD`; `null` hasta que el docente la fije. */
  evaluationDate: string | null
  evaluators: GroupEvaluator[]
  myGroup: boolean
}

/** Cambios parciales de un grupo (`PATCH /api/groups/{id}`); solo se envían los que cambian. */
export interface UpdateGroupRequest {
  name?: string
  submissionDeadline?: string
  evaluationDate?: string
}

/** Estudiante del programa del momento que no tiene grupo en él. */
export interface StudentWithoutGroup {
  id: number
  fullName: string
  studentCode: string
  currentSemester: number
}
