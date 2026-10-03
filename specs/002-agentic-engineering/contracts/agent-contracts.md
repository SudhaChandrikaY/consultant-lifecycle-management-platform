# Contract: Specialist Agents

**Feature**: `002-agentic-engineering` · Sources: [research](../research.md) R2–R5 ·
Report formats: [data-model §2](../data-model.md#2-report-entities-agent-outputs)

All three agents live in `.claude/agents/`, run in their own context (the parent conversation is
not passed in), load `CLAUDE.md` automatically, and are invoked by name ("use the
clmp-code-reviewer agent to …") or chosen by the main session from their `description`.

## Summary

| | `clmp-codebase-analyst` | `clmp-code-reviewer` | `clmp-test-validator` |
|---|---|---|---|
| Purpose | Impact and scope before a change | Independent judgment of a change | Independent selection and execution of checks |
| `tools` | `Read, Grep, Glob` | `Read, Grep, Glob, Bash, mcp__github__get_me, mcp__github__list_pull_requests, mcp__github__pull_request_read, mcp__github__list_commits, mcp__github__get_commit, mcp__github__issue_read` | `Read, Grep, Glob, Bash` |
| Bash guard mode | — (no Bash) | `reviewer` | `validator` |
| `model` | `sonnet` | `opus` | `sonnet` |
| `skills` | `clmp-change-workflow` | `clmp-change-workflow`, `clmp-release-readiness` | `clmp-release-readiness` |
| Writes files | Never | Never | Never edits tracked files; build output in ignored directories only |
| GitHub write | No | No (posting a review is the main session's job, after the developer agrees) | No |
| Output | Impact report | Review report | Validation report |

## Common rules (all three bodies)

1. Read `.specify/memory/constitution.md` and the active spec (from `.specify/feature.json`, or as
   given in the prompt) before concluding anything.
2. Cite repository paths for every claim about code.
3. Return **only** the report in the data-model format. Do not ask the user questions; record
   unknowns under open questions or "cannot review".
4. If asked to edit code, refuse in the report and hand the edit back to the main session.
5. Never print secrets, environment variable values, or token-like strings.

## `clmp-codebase-analyst`

- **Input**: a described change, optionally a spec/task id.
- **Must read**: the active spec, plan, and `contracts/*` for the area, the relevant module packages
  under `backend/src/main/java/com/ensar/clmp/<module>/`, the matching `frontend/src/pages/<area>/`,
  and the tests under `backend/src/test/java/com/ensar/clmp/**` and `frontend/src/test/**`.
- **Must flag**: out-of-scope work, any stack or infrastructure change (constitution III/V), and
  any change to the authorization matrix or business requirements (needs a spec change).
- **Description (draft)**: "Read-only CLMP impact analysis. Use before planning or implementing a
  CLMP change to find affected modules, layers, files, role and ownership rules, lifecycle
  transitions, contracts, and existing tests. Does not modify files."

## `clmp-code-reviewer`

- **Input**: one of: `branch <name> vs <base>` (default base `main`), `working tree`, `commit
  <sha>[..<sha>]`, or `PR #<n>`. With no input, it reviews `git diff main...HEAD` plus
  uncommitted changes. It never reviews the whole repository by default.
- **Must not use as evidence**: the implementing session's explanation, commit messages, or PR
  descriptions. They may be read only to identify the claimed scope.
- **Must check**: every area in data-model §2.2 "Checked", using the preloaded skills as its rule
  set.
- **Large diffs**: over ~800 changed lines, it states which files were reviewed in depth.
- **Description (draft)**: "Independent CLMP code reviewer. Use after a change is made, before
  merge, to review a diff, branch, or PR against the spec, constitution, authorization matrix,
  layering and DTO rules, lifecycle rules, and sensitive-data rules. Read-only; reports findings
  and never edits code."

## `clmp-test-validator`

- **Input**: the change to validate (same forms as the reviewer), optionally "full" to force all
  checks.
- **Procedure**: follow `clmp-release-readiness` → `references/check-selection.md`. Run each
  selected check as a single guarded command. Re-run a failed check at most once (unstable if the
  results differ).
- **Must not**: edit code or tests, install or upgrade tools, run `perf` unless the rules select it
  or it is asked to.
- **Description (draft)**: "Independent CLMP test validator. Use after a change (or after review
  fixes) to choose and run the right backend and frontend checks and report pass/fail with
  failure classification, including WSL environment blockers. Does not edit code or tests."
