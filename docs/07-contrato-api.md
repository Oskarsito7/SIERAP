# 07 — API contract

> "Contract before code" (`06-plan-de-trabajo.md`): the routes, DTOs and error codes are
> agreed here before implementing them, so the frontend can work against simulated
> data while the backend is being built. Drafted by D1; approved by the project lead.
> Stage 2 (authentication and accounts) and Stages 4 to 6 (groups, submissions and
> evaluation) are below. The moment configuration (Stage 3) is written by D2 in their
> PR, on the same `/api/moments` resource.

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

## Stages 4 to 6 — Groups, submissions and evaluation

Rules come from `docs/04` (flow of a moment and team decisions) and ADR-012, ADR-015 and
ADR-018. Items marked *(proposed)* are technical details D1 proposed in this contract;
the lead confirms or changes them when approving it.

**Windows.** "Today" is always the date of the injected `Clock` (`America/Bogota`). A
window is open from its start date to its end date, both included.

| Action | Allowed when |
|---|---|
| Enable, rename or delete a group | The moment is in `CONFIGURATION` (before the launch) |
| Join, leave or switch groups (student) | The moment is in `CONFIGURATION` and today is within the group formation window |
| Change the group dates, assign or remove evaluators (teacher) | Before the launch, or after it until the grading window closes |
| Add a student without a group (teacher) | Any time until the grading window closes *(proposed)*, even after the launch and above the maximum |
| Edit the project title (members or teacher) | Until the submission window ends |
| Upload a submission | The moment is `LAUNCHED` and today is within the submission window |
| Grade (draft, submit, correct, individual adjustments) | The moment is `LAUNCHED` and today is between the group's `evaluationDate` and the end of the grading window |
| Student sees their results | After the grading window ends |

Outside those periods the API answers `422` with a message that says which window
applies (e.g. "La ventana de entregas cerró el 2026-11-20.").

### Moments (read only) — implemented by D2 at the start of Stage 3

The operations screens need to list moments. Stage 3 adds the configuration endpoints
(create, edit, launch, copy from the previous period) on this same resource, with their
own contract written by D2. Until these two routes exist, D3 mocks them.

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
| `GET /api/moments?periodId=` | super_admin (all), admin and teacher (programs of their faculty), student (their program, only moments whose semester range includes their current semester) | `200 MomentSummary[]`. Without `periodId`, the current period: the one whose dates include today *(proposed)* |
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
  "evaluators": [ { "id": 51, "fullName": "Sofía Vega", "evaluatorType": "Empresario" } ],
  "myGroup": false                      // true if the caller is a member
}
```

Students see only `fullName` of the members of other groups (to know who is in each
one), without `studentCode`, `email` or `evaluators` *(proposed)*; in their own group
they see everything except `evaluators`.

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `GET /api/groups?momentId=` | teacher (their groups; `momentId` optional), external evaluator (the groups assigned to them; `momentId` optional), student (all the groups of an eligible moment; `momentId` required), admin (their faculty; required), super_admin (required) | — | `200 Group[]` ordered by name |
| `GET /api/groups/{id}` | the group's teacher, its members, its assigned evaluators, students of an eligible moment, admin of the faculty, super_admin | — | `200 Group`. `403` out of scope |
| `POST /api/groups` | teacher of the moment's faculty | `{ "momentId", "name" }` | `201 Group`. `422` if the moment was already launched |
| `PATCH /api/groups/{id}` | the group's teacher | any of `{ "name", "submissionDeadline", "evaluationDate" }` | `200 Group`. The name only changes before the launch. `submissionDeadline` must be within the submission window and `evaluationDate` within the grading window; neither can be earlier than today *(proposed)*. `422` otherwise |
| `DELETE /api/groups/{id}` | the group's teacher | — | `204`. Only before the launch and while the group has no members *(proposed)*; otherwise `422` |
| `PUT /api/groups/{id}/project-title` | the group's members and teacher | `{ "projectTitle" }` (up to 200 characters) | `200 Group`. `422` after the submission window ends |
| `POST /api/groups/{id}/join` | student of an eligible moment | — | `200 Group`. If they were in another group of the same moment, they leave it in the same step (the screen asks for confirmation) *(proposed)*. `422` if the group is full or the group formation window is not open |
| `POST /api/groups/{id}/leave` | a member | — | `204`. `422` if the group formation window is not open |
| `GET /api/moments/{id}/students-without-group` | teacher and admin of the faculty | — | `200 [ { "id", "fullName", "studentCode", "currentSemester" } ]`: students of the program with their semester in range and no group in the moment |
| `POST /api/groups/{id}/members` | the group's teacher | `{ "studentId" }` | `200 Group`. Any student of the moment's program who has no group in the moment, even outside the semester range, after the launch and above the maximum. `422` if they already have a group |
| `DELETE /api/groups/{id}/members/{studentId}` | the group's teacher | — | `204`. While the group has no submitted evaluation *(proposed)*; otherwise `422` |
| `GET /api/students/me/groups` | student | — | `200 Group[]`: their groups, one per moment, newest first (history) |

**External evaluators of a group** (Stage 4):

| Method and route | Who | Body | Answer |
|---|---|---|---|
| `GET /api/external-evaluators/lookup?q=` | teacher, admin, super_admin | — | `200 UserSummary` of the active external evaluator whose email or document number is **exactly** `q`; `404` if there is none. It does not list or search partially, so evaluator accounts cannot be browsed *(proposed)* |
| `POST /api/groups/{id}/evaluators` | the group's teacher | `{ "evaluatorId" }` | `200 Group`. `422` if the evaluator is deactivated or already assigned |
| `DELETE /api/groups/{id}/evaluators/{evaluatorId}` | the group's teacher | — | `204`. `422` if that evaluator already has an evaluation (draft or submitted) of the group *(proposed)* |

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
| `POST /api/groups/{id}/submissions` | a member | `multipart/form-data`: `zip` (required, `.zip`, up to 100 MB) and `presentation` (optional, `.pdf`, `.docx` or `.pptx`, up to 30 MB) | `201 Submission`, a new version (never overwrites). `late` is `true` if today is after the group's `submissionDeadline` (or, if the teacher did not set one, after the end of the window *(proposed)*). `400` wrong type, `413` too large, `422` window closed |
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
| `GET /api/groups/{id}/evaluations` | the group's teacher, admin of the faculty, super_admin | — | `200 [ { "id", "evaluator": { "id", "fullName", "kind" }, "status", "evaluatedAt", "totalScore", "comment" } ]`. `kind` is "Docente" or the evaluator type name. External evaluators only see their own evaluation *(proposed)* |

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
| `GET /api/students/me/results` | student | `200 StudentResult[]`, newest first. Before the grading window ends, `available` is `false` and grades and comments are `null`/empty. Comments show the kind of evaluator, not their name *(proposed)*, and never the levels each evaluator picked |

**Formula** (`docs/03`, ADR-018): only submitted evaluations count. For each kind of
evaluator (the teacher, and each evaluator type), average the scores of its evaluators;
then weight the kinds with the weights the admin set in the moment (Stage 3),
redistributing the weight of a kind without submitted evaluations among the others in
proportion. The group grade uses the group levels; each individual grade uses the group
levels replaced by that student's adjustments. Per RAP, the same with the scores of that
RAP's criteria. Every grade is `BigDecimal` (ADR-013) with 2 decimals in the answer.

