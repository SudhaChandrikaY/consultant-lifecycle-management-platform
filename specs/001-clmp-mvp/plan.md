# Implementation Plan: CLMP MVP — Consultant-to-Placement Lifecycle

**Branch**: `001-clmp-mvp` | **Date**: 2026-10-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-clmp-mvp/spec.md`

## Summary

This plan builds the first working CLMP: an internal web application that tracks each consultant
from Bench → Ready → Marketing → Submission/Interview → Placement → Active Project. It serves four
roles (ADMIN, MANAGER, RECRUITER, HR_OPERATIONS) across eight areas: Login, Dashboard, Recruiters,
Consultants, Marketing, Submissions, Placements, and Reports.

**Technical approach**:
- **Backend**: A Spring Boot modular monolith with package-per-domain modules and
  Controller → Service → Repository layering. H2 handles persistence through JPA.
- **Sessions and authorization**: Spring Security session login, with role gates on controllers
  and ownership/scope checks in services.
- **Lifecycle rules**: One `ConsultantLifecycleService` owns every manual and automatic consultant
  status rule. It runs cross-module transitions synchronously in the triggering transaction and
  writes immutable, system-triggered history records.
- **Frontend**: A React + TypeScript SPA built with Vite talks to the backend only through the
  REST contract. It uses shared table, form, and status components and role-aware navigation.

Research decisions are in [research.md](./research.md).

## Technical Context

**Language/Version**: Java 21 (LTS); TypeScript 5.x on Node.js 22 LTS (≥ 20.19)

**Primary Dependencies**:
- Backend: Spring Boot 4.0.x (Web, Data JPA, Security, Validation) and H2
- Frontend: React 19, React Router 7, and Vite 7, with no UI kit or state library (research R2,
  R3)

**Storage**: H2. It runs in-memory by default with Hibernate-generated schema and a Java seeder.
An optional `file` profile persists data to disk (research R4).

**Testing**:
- Backend: JUnit 5, Spring Boot Test, MockMvc, and spring-security-test
- Frontend: Vitest and React Testing Library
- End-to-end: a manual scripted walkthrough in [quickstart.md](./quickstart.md) (research R16)

**Target Platform**: The backend is a single runnable JAR on JVM 21 (Linux/Windows). The frontend
is a browser SPA for current Chrome/Edge/Firefox desktop on the internal network.

**Project Type**: Web application (`frontend/` + `backend/`)

**Performance Goals**: Lists, dashboard, and reports return in < 2 s with 500 consultants, 50
recruiters, and 2,000 submissions (SC-007). This is met through server-side paging, indexes, and
count queries (research R14).

**Constraints**:
- 30-minute idle session timeout (FR-006)
- No SSN, DOB, bank, or ID-image data is stored (FR-027)
- Sensitive fields never appear in list DTOs or logs (FR-024, FR-103)
- No hard deletes (FR-028)
- No infrastructure beyond the stack the constitution fixes

**Scale/Scope**:
- Single organization, tens of internal users
- About 12 backend modules and about 18 SPA routes ([contracts/ui-routes.md](./contracts/ui-routes.md))
- 8 user stories (P1–P4)

**Open clarifications**: None. The spec has no `[NEEDS CLARIFICATION]` markers. Five planning
findings (one spec inconsistency, now resolved, and four assumptions) are listed in
[research.md § R18](./research.md#r18-spec-inconsistencies-found-during-planning-surfaced-per-constitution-viii)
and summarized under [Deviations & Assumptions](#deviations--assumptions).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principle | Gate | Pre-research | Post-design |
|---|-----------|------|:------------:|:-----------:|
| I | Spec-Driven Development | An approved spec exists. The plan traces to its US/FR/SC (see the traceability table below). | ✅ | ✅ |
| II | Working Vertical Slices | Delivery is ordered by user story, and each slice is UI → API → service → H2. No framework layers are built ahead of a slice that needs them. | ✅ | ✅ The phases below are slices. tasks.md Phase 2 holds only what US1 consumes, and every other shared backend or UI piece is created in the first story that uses it (research R19-1). |
| III | Technology Stack | React + TS + Vite, Java + Spring Boot, H2. No PostgreSQL or other stores. | ✅ | ✅ |
| IV | Modular Monolith | There is one deployable with domain packages. Business rules live in services, and DTOs at the API. The frontend uses REST only. | ✅ | ✅ Separate role-shaped DTOs (R8). An import-scan test blocks entity types in controller signatures (R12). |
| V | Scope Discipline | No Docker, K8s, Kafka, microservices, or extra observability. Every dependency maps to a requirement. | ✅ | ✅ Dependency list R2/R3, each justified. Flyway, Playwright, TanStack Query, and a UI kit are explicitly deferred. |
| VI | Buildable, Tested Quality | Both builds pass, and acceptance criteria are covered by automated tests. | ✅ | ✅ R16 maps tests to AS/SC: an authorization matrix test, a consistency test, and lifecycle unit tests. ⚠️ The local environment lacks a JDK and has Node 12. This is an environment prerequisite, not a design issue (quickstart). |
| VII | Backend-Enforced Security | RBAC and ownership are enforced on the server, and no sensitive data appears in logs or responses. | ✅ | ✅ Role gates plus service ownership policies (R7), DTO-level field omission, read-time redaction of history for HR (R8), BCrypt, CSRF, and HttpOnly session cookies (R5). |
| VIII | Traceability & Review | Deviations and assumptions are surfaced, and requirements are not changed silently. | ✅ | ✅ R18 lists them. They are repeated below for reviewer sign-off. |

**Gate result**: PASS. There are no violations, so Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/001-clmp-mvp/
├── spec.md
├── plan.md                         # This file
├── research.md                     # Phase 0: decisions R1–R18
├── data-model.md                   # Phase 1: entities, state machines, invariants
├── quickstart.md                   # Phase 1: build/run + validation scenarios V1–V10
├── contracts/
│   ├── rest-api.md                 # Endpoints, DTO shapes, error codes
│   ├── authorization-matrix.md     # Endpoint × role, field visibility, nav
│   └── ui-routes.md                # SPA routes and cross-cutting UI behavior
├── checklists/requirements.md
└── tasks.md                        # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/wrapper/
└── src/
    ├── main/
    │   ├── java/com/ensar/clmp/
    │   │   ├── ClmpApplication.java
    │   │   ├── common/          # ProblemDetail advice, error codes, PageResponse, Clock config, CurrentUser
    │   │   ├── auth/            # SecurityConfig, login/logout/me controller, AppUser entity + repo, UserDetailsService
    │   │   ├── reference/       # Team, Region, VisaType, Vendor, Client; reference controller; find-or-create
    │   │   ├── recruiter/       # web/ service/ domain/  (Recruiter, RecruiterAccessPolicy)
    │   │   ├── consultant/      # web/ service/ domain/  (Consultant, filters/Specifications, readiness check)
    │   │   ├── lifecycle/       # ConsultantLifecycleService, transition tables, TriggerEvent
    │   │   ├── marketing/       # web/ service/ domain/  (MarketingAssignment, MarketingNote, state machine)
    │   │   ├── submission/      # web/ service/ domain/  (Submission, SubmissionNote, DuplicateRef, state machine)
    │   │   ├── placement/       # web/ service/ domain/  (Placement)
    │   │   ├── history/         # HistoryRecord, HistoryService (write), description renderer, activity query
    │   │   ├── dashboard/       # DashboardService (counts via shared filters), controller
    │   │   ├── report/          # ReportService, controller
    │   │   └── seed/            # DemoDataSeeder (dev), PerfDataSeeder (perf)
    │   └── resources/
    │       ├── application.yml            # session timeout, clmp.time-zone, H2 mem
    │       ├── application-dev.yml        # seed on, H2 console (ADMIN)
    │       ├── application-file.yml       # H2 file mode
    │       └── application-perf.yml       # large seed
    └── test/java/com/ensar/clmp/
        ├── lifecycle/           # unit: transition tables, readiness
        ├── submission/          # unit: state machine, duplicate normalization
        ├── it/                  # integration per user story (US1…US8)
        ├── security/            # AuthorizationMatrixTest, SensitiveFieldExposureTest
        ├── consistency/         # Dashboard/Report ↔ list totals per role
        └── architecture/        # No @Entity in controller signatures

frontend/
├── package.json, tsconfig.json, vite.config.ts   # dev proxy /api → :8080
├── index.html
└── src/
    ├── main.tsx, App.tsx, routes.tsx
    ├── api/                 # client.ts (fetch + CSRF + ProblemDetail), per-area modules, types.ts
    ├── auth/                # AuthProvider, RequireRole guard, permissions.ts (mirrors matrix)
    ├── components/          # DataTable, FilterBar, FormField, StatusBadge, PageLayout, NavBar,
    │                        # EmptyState, LoadingState, ErrorBanner, ConfirmDialog, ReasonDialog, HistoryList
    ├── pages/
    │   ├── login/  dashboard/  recruiters/  consultants/
    │   ├── marketing/  submissions/  placements/  reports/
    │   └── NotAuthorized.tsx, NotFound.tsx
    ├── styles/              # tokens.css, global.css
    └── test/                # setup + role-nav, route-guard, duplicate-flow, field-error tests
```

**Structure Decision**: This is a web application with two top-level projects, as `CLAUDE.md`
prescribes. `backend/` is one Spring Boot deployable organized by business domain (constitution
IV). `frontend/` is a single Vite SPA. No shared code package exists between them. The REST
contract in `contracts/` is the shared interface, and the frontend's `api/types.ts` is written by
hand from it.

## Delivery Slices (input to `/speckit-tasks`)

Each slice is a demonstrable vertical flow (constitution II). Shared pieces are built in the first
slice that needs them.

| Slice | Stories | Delivers | Demo |
|-------|---------|----------|------|
| S0 Skeleton | (enabler for US1) | Both projects scaffolded, builds green, `/api/auth/me` round-trip through the Vite proxy | `mvnw verify` + `npm run build` pass |
| S1 Sign-in & shell | US1 | Security config, seeded users and recruiters, login/logout/timeout, role nav + route guard, NotAuthorized, ProblemDetail errors | V1 |
| S2 Consultants | US2 | Consultant CRUD, list/detail DTOs, readiness, manual transitions via lifecycle service, history (write + view), reference API, shared list/form components, unlinked-recruiter empty state | V2 |
| S3 Recruiters & assignment | US3 | Recruiter CRUD, status with confirm, list filters + counts, assign/reassign, needs-reassignment | V3 |
| S4 Marketing | US4 | Marketing state machine (create for READY only), notes, overdue, automatic consultant transitions, consultant detail marketing panel | V4 |
| S5 Submissions | US5 | Vendors/clients find-or-create, submission state machine, duplicate flow, timeline, notes (no general edit), Interviewing automation, HR-safe history descriptions, consultant detail submissions and on-Hold panels | V5 |
| S6 Placements | US6 | Placement creation (atomic; submission's recruiter or ADMIN; HOLD → PLACED allowed), edits with start-date rule, other-open-submissions panel, Active Project, HR exclusion, consultant detail placements panel | V6 |
| S7 Dashboard | US7 | Role-shaped dashboard with linked counts and activity feed (recent-activity query built here) | V7 |
| S8 Reports | US8 | Five reports with date range and empty states | V8 |
| S9 Hardening | SC-002/006/007 | Authorization matrix test complete, consistency test, perf seed and check, sensitive-field test | V9, V10 |

## Requirements Traceability (summary)

| Spec area | Design artifacts |
|-----------|------------------|
| FR-001–007 (access) | research R5, R7; rest-api § Auth; authorization-matrix |
| FR-010–017 (recruiters, manager scope) | data-model § Recruiter; rest-api § Recruiters; matrix |
| FR-020–028 (consultants) | data-model § Consultant; rest-api § Consultants (list vs detail DTO) |
| FR-030–036 (status, assignment, automation) | data-model § ConsultantStatus state machine; research R9 |
| FR-040–045 (marketing) | data-model § MarketingAssignment; rest-api § Marketing |
| FR-050–064 (submissions) | data-model § Submission + duplicate rule; rest-api § Submissions; research R8 |
| FR-070–077 (placements) | data-model § Placement; rest-api § Placements |
| FR-080–083 (dashboard) | rest-api § Dashboard; research R13 |
| FR-090–093 (reports) | rest-api § Reports; research R13 |
| FR-100–104 (history, data protection) | data-model § HistoryRecord; research R8, R11 |
| Edge cases | data-model guards; research R10 (concurrency), R15 (dates); quickstart V9 |
| SC-001–008 | quickstart V10 |

## Deviations & Assumptions

These were raised for reviewer acknowledgement (constitution VIII). Details are in research R18.
**Review status (2026-10-02)**: item 1 is resolved, and items 2–6 are reviewed and accepted as
written. None of them changes the design or scope.

1. ~~**Spec inconsistency**~~ **Resolved (2026-10-02)**: The US6 narrative said a "manager" could
   convert an offer into a placement, contradicting FR-017, FR-070, and Clarification Q3. The spec
   narrative now says the submission's recruiter or an Admin converts it, and Managers view
   placements but cannot create or edit them. The plan already followed the FRs, so no design
   change was needed.
2. **Accepted (2026-10-02)**: **Recruiter Assignment** is modeled as the current FK plus
   immutable history rows, not as a separate table.
3. **Accepted (2026-10-02)**: **Needs reassignment** is derived ("current recruiter is
   Inactive") and not persisted as a separate flag. It also clears if the recruiter is
   reactivated.
4. **Accepted (2026-10-02)**: **Submissions of a consultant on Hold** can still change status.
   New submissions for that consultant are refused.
5. **Accepted (2026-10-02)**: **Minimum fields at consultant creation** are first name, last
   name, and email. The rest are required only for Ready.
6. **Acknowledged (2026-10-02)**: **Environment**: JDK 21 and Node ≥ 20.19 must be installed
   before implementation, and the current shell has neither. The design stays on Java 21 and
   Node 22. The active shell environment will be verified before implementation starts.

### Analysis remediation (2026-10-02)

The `/speckit-analyze` findings were resolved with reviewer decisions, listed in
[research.md § R19](./research.md#r19-analysis-remediation-decisions-2026-10-02): vertical-slice task
placement, consultant detail panels, placement ownership (submission's recruiter or ADMIN),
Hold → Placed, the unlinked-recruiter empty state, the interviews-scheduled report definition,
removal of the submission edit endpoint, marketing creation for READY only, the placement edit
date rule, and the delivery-order alignment. Scope is unchanged.

## Complexity Tracking

No constitution violations, so nothing to justify.
