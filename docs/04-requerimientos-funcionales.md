# 04 — Functional requirements

> Formalization of the 12 original requirements from the team's brainstorming,
> organized by role, with the cross-cutting configuration rules that apply to all of
> them. Adjusted on 2026-09-24 to the flow of a moment defined by the project lead
> (see "Flow of a moment" and "Team decisions").

## Student

1. Can log in to the system (with their email).
2. Can see the groups that **all** the teachers enabled in a moment of their program
   and join the one agreed with their classmates, even if it belongs to a teacher
   other than theirs. They can only be in one group per moment, and the group
   respects the minimum and maximum number of members the admin set for that moment.
   While the group formation window is open they can leave a group and join another
   one with room.
3. Can upload the classroom project submissions of their group (`.zip` file, up to
   100 MB), with versioning/history of every submission made, and also upload their
   presentation (`.pdf`, `.docx` or `.pptx`, up to 30 MB). Uploads are accepted while
   the submission window is open; those after the group's deadline are marked as
   **late**.
4. Can see their final grade and their individual performance on each RAP.

## External evaluators (today business people and alumni)

5. Can log in and see all the groups they were assigned to.
6. Can grade those groups, only on the evaluation date set by the teacher, within the
   grading window of the moment. They can save an evaluation as a draft and submit it
   later; only submitted evaluations count, and they can be corrected while the
   grading window is open.

External evaluator types ("Empresario", "Egresado" and any created later) are a
configurable catalog, not fixed roles (ADR-011).

## Teacher

7. Can log in, **enable the groups** of a moment that students will join, and set the
   submission deadline of each one (within the submission window). **The teacher
   only sees, manages and grades the groups they enabled.**
8. Can create external evaluators (of any type in the catalog) and assign them
   **only** to the groups they enabled. Can also add to one of their groups a student
   who did not join any group, even after the formation window or the launch, and
   even above the maximum.
9. Can set the date on which each of their groups is evaluated, **within the grading
   window of the moment**, which in turn ends no later than the deadline the UPC sets
   for the period.

## Admin (faculty / dean)

10. Creates and configures the moments of each program of their faculty, in each
    period: RAP, evaluation criteria, performance levels with their scores, minimum
    and maximum members per group, date windows (group formation, submissions and
    grading) and the weight of each kind of evaluator in the final grade (the teacher
    and each evaluator type). Once the groups are formed, **launches** the moment; if
    some groups are below the minimum, the system warns and lets them launch after
    confirming.

## Super Admin

11. Logs in and defines the global system parameters, the faculties and programs, the
    academic periods (with the UPC evaluation deadline) and the external evaluator
    type catalog.

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
| `super_admin` | Everything | Global parameters, faculties, programs, periods, evaluator type catalog |
| `admin` | Their whole faculty | Moments of the programs of their faculty (configuration and launch) |
| `teacher` | Only the groups they enabled | Their groups, their dates, the external evaluators of their groups, adding students without a group to their groups, their evaluations |
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
| Group final grade | Weighted by kind of evaluator (the teacher and each evaluator type), with weights the admin sets in each moment. Several evaluators of the same kind are averaged within that kind; if a kind has no submitted evaluation, its weight is redistributed among the present kinds in proportion to their weights. *(2026-09-25)* |
| Late submissions | Accepted while the submission window is open, marked as late. The graded version is the last on-time one; if there is none, the last late one. The evaluator sees the late mark and decides the levels. *(2026-09-25)* |
| Submission files | Stored on the server disk (ADR-015). `.zip` up to 100 MB; presentation `.pdf`, `.docx` or `.pptx` up to 30 MB. *(2026-09-25)* |
| `Evaluator` interface | `canEvaluate(ProjectGroup)` is abstract and each class implements its own rule (teacher: the groups they enabled; external evaluator: the groups assigned to them). Dates are checked in the service with the `Clock`. *(2026-09-25)* |
| Score in each grade | `CriterionGrade` stores a copy of the level score when grading. *(2026-09-25)* |
| API contract | D1 drafts the routes and DTOs of groups, submissions and evaluation when Stage 1 closes; the lead approves them before the frontend work starts. *(2026-09-25)* |
| Reviews | Review observations are left as comments on the PR. The GitHub branch protection of `develop` stays as it is; reviewing stays a team agreement. *(2026-09-25)* |

## Still to be defined with the team

- Who creates teacher and student accounts (super_admin, faculty admin, bulk upload,
  self-registration)?
- What happens if a teacher needs to reassign a group to another teacher in the
  middle of a moment?
- Do comparative reports require anonymizing student data, or are they always
  handled at an aggregate level?
- Do the evaluator weights of a moment have to add up to 100 %?
