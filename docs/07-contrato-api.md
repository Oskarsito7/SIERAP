# 07 — API contract

> "Contract before code" (`06-plan-de-trabajo.md`): the routes, DTOs and error codes are
> agreed here before implementing them, so the frontend can work against simulated
> data while the backend is being built. Drafted by D1; approved by the project lead.
> Stage 2 (authentication and accounts) and Stages 4 to 6 (groups, submissions and
> evaluation) are below, and so is the academic configuration (Stage 3, written by D2:
> catalogs, moments, RAP, launch and copy).

## Conventions

- Base path `/api`. JSON bodies with **camelCase** English field names (ADR-014);
  dates `YYYY-MM-DD`, date-times `YYYY-MM-DDTHH:mm:ss` in `America/Bogota`.
- Authenticated calls send `Authorization: Bearer <accessToken>`.
- Every error answers with the uniform `ErrorResponse`
  (`timestamp`, `status`, `error`, `message` in Spanish, `path`, `fieldErrors`):

| Status | When |
|---|---|
| 400 | Invalid data (`fieldErrors` has the message of each field) |
| 401 | Missing, invalid or expired access token; wrong credentials |
| 403 | Authenticated but not allowed (role or scope), or the password must be changed first |
| 404 | The resource does not exist |
| 409 | The database rejects the operation (duplicate or related data, ADR-016) |
| 413 | An uploaded file is larger than allowed (submissions, ADR-015) |
| 422 | A business rule is violated |

## Shared DTOs

```jsonc
// UserSummary
{
  "id": 12,
  "firstNames": "Marta",
  "lastNames": "Díaz",
  "fullName": "Marta Díaz",
  "email": "marta@unicesar.edu.co",
  "role": "TEACHER",               // SUPER_ADMIN | ADMIN | TEACHER | STUDENT | EXTERNAL_EVALUATOR
  "active": true,
  "mustChangePassword": false
}

// TokenResponse
{
  "accessToken": "eyJhbGciOi...",
  "expiresIn": 900,                // seconds (15 minutes)
  "user": { /* UserSummary */ }
}
```

## Stage 2 — Authentication (`/api/auth`, ADR-017)

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `POST /api/auth/login` | anyone | `{ "email", "password" }` | `200 TokenResponse` + refresh cookie. `401` "Correo o contraseña incorrectos." (the same message whether the email exists or not, and for accounts that have not set their password yet). `403` if the account is deactivated |
| `POST /api/auth/refresh` | anyone with the refresh cookie | — | `200 TokenResponse` + rotated cookie. `401` if the cookie is missing, expired, already used or its session was closed |
| `POST /api/auth/logout` | anyone with the refresh cookie (works even if the access token expired) | — | `204`; closes that session and clears the cookie. Other sessions of the user stay open |
| `GET /api/auth/me` | authenticated | — | `200 UserSummary` |
| `POST /api/auth/password/change` | authenticated | `{ "currentPassword", "newPassword" }` | `200 TokenResponse` + new refresh cookie, with `mustChangePassword: false`; the user's other sessions are closed. `422` "La contraseña actual no es correcta." |
| `POST /api/auth/password/forgot` | anyone | `{ "email" }` | Always `204`; if the account exists and is active, an email with a link is sent |
| `POST /api/auth/password/set` | anyone with a link token | `{ "token", "newPassword" }` | `204`; sets the password of a new account or resets a forgotten one, and closes every session of the user. `422` if the token expired or was already used. The user then logs in normally |
| `POST /api/auth/register` | anyone with a registration code | `{ "registrationCode", "firstNames", "lastNames", "documentType", "documentNumber", "email", "studentCode", "currentSemester" }` | `202`; creates the student in the program of the code and sends the link to set the password. The code is case-insensitive. `422` if the code does not exist, expired or was deactivated, if the email is not institutional (`@unicesar.edu.co`), or if the email, document or student code is already registered |

**Temporary password:** while `user.mustChangePassword` is `true`, only the
`/api/auth/*` calls (and `/api/status`) work; any other answers `403` "Debe cambiar su
contraseña temporal antes de continuar." After `password/change`, use the new
`accessToken` it returns.

**Refresh cookie:** name `sierap_refresh`, `HttpOnly`, `SameSite=Strict`,
`Path=/api/auth`, `Secure` in production (configurable, off in local development over
plain http). In development the Vite proxy makes the frontend and the API the same
origin, so the cookie works on `localhost:5173`.

**Frontend flow:** when the app loads it calls `POST /api/auth/refresh`; if it answers
`200`, the session is restored without asking for the password. The access token lives
only in memory; when a call answers `401`, the HTTP client calls `/refresh` once and
retries; if that fails, it goes to the login page. If `user.mustChangePassword` is
`true`, the only screen shown is "change password".

**Only one refresh at a time.** Each refresh cookie works once: presenting one that was
already rotated is treated as a stolen token and **closes every session of the user**.
The frontend must share a single in-flight `/refresh` call (keep the pending promise and
reuse it), both in the `401` interceptor and on app load: React's `StrictMode` runs
effects twice in development, which would otherwise send two refreshes with the same
cookie.

**Email links** point to the frontend: `/set-password?token=...` (new account or
forgotten password). The frontend page calls `POST /api/auth/password/set`. The link of
a new account expires in 72 hours and the "forgot password" one in 1 hour.

**Password rules** (`newPassword` in every call): at least 8 characters, with at least
one letter and one number. Otherwise the answer is `400` with the message in
`fieldErrors.newPassword`.

## Stage 2 — Accounts

Who can create whom (ADR-017): super_admin → anyone; admin → teachers, students and
external evaluators of their faculty; teacher → students of the programs of their
faculty and external evaluators. New accounts have no password until the person uses
the link sent by email.

```jsonc
// NewUserRequest (common part of every creation)
{ "firstNames", "lastNames", "documentType", "documentNumber", "email" }
```

| Method and route | Who | Body (besides `NewUserRequest`) | Answer |
|---|---|---|---|
| `POST /api/admins` | super_admin | `facultyId` | `201 UserSummary` |
| `POST /api/teachers` | super_admin, admin of the faculty | `facultyId` | `201 UserSummary` |
| `POST /api/students` | super_admin, admin or teacher of the faculty | `programId`, `studentCode`, `currentSemester` | `201 UserSummary` |
| `POST /api/students/bulk` | same as above | `multipart/form-data`: `file`, `programId` | `200 { "created": n, "errors": [ { "row", "message" } ] }`. **Not implemented yet:** the file format is pending (see `docs/04`) |
| `POST /api/external-evaluators` | super_admin, admin, teacher | `evaluatorTypeId`; optional `organization`, `position`, `graduationYear`, `graduationProgramId` | `201 UserSummary` |
| `GET /api/teachers?facultyId=` | super_admin, admin of the faculty | — | `200 UserSummary[]` |
| `GET /api/students?programId=` | super_admin, admin or teacher of the faculty | — | `200 UserSummary[]` |
| `GET /api/external-evaluators` | super_admin (all), admin and teacher (the ones they created) | — | `200 UserSummary[]` |
| `POST /api/users/{id}/password-link` | whoever manages that user (see below) | — | `204`; sends a new link: to create the password if the account has none yet, otherwise to reset it |
| `PATCH /api/users/{id}` | whoever manages that user (see below) | `{ "active": false }` | `200 UserSummary`; deactivating also closes their sessions. `422` when deactivating your own account |
| `GET /api/evaluator-types` | authenticated | — | `200 [ { "id", "name", "description", "active" } ]` |

**Who manages a user** (password link and activation): the super_admin, anyone; an
admin, the teachers and students of their faculty and the external evaluators created
by them or by users of their faculty; a teacher, the students and external evaluators
they created. Out of scope answers `403`.

**Registration codes** (teacher, or admin/super_admin on behalf of a teacher):

```jsonc
// RegistrationCode
{
  "id": 3,
  "code": "K7MQ4TZP",              // 8 characters, no 0/O or 1/I
  "programId": 1,
  "programName": "Ingeniería de Sistemas",
  "teacherId": 12,
  "teacherName": "Marta Díaz",
  "expiresOn": "2026-10-15",
  "active": true,
  "registeredCount": 27            // students who registered with it
}
```

| Method and route | Body | Answer |
|---|---|---|
| `POST /api/registration-codes` | `{ "programId", "expiresOn", "teacherId" }` — `teacherId` is omitted by a teacher (the code is theirs) and required for an admin or the super_admin | `201 RegistrationCode`. `422` if the program is not of the teacher's faculty or `expiresOn` is in the past |
| `GET /api/registration-codes` | — | `200 RegistrationCode[]`, newest first: a teacher's own codes, the codes of the faculty for an admin, all for the super_admin |
| `PATCH /api/registration-codes/{id}` | `{ "active": false }` | `200 RegistrationCode`; the code stops working immediately. A deactivated code cannot be reactivated (`422`): generate a new one |

## Stage 3 — Academic configuration — D2 (backend) and Vero (admin and super_admin screens)

Rules come from `docs/04` (flow of a moment, cross-cutting configuration rules) and
ADR-010, ADR-012 and ADR-016. The `admin` and `super_admin` screens are built against
this section; the read routes of moments are in the "Moments" section below.

**Who can do what:**

| Resource | Read | Write |
|---|---|---|
| Faculties and programs | super_admin (all); admin and teacher (their faculty) | super_admin |
| Academic periods | any authenticated user | super_admin |
| Evaluator types | any authenticated user | super_admin |
| Global parameters | super_admin | super_admin |
| Moments, weights, RAP, criteria and levels | as `GET /api/moments` | super_admin, and the admin of the faculty of the moment's program |

**Common rules:**

- **Out of scope** (an admin on a program of another faculty, any non super_admin on a
  catalog) answers `403`.
- **Locked configuration** (ADR-012): every write on a moment, its weights, RAP,
  criteria or levels answers `422` "El momento ya fue lanzado: su configuración no se
  puede modificar." once the moment is `LAUNCHED`.
- **Duplicates** (a code, a name, a moment number, a RAP code) answer `409` with a
  message that says which value, e.g. "Ya existe una facultad con el código 04."
- **Deleting** (ADR-016): `204` when nothing hangs from the resource; `409` with a
  message in Spanish when the database blocks it (groups, grades, accounts or moments).
  Configuration without groups or grades is deleted in cascade
  (faculty → program → moment → RAP → criteria → levels).
- **`PATCH`** receives only the fields to change; the ones that are omitted keep their
  value. The answer is the whole resource.
- **After any change to a moment** (data, weights, RAP, criteria, levels) the screen
  reads `GET /api/moments/{id}` again: it is the single source of the configuration.
- Texts have a maximum length: code 20, faculty and program name 150, evaluator type name
  100, moment name 100, subject 150, requirement level 100, RAP description 1000, criterion
  description 500, level name 50, level description 1000. Longer ones answer `400`.

### Catalogs: faculties, programs, periods, evaluator types and parameters

```jsonc
// Faculty
{ "id": 1, "code": "04", "name": "Facultad de Ingenierías y Tecnologías", "programCount": 2 }

// Program
{
  "id": 1, "code": "108", "name": "Ingeniería de Sistemas",
  "facultyId": 1, "facultyName": "Facultad de Ingenierías y Tecnologías",
  "currentPeriodMomentCount": 3,   // moments in the current period
  "studentCount": 120
}

// AcademicPeriod
{
  "id": 1, "code": "2026-2",
  "startDate": "2026-08-03", "endDate": "2026-12-05",
  "upcEvaluationDeadline": "2026-11-28",
  "status": "CURRENT"              // UPCOMING | CURRENT | CLOSED, computed with the Clock
}

// EvaluatorType (the existing one, plus evaluatorCount)
{ "id": 1, "name": "Empresario", "description": "...", "active": true, "evaluatorCount": 4 }

// GlobalParameter
{ "key": "example_key", "value": "10", "description": "..." }
```

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `GET /api/faculties` | super_admin (all), admin and teacher (their own) | — | `200 Faculty[]` by name |
| `POST /api/faculties` | super_admin | `{ "code", "name" }` | `201 Faculty`. `409` if the code is in use |
| `PATCH /api/faculties/{id}` | super_admin | any of `{ "code", "name" }` | `200 Faculty`. `409` if the code is in use |
| `DELETE /api/faculties/{id}` | super_admin | — | `204`; `409` while its programs have groups or grades, or it has accounts |
| `GET /api/programs?facultyId=` | super_admin (all; `facultyId` optional), admin and teacher (their faculty; another `facultyId` answers `403`) | — | `200 Program[]` by name |
| `POST /api/programs` | super_admin | `{ "facultyId", "code", "name" }` | `201 Program`. `409` if the code is in use |
| `PATCH /api/programs/{id}` | super_admin | any of `{ "code", "name" }`. The faculty of a program cannot be changed | `200 Program`. `409` if the code is in use |
| `DELETE /api/programs/{id}` | super_admin | — | `204`; `409` while it has groups, grades or students |
| `GET /api/periods` | any authenticated user | — | `200 AcademicPeriod[]`, newest first |
| `POST /api/periods` | super_admin | `{ "code", "startDate", "endDate", "upcEvaluationDeadline" }` | `201 AcademicPeriod`. `422` if the end is before the start or the UPC deadline is not between them (ADR-012, `docs/04`). `409` if the code is in use |
| `PATCH /api/periods/{id}` | super_admin | any of the four fields | `200 AcademicPeriod`. `422` for the same rules, and if the new UPC deadline is earlier than the grading end of a moment of the period |
| `DELETE /api/periods/{id}` | super_admin | — | `204`; `409` while it has moments |
| `POST /api/evaluator-types` | super_admin | `{ "name", "description" }` | `201 EvaluatorType`. `409` if the name is in use |
| `PATCH /api/evaluator-types/{id}` | super_admin | any of `{ "name", "description", "active" }` | `200 EvaluatorType`. Deactivating a type does not touch its evaluators or its weights in moments |
| `DELETE /api/evaluator-types/{id}` | super_admin | — | `204`; `409` while an evaluator or a moment weight uses it (deactivate it instead) |
| `GET /api/global-parameters` | super_admin | — | `200 GlobalParameter[]` by key |
| `PUT /api/global-parameters/{key}` | super_admin | `{ "value", "description" }` (`description` optional) | `200 GlobalParameter`. Creates the parameter if the key does not exist; otherwise it changes the value and, only if sent, the description |

`GET /api/evaluator-types` keeps its route and who can call it; it only adds
`evaluatorCount`. Today no rule of the system reads the global parameters (the table is
empty): each parameter that a rule starts to use is added with its own contract.

### Moments: create, edit, delete and weights

Fields that Stage 3 adds to the read DTOs of the "Moments" section below (existing ones do not change, so D3's
screens keep working):

```jsonc
// MomentSummary adds:
{ "requirementLevel": "Bajo", "groupCount": 6 }

// MomentDetail adds (the weights, and what the configuration screens need):
{
  "teacherWeight": 60.00,           // percentage; 100 in a new moment
  "evaluatorWeights": [
    { "evaluatorTypeId": 1, "evaluatorTypeName": "Empresario", "weight": 40.00 }
  ],
  "totalWeight": 100.00,            // teacher + evaluator types; must be 100 to launch
  // and each criterion adds "displayOrder" and "maxScore" (its highest level score),
  // each level adds "displayOrder"
}
```

```jsonc
// NewMoment
{
  "programId": 1, "periodId": 1, "number": 1,
  "name": "Momento 1", "subject": "Programación II (SS300)",
  "semesterFrom": 1, "semesterTo": 3,
  "requirementLevel": "Bajo",       // free text (ADR-010)
  "minMembers": 3, "maxMembers": 5,
  // optional, null or omitted while configuring (each window is complete or empty):
  "groupFormationStart": "2026-08-10", "groupFormationEnd": "2026-08-24",
  "submissionStart": "2026-10-01",     "submissionEnd": "2026-11-20",
  "gradingStart": "2026-11-21",        "gradingEnd": "2026-11-28"
}

// MomentWeights (body of PUT and its answer)
{
  "teacherWeight": 60.00,
  "evaluatorWeights": [ { "evaluatorTypeId": 1, "weight": 40.00 } ]
  // the answer also has "evaluatorTypeName" in each item and "totalWeight"
}
```

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `POST /api/moments` | super_admin, admin of the program's faculty | `NewMoment` | `201 MomentDetail`: in `CONFIGURATION`, teacher weight 100, no weights of evaluator types and no RAP. `409` if the program already has that number in the period |
| `PATCH /api/moments/{id}` | same | any of the fields of `NewMoment` except `programId`. A window is changed by sending both of its dates (or both `null` to clear it) | `200 MomentDetail`. `409` if the new number or period collides with another moment of the program |
| `DELETE /api/moments/{id}` | same | — | `204`, with its RAP, criteria, levels and weights. `422` if it was launched; `409` if it has groups |
| `PUT /api/moments/{id}/weights` | same | `MomentWeights`: **replaces** every weight of the moment | `200 MomentWeights` |

**Validations of the moment** (`422` with a message in Spanish, `400` when a value is out
of its type or format):

- `number`, `semesterFrom` and `minMembers` are at least 1; `semesterFrom` is not greater
  than `semesterTo`; `minMembers` is not greater than `maxMembers`.
- The program belongs to the admin's faculty (`403` otherwise); the program and the period
  exist (`404` otherwise).
- **Windows** (ADR-012): a window has both dates or none (`422` "La ventana de entregas
  debe tener fecha de inicio y de fin."); in each one the start is not after the end; the
  end of group formation is before the start of submissions and the end of submissions is
  before the start of grading (only checked between the windows that are set); the grading
  end is not after the `upcEvaluationDeadline` of the period. Every rule is checked again
  when launching.
- **Weights:** each one is greater than 0 and up to 100, with at most 2 decimals; a type
  appears once; the teacher weight is required; the types exist (`404`). They **do not
  have to add up to 100 while configuring**: that is checked when launching.

**An evaluator type without weight** in the moment (or whose weight was removed) is not an
error: its evaluators' evaluations do not count in the grade, and the screen that assigns
evaluators only warns about it (decision of 2026-10-16, `docs/04`). Assigning evaluators
does not check the weights; the only weight validation is the launch one.

### RAP, criteria and levels

Only while the moment is in `CONFIGURATION`. The shapes are the ones of
`MomentDetail` (`learningOutcomes[]`, their `criteria[]` and their `levels[]`).

```jsonc
// NewLearningOutcome
{ "code": "RAP1", "description": "..." }

// NewCriterion
{ "description": "Identificación del problema", "displayOrder": 1 }  // displayOrder optional: goes last

// NewLevel
{ "name": "Alto", "description": "...", "score": 0.48, "displayOrder": 3 }  // description and displayOrder optional
```

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `POST /api/moments/{id}/learning-outcomes` | super_admin, admin of the moment's faculty | `NewLearningOutcome` | `201 LearningOutcome` (with `criteria: []`). `409` if the code is in use in the moment |
| `PATCH /api/learning-outcomes/{id}` | same | any of `{ "code", "description" }` | `200 LearningOutcome`. `409` if the code is in use in the moment |
| `DELETE /api/learning-outcomes/{id}` | same | — | `204`, with its criteria and levels |
| `POST /api/learning-outcomes/{id}/criteria` | same | `NewCriterion` | `201 Criterion` (with `levels: []`) |
| `PATCH /api/criteria/{id}` | same | any of `{ "description", "displayOrder" }` | `200 Criterion` |
| `DELETE /api/criteria/{id}` | same | — | `204`, with its levels |
| `POST /api/criteria/{id}/levels` | same | `NewLevel` | `201 Level` |
| `PATCH /api/levels/{id}` | same | any of `{ "name", "description", "score", "displayOrder" }` | `200 Level` |
| `DELETE /api/levels/{id}` | same | — | `204` |

A `score` is at least 0 with at most 2 decimals (`BigDecimal`, ADR-013). The maximum score
of a criterion is its highest level, the one of a RAP is the sum of its criteria and the
one of the moment the sum of its RAP; nothing forces the total to be 5.0, because the
scoring is configurable (`docs/04`). No fixed names or counts of levels exist.

### Launch

```jsonc
// LaunchCheck: what the screen shows before the admin launches
{
  "momentId": 1,
  "canLaunch": false,               // no problems and the moment is still in CONFIGURATION
  "problems": [ "Para lanzar el momento se deben definir las tres ventanas de fechas." ],
  "minMembers": 3,
  "groupsBelowMinimum": [
    { "id": 7, "name": "Grupo 3", "teacher": { "id": 12, "fullName": "Marta Díaz" }, "memberCount": 2 }
  ],
  "evaluatorTypesWithoutWeight": [  // assigned in its groups but with no weight: they will not count
    { "evaluatorTypeId": 2, "evaluatorTypeName": "Egresado", "evaluatorCount": 3 }
  ]
}
```

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `GET /api/moments/{id}/launch-check` | super_admin, admin of the moment's faculty | — | `200 LaunchCheck`. It changes nothing. `problems` lists **every** blocker at once, not only the first; if the moment was already launched, `canLaunch` is `false` with "El momento ya fue lanzado." |
| `POST /api/moments/{id}/launch` | same | `{ "confirmUnderMinimum": true }`; the body is optional and the field defaults to `false` | `200 MomentDetail` with `status: LAUNCHED`. `422` with the first blocker if the configuration is incomplete or invalid. `422` "Hay 2 grupos por debajo del mínimo de 3 integrantes (Grupo 3, Grupo 5). Confirme para lanzar de todos modos." if there are such groups and the confirmation was not sent; nothing changes |

**Blockers** (the rules of `Moment.launch()`, ADR-012 and ADR-018): the three windows are
complete, each with its start not after its end, ordered without overlapping, and the
grading one ends no later than the UPC deadline; there is at least one RAP, every RAP has
a criterion and every criterion has levels; and the teacher and evaluator type weights add
up to exactly 100 %.

**Not blockers:** groups below the minimum (they ask for confirmation and stay as they
are, `docs/04` 2026-09-25), and evaluator types without weight (only a warning). The admin
can launch before the group formation window ends: launching closes group formation
(`docs/04`, 2026-09-27). After the launch, groups and configuration are locked.

### Copy the configuration of a period

```jsonc
// CopyMomentsRequest
{ "fromPeriodId": 1, "toPeriodId": 2, "programId": 1 }   // programId optional
```

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `POST /api/moments/copy` | super_admin (any program), admin (programs of their faculty) | `CopyMomentsRequest` | `201 MomentSummary[]`: the new moments, in the destination period, ordered by program and number |

**What it copies** (ADR-010): every moment of the source period in the programs in scope
(`programId`, or all the programs of the admin's faculty, or all of them for the
super_admin), with its number, name, subject, semester range, requirement level,
members per group, RAP, criteria, levels (with their scores and order) and weights.

**What it does not copy:** the date windows (those of the previous semester are not valid
in the new one, so the admin sets them again), the groups, submissions and grades. The new
moments start in `CONFIGURATION`. The source period is not touched.

**Errors:** `422` if the periods are the same, if the source has no moment in scope, or if
the destination already has a moment with the same program and number: the message lists
those collisions and **nothing is copied** (all or nothing). `404` if a period or the
program does not exist; `403` if `programId` is not of the admin's faculty.

## Stages 4 to 6 — Groups, submissions and evaluation

Rules come from `docs/04` (flow of a moment and team decisions) and ADR-012, ADR-015 and
ADR-018. The technical details D1 proposed in this contract (what students see of other
groups, switching groups in one step, when groups, members and evaluators can be
removed, date limits, the exact evaluator lookup, anonymous comments, the current
period) were approved by the lead on 2026-09-28.

**Windows.** "Today" is always the date of the injected `Clock` (`America/Bogota`). A
window is open from its start date to its end date, both included.

| Action | Allowed when |
|---|---|
| Enable, rename or delete a group | The moment is in `CONFIGURATION` (before the launch) |
| Join, leave or switch groups (student) | The moment is in `CONFIGURATION` and today is within the group formation window |
| Change the group dates, assign or remove evaluators (teacher) | Before the launch, or after it until the grading window closes |
| Add a student without a group (teacher) | Any time until the grading window closes, even after the launch and above the maximum |
| Edit the project title (members or teacher) | Until the submission window ends |
| Upload a submission | The moment is `LAUNCHED` and today is within the submission window |
| Grade (draft, submit, correct, individual adjustments) | The moment is `LAUNCHED` and today is between the group's `evaluationDate` and the end of the grading window |
| Student sees their results | After the grading window ends |

Outside those periods the API answers `422` with a message that says which window
applies (e.g. "La ventana de entregas cerró el 2026-11-20.").

### Moments (read only) — implemented by D2 at the start of Stage 3

The operations screens need to list moments. The configuration routes (create, edit,
delete, weights, RAP, launch and copy from the previous period) are in the "Stage 3 —
Academic configuration" section below.

```jsonc
// MomentSummary
{
  "id": 1,
  "number": 1,
  "name": "Momento 1",
  "subject": "Programación II (SS300)",
  "programId": 1,
  "programName": "Ingeniería de Sistemas",
  "periodId": 1,
  "periodCode": "2026-2",
  "semesterFrom": 1,
  "semesterTo": 3,
  "minMembers": 3,
  "maxMembers": 5,
  "status": "CONFIGURATION",       // CONFIGURATION | LAUNCHED
  "groupFormationStart": "2026-08-10", "groupFormationEnd": "2026-08-24",
  "submissionStart": "2026-10-01",     "submissionEnd": "2026-11-20",
  "gradingStart": "2026-11-21",        "gradingEnd": "2026-11-28",
  "groupFormationOpen": true,      // computed with the Clock, per the table above
  "submissionOpen": false,
  "gradingOpen": false
}

// MomentDetail = MomentSummary + the assessment structure
{
  /* ...MomentSummary... */
  "maxScore": 5.0,
  "learningOutcomes": [
    {
      "id": 10, "code": "RAP1", "description": "...", "maxScore": 0.6,
      "criteria": [
        {
          "id": 20, "description": "Identificación del problema",
          "levels": [ { "id": 30, "name": "Bajo", "description": "...", "score": 0 } ]
        }
      ]
    }
  ]
}
```

| Method and route | Who | Answer |
|---|---|---|
| `GET /api/moments?periodId=` | super_admin (all), admin and teacher (programs of their faculty), student (their program, the moments whose semester range includes their current semester, and any moment where they already have a group, since a teacher can add them out of range) | `200 MomentSummary[]`. Without `periodId`, the current period: the one whose dates include today |
| `GET /api/moments/{id}` | same scope | `200 MomentDetail`. `403` out of scope |

### Groups (Stage 4 teacher, Stage 5 student) — D3

```jsonc
// Group
{
  "id": 7,
  "momentId": 1,
  "momentName": "Momento 1",
  "name": "Grupo 3",
  "projectTitle": "SIERAP",        // null until the members write it
  "teacher": { "id": 12, "fullName": "Marta Díaz" },
  "memberCount": 4,
  "minMembers": 3,
  "maxMembers": 5,
  "full": false,
  "members": [ { "id": 40, "fullName": "Juan Rojas", "studentCode": "20261001", "email": "juan@unicesar.edu.co" } ],
  "submissionDeadline": "2026-11-15",   // null until the teacher sets it
  "evaluationDate": "2026-11-24",       // null until the teacher sets it
  "evaluators": [ { "id": 51, "fullName": "Sofía Vega", "evaluatorType": "Empresario",
                    "countsInGrade": true } ],  // false if their type has no weight in the moment
  "myGroup": false                      // true if the caller is a member
}
```

Students see only `fullName` of the members of other groups (to know who is in each
one): `studentCode` and `email` come as `null` and `evaluators` as `[]`; in their own
group they see everything except `evaluators` (also `[]`). A student can see the groups
of the moments of their program whose semester range includes their current semester,
and of any moment where they already have a group (a teacher can add them out of range).

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `GET /api/groups?momentId=` | teacher (their groups; `momentId` optional), external evaluator (the groups assigned to them; `momentId` optional), student (all the groups of an eligible moment; `momentId` required), admin (their faculty; required), super_admin (required) | — | `200 Group[]` ordered by name |
| `GET /api/groups/{id}` | the group's teacher, its members, its assigned evaluators, students of an eligible moment, admin of the faculty, super_admin | — | `200 Group`. `403` out of scope |
| `POST /api/groups` | teacher of the moment's faculty | `{ "momentId", "name" }` | `201 Group`. `422` if the moment was already launched |
| `PATCH /api/groups/{id}` | the group's teacher | any of `{ "name", "submissionDeadline", "evaluationDate" }` | `200 Group`. The name only changes before the launch. `submissionDeadline` must be within the submission window and `evaluationDate` within the grading window; neither can be earlier than today. `422` otherwise |
| `DELETE /api/groups/{id}` | the group's teacher | — | `204`. Only before the launch and while the group has no members; otherwise `422` |
| `PUT /api/groups/{id}/project-title` | the group's members and teacher | `{ "projectTitle" }` (up to 200 characters) | `200 Group`. `422` after the submission window ends |
| `POST /api/groups/{id}/join` | student of an eligible moment | — | `200 Group`. If they were in another group of the same moment, they leave it in the same step (the screen asks for confirmation). `422` if the group is full or the group formation window is not open |
| `POST /api/groups/{id}/leave` | a member | — | `204`. `422` if the group formation window is not open |
| `GET /api/moments/{id}/students-without-group` | teacher and admin of the faculty | — | `200 [ { "id", "fullName", "studentCode", "currentSemester" } ]`: students of the program with their semester in range and no group in the moment |
| `POST /api/groups/{id}/members` | the group's teacher | `{ "studentId" }` | `200 Group`. Any student of the moment's program who has no group in the moment, even outside the semester range, after the launch and above the maximum. `422` if they already have a group |
| `DELETE /api/groups/{id}/members/{studentId}` | the group's teacher | — | `204`. While the group has no submitted evaluation; otherwise `422` |
| `GET /api/students/me/groups` | student | — | `200 Group[]`: their groups, one per moment, newest first (history) |

**External evaluators of a group** (Stage 4):

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `GET /api/external-evaluators/lookup?q=` | teacher, admin, super_admin | — | `200 UserSummary` of the active external evaluator whose email or document number is **exactly** `q`; `404` if there is none. It does not list or search partially, so evaluator accounts cannot be browsed |
| `POST /api/groups/{id}/evaluators` | the group's teacher | `{ "evaluatorId" }` | `200 Group`. `422` if the evaluator is deactivated or already assigned. An evaluator whose type has no weight in the moment (or 0 %) is still assigned, with `countsInGrade: false`; the screen warns that their evaluation will not count (2026-10-16) |
| `DELETE /api/groups/{id}/evaluators/{evaluatorId}` | the group's teacher | — | `204`. `422` if that evaluator already has an evaluation (draft or submitted) of the group |

Creating an evaluator is the existing `POST /api/external-evaluators` (Stage 2).

### Submissions (Stage 5) — D3

```jsonc
// Submission
{
  "id": 90,
  "groupId": 7,
  "versionNumber": 3,
  "submittedAt": "2026-11-14T22:41:05",
  "late": false,
  "zipFileName": "sierap-v3.zip",           // original name, only for display and download
  "zipSize": 5242880,                       // bytes
  "presentationFileName": "sustentacion.pdf", // null if this version has none
  "presentationSize": 1048576,
  "toGrade": true                           // the version the evaluators grade
}
```

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `POST /api/groups/{id}/submissions` | a member | `multipart/form-data`: `zip` (required, `.zip`, up to 100 MB) and `presentation` (optional, `.pdf`, `.docx` or `.pptx`, up to 30 MB) | `201 Submission`, a new version (never overwrites). `late` is `true` if today is after the group's `submissionDeadline` (or, if the teacher did not set one, after the end of the window). `400` wrong type, `413` too large, `422` window closed |
| `GET /api/groups/{id}/submissions` | members, the group's teacher, its assigned evaluators, admin of the faculty, super_admin | — | `200 { "versions": Submission[] (newest first), "currentPresentation": Submission \| null }`. `currentPresentation` is the newest version that has a presentation |
| `GET /api/submissions/{id}/zip` | same | — | `200` the file (`Content-Disposition: attachment`) |
| `GET /api/submissions/{id}/presentation` | same | — | `200` the file; `404` if that version has none |

The version to grade is the last on-time one or, if there is none, the last late one
(`docs/04`).

### Evaluation (Stage 6) — D2 + D3

Each evaluator (the group's teacher or an assigned external evaluator) has **one**
evaluation per group. They grade the group once and may adjust single students
(ADR-018).

```jsonc
// EvaluationForm: the caller's own evaluation of a group, or an empty one to start
{
  "id": 15,                         // null if not started yet
  "groupId": 7,
  "status": "DRAFT",                // DRAFT | SUBMITTED | null (not started)
  "evaluatedAt": "2026-11-24T10:15:00",
  "comment": "Buen trabajo en equipo.",
  "canEdit": true,                  // the grading period of the group is open
  "submissionToGrade": { "id": 90, "versionNumber": 3, "late": false },  // null if nothing was submitted
  "learningOutcomes": [
    {
      "id": 10, "code": "RAP1", "description": "...", "maxScore": 0.6,
      "criteria": [
        {
          "id": 20, "description": "Identificación del problema",
          "levels": [ { "id": 30, "name": "Bajo", "description": "...", "score": 0 } ],
          "levelId": 33               // group level picked; null if not graded yet
        }
      ]
    }
  ],
  "members": [
    {
      "id": 40, "fullName": "Juan Rojas",
      "adjustments": [ { "criterionId": 20, "levelId": 32 } ],  // only criteria that differ from the group
      "totalScore": 4.40
    }
  ],
  "totalScore": 4.52,               // group levels only
  "maxScore": 5.0,
  "complete": false                 // every criterion has a group level
}
```

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `GET /api/groups/{id}/evaluation` | the group's teacher and its assigned evaluators | — | `200 EvaluationForm` (their own) |
| `PUT /api/groups/{id}/evaluation` | same, within the grading period of the group | `{ "levels": [ { "criterionId", "levelId" } ], "comment", "adjustments": [ { "studentId", "criterionId", "levelId" } ] }` | `200 EvaluationForm`. Creates the evaluation as a draft the first time (a draft can be partial). `levels` sets the level of each criterion sent; the others keep theirs. `adjustments`, when sent, **replaces** all the previous ones (omit it to keep them); one equal to the group's level is dropped. If it was already submitted, it stays submitted, so it must stay complete (`422` otherwise). `422` if a level does not belong to its criterion or a student is not a member |
| `POST /api/groups/{id}/evaluation/submit` | same | — | `200 EvaluationForm` with `status: SUBMITTED`. `422` "Falta calificar N criterios." if it is incomplete |
| `GET /api/groups/{id}/evaluations` | the group's teacher, admin of the faculty, super_admin | — | `200 [ { "id", "evaluator": { "id", "fullName", "kind" }, "status", "evaluatedAt", "totalScore", "comment" } ]`. `kind` is "Docente" or the evaluator type name. External evaluators only see their own evaluation |

### Results (Stage 6) — D2 + D3

```jsonc
// GroupResults: group grade and each member's individual grade
{
  "groupId": 7,
  "final": false,                   // true once the grading window ended
  "maxScore": 5.0,
  "groupGrade": 4.52,               // null while there is no submitted evaluation
  "groupLearningOutcomes": [ { "id": 10, "code": "RAP1", "score": 0.6, "maxScore": 0.6 } ],
  "members": [
    {
      "studentId": 40, "fullName": "Juan Rojas",
      "individualGrade": 4.40,
      "learningOutcomes": [ { "id": 10, "code": "RAP1", "score": 0.6, "maxScore": 0.6 } ]
    }
  ],
  "submittedEvaluations": 2,
  "pendingEvaluations": 1           // assigned evaluators (and the teacher) who have not submitted
}

// StudentResult: what a student sees, one per moment
{
  "momentId": 1, "momentName": "Momento 1", "periodCode": "2026-2",
  "groupId": 7, "groupName": "Grupo 3", "projectTitle": "SIERAP",
  "available": true,                // false until the grading window ends
  "finalGrade": 4.40,               // their individual grade; null while not available
  "groupGrade": 4.52,
  "maxScore": 5.0,
  "learningOutcomes": [ { "code": "RAP1", "description": "...", "score": 0.6, "maxScore": 0.6 } ],
  "comments": [ { "evaluatorKind": "Empresario", "comment": "Buen trabajo en equipo." } ]
}
```

| Method and route | Who | Answer |
|---|---|---|
| `GET /api/groups/{id}/results` | the group's teacher, admin of the faculty, super_admin | `200 GroupResults`, at any time (while the window is open it is a preview with the submitted evaluations so far) |
| `GET /api/students/me/results` | student | `200 StudentResult[]`, newest first. Before the grading window ends, `available` is `false` and grades and comments are `null`/empty. Comments show the kind of evaluator, not their name, and never the levels each evaluator picked |

**Formula** (`docs/03`, ADR-018): only submitted evaluations count. For each kind of
evaluator (the teacher, and each evaluator type), average the scores of its evaluators;
then weight the kinds with the weights the admin set in the moment (Stage 3),
redistributing the weight of a kind without submitted evaluations among the others in
proportion. The group grade uses the group levels; each individual grade uses the group
levels replaced by that student's adjustments. Per RAP, the same with the scores of that
RAP's criteria. Every grade is `BigDecimal` (ADR-013) with 2 decimals in the answer.

## Stage 7 — Reporting (D2, with D3 on charts)

Reports (RF 12) are SIERAP's main differentiator compared with AULAWEB. In Moment 1 they
are rendered on screen with charts and tables; exporting to PDF or Excel is deferred to
Moment 2 (`docs/04`, `docs/06`).

### Polymorphic domain model (`Report` and `generate()`)

The `Report` hierarchy in the class diagram (`docs/03`, `docs/diagramas/`) is preserved
as an in-memory domain hierarchy (no database table: reports are computed on demand from
the grades of `ProjectGroup`):

- `Report` (abstract): holds `generatedAt: LocalDateTime` (from the injected `Clock`) and
  `period: AcademicPeriod`, and declares `public abstract ReportData generate()`.
- `ReportData`: sealed interface implemented by the four response records below
  (`IndividualReportData`, `GroupReportData`, `MomentReportData`,
  `ComparativeReportData`).
- Subclasses — each overrides `generate()` with a covariant return type and computes its
  figures using `ProjectGroup.getGroupGrade()`, `getGroupScoreFor(outcome)`,
  `getIndividualGrade(student)` and `getIndividualScoreFor(outcome, student)`:
  - `IndividualReport`: performance of a student in a moment, overall and per RAP.
  - `GroupReport`: performance of a group per RAP and each member's individual grade.
  - `MomentReport`: consolidated averages per RAP and grade distribution of a moment.
  - `ComparativeReport`: semester X vs. semester Y for the same program and moment
    number across two academic periods.

### Aggregate-only rule for comparative reports (`docs/04`)

Comparative reports (`ComparativeReport`) are handled **strictly at an aggregate level,
without student names, document numbers, student codes or individual rows**. Comparing
two cohorts across semesters serves curricular analysis and accreditation (evaluating how
each RAP evolved from one period to another), not auditing individual students from past
periods. Individual performance belongs exclusively to `IndividualReport` and
`GroupReport`.

### Who can read which report

| Report | `super_admin` | `admin` | `teacher` | `student` |
|---|---|---|---|---|
| Individual (`GET /api/reports/individual`) | any | moments of their faculty | students of the groups they enabled | only their own (`studentId` omitted or their own id), after the grading window ends |
| Group (`GET /api/reports/groups/{groupId}`) | any | groups of their faculty | the groups they enabled | — |
| Consolidated per moment (`GET /api/reports/moments/{momentId}`) | any | moments of their faculty | moments of their faculty | — |
| Comparative (`GET /api/reports/comparative`) | any | programs of their faculty | programs of their faculty | — |

Out of scope answers `403`.

### 1. Individual report — performance of a student per RAP

```jsonc
// IndividualReportData
{
  "generatedAt": "2026-11-29T10:30:00",
  "period": { "id": 2, "code": "2026-2" },
  "program": { "id": 1, "code": "108", "name": "Ingeniería de Sistemas" },
  "moment": {
    "id": 1, "momentNumber": 1, "name": "Momento 1",
    "subject": "Programación II", "maxScore": 5.00
  },
  "student": {
    "id": 40, "studentCode": "20241001",
    "fullName": "Juan Rojas", "currentSemester": 4
  },
  "group": {
    "id": 7, "name": "Grupo 3",
    "projectTitle": "SIERAP", "teacherName": "Marta Díaz"
  },
  "final": true,                    // true once the grading window ended
  "individualGrade": 4.40,          // null while the group has no countable submitted evaluation
  "groupGrade": 4.52,
  "percentage": 88.00,              // individualGrade / maxScore * 100 (null if individualGrade is null)
  "learningOutcomes": [
    {
      "id": 10, "code": "RAP1", "description": "...",
      "score": 0.48,                // student's individual score in this RAP
      "groupScore": 0.60,           // group score in this RAP before individual adjustments
      "maxScore": 0.60,
      "percentage": 80.00           // score / maxScore * 100, rounded half up to 2 decimals
    }
  ],
  "submittedEvaluations": 2,
  "pendingEvaluations": 0
}
```

### 2. Group report

```jsonc
// GroupReportData
{
  "generatedAt": "2026-11-29T10:30:00",
  "period": { "id": 2, "code": "2026-2" },
  "program": { "id": 1, "code": "108", "name": "Ingeniería de Sistemas" },
  "moment": {
    "id": 1, "momentNumber": 1, "name": "Momento 1",
    "subject": "Programación II", "maxScore": 5.00
  },
  "group": {
    "id": 7, "name": "Grupo 3",
    "projectTitle": "SIERAP", "teacherName": "Marta Díaz"
  },
  "final": true,
  "groupGrade": 4.52,               // null while there is no countable submitted evaluation
  "percentage": 90.40,              // groupGrade / maxScore * 100
  "learningOutcomes": [
    {
      "id": 10, "code": "RAP1", "description": "...",
      "score": 0.60, "maxScore": 0.60, "percentage": 100.00
    }
  ],
  "members": [
    {
      "studentId": 40, "studentCode": "20241001", "fullName": "Juan Rojas",
      "individualGrade": 4.40,
      "adjusted": true,             // true when individualGrade or any RAP score differs from the group's
      "learningOutcomes": [
        { "id": 10, "code": "RAP1", "score": 0.48, "maxScore": 0.60, "percentage": 80.00 }
      ]
    }
  ],
  "submittedEvaluations": 2,
  "pendingEvaluations": 0
}
```

### 3. Consolidated report per moment and program

Aggregates all groups and students of a moment. Only groups with a computed grade
(`getGroupGrade().isPresent()`, i.e. at least one countable submitted evaluation) enter
the averages and the grade distribution; ungraded groups and students are reflected in
`totalGroups` vs. `gradedGroups` and `totalStudents` vs. `gradedStudents`. Student-level
metrics (`averageGrade`, `minGrade`, `maxGrade`, `passingCount`, `passingPercentage`, per-RAP
averages and `distribution`) use each member's **individual grade** (`getIndividualGrade(student)`
and `getIndividualScoreFor(outcome, student)`), which is their final grade (ADR-018).

```jsonc
// MomentReportData
{
  "generatedAt": "2026-11-29T10:30:00",
  "period": { "id": 2, "code": "2026-2" },
  "program": { "id": 1, "code": "108", "name": "Ingeniería de Sistemas" },
  "moment": {
    "id": 1, "momentNumber": 1, "name": "Momento 1",
    "subject": "Programación II", "maxScore": 5.00
  },
  "final": true,
  "totalGroups": 6,
  "gradedGroups": 5,
  "totalStudents": 24,
  "gradedStudents": 20,
  "averageGroupGrade": 4.18,        // null when gradedGroups is 0
  "averageGrade": 4.12,             // average of graded students' individual grades; null when gradedStudents is 0
  "minGrade": 2.65,
  "maxGrade": 4.85,
  "passingCount": 18,               // graded students with individualGrade >= 3.00
  "passingPercentage": 90.00,       // passingCount / gradedStudents * 100 (null when gradedStudents is 0)
  "learningOutcomes": [
    {
      "id": 10, "code": "RAP1", "description": "...",
      "averageScore": 0.47,         // average of getIndividualScoreFor(outcome, student) across graded students
      "maxScore": 0.60,
      "percentage": 78.33           // averageScore / maxScore * 100
    }
  ],
  "distribution": [                 // always 4 buckets on the 0.00–5.00 scale, matching the UPC rubric levels
    { "range": "0.00 - 2.99", "label": "Bajo",          "min": 0.00, "max": 2.99, "count": 2, "percentage": 10.00 },
    { "range": "3.00 - 3.79", "label": "Básico",        "min": 3.00, "max": 3.79, "count": 6, "percentage": 30.00 },
    { "range": "3.80 - 4.49", "label": "Alto",          "min": 3.80, "max": 4.49, "count": 8, "percentage": 40.00 },
    { "range": "4.50 - 5.00", "label": "Sobresaliente", "min": 4.50, "max": 5.00, "count": 4, "percentage": 20.00 }
  ]
}
```

### 4. Comparative report — semester X vs. semester Y

Compares two academic periods (`periodA` as baseline and `periodB` as target) for the
**same program and moment number** (`momentNumber`). Aggregate only (no student names or
individual rows). RAPs are paired by their `code` (case-insensitive, ordered by appearance
in `periodB` and then any RAP present only in `periodA`); if a RAP exists in only one of
the two periods, the other period's fields (`averageScore`, `maxScore`, `percentage`) and
`deltaPercentage` are `null`. Because `maxScore` can change between periods (ADR-010), the
comparison across periods is expressed as achievement percentage (`0–100 %`) and its
change in percentage points (`deltaPercentage = percentageB - percentageA`, as shown in
wireframe `AD-08`).

```jsonc
// ComparativeReportData
{
  "generatedAt": "2026-11-29T10:30:00",
  "program": { "id": 1, "code": "108", "name": "Ingeniería de Sistemas" },
  "momentNumber": 1,
  "periodA": {
    "period": { "id": 1, "code": "2026-1" },
    "momentId": 4, "momentName": "Momento 1", "subject": "Programación II", "maxScore": 5.00,
    "gradedGroups": 5, "gradedStudents": 19,
    "averageGrade": 3.78, "passingPercentage": 84.21,
    "distribution": [
      { "range": "0.00 - 2.99", "label": "Bajo",          "min": 0.00, "max": 2.99, "count": 3, "percentage": 15.79 },
      { "range": "3.00 - 3.79", "label": "Básico",        "min": 3.00, "max": 3.79, "count": 7, "percentage": 36.84 },
      { "range": "3.80 - 4.49", "label": "Alto",          "min": 3.80, "max": 4.49, "count": 6, "percentage": 31.58 },
      { "range": "4.50 - 5.00", "label": "Sobresaliente", "min": 4.50, "max": 5.00, "count": 3, "percentage": 15.79 }
    ]
  },
  "periodB": {
    "period": { "id": 2, "code": "2026-2" },
    "momentId": 1, "momentName": "Momento 1", "subject": "Programación II", "maxScore": 5.00,
    "gradedGroups": 5, "gradedStudents": 20,
    "averageGrade": 4.12, "passingPercentage": 90.00,
    "distribution": [
      { "range": "0.00 - 2.99", "label": "Bajo",          "min": 0.00, "max": 2.99, "count": 2, "percentage": 10.00 },
      { "range": "3.00 - 3.79", "label": "Básico",        "min": 3.00, "max": 3.79, "count": 6, "percentage": 30.00 },
      { "range": "3.80 - 4.49", "label": "Alto",          "min": 3.80, "max": 4.49, "count": 8, "percentage": 40.00 },
      { "range": "4.50 - 5.00", "label": "Sobresaliente", "min": 4.50, "max": 5.00, "count": 4, "percentage": 20.00 }
    ]
  },
  "deltaAverageGrade": 0.34,        // periodB.averageGrade - periodA.averageGrade (null if either is null)
  "deltaPassingPercentage": 5.79,   // periodB.passingPercentage - periodA.passingPercentage
  "learningOutcomes": [
    {
      "code": "RAP1",
      "description": "...",         // description in periodB (or periodA if absent in periodB)
      "averageScoreA": 0.43, "maxScoreA": 0.60, "percentageA": 71.67,
      "averageScoreB": 0.47, "maxScoreB": 0.60, "percentageB": 78.33,
      "deltaPercentage": 6.66       // percentageB - percentageA (null if either percentage is null)
    }
  ]
}
```

### Routes and error rules

| Method and route | Who | Answer |
|---|---|---|
| `GET /api/reports/individual?momentId=&studentId=` | `super_admin`, `admin` of the faculty, `teacher` of the student's group in that moment, or the `student` themselves (`studentId` optional for a student) | `200 IndividualReportData`. `404` if the moment or student does not exist. `422` "El estudiante no pertenece a ningún grupo de este momento." if the student has no group in that moment. `422` "Los resultados de este momento aún no están disponibles." when called by a student before the grading window ends |
| `GET /api/reports/groups/{groupId}` | `super_admin`, `admin` of the faculty, the `teacher` who enabled the group | `200 GroupReportData` (sorted by member `fullName` and RAP `id`). `404` if the group does not exist |
| `GET /api/reports/moments/{momentId}` | `super_admin`, `admin` and `teacher` of the faculty | `200 MomentReportData`. `404` if the moment does not exist |
| `GET /api/reports/comparative?programId=&momentNumber=&periodAId=&periodBId=` | `super_admin`, `admin` and `teacher` of the faculty | `200 ComparativeReportData`. `422` "Debe elegir dos periodos académicos distintos para comparar." if `periodAId = periodBId`. `404` if the program or a period does not exist, or if the program has no moment with `momentNumber` in one of the two periods |

**Rounding rule:** as decided for Stage 6 (`docs/04`, 2026-10-16), intermediate sums and
averages keep 16 significant digits (`Decimal128`) and every decimal returned in a report
(grades, RAP averages and percentages) is a `BigDecimal` rounded half up to 2 decimals at
the end.


