---
name: clmp-release-readiness
description: "CLMP project rule (not a Spec Kit step): decide whether a change is ready to merge. Use when asked to validate, test, check readiness, or confirm Quality Gates for a CLMP change."
metadata:
  owner: clmp
---

# CLMP Release Readiness

Decides whether a CLMP change is ready to merge, measured against the Quality Gates in the
[constitution](../../../.specify/memory/constitution.md). The validation report format is defined
in [002 data-model §2.3](../../../specs/002-agentic-engineering/data-model.md#23-validation-report-validator).

## 1. Delegate execution

Ask the `clmp-test-validator` agent to validate the change. The main agent does not choose or run
the checks itself; that keeps validation independent of the implementer.

## 2. Check selection

The validator uses this table. Run commands from the repository root, exactly as shown.

| Changed area | Checks |
|--------------|--------|
| `backend/**` | targeted: `cd backend && ./mvnw test -Dtest=<Class>[,<Class>]`; then full: `cd backend && ./mvnw verify` |
| Authorization/ownership or sensitive-data code (`*AccessPolicy*`, `*Specifications*`, `SecurityConfig`, `@PreAuthorize`, DTOs, logging, the seeder) | additionally `cd backend && ./mvnw test -Dtest=AuthorizationMatrixTest,SensitiveFieldExposureTest` |
| `frontend/**` | `cd frontend && npm run typecheck`; `cd frontend && npm test -- --run`; `cd frontend && npm run build` |
| Docs, `specs/`, or `.claude/` only | not run: no code change (unless full/all is requested) |
| Explicitly asked for **full** / **all** validation | every backend and frontend check above (targeted where applicable, `verify`, the authorization tests, typecheck, tests, build), regardless of changed paths. Perf still only if asked. |
| Performance | only if asked: `cd backend && ./mvnw verify -Dgroups=perf` |

- Run targeted tests before full ones, so failures show up fast.
- If a check fails, re-run it once. If the two results differ, report the check as unstable,
  with both outcomes.
- Take test counts from the tool output (the Surefire `Tests run:` summary, the Vitest summary).

## 3. Environment blockers

Classify these as `environment blocker`, never as product or test failures:

- **Toolchain**: `java -version` is not 21, or `node -v` is below 20.19. Name the tool, and
  install nothing.
- **WSL `/mnt/` Vitest timeout**: the frontend test output contains
  `Timeout waiting for worker to respond` **and** the repository path starts with `/mnt/`. Report
  that the frontend tests were **not exercised**. There are two workarounds (README "WSL note"):
  - clone the repository into the WSL file system, or
  - run the frontend tests with Windows-native Node.

## 4. Classify outcomes

| Classification | Meaning |
|----------------|---------|
| `passed` | The check succeeded. |
| `product failure` | Production code under test behaves incorrectly. Use this when unsure whether the product or the test is at fault, and say so. |
| `test failure` | The test itself is wrong (fixture or assertion) while the code matches the spec. |
| `environment blocker` | A tool, version, or platform problem (section 3). |
| `not run` | Skipped, with a reason. |

## 5. Map the results to the Quality Gates

| Quality Gate | Evidence |
|--------------|----------|
| Frontend and backend builds pass | Backend `verify`; frontend `build` (or a justified skip) |
| Tests for affected workflows pass | Targeted and full test results |
| The change satisfies its acceptance criteria | Impact report requirements + review "Checked" section |
| No role check bypassed; no sensitive data exposed | Reviewer findings + authorization tests when selected |
| Deviations documented | Reviewer scope deviations + change description |

Always include this table in the answer, one row per gate, each with a status (met / not met /
blocked / not applicable) and its evidence.

## 6. Verdict

`ready`, `not ready`, or `blocked by environment`. An environment blocker is never reported as
`ready`. List any open blocking or major review findings as reasons for `not ready`.
