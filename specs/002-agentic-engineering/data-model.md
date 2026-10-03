# Data Model: CLMP Agentic Engineering Layer

**Feature**: `002-agentic-engineering` | **Date**: 2026-10-03

This feature stores no application data. Its "entities" are configuration files and the
Markdown reports that agents produce. This document fixes their fields so that reports can be
compared across runs (FR-005) and evidence can be traced (FR-071). The file-level contracts are in
[contracts/](./contracts/).

---

## 1. Configuration entities

### 1.1 Specialist agent definition

File: `.claude/agents/<name>.md`, with YAML frontmatter followed by a Markdown body (the system
prompt).

| Field | Rule |
|-------|------|
| `name` | Must be one of `clmp-codebase-analyst`, `clmp-code-reviewer`, or `clmp-test-validator`. Must match the filename. |
| `description` | Starts with what the agent does, then "Use when …" / "Do not use for …". At most ~400 characters. |
| `tools` | An explicit allowlist per [contracts/agent-contracts.md](./contracts/agent-contracts.md). Must never include `Edit`, `Write`, `NotebookEdit`, or `Agent`. |
| `model` | Set per research R4. |
| `skills` | The preloaded CLMP skills per research R5. |
| `hooks` | Required when `tools` contains `Bash`: a `PreToolUse` hook on `Bash` that runs the guard in the agent's mode. |
| Body | Covers the role, the inputs it expects, the documents it must read, its hard prohibitions, and the exact report format (§2). |

**Invariant**: exactly three agent definitions exist (FR-001, SC-010).

### 1.2 CLMP skill

Directory: `.claude/skills/<name>/`, containing `SKILL.md` and optional `references/*.md`.

| Field | Rule |
|-------|------|
| `name` | Must be `clmp-change-workflow` or `clmp-release-readiness`. Must match the directory. |
| `description` | Says when to use the skill, and states "CLMP project rule; not a Spec Kit step". |
| `allowed-tools` | Must not be set (research R6). |
| `metadata.owner` | `clmp`. This distinguishes the skill from `metadata.author: github-spec-kit`. |
| Body | Ordered steps. Links to governing documents instead of copying them. |

**Invariant**: exactly two CLMP skills exist (FR-044), and no `speckit-*` file changes (FR-060).

### 1.3 GitHub integration configuration

| File | Content |
|------|---------|
| `.mcp.json` | `mcpServers.github`: the remote URL, an `Authorization` header that references `${CLMP_GITHUB_PAT}`, and the `X-MCP-Tools` and `X-MCP-Toolsets` headers. |
| `.claude/settings.json` | `permissions.allow` / `ask` / `deny` for `mcp__github__*` tools. Does **not** set `enableAllProjectMcpServers` or `enabledMcpjsonServers`. |
| Developer environment | `CLMP_GITHUB_PAT`, a fine-grained token (research R9). Never in any repository file. |

**Invariant**: no committed file matches the credential pattern in research R14 (SC-005).

---

## 2. Report entities (agent outputs)

Every report starts with this header:

```text
Report: <Impact|Review|Validation>  ·  Agent: <agent name>  ·  Run: <evidence id or "ad hoc">
Target: <described change | branch vs base | PR #n | working tree>
Spec scope: <spec path and story/task ids, or "trivial fix (constitution I)", or "none found">
```

### 2.1 Impact report (`clmp-codebase-analyst`)

Its sections appear in this order (FR-010–012):

1. **Summary**: one or two sentences.
2. **Scope check**: in scope, out of scope (cite the boundary), or needs a new spec (infrastructure
   or a stack change).
3. **Affected modules**: a table of module | layer(s) | files (paths, plus symbols where helpful).
4. **Requirements**: FR, user-story, acceptance-scenario, and SC ids, with one-line quotes.
5. **Authorization**: the relevant matrix rows and ownership scopes, quoted from
   `contracts/authorization-matrix.md`.
6. **Lifecycle**: the states and transitions involved (manual or automatic), plus the history
   records they write.
7. **Contracts and data**: the endpoints, DTOs, and entities involved.
8. **Existing tests**: the tests covering the area, and the gaps.
9. **Risks and open questions**.

### 2.2 Review report (`clmp-code-reviewer`)

1. **Checked**: the rule areas reviewed (spec/task scope, constitution, authorization matrix,
   layering, DTO boundary, lifecycle, sensitive data, tests), and the files reviewed in depth
   versus skimmed.
2. **Findings**: one row per finding:

   | Field | Values |
   |-------|--------|
   | `id` | `R<n>` (unique within the report) |
   | `severity` | `blocking` · `major` · `minor` · `note` |
   | `location` | `path:line` or `path` (area) |
   | `description` | What is wrong, and the concrete failure scenario |
   | `rule` | e.g. `Constitution IV`, `authorization-matrix: Recruiter own marketing`, `001 FR-031`, `001 FR-103` |
   | `suggestion` | The direction for a correction, not code |

   Severity rules: any violation of constitution IV (DTO boundary, layering), VII (server-side
   authorization, sensitive data), the authorization matrix, or the lifecycle transition rules is
   **blocking** (spec US2 AS-2).
3. **Scope deviations**: changes outside the task or spec scope, listed separately (FR-023).
4. **Spec questions**: concerns about the spec itself, routed to the human reviewer.
5. **Verdict**: `approve` (no blocking or major findings), `changes requested`, or
   `cannot review` (with the reason). When there are no findings, the report says
   "No findings." explicitly (FR-024).

### 2.3 Validation report (`clmp-test-validator`)

1. **Changed areas**: backend, frontend, authorization or sensitive data, with the evidence (the
   changed paths).
2. **Checks**: one row per check:

   | Field | Values |
   |-------|--------|
   | `check` | e.g. `backend targeted`, `backend verify`, `frontend typecheck`, `frontend test`, `frontend build`, `authorization tests`, `perf smoke` |
   | `command` | The exact command run, or `—` |
   | `reason` | Why it was run or skipped (from the check-selection rules) |
   | `outcome` | `passed` · `failed` · `not run` · `blocked` |
   | `classification` | `passed` · `product failure` · `test failure` · `environment blocker` · `not run` |
   | `duration` | Wall-clock time, e.g. `1m42s` |
   | `detail` | For failures: the test name, the related story or acceptance criterion, and a short excerpt. For blockers: the cause and the workarounds. |

3. **Test counts**: backend tests run / failed / skipped (from the Surefire summary); frontend
   tests where they ran.
4. **Verdict**: `ready`, `not ready`, or `blocked by environment` (FR-035).

Classification rules: a failure inside production code paths under test is a `product failure`.
A failure caused by the test itself (wrong fixture or assertion), when the code matches the spec,
is a `test failure`. A tool missing or at the wrong version, or the WSL `/mnt/` Vitest worker
timeout (research R15), is an `environment blocker`. When the validator can't tell a product
failure from a test failure, it reports `product failure` and says that it is uncertain.

---

## 3. Evidence entities

Directory: `specs/002-agentic-engineering/evidence/`

| File | Content |
|------|---------|
| `README.md` | Index: the run ids, which agent produced which file, dates, and the commit SHAs reviewed |
| `baseline.md` | Pre-change baseline: backend test counts, the frontend check status, and the Spec Kit hash check result |
| `part1-impact.md` | Impact report (§2.1) |
| `part1-review-<n>.md` | Review report(s) (§2.2); `n` = 1, 2, … per review round |
| `part1-validation-<n>.md` | Validation report(s) (§2.3) |
| `part1-findings.md` | The finding resolution log (§3.1) |
| `part2-seeded.patch` | The planted-defect diff (research R12) |
| `part2-review.md` | The reviewer's report on the scratch branch, unedited |
| `part2-scorecard.md` | The planted defects D1–D4 mapped to reviewer findings: caught or missed, severity correct, rule correct |
| `final-checks.md` | SC-001, SC-003, SC-005, SC-006, and SC-010 results, with the commands used |

Reports are saved **verbatim** as the agent returned them. Any later annotation goes in a clearly
marked `> Note (implementing session): …` block, never as an edit to the agent's text.

### 3.1 Finding resolution record

`part1-findings.md` holds one row per review finding:

| Field | Values |
|-------|--------|
| `finding` | `<review file>#R<n>` |
| `severity` | Copied from the review |
| `resolution` | `fixed` · `accepted` · `rejected` |
| `reason` | Required for `accepted` or `rejected` |
| `change` | The commit SHA or file reference for `fixed` |
| `confirmed by` | The re-review and re-validation report that confirmed it |

**Invariant**: every finding in every Part 1 review report has a row (SC-007).

### 3.2 State flow of the demonstration (Part 1)

```text
baseline ──► impact ──► implement ──► review(n) ──► [findings?]
                                          │              │ yes
                                          │              ▼
                                          │         correct ──► review(n+1)
                                          ▼ no blocking/major
                                     validation(n) ──► [ready?] ──► human review
                                                         │ no
                                                         ▼
                                                    correct ──► review(n+1)
```

A change to code after a review always triggers another review **and** another validation round.
