# Research: CLMP Agentic Engineering Layer

**Feature**: `002-agentic-engineering` | **Date**: 2026-10-03 | **Spec**: [spec.md](./spec.md)

Each item lists the decision, the reason, and the alternatives that were rejected. Sources are
cited where an item depends on external tool behavior. Items marked **Verify** are checked during
implementation by a quickstart step (see [quickstart.md](./quickstart.md)), because the published
docs were incomplete or the behavior depends on the installed Claude Code version (2.1.288 at the
time of writing).

Sources:

- Claude Code subagents: https://code.claude.com/docs/en/sub-agents.md
- Claude Code skills: https://code.claude.com/docs/en/skills.md
- Claude Code MCP: https://code.claude.com/docs/en/mcp.md
- Claude Code permissions and settings: https://code.claude.com/docs/en/permissions.md,
  https://code.claude.com/docs/en/settings.md
- Claude Code hooks: https://code.claude.com/docs/en/hooks.md
- GitHub MCP server: https://github.com/github/github-mcp-server (README,
  `docs/remote-server.md`, `docs/installation-guides/install-claude.md`)
- Fine-grained token permissions:
  https://docs.github.com/en/rest/authentication/permissions-required-for-fine-grained-personal-access-tokens

---

## R1. Where the agentic layer lives

**Decision**: Use Claude Code's native project-level locations, all committed:

| Piece | Location |
|-------|----------|
| Specialist agents | `.claude/agents/clmp-*.md` |
| CLMP skills | `.claude/skills/clmp-*/SKILL.md` (+ `references/`) |
| Bash guard used by agents | `.claude/hooks/clmp-bash-guard.mjs` |
| Shared permissions | `.claude/settings.json` |
| GitHub MCP server | `.mcp.json` (repository root) |
| Developer documentation | `docs/agentic-engineering.md` |
| Demonstration evidence | `specs/002-agentic-engineering/evidence/` |

**Rationale**: These are the locations Claude Code discovers automatically, so no wrapper or
registration step is needed. None of them is listed in either Spec Kit manifest
(`.specify/integrations/claude.manifest.json`, `speckit.manifest.json`), so a Spec Kit upgrade
does not overwrite them (FR-062). The `clmp-` prefix keeps CLMP items visibly separate from
`speckit-*` (FR-043).

**Alternatives considered**: User-level `~/.claude/` (not shared with the team, so rejected); a
Claude Code plugin (adds a packaging layer the project doesn't need yet, which conflicts with
constitution V).

## R2. Agent capability restriction (FR-003)

**Decision**:

| Agent | `tools` | Enforcement |
|-------|---------|-------------|
| `clmp-codebase-analyst` | `Read, Grep, Glob` | No write-capable or shell tool is granted at all. |
| `clmp-code-reviewer` | `Read, Grep, Glob, Bash`, plus read-only GitHub MCP tools (R7) | A `PreToolUse` hook on `Bash` in the agent frontmatter runs the guard in `reviewer` mode: read-only `git` commands only (R3). |
| `clmp-test-validator` | `Read, Grep, Glob, Bash` | The same guard in `validator` mode: build/test commands, read-only `git`, version checks (R3). |

No agent is given `Edit`, `Write`, `NotebookEdit`, or `Agent`. The `tools` field is an explicit
allowlist, so leaving those out removes them. Omitting `tools` would have inherited **all** tools,
including MCP tools.

**Rationale**: The spec says these agents "MUST NOT be able to" write, which is a capability
constraint, not just an instruction. The `tools` field accepts tool names only, not
`Bash(pattern)` rules, so the only way to limit what `Bash` can do inside a subagent is a hook.
Subagent frontmatter supports `hooks`, which scope the guard to that agent only, so the main
session is unaffected.

**Alternatives considered**:
- Instruction-only ("do not modify files"): rejected; not an enforcement mechanism.
- Removing `Bash` from the reviewer and having the main session pass it the diff: rejected,
  because the reviewer would then depend on what the implementing session chose to show it, which
  weakens independence (FR-020, FR-026).
- `permissionMode: plan`: rejected, because plan mode also blocks the validator's test runs, and
  its exact behavior inside subagents is version-dependent.
- A worktree (`isolation: worktree`) for the validator: considered for later. It isolates writes
  but doesn't prevent them, and it validates a copy of the repo rather than the change under test.

## R3. Bash guard design

**Decision**: One Node.js script, `.claude/hooks/clmp-bash-guard.mjs <mode>`. It reads the hook's
JSON input from stdin, takes `tool_input.command`, and exits with code `2` and a reason on stderr
to block (exit `0` allows). The rules (full contract in
[contracts/bash-guard.md](./contracts/bash-guard.md)):

1. It rejects shell control and redirection: `;`, `&&`, `||`, `|`, `` ` ``, `$(`, `>`, `<`,
   newlines, and `&` in the background position. The one exception is a trailing `2>&1`.
2. The first word (and subcommand) must be on the mode's allowlist. The `reviewer` mode allows
   `git diff|log|show|status|rev-parse|merge-base|branch --list|ls-files|blame` only. The
   `validator` mode adds `./backend/mvnw` / `backend/mvnw` with `-f backend/pom.xml`,
   `npm --prefix frontend run typecheck|build`, `npm --prefix frontend test -- --run`,
   `npm --prefix frontend ci` (only when `frontend/node_modules` is missing), `java -version`,
   `node -v`, and `npm -v`.
3. Anything else is blocked with a message telling the agent to report the need instead.

**Rationale**: Node is already a project prerequisite (README, Node 22), so the guard adds no
tool or dependency. `jq` isn't installed in the dev environment, and Python isn't a project
prerequisite. An allowlist is safer than a denylist. Banning chaining means every command can be
judged on its own. Using `-f`/`--prefix` instead of `cd … &&` keeps commands single.

**Alternatives considered**: A Bash script parsing JSON with `grep`/`sed` (fragile); `jq`
(missing); Python (not a declared prerequisite).

**Verify** (Q-V1): That a `PreToolUse` hook declared in subagent frontmatter fires only for that
subagent, and that exit code `2` blocks the call and returns the reason to the agent.

## R4. Model choice per agent

**Decision**: `clmp-codebase-analyst` → `sonnet`; `clmp-code-reviewer` → `opus`;
`clmp-test-validator` → `sonnet`.

**Rationale**: Review is the judgment-heavy role where missed defects cost the most, so it gets
the most capable model. Analysis is broad reading, and validation is mostly running and
classifying commands; a faster model fits both. The developer can override per call.

**Alternatives considered**: `inherit` for all (ties review quality to whatever the main session
uses, so rejected for the reviewer only).

## R5. Agent–skill relationship (FR-042, no drift)

**Decision**: Rules live **once**, in the skills. Agents preload them through the `skills`
frontmatter field:

| Agent | Preloaded skills |
|-------|-----------------|
| `clmp-codebase-analyst` | `clmp-change-workflow` |
| `clmp-code-reviewer` | `clmp-change-workflow`, `clmp-release-readiness` |
| `clmp-test-validator` | `clmp-release-readiness` |

Agent bodies contain only role-specific instructions (what to read, what to output, what not to
do). Project rules come from the preloaded skill text, which in turn links to the governing
documents rather than copying them: the constitution, CLAUDE.md, `specs/001-clmp-mvp/contracts/*`,
and `data-model.md`.

**Rationale**: One copy of each rule means the reviewer judges by exactly the rules the
implementer followed. Linking to the 001 contracts instead of paraphrasing them keeps the
authorization matrix as the only source of truth.

**Alternatives considered**: Duplicating checklists in each agent (drifts); a third "rules" skill
(an extra skill with no workflow of its own, which conflicts with FR-044).

## R6. Skill shape and invocation

**Decision**:

- `clmp-change-workflow`: model-invocable and user-invocable (`/clmp-change-workflow`). Its
  description triggers on "implement / change / fix / add … in CLMP".
- `clmp-release-readiness`: model-invocable and user-invocable. Its description triggers on
  "validate / ready to merge / release readiness / quality gates".
- Supporting files: `clmp-change-workflow/references/architecture-rules.md`;
  `clmp-release-readiness/references/check-selection.md` and
  `references/environment-blockers.md`.
- Neither skill sets `allowed-tools`. A skill must not pre-approve commands, so the normal
  permission prompts still apply to the main session.
- Neither skill uses `context: fork`. Delegation to the agents is an explicit step in the skill
  text, so the developer can see it happen.

**Rationale**: Keeping the skills plain and inspectable is enough. The independence requirement
is met by the agents, not by forking skills.

**Alternatives considered**: `disable-model-invocation: true` (the skill would then never trigger
automatically, defeating FR-040's purpose); separate backend and frontend skills (more skills
without a separate workflow).

## R7. GitHub MCP connection method (FR-050–055)

**Decision**: GitHub's **remote** MCP server over HTTP, configured in `.mcp.json`:

- `type: "http"`, `url: "https://api.githubcopilot.com/mcp/"`
- `headers.Authorization: "Bearer ${CLMP_GITHUB_PAT}"` (no default value)
- `headers.X-MCP-Tools`: an explicit allowlist of tools (see below)
- `headers.X-MCP-Toolsets: "context,repos,issues,pull_requests"`. This is a defensive cap. If
  the server combines it with `X-MCP-Tools` as a union rather than an intersection, the
  `settings.json` deny list (R8) still blocks dangerous tools.

Allowed tool set (a per-tool allowlist; exact list in
[contracts/github-mcp-access.md](./contracts/github-mcp-access.md)):

- Read: `get_me`, `list_pull_requests`, `search_pull_requests`, `pull_request_read`,
  `list_branches`, `list_commits`, `get_commit`, `list_issues`, `search_issues`, `issue_read`
- Write: `issue_write` (create issues) and `add_issue_comment` (top-level comment on an issue
  **or** pull request, which covers "comment on pull requests")

**Rationale**:
- No container runtime is needed (FR-054, constitution V).
- `${VAR}` expansion keeps the token out of git (FR-051), and an unset variable makes the server
  fail to start with a warning (FR-055).
- Project-scoped `.mcp.json` servers need explicit per-developer approval (FR-053).
- The `CLMP_GITHUB_PAT` name makes it obvious that this is a repo-scoped token, not a
  general-purpose `GITHUB_TOKEN`.
- `add_issue_comment` serves the PR-comment need without `pull_request_review_write`. That tool
  also exposes `delete` and `resolve_thread` methods, which can't be denied separately.

**Alternatives considered**:
- Local `github-mcp-server` binary over stdio: a documented fallback only. It needs a
  per-developer binary install, which adds setup.
- Docker image: excluded by the constitution.
- `gh` CLI through Bash: no tool-level access control, and the token is the user's full `gh`
  login.

**Known limitation (accepted)**: `issue_write` exposes an `update` method (it can edit or close
issues), and the server doesn't let a single method be denied. Mitigation: `issue_write` is an
`ask` rule in `settings.json`, so every call needs the developer's confirmation, and the token has
no other write scope.

**Verify** (Q-V2): That `X-MCP-Tools` limits the `tools/list` result to the allowlist; that the
startup failure with `CLMP_GITHUB_PAT` unset is clear; and the exact tool names on the live server
(tool names have changed across server versions).

## R8. Claude Code permission rules for GitHub tools

**Decision**: `.claude/settings.json`:

- `permissions.allow`: the read tools listed in R7, so read calls don't prompt.
- `permissions.ask`: `mcp__github__issue_write`, `mcp__github__add_issue_comment`, so every
  outward-facing write is confirmed.
- `permissions.deny`: `merge_pull_request`, `update_pull_request`, `update_pull_request_branch`,
  `create_pull_request`, `pull_request_review_write`, `push_files`, `create_or_update_file`,
  `delete_file`, `create_branch`, `create_repository`, `fork_repository`, `sub_issue_write`,
  `update_issue_comment`, `actions_run_trigger`, each as `mcp__github__<name>`.
- `enableAllProjectMcpServers` and `enabledMcpjsonServers` are **not** set, so each developer
  approves the server themselves (FR-053).

**Rationale**: This is defense in depth. The deny list still holds if the server-side allowlist
header is ignored or a future server version adds tools. Claude Code evaluates **deny first**:
deny rules take precedence over ask and allow. (One research summary claimed the opposite
precedence; it contradicts the permissions documentation and was disregarded. Q-V3 confirms the
real behavior.)

**Alternatives considered**: A blanket `mcp__github` allow (too broad); no settings file (relies
only on the server header).

**Verify** (Q-V3): That calling a denied tool, for example by asking the main session to merge a
PR, is refused by Claude Code before any network call.

## R9. Fine-grained token scope

**Decision**: Each developer creates a **fine-grained** personal access token limited to the one
repository `SudhaChandrikaY/consultant-lifecycle-management-platform`, with:

| Permission | Access | Needed for |
|------------|--------|------------|
| Metadata | Read | Always required |
| Contents | Read | Branches, commits, files |
| Pull requests | Read | PR list, diff, files, reviews |
| Issues | Read and write | Create issues; comment on issues and PRs |
| Commit statuses | Read | Combined status on PRs |
| Checks | Read (if offered) | Check runs on PRs |

No Contents write, no Pull requests write, no Actions, no Administration, no Secrets, and an
expiry of 90 days or less.

**Rationale**: Merging needs Contents write, which the token lacks, so the token itself can't
merge or push even if every other layer failed. Commenting on a PR uses the issues comment
endpoint, which accepts Issues write. Leaving out Pull requests write removes the ability to edit
or close PRs and to dismiss reviews.

**Verify** (Q-V4): That `add_issue_comment` on a PR succeeds with Issues write and Pull requests
read only. If GitHub rejects it, the documented fallback is Pull requests: Read and write, recorded
as a deviation.

## R10. Spec Kit coexistence and extension hooks (FR-060–062)

**Decision**: Do **not** create `.specify/extensions.yml` in this feature. The documentation maps
the CLMP agents and skills to Spec Kit steps instead (R13). The managed files are left
byte-identical and are checked by hashing them against both manifests (SC-006).

**Rationale**: The extension mechanism is supported, but its entries are normally produced by
`specify extension add` for packaged extensions. Hand-authoring entries for local skills is
undocumented in this Spec Kit version, and each speckit command would emit an extra prompt. FR-061
only constrains the connection *if* one is made. The value (reminding the developer to run
readiness after `/speckit-implement`) is delivered as well by CLAUDE.md guidance (FR-081).

**Alternatives considered**: An optional `after_implement` hook pointing to
`clmp-release-readiness`. Deferred and recorded as a future option in the docs.

## R11. Part 1 demonstration change

**Decision**: Add a backend integration test asserting that an **unlinked RECRUITER**
(`recruiter3`) gets an empty, but successful, Marketing, Submissions, and Placements list, while
the same lists are non-empty for ADMIN.

**Traceability**:
- `specs/001-clmp-mvp/spec.md`, edge case: "A RECRUITER user not linked to a recruiter profile
  sees an empty-state dashboard and lists…"
- `contracts/authorization-matrix.md`, ownership scopes: "Unlinked RECRUITER: the scope is empty.
  Lists come back empty…"
- FR-004 / FR-015

**Gap evidence** (verified 2026-10-03):
- The `recruiterId() == null → cb.disjunction()` branch exists in
  `marketing/service/MarketingSpecifications.java`, `submission/service/SubmissionSpecifications.java`,
  and `placement/service/PlacementSpecifications.java`.
- Only `/api/consultants` (`US2ConsultantProfileIT.unlinkedRecruiterGetsAnEmptyPage`) and
  `/api/dashboard` (`US7DashboardIT.unlinkedRecruiterGetsZerosAndMessage`) assert this for
  `recruiter3`.
- `AuthorizationMatrixTest` covers `recruiter3` only for single-record reads and actions (403),
  not list emptiness.

**Rationale**:
- Test-only, so it preserves behavior (FR-090).
- Small: one test class, about 40 lines.
- It touches role and ownership scope across three modules, which gives the analyst real impact to
  find and the reviewer real rules to check.
- It is backend-only, so the validator's check-selection rule must *justify skipping* frontend
  checks. That rule is exercised too.

**Alternatives considered**:
- Two extra rows in `AuthorizationMatrixTest` for marketing history: too small to exercise review.
- Placement visibility after consultant reassignment: valuable, but it depends on an unverified
  precondition (whether a PLACED consultant can be reassigned).

## R12. Part 2 seeded-defect exercise (SC-002)

**Decision**: On a local scratch branch `scratch/002-seeded-review`, created from the Part 1
result, plant exactly four defects in **one** commit with the neutral message
`chore: scratch changes`. Then invoke the reviewer with only "review this branch against
`002-agentic-engineering`". It is not told that defects were planted, how many, or what kind.

| # | Category (SC-002) | Planted defect |
|---|-------------------|----------------|
| D1 | Stored record in API contract | A new `GET /api/consultants/{id}/raw` endpoint in `consultant/web/ConsultantController.java` returning the `Consultant` entity |
| D2 | Missing server-side ownership check | Remove the `access.assertCanView(...)` call from `MarketingService.history` (`marketing/service/MarketingService.java`) |
| D3 | Disallowed lifecycle transition | Add `PLACED` to the manual transitions from `BENCH` in `lifecycle/ConsultantLifecycleService.java` (`MANUAL.put(BENCH, …)`) |
| D4 | Sensitive value logged | Log the submitted password at INFO on failed sign-in in `auth/web/AuthController.java` |

The planted diff is saved as `evidence/part2-seeded.patch` before the branch is deleted
(`git branch -D`). The branch is never pushed.

**Rationale**: Each defect maps to one SC-002 category and one constitution or contract rule
(IV, VII, FR-031, FR-103). The neutral prompt keeps the result honest. Keeping the patch makes the
exercise reproducible without keeping the defects on any branch.

Paths are relative to `backend/src/main/java/com/ensar/clmp/`. D3 is also expected to fail the
existing `ConsultantManualTransitionTest`. That is useful supporting evidence, but it doesn't
count toward SC-002, which measures the reviewer only.

**Alternatives considered**: Several commits with revealing messages (the commit messages would
leak the answers); pushing the branch for a PR-based review (it would put planted defects on the
remote).

## R13. Lifecycle mapping (documentation and CLAUDE.md)

**Decision**:

| SDD step | Spec Kit skill | CLMP agent or skill |
|----------|---------------|---------------------|
| Specify / Clarify | `speckit-specify`, `speckit-clarify` | `clmp-codebase-analyst` (optional, for existing-behavior questions) |
| Plan | `speckit-plan` | `clmp-codebase-analyst` (impact and affected files) |
| Task | `speckit-tasks`, `speckit-analyze`; `speckit-taskstoissues` (now possible through GitHub MCP) | — |
| Implement | `speckit-implement` | `clmp-change-workflow` (how each task is implemented) |
| Review | — (human) | `clmp-code-reviewer` → corrections → `clmp-test-validator` / `clmp-release-readiness` → human reviewer; optional PR comment through GitHub MCP |

**Rationale**: Spec Kit says *what* to build and in what order. The CLMP layer says *how* it is
built, judged, and validated in this codebase. Neither duplicates the other (FR-043).

## R14. Credential safety checks (SC-005)

**Decision**: Before committing, scan every added or changed file with
`git grep -nE '(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|Bearer [A-Za-z0-9_]{20,}'`
(staged and working tree). Also confirm that `.claude/settings.local.json` is ignored by adding it
to the root `.gitignore`, which makes the existing auto-exclude explicit for the team.

**Rationale**: It uses only the version-control tool already in use. The pattern covers the
GitHub token formats and any literal bearer token.

## R15. Environment fact: WSL `/mnt/c` and Vitest

**Decision**: The validator classifies a frontend test failure as **environment blocker** when
the output contains `Timeout waiting for worker to respond` (or the Vitest `startup` worker
timeout variant), **and** the repository path starts with `/mnt/`. The report then names the two
README workarounds: clone into the WSL file system, or run the tests with Windows-native Node.

**Rationale**: This matches the README's documented limitation. Requiring both conditions stops
the validator from hiding a real timeout on a normal file system. The current checkout is on
`/mnt/c`, so the demonstration exercises this path (SC-004).
