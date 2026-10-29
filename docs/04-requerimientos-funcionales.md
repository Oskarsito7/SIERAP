# 04 — Functional requirements

> Formalization of the 12 original requirements from the team's brainstorming,
> organized by role, with the cross-cutting configuration rules that apply to all of
> them. Adjusted on 2026-09-24 to the flow of a moment defined by the project lead
> (see "Flow of a moment" and "Team decisions").

## Student

1. Can log in to the system (with their email). Gets their account from their teacher
   (created one by one or by bulk upload) or registers with a **registration code**
   the teacher shares; either way they set their password through a link sent by
   email. Can recover a forgotten password the same way.
2. Can see the groups that **all** the teachers enabled in a moment of their program
   whose semester range includes their current semester, and join the one agreed
   with their classmates, even if it belongs to a teacher other than theirs. The
   members write the title of their classroom project. They can only be in one group per moment, and the group
   respects the minimum and maximum number of members the admin set for that moment.
   While the group formation window is open they can leave a group and join another
   one with room.
3. Can upload the classroom project submissions of their group (`.zip` file, up to
   100 MB, required in every version), with versioning/history of every submission
   made, and optionally their presentation (`.pdf`, `.docx` or `.pptx`, up to 30 MB). Uploads are accepted while
   the submission window is open; those after the group's deadline are marked as
   **late**.
4. Can see, once the grading window closes, their final grade (their **individual**
   grade), their performance on each RAP and the evaluators' comments.

## External evaluators (today business people and alumni)

5. Can log in and see all the groups they were assigned to.
6. Can grade those groups from the evaluation date set by the teacher until the
   grading window of the moment closes. They can save an evaluation as a draft and
   submit it later; only submitted evaluations count, and they can be corrected while
   the grading window is open. They grade the group once and can adjust the level of
   a specific student in some criteria (individual grade, ADR-018).

External evaluator types ("Empresario", "Egresado" and any created later) are a
configurable catalog, not fixed roles (ADR-011).

## Teacher

7. Can log in, **enable the groups** of a moment that students will join, and set the
   submission deadline of each one (within the submission window). **The teacher
   only sees, manages and grades the groups they enabled.** Creates the accounts of
   their students (one by one or by bulk upload) or generates a **registration
   code** for one program of their faculty, reusable until the expiry date they set
   (they can deactivate it earlier).
8. Can create external evaluators (of any type in the catalog), or find an existing
   one by email or document, and assign them **only** to the groups they enabled. Can also add to one of their groups a student
   who did not join any group, even after the formation window or the launch, and
   even above the maximum.
9. Can set the date on which each of their groups is evaluated, **within the grading
   window of the moment**, which in turn ends no later than the deadline the UPC sets
   for the period.

## Admin (faculty / dean)

10. Creates the teachers of their faculty (and can also create its students and
    external evaluators). Creates and configures the moments of each program of their faculty, in each
    period: RAP, evaluation criteria, performance levels with their scores, minimum
    and maximum members per group, date windows (group formation, submissions and
    grading) and the weight of each kind of evaluator in the final grade (the teacher
    and each evaluator type). Once the groups are formed, **launches** the moment; if
    some groups are below the minimum, the system warns and lets them launch after
    confirming.

## Super Admin

11. Logs in and defines the global system parameters, the faculties and programs, the
    academic periods (with the UPC evaluation deadline) and the external evaluator
    type catalog. Creates the admins of each faculty (and can create any other user).

## Cross-cutting — Reporting

12. Individual, group and full reports can be generated, including comparative ones
    (compare semester X with semester Y, compare by years, etc.).

## Flow of a moment

This is how Moment 1 runs, and exactly the same for Moment 2, Moment 3 and those of any
program (ADR-012):

| # | Phase | Who | What they do |
|---|---|---|---|
| 1 | Configuration | Admin | Creates the moment (e.g. "Momento 1") in the period and configures its RAP, criteria (grading items), levels with scores, members per group and date windows. |
| 2 | Configuration | Teacher | Enables their groups in the moment, creates and assigns their external evaluators and sets the submission and evaluation dates of their groups. |
| 3 | Group formation (window) | Student | Enters SIERAP, sees the groups of all the teachers of the moment and joins the one agreed with their classmates ("I'm going with so-and-so"). |
| 4 | Launch | Admin | Launches the moment: configuration and group membership are locked. |
| 5 | Submissions (window) | Student | Their group uploads submission versions (zip + presentation). |
| 6 | Grading (window) | Teacher and external evaluators | Grade only the groups assigned to them, picking one level per criterion. |
| 7 | Closing | — | After the grading window, grades become final and feed the reports. |

## Cross-cutting configuration rules (apply to all the roles above)

- Evaluation criteria differ by program; even the RAP may differ. The
  admin/super_admin of each faculty and program must be able to define their own RAP
  in the system.
- Each moment assesses up to a different semester for the same program (Moment 1
  assesses up to a certain semester with its RAP; Moment 2 up to another semester;
  and so on until all the moments are covered).
- **Everything must be configurable:** evaluation criteria, scoring, moments (from/to
  which semester each one assesses) and RAP. Avoid hard-coding these business values
  in the code — they must live in the database and be editable from the
  corresponding role.
- **Nothing standard, and per period:** each moment has its own RAP; each RAP, its
  criteria; each criterion, its performance levels with their scores. The admin
  creates, modifies or deletes them while the moment is in configuration. That
  configuration is defined per academic period (at the start of a semester the
  previous one is copied and adjusted), so past semesters do not change and can be
  compared (ADR-010).

## Access control — summary by role

| Role | Can see | Can modify |
|---|---|---|
| `super_admin` | Everything | Global parameters, faculties, programs, periods, evaluator type catalog; accounts of any role |
| `admin` | Their whole faculty | Moments of the programs of their faculty (configuration and launch); accounts of teachers, students and external evaluators of their faculty |
| `teacher` | Only the groups they enabled | Their groups, their dates, the external evaluators of their groups, adding students without a group to their groups, their evaluations; accounts and registration codes of their students |
| `student` | The groups of the moments of their program; their group and their grades | Their enrollment (during group formation) and their group's submissions |
| `external_evaluator` | Only the groups they were assigned to | Their own evaluations (on the set date) |

## Team decisions

Defined by the project lead on 2026-09-24.

| Topic | Decision |
|---|---|
| Login | Every user logs in with their **email** (institutional, or personal for external evaluators). The identity document type and number are also stored. |
| Student and groups | A student has **one group per moment**; they may go through several groups during the program and the history is kept. |
| Who forms the groups | The teacher **enables** groups; the program's students see the groups of **all** the teachers of the moment and **join** them themselves, even if the group belongs to another teacher. There is no prior list of students per moment. |
| Group and project | Each group has **a single** classroom project: group and project are the same entity (`ProjectGroup`). |
| Members per group | The minimum and maximum are defined by the admin **in each moment**. |
| Self-assessment | The student does **not** self-assess. The group's teacher and the assigned external evaluators grade. |
| Evaluator types | Today they are business person and alumnus, but more can be created with other names: an `EvaluatorType` catalog maintained by the super_admin (ADR-011). |
| Teachers per group | Each group has **a single** teacher: the one who enabled it, and only they grade it (together with the external evaluators they assign). |
| Teacher and faculty | Each teacher belongs to **one** faculty. Teachers of filler courses who teach in several faculties do not use SIERAP. |
| Assessment structure | Moment → RAP → criteria → performance levels with scores. The evaluator picks one level per criterion and the grade is the sum of the scores. |
| Levels | The requirement level of the moment and the performance levels are configurable; there are no fixed lists. |
| Configuration over time | Defined per academic period; when a period opens, the previous one is copied (ADR-010). |
| Moment lifecycle | Configuration (includes group formation) → **Launch** (admin, with the groups formed; locks configuration and groups) → submissions → grading → closing (ADR-012). |
| Dates | The admin sets the windows of the moment (group formation, submissions, grading) within the UPC limit; each teacher sets the submission and evaluation dates of their groups within those windows. |
| `User` inheritance | `JOINED`: common `app_user` table + one table per role (ADR-009). |
| Faculty and program codes | `Faculty` and `Program` have an institutional `code` (unique, required): Facultad de Ingenierías y Tecnologías = `04`, Ingeniería de Sistemas = `108`. |
| Seed data | Faculty, Ingeniería de Sistemas, current period and the complete Moment 1 (5 RAP with the criteria and levels of the official rubric, `docs/00`), in configuration status and editable. Moment 1 assesses semesters 1 to 3, with groups of 3 to 5 members, and has no date windows yet. The 2026-2 period dates are provisional and will be replaced with the official ones through a new migration. |
| Scores | Handled as exact decimals (`BigDecimal`) so Moment 1 adds up to exactly 5.0 (ADR-013). |
| Language | Code, documentation and commit messages in English; screens, API error messages, Javadoc and code comments in Spanish (ADR-014). |
| Migrations | A Flyway migration that is already in `develop` is never edited: any fix goes in a new migration. |
| Admins per faculty | A faculty can have **several** admins (e.g. dean and coordinator), all with the same permissions. *(2026-09-25)* |
| Deleting data | Configuration without groups or grades is deleted in cascade (faculty → program → moment → RAP → criteria → levels); once there are groups or grades the database blocks the deletion and the API answers 409 with a message in Spanish (ADR-016). *(2026-09-25)* |
| Launch with incomplete groups | The system warns which groups are below the minimum and lets the admin launch after confirming; those groups stay as they are. *(2026-09-25)* |
| Students without a group | A student who did not join a group can be added by a teacher to one of their groups, even after the formation window or the launch, and even above the maximum. *(2026-09-25)* |
| Switching groups | Free while the group formation window is open (leave a group, join another one with room). *(2026-09-25)* |
| Evaluation drafts | An evaluation can be saved as a draft (`DRAFT`) and submitted later (`SUBMITTED`). Only submitted evaluations count; they can be corrected while the grading window is open. *(2026-09-25)* |
| Submitting an evaluation | It can only be submitted with a level picked in **every** criterion of the moment; otherwise the API answers `422` and it stays as a draft. *(2026-09-27)* |
| Group final grade | Weighted by kind of evaluator (the teacher and each evaluator type), with weights the admin sets in each moment. Several evaluators of the same kind are averaged within that kind; if a kind has no submitted evaluation, its weight is redistributed among the present kinds in proportion to their weights. *(2026-09-25)* |
| Late submissions | Accepted while the submission window is open, marked as late. The graded version is the last on-time one; if there is none, the last late one. The evaluator sees the late mark and decides the levels. *(2026-09-25)* |
| Submission files | Stored on the server disk (ADR-015). `.zip` up to 100 MB; presentation `.pdf`, `.docx` or `.pptx` up to 30 MB. *(2026-09-25)* |
| `Evaluator` interface | `canEvaluate(ProjectGroup)` is abstract and each class implements its own rule (teacher: the groups they enabled; external evaluator: the groups assigned to them). Dates are checked in the service with the `Clock`. *(2026-09-25)* |
| Score in each grade | `CriterionGrade` stores a copy of the level score when grading. *(2026-09-25)* |
| API contract | D1 drafts the routes and DTOs of groups, submissions and evaluation; the lead approves them before the frontend work starts. *(2026-09-25)* It is written **before** the wireframes and without waiting for Stage 1 to close, so D3 can start Stage 4. *(2026-09-27)* |
| Reviews | Review observations are left as comments on the PR. The GitHub branch protection of `develop` stays as it is; reviewing stays a team agreement. *(2026-09-25)* |
| Launching early | The admin can launch a moment before the group formation window ends; launching closes group formation. *(2026-09-27)* |
| UPC deadline | The UPC evaluation deadline always falls within the academic period (between its start and end dates). *(2026-09-27)* |
| Who creates accounts | super_admin → faculty admins; admin → teachers; teacher → their students (one by one or bulk upload) and external evaluators. Upper roles can also create lower users within their scope (ADR-017). *(2026-09-27)* |
| Registration code | A teacher can generate a code linked to themselves and one program of their faculty, reusable until the expiry date they set, and deactivate it earlier. Students register with it and land in that program. *(2026-09-27)* |
| Getting a password | Accounts created by someone else, and self-registered students, receive an **email link** to set their password. "Forgot password" uses the same mechanism (ADR-017). *(2026-09-27)* |
| First super_admin | Created by a migration with a temporary password that must be changed at the first login (ADR-017). *(2026-09-27)* |
| Sessions | 15-minute access token kept in memory + 7-day refresh token in an httpOnly cookie (ADR-017). *(2026-09-27)* |
| Email | Institutional UPC account (SMTP) in production; Mailpit in local development (ADR-017). *(2026-09-27)* |
| GUI mockups | D1 (with Claude Code) proposes the wireframes of every screen; the team adjusts them. *(2026-09-27)* |
| Password rules | At least 8 characters, with at least one letter and one number. *(2026-09-27)* |
| Email link expiry | The link to set the password of a new account expires in **72 hours**; the "forgot password" link, in **1 hour** (both configurable). *(2026-09-27)* |
| Registration with a code | Only institutional emails (`@unicesar.edu.co`, configurable) are accepted. *(2026-09-27)* |
| Who joins a moment's groups | Only students of the program whose **current semester** is within the moment's range see and join its groups. The teacher can add any student of the program by hand (rule of students without a group). *(2026-09-28)* |
| Shared external evaluators | When assigning evaluators, a teacher can find an existing one by exact email or document and assign them without creating a new account. Each teacher still manages (deactivate, resend link) only the evaluators they created. *(2026-09-28)* |
| Teacher changes after the launch | The teacher can still change the submission deadline and the evaluation date of their groups (within the windows) and assign or remove external evaluators while the grading window is open. Groups cannot be created or deleted after the launch. *(2026-09-28)* |
| Project title | The members of the group write the project title and can change it until the submission window ends; the teacher can edit it too. The teacher sets the group name. *(2026-09-28)* |
| Submission contents | Each version needs the `.zip`; the presentation is optional. If a version has none, the latest presentation of an earlier version is shown. *(2026-09-28)* |
| Grading period of a group | Teacher and external evaluators grade a group from its evaluation date until the grading window closes; in that span they save drafts, submit and correct. *(2026-09-28)* |
| What the student sees | After the grading window closes: their final grade, their performance per RAP and the evaluators' comments, without the levels each evaluator picked. *(2026-09-28)* |
| Group grade and individual grade | The group is graded **once** and every member inherits those levels. Any evaluator can open one student and change the level of the criteria where that student did better or worse; that student's individual grade then differs from the others'. There are two grades: the group grade and each member's individual grade, which is their final grade (ADR-018). *(2026-09-28)* |
| Grade reviews | Disagreements are handled outside SIERAP: the student talks to their teacher and the evaluator adjusts the individual grade while the grading window is open. *(2026-09-28)* |
| API contract of Stages 4 to 6 | Approved with the technical details D1 proposed in `docs/07`. *(2026-09-28)* |
| Wireframes | Medium-fidelity grayscale HTML pages in `docs/wireframes/` (one per role), exported to PNG for the delivery document and published as a private link for the team; changes go through PRs. *(2026-09-28)* |
| Evaluator weights | The weights of a moment (the teacher and each evaluator type) are percentages that must add up to **100 %**; the moment cannot be launched otherwise. If a kind has no submitted evaluation, its weight is still redistributed among the present kinds. *(2026-10-02)* |
| Stage 6 split | D3: `individual_grade` (V7), the evaluation endpoints and the screens to grade, the group results and "My grades". D2: the weights in the moment configuration (V9) while finishing Stage 3. D1: the grade calculation (ADR-018 formula) and the two results routes. *(2026-10-02)* |
| Evaluator type without weight | The evaluation of an external evaluator whose type has no weight in the moment (or 0 %) does not count in the grade. The screen to assign evaluators warns about it but does not block the assignment, because the admin may still be setting the weights. *(2026-10-16)* |
| Rounding of grades | Averages and weighting keep 16 significant digits; only the result (the total and each RAP, group and individual) is rounded to 2 decimals, half up: 2.995 → 3.00. *(2026-10-16)* |
| Grade in the domain model | `ProjectGroup` computes the group grade and each individual grade (in total and per RAP) from its submitted evaluations and the weights of its moment, with the `GradeCalculator` utility; the results service only checks who sees them and when. *(2026-10-16)* |
| Comments of evaluators without weight | The student also sees the comment of an evaluator whose type has no weight: the grade does not count, but the feedback does. *(2026-10-16)* |
| Reporting scope (Moment 1) | Reports are shown on screen with charts and tables (individual, group, consolidated per moment and comparative across periods); exporting to PDF or Excel is deferred to Moment 2. `Report.generate()` returns a `ReportData` DTO instead of `byte[]`, keeping the polymorphic hierarchy. *(2026-10-29)* |
| Comparative reports | Handled strictly at an aggregate level, without student names, codes or individual rows: they compare cohort averages, per-RAP achievement percentages and grade distributions across two periods of the same program and moment number. *(2026-10-29)* |
| Bulk upload of students | Moves to Moment 2; students are created one by one or register with a code. *(2026-10-29)* |
| Moment 1 delivery | Between November 15 and 30, 2026. *(2026-10-29)* |
| Students list (DO-08) | `GET /api/students` returns each student's code, semester and how they arrived (registration code or creator), plus whether the viewer can manage them, so the screen hides the actions it cannot use. The "Without a group" filter is in Moment 1, with the same rule as the list used to add members to a group. *(2026-10-29)* |

## Still to be defined with the team

- What happens if a teacher needs to reassign a group to another teacher in the
  middle of a moment?
