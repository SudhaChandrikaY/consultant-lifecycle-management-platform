# Research: CLMP Agentic Engineering Layer

**Feature**: `002-agentic-engineering` | **Date**: 2026-10-03 (simplified) | **Spec**: [spec.md](./spec.md)

Each item gives the decision, the reason, and the alternatives that were rejected.

Sources:

- Claude Code docs:
  - subagents: https://code.claude.com/docs/en/sub-agents.md
  - skills: https://code.claude.com/docs/en/skills.md
  - MCP: https://code.claude.com/docs/en/mcp.md
- GitHub MCP server: https://github.com/github/github-mcp-server (README, `docs/remote-server.md`)
- Fine-grained token permissions:
  https://docs.github.com/en/rest/authentication/permissions-required-for-fine-grained-personal-access-tokens

---

## R1. Where the layer lives

**Decision**: Use Claude Code's native, auto-discovered project locations:

| Piece | Location |
|-------|----------|
| Agents | `.claude/agents/clmp-codebase-analyst.md`, `clmp-code-reviewer.md`, `clmp-test-validator.md` |
| Skills | `.claude/skills/clmp-change-workflow/SKILL.md`, `.claude/skills/clmp-release-readiness/SKILL.md` |
| GitHub MCP | `.mcp.json` (repository root) |
| Docs | `docs/agentic-engineering.md`, plus pointers in `CLAUDE.md` and `README.md` |
| Demonstration record | `specs/002-agentic-engineering/demo.md` |

**Rationale**:
- Claude Code discovers these locations automatically, so no setup is needed.
- None of them is in a Spec Kit manifest, so a Spec Kit upgrade leaves them alone (FR-061).
- The `clmp-` prefix separates them from `speckit-*`.

**Alternatives considered**:
- User-level `~/.claude/`: not shared with the team.
- A plugin: unnecessary packaging.
- Spec Kit extension hooks: out of scope; the docs and `CLAUDE.md` describe the lifecycle mapping
  instead.

## R2. Agent tools (FR-002)

**Decision**: Each agent's `tools` field is an explicit allowlist. Anything not listed is
unavailable.

| Agent | `tools` |
|-------|---------|
| `clmp-codebase-analyst` | `Read, Grep, Glob` |
| `clmp-code-reviewer` | `Read, Grep, Glob, Bash`, plus the GitHub MCP server's tools |
| `clmp-test-validator` | `Read, Grep, Glob, Bash` |

None of them gets `Edit`, `Write`, or `NotebookEdit`. The reviewer's instructions limit Bash to
read-only `git` commands. The validator's instructions limit Bash to the README's
build/test/typecheck commands. The main agent runs `git status` before and after each agent run,
in the demonstration.

**Rationale**:
- The analyst is read-only by construction.
- The reviewer needs `git diff`/`log`/`show` to see the change itself rather than relying on what
  the implementer shows it, which keeps it independent.
- The validator needs a shell to run the checks.
- Instruction-level limits, plus a `git status` check afterwards, match this feature's purpose: a
  working, understandable layer, not a sandbox.

**Alternatives considered**:
- A custom Bash-guard hook: removed in the simplification, because it is framework hardening
  rather than CLMP value.
- `permissionMode: plan`: it would also block the validator's test runs.

**Model**: agents omit `model`, so they inherit the session model. This keeps one less decision
to maintain.

## R3. Rules live in the skills (FR-042)

**Decision**: The CLMP rules are written once, in the two skills. The agents preload them through
the `skills` frontmatter field:

- the analyst preloads `clmp-change-workflow`;
- the reviewer preloads both skills;
- the validator preloads `clmp-release-readiness`.

The skills link to the governing documents (the constitution, `CLAUDE.md`,
`specs/001-clmp-mvp/contracts/*`, `data-model.md`) rather than copying them.

**Rationale**: The reviewer judges by exactly the rules the implementer followed, and the
authorization matrix stays the only source of truth.

**Alternatives considered**: Checklists repeated in each agent, which drift apart; a third
"rules" skill, which is an unnecessary extra skill.

## R4. Skill shape

**Decision**: Each skill is a single `SKILL.md` with frontmatter `name` and a `description`
starting "CLMP project rule (not a Spec Kit step): …", plus `metadata: { owner: clmp }`. They have
no `allowed-tools` and no supporting files, and both the model and the user can invoke them.

**Rationale**: Each skill is short enough to fit in one file, and invoking them should feel
familiar to anyone who knows `/speckit-*`.

## R5. GitHub MCP: read-only, simplest configuration (FR-050–052)

**Decision**: Use GitHub's hosted MCP server at its **read-only** endpoint:

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

**Rationale**:
- The read-only endpoint exposes only read tools (the server's documented `/readonly` mode), so
  write tools never reach Claude.
- The token is read-only as well, so GitHub refuses writes even if the server did not.
- `${CLMP_GITHUB_PAT}` is expanded from the developer's environment, so the file holds no secret.
- No container runtime is needed.
- Project `.mcp.json` servers ask each developer to approve them before first use.

**Alternatives considered**:
- Per-tool allowlists and deny rules in `.claude/settings.json`: unnecessary once both the
  endpoint and the token are read-only.
- A local binary, or Docker: extra setup, and Docker is excluded by the constitution.
- `gh` CLI: the user's full login scope.

**Check during implementation**: when `/mcp` lists the `github` tools, none of them writes. If
the read-only path behaves differently from the docs, use the documented
`X-MCP-Readonly: true` header instead, and note it in the docs.

## R6. Read-only token

**Decision**: A **fine-grained** personal access token, limited to this one repository, with
read access only: Metadata, Contents, Pull requests, and Issues, plus Commit statuses and Checks
if offered. Expiry ≤ 90 days. Each developer sets `CLMP_GITHUB_PAT` in their own shell profile or
OS environment, never in the repository.

**Rationale**: Without any write scope, the token cannot change anything on GitHub. It is the
simplest least-privilege control.

## R7. Demonstration change

**Decision**: Add a backend integration test asserting that `recruiter3` (a RECRUITER account not
linked to any recruiter profile) gets `200` with zero items from `GET /api/marketing-assignments`,
`GET /api/submissions`, and `GET /api/placements`, while ADMIN gets more than zero items from the
same lists.

**Traceability**:
- `specs/001-clmp-mvp/spec.md`, edge case: "A RECRUITER user not linked to a recruiter profile
  sees an empty-state dashboard and lists…"
- `contracts/authorization-matrix.md`, "Unlinked RECRUITER: the scope is empty. Lists come back
  empty…"
- FR-004, FR-015

**Gap (verified 2026-10-03)**:
- The `recruiterId() == null → cb.disjunction()` branch exists in three places:
  - `marketing/service/MarketingSpecifications.java`
  - `submission/service/SubmissionSpecifications.java`
  - `placement/service/PlacementSpecifications.java`
- Only `/api/consultants` (`US2ConsultantProfileIT.unlinkedRecruiterGetsAnEmptyPage`) and
  `/api/dashboard` (`US7DashboardIT`) assert this for `recruiter3`.

**Rationale**:
- Genuine value: it closes a real gap in authorization coverage.
- Test-only, so behavior is preserved.
- Small.
- It spans three modules and an ownership rule, so the analyst and the reviewer have real work to
  do.

## R8. Validator check selection (FR-030)

**Decision**: The README's commands, run from the repository root:

| Changed area | Checks |
|--------------|--------|
| `backend/**` | targeted `cd backend && ./mvnw test -Dtest=<Class>`; then `cd backend && ./mvnw verify` |
| Authorization/ownership or sensitive-data code (`*AccessPolicy*`, `*Specifications*`, `SecurityConfig`, `@PreAuthorize`, DTOs, logging) | additionally `-Dtest=AuthorizationMatrixTest,SensitiveFieldExposureTest` |
| `frontend/**` | `cd frontend && npm run typecheck`; `npm test -- --run`; `npm run build` |
| Docs/specs/`.claude/` only | no build checks ("not run: no code change"), unless full/all is requested |
| Explicitly asked for **full** / **all** validation | every backend and frontend check above, regardless of changed paths (perf still only if asked) |
| Performance | not run unless asked (`./mvnw verify -Dgroups=perf`) |

The validator re-runs a failed check once before reporting it. No timing is reported.

## R9. WSL `/mnt/c` and Vitest (FR-032)

**Decision**: If the frontend test output contains `Timeout waiting for worker to respond` and the
repository path starts with `/mnt/`, the validator classifies it as an **environment blocker**,
says that the frontend tests were not exercised, and names the README workarounds: clone into the
WSL file system, or use Windows-native Node.

## R10. Lifecycle mapping (docs and `CLAUDE.md`)

| SDD step | Spec Kit | CLMP layer |
|----------|----------|------------|
| Specify / Plan | `speckit-specify`, `speckit-clarify`, `speckit-plan` | `clmp-codebase-analyst` for impact |
| Task | `speckit-tasks`, `speckit-analyze` | — |
| Implement | `speckit-implement` | `clmp-change-workflow` |
| Review | — | `clmp-code-reviewer` → fixes → `clmp-release-readiness` (→ `clmp-test-validator`) → human |
| Any step | — | GitHub MCP for read-only repo/PR/issue context |

Spec Kit decides *what* to build and in what order. The CLMP layer decides *how* a change is
built, judged, and validated in this codebase.
