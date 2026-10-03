# REST API Contract: CLMP MVP

**Feature**: `001-clmp-mvp` | **Base path**: `/api` | **Format**: JSON (UTF-8)

Field shapes refer to [data-model.md](../data-model.md). Role access per endpoint is defined in
[authorization-matrix.md](./authorization-matrix.md). This document covers request and response
shapes and error behavior.

## Conventions

- **Auth**: The session cookie is issued by `POST /api/auth/login`. Every endpoint except
  `/api/auth/login` and `/api/auth/csrf` requires an authenticated session. If the session is
  missing or expired, the endpoint returns `401` (never a redirect).
- **CSRF**: Mutating requests (`POST/PUT/PATCH`) MUST send the `X-XSRF-TOKEN` header, matching
  the `XSRF-TOKEN` cookie.
- **Ids**: numeric `id` (Long). **Dates**: `YYYY-MM-DD`. **Timestamps**: ISO-8601 UTC
  (`2026-10-02T14:05:00Z`). **Money**: decimal number with 2 places (`85.00`), USD/hour.
- **Enums**: UPPER_SNAKE codes, as in data-model (e.g., `INTERVIEW_SCHEDULED`). The frontend maps
  them to labels.
- **Optimistic concurrency**: Every update and transition body includes `version` (the value from
  the last read). A mismatch returns `409 CONCURRENT_MODIFICATION`.
- **Paging** (all list endpoints): `?page=0&size=25&sort=field,asc`. `size` is at most 100.
  `sort` fields are allowlisted per list. The response looks like this:

  ```json
  { "items": [ ... ], "page": 0, "size": 25, "totalItems": 137, "totalPages": 6 }
  ```

- **Scope**: List endpoints silently apply the caller's scope (for example, a RECRUITER sees only
  their own records). Single-record endpoints outside the caller's scope return `403 NOT_AUTHORIZED`.

## Errors

All errors use RFC 9457 `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "code": "VALIDATION_FAILED",
  "fieldErrors": [ { "field": "targetDate", "message": "Target date must be on or after start date." } ]
}
```

| HTTP | `code` | When | Extra properties |
|------|--------|------|------------------|
| 400 | `VALIDATION_FAILED` | Bean validation / field format (FR-104) | `fieldErrors[]` |
| 401 | `UNAUTHENTICATED` | No/expired session | — |
| 401 | `INVALID_CREDENTIALS` | Bad username/password **or** inactive user (generic message) | — |
| 403 | `NOT_AUTHORIZED` | Role or ownership refused (FR-004) | — |
| 404 | `NOT_FOUND` | Record id does not exist | — |
| 409 | `CONCURRENT_MODIFICATION` | `version` mismatch | `currentVersion` |
| 409 | `DUPLICATE_EMAIL` | Recruiter/consultant email in use | `fieldErrors[]` (email) |
| 409 | `DUPLICATE_SUBMISSION` | Matching earlier submission(s) and `acknowledgeDuplicate` not `true` (FR-055) | `duplicates[]`: `{id, status, submittedDate, createdAt, recruiterName}` |
| 409 | `OPEN_ASSIGNMENT_EXISTS` | Consultant already has DRAFT/ACTIVE/HOLD marketing (FR-042) | `existingRecordId` |
| 409 | `CONFIRMATION_REQUIRED` | Deactivating recruiter with assigned consultants without `confirm=true` (FR-014) | `affectedConsultantCount` |
| 422 | `INVALID_TRANSITION` | Transition not allowed from the current status | `currentStatus`, `allowedTransitions[]` |
| 422 | `READINESS_INCOMPLETE` | Bench/Hold → Ready with missing items (FR-035) | `missingItems[]` (e.g., `"phone"`, `"assignedActiveRecruiter"`) |
| 422 | `CONSULTANT_NOT_ELIGIBLE` | Consultant status forbids action (market/submit/place) | `consultantStatus` |
| 422 | `RECRUITER_INACTIVE` | Assigning to inactive recruiter (FR-034) | — |
| 422 | `OPEN_SUBMISSIONS_EXIST` | Setting consultant Inactive with open submissions | `openSubmissionIds[]` |
| 422 | `SUBMISSION_NOT_AT_OFFER` | Placement from non-Offer submission (FR-070) | `submissionStatus` |
| 422 | `ALREADY_PLACED` | Consultant PLACED/ACTIVE_PROJECT (FR-075) | — |
| 422 | `BUSINESS_RULE` | Other rule (e.g., date ordering, reopen of "Placed" assignment) | `fieldErrors[]` optional |

Error bodies never echo passwords, and they never include sensitive fields the caller can't see.

---

## Auth — `/api/auth`

| Method & path | Body | Success |
|---------------|------|---------|
| `GET /csrf` | — | `204`; sets `XSRF-TOKEN` cookie |
| `POST /login` | `{ "username", "password" }` | `200` `CurrentUser` |
| `POST /logout` | — | `204`; session invalidated |
| `GET /me` | — | `200` `CurrentUser` |

`CurrentUser`:

```json
{ "id": 3, "username": "recruiter1", "displayName": "Riya Patel", "role": "RECRUITER",
  "recruiterId": 7, "sessionTimeoutMinutes": 30 }
```

`recruiterId` is `null` if the user isn't linked to a recruiter profile (that user gets the
empty-state experience).

## Reference — `/api/reference`

| Method & path | Success |
|---------------|---------|
| `GET /api/reference` | `{ teams:[{id,code,name}], regions:[…], visaTypes:[code…], consultantStatuses:[…], marketingStatuses:[…], submissionStatuses:[…] }` |
| `GET /api/vendors?q=acm` | `[{ id, name }]`, at most 20, case-insensitive contains |
| `GET /api/clients?q=…` | same |

New vendors and clients are created implicitly through submission create (FR-051).

## Recruiters — `/api/recruiters`

| Method & path | Body / Query | Success |
|---------------|--------------|---------|
| `GET /` | `q, teamId, regionId, status, page, size, sort` | Page of `RecruiterListItem {id, fullName, team{id,name}, region{id,name}, status, assignedConsultantCount}` |
| `GET /{id}` | — | `RecruiterDetail` = list item + `{email, phone, linkedUser{id,username,displayName}?, version}` |
| `POST /` | `RecruiterRequest {fullName, email, phone?, teamId, regionId, linkedUserId?}` | `201` `RecruiterDetail` (status `ACTIVE`) |
| `PUT /{id}` | `RecruiterRequest + version` | `200` `RecruiterDetail` |
| `POST /{id}/status` | `{ status: "ACTIVE"\|"INACTIVE", confirm?: boolean, version }` | `200` `RecruiterDetail` with `consultantsFlaggedForReassignment` count when deactivated |
| `GET /linkable-users` | — | `[{id, username, displayName}]`: RECRUITER users not yet linked |

## Consultants — `/api/consultants`

| Method & path | Body / Query | Success |
|---------------|--------------|---------|
| `GET /` | `q, status (multi), primarySkill, visaType, recruiterId, needsReassignment, page, size, sort` | Page of `ConsultantListItem` |
| `GET /{id}` | — | `ConsultantDetail` (role-shaped) |
| `POST /` | `ConsultantRequest` | `201` `ConsultantDetail` (status `BENCH`) |
| `PUT /{id}` | `ConsultantRequest + version` | `200` `ConsultantDetail` |
| `POST /{id}/status` | `{ targetStatus, reason?, version }` | `200` `ConsultantDetail` |
| `POST /{id}/recruiter` | `{ recruiterId, version }` | `200` `ConsultantDetail` |
| `GET /{id}/history` | `page, size` | Page of `HistoryEntry` |
| `GET /skills` | — | Distinct primary skills (filter dropdown) |

`ConsultantRequest`: `{firstName, lastName, email, phone?, city?, state?, primarySkill?,
additionalSkills?, yearsExperience? (0–50), visaType?, visaExpirationDate?, notes?}`

`ConsultantListItem` contains **no contact fields** (FR-024):

```json
{ "id": 12, "fullName": "Arun Kumar", "primarySkill": "Java", "yearsExperience": 8,
  "visaType": "H1B", "assignedRecruiter": { "id": 7, "fullName": "Riya Patel" },
  "status": "READY", "needsReassignment": false }
```

`ConsultantDetail`:

```json
{
  "id": 12, "firstName": "Arun", "lastName": "Kumar", "city": "Edison", "state": "NJ",
  "primarySkill": "Java", "additionalSkills": "Spring, Kafka", "yearsExperience": 8,
  "visaType": "H1B", "status": "READY", "needsReassignment": false,
  "assignedRecruiter": { "id": 7, "fullName": "Riya Patel", "status": "ACTIVE" },
  "contact": { "email": "…", "phone": "…", "visaExpirationDate": "2027-05-31", "notes": "…" },
  "allowedStatusTransitions": ["BENCH", "HOLD", "INACTIVE"],
  "missingReadinessItems": [],
  "currentMarketingAssignment": { "id": 4, "status": "ACTIVE", "targetDate": "…", "overdue": false },
  "submissions": [ { "id": 31, "vendorName": "…", "clientName": "…", "jobTitle": "…", "status": "SUBMITTED", "submittedDate": "…" } ],
  "placements": [ { "id": 2, "clientName": "…", "startDate": "…" } ],
  "openSubmissionsWhileOnHold": [ ],
  "version": 5
}
```

- `contact` is present only for ADMIN, MANAGER, HR_OPERATIONS, and the assigned RECRUITER
  (FR-103).
- For **HR_OPERATIONS**, `currentMarketingAssignment`, `submissions`, `placements`, and
  `openSubmissionsWhileOnHold` are **omitted entirely** (FR-064, AS 6.7).
- `allowedStatusTransitions` lists the manual transitions available to this caller (empty for
  MANAGER and RECRUITER).

`HistoryEntry` (shared by all history endpoints):

```json
{ "id": 901, "occurredAt": "2026-10-02T14:05:00Z", "actor": "Riya Patel",
  "changeType": "STATUS", "field": null, "oldValue": "MARKETING", "newValue": "INTERVIEWING",
  "reason": null, "note": null, "systemTriggered": true,
  "trigger": { "event": "SUBMISSION_INTERVIEW_SCHEDULED", "entityType": "SUBMISSION", "entityId": 31 },
  "description": "Submission for Acme / Java Developer moved to Interview Scheduled" }
```

For HR_OPERATIONS viewers, `trigger.entityType`/`entityId` are `null` and `description` is
generic, for example "Submission moved to Interview Scheduled" or "Placement created" (research
R8).

## Marketing — `/api/marketing-assignments`

| Method & path | Body / Query | Success |
|---------------|--------------|---------|
| `GET /` | `status (multi), recruiterId, teamId, overdue, page, size, sort` | Page of `MarketingListItem {id, consultant{id,fullName}, ownerRecruiter{id,fullName}, team{id,name}, startDate, targetDate, status, overdue}` |
| `GET /{id}` | — | `MarketingDetail` = list item + `{holdReason, closeReason, notes[], allowedTransitions[], version}` |
| `POST /` | `{ consultantId, ownerRecruiterId?, startDate, targetDate }` | `201` `MarketingDetail` (status `DRAFT`) |
| `PUT /{id}` | `{ startDate, targetDate, version }` | `200` (not MANAGER) |
| `POST /{id}/transition` | `{ targetStatus: "ACTIVE"\|"HOLD"\|"CLOSED", reason?, version }` | `200` `MarketingDetail` |
| `POST /{id}/notes` | `{ body }` | `201` `Note {id, body, author, createdAt}` (not MANAGER) |
| `GET /{id}/history` | — | Page of `HistoryEntry` |

`allowedTransitions` already accounts for the caller's role (for example, it never offers
`CLOSED → ACTIVE` to a RECRUITER).

## Submissions — `/api/submissions`

| Method & path | Body / Query | Success |
|---------------|--------------|---------|
| `GET /` | `status (multi), recruiterId, vendorId, clientId, consultantId, submittedFrom, submittedTo, page, size, sort` | Page of `SubmissionListItem {id, consultant{id,fullName}, recruiter{id,fullName}, vendor{id,name}, client{id,name}, jobTitle, submittedDate, billRate, status}` |
| `GET /{id}` | — | `SubmissionDetail` = list item + `{duplicateAcknowledgement?, notes[], timeline[] (HistoryEntry), allowedTransitions[], canCreatePlacement, version}` |
| `POST /` | `SubmissionCreateRequest` | `201` `SubmissionDetail`, or `409 DUPLICATE_SUBMISSION` |
| `POST /{id}/status` | `{ targetStatus, note?, submittedDate?, version }` | `200` `SubmissionDetail` |
| `POST /{id}/notes` | `{ body }` | `201` `Note` |

`SubmissionCreateRequest`:

```json
{ "consultantId": 12, "recruiterId": null,
  "vendorId": null, "vendorName": "Acme Staffing",
  "clientId": 5, "clientName": null,
  "jobTitle": "Java Developer", "billRate": 85.00,
  "submitNow": true, "submittedDate": "2026-10-02",
  "note": "Strong Spring match",
  "acknowledgeDuplicate": false }
```

- Exactly one of `vendorId`/`vendorName` (and likewise for clients) must be given. A name is
  resolved by find-or-create on its normalized form.
- `submitNow=false` saves a `DRAFT` with no submitted date.
- Duplicate flow (AS 5.3): the first POST returns `409 DUPLICATE_SUBMISSION` with `duplicates[]`.
  The UI shows the warning. On confirm, the UI re-POSTs the same body with
  `acknowledgeDuplicate: true`. The server records the acknowledgement (user, time, earlier ids).
- `targetStatus: "PLACED"` on `/status` is always refused with `INVALID_TRANSITION`. Use the
  placement endpoint instead.
- There is no general submission edit endpoint. The MVP supports create, status progression, and
  notes only.
- `canCreatePlacement` = status is `OFFER` AND (the caller is ADMIN OR the caller is the
  submission's recruiter). A RECRUITER who can see the submission only because the consultant is
  now assigned to them gets `false` (FR-070).
- `duplicateAcknowledgement`: `{ acknowledgedBy, acknowledgedAt, earlierSubmissionIds[] }`.

## Placements — `/api/placements`

| Method & path | Body / Query | Success |
|---------------|--------------|---------|
| `GET /` | `recruiterId, clientId, vendorId, startFrom, startTo, createdFrom, createdTo, page, size, sort` | Page of `PlacementListItem {id, consultant{id,fullName}, recruiter{id,fullName}, client{id,name}, vendor{id,name}, startDate, billRate, contractTermMonths, expectedEndDate}` |
| `GET /{id}` | — | `PlacementDetail` = list item + `{submissionId, jobTitle, createdAt, createdBy, history[], version}` |
| `GET /draft?submissionId=31` | — | `PlacementDraft {submissionId, consultant, recruiter, vendor, client, jobTitle, billRate}`: pre-fill (AS 6.1); refused unless at OFFER |
| `POST /` | `{ submissionId, startDate, billRate, contractTermMonths }` | `201` `PlacementCreated`. ADMIN, or the RECRUITER who is the submission's recruiter (FR-070); the placement's recruiter is copied from the submission. A consultant on HOLD may be placed (HOLD → PLACED) |
| `PATCH /{id}` | `{ startDate?, billRate?, contractTermMonths?, version }` | `200` `PlacementDetail` (ADMIN only). `startDate` earlier than the submission's `submittedDate` → `400 VALIDATION_FAILED` |

`PlacementCreated` = `PlacementDetail` + `otherOpenSubmissions: [{id, vendorName, clientName,
jobTitle, status}]`. The UI then offers to withdraw each one through
`POST /api/submissions/{id}/status {targetStatus:"WITHDRAWN"}` (FR-076). Nothing is withdrawn
automatically.

## Dashboard — `/api/dashboard`

`GET /api/dashboard` returns a role-shaped `Dashboard`:

```json
{
  "scope": "ORGANIZATION",
  "message": null,
  "counts": [
    { "key": "BENCH_CONSULTANTS", "label": "Bench", "value": 14,
      "link": { "list": "consultants", "query": "status=BENCH" } },
    { "key": "READY_CONSULTANTS", "value": 6, "link": { "list": "consultants", "query": "status=READY" } },
    { "key": "ACTIVE_SUBMISSIONS", "value": 22, "link": { "list": "submissions", "query": "status=SUBMITTED&status=UNDER_REVIEW&status=INTERVIEW_SCHEDULED&status=INTERVIEW_CLEARED&status=OFFER" } },
    { "key": "INTERVIEW_SUBMISSIONS", "value": 5, "link": { "list": "submissions", "query": "status=INTERVIEW_SCHEDULED&status=INTERVIEW_CLEARED" } },
    { "key": "PLACEMENTS_THIS_MONTH", "value": 2, "link": { "list": "placements", "query": "createdFrom=2026-10-01&createdTo=2026-10-31" } },
    { "key": "NEEDS_REASSIGNMENT", "value": 3, "link": { "list": "consultants", "query": "needsReassignment=true" } }
  ],
  "consultantsByStatus": null,
  "recruiterPerformance": [
    { "recruiterId": 7, "recruiterName": "Riya Patel", "assignedConsultants": 9,
      "activeSubmissions": 6, "interviews": 2, "placementsThisMonth": 1 }
  ],
  "recentActivity": [ /* up to 20 HistoryEntry */ ]
}
```

| Role | `scope` | Includes |
|------|---------|----------|
| ADMIN, MANAGER | `ORGANIZATION` | All counts, `recruiterPerformance`, `NEEDS_REASSIGNMENT` (FR-080, FR-081) |
| RECRUITER | `OWN` | The five FR-080 counts limited to own scope; no `recruiterPerformance` and no `NEEDS_REASSIGNMENT` (FR-082) |
| RECRUITER (unlinked) | `OWN` | All zero, `message: "Your account is not linked to a recruiter profile. Contact an Admin."` |
| HR_OPERATIONS | `CONSULTANT_PIPELINE` | `consultantsByStatus` (every status, each with `link`), `BENCH`/`READY` counts, consultant-only `recentActivity`; **no** submission/placement counts, rates, or performance (FR-082) |

Placement links filter by `createdFrom/createdTo` (placement creation date, which is what FR-080
counts), so the placements list also accepts `createdFrom`/`createdTo`.

## Reports — `/api/reports` (ADMIN, MANAGER)

| Method & path | Query | Response |
|---------------|-------|----------|
| `GET /submissions-by-recruiter` | `from, to` (default current month) | `{from, to, rows:[{recruiterId, recruiterName, total, byStatus:{SUBMITTED:3, …}}], totals}` (counts by `submitted_date` in range; status = current) |
| `GET /placements-by-recruiter` | `from, to` | `{from, to, rows:[{recruiterId, recruiterName, placements}], totals}` (by placement `created_at` date) |
| `GET /consultant-pipeline` | — | `{rows:[{status, count}], total}` (all 8 statuses, zeros included) |
| `GET /bench-ready` | — | `{bench, ready, asOf}` |
| `GET /vendor-client-activity` | `from, to` | `{vendors:[{id, name, submissions, interviewsScheduled, placements}], clients:[…]}` |

- In `vendor-client-activity`, `submissions` counts submissions with `submitted_date` in range,
  `interviewsScheduled` counts those of them whose **current** status is `INTERVIEW_SCHEDULED`
  (list link: `status=INTERVIEW_SCHEDULED&vendorId=…&submittedFrom=…&submittedTo=…`), and
  `placements` counts placements by `created_at` date in range (list link: `createdFrom/createdTo`).
- Every report response has `"empty": true` when there are no rows, so the UI can show "No data
  for this period" (AS 8.5).
- Each row includes `link` query strings into the matching filtered list (FR-092 / research R13).
