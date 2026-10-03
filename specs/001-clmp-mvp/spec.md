# Feature Specification: CLMP MVP — Consultant-to-Placement Lifecycle

**Feature Branch**: `001-clmp-mvp`

**Created**: 2026-10-02

**Status**: Draft

**Input**: User description: "Create the first business specification for the Consultant Lifecycle Management Platform (CLMP): an internal staffing and recruiting application that manages the consultant lifecycle from bench/profile readiness through recruiter marketing, client/vendor submission, interview, placement, and operational reporting, for the roles ADMIN, MANAGER, RECRUITER, and HR_OPERATIONS, covering Login, Dashboard, Recruiters, Consultants, Marketing, Submissions, Placements, and Reports."

## Overview

CLMP gives a staffing organization one place to track every consultant from the moment their
profile is created on the bench, through readiness, recruiter marketing, client/vendor
submissions and interviews, to placement on a project. The MVP must let the organization
demonstrate the full consultant-to-placement workflow end to end across multiple pages, with
each role seeing and doing only what its responsibilities require.

### Roles at a Glance

| Capability | ADMIN | MANAGER | RECRUITER | HR_OPERATIONS |
|------------|-------|---------|-----------|---------------|
| Dashboard | Organization-wide | Organization-wide | Own work only | Consultant pipeline only |
| Recruiters: view | All | All | — | — |
| Recruiters: add / edit / activate / deactivate | Yes | — | — | — |
| Consultants: view | All | All | Assigned to them only | All |
| Consultants: add / edit profile, contact, skills, visa | Yes | — | — | Yes |
| Consultants: mark Ready / set Bench, Hold, Inactive, Active Project | Yes | — | — | Yes |
| Consultants: assign / reassign recruiter | Yes | Yes | — | — |
| Marketing assignments: view | All | All | Own consultants | — |
| Marketing assignments: create / update details / add notes | Yes | — | Own consultants | — |
| Marketing assignments: hold / reopen / close | Yes | Yes | Own consultants | — |
| Submissions: view | All | All | Own (see FR-063) | — |
| Submissions: create / update status / add notes | Yes | — | Own (see FR-063) | — |
| Placements: view | All | All | Own (see FR-073) | — |
| Placements: create from qualifying submission | Yes | — | Own submissions (submission's recruiter, see FR-070) | — |
| Placements: edit | Yes | — | — | — |
| Reports | Yes | Yes | — | — |

"—" means the area is not shown in navigation and the action is refused if attempted.

## Clarifications

### Session 2026-10-02

- Q: Is MANAGER visibility limited to one team or organization-wide? → A: Organization-wide read visibility across recruiters, consultants, marketing, submissions, placements, and reports; not limited to one team.
- Q: Does HR_OPERATIONS use the Placements workspace? → A: No. HR_OPERATIONS may see a consultant's current lifecycle status (including Placed / Active Project) on the consultant record, but has no access to the Placements workspace, placement records, or commercial placement details.
- Q: Should Managers create and update marketing assignments, submissions, and placements, or only view them and reassign consultants? → A: Managers view everything organization-wide, assign/reassign consultants, and may put marketing assignments on hold, reopen them, or close them; they cannot create or edit marketing assignment details, submissions, or placements.
- Q: Should cross-module workflow events change consultant and marketing status automatically? → A: Yes. Keep explicit automatic transitions where they reflect business rules — at minimum, a submission reaching Interview Scheduled moves the consultant to Interviewing, and creating a placement moves the consultant to Placed and closes the open marketing assignment — and every automatic transition records the user and triggering event that caused it, and when.

### Session 2026-10-02 (analysis remediation)

- Q: Which recruiter may create a placement, and who is the placement's recruiter? → A: ADMIN may create a placement from any qualifying (Offer) submission. A RECRUITER may create one only from a qualifying submission where they are the submission's recruiter. The placement's recruiter is always the submission's recruiter. MANAGER and ADMIN keep organization-wide visibility of placements.
- Q: Can a consultant on Hold be placed? → A: Yes, from an already-existing qualifying Offer submission. Creating the placement moves the consultant Hold → Placed and closes the open marketing assignment. New submissions remain blocked while the consultant is on Hold.
- Q: In which consultant status may a new marketing assignment be created? → A: Ready only.
- Q: How is "interviews scheduled" counted in the Vendor/Client Activity report? → A: As submissions whose current status is Interview Scheduled and whose submitted date falls within the selected reporting range, so the figure can be reproduced from the submission list with the same filters.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Sign In and Role-Appropriate Experience (Priority: P1)

A staff member signs in with their credentials and lands on a dashboard. The navigation and
available actions reflect their role; areas they are not entitled to are neither shown nor
reachable.

**Why this priority**: Every other story depends on knowing who the user is and what they may
see and do. Role restriction is also a core business rule.

**Independent Test**: Sign in as each of the four seeded demo users and confirm each lands on
their dashboard with only their permitted navigation items; attempt to open a forbidden page
directly and confirm access is refused.

**Acceptance Scenarios**:

1. **Given** an active user with valid credentials, **When** they sign in, **Then** they land on
   the dashboard and see navigation only for areas their role permits (per Roles at a Glance).
2. **Given** a user enters an incorrect username or password, **When** they submit, **Then**
   sign-in fails with a generic message that does not reveal which field was wrong.
3. **Given** a signed-in RECRUITER, **When** they navigate directly to the Recruiters or Reports
   page, **Then** access is refused with a "not authorized" message and no data from that area is
   shown.
4. **Given** a signed-in HR_OPERATIONS user, **When** they attempt to create a submission or
   marketing assignment by any means, **Then** the action is refused and nothing is recorded.
5. **Given** a signed-in user, **When** they sign out or remain inactive for 30 minutes, **Then**
   their session ends and they must sign in again to continue.
6. **Given** a user account that has been deactivated, **When** that user attempts to sign in,
   **Then** sign-in is refused.

---

### User Story 2 - Maintain Consultant Profiles and Readiness (Priority: P1)

HR Operations creates consultant profiles when consultants join the bench, maintains their
contact information, primary skill, experience, and visa information, and marks a consultant
Ready for marketing once the profile is complete. Every status change is recorded with who made
it and when.

**Why this priority**: Consultants are the core entity; nothing can be marketed, submitted, or
placed without a consultant who has reached Ready.

**Independent Test**: As HR_OPERATIONS, create a consultant (lands in Bench), complete the
profile, mark them Ready, and view the details page showing the status history with user and
timestamp for each change.

**Acceptance Scenarios**:

1. **Given** HR_OPERATIONS is on the Consultants page, **When** they add a consultant with the
   required fields, **Then** the consultant is created with status Bench and appears in the
   consultant list.
2. **Given** a Bench consultant missing a required readiness field (see FR-035), **When**
   HR_OPERATIONS tries to mark them Ready, **Then** the change is refused and the missing fields
   are listed.
3. **Given** a Bench consultant with a complete profile and an assigned active recruiter,
   **When** HR_OPERATIONS marks them Ready, **Then** the status becomes Ready and the history shows
   Bench → Ready with the HR user's name and the date/time.
4. **Given** a consultant list with many consultants, **When** a user searches by name or filters
   by status, primary skill, visa type, or assigned recruiter, **Then** only matching consultants
   are shown.
5. **Given** a consultant with an email address already used by another consultant, **When**
   HR_OPERATIONS saves the profile, **Then** the save is refused with a duplicate-email message.
6. **Given** HR_OPERATIONS sets a Ready consultant to Hold with a reason, **When** they view the
   details page, **Then** the status is Hold and the history records the reason, user, and time.

---

### User Story 3 - Manage Recruiters and Consultant Assignment (Priority: P1)

An Admin maintains the recruiter roster (team, region, active/inactive status) and assigns each
consultant to exactly one primary recruiter. Managers can reassign consultants to balance
workload. Inactive recruiters cannot receive new assignments.

**Why this priority**: Recruiter ownership determines who can market and submit a consultant and
drives workload and performance reporting.

**Independent Test**: As ADMIN, add a recruiter, assign a consultant to them, confirm the
recruiter's assigned consultant count increases; deactivate the recruiter and confirm a new
assignment to them is refused; as MANAGER, reassign the consultant to another active recruiter.

**Acceptance Scenarios**:

1. **Given** ADMIN is on the Recruiters page, **When** they add a recruiter with name, email,
   team, and region, **Then** the recruiter appears in the list as Active with an assigned
   consultant count of 0.
2. **Given** an active recruiter, **When** ADMIN assigns a consultant to them, **Then** the
   consultant shows that recruiter as their assigned recruiter and the recruiter's assigned
   consultant count increases by one.
3. **Given** a consultant already assigned to Recruiter A, **When** a MANAGER reassigns them to
   Recruiter B, **Then** Recruiter B becomes the only primary recruiter, Recruiter A's count
   decreases, Recruiter B's count increases, and the assignment history records the change, user,
   and time.
4. **Given** an inactive recruiter, **When** ADMIN or MANAGER tries to assign any consultant to
   them, **Then** the assignment is refused with a message that the recruiter is inactive.
5. **Given** a recruiter with assigned consultants, **When** ADMIN deactivates the recruiter,
   **Then** the system warns how many consultants are still assigned, the deactivation proceeds
   on confirmation, and those consultants are flagged "needs reassignment" in the consultant list
   and dashboard.
6. **Given** the recruiter list, **When** ADMIN or MANAGER filters by team, region, or status,
   **Then** only matching recruiters are shown, each with their current assigned consultant count.

---

### User Story 4 - Market a Ready Consultant (Priority: P2)

A recruiter starts a marketing assignment for a Ready consultant assigned to them, sets a start
and target date, keeps notes on marketing activity, and can put marketing on hold, reopen it, or
close it.

**Why this priority**: Marketing is the bridge between a Ready consultant and client/vendor
submissions.

**Independent Test**: As RECRUITER, create a Draft marketing assignment for a Ready consultant,
activate it (consultant becomes Marketing), add a note, place it on Hold, reopen it, and close
it (consultant returns to Ready).

**Acceptance Scenarios**:

1. **Given** a Ready consultant assigned to the recruiter, **When** the recruiter creates a
   marketing assignment, **Then** it is created in Draft with the consultant, the recruiter and
   their team as owner, a start date, and a target date.
2. **Given** a Draft marketing assignment for a Ready consultant, **When** the recruiter activates
   it, **Then** the assignment becomes Active and the consultant's status becomes Marketing, both
   changes recorded with user and time.
3. **Given** a consultant who is Bench, Hold, or Inactive, **When** anyone tries to activate a
   marketing assignment for them, **Then** activation is refused with a message that the
   consultant must be Ready.
4. **Given** a consultant who already has a Draft, Active, or Hold marketing assignment, **When**
   a user tries to create another, **Then** creation is refused and the existing assignment is
   referenced.
5. **Given** an Active assignment, **When** the recruiter puts it on Hold with a reason, **Then**
   the assignment status becomes Hold and the reason is recorded; **When** they later reopen it,
   **Then** it returns to Active.
6. **Given** an Active or Hold assignment, **When** the recruiter closes it with a reason and the
   consultant has no placement, **Then** the assignment becomes Closed and the consultant returns
   to Ready.
7. **Given** a RECRUITER, **When** they view the marketing list, **Then** they see only
   assignments for consultants currently assigned to them; overdue assignments (past target date
   and not Closed) are visibly flagged.
8. **Given** a signed-in MANAGER, **When** they view the marketing list, **Then** they see
   assignments for all teams; they can put an Active assignment on Hold, reopen a Hold or Closed
   assignment, or close one (each with the reason recorded), but an attempt to create an
   assignment or edit its dates or notes is refused.

---

### User Story 5 - Submit Consultants and Track Progress (Priority: P2)

A recruiter submits a consultant to a vendor/client for a specific job, records the bill rate,
and advances the submission through review and interviews, adding notes along the way. The
system warns about duplicate submissions and keeps a full status timeline.

**Why this priority**: Submissions are where revenue opportunities are created and tracked; they
are prerequisite to placements.

**Independent Test**: As RECRUITER, create a submission for a consultant in Marketing, move it
Submitted → Under Review → Interview Scheduled (consultant becomes Interviewing) → Interview
Cleared → Offer, add notes, and view the timeline; then create a second submission for the same
consultant, vendor, client, and job title and confirm the duplicate warning.

**Acceptance Scenarios**:

1. **Given** a consultant in Ready, Marketing, or Interviewing status who is assigned to the
   recruiter, **When** the recruiter creates a submission with vendor, client, job title, and bill
   rate, **Then** it is saved as Draft (or as Submitted with a submitted date if submitted
   immediately).
2. **Given** a consultant in Bench, Hold, Inactive, Placed, or Active Project status, **When** a
   user tries to create a submission for them, **Then** creation is refused with a message
   explaining the consultant is not eligible.
3. **Given** an existing submission for the same consultant, vendor, client, and job title,
   **When** the recruiter creates another, **Then** the system shows a warning listing the
   earlier submission(s) with their status and date; the recruiter may cancel or confirm, and a
   confirmed duplicate is saved with the acknowledgement recorded.
4. **Given** a submission in any non-terminal status, **When** the recruiter changes the status,
   **Then** only the transitions allowed in FR-058 are offered, and each change is added to the
   timeline with old status, new status, user, time, and optional note.
5. **Given** a consultant in Ready or Marketing status, **When** one of their submissions moves
   to Interview Scheduled, **Then** the consultant's status becomes Interviewing automatically,
   and the consultant's history shows the change attributed to the user who updated the
   submission, with the triggering submission and the date/time.
6. **Given** a consultant in Interviewing whose last submission in an interview or offer stage is
   Rejected or Withdrawn, **When** that change is saved, **Then** the consultant returns to
   Marketing if they still have an Active marketing assignment, otherwise to Ready.
7. **Given** a submission, **When** a user adds a note, **Then** the note is shown in the
   submission details with author and time, and notes cannot be edited or deleted afterwards.
8. **Given** the submission list, **When** a user filters by status, recruiter, vendor, client, or
   submitted date range, **Then** only matching submissions are shown.
9. **Given** a signed-in MANAGER, **When** they open any submission in the organization, **Then**
   they can view its details, timeline, and notes, but attempts to create a submission, change its
   status, add a note, or create a placement from it are refused.

---

### User Story 6 - Convert an Offer into a Placement (Priority: P3)

When a submission reaches Offer, the submission's recruiter or an Admin converts it into a
placement, confirming start date, bill rate, and contract term. The consultant becomes Placed, and
later Active Project once they start. Managers can view placements organization-wide but cannot
create or edit them.

**Why this priority**: Placement completes the lifecycle and is the key business outcome, but it
depends on stories 2–5.

**Independent Test**: Convert a submission in Offer to a placement; verify the placement details
carry over consultant, recruiter, vendor, client, and bill rate; verify the submission becomes
Placed, the consultant becomes Placed, and the marketing assignment is Closed; then mark the
consultant Active Project.

**Acceptance Scenarios**:

1. **Given** a submission in Offer, **When** an authorized user chooses "Create Placement",
   **Then** a placement form is pre-filled with consultant, recruiter, vendor, client, job title,
   and bill rate, and requires a start date and contract term.
2. **Given** the placement is saved, **When** the user views the records, **Then** the submission
   status is Placed, the consultant status is Placed, the consultant's open marketing assignment
   is Closed with reason "Placed", and all three changes are recorded with the user, the
   triggering placement, and the date/time.
3. **Given** a submission in any status other than Offer, **When** any user attempts to create a
   placement from it, **Then** the action is refused with a message that only submissions at
   Offer qualify.
4. **Given** a placement has been created, **When** the consultant has other open submissions,
   **Then** the user is shown those open submissions with an option to withdraw them; they are
   not changed automatically.
5. **Given** a consultant in Placed status, **When** ADMIN or HR_OPERATIONS marks them Active
   Project on or after the placement start date, **Then** the consultant's status becomes Active
   Project and the change is recorded.
6. **Given** a RECRUITER, **When** they view the placement list, **Then** they see only placements
   where they are the placement's recruiter.
7. **Given** a signed-in HR_OPERATIONS user, **When** they open a consultant who has been placed,
   **Then** they see the consultant's status (Placed or Active Project) and status history, but no
   Placements navigation item, placement record, vendor, client, bill rate, or contract term; a
   direct attempt to open the Placements workspace or a placement record is refused.

---

### User Story 7 - Operational Dashboard (Priority: P3)

Each user lands on a dashboard summarizing the operation within their access scope: bench and
ready counts, active submissions, interviews, placements, recent activity, and (for Admin and
Manager) a recruiter performance summary.

**Why this priority**: The dashboard makes the workflow visible at a glance but adds no new data
of its own.

**Independent Test**: Seed data across statuses; sign in as each role and verify the dashboard
figures match the corresponding filtered lists for that role's scope.

**Acceptance Scenarios**:

1. **Given** ADMIN or MANAGER signs in, **When** the dashboard loads, **Then** it shows
   organization-wide counts of Bench consultants, Ready consultants, active submissions,
   submissions in interview stages, placements in the current month, the 20 most recent activity
   events, a recruiter performance summary, and the count of consultants needing reassignment.
   For MANAGER, every figure covers all teams and regions, not only one team.
2. **Given** RECRUITER signs in, **When** the dashboard loads, **Then** every figure and activity
   event is limited to consultants assigned to them and submissions/placements they own, and no
   other recruiter's performance is shown.
3. **Given** HR_OPERATIONS signs in, **When** the dashboard loads, **Then** it shows consultant
   counts by status and recent consultant profile and status activity, with no submission bill
   rates or recruiter performance.
4. **Given** a dashboard count, **When** the user selects it, **Then** they are taken to the
   corresponding list pre-filtered to the same records, and the counts match.

---

### User Story 8 - Operational Reports (Priority: P4)

Admins and managers view simple operational reports — submissions by recruiter, placements by
recruiter, consultant pipeline, bench and ready counts, and vendor/client activity — for a
chosen date range.

**Why this priority**: Reports support management decisions but are not required to run the
day-to-day workflow.

**Independent Test**: With seeded data, open each report for a date range and verify its totals
against the underlying submission, placement, and consultant lists.

**Acceptance Scenarios**:

1. **Given** ADMIN or MANAGER opens Submissions by Recruiter for a date range, **When** the report
   loads, **Then** it shows, per recruiter, the number of submissions submitted in that range and
   a breakdown by current status.
2. **Given** Placements by Recruiter for a date range, **When** the report loads, **Then** it
   shows, per recruiter, the number of placements created in that range.
3. **Given** the Consultant Pipeline report, **When** it loads, **Then** it shows the current
   count of consultants in each consultant status, including Bench and Ready counts.
4. **Given** Vendor/Client Activity for a date range, **When** it loads, **Then** it shows, per
   vendor and per client, the number of submissions, interviews scheduled, and placements.
5. **Given** a report with no matching data, **When** it loads, **Then** it shows an explicit
   "no data for this period" state rather than an error or blank page.

---

### Edge Cases

- **Recruiter deactivated with active work**: Existing marketing assignments and submissions are
  not altered; affected consultants are flagged "needs reassignment" until an Admin or Manager
  reassigns them.
- **Consultant reassigned mid-marketing**: Ownership of the consultant's open marketing
  assignment transfers to the new recruiter; existing submissions keep the original recruiter for
  reporting credit, but the new recruiter can also view and update them.
- **Consultant put on Hold during marketing**: Any Active marketing assignment moves to Hold
  automatically (FR-032, FR-036); open submissions are left as is and flagged on the consultant
  details page. A consultant on Hold may still be placed from an existing Offer submission
  (FR-032, FR-070), but cannot receive new submissions (FR-052).
- **Consultant set to Inactive**: Refused while the consultant has open submissions (any status
  other than Rejected, Withdrawn, or Placed); any open marketing assignment is closed with reason
  "Consultant inactive".
- **Two offers for one consultant**: Once a consultant has a placement, any further attempt to
  create a placement for them is refused while they are Placed or Active Project.
- **Duplicate detection matching**: Vendor, client, and job title are compared ignoring case and
  leading/trailing spaces; Withdrawn and Rejected earlier submissions still trigger the warning.
- **Dates**: A marketing target date before its start date, a submitted date in the future, or a
  placement start date before the submission's submitted date is refused.
- **Concurrent edits**: If two users update the same record, the second save is refused with a
  message to reload, so no status change is silently lost.
- **Recruiter user without a recruiter profile**: A RECRUITER user not linked to a recruiter
  profile sees an empty-state dashboard and lists with a message to contact an Admin.
- **Empty states**: Every list, dashboard panel, and report shows a clear message when no
  records match, rather than a blank area.

## Requirements *(mandatory)*

### Functional Requirements

#### Access and Sign-In

- **FR-001**: System MUST require users to sign in with a username and password before accessing
  any page or data.
- **FR-002**: System MUST assign each user exactly one role: ADMIN, MANAGER, RECRUITER, or
  HR_OPERATIONS.
- **FR-003**: System MUST show only the navigation items and actions permitted for the user's
  role, as defined in Roles at a Glance.
- **FR-004**: System MUST enforce all role and ownership restrictions on every read and every
  action, independently of what the interface shows; refused attempts MUST change nothing and
  return a "not authorized" outcome.
- **FR-005**: System MUST show a generic failure message on unsuccessful sign-in and MUST refuse
  sign-in for deactivated user accounts.
- **FR-006**: System MUST let users sign out and MUST end sessions after 30 minutes of
  inactivity.
- **FR-007**: System MUST never display passwords or other credentials, and MUST NOT include them
  in any record history or activity feed.

#### Recruiters

- **FR-010**: ADMIN MUST be able to add and edit recruiters with: full name, work email (unique),
  phone (optional), team, region, and status (Active/Inactive).
- **FR-011**: Team and region MUST be selected from predefined lists.
- **FR-012**: The recruiter list MUST show name, team, region, status, and assigned consultant
  count (the number of consultants for whom the recruiter is the current primary recruiter), and
  MUST support search by name and filtering by team, region, and status.
- **FR-013**: ADMIN and MANAGER MUST be able to view the recruiter list and recruiter details;
  RECRUITER and HR_OPERATIONS MUST NOT.
- **FR-016**: MANAGER visibility MUST be organization-wide: a MANAGER MUST be able to view all
  recruiters, consultants, marketing assignments, submissions, placements, dashboard figures, and
  reports across every team and region, without any team-based filtering being imposed.
- **FR-017**: MANAGER actions MUST be limited to: assigning/reassigning a consultant's primary
  recruiter (FR-033) and putting a marketing assignment on hold, reopening it, or closing it
  (FR-041). A MANAGER MUST NOT create marketing assignments, edit their details or notes, create or
  update submissions or submission notes, or create or edit placements; such attempts MUST be
  refused.
- **FR-014**: When ADMIN deactivates a recruiter who has assigned consultants, the system MUST warn
  with the number of affected consultants and require confirmation, then flag those consultants
  as "needs reassignment".
- **FR-015**: A recruiter profile MUST be linkable to one RECRUITER user account so that the
  user's "own" scope can be determined.

#### Consultants

- **FR-020**: ADMIN and HR_OPERATIONS MUST be able to add and edit consultants with: first and
  last name, email (unique), phone, location (city/state), primary skill/technology, additional
  skills (optional), years of experience (0–50), visa type, visa expiration date (optional), and
  notes.
- **FR-021**: Visa type MUST be chosen from a predefined list (e.g., US Citizen, Green Card,
  H-1B, H-4 EAD, L-2 EAD, OPT, STEM OPT, CPT, TN, Other).
- **FR-022**: New consultants MUST start in Bench status.
- **FR-023**: The consultant list MUST show name, primary skill, years of experience, visa type,
  assigned recruiter, current status, and a "needs reassignment" flag where applicable, and MUST
  support search by name and filtering by status, primary skill, visa type, and assigned
  recruiter.
- **FR-024**: The consultant list MUST NOT show phone, email, visa expiration date, or notes;
  these appear only on the consultant details page.
- **FR-025**: The consultant details page MUST show the full profile, current status, assigned
  recruiter, current marketing assignment (if any), related submissions and placements the
  viewer is permitted to see, and the consultant's status and assignment history.
- **FR-026**: RECRUITER users MUST be able to view only consultants currently assigned to them and
  MUST NOT be able to edit consultant profile fields.
- **FR-027**: The system MUST NOT collect or store Social Security numbers, dates of birth, bank
  details, or identity document images.
- **FR-028**: Consultants MUST NOT be hard-deleted; they are retired by setting status Inactive.

#### Consultant Status and Assignment

- **FR-030**: The consultant statuses MUST be exactly: Bench, Ready, Marketing, Interviewing,
  Placed, Active Project, Hold, Inactive.
- **FR-031**: Manual consultant status changes MUST be limited to ADMIN and HR_OPERATIONS and to
  these transitions:
  - Bench → Ready (subject to FR-035)
  - Ready → Bench
  - Placed → Active Project (on or after placement start date)
  - Active Project → Bench (project ended)
  - Bench, Ready, Marketing, or Interviewing → Hold (reason required)
  - Hold → Bench or Ready (Ready subject to FR-035)
  - Any status → Inactive (subject to the Inactive edge case; reason required)
  - Inactive → Bench
- **FR-032**: The following status changes MUST happen automatically as a result of workflow
  events, in the same action as the triggering event, and the resulting consultant statuses
  (Marketing, Interviewing, Placed) MUST NOT be set manually:
  - Consultant Ready → Marketing when a marketing assignment is activated or reopened
  - Consultant Marketing → Ready when the marketing assignment is closed without a placement
  - Consultant Ready or Marketing → Interviewing when one of the consultant's submissions moves to
    Interview Scheduled (no change if already Interviewing)
  - Consultant Interviewing → Marketing (or Ready, if no Active marketing assignment) when the
    consultant no longer has any submission in Interview Scheduled, Interview Cleared, or Offer
  - Consultant Ready, Marketing, Interviewing, or Hold → Placed when a placement is created
  - Marketing assignment Draft, Active, or Hold → Closed (reason "Placed") when a placement is
    created for the consultant
  - Marketing assignment Active → Hold when the consultant is set to Hold
  - Marketing assignment Draft, Active, or Hold → Closed (reason "Consultant inactive") when the
    consultant is set to Inactive
- **FR-036**: Every automatic transition in FR-032 MUST be recorded in history (FR-100) as
  system-triggered, identifying the user whose action caused it, the triggering event and record
  (e.g., "Submission for Acme / Java Developer moved to Interview Scheduled"), and the date/time.
- **FR-033**: ADMIN and MANAGER MUST be able to assign or reassign a consultant's primary
  recruiter; a consultant MUST have at most one primary recruiter at any time.
- **FR-034**: The system MUST refuse assigning a consultant to an Inactive recruiter.
- **FR-035**: A consultant MUST NOT become Ready unless they have: first and last name, email,
  phone, primary skill, years of experience, visa type, and an assigned Active recruiter. A
  refused change MUST list the missing items.

#### Marketing

- **FR-040**: ADMIN and the consultant's assigned RECRUITER MUST be able to create and
  update marketing assignments with: consultant, owning recruiter (defaults to the consultant's
  assigned recruiter), owning team (derived from the recruiter), start date, target date, status,
  and notes. A new marketing assignment MUST only be created for a consultant in Ready status.
- **FR-041**: Marketing statuses MUST be exactly: Draft, Active, Hold, Closed. ADMIN and the
  consultant's assigned RECRUITER may perform all transitions below; MANAGER may perform only the
  hold, reopen, and close transitions. Allowed transitions:
  - Draft → Active (activation; consultant must be Ready; not MANAGER)
  - Draft → Closed
  - Active → Hold (reason required; the consultant's own status is not changed by a marketing
    hold)
  - Hold → Active (reopen; consultant must be Ready, Marketing, or Interviewing — a consultant
    who is on Hold or Inactive must first be returned to Ready; a Ready consultant becomes
    Marketing on reopen)
  - Active or Hold → Closed (reason required)
  - Closed → Active (reopen; ADMIN or MANAGER only, not RECRUITER; consultant must be Ready; not allowed if the
    assignment was closed with reason "Placed")
- **FR-042**: A consultant MUST have at most one marketing assignment in Draft, Active, or Hold at a
  time.
- **FR-043**: Target date MUST be on or after start date; assignments past their target date and
  not Closed MUST be flagged as overdue in the list.
- **FR-044**: The marketing list MUST show consultant, owning recruiter, team, start date, target
  date, status, and overdue flag, and MUST support filtering by status, recruiter, and team.
- **FR-045**: Marketing notes MUST be append-only and record author and time.

#### Submissions

- **FR-050**: ADMIN and the consultant's assigned RECRUITER MUST be able to create
  submissions with: consultant, recruiter (defaults to the consultant's assigned recruiter),
  vendor, client, job title, submitted date, bill rate, status, and notes.
- **FR-051**: Vendors and clients MUST be selectable from existing names or added by name during
  submission entry, so the same vendor or client is counted consistently in reports.
- **FR-052**: A submission MUST only be created for a consultant whose status is Ready, Marketing,
  or Interviewing.
- **FR-053**: Bill rate MUST be a positive hourly amount in US dollars.
- **FR-054**: Submitted date MUST be required once a submission leaves Draft, MUST default to the
  date it moved to Submitted, and MUST NOT be in the future.
- **FR-055**: When a submission is created for a consultant who already has a submission with the
  same vendor, client, and job title (case- and whitespace-insensitive, any status), the system
  MUST warn, list the earlier submission(s), and require explicit confirmation to proceed; the
  confirmation MUST be recorded on the new submission.
- **FR-056**: Submission statuses MUST be exactly: Draft, Submitted, Under Review, Interview
  Scheduled, Interview Cleared, Rejected, Offer, Placed, Withdrawn.
- **FR-057**: Rejected, Withdrawn, and Placed MUST be terminal; no further status changes are
  allowed.
- **FR-058**: Allowed submission transitions MUST be:
  - Draft → Submitted, Withdrawn
  - Submitted → Under Review, Interview Scheduled, Rejected, Withdrawn
  - Under Review → Interview Scheduled, Rejected, Withdrawn
  - Interview Scheduled → Interview Cleared, Rejected, Withdrawn
  - Interview Cleared → Interview Scheduled (next round), Offer, Rejected, Withdrawn
  - Offer → Rejected (offer declined), Withdrawn
  - Offer → Placed (only by creating a placement, FR-070)
- **FR-059**: Each submission MUST have a status timeline showing every status change with old
  status, new status, user, date/time, and optional note.
- **FR-060**: Submission notes MUST be append-only and record author and time.
- **FR-061**: The submission list MUST show consultant, recruiter, vendor, client, job title,
  submitted date, bill rate, and status, and MUST support filtering by status, recruiter, vendor,
  client, and submitted date range.
- **FR-062**: The submission details page MUST show all submission fields, the status timeline,
  notes, and any duplicate acknowledgement.
- **FR-063**: A RECRUITER MUST be able to view and update only submissions where they are the
  submission's recruiter or the consultant is currently assigned to them.
- **FR-064**: HR_OPERATIONS MUST NOT see submission records, placement records, vendors, clients,
  bill rates, or contract terms; on consultant details they see only the consultant's profile,
  current lifecycle status, and status history.

#### Placements

- **FR-070**: A placement MUST only be created from a submission in Offer status, by ADMIN
  or by the RECRUITER who is that submission's recruiter. The placement's recruiter MUST be the
  submission's recruiter.
- **FR-071**: A placement MUST record: source submission, consultant, recruiter, vendor, client,
  job title, start date, bill rate (pre-filled from the submission, editable), and contract term
  in months (whole number 1–60); expected end date MUST be shown as start date plus term.
- **FR-072**: Creating a placement MUST, as one action: set the submission to Placed, set the
  consultant to Placed, close the consultant's open (Draft, Active, or Hold) marketing assignment
  with reason "Placed", and record each change as described in FR-036.
- **FR-073**: The placement list MUST show consultant, recruiter, client, vendor, start date, bill
  rate, and contract term, and MUST support filtering by recruiter, client, vendor, and start
  date range. RECRUITER users MUST see only placements where they are the placement's recruiter.
- **FR-074**: ADMIN MUST be able to edit a placement's start date, bill rate, and
  contract term; each edit MUST be recorded with user and time. An edited start date MUST NOT be
  earlier than the source submission's submitted date.
- **FR-075**: A consultant in Placed or Active Project status MUST NOT receive another placement.
- **FR-076**: After a placement is created, the system MUST show the consultant's other open
  submissions and allow the user to withdraw them; it MUST NOT change them automatically.
- **FR-077**: HR_OPERATIONS MUST NOT have access to the Placements workspace: no navigation item,
  no placement list or details, and no placement actions; direct attempts MUST be refused.

#### Dashboard

- **FR-080**: The dashboard MUST show, within the user's scope: count of Bench consultants, count
  of Ready consultants, count of active submissions (status Submitted, Under Review, Interview
  Scheduled, Interview Cleared, or Offer), count of submissions in Interview Scheduled or
  Interview Cleared, count of placements created in the current calendar month, and the 20 most
  recent activity events.
- **FR-081**: ADMIN and MANAGER dashboards MUST additionally show a recruiter performance summary
  (per active recruiter: assigned consultants, active submissions, interviews, and placements in
  the current month) and the number of consultants needing reassignment.
- **FR-082**: RECRUITER dashboards MUST be limited to their own consultants, submissions, and
  placements; HR_OPERATIONS dashboards MUST show consultant counts by status and consultant
  profile and status activity only, with no submission, placement, vendor, client, or rate
  details (a placement appears to HR_OPERATIONS only as the consultant's status change to
  Placed).
- **FR-083**: Each dashboard count MUST link to the corresponding list pre-filtered to the same
  records.

#### Reports

- **FR-090**: ADMIN and MANAGER MUST be able to view these reports: Submissions by Recruiter,
  Placements by Recruiter, Consultant Pipeline, Bench and Ready Counts, and Vendor/Client
  Activity.
- **FR-091**: Submissions by Recruiter, Placements by Recruiter, and Vendor/Client Activity MUST
  accept a date range, defaulting to the current calendar month; Consultant Pipeline and Bench and
  Ready Counts MUST reflect current status. In Vendor/Client Activity, submissions and interviews
  scheduled are counted by submitted date within the range (interviews scheduled = submissions
  currently in Interview Scheduled), and placements by placement creation date within the range.
- **FR-092**: Report totals MUST agree with the corresponding filtered lists for the same criteria.
- **FR-093**: Reports MUST be viewable on screen; export to files is out of scope.

#### History, Activity, and Data Protection

- **FR-100**: Every status change to a consultant, marketing assignment, or submission, every
  recruiter assignment change, and every placement creation or edit MUST be recorded with the
  previous value, new value, the user who made it, and the date/time; automatic changes MUST
  additionally be marked system-triggered and identify the triggering event and record
  (FR-036).
- **FR-101**: History records MUST be read-only for all users.
- **FR-102**: Recent activity MUST be derived from these history records and filtered to what the
  viewing user is permitted to see.
- **FR-103**: Consultant phone, email, visa expiration date, and notes MUST be visible only to
  ADMIN, MANAGER, HR_OPERATIONS, and the consultant's assigned RECRUITER, and only on the details
  page.
- **FR-104**: All user-entered required fields MUST be validated with clear field-level messages
  before saving; invalid saves MUST change nothing.

### Key Entities

- **User**: A person who signs in. Username, display name, role, active/inactive. Optionally
  linked to one Recruiter profile.
- **Recruiter**: A recruiting staff member who owns consultants. Name, work email, phone, team,
  region, status (Active/Inactive), linked user, derived assigned consultant count.
- **Team / Region**: Predefined reference values used to group recruiters and their work.
- **Consultant**: A person being prepared for and placed on projects. Name, contact details,
  location, primary and additional skills, years of experience, visa type and expiration, current
  status, current primary recruiter, "needs reassignment" flag, notes.
- **Recruiter Assignment**: The link between a consultant and their primary recruiter over time;
  at most one current assignment per consultant.
- **Marketing Assignment**: A period of active marketing for one consultant. Consultant, owning
  recruiter and team, start date, target date, status (Draft/Active/Hold/Closed), hold/close
  reasons, notes.
- **Vendor**: An intermediary staffing firm a consultant is submitted through. Name.
- **Client**: The end company for whom the job exists. Name.
- **Submission**: A consultant put forward for a specific job. Consultant, recruiter, vendor,
  client, job title, submitted date, bill rate, status, duplicate acknowledgement, notes,
  timeline.
- **Placement**: The confirmed engagement resulting from a submission at Offer. Source submission,
  consultant, recruiter, vendor, client, job title, start date, bill rate, contract term, expected
  end date.
- **Note**: Append-only text attached to a marketing assignment or submission, with author and
  time.
- **History Record**: An immutable record of a change: entity, field/status, previous value, new
  value, user, date/time, optional reason, and — for automatic changes — a system-triggered marker
  and the triggering event and record.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can complete the full lifecycle — create a consultant, mark Ready, assign a
  recruiter, start marketing, create a submission, advance it to Offer, and create a placement —
  across the application's pages in under 15 minutes using only seeded reference data.
- **SC-002**: 100% of attempts to view or act on data outside a role's permissions (per Roles at a
  Glance) are refused, including attempts that bypass the visible navigation.
- **SC-003**: 100% of consultant, marketing, and submission status changes, recruiter assignment
  changes, and placement creations show who made the change and when; 100% of automatic changes
  also show the triggering event.
- **SC-004**: 100% of attempts to break the core business rules (assign to an inactive recruiter,
  market or submit a non-Ready consultant, create a placement from a non-Offer submission, give a
  consultant two primary recruiters) are refused with an explanatory message.
- **SC-005**: Every duplicate submission (same consultant, vendor, client, and job title) triggers
  a warning before it is saved.
- **SC-006**: Dashboard and report figures match the corresponding filtered lists exactly for every
  role tested.
- **SC-007**: With 500 consultants, 50 recruiters, and 2,000 submissions loaded, lists, dashboard,
  and reports display within 2 seconds for a user on the internal network.
- **SC-008**: A recruiter new to the system can create a correct submission on their first attempt
  in under 3 minutes without assistance.

## Assumptions

- **Single organization**: The MVP serves one organization (MANAGER visibility is organization-wide
  per FR-016).
- **User accounts are seeded**: Demo users for each role (and their linked recruiter profiles) are
  pre-provisioned; self-registration, password reset, user administration screens, and single
  sign-on are out of scope. Adding a recruiter profile does not create a login account.
- **Reference lists are seeded**: Teams, regions, and visa types are predefined; screens to maintain
  them are out of scope.
- **Readiness**: "Readiness for marketing" is represented by the consultant reaching Ready status
  under the completeness rule in FR-035, rather than a separate checklist.
- **Qualifying submission**: A submission qualifies for placement only at Offer.
- **Currency and rates**: Bill rates are hourly in US dollars; pay rates, margins, and invoicing are
  out of scope.
- **Placement lifecycle**: Placements have no status workflow of their own in the MVP; placement
  cancellation, extension, and project-end tracking beyond setting the consultant back to Bench
  are out of scope.
- **Interviews**: Interviews are tracked through submission statuses and notes; separate interview
  scheduling records (date, panel, feedback forms) are out of scope.
- **No deletion**: Records are not hard-deleted in the MVP; inactive/closed/withdrawn states are
  used instead.
- **Out of scope** (per request): incentive payout engine, document/resume storage, payroll,
  external job-board integration, email or notification automation, advanced analytics, report
  export, infrastructure and deployment concerns, and unrelated enterprise features.
