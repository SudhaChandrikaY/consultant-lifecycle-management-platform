---
name: clmp-change-workflow
description: "CLMP project rule (not a Spec Kit step): how to make a change in this codebase. Use when implementing, fixing, or modifying CLMP backend or frontend code, including during /speckit-implement tasks."
metadata:
  owner: clmp
---

# CLMP Change Workflow

How a change is made in the Consultant Lifecycle Management Platform. Follow the steps in order.
The governing documents win over this summary if they disagree:

- [Constitution](../../../.specify/memory/constitution.md)
- [CLAUDE.md](../../../CLAUDE.md)
- [Authorization matrix](../../../specs/001-clmp-mvp/contracts/authorization-matrix.md)
- [REST API contract](../../../specs/001-clmp-mvp/contracts/rest-api.md)
- [Data model](../../../specs/001-clmp-mvp/data-model.md)

## 1. Confirm scope

- Find the approved spec and task: `.specify/feature.json` points to the active feature; tasks
  are in `specs/<feature>/tasks.md`.
- If no spec covers the change, it must be a trivial fix (a typo, a small bug fix, or a
  dependency bump; constitution I), and you must say so. Otherwise, stop and recommend
  `/speckit-specify`.
- A change to business requirements, role permissions, or infrastructure (database, containers,
  messaging, deployment) needs its own approved spec.

## 2. Identify the impact

For anything beyond a single file, delegate to the `clmp-codebase-analyst` agent and work from
its impact report. Note the affected modules, layers, rules, and existing tests.

## 3. Follow the architecture rules

Backend modules live in `backend/src/main/java/com/ensar/clmp/<module>/`, with `domain`,
`service`, and `web` packages.

| Rule | Where it is anchored |
|------|----------------------|
| Controller → Service → Repository. Controllers stay thin; business rules live in services. | Constitution IV |
| REST endpoints use request/response DTOs. Never return or accept JPA entities. | `backend/src/test/java/com/ensar/clmp/architecture/NoEntityInControllerSignatureTest.java` |
| Role gates are enforced on the server with `@PreAuthorize` and URL rules. | `auth/config/SecurityConfig.java`, authorization matrix |
| Ownership scope is enforced in services. | `*AccessPolicy` (single records), `*Specifications` (lists); matrix "Ownership scopes", including "Unlinked RECRUITER" |
| Frontend role checks are UX only. | `frontend/src/auth/permissions.ts` |
| All consultant status changes go through the lifecycle service. Manual transitions come from its `MANUAL` map. Automatic transitions record the actor and trigger in history, in the same transaction. | `lifecycle/ConsultantLifecycleService.java`; 001 FR-031, FR-100 |
| No sensitive values in logs, list DTOs, or history. History stores field names, not values. | `backend/src/test/java/com/ensar/clmp/security/SensitiveFieldExposureTest.java`; 001 FR-024, FR-103 |
| Errors use `ErrorCode` + `ProblemDetail`. Edits use optimistic versioning (409 `CONCURRENT_MODIFICATION`). | `common/error/`, `common/service/VersionGuard.java` |

A violation of the DTO boundary, server-side authorization, lifecycle, or sensitive-data rules is
a **blocking** defect.

## 4. Build a vertical slice

UI → REST API → service → persistence, as far as the task needs. In the frontend, reuse the
shared components (`DataTable`, `FormField`, `ErrorBanner`, status badges, `PageLayout`) and handle
the loading, empty, validation, and error states.

## 5. Test the acceptance criteria

- **Backend**: unit tests `*Test`, and integration tests `*IT` that extend
  `backend/src/test/java/com/ensar/clmp/support/IntegrationTestBase.java` (MockMvc, seeded demo
  users, fixed clock).
- **Frontend**: Vitest and Testing Library in `frontend/src/test/`.
- Cite the requirement or acceptance scenario the test covers.

## 6. Stay in scope

No unrelated refactors, renames, dependencies, or infrastructure (constitution V). Keep the diff
reviewable.

## 7. Hand off for review and validation

1. Ask the `clmp-code-reviewer` agent to review the change.
2. Fix blocking and major findings, or record why they are accepted or rejected, then re-review.
3. Use the `clmp-release-readiness` skill, which delegates to `clmp-test-validator`.

## 8. Report

In the change description, state the deviations from the spec or constitution, any assumptions,
and any blockers. Requirements are never changed silently (constitution VIII).
