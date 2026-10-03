# Agentic Engineering in CLMP

CLMP is built with Spec-Driven Development and an AI coding agent (Claude Code). This guide
describes the project's small **agentic engineering layer**:

- three specialist agents;
- two workflow skills;
- one read-only GitHub integration.

The layer makes the recurring parts of that work (impact analysis, review, and validation)
consistent, and separates them by role. It is developer tooling only; it does not change how
CLMP behaves.

- Specification: [`specs/002-agentic-engineering/spec.md`](../specs/002-agentic-engineering/spec.md)
- Each component used once: [`specs/002-agentic-engineering/demo.md`](../specs/002-agentic-engineering/demo.md)

## Who does what

| Responsibility | Who | May modify files? |
|----------------|-----|-------------------|
| Understand scope and impact before a change | `clmp-codebase-analyst` agent | No |
| Make the change | Main agent, following the `clmp-change-workflow` skill | Yes, within task scope |
| Judge the change | `clmp-code-reviewer` agent | No |
| Run the existing checks | `clmp-test-validator` agent, via the `clmp-release-readiness` skill | No application code or tests |
| Fix findings | Main agent | Yes, within task scope |
| Accept the change | Human reviewer | — |

## Subagents vs skills vs MCP

| | What it is | Runs where | In CLMP |
|---|---|---|---|
| **Subagent** | A separate worker with its own instructions, context, and tool list | Its own context window; returns a report | `clmp-codebase-analyst`, `clmp-code-reviewer`, `clmp-test-validator` |
| **Skill** | Reusable instructions (a `SKILL.md`) | Loaded into whichever conversation uses it, the main one or a subagent's | `clmp-change-workflow`, `clmp-release-readiness` |
| **MCP server** | A connection that gives Claude tools for an external system | Tools callable by the main agent or by a subagent that lists them | `github` (read-only) |

## Quick prompts

| To… | Say |
|-----|-----|
| See what a change touches | "Use the clmp-codebase-analyst agent to inspect the Consultants search and list the files and modules involved." |
| Get an independent review | "Use the clmp-code-reviewer agent to review commit b46385e and report any concerns." |
| Run a small targeted check | "Use the clmp-test-validator agent to run one small targeted validation for the name-search fix." |
| Follow the project's change process | `/clmp-change-workflow`, then describe the change |
| Decide which checks a change needs | `/clmp-release-readiness`, then describe the change |
| Read GitHub context | "Use the github tools to read PR #1 and summarize what it did." |

## The agents (`.claude/agents/`)

Each agent runs in its **own context**: it doesn't see your conversation, only its instructions,
`CLAUDE.md`, and the task you give it. That is what makes review and validation independent of
the implementer. Ask for one by name: "Use the clmp-code-reviewer agent to …".

### `clmp-codebase-analyst`: impact before a change

- **Use when** you are about to plan or implement a change and want to know what it touches.
- **Returns** an impact report with 9 sections:
  1. summary;
  2. scope check;
  3. affected modules, layers, and files;
  4. requirements;
  5. authorization and ownership rules;
  6. lifecycle rules;
  7. API contracts and data;
  8. existing tests and gaps;
  9. risks and open questions.
- **Flags** changes that are out of scope or need infrastructure, as needing a new spec.
- **Tools**: `Read`, `Grep`, and `Glob` only, so it cannot write anything.
- **Example**: "Use the clmp-codebase-analyst agent to inspect the Consultants search and list the
  files and modules involved."

### `clmp-code-reviewer`: independent review

- **Use when** a change is made and you want it judged before merge.
- **Targets**: a branch against its base (default `main`), a commit, a pull request, or the
  uncommitted working tree.
- **Checks** the change against:
  - the spec and task scope;
  - the constitution;
  - Controller → Service → Repository layering and the DTO boundary;
  - the authorization matrix (server-side role and ownership rules);
  - the lifecycle rules;
  - the sensitive-data rules.
- **Returns** findings as `id | severity | location | description | rule | suggestion`, plus
  scope deviations and a verdict (`approve` / `changes requested`).
  - The severities are blocking, major, minor, and note.
  - DTO, authorization, lifecycle, and sensitive-data violations are always **blocking**.
- **Tools**: `Read`, `Grep`, `Glob`, the read-only GitHub tools, and `Bash`. Its instructions
  limit `Bash` to read-only `git`. It has no edit tools.
- **Example**: "Use the clmp-code-reviewer agent to review commit b46385e and report any concerns."

### `clmp-test-validator`: independent validation

- **Use when** a change is made, or after review fixes, to run the right existing checks.
- **Chooses** checks by changed area, using the table in `clmp-release-readiness`:
  - backend → targeted tests, then `./mvnw verify`;
  - authorization or sensitive-data code → also the authorization and exposure tests;
  - frontend → typecheck, tests, build.

  Say **"full"** to run every check regardless of what changed.
- **Returns** `check | command | reason | outcome | classification | detail`, test counts, and a
  verdict (`ready` / `not ready` / `blocked by environment`).
- **Tools**: `Read`, `Grep`, `Glob`, `Bash`. Its instructions limit `Bash` to the README's test,
  typecheck, and build commands. It never edits code or tests, and never installs anything.
- **Example**: "Use the clmp-test-validator agent to run one small targeted validation for the
  name-search fix." (Say "full" only when you really want every suite; it takes about 15 minutes
  on a `/mnt/c` checkout.)

### How the limits are enforced

| Agent | Enforced by Claude Code | Enforced by instructions |
|-------|-------------------------|--------------------------|
| Analyst | No write or shell tools at all | — |
| Reviewer | No edit tools; only read-only GitHub tools | Bash is limited to read-only `git` |
| Validator | No edit tools | Bash is limited to the test, typecheck, and build commands |

This layer is deliberately not a hardened sandbox. Your normal Claude Code permission prompts
still apply to every shell command an agent runs. Check `git status` after an agent run if in
doubt. The human reviewer stays accountable
(constitution VIII).

## The skills (`.claude/skills/clmp-*`)

| Skill | Use when | What it does |
|-------|----------|--------------|
| `clmp-change-workflow` | Implementing, fixing, or modifying CLMP code (including during `/speckit-implement`) | 1. Confirm the spec/task scope. 2. Get the impact (analyst). 3. Follow the architecture rules (layering, DTOs, server-side authorization, lifecycle service, sensitive data). 4. Build a vertical slice. 5. Test the acceptance criteria. 6. Stay in scope. 7. Hand off to the reviewer and readiness. 8. Report deviations. |
| `clmp-release-readiness` | "Is this ready to merge?", or validate or test a change | Delegates to `clmp-test-validator`. It defines the check-selection table, the environment-blocker rules, and the failure classification, then maps the results to every constitution Quality Gate and gives a verdict. |

The agents **preload** these skills, so the reviewer judges by exactly the rules the implementer
followed.

## CLMP skills vs Spec Kit skills

| | `speckit-*` (10) | `clmp-*` (2) |
|---|---|---|
| Owner | Generated by Spec Kit 1.0.13, tracked in `.specify/integrations/claude.manifest.json` | The CLMP project, reviewed like code |
| Answers | *What* to build and in what order (spec → plan → tasks → implement) | *How* to build, judge, and validate a change in this codebase |
| Content | A generic process | CLMP architecture, roles, lifecycle, tests, and environment |
| Edit policy | **Never edit**; upgrade through `specify` | Edit through a reviewed change |

## Where each piece fits in the lifecycle

| Step | Spec Kit | CLMP layer |
|------|----------|------------|
| Specify / Clarify / Plan | `/speckit-specify`, `/speckit-clarify`, `/speckit-plan` | `clmp-codebase-analyst` for impact and affected files |
| Task | `/speckit-tasks`, `/speckit-analyze` | — |
| Implement | `/speckit-implement` | `clmp-change-workflow` for each task |
| Review | — | `clmp-code-reviewer` → fixes → `clmp-release-readiness` (→ `clmp-test-validator`) → human reviewer |
| Any step | — | GitHub MCP for read-only repository, PR, and issue context |

## GitHub integration (read-only)

`.mcp.json` connects Claude Code to GitHub's hosted MCP server at its **read-only** endpoint
(`https://api.githubcopilot.com/mcp/readonly`). Agents can read this repository's branches,
commits, pull requests (details, diff, files, reviews, checks), and issues. They can't create
issues, comment, merge, push, delete, or change settings.

There are two independent read-only layers:

1. the read-only server endpoint, which exposes no write tools;
2. a read-only token, so GitHub itself refuses any write.

### Setting up your token

1. On GitHub, create a **fine-grained personal access token** with these settings:
   - **Repository access**: only `consultant-lifecycle-management-platform`.
   - **Permissions**: all **read-only**: Metadata, Contents, Pull requests, Issues, plus Commit
     statuses and Checks if offered.
   - **Expiry**: 90 days or less.
2. Add it to your **own** environment, never to a repository file:

   ```bash
   export CLMP_GITHUB_PAT='<token>'    # e.g. in ~/.bashrc
   ```

3. Open a new shell, start `claude`, and approve the `github` server when asked (or use `/mcp`).

`.mcp.json` only contains the placeholder `${CLMP_GITHUB_PAT}`, which Claude Code expands from
your environment.

**Without a token**, the `github` server doesn't connect and its tools are unavailable. Everything
else, including the reviewer (which falls back to local `git`), keeps working.

## Secrets

- Never commit tokens, passwords, or keys, including in `.mcp.json`, docs, or specs.
- Personal Claude Code settings belong in `.claude/settings.local.json`, which stays on your
  machine. Shared settings and `.mcp.json` are committed and contain no secrets.

## WSL note

When the repository is on a Windows drive (`/mnt/c/...`) and Node runs in WSL, Vitest fails with
"Timeout waiting for worker to respond". The validator reports this as an **environment blocker**
(the frontend tests were not exercised) rather than a test failure. Workarounds: clone into the
WSL file system, or run the frontend tests with Windows-native Node.

## Known limitations

- The reviewer's and validator's shell use is limited by instruction, not by a sandbox.
- Claude Code loads custom agents when a session starts. After adding or editing an agent, start
  a new session.

- The GitHub integration is read-only by design. Write workflows such as
  `/speckit-taskstoissues` need a separate, approved change.

## Changing the layer

Treat agents, skills, and `.mcp.json` as code: change them through a reviewed change. The layer is
intentionally limited to 3 agents, 2 skills, and 1 integration; anything more needs a new
specification. Never edit the `speckit-*` skills or `.specify/` files by hand.
