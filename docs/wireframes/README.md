# Wireframes

Medium-fidelity wireframes of every SIERAP screen, grouped by role: 48 screens in
grayscale, with components that imitate shadcn/ui (ADR-006) so the frontend can build them
almost as they are. D1 proposed them and the lead approved the format on 2026-09-28
(`docs/04`, "GUI mockups"). They follow the decisions of `docs/04` and the API contract of
`docs/07`. They were updated on 2026-11-24 with everything built after that approval
(home page per role, audit log, exceptional extension, data consent, account editing,
bulk semester and CSV upload, pagination, reports with Excel and PDF export, autosave and
the presentation viewer, group filters, upload progress and version limit, and the
per-criterion detail in My grades), so they match the frontend.

Everything a user sees (screens, captions and notes) is in **Spanish**, because the
wireframes also go into section 5 ("Diseño de GUI") of the delivery document
(`docs/entregas`, ADR-014). The example data uses the real Moment 1 of Ingeniería de
Sistemas (5 RAP, 8 criteria, maximum 5.00); names, emails and figures are made up.

| File | Content |
|---|---|
| `index.html` | Start page: the screens in the flow of a moment and the list by role |
| `acceso.html` | `AU-01`–`AU-07`: login, password links, registration with a code, temporary password, my account, "coming soon" |
| `super-admin.html` | `SA-01`–`SA-07`: faculties and programs, periods, global parameters, evaluator types, admins, home, audit log |
| `admin.html` | `AD-01`–`AD-13`: moments, moment configuration (data and windows, RAP, weights), groups and launch, teachers, registration codes, reports (per moment, group and individual, comparative), exceptional extension, home, audit log |
| `docente.html` | `DO-01`–`DO-12`: groups, group detail, assigning evaluators, submissions, grading, individual adjustment, results, students, evaluators, registration codes, home, CSV upload of students |
| `estudiante.html` | `ES-01`–`ES-06`: groups of the moment, switching groups, my group and submissions, my grades, home |
| `evaluador-externo.html` | `EV-01`–`EV-03`: assigned groups, correcting a submitted evaluation, home |
| `wireframes.css`, `wireframes.js` | Shared styles and the app shell (browser bar, sidebar with the approved menu of each role, top bar) |
| `export-png.cjs` | Exports every screen to `png/<code>.png` |
| `png/` | The exported images, for the delivery document |

## Viewing them

Open `index.html` in a browser. The fonts come from Google Fonts; without internet the
pages still work with the fallback fonts. They are also published as a private page,
<https://claude.ai/artifact/7cKiAJrH9vNm8gxhNRpzrk>, which the lead shares with the team.

## Changing a screen

1. If the change alters a rule (who can do what, when, with which data), update
   `docs/04` and `docs/07` first and get the lead's approval.
2. Edit the role page. Each screen is a `<section class="wf-screen">` with its code,
   notes and a `.frame`. A frame with `data-role`, `data-active` and `data-title` gets the
   app shell from `wireframes.js`; a dialog is a `.w-dim` inside the frame.
3. Export the images again and commit them with the HTML:

   ```bash
   NODE_PATH="$(npm root -g)" node docs/wireframes/export-png.cjs            # all pages
   NODE_PATH="$(npm root -g)" node docs/wireframes/export-png.cjs docente    # one page
   ```

   It needs Playwright with its Chromium installed globally (`npm install -g playwright`
   and `npx playwright install chromium`); it is not a project dependency.
