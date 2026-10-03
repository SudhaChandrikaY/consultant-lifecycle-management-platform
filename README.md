# Consultant Lifecycle Management Platform (CLMP)

CLMP is an internal staffing and recruiting application. It tracks each consultant from Bench →
Ready → Marketing → Submission/Interview → Placement → Active Project. It gives a staffing
organization one place to manage recruiters, consultants, marketing assignments, client/vendor
submissions, placements, and operational reporting. Each role sees and does only what its
responsibilities require.

## Project Overview

### Main workflows

- **Sign-in and role-based navigation**: Session-based sign-in. Navigation and actions are shaped
  by role, and the backend enforces every rule.
- **Consultant profiles and readiness**: Maintain profile, contact, skills, and visa details. Move
  consultants through Bench, Ready, Hold, Inactive, and Active Project.
- **Recruiters and assignment**: Manage recruiter profiles, and assign or reassign consultants to
  recruiters.
- **Marketing**: Market Ready consultants. Track target dates and notes, and hold, reopen, or close
  marketing assignments.
- **Submissions**: Submit consultants to clients or vendors, and track them through interview and
  offer statuses with notes.
- **Placements**: Convert a submission at the Offer stage into a placement.
- **Dashboard**: An operational dashboard shaped by role.
- **Reports**: On-screen operational reports that drill down into the matching filtered lists.
- **History**: Status and activity history is recorded on key records.

### User roles

| Role | Summary |
|------|---------|
| **Admin** | Full access, including recruiter management, consultant profiles, marketing, submissions, placements, and reports |
| **Manager** | Organization-wide view; assigns recruiters; holds, reopens, or closes marketing; views reports |
| **Recruiter** | Works only on their assigned consultants: marketing, submissions, and placements from their own offers |
| **HR Operations** | Maintains consultant profiles and status; no access to marketing, submissions, placements, or commercial data |

The full capability matrix is in
[`specs/001-clmp-mvp/spec.md`](specs/001-clmp-mvp/spec.md#roles-at-a-glance) and
[`contracts/authorization-matrix.md`](specs/001-clmp-mvp/contracts/authorization-matrix.md).

## Technology Stack

- **Frontend**: React 19, TypeScript, and Vite 7, with Vitest and Testing Library for tests. It is
  a single-page app that talks to the backend only through the REST API.
- **Backend**: Java 21 and Spring Boot 4. It is a modular monolith layered as Controller → Service
  → Repository, with request and response DTOs at the API boundary.
  - Spring Security for session-based authentication and server-side authorization
  - Spring Data JPA (Hibernate) for persistence
  - Bean Validation for request validation
- **Database**: H2, used for the current local/demo environment (in-memory by default)
- **Process**: Spec-Driven Development with [Spec Kit](https://github.com/github/spec-kit). The
  features are specified, planned, and broken into tasks under `specs/` before they are implemented.

## Prerequisites

| Tool | Version | Check |
|------|---------|-------|
| JDK | 21 (Temurin or equivalent) | `java -version` |
| Node.js | 22 LTS (≥ 20.19) | `node -v` |
| npm | ≥ 10 | `npm -v` |

You don't need to install Maven separately, because the backend includes the Maven Wrapper
(`./mvnw`, or `mvnw.cmd` on Windows).

## Running Locally

[`specs/001-clmp-mvp/quickstart.md`](specs/001-clmp-mvp/quickstart.md) is the detailed guide. It
also includes the V1–V10 manual validation walkthrough.

### Backend

Run the backend with the `dev` profile. This profile seeds the synthetic demo users and data
at startup. Without it, the database starts empty and no one can sign in.

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The API listens on http://localhost:8080.

Optional Spring profiles:

- `file` keeps data in `backend/data/` across restarts. Use it with `dev`, as `dev,file`.
- `perf` adds a large performance seed. Use it with `dev`, as `dev,perf`.

### Frontend

In a second terminal:

```bash
cd frontend
npm ci
npm run dev
```

Open http://localhost:5173. The Vite dev server proxies `/api` requests to the backend at
`http://localhost:8080`, so the browser talks only to Vite and no CORS setup is needed.

## Demo Accounts

These synthetic accounts are created only when the `dev` profile is active. They share the
password `Demo@123`.

| Username | Role | Notes |
|----------|------|-------|
| `admin` | Admin | |
| `manager` | Manager | Organization-wide view; holds, reopens, or closes marketing |
| `recruiter1` | Recruiter | Linked to recruiter "Riya Patel" (Java team) |
| `recruiter2` | Recruiter | Linked to recruiter "Marcus Lee" (Data team) |
| `recruiter3` | Recruiter | Not linked to a recruiter profile, to show the empty-state experience |
| `hr` | HR Operations | Consultant profiles and status only; no commercial data |
| `inactive.user` | Recruiter | Deactivated, so sign-in is refused |

> **`Demo@123` is a local development and demo credential only.** All demo names and data are
> fictitious. The seeder refuses to start if the `prod` profile is active. Never use these
> accounts or this password in a shared or production environment.

## How to Test

### Backend

```bash
cd backend
./mvnw verify
```

This runs the unit tests (`*Test`) and the per-story integration tests (`*IT`), including the
authorization-matrix and consistency tests, against in-memory H2.

The performance smoke test (SC-007: 500 consultants and 2,000 submissions) is excluded by default.
Run it on its own with:

```bash
./mvnw verify -Dgroups=perf
```

### Frontend

```bash
cd frontend
npm run typecheck      # tsc --noEmit
npm test -- --run      # Vitest, single run (plain `npm test` starts watch mode)
npm run build          # type check plus Vite production build into dist/
```

A change is done only when both builds and all tests pass.

> **WSL note**: When the repository lives on a Windows drive (`/mnt/c/...`) and you run Node from
> WSL, jsdom loads too slowly for Vitest's 60-second worker start limit, and `npm test` fails with
> "Timeout waiting for worker to respond". Fix this by cloning the repository into the WSL file
> system, or by running the frontend tests with Windows-native Node.

## Useful Local URLs

| URL | What it is |
|-----|------------|
| http://localhost:5173 | Frontend (Vite dev server) |
| http://localhost:8080/api | Backend REST API |
| http://localhost:8080/h2-console | H2 console. It is enabled only under the `dev` profile, and only Admin can open it after signing in. JDBC URL: `jdbc:h2:mem:clmp` |

## Stopping and Resetting

- Press `Ctrl+C` in each terminal to stop the backend and the frontend.
- By default, H2 runs **in memory**. All data is lost when the backend stops, and the `dev` profile
  re-creates the demo data on the next start. A restart is therefore a full reset.
- If you used the `file` profile, data persists in `backend/data/`. Delete that directory to
  reset.

## Project Structure

```
frontend/    React + TypeScript + Vite SPA (pages, shared components, API client, tests)
backend/     Spring Boot modular monolith (auth, consultant, recruiter, marketing,
             submission, placement, dashboard, report, history, seed, ...)
specs/       Spec Kit feature artifacts: spec, plan, research, data model,
             REST contracts, authorization matrix, quickstart, and tasks
.specify/    Spec Kit infrastructure: project constitution, templates, and scripts
.claude/     Claude Code skills (the Spec Kit workflow commands) used during development
CLAUDE.md    Guidance for AI coding agents working in this repository
```

The project constitution, [`.specify/memory/constitution.md`](.specify/memory/constitution.md), is
the governing source of truth for architecture and engineering rules.

## Current Scope

This repository is a **local/demo MVP**. It currently provides:

- The full consultant-to-placement workflow, the dashboard, and the reports described above
- Session-based sign-in against **seeded** user accounts
- H2 persistence (in memory by default, with an optional file mode) and synthetic demo data

The following are **not** implemented and are out of scope for the current MVP:

- A production database, schema migrations, containers, or a cloud/deployment setup
- Single sign-on, self-registration, password reset, and user-administration screens
- Report export, document/resume storage, email or notification automation, payroll, and
  incentive payouts

Infrastructure changes require a separate, approved specification. See the Assumptions section of
[`specs/001-clmp-mvp/spec.md`](specs/001-clmp-mvp/spec.md) for the full list.
