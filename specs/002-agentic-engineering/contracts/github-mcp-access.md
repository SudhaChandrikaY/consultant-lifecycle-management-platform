# Contract: GitHub MCP Integration (Read-Only)

**Feature**: `002-agentic-engineering` · Sources: [research](../research.md) R5, R6

## Purpose

Gives agents read-only context about this repository: branches, commits, pull requests (details,
diff, files, reviews, checks), and issues. It cannot write anything to GitHub.

## `.mcp.json`

```json
{
  "mcpServers": {
    "github": {
      "type": "http",
      "url": "https://api.githubcopilot.com/mcp/readonly",
      "headers": { "Authorization": "Bearer ${CLMP_GITHUB_PAT}" }
    }
  }
}
```

The file contains no secret. Claude Code expands `${CLMP_GITHUB_PAT}` from the developer's
environment. If the read-only path does not behave as documented, keep the base URL
`https://api.githubcopilot.com/mcp/` and add the header `"X-MCP-Readonly": "true"`.

## Two read-only layers

| Layer | Control |
|-------|---------|
| Server | The read-only endpoint exposes only read tools |
| Token | A fine-grained token for this repository only, with read permissions only, so GitHub refuses any write |

In addition, Claude Code asks each developer to approve a project `.mcp.json` server before its
first use.

## Token (`CLMP_GITHUB_PAT`)

- Fine-grained; repository access: **only** `consultant-lifecycle-management-platform`; expiry
  ≤ 90 days.
- Permissions, all **read-only**: Metadata, Contents, Pull requests, Issues, plus Commit statuses
  and Checks if offered.
- Set it in your own shell profile or OS environment (for example `export CLMP_GITHUB_PAT=…` in
  `~/.bashrc`) before starting Claude Code. Never put it in any repository file.

## Without a token

If the token is unset or invalid, the `github` server doesn't connect, and its tools are
unavailable. All agents and skills keep working from the local repository.
