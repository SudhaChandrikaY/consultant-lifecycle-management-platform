---

description: "Task list for 002-agentic-engineering (simplified)"
---

# Tasks: CLMP Agentic Engineering Layer

**Input**: Design documents from `/specs/002-agentic-engineering/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Each story ends with a short check from [quickstart.md](./quickstart.md). The only new
automated test is the demonstration test (T014). Only the demonstration is recorded, in
`specs/002-agentic-engineering/demo.md`.

**Organization**: Grouped by user story. The two skills come first (Phase 2) because the agents
preload them.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: The user story the task belongs to (US1–US7)

## Rules for every task

- Do not edit `.claude/skills/speckit-*` or `.specify/**` (FR-060).
- Do not change `backend/src/main/**`, `frontend/src/**`, or `specs/001-clmp-mvp/**` (FR-090).
- Never write a token into any file (FR-052).
- Commit only when the developer asks.

---

## Phase 1: Setup

- [ ] T001 Record the baseline in a new `specs/002-agentic-engineering/demo.md`, before any other change (data-model §3 "Baseline"):
  - Run `cd backend && ./mvnw verify` and copy the final `Tests run: …` line and the BUILD result.
  - Run `cd frontend && npm run typecheck && npm run build` and note the result.
  - Note `java -version` and `node -v`.
  - Create the remaining section headings of data-model §3, empty.

---

## Phase 2: Foundational — the two CLMP skills (block all agents)

- [ ] T002 [P] Create `.claude/skills/clmp-change-workflow/SKILL.md` per [contracts/skill-contracts.md § clmp-change-workflow](./contracts/skill-contracts.md#clmp-change-workflow):
  - Frontmatter: `name: clmp-change-workflow`; the draft `description` beginning "CLMP project rule (not a Spec Kit step): …"; `metadata: { owner: clmp }`. No `allowed-tools`.
  - Body: the 8 steps with their code anchors.
  - It links to `.specify/memory/constitution.md`, `CLAUDE.md`, `specs/001-clmp-mvp/contracts/authorization-matrix.md`, `specs/001-clmp-mvp/contracts/rest-api.md`, and `specs/001-clmp-mvp/data-model.md` rather than copying them.
- [ ] T003 [P] Create `.claude/skills/clmp-release-readiness/SKILL.md` per [contracts/skill-contracts.md § clmp-release-readiness](./contracts/skill-contracts.md#clmp-release-readiness):
  - The same frontmatter conventions as T002.
  - Body: delegate to `clmp-test-validator`; the check-selection table with the exact commands, including the row "explicitly asked for **full** / **all** validation → every backend and frontend check regardless of changed paths (perf still only if asked)"; the environment-blocker rules (the toolchain rule, and the WSL rule "`Timeout waiting for worker to respond` + path starts with `/mnt/`" with both workarounds); the Quality Gate → evidence table; the verdicts `ready` / `not ready` / `blocked by environment`; and "an environment blocker is never reported as ready".
  - It links to the validation report format in `specs/002-agentic-engineering/data-model.md` §2.3.

**Checkpoint**: Both skills exist, and the agents can preload them.

---

## Phase 3: User Story 1 — Analyze Impact (P1) 🎯 MVP

**Goal**: A read-only analyst that returns the 9-section impact report.
**Independent Test**: Quickstart "US1 Analyst" and "US1 out of scope".

- [ ] T004 [US1] Create `.claude/agents/clmp-codebase-analyst.md` per [contracts/agent-contracts.md](./contracts/agent-contracts.md):
  - Frontmatter: `name: clmp-codebase-analyst`; the draft description; `tools: Read, Grep, Glob`; `skills: [clmp-change-workflow]`.
  - Body: the common instructions 1–5; what to read; flag out-of-scope or infrastructure work; the Impact report format from data-model §2.1 (the header plus the 9 sections); "return only the report".
- [ ] T005 [US1] Run both US1 quickstart checks, with `git status --porcelain` before and after. If a section is missing or the out-of-scope probe isn't flagged, adjust T004 and rerun. No evidence file is needed.

---

## Phase 4: User Story 2 — Independent Review (P1)

**Goal**: A read-only reviewer that reports findings in the fixed format.
**Independent Test**: Quickstart "US2 Reviewer".

- [ ] T006 [US2] Create `.claude/agents/clmp-code-reviewer.md` per [contracts/agent-contracts.md](./contracts/agent-contracts.md):
  - Frontmatter: `name: clmp-code-reviewer`; the draft description; `tools: Read, Grep, Glob, Bash, mcp__github`; `skills: [clmp-change-workflow, clmp-release-readiness]`.
  - Body:
    - Bash is for read-only `git` only (`diff`, `log`, `show`, `status`, `merge-base`, `ls-files`, `blame`): never edit, build, check out, or commit.
    - Accepted targets and the default `git diff main...HEAD` plus uncommitted changes; never the whole repository.
    - Don't use commit messages or the implementer's explanation as evidence.
    - The checked areas.
    - The Review report format from data-model §2.2: `id | severity | location | description | rule | suggestion`, with the severities `blocking` · `major` · `minor` · `note`, and blocking for DTO-boundary, authorization, lifecycle, or sensitive-data violations.
    - Scope deviations; the verdict; "No findings." when there are none.
    - If the GitHub tools are unavailable, continue with local git.
- [ ] T007 [US2] Run the US2 quickstart check (review commit `b46385e`) with `git status --porcelain` before and after. Adjust T006 if the format is wrong.

---

## Phase 5: User Story 3 — Independent Validation (P1)

**Goal**: A validator that selects and runs the existing checks and classifies the outcomes.
**Independent Test**: Quickstart "US3 Validator".

- [ ] T008 [US3] Create `.claude/agents/clmp-test-validator.md` per [contracts/agent-contracts.md](./contracts/agent-contracts.md):
  - Frontmatter: `name: clmp-test-validator`; the draft description; `tools: Read, Grep, Glob, Bash`; `skills: [clmp-release-readiness]`.
  - Body:
    - Bash is only for the commands in the readiness skill's check-selection table, plus `git diff --name-only`, `git status`, `java -version`, `node -v`. Never edit code or tests, install tools, or run perf unless asked.
    - When explicitly asked for **full** or **all** validation, run every backend and frontend check in the readiness table regardless of changed paths (perf still only if asked).
    - Otherwise, find the changed paths, then run targeted tests before full ones.
    - Re-run a failure once, then classify it.
    - The Validation report format from data-model §2.3: `check | command | reason | outcome | classification | detail`, test counts, and the verdict.
- [ ] T009 [US3] Run the US3 quickstart check ("validate the working tree, full") with `git status --porcelain` before and after. Confirm that every backend and frontend check ran even though only `.claude/` and `specs/` changed (backend `verify`, the authorization tests, frontend typecheck, tests, and build), and that the WSL frontend-test timeout is classified as an environment blocker with the workarounds. Adjust T003/T008 if not.

**Checkpoint**: The analyst, reviewer, and validator all work on their own.

---

## Phase 6: User Story 4 — Skills in Use (P2)

**Goal**: Both skills trigger naturally and are visibly separate from Spec Kit.
**Independent Test**: Quickstart "US4 Skills".

- [ ] T010 [US4] In a fresh session, run the US4 quickstart prompts (stop the change request after its plan; do not implement). Confirm that `clmp-change-workflow` and `clmp-release-readiness` are used, that the readiness answer lists every constitution Quality Gate (builds, tests, acceptance criteria, no role bypass or data exposure, deviations documented), and that the `/` menu lists `clmp-*` apart from `speckit-*`. If a skill doesn't trigger, sharpen its `description` (T002/T003).

---

## Phase 7: User Story 5 — Read-Only GitHub Context (P2)

**Goal**: A shareable, read-only GitHub MCP configuration with no secrets.
**Independent Test**: Quickstart "US5 GitHub".

- [ ] T011 [P] [US5] Create `.mcp.json` at the repository root, exactly as in [contracts/github-mcp-access.md](./contracts/github-mcp-access.md#mcpjson): server `github`, `type: "http"`, `url: "https://api.githubcopilot.com/mcp/readonly"`, and `headers: { "Authorization": "Bearer ${CLMP_GITHUB_PAT}" }`.
- [ ] T012 [US5] The developer creates the fine-grained **read-only** token (contract § Token), exports `CLMP_GITHUB_PAT`, restarts Claude Code, and approves `github`. Then run the US5 quickstart check: the tools listed are read-only, and the PRs and branches are returned. If write tools appear, switch to the documented `X-MCP-Readonly: true` header form (research R5) and note it in `plan.md` § Deviations.

---

## Phase 8: User Story 6 — Demonstration on Real CLMP Work (P2)

**Goal**: The unlinked-RECRUITER test is taken through the full agentic loop and kept, with
`demo.md` recording it.
**Independent Test**: `demo.md` contains the impact, review, and validation reports (each labelled
with its agent) and an outcome for every finding, and the new test passes.

Around each agent run (T013, T015, T017), record `git status --porcelain` before and after in
`demo.md` (SC-003).

- [ ] T013 [US6] **Analyze**: run the quickstart Demonstration step 1 prompt with `clmp-codebase-analyst`. Paste the report verbatim into `demo.md` § Impact.
- [ ] T014 [US6] **Implement** following `clmp-change-workflow` and the impact report: add `backend/src/test/java/com/ensar/clmp/it/UnlinkedRecruiterScopeIT.java` (or the location the impact report recommends; note it in `demo.md` § Implementation).
  - It extends `IntegrationTestBase`.
  - It asserts that ADMIN gets `totalItems > 0` from `GET /api/marketing-assignments`, `GET /api/submissions`, and `GET /api/placements`, and that `recruiter3` gets HTTP 200 with `totalItems == 0` from each.
  - A class comment cites the 001 spec edge case, `contracts/authorization-matrix.md` "Unlinked RECRUITER", and FR-004/FR-015.
  - No production code changes.
- [ ] T015 [US6] **Review**: run the quickstart Demonstration step 3 prompt with `clmp-code-reviewer`. Paste the report verbatim into `demo.md` § Review round 1.
- [ ] T016 [US6] **Fix**: add one row per finding to `demo.md` § Findings (`finding | from | resolution | reason | confirmed in round`). Fix the `fixed` rows in the test file only. If anything changed, re-run the reviewer and paste the result as "Review round 2". Repeat until no blocking or major finding remains open.
- [ ] T017 [US6] **Validate**: ask "Use clmp-release-readiness: is this change ready to merge?" and paste the validator report into `demo.md` § Validation round 1.
  - **Expected**: the new test passes; the backend count is the baseline plus the new test(s); frontend is "not run: no frontend change"; the verdict is `ready`.
  - If it isn't ready: fix, re-review (T015), re-validate, and add the findings rows.
- [ ] T018 [US6] Complete `demo.md` § Result: the final test totals, the three `git status` comparisons, the final verdict, and the commit SHA once the developer agrees to commit.

**Checkpoint**: The demonstration is complete, and the test is kept.

---

## Phase 9: User Story 7 — Documentation (P3)

**Goal**: One place to learn the layer, with pointers from `CLAUDE.md` and the README.
**Independent Test**: Quickstart "US7 Docs" (≥ 4/5).

- [ ] T019 [P] [US7] Create `docs/agentic-engineering.md`:
  1. Purpose and the separation-of-responsibilities table (spec Overview).
  2. The three agents: what each does, when to use it, its tools and limits, and an example prompt.
  3. The two skills: when to use each.
  4. CLMP skills vs Spec Kit skills (the table from contracts/skill-contracts.md).
  5. The lifecycle mapping (research R10).
  6. GitHub MCP: what it provides, why it is read-only, the token setup with `CLMP_GITHUB_PAT`, and what happens without a token.
  7. Secrets: never commit tokens; `.claude/settings.local.json` is personal.
  8. The WSL `/mnt/c` frontend-test note.
  9. Known limitations (plan § Deviations).
  10. A link to `specs/002-agentic-engineering/demo.md` as the worked example.
- [ ] T020 [P] [US7] Edit `CLAUDE.md`: add a short `## Agentic Workflow` section after `## Agent Workflow`.
  - It maps analyst → before planning or implementing, `clmp-change-workflow` → when implementing, reviewer → after a change, and `clmp-release-readiness` (validator) → before merge.
  - It notes that GitHub MCP is read-only context and that `speckit-*` skills must not be edited, and links `docs/agentic-engineering.md`.
  - Leave the existing lines unchanged.
- [ ] T021 [P] [US7] Edit `README.md`:
  - In `## Project Structure`, update the `.claude/` line to mention the `speckit-*` and `clmp-*` skills and the `clmp-*` agents, and add lines for `.mcp.json` ("read-only GitHub MCP; token from `CLMP_GITHUB_PAT`, never committed") and `docs/`.
  - Add a short `## Agentic Engineering` section before `## Current Scope` that links to `docs/agentic-engineering.md`.
- [ ] T022 [US7] Run the US7 quickstart check (the five situations, using only the doc). If fewer than 4/5 are correct, revise T019.

---

## Phase 10: Polish

- [ ] T023 Run the quickstart **Final checks** (SC-001 including the final frontend `npm run typecheck` and `npm run build`, SC-005, SC-006, SC-007) and append the commands and results to `demo.md` § Result. The diff checks compare against `main` and cover both committed and uncommitted changes, so they may run before or after the commit.
- [ ] T024 Draft the change description in `demo.md` (a final "Change description" section): what was added per story, the SC-001–SC-008 results, the deviations, and the follow-ups. Do not open a PR unless the developer asks.

---

## Dependencies & Execution Order

- **T001** comes first, because it is the baseline.
- **Phase 2** (T002, T003) blocks all the agents.
- **US1, US2, US3** (T004–T009) each depend only on Phase 2. Their agent files can be written in parallel; run their checks one at a time.
- **US4** (T010) depends on US3, because readiness delegates to the validator.
- **US5** (T011–T012) is independent. T012 needs the developer's token.
- **US6** (T013–T018) depends on US1–US3. It does not need US5.
- **US7** (T019–T022) is best done after US6, so the docs can link to the worked example.
- **Polish** (T023–T024) comes last.

```text
T001 ─► T002,T003 ─► T004/T006/T008 ─► T005,T007,T009 ─► T010 ─► T013…T018 ─► T019…T022 ─► T023,T024
T011 ─► T012 (developer, any time)
```

## Parallel Opportunities

- T002 and T003 (the two skills).
- T004, T006, and T008 (the three agent files), after Phase 2.
- T011 alongside any phase.
- T019, T020, and T021 (the docs).

## Implementation Strategy

1. **MVP**: T001–T005. Baseline, skills, and analyst: useful immediately for any CLMP change.
2. Add the reviewer and the validator (T006–T009). That completes the independent
   analyze → review → validate trio.
3. The skills check (T010) and GitHub (T011–T012).
4. The demonstration (T013–T018): the acceptance test for the feature, which leaves a real test
   behind.
5. The docs (T019–T022), then the final checks (T023–T024).
