# Quickstart: Using and Validating the CLMP Agentic Layer

**Feature**: `002-agentic-engineering` · [plan](./plan.md) · [contracts](./contracts/) ·
[data model](./data-model.md)

"Ask Claude" means typing the prompt into a Claude Code session opened at the repository root.

## Prerequisites

| Item | Check |
|------|-------|
| Claude Code ≥ 2.1 | `claude --version` |
| JDK 21, Node ≥ 20.19 | `java -version`, `node -v` |
| Read-only GitHub token (US5 only) | Created per [github-mcp-access.md § Token](./contracts/github-mcp-access.md#token-clmp_github_pat) |

GitHub setup (once per developer, outside the repository):

```bash
export CLMP_GITHUB_PAT='<fine-grained read-only token>'   # e.g. in ~/.bashrc
```

Open a new shell, start `claude`, and approve the `github` server when prompted (or use `/mcp`).

## Story checks

| Story | Do this | Expect |
|-------|---------|--------|
| US1 Analyst | Ask Claude: "Use clmp-codebase-analyst: impact of adding a 'visa type' column to the submission list." | An impact report with all 9 sections and file paths (submission web/service/DTO, `frontend/src/pages/submissions/`, `US5SubmissionIT`). `git status` is unchanged. |
| US1 out of scope | "Use clmp-codebase-analyst: impact of moving CLMP to PostgreSQL." | The report says a new approved spec is required. |
| US2 Reviewer | "Use clmp-code-reviewer to review commit b46385e." | A review report: a Checked section, findings in the required columns (or "No findings."), and a verdict. `git status` is unchanged. |
| US3 Validator | "Use clmp-test-validator to validate the working tree, full." | Every backend and frontend check runs, even though only `.claude/` and `specs/` changed (full/all overrides changed-path selection). A row for every check. On a `/mnt/c` checkout, the frontend tests are reported as an environment blocker with the workarounds, and the backend and frontend typecheck/build pass. No tracked file changes. |
| US4 Skills | In a fresh session: "Add a missing test for an existing CLMP acceptance criterion", then "Is this ready to merge?" | `clmp-change-workflow` and then `clmp-release-readiness` are used; the readiness answer lists every constitution Quality Gate. The `/` menu shows `clmp-*` and `speckit-*` separately. |
| US5 GitHub | `/mcp` → `github` → list tools. Then: "List open PRs and branches for this repository." | Only read tools are listed, and the PRs and branches are returned. |
| US7 Docs | Using only `docs/agentic-engineering.md`, answer the five situations below. | At least 4 of 5 correct. |

US7 situations and their answers:

1. Before planning a placement change, what is affected? → analyst
2. I'm implementing a task from `tasks.md`. → change workflow
3. Is my change right against the auth matrix? → reviewer
4. Can we merge? → readiness (→ validator)
5. Which PRs are open? → GitHub MCP

## Demonstration (US6)

Record everything in `specs/002-agentic-engineering/demo.md` ([data-model §3](./data-model.md#3-demonstration-record)).

| Step | Who | Action |
|------|-----|--------|
| 0 | Main agent | Baseline: `cd backend && ./mvnw verify`, then copy the final `Tests run:` line; `cd frontend && npm run typecheck && npm run build`. |
| 1 | Analyst | "Use clmp-codebase-analyst: impact of adding an integration test that an unlinked RECRUITER (recruiter3) gets empty marketing, submission and placement lists (001 spec edge case; authorization-matrix 'Unlinked RECRUITER')." |
| 2 | Main agent (using `clmp-change-workflow`) | Add the test only. No production code changes. |
| 3 | Reviewer | "Use clmp-code-reviewer: review the uncommitted changes in the working tree" (or the commit). |
| 4 | Main agent | Record each finding's outcome. Fix if needed, then re-review. |
| 5 | Validator (via `clmp-release-readiness`) | "Is this change ready to merge?" |
| 6 | Main agent | If the result is not ready: fix, then re-review and re-validate. Otherwise record the result. |

Around steps 1, 3, and 5, run `git status --porcelain` before and after the agent, and record
that nothing changed (SC-003).

**Expect**: the new test passes; the backend test count is the baseline plus the new test(s); the
frontend checks are "not run: no frontend change"; the verdict is `ready`.

## Final checks

```bash
# Diffs compare the working tree against main, so committed AND uncommitted changes are covered.
# New untracked files are listed by: git status --porcelain -- <paths>

# SC-001: no application source/spec/matrix change; only the demo test added
git diff --stat main -- backend/src/main frontend/src specs/001-clmp-mvp   # → empty
git status --porcelain -- backend/src/main frontend/src specs/001-clmp-mvp  # → empty
git diff --stat main -- backend/src/test; git status --porcelain -- backend/src/test   # → demo test only

# SC-001: final frontend validation
cd frontend && npm run typecheck && npm run build && cd ..

# SC-005: no credentials
git grep -nE '(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}' ; echo "exit=$? (1 = none)"

# SC-006: Spec Kit-managed files unchanged
git diff --stat main -- .claude/skills/speckit-* .specify   # → empty

# SC-007: 3 agents, 2 skills, 1 MCP server
ls .claude/agents/clmp-*.md | wc -l; ls -d .claude/skills/clmp-*/ | wc -l; grep -c '"type"' .mcp.json
```
