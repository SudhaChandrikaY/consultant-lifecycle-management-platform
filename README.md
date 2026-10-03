# Consultant Lifecycle Management Platform (CLMP)

CLMP is an internal staffing and recruiting application. It tracks each consultant from Bench →
Ready → Marketing → Submission/Interview → Placement → Active Project. It supports four roles:
Admin, Manager, Recruiter, and HR Operations.

- **Backend**: Java 21 and Spring Boot 4 in `backend/` (a modular monolith on H2)
- **Frontend**: React 19, TypeScript, and Vite 7 in `frontend/` (a SPA that talks only to the REST API)
- **Specification**: `specs/001-clmp-mvp/`, which holds the spec, plan, contracts, data model, and tasks
- **Governance**: `.specify/memory/constitution.md`

## Build, run, and validate

[`specs/001-clmp-mvp/quickstart.md`](specs/001-clmp-mvp/quickstart.md) is the source of truth for
prerequisites, build and test commands, and the V1–V10 validation walkthrough. In short:

```bash
# Backend: API on :8080, H2 in memory, demo data seeded at startup
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend: SPA on :5173, proxies /api to :8080
cd frontend && npm ci && npm run dev
```

Then open http://localhost:5173.

| Command | Purpose |
|---------|---------|
| `./mvnw verify` (in `backend/`) | Unit and integration tests, including the authorization matrix and consistency tests |
| `./mvnw verify -Dgroups=perf` | Performance smoke test only (SC-007: 500 consultants, 2,000 submissions) |
| `npm run typecheck && npm test -- --run && npm run build` (in `frontend/`) | Frontend gate |

Optional Spring profiles:

- `file` keeps data in `backend/data/`.
- `perf` adds the large performance seed. Use it with `dev`, as `dev,perf`.

> **WSL note**: When the repository lives on a Windows drive (`/mnt/c/...`) and you run Node from
> WSL, jsdom loads too slowly for Vitest's 60-second worker start limit, and `npm test` fails with
> "Timeout waiting for worker to respond". Fix this by cloning the repository into the WSL file
> system, or by running the frontend tests with Windows-native Node.

## Demo users (`dev` profile only)

All demo users share the password `Demo@123`.

| Username | Role | Notes |
|----------|------|-------|
| `admin` | Admin | |
| `manager` | Manager | Organization-wide view; holds, reopens, or closes marketing |
| `recruiter1` | Recruiter | Linked to recruiter "Riya Patel" (Java team) |
| `recruiter2` | Recruiter | Linked to recruiter "Marcus Lee" (Data team) |
| `recruiter3` | Recruiter | Not linked to a recruiter profile, to show the empty-state experience |
| `hr` | HR Operations | Consultant profiles and status only; no commercial data |
| `inactive.user` | Recruiter | Deactivated, so sign-in is refused |

**The demo passwords are for local development only.** The seeder runs only under the `dev`
profile, and it refuses to start if the `prod` profile is active. The H2 console (`/h2-console`)
is enabled only in `dev`, and only Admin can open it.
