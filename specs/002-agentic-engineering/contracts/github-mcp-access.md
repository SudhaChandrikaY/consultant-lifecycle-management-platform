# Contract: GitHub MCP Integration Access

**Feature**: `002-agentic-engineering` · Sources: [research](../research.md) R7–R9, R14

## Purpose

Lets agents read this repository's pull requests, branches, commits, issues, and check results;
create issues (the existing `speckit-taskstoissues` step); and post a top-level comment on an
issue or pull request (e.g. review findings). Nothing else.

## Layers of control

| Layer | Mechanism | What it blocks |
|-------|-----------|----------------|
| 1. Token | Fine-grained PAT, one repository, permissions below | Any API call outside those permissions, at GitHub |
| 2. Server tool allowlist | `X-MCP-Tools` header in `.mcp.json` | Tools outside the allowlist never reach Claude |
| 3. Claude Code permissions | `deny` / `ask` / `allow` in `.claude/settings.json` | Dangerous tools refused locally; writes need confirmation each time |
| 4. Server approval | Project `.mcp.json` servers need per-developer approval | Silent enablement |
| 5. Agent tools | Only the reviewer gets GitHub tools, and only read tools | Agents writing to GitHub |

## `.mcp.json` (shape; no secret)

```json
{
  "mcpServers": {
    "github": {
      "type": "http",
      "url": "https://api.githubcopilot.com/mcp/",
      "headers": {
        "Authorization": "Bearer ${CLMP_GITHUB_PAT}",
        "X-MCP-Toolsets": "context,repos,issues,pull_requests",
        "X-MCP-Tools": "get_me,list_pull_requests,search_pull_requests,pull_request_read,list_branches,list_commits,get_commit,list_issues,search_issues,issue_read,issue_write,add_issue_comment"
      }
    }
  }
}
```

## Tool policy

| Tool | Toolset | Use | Claude Code rule |
|------|---------|-----|------------------|
| `get_me` | context | Confirm identity | allow |
| `list_pull_requests`, `search_pull_requests` | pull_requests | Find PRs | allow |
| `pull_request_read` | pull_requests | PR details, diff, files, reviews, comments, check runs, status | allow |
| `list_branches`, `list_commits`, `get_commit` | repos | Branch and commit context | allow |
| `list_issues`, `search_issues`, `issue_read` | issues | Read issues; `taskstoissues` dedupe | allow |
| `issue_write` | issues | Create issues (`taskstoissues`) | **ask** |
| `add_issue_comment` | issues | Comment on an issue or PR | **ask** |
| `merge_pull_request`, `update_pull_request`, `update_pull_request_branch`, `create_pull_request`, `pull_request_review_write`, `push_files`, `create_or_update_file`, `delete_file`, `create_branch`, `create_repository`, `fork_repository`, `sub_issue_write`, `update_issue_comment`, `actions_run_trigger` | various | — | **deny** |

The rule names are `mcp__github__<tool>`. Deny is evaluated before ask and allow.

## Token (`CLMP_GITHUB_PAT`)

- Type: fine-grained. Resource owner: the repository owner. Repository access: **only**
  `consultant-lifecycle-management-platform`. Expiry ≤ 90 days.
- Permissions: Metadata R · Contents R · Pull requests R · Issues R/W · Commit statuses R ·
  Checks R (if offered). Nothing else.
- Supplied through the developer's shell profile or OS environment (for example
  `export CLMP_GITHUB_PAT=…` in `~/.bashrc`, not in the repository), set before starting Claude
  Code.
- Never placed in `.mcp.json`, `.claude/settings*.json`, `.env*`, docs, or evidence.

## Failure behavior

| Condition | Expected |
|-----------|----------|
| `CLMP_GITHUB_PAT` unset | Claude Code warns that the variable is missing and the `github` server fails to connect. Other agents and skills are unaffected. |
| Token expired or invalid | Server calls return 401. The agent reports "GitHub unavailable" and continues locally (reviewer: `git diff`). |
| Missing permission | A 403 from GitHub on that call only. Reported, not retried. |
| A denied tool is requested | Claude Code refuses before any network call. |

## Deviation fallback

If Q-V4 shows that PR comments need Pull requests: write, the token gains that permission, and
the change is recorded as a deviation in the plan and docs. `merge_pull_request` and
`update_pull_request` stay denied in layer 3.
