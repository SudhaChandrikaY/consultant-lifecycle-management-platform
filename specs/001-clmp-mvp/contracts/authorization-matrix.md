# Authorization Matrix: CLMP MVP

**Feature**: `001-clmp-mvp`. This matrix is the source for the backend `@PreAuthorize` gates, the
service ownership checks (research R7), the parameterized authorization test (SC-002), and the
frontend navigation map (UX only).

Legend: **✓** allowed · **own** allowed within ownership scope (defined below) · **—** refused
with `403 NOT_AUTHORIZED` and no change.

## Ownership scopes

- **Recruiter "own consultant"**: the consultant's `current_recruiter_id` is the caller's linked
  recruiter.
- **Recruiter "own submission"** (FR-063): `submission.recruiter_id = me` **or** the consultant is
  my own consultant.
- **Recruiter "own placement"** (FR-073): `placement.recruiter_id = me`.
- **Recruiter "own submission for placement"** (FR-070): `submission.recruiter_id = me` only.
  This is narrower than "own submission": being the consultant's current recruiter is not enough.
- **Recruiter "own marketing"**: the assignment's consultant is my own consultant (ownership
  follows reassignment).
- **Unlinked RECRUITER**: the scope is empty. Lists come back empty and single reads and actions
  return `403`.

## Endpoint × Role

| Endpoint | ADMIN | MANAGER | RECRUITER | HR_OPS |
|----------|:-----:|:-------:|:---------:|:------:|
| `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `GET /auth/csrf` | ✓ | ✓ | ✓ | ✓ |
| `GET /reference` | ✓ | ✓ | ✓ | ✓ |
| `GET /vendors`, `GET /clients` | ✓ | ✓ | ✓ | — |
| `GET /dashboard` | ✓ org | ✓ org | ✓ own | ✓ pipeline |
| `GET /recruiters`, `GET /recruiters/{id}` | ✓ | ✓ | — | — |
| `POST /recruiters`, `PUT /recruiters/{id}`, `POST /recruiters/{id}/status`, `GET /recruiters/linkable-users` | ✓ | — | — | — |
| `GET /consultants`, `GET /consultants/skills` | ✓ | ✓ | own | ✓ |
| `GET /consultants/{id}`, `GET /consultants/{id}/history` | ✓ | ✓ | own | ✓ (no commercial sections) |
| `POST /consultants`, `PUT /consultants/{id}` | ✓ | — | — | ✓ |
| `POST /consultants/{id}/status` (manual, FR-031) | ✓ | — | — | ✓ |
| `POST /consultants/{id}/recruiter` (FR-033) | ✓ | ✓ | — | — |
| `GET /marketing-assignments`, `GET /…/{id}`, `GET /…/{id}/history` | ✓ | ✓ | own | — |
| `POST /marketing-assignments` | ✓ | — | own | — |
| `PUT /marketing-assignments/{id}` (dates) | ✓ | — | own | — |
| `POST /marketing-assignments/{id}/notes` | ✓ | — | own | — |
| `POST /marketing-assignments/{id}/transition` → ACTIVE from DRAFT | ✓ | — | own | — |
| `…/transition` ACTIVE→HOLD, HOLD→ACTIVE, →CLOSED | ✓ | ✓ | own | — |
| `…/transition` CLOSED→ACTIVE | ✓ | ✓ | — | — |
| `GET /submissions`, `GET /submissions/{id}` | ✓ | ✓ | own | — |
| `POST /submissions`, `POST /submissions/{id}/status`, `POST /submissions/{id}/notes` | ✓ | — | own | — |
| `GET /placements`, `GET /placements/{id}` | ✓ | ✓ | own | — |
| `GET /placements/draft`, `POST /placements` | ✓ | — | own submission for placement | — |
| `PATCH /placements/{id}` | ✓ | — | — | — |
| `GET /reports/**` | ✓ | ✓ | — | — |
| H2 console (`/h2-console`, `dev` profile only) | ✓ | — | — | — |

## Field-level visibility

| Data | ADMIN | MANAGER | RECRUITER | HR_OPS |
|------|:-----:|:-------:|:---------:|:------:|
| Consultant email / phone / visa expiration / notes (details page only, FR-103) | ✓ | ✓ | own | ✓ |
| Consultant's marketing, submissions, placements on details page | ✓ | ✓ | own | — |
| Vendor, client, bill rate, contract term (anywhere, incl. history descriptions) | ✓ | ✓ | own | — |
| Recruiter performance summary | ✓ | ✓ | — | — |
| Passwords / password hashes | never | never | never | never |

## Frontend navigation (UX only; mirrors the matrix)

| Nav item | ADMIN | MANAGER | RECRUITER | HR_OPS |
|----------|:-----:|:-------:|:---------:|:------:|
| Dashboard | ✓ | ✓ | ✓ | ✓ |
| Recruiters | ✓ | ✓ | — | — |
| Consultants | ✓ | ✓ | ✓ | ✓ |
| Marketing | ✓ | ✓ | ✓ | — |
| Submissions | ✓ | ✓ | ✓ | — |
| Placements | ✓ | ✓ | ✓ | — |
| Reports | ✓ | ✓ | — | — |
