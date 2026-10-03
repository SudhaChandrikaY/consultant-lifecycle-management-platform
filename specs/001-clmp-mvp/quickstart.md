# Quickstart & Validation Guide: CLMP MVP

**Feature**: `001-clmp-mvp` | **Plan**: [plan.md](./plan.md)

This guide covers how to build and run the MVP and how to prove the spec's acceptance scenarios
and success criteria end to end. Request and response shapes are in
[contracts/rest-api.md](./contracts/rest-api.md). Role rules are in
[contracts/authorization-matrix.md](./contracts/authorization-matrix.md). Entity rules are in
[data-model.md](./data-model.md).

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| JDK | 21 (Temurin or equivalent) | `java -version` |
| Node.js | 22 LTS (≥ 20.19) | `node -v` |
| npm | ≥ 10 | `npm -v` |

Maven doesn't need to be installed because the backend ships the Maven Wrapper (`./mvnw`).

> **Current environment gap**: The WSL shell used for planning has **no JDK** and **Node
> v12.22**. Install JDK 21 and Node 22 (for example through `sdkman` and `nvm`) before running
> the implementation phase, otherwise neither build can run.

## Build & test

```bash
# Backend
cd backend
./mvnw verify            # compiles, runs unit + integration tests (H2 in-memory)

# Frontend
cd frontend
npm ci
npm run typecheck        # tsc --noEmit
npm test -- --run        # Vitest
npm run build            # production bundle (Vite)
```

Constitution gate: both builds and all tests MUST pass before a change is considered done.

## Run locally

```bash
# Terminal 1 — API on :8080, H2 in-memory, demo data seeded at startup
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Terminal 2 — SPA on :5173, proxies /api → :8080
cd frontend && npm run dev
```

Open `http://localhost:5173`. Optional profiles: `file` (data persists in `backend/data/`) and
`perf` (large seed for SC-007).

### Seeded demo users (`dev` profile only)

All demo users share the password `Demo@123`.

| Username | Role | Notes |
|----------|------|-------|
| `admin` | ADMIN | |
| `manager` | MANAGER | Organization-wide view |
| `recruiter1` | RECRUITER | Linked to recruiter "Riya Patel" (Java team) |
| `recruiter2` | RECRUITER | Linked to recruiter "Marcus Lee" (Data team) |
| `recruiter3` | RECRUITER | **Not linked**, so empty-state experience |
| `hr` | HR_OPERATIONS | |
| `inactive.user` | RECRUITER | Deactivated, so sign-in refused |

## Validation scenarios

Run these against the running app. Each one maps to spec acceptance scenarios (AS x.y) or success
criteria. Automated integration tests cover the same scenarios (research R16). The manual pass
confirms the UI wiring.

### V1 — Sign-in and role navigation (US1)

1. Sign in as each user and check that the nav items match the
   [navigation table](./contracts/authorization-matrix.md#frontend-navigation-ux-only-mirrors-the-matrix)
   (AS 1.1).
2. Sign in with a wrong password, then with an unknown username. Both should show the **same**
   generic message (AS 1.2).
3. Sign in as `inactive.user`. Sign-in should be refused (AS 1.6).
4. As `recruiter1`, browse to `/recruiters` and `/reports`. Both should show "Not authorized" with
   no data (AS 1.3).
5. As `hr`, call `POST /api/submissions` directly, for example from the browser devtools with the
   CSRF header. It should return `403 NOT_AUTHORIZED`, and the submissions count should not change
   (AS 1.4).
6. Sign out, then go Back. The browser should redirect to `/login`. To test the idle timeout, run
   with `server.servlet.session.timeout=1m`, wait, and click any link; it should redirect to
   `/login?expired=1` (AS 1.5).

### V2 — Consultant profile and readiness (US2)

1. As `hr`, add a consultant with only first name, last name, and email. The status should be
   **Bench** (AS 2.1).
2. Click **Mark Ready**. The change should be refused, listing phone, primary skill, years of
   experience, visa type, and the assigned active recruiter (AS 2.2).
3. Fill in the profile. As `admin`, assign it to Riya Patel. As `hr`, click **Mark Ready**. The
   status should be **Ready**, and the history should show "Bench → Ready · <HR name> · <time>"
   (AS 2.3).
4. Save another consultant using the same email. It should be refused with a duplicate-email
   message (AS 2.5).
5. Set Hold with a reason. The history should record the reason, user, and time (AS 2.6).
6. Filter the list by status, skill, visa, and recruiter, and search by name (AS 2.4). The list
   should never show email or phone (FR-024).

### V3 — Recruiters and assignment (US3)

1. As `admin`, add a recruiter. They should be Active with count 0 (AS 3.1). Assign a consultant;
   the count should become 1 (AS 3.2).
2. As `manager`, reassign that consultant to another recruiter. The counts should move, and the
   history should show the change (AS 3.3).
3. As `admin`, deactivate a recruiter who has consultants. A dialog should show the affected count.
   After you confirm, those consultants should show **Needs reassignment** in the list and on the
   dashboard (AS 3.5).
4. Try to assign a consultant to the inactive recruiter. It should be refused with "recruiter is
   inactive" (AS 3.4).

### V4 — Marketing (US4)

1. As `recruiter1`, create marketing for one of your own Ready consultants. It should be **Draft**,
   with you and your team as owner (AS 4.1). Activate it. The assignment should be **Active** and
   the consultant **Marketing** (AS 4.2).
2. Try to create a second assignment for the same consultant. It should be refused, with a link to
   the existing one (AS 4.4).
3. Put it on Hold with a reason, then reopen it (AS 4.5). Close it with a reason. The consultant
   should return to **Ready** (AS 4.6).
4. As `manager`, check that Hold, Reopen, and Close are offered but Create and Edit dates/notes are
   not. A direct `PUT` should return `403` (AS 4.8).
5. An assignment whose target date is in the past should show the **Overdue** flag (AS 4.7).

### V5 — Submissions (US5)

1. As `recruiter1`, submit a Marketing consultant to vendor "Acme Staffing" (new), client "Globex"
   (new), and job title "Java Developer" at $85/hr. The submission should be **Submitted** with
   today's date (AS 5.1).
2. Repeat with " acme staffing ", "GLOBEX", and "java developer". A duplicate warning should list
   the earlier submission. After you confirm, the new submission's details should show the
   acknowledgement (AS 5.3, SC-005).
3. Move the submission through Under Review and then Interview Scheduled. The consultant should
   become **Interviewing**, and the history entry should be marked system-triggered with the
   submission named (AS 5.5).
4. Add a note. There should be no edit or delete control, and the timeline should show every
   change (AS 5.4, AS 5.7).
5. Reject the only interview-stage submission. The consultant should return to Marketing, or to
   Ready if no Active marketing assignment exists (AS 5.6).
6. Try to submit a Bench consultant. It should be refused (AS 5.2).
7. As `manager`, open the submission. You should be able to view it but get no actions, and direct
   POSTs should return `403` (AS 5.9).

### V6 — Placement (US6)

1. As `recruiter1`, advance a submission to **Offer** and click **Create Placement**. The form
   should be pre-filled, with start date and term required (AS 6.1).
2. Save it. The submission should become **Placed**, the consultant **Placed**, and marketing
   **Closed ("Placed")**. Each change should show the actor and the triggering placement (AS 6.2).
   The other open submissions should be listed with Withdraw buttons, and none should change
   automatically (AS 6.4).
3. Try to create a placement from a non-Offer submission through the API. It should return
   `422 SUBMISSION_NOT_AT_OFFER` (AS 6.3).
4. As `hr`, on or after the start date, set the consultant to **Active Project** (AS 6.5). The
   consultant page should show the status and history but no vendor, client, rate, or placement
   panel. `/placements` should show "Not authorized" (AS 6.7).
5. As `recruiter2`, the placement list should not contain recruiter1's placement (AS 6.6).

### V7 — Dashboard (US7) and V8 — Reports (US8)

1. For each role, click every dashboard count. The list it opens should have the same total
   (AS 7.4, SC-006).
2. `recruiter3` should get zero figures and the "contact an Admin" message (edge case).
3. As `hr`, the dashboard should show counts by status and consultant activity only, with no
   submissions, rates, or performance (AS 7.3).
4. As `manager`, open each report for the current month. Totals should match the linked lists
   (AS 8.1–8.4). Then pick an empty date range; it should show "No data for this period"
   (AS 8.5).

### V9 — Edge cases

- **Concurrent edit**: Open the same consultant in two tabs and save in both. The second save
  should show the "record changed — reload" banner.
- **Inactive with open submissions**: Setting Inactive should be refused with the open submissions
  listed. After you withdraw them, Inactive should succeed and close open marketing ("Consultant
  inactive").
- **Dates**: A target date before the start date, a future submitted date, or a placement start
  date before the submitted date should each be refused with a field message.

### V10 — Success criteria checks

| SC | How to verify |
|----|---------------|
| SC-001 | Timed run of V2.1 → V6.2 as `admin` with one fresh consultant: < 15 min |
| SC-002 | `./mvnw verify` runs the parameterized authorization matrix test; it should report 0 failures |
| SC-003 | Spot-check history on V4–V6 records; the automated test asserts actor, time, and trigger on every transition |
| SC-004 | V3.4, V5.6, V6.3, V4.2, plus the reassignment single-recruiter invariant test |
| SC-005 | V5.2 + automated duplicate test (case/whitespace variants, Withdrawn/Rejected originals) |
| SC-006 | V7.1/V7.4 + automated dashboard/report ↔ list consistency test per role |
| SC-007 | Start with `-Dspring-boot.run.profiles=dev,perf` (500 consultants / 50 recruiters / 2,000 submissions). Lists, dashboard, and reports should each load in < 2 s (devtools Network tab) |
| SC-008 | Hand V5.1 to a first-time user; they should complete it unaided in < 3 min |
