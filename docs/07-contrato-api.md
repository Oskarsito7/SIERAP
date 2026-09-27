# 07 — API contract

> "Contract before code" (`06-plan-de-trabajo.md`): the routes, DTOs and error codes are
> agreed here before implementing them, so the frontend can work against simulated
> data while the backend is being built. Drafted by D1; approved by the project lead.
> Stage 2 (authentication and accounts) is below; groups, submissions and evaluation
> are added when Stage 1 closes.

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

## Later stages

Groups, submissions and evaluation (Stages 4 to 6), and the moment configuration
(Stage 3), are added here when Stage 1 closes.
