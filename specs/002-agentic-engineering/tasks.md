---

description: "Task list for 002-agentic-engineering (learning scope)"
---

# Tasks: CLMP Agentic Engineering Layer

**Input**: Design documents from `/specs/002-agentic-engineering/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md),
[data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Scope**: This feature is for learning the difference between subagents, skills, and MCP. Each
component is built, then used **once** with a simple request on existing CLMP code
([quickstart.md § Demonstration](./quickstart.md#demonstration-us6-each-component-once)). No
application code or tests are added.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: The user story the task belongs to (US1–US7)

## Rules for every task

- Do not edit `.claude/skills/speckit-*` or `.specify/**`.
- Do not change `backend/`, `frontend/`, or `specs/001-clmp-mvp/`.
- Never write a token into any file.

---

## Phase 1: Setup

- [X] T001 Confirm the toolchain (`java -version` 21, `node -v` ≥ 20.19) and that the existing builds pass before adding the layer. Backend `verify`: 375 tests, 0 failures. Frontend typecheck and build pass.

---

## Phase 2: Foundational: the two CLMP skills

- [X] T002 [P] Create `.claude/skills/clmp-change-workflow/SKILL.md` per [contracts/skill-contracts.md](./contracts/skill-contracts.md#clmp-change-workflow).
- [X] T003 [P] Create `.claude/skills/clmp-release-readiness/SKILL.md` per [contracts/skill-contracts.md](./contracts/skill-contracts.md#clmp-release-readiness), including the full/all row, the WSL rule, and the Quality Gate table.

---

## Phase 3: User Story 1: Analyst (P1)

- [X] T004 [US1] Create `.claude/agents/clmp-codebase-analyst.md` (`tools: Read, Grep, Glob`; preloads `clmp-change-workflow`).
- [X] T005 [US1] Demonstrate: "Use the clmp-codebase-analyst agent to inspect the existing Consultants search and tell us which files and modules are involved." Note the result in `specs/002-agentic-engineering/demo.md`.

## Phase 4: User Story 2: Reviewer (P1)

- [X] T006 [US2] Create `.claude/agents/clmp-code-reviewer.md` (`tools: Read, Grep, Glob, Bash, mcp__github`; read-only `git` by instruction; preloads both skills).
- [X] T007 [US2] Demonstrate: "Use the clmp-code-reviewer agent to review commit b46385e (the name-search fix) and report any concerns." Note the result in `demo.md`.

## Phase 5: User Story 3: Validator (P1)

- [X] T008 [US3] Create `.claude/agents/clmp-test-validator.md` (`tools: Read, Grep, Glob, Bash`; README commands only by instruction; no installs; preloads `clmp-release-readiness`).
- [X] T009 [US3] Demonstrate: "Use the clmp-test-validator agent to run one small targeted validation for the name-search fix" (the typecheck plus its two Vitest files, not the full suites). Note the result in `demo.md`.

## Phase 6: User Story 4: Skills (P2)

- [X] T010 [US4] Demonstrate `/clmp-change-workflow`: have it explain how it would approach a small CLMP change. Note the result in `demo.md`.
- [X] T011 [US4] Demonstrate `/clmp-release-readiness`: have it list the checks it would choose for a small frontend-only change. Note the result in `demo.md`.

## Phase 7: User Story 5: Read-only GitHub MCP (P2)

- [X] T012 [P] [US5] Create `.mcp.json`: server `github`, `type: "http"`, `url: "https://api.githubcopilot.com/mcp/readonly"`, `Authorization: "Bearer ${CLMP_GITHUB_PAT}"`.
- [X] T013 [US5] **Developer**: create a fine-grained **read-only** token for this repository ([contract § Token](./contracts/github-mcp-access.md#token-clmp_github_pat)), `export CLMP_GITHUB_PAT=…` in your own shell profile, restart Claude Code, and approve the `github` server (`/mcp`).
- [X] T014 [US5] Demonstrate (after T013): "Use the github tools to read PR #1 and summarize what it did." Confirm in `/mcp` that only read tools are listed. Note the result in `demo.md`.

## Phase 8: User Story 6: Demonstration record (P2)

- [X] T015 [US6] Write `specs/002-agentic-engineering/demo.md` as one short table, `component | example request | result | what it demonstrated`, with one row per component (T005, T007, T009, T010, T011, T014).

## Phase 9: User Story 7: Documentation (P3)

- [X] T016 [P] [US7] Create `docs/agentic-engineering.md`: the agents, the skills, CLMP vs Spec Kit skills, the lifecycle mapping, GitHub MCP and token setup, secrets, the WSL note, the limitations, and the simple example prompts.
- [X] T017 [P] [US7] Add a short `## Agentic Workflow` section to `CLAUDE.md` that links the doc.
- [X] T018 [P] [US7] Update `README.md`: the `.claude/`, `.mcp.json`, and `docs/` lines in Project Structure, and a short `## Agentic Engineering` section that links the doc.
- [X] T019 [US7] Run the quickstart docs check (≥ 4/5).

## Phase 10: Polish

- [X] T020 Run the quickstart **Final checks** (SC-001, SC-005, SC-006, SC-007).

---

## Dependencies

- T002, T003 → T004, T006, T008 (the agents preload the skills).
- Each demonstration task (T005, T007, T009, T010, T011) depends only on its component.
- T014 depends on T013 (the developer's token).
- T015 collects T005–T014. T016–T019 come after it, then T020.

## Parallel Opportunities

T002/T003; T005/T007/T009 (independent agent demonstrations); T016/T017/T018.
