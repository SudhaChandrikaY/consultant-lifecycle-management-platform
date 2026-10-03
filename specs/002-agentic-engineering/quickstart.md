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

## Docs check (US7)

Using only `docs/agentic-engineering.md`, pick the right component for each situation. At least
4 of 5 should be correct.

1. Before planning a placement change, what is affected? → analyst
2. I'm implementing a task from `tasks.md`. → change workflow
3. Is my change right against the auth matrix? → reviewer
4. Can we merge? → readiness (→ validator)
5. Which PRs are open? → GitHub MCP

## Demonstration (US6): each component once

| Component | How to invoke | Example request |
|-----------|---------------|-----------------|
| `clmp-codebase-analyst` | "Use the clmp-codebase-analyst agent …" | Inspect the existing Consultants search and tell us which files and modules are involved. |
| `clmp-code-reviewer` | "Use the clmp-code-reviewer agent …" | Review commit `b46385e` (the name-search fix) and report any concerns. |
| `clmp-test-validator` | "Use the clmp-test-validator agent …" | Run one small targeted validation for that search fix (the typecheck and its two Vitest files only). |
| `clmp-change-workflow` | `/clmp-change-workflow …` | Explain how you would approach a small CLMP change. |
| `clmp-release-readiness` | `/clmp-release-readiness …` | List the checks you would choose for a small frontend-only change. |
| GitHub MCP | "Use the github tools …" (needs `CLMP_GITHUB_PAT`) | Read PR #1 and summarize what it did. |

Note the result of each in `demo.md`.

## Final checks

```bash
# Diffs compare the working tree against main, so committed AND uncommitted changes are covered.
# New untracked files are listed by: git status --porcelain -- <paths>

# SC-001: no application source, test, or 001 spec change
git diff --stat main -- backend frontend specs/001-clmp-mvp      # → empty
git status --porcelain -- backend frontend specs/001-clmp-mvp     # → empty

# SC-005: no credentials
git grep -nE '(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}' ; echo "exit=$? (1 = none)"

# SC-006: Spec Kit-managed files unchanged
git diff --stat main -- .claude/skills/speckit-* .specify   # → empty

# SC-007: 3 agents, 2 skills, 1 MCP server
ls .claude/agents/clmp-*.md | wc -l; ls -d .claude/skills/clmp-*/ | wc -l; grep -c '"type"' .mcp.json
```
