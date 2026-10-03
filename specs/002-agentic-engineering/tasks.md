---

description: "Task list for 002-agentic-engineering"
---

# Tasks: CLMP Agentic Engineering Layer

**Input**: Design documents from `/specs/002-agentic-engineering/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: The spec requires demonstrated behavior (US6, SC-001–SC-010), so each story ends with
**verification tasks** that run the matching [quickstart.md](./quickstart.md) scenario and save
the result under `specs/002-agentic-engineering/evidence/`. The only application test is the Part
1 demonstration test (T028).

**Organization**: Tasks are grouped by user story. The two CLMP skills are in Phase 2
(Foundational) because all three agents preload them (research R5). US4 verifies them as a user
journey.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: The user story the task belongs to (US1–US7)
- **[developer]**: The task needs the developer's own GitHub token or judgment. The implementing
  agent prepares the task but must not handle the token.

## Path Conventions

- Agentic layer: `.claude/agents/`, `.claude/skills/clmp-*/`, `.claude/hooks/`,
  `.claude/settings.json`, `.mcp.json` (repository root)
- Docs: `docs/agentic-engineering.md`, `CLAUDE.md`, `README.md`
- Evidence: `specs/002-agentic-engineering/evidence/` (abbreviated `evidence/` below)
- Backend tests: `backend/src/test/java/com/ensar/clmp/`
- Run all commands from the repository root. Build commands use `-f backend/pom.xml` and
  `--prefix frontend` ([contracts/bash-guard.md](./contracts/bash-guard.md)).

## Global rules for every task

- Never edit any file listed in `.specify/integrations/claude.manifest.json` or
  `.specify/integrations/speckit.manifest.json` (FR-060).
- Never change `backend/src/main/**`, `frontend/src/**`, or `specs/001-clmp-mvp/**`. The only
  exception is the temporary planted defects on the discarded scratch branch in T038 (FR-090).
- Never write a token or token-like value into any file (FR-051).
- Save agent reports **verbatim**. Annotations go only in `> Note (implementing session): …`
  blocks (data-model §3).

---

## Phase 1: Setup

**Purpose**: Evidence location, ignore rules, and the pre-change baseline

- [ ] T001 Create `specs/002-agentic-engineering/evidence/` with an `evidence/README.md` index skeleton. It has a table with the columns `file | produced by (agent or session) | date | commit reviewed | story`, filled in by later tasks (data-model §3).
- [ ] T002 [P] Add `.claude/settings.local.json` under a new `# Claude Code (personal)` heading in the root `.gitignore` (research R14). Change no other line.
- [ ] T003 Record the baseline in `evidence/baseline.md`, before any other repository change:
  - Run `./backend/mvnw -f backend/pom.xml verify` and copy the final Surefire `Tests run: N, Failures: F, Errors: E, Skipped: S` line and `BUILD SUCCESS|FAILURE`.
  - Run `npm --prefix frontend run typecheck` and `npm --prefix frontend run build` and record the outcome of each.
  - Run `npm --prefix frontend test -- --run` and record the outcome. On this `/mnt/c` checkout, expect the WSL worker timeout; record it verbatim as an environment blocker.
  - Record `java -version` and `node -v`.
  - Record the Spec Kit hash check from [quickstart.md § Final checks](./quickstart.md#final-checks-record-in-evidencefinal-checksmd) (SC-006); the expected `DIFF` count is `0`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: The Bash guard and the two CLMP skills that all three agents depend on

**⚠️ CRITICAL**: No agent can be created until T004–T010 are complete

- [ ] T004 Create `.claude/hooks/clmp-bash-guard.mjs` exactly per [contracts/bash-guard.md](./contracts/bash-guard.md):
  - Node ≥ 20 ES module with no dependencies. Mode comes from `process.argv[2]` (`reviewer` | `validator`). It reads the hook JSON from stdin and uses `tool_input.command`.
  - Rule 1 (single command): block newline, `;`, `&&`, `||`, `|`, `` ` ``, `$(`, `${`, `<`, `>`, and a trailing `&`, after stripping one trailing `2>&1`.
  - Rule 2: the per-mode allowlist table.
  - Rule 3: the argument checks (git `-c`/`--output`/`--exec`/`-o`; Maven goals limited to `test`/`verify` with only the listed `-D` keys and required `-f backend/pom.xml`; `npm --prefix frontend ci` only when `frontend/node_modules` does not exist).
  - Exit `0` to allow. To block, exit `2` with the single stderr line `clmp-bash-guard: <mode> may not run "<first 80 chars>" — report the need in your report instead.` Malformed JSON or an unknown mode blocks.
  - A short header comment states its purpose and cites FR-003.
- [ ] T005 Run every row of the [bash-guard test table](./contracts/bash-guard.md#test-cases-run-during-implementation-see-quickstart-q-v1) through the guard, e.g. `echo '{"tool_input":{"command":"git checkout main"}}' | node .claude/hooks/clmp-bash-guard.mjs reviewer; echo $?` (quickstart Q-V1). All 15 rows must match. Record a `command | mode | expected | actual` table in `evidence/guard-tests.md`. If any row fails, fix T004 and rerun all rows.
- [ ] T006 [P] Create `.claude/skills/clmp-change-workflow/SKILL.md` per [contracts/skill-contracts.md § clmp-change-workflow](./contracts/skill-contracts.md#clmp-change-workflow):
  - Frontmatter: `name: clmp-change-workflow`; the draft `description` starting "CLMP project rule (not a Spec Kit step): …"; `metadata: { owner: clmp }`. No `allowed-tools`, `context`, or `disable-model-invocation`.
  - Body: the 8 ordered steps. Link to `.specify/memory/constitution.md`, `CLAUDE.md`, `specs/001-clmp-mvp/contracts/authorization-matrix.md`, `specs/001-clmp-mvp/contracts/rest-api.md`, `specs/001-clmp-mvp/data-model.md`, and `references/architecture-rules.md` instead of copying them.
- [ ] T007 [P] Create `.claude/skills/clmp-change-workflow/references/architecture-rules.md`. It is the concise CLMP rule sheet used by implementer, analyst, and reviewer alike. Each rule cites its source and a concrete code anchor:
  - Layering (constitution IV), with the module package layout `com.ensar.clmp.<module>.{domain,service,web}`.
  - DTO boundary (IV; enforced by `backend/src/test/java/com/ensar/clmp/architecture/NoEntityInControllerSignatureTest.java`).
  - Server-side role gates (`@PreAuthorize` in controllers, `auth/config/SecurityConfig.java`) and ownership checks (`*AccessPolicy` / `*Specifications` in services; matrix "Ownership scopes", including "Unlinked RECRUITER").
  - Lifecycle: all consultant status changes go through `lifecycle/ConsultantLifecycleService.java`; manual transitions come from its `MANUAL` map; automatic transitions record the actor and trigger (001 FR-031, FR-100).
  - Sensitive data: no secrets or PII in logs or list DTOs; history stores field names only (001 FR-024, FR-103; `security/SensitiveFieldExposureTest.java`).
  - Errors: `ErrorCode` + `ProblemDetail`; optimistic versioning via `VersionGuard` (409 `CONCURRENT_MODIFICATION`).
  - Frontend: `permissions.ts` is UX only; reuse the shared components; handle loading, empty, validation, and error states.
  - Each rule carries the severity the reviewer must assign: "blocking" for IV, VII, matrix, and lifecycle rules (data-model §2.2).
- [ ] T008 [P] Create `.claude/skills/clmp-release-readiness/SKILL.md` per [contracts/skill-contracts.md § clmp-release-readiness](./contracts/skill-contracts.md#clmp-release-readiness):
  - Frontmatter: `name: clmp-release-readiness`; the draft `description` starting "CLMP project rule (not a Spec Kit step): …"; `metadata: { owner: clmp }`. No `allowed-tools`.
  - Body: the 4 steps; the Quality Gate → evidence table; the verdicts `ready` / `not ready` / `blocked by environment`; "an environment blocker is never reported as ready"; and a link to the validation report format in `specs/002-agentic-engineering/data-model.md` §2.3.
- [ ] T009 [P] Create `.claude/skills/clmp-release-readiness/references/check-selection.md`, containing the changed-paths → checks table from [contracts/skill-contracts.md](./contracts/skill-contracts.md#referencescheck-selectionmd-rules) verbatim:
  - The exact commands (`./backend/mvnw -q -f backend/pom.xml test -Dtest=<Class>[,<Class>]`, `./backend/mvnw -f backend/pom.xml verify`, `npm --prefix frontend run typecheck`, `npm --prefix frontend test -- --run`, `npm --prefix frontend run build`, `-Dtest=AuthorizationMatrixTest,SensitiveFieldExposureTest`, and `-Dgroups=perf`).
  - The rules "targeted first; full backend verify always for backend changes" and "re-run a failed check at most once; differing results = unstable".
- [ ] T010 [P] Create `.claude/skills/clmp-release-readiness/references/environment-blockers.md` per [contracts/skill-contracts.md](./contracts/skill-contracts.md#referencesenvironment-blockersmd) and research R15:
  - The toolchain version rule.
  - The WSL rule: the output contains `Timeout waiting for worker to respond` **and** the repo path starts with `/mnt/` → `environment blocker`, "frontend tests were not exercised", and the two README workarounds.
  - The port/network rule.
  - The classification definitions from data-model §2.3, verbatim.

**Checkpoint**: The guard passes all 15 cases, and both skills exist with their references. Agents can now be built.

---

## Phase 3: User Story 1 — Analyze Impact Before Changing CLMP (Priority: P1) 🎯 MVP

**Goal**: A read-only analyst returns a 9-section impact report for any described CLMP change.

**Independent Test**: Quickstart US1. The report has all 9 sections, names the right modules,
rules, and tests, flags an out-of-scope/infrastructure request, and `git status --porcelain` is
unchanged.

- [ ] T011 [US1] Create `.claude/agents/clmp-codebase-analyst.md` per [contracts/agent-contracts.md](./contracts/agent-contracts.md):
  - Frontmatter: `name: clmp-codebase-analyst`; the draft `description` (≤ ~400 chars, with "Use when …"); `tools: Read, Grep, Glob`; `model: sonnet`; `skills: [clmp-change-workflow]`. No `hooks`, because there is no Bash.
  - Body: the role; the common rules 1–5; the "Must read" list; the "Must flag" list; the exact Impact report format from data-model §2.1 (the header plus the 9 sections in order); and "return only the report".
- [ ] T012 [US1] Run quickstart US1 in a fresh Claude Code session:
  - Steps 1–4: the "visa type column on submission list" analysis plus the `git status` diff.
  - Step 5: the PostgreSQL out-of-scope probe.
  - Save both reports verbatim, plus the `git status` comparison, to `evidence/us1-analyst-check.md`. If a section is missing or the probe isn't flagged, fix T011 and rerun.

**Checkpoint**: The analyst works on its own (FR-010–012, SC-003 for the analyst).

---

## Phase 4: User Story 2 — Review a Change Independently (Priority: P1)

**Goal**: A read-only reviewer, confined by the guard, reports findings in the fixed format and
never edits.

**Independent Test**: Q-V1b shows the guard blocks the reviewer's writes. A review of a known
clean commit returns "No findings." with the "Checked" list. The full proof is Part 1/Part 2 in
US6.

- [ ] T013 [US2] Create `.claude/agents/clmp-code-reviewer.md` per [contracts/agent-contracts.md](./contracts/agent-contracts.md):
  - Frontmatter:
    - `name: clmp-code-reviewer`; the draft `description`.
    - `tools: Read, Grep, Glob, Bash, mcp__github__get_me, mcp__github__list_pull_requests, mcp__github__pull_request_read, mcp__github__list_commits, mcp__github__get_commit, mcp__github__issue_read`.
    - `model: opus`; `skills: [clmp-change-workflow, clmp-release-readiness]`.
    - `hooks`: `PreToolUse` with matcher `Bash` running `node .claude/hooks/clmp-bash-guard.mjs reviewer`.
  - Body:
    - The accepted inputs and the default target (`git diff main...HEAD` plus uncommitted changes; never the whole repo).
    - "Do not use commit messages, PR descriptions, or the implementer's explanation as evidence."
    - The checked areas and the severity rules (blocking for constitution IV/VII, matrix, lifecycle).
    - The Review report format from data-model §2.2, with the fields `id`, `severity` (`blocking` · `major` · `minor` · `note`), `location`, `description`, `rule`, and `suggestion`.
    - The separate Scope deviations and Spec questions sections; the verdicts `approve` / `changes requested` / `cannot review`; "No findings." explicitly when there are none.
    - The large-diff rule (> ~800 lines).
    - "If GitHub tools fail, continue with local git and note it."
- [ ] T014 [US2] Run quickstart Q-V1b. Ask the reviewer agent to run `touch /tmp/clmp-guard-probe` first. Confirm that the call is blocked with a `clmp-bash-guard:` message and `/tmp/clmp-guard-probe` doesn't exist, and that the main session's own Bash still works. Append the result to `evidence/guard-tests.md`. **Fallback** (if frontmatter hooks don't fire): move the hook into `.claude/settings.json` `hooks.PreToolUse`, keyed to the subagent per the hooks docs, and record a deviation (collected by T046).
- [ ] T015 [US2] Smoke review (spec US2 AS-4, AS-5):
  - Run `git status --porcelain`, then ask: "Use clmp-code-reviewer to review commit b46385e (fix: stabilize list name search inputs)."
  - Save the report verbatim to `evidence/us2-reviewer-smoke.md`. Confirm it has the "Checked" section, findings in the required fields (or "No findings."), and a verdict. Then re-check that `git status --porcelain` is unchanged.

**Checkpoint**: The reviewer is confined and produces correctly formatted reports.

---

## Phase 5: User Story 3 — Validate a Change Independently (Priority: P1)

**Goal**: The validator selects and runs checks, classifies outcomes (including the WSL blocker),
and gives a verdict.

**Independent Test**: Quickstart US3. A "full" validation on this `/mnt/c` checkout reports
backend `passed`, frontend tests `blocked` / `environment blocker` with the workarounds, and the
verdict `blocked by environment`. No tracked file changes.

- [ ] T016 [US3] Create `.claude/agents/clmp-test-validator.md` per [contracts/agent-contracts.md](./contracts/agent-contracts.md):
  - Frontmatter: `name: clmp-test-validator`; the draft `description`; `tools: Read, Grep, Glob, Bash`; `model: sonnet`; `skills: [clmp-release-readiness]`; `hooks`: `PreToolUse` with matcher `Bash` running `node .claude/hooks/clmp-bash-guard.mjs validator`.
  - Body:
    - The procedure: determine the changed paths (`git diff --name-only <base>...HEAD` plus `git status --porcelain`); apply `references/check-selection.md`; run targeted tests before full ones; re-run a failure at most once; apply `references/environment-blockers.md`.
    - The prohibitions: no code or test edits, no installs, no perf unless selected.
    - The Validation report format from data-model §2.3: the `check`, `command`, `reason`, `outcome` (`passed` · `failed` · `not run` · `blocked`), `classification` (`passed` · `product failure` · `test failure` · `environment blocker` · `not run`), `duration`, and `detail` columns; test counts; and the verdict `ready` / `not ready` / `blocked by environment`.
- [ ] T017 [US3] Run quickstart US3: `git status --porcelain` before, then "Use clmp-test-validator to validate the working tree, full.", then `git status --porcelain` after. Save the report verbatim plus both statuses to `evidence/us3-validation-full.md`. Confirm the WSL classification (SC-004), that every check row is present, and that there is no tracked-file change. If misclassified, fix T010/T016 and rerun.

**Checkpoint**: All three P1 agents work on their own. Every check so far ran inside Claude Code; GitHub isn't involved yet.

---

## Phase 6: User Story 4 — Make a CLMP Change the Project's Way (Priority: P2)

**Goal**: The two skills trigger in fresh sessions, are followed, and are visibly separate from
Spec Kit.

**Independent Test**: Quickstart US4 steps 1–3. SC-006 still holds.

- [ ] T018 [US4] Run quickstart US4 in a **fresh** session:
  - Step 1: a change request → `clmp-change-workflow` is invoked. Stop after the plan; do not implement.
  - Step 2: "Is this branch ready to merge?" → `clmp-release-readiness` is invoked and delegates to `clmp-test-validator`.
  - Step 3: the `/` menu lists `clmp-*` and `speckit-*` separately, and the descriptions say "CLMP project rule (not a Spec Kit step)".

  Save the session excerpts to `evidence/us4-skills-check.md`. If a skill doesn't trigger, tighten its `description` (T006/T008) and rerun.
- [ ] T019 [US4] Re-run the SC-006 Spec Kit hash check from quickstart Final checks and append the result (`DIFF` count must be `0`) to `evidence/us4-skills-check.md`.

**Checkpoint**: The skills are usable directly and through the agents. Spec Kit is untouched.

---

## Phase 7: User Story 5 — Work With GitHub Under Limited Access (Priority: P2)

**Goal**: A shareable GitHub MCP configuration with no secrets and four layers of access control.

**Independent Test**: Quickstart Q-V2–Q-V4 and US5. The read tools work without a prompt, writes
prompt, merge is refused, and an unset token fails clearly.

- [ ] T020 [P] [US5] Create `.mcp.json` at the repository root, exactly matching the shape in [contracts/github-mcp-access.md](./contracts/github-mcp-access.md#mcpjson-shape-no-secret):
  - Server `github`; `type: "http"`; `url: "https://api.githubcopilot.com/mcp/"`.
  - Headers: `Authorization: "Bearer ${CLMP_GITHUB_PAT}"` (no default value); `X-MCP-Toolsets: "context,repos,issues,pull_requests"`; `X-MCP-Tools` with the 12 tools `get_me,list_pull_requests,search_pull_requests,pull_request_read,list_branches,list_commits,get_commit,list_issues,search_issues,issue_read,issue_write,add_issue_comment`.
- [ ] T021 [P] [US5] Create `.claude/settings.json` per [contracts/github-mcp-access.md § Tool policy](./contracts/github-mcp-access.md#tool-policy):
  - `permissions.allow`: the 10 read tools as `mcp__github__<tool>`.
  - `permissions.ask`: `mcp__github__issue_write`, `mcp__github__add_issue_comment`.
  - `permissions.deny`: the 14 tools `merge_pull_request`, `update_pull_request`, `update_pull_request_branch`, `create_pull_request`, `pull_request_review_write`, `push_files`, `create_or_update_file`, `delete_file`, `create_branch`, `create_repository`, `fork_repository`, `sub_issue_write`, `update_issue_comment`, `actions_run_trigger`, each as `mcp__github__<tool>`.
  - Do **not** set `enableAllProjectMcpServers` or `enabledMcpjsonServers`. Add no hooks and no other keys (FR-053).
- [ ] T022 [US5] Run the SC-005 credential scan from quickstart Final checks against the working tree (`git grep -nE '…' -- . ':!specs/002-agentic-engineering/research.md' ':!specs/002-agentic-engineering/quickstart.md'`, plus a check that the pattern literal itself is the only hit in those two docs). Record the command and result in `evidence/us5-github-check.md`. The expected result is no match in any config file.
- [ ] T023 [US5] [developer] Run quickstart Q-V2 (`/mcp` tool list = the 12 allowlisted tools) and Q-V2b (`env -u CLMP_GITHUB_PAT claude` → the `github` server fails with a clear message, and the analyst still answers). Record the observed tool list and the exact message in `evidence/us5-github-check.md`. If the allowlist isn't honored, record "relying on layer 3 deny list" as a deviation.
- [ ] T024 [US5] [developer] Run quickstart Q-V3 ("merge PR #1 with the github tools") and confirm it is refused by the permission rules before any tool call. Run Q-V4 (an `add_issue_comment` on an issue or PR the developer chooses) and confirm the `ask` prompt appears and the comment posts with Issues R/W and Pull requests R only. Record both in `evidence/us5-github-check.md`. If Q-V4 fails, apply the [deviation fallback](./contracts/github-mcp-access.md#deviation-fallback) and record it.
- [ ] T025 [US5] [developer] Run the quickstart US5 read check ("List open PRs and branches for this repository with the github tools") and record that it succeeds without a permission prompt in `evidence/us5-github-check.md`.

**Checkpoint**: GitHub works under limited access, and no secret is in the repository.

---

## Phase 8: User Story 6 — Prove the Layer Works on Real CLMP Work (Priority: P2)

**Goal**: The Part 1 full loop on a kept, test-only change, and the Part 2 seeded-defect review on
a discarded branch, with traceable evidence.

**Independent Test**: The evidence contains separate impact, review, and validation reports; a
resolution row for every finding (SC-007); a 4/4 scorecard (SC-002); and no `scratch/*` branch
locally or on the remote.

### Part 1 — Full loop (kept)

- [ ] T026 [US6] Commit the work so far on `002-agentic-engineering`. Ask the developer first (Global: commit only when asked). This gives the reviewer a clean base for Part 1. Record the SHA as the "Part 1 base" in `evidence/README.md`.
- [ ] T027 [US6] Analysis: in a fresh session, run the quickstart US6 Part 1 step 1 prompt with `clmp-codebase-analyst`. Save it verbatim to `evidence/part1-impact.md`.
- [ ] T028 [US6] Implement per `clmp-change-workflow`, informed by `evidence/part1-impact.md`:
  - Add the backend integration test `backend/src/test/java/com/ensar/clmp/it/UnlinkedRecruiterScopeIT.java`, or the class or location the impact report recommends, recorded in a Note block. It extends `IntegrationTestBase`.
  - Assert that ADMIN gets `totalItems > 0` on `GET /api/marketing-assignments`, `GET /api/submissions`, and `GET /api/placements`, and that `recruiter3` gets `200` with `totalItems == 0` on each.
  - Add a class comment citing the 001 spec edge case "A RECRUITER user not linked to a recruiter profile…", `contracts/authorization-matrix.md` "Unlinked RECRUITER", and FR-004/FR-015.
  - No production code change (research R11).
- [ ] T029 [US6] Review round 1: run the quickstart Part 1 step 3 prompt with `clmp-code-reviewer` in a fresh context. Save it verbatim to `evidence/part1-review-1.md`.
- [ ] T030 [US6] Create `evidence/part1-findings.md` with one row per finding in `part1-review-1.md`: `finding | severity | resolution (fixed · accepted · rejected) | reason | change | confirmed by` (data-model §3.1). Apply the fixes for the `fixed` rows to the test file only. If there are no findings, write "No findings in part1-review-1.md" and skip T031.
- [ ] T031 [US6] Review round 2, only if T030 changed code: rerun the reviewer and save to `evidence/part1-review-2.md`. Fill in the `confirmed by` column in `part1-findings.md`. Repeat T030/T031 until there are no blocking or major findings left open, numbering rounds `-3`, `-4`, and so on.
- [ ] T032 [US6] Validation: ask "Use clmp-release-readiness: is the Part 1 change ready to merge?". This delegates to `clmp-test-validator`. Save the validator report verbatim to `evidence/part1-validation-1.md` and the readiness Quality Gate summary below it.
  - **Expect**: `UnlinkedRecruiterScopeIT` passes; the backend test count equals the T003 baseline plus the new tests; frontend checks are `not run` ("no frontend change"); the verdict is `ready`.
  - If not `ready`, fix and loop back to T029 (data-model §3.2).
- [ ] T033 [US6] Check SC-008: list `git diff --name-only <Part 1 base>...HEAD` and confirm every listed file appears in `evidence/part1-impact.md`. Record the result in `evidence/part1-findings.md` under "SC-008". Commit Part 1 when the developer agrees.

### Part 2 — Seeded-defect review (discarded)

- [ ] T034 [US6] Create a local branch `scratch/002-seeded-review` from the Part 1 commit. Plant D1–D4 exactly as in [research.md R12](./research.md#r12-part-2-seeded-defect-exercise-sc-002), all under `backend/src/main/java/com/ensar/clmp/`:
  - D1: a `GET /api/consultants/{id}/raw` endpoint returning the `Consultant` entity in `consultant/web/ConsultantController.java`.
  - D2: remove `access.assertCanView(...)` from `MarketingService.history` in `marketing/service/MarketingService.java`.
  - D3: add `PLACED` to `MANUAL.put(BENCH, …)` in `lifecycle/ConsultantLifecycleService.java`.
  - D4: log the submitted password at INFO on failed sign-in in `auth/web/AuthController.java`.

  Commit them as **one** commit with the message `chore: scratch changes`. Then run `git diff 002-agentic-engineering...HEAD > specs/002-agentic-engineering/evidence/part2-seeded.patch` (the patch stays untracked). **Never push this branch.**
- [ ] T035 [US6] In a **fresh** session, send exactly "Use clmp-code-reviewer to review branch `scratch/002-seeded-review` against `002-agentic-engineering`." with no hints. Save the output verbatim to `evidence/part2-review.md`.
- [ ] T036 [US6] Create `evidence/part2-scorecard.md`: a table `defect | category (SC-002) | caught? (finding id) | severity blocking? | rule reference correct?` for D1–D4, then a total (target 4/4). Note any extra findings. If < 4/4, record the miss honestly. Do not re-run with hints. Improving the reviewer is a follow-up task (T048 note).
- [ ] T037 [US6] Clean up:
  - `git switch 002-agentic-engineering`, then `git branch -D scratch/002-seeded-review`.
  - Verify that `git branch --list 'scratch/*'` and `git ls-remote --heads origin 'scratch/*'` are both empty.
  - Verify that none of D1–D4 exists: `git grep -n '/raw' -- backend/src/main` returns nothing, and the `MANUAL.put(BENCH` line in `ConsultantLifecycleService.java` is unchanged from `main`.
  - Append the outputs to `evidence/part2-scorecard.md`.
- [ ] T038 [US6] Complete `evidence/README.md`: one row per evidence file (T003–T037), each with the producing agent or session, the date, and the commit SHA reviewed.

**Checkpoint**: The demonstration is complete and traceable (FR-070–073, SC-002, SC-007, SC-008).

---

## Phase 9: User Story 7 — Understand and Adopt the Layer (Priority: P3)

**Goal**: One place for a new developer to learn the layer, plus pointers from `CLAUDE.md` and the README.

**Independent Test**: The quickstart US7 five-scenario check scores ≥ 4/5 (SC-009).

- [ ] T039 [P] [US7] Create `docs/agentic-engineering.md` (FR-080) with these sections:
  1. Overview and the separation-of-responsibilities table (spec Overview).
  2. Agents: for each of the three, what it does, when to use it, what it may and may not do, its tools, its guard mode, its model, the example prompt, and the report format (link to the 002 data-model).
  3. Skills: when to use each, with their steps summarized.
  4. CLMP skills vs Spec Kit skills (the table from contracts/skill-contracts.md).
  5. The lifecycle mapping table (research R13).
  6. The GitHub integration: what it provides, the 5 control layers, the tool policy, the token setup with `CLMP_GITHUB_PAT` and the fine-grained permissions, and the failure behavior.
  7. Secrets and access: never commit tokens; `.claude/settings.local.json` is personal and ignored.
  8. The WSL `/mnt/c` note.
  9. Deviations and limitations (from plan.md and any Q-V fallbacks).
  10. How to change the layer (a reviewed change; the 3/2/1 limit unless a new spec).

  Link to the 002 spec and evidence rather than duplicating them.
- [ ] T040 [P] [US7] Edit `CLAUDE.md` (FR-081): add an `## Agentic Workflow` section after `## Agent Workflow`. It maps each lifecycle step to `clmp-codebase-analyst` (before planning or implementing), `clmp-change-workflow` (when implementing), `clmp-code-reviewer` (after a change, before readiness), and `clmp-release-readiness` → `clmp-test-validator` (before merge). It says that GitHub writes need developer confirmation and that the `speckit-*` skills must not be edited, and links `docs/agentic-engineering.md`. Change no existing line except where a pointer must be added. Do not contradict the constitution.
- [ ] T041 [P] [US7] Edit `README.md` (FR-082):
  - In `## Project Structure`, change the `.claude/` line to "Claude Code configuration: Spec Kit workflow skills (`speckit-*`), CLMP agents and skills (`clmp-*`), the agent Bash guard, and shared permissions". Add a `.mcp.json` line ("GitHub MCP integration — token from `CLMP_GITHUB_PAT`, never committed") and a `docs/` line.
  - Add a short `## Agentic Engineering` section, before `## Current Scope`, linking to `docs/agentic-engineering.md`.
- [ ] T042 [US7] Run the quickstart US7 five-scenario check: a fresh session instructed to answer using only `docs/agentic-engineering.md`, or a colleague. Record the answers and the score (target ≥ 4/5) in `evidence/us7-docs-check.md`. If < 4/5, revise T039 and rerun.

**Checkpoint**: The layer is documented and discoverable.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Final whole-branch verification against every success criterion

- [ ] T043 Final independent review of the whole branch: "Use clmp-code-reviewer to review branch `002-agentic-engineering` vs `main`." Save it verbatim to `evidence/final-review.md`. Resolve the findings in `evidence/part1-findings.md` under a "Final review" heading, using the same columns, and re-review if anything changed.
- [ ] T044 Final validation: "Use clmp-release-readiness on branch `002-agentic-engineering`." Save it to `evidence/final-validation.md`. The expected verdict is `ready`, or `blocked by environment` solely because of the WSL frontend-test blocker, with backend and frontend typecheck/build passing.
- [ ] T045 Run all quickstart Final checks: SC-001 (`git diff --stat main...HEAD -- backend/src/main frontend/src specs/001-clmp-mvp` empty; `backend/src/test` lists only the Part 1 test), SC-005, SC-006, SC-010 (3 / 2 / 1), and SC-003 (summarize the before/after `git status` comparisons from T012, T015, T017). Record the commands and outputs in `evidence/final-checks.md`, together with the Q-V outcomes collected from `guard-tests.md` and `us5-github-check.md`.
- [ ] T046 Update `plan.md § Deviations & Assumptions` and `docs/agentic-engineering.md` § Deviations with every fallback actually used (T014, T023, T024) and any reviewer miss from T036. If none were used, state "No fallbacks were needed" in both.
- [ ] T047 Re-run the SC-006 hash check and the SC-005 scan one last time after T046, and append the results to `evidence/final-checks.md`.
- [ ] T048 Prepare the change description for the human reviewer (constitution VIII). Draft it in `evidence/README.md` § "Change description":
  - Summary per user story.
  - SC-001–SC-010 results with links to evidence.
  - Deviations.
  - Developer-run checks still pending, if any.
  - Follow-ups, e.g. reviewer improvements if SC-002 < 4/4, or the optional Spec Kit `after_implement` hook (research R10).

  Do not open a PR unless the developer asks.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: none. T003 must run before **any** other repository change, because it records the baseline.
- **Foundational (Phase 2)**: depends on Phase 1. Blocks all agents (US1–US3).
- **US1, US2, US3 (Phases 3–5)**: each depends only on Phase 2. They can be built in parallel (different files), but the verification tasks should run one at a time because they share the working tree.
- **US4 (Phase 6)**: depends on Phase 2 plus at least US3 (step 2 delegates to the validator).
- **US5 (Phase 7)**: T020–T022 depend only on Phase 1. T023–T025 need the developer's token and T013 (the reviewer's GitHub tools are listed, though not required for these checks).
- **US6 (Phase 8)**: depends on US1, US2, US3, and US4. It doesn't depend on US5: the reviewer falls back to local git.
- **US7 (Phase 9)**: T039–T041 can start after Phase 2, but their content must reflect the final agent and skill text. T042 depends on T039.
- **Polish (Phase 10)**: depends on all stories.

### Story Dependency Graph

```text
Setup ─► Foundational (guard + skills)
            ├─► US1 analyst ─┐
            ├─► US2 reviewer ├─► US4 skills check ─► US6 demonstration ─┐
            └─► US3 validator┘                                         ├─► Polish
Setup ─► US5 GitHub config (T020–T022) ─► [developer] T023–T025 ───────┤
Foundational ─► US7 docs (T039–T041) ─► T042 ──────────────────────────┘
```

### Within each story

Create the definition, then run the verification scenario. Save the evidence, then fix and rerun
if the scenario fails.

---

## Parallel Opportunities

- **Phase 1**: T002 runs in parallel with T001. T003 is sequential, because it is a long build.
- **Phase 2**: T006, T007, T008, T009, and T010 run in parallel (five different files). T004 → T005 is sequential.
- **Phases 3–5**: T011, T013, and T016 (the three agent files) can be written in parallel once Phase 2 is done.
- **Phase 7**: T020 and T021 run in parallel.
- **Phase 9**: T039, T040, and T041 run in parallel.

### Example: Phase 2 parallel batch

```text
Task: "T006 Create .claude/skills/clmp-change-workflow/SKILL.md"
Task: "T007 Create .claude/skills/clmp-change-workflow/references/architecture-rules.md"
Task: "T008 Create .claude/skills/clmp-release-readiness/SKILL.md"
Task: "T009 Create .claude/skills/clmp-release-readiness/references/check-selection.md"
Task: "T010 Create .claude/skills/clmp-release-readiness/references/environment-blockers.md"
```

### Example: Agents batch (after Phase 2)

```text
Task: "T011 Create .claude/agents/clmp-codebase-analyst.md"
Task: "T013 Create .claude/agents/clmp-code-reviewer.md"
Task: "T016 Create .claude/agents/clmp-test-validator.md"
```

Then run T012 → T014 → T015 → T017 one after another.

---

## Implementation Strategy

### MVP first (User Story 1, plus the foundation)

1. Phase 1 (with the baseline) → Phase 2 (guard + skills).
2. Phase 3: the analyst plus its verification.
3. **Stop and validate**: the analyst gives useful, read-only impact reports. This is already
   useful for any CLMP change.

### Incremental delivery

1. + US2 reviewer, then + US3 validator. Together with the analyst, that completes the
   independent analysis → review → validation trio.
2. + US4 skills check (fresh-session triggering).
3. + US5 GitHub config. The developer-run checks can happen whenever the token is available.
4. + US6 demonstration: the acceptance test for the whole feature.
5. + US7 docs → Polish (final review, validation, and checks).

### Notes

- Agents must be invoked in fresh contexts for every evidence-producing run (independence,
  FR-026). Part 2 additionally needs a fresh **session**, so that nothing about the planted
  defects is in context.
- Every story checkpoint is a sensible commit point. Commit only when the developer asks.
