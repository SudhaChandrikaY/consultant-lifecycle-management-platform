# Contract: Agents

**Feature**: `002-agentic-engineering` · Sources: [research](../research.md) R2, R3 ·
Report formats: [data-model §2](../data-model.md#2-reports)

All three agents live in `.claude/agents/`. Each one runs in its own context (it does not see the
parent conversation), loads `CLAUDE.md`, and is invoked by name ("use the clmp-code-reviewer agent
to …"). The main agent may also pick one based on its `description`.

| | `clmp-codebase-analyst` | `clmp-code-reviewer` | `clmp-test-validator` |
|---|---|---|---|
| Purpose | Impact before a change | Independent review of a change | Independent check selection and execution |
| `tools` | `Read, Grep, Glob` | `Read, Grep, Glob, Bash`, plus the `mcp__github` tools | `Read, Grep, Glob, Bash` |
| `skills` | `clmp-change-workflow` | `clmp-change-workflow`, `clmp-release-readiness` | `clmp-release-readiness` |
| Bash use (by instruction) | — | Read-only `git` (`diff`, `log`, `show`, `status`, `merge-base`, `ls-files`, `blame`) | The README's build, test, and typecheck commands, plus `git diff --name-only` and `git status` |
| Never | Write files | Edit files, run builds, write to GitHub | Edit code or tests, install tools, run perf unless asked |
| Output | Impact report | Review report | Validation report |

## Common instructions (all three)

1. Read `.specify/memory/constitution.md` and the active spec and plan before concluding.
2. Cite repository paths for every claim.
3. Return only the report, in the data-model format.
4. If asked to edit code, decline and hand the edit back to the main agent.
5. Never print secrets or environment variable values.

## Specifics

**Analyst**: it reads the spec, plan, and contracts for the area, the module packages under
`backend/src/main/java/com/ensar/clmp/<module>/`, the matching `frontend/src/pages/<area>/`, and
the related tests. It flags out-of-scope or infrastructure work as needing a new spec.

**Reviewer**:
- Its input is a branch against its base (default `main`), a commit, or the working tree.
- With no input, it reviews `git diff main...HEAD` plus any uncommitted changes.
- It never reviews the whole repository by default.
- It does not use commit messages or the implementer's explanation as evidence.
- It may use the GitHub tools for pull request context. If they are unavailable, it continues
  with local `git`.

**Validator**:
- It finds the changed paths (`git diff --name-only <base>...HEAD` plus `git status --porcelain`).
- When explicitly asked for **full** or **all** validation, it runs every backend and frontend
  check in the `clmp-release-readiness` check-selection table, regardless of changed paths (perf
  still only if asked).
- Otherwise it applies that table to the changed paths and runs targeted tests before full ones.
- It re-runs a failure once, then classifies it, using the WSL rule.

## Draft descriptions

- **Analyst**: "Read-only CLMP impact analysis. Use before planning or implementing a CLMP change
  to find affected modules, layers, files, role/ownership and lifecycle rules, contracts, and
  existing tests. Does not modify files."
- **Reviewer**: "Independent CLMP code reviewer. Use after a change, before merge, to review a
  diff, branch, or commit against the spec, constitution, architecture, authorization matrix,
  lifecycle, and sensitive-data rules. Read-only; reports findings and never edits."
- **Validator**: "Independent CLMP test validator. Use after a change or after review fixes to
  choose and run the existing backend/frontend tests, typecheck, and build, and report results
  with failure classification (including the WSL frontend-test limitation). Never edits code or
  tests."
