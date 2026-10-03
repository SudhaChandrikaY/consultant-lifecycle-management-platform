# UI Route Contract: CLMP MVP

**Feature**: `001-clmp-mvp`. These are client-side routes served by the Vite SPA. Every route
except `/login` requires a session. A route the role may not use shows a "Not authorized" page and
never fetches that area's data (AS 1.3). Role access follows
[authorization-matrix.md](./authorization-matrix.md).

List routes keep filters in the URL query string, using the same parameter names as the matching
`GET /api/...` list endpoint. That lets dashboard and report links (`link.query`) open a list
pre-filtered to exactly the records counted (FR-083, FR-092).

| Route | Page | Roles | Primary API calls |
|-------|------|-------|-------------------|
| `/login` | Sign-in form; generic error; "session expired" notice | public | `GET /auth/csrf`, `POST /auth/login` |
| `/` | Dashboard (role-shaped panels; count tiles link to lists) | all | `GET /dashboard` |
| `/recruiters` | Recruiter list + filters (team, region, status, search) | ADMIN, MANAGER | `GET /recruiters` |
| `/recruiters/new`, `/recruiters/:id/edit` | Recruiter form | ADMIN | `POST`/`PUT /recruiters` |
| `/recruiters/:id` | Recruiter details; activate/deactivate with confirm dialog | ADMIN, MANAGER (view) | `GET /recruiters/{id}`, `POST /recruiters/{id}/status` |
| `/consultants` | Consultant list + filters; "needs reassignment" badge | ADMIN, MANAGER, RECRUITER (own), HR_OPS | `GET /consultants` |
| `/consultants/new`, `/consultants/:id/edit` | Consultant profile form | ADMIN, HR_OPS | `POST`/`PUT /consultants` |
| `/consultants/:id` | Details: profile, contact (if permitted), status actions, missing-readiness list, assign recruiter, marketing/submissions/placements panels (not HR), history | per matrix | `GET /consultants/{id}`, `/history`, `POST …/status`, `POST …/recruiter` |
| `/marketing` | Marketing list + filters; overdue flag | ADMIN, MANAGER, RECRUITER | `GET /marketing-assignments` |
| `/marketing/new?consultantId=` | Create assignment | ADMIN, RECRUITER | `POST /marketing-assignments` |
| `/marketing/:id` | Details: transitions offered from `allowedTransitions`, reason dialog, notes, history | ADMIN, MANAGER, RECRUITER | `GET`, `POST …/transition`, `POST …/notes` |
| `/submissions` | Submission list + filters incl. date range | ADMIN, MANAGER, RECRUITER | `GET /submissions` |
| `/submissions/new?consultantId=` | Create submission; vendor/client typeahead with "add new"; duplicate-warning dialog | ADMIN, RECRUITER | `GET /vendors`, `GET /clients`, `POST /submissions` |
| `/submissions/:id` | Details, timeline, notes, status change, "Create Placement" when `canCreatePlacement` | ADMIN, MANAGER (read), RECRUITER | `GET`, `POST …/status`, `POST …/notes` |
| `/placements` | Placement list + filters | ADMIN, MANAGER, RECRUITER | `GET /placements` |
| `/placements/new?submissionId=` | Pre-filled placement form; afterwards an "other open submissions — withdraw?" panel | ADMIN, RECRUITER | `GET /placements/draft`, `POST /placements` |
| `/placements/:id` | Placement details + edit (ADMIN) + history | ADMIN, MANAGER, RECRUITER | `GET`, `PATCH /placements/{id}` |
| `/reports` | Report picker + date range; each report table with an empty state | ADMIN, MANAGER | `GET /reports/*` |
| `*` | Not found | all | — |

## Cross-cutting UI behavior

- **Session**: Any `401` response clears client auth state and redirects to
  `/login?expired=1`.
- **403**: Shows the shared "Not authorized" page, with no partial data.
- **409 `CONCURRENT_MODIFICATION`**: Shows a banner reading "This record was changed by someone
  else. Reload to continue." with a Reload action.
- **400/422**: `fieldErrors` render inline next to their fields. `missingItems` and other
  business messages render in a form-level `ErrorBanner`.
- **Loading / empty / error**: Every list, panel, and report uses the shared `LoadingState`,
  `EmptyState`, and `ErrorBanner` components (CLAUDE.md frontend conventions).
- **Unlinked recruiter**: When the signed-in user is a RECRUITER with `recruiterId = null`, the
  shared list empty state shows "Your account is not linked to a recruiter profile. Contact an
  Admin." instead of a generic "no records" message. The dashboard shows the same message from
  the API `message` field.
- **Consultant details for HR_OPERATIONS**: The Marketing, Submissions, Placements, and
  on-Hold open-submission panels are not rendered. The API omits that data, and the page also
  checks the role (UX only).
