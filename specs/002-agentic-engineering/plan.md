# Implementation Plan: CLMP Agentic Engineering Layer

**Branch**: `002-agentic-engineering` | **Date**: 2026-10-03 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-agentic-engineering/spec.md`

## Summary

This plan adds a small, project-owned agentic engineering layer to the CLMP repository, without
changing any CLMP business behavior:

- **Three Claude Code subagents** in `.claude/agents/`:
  - `clmp-codebase-analyst`: read-only impact analysis.
  - `clmp-code-reviewer`: independent, read-only review.
  - `clmp-test-validator`: independent check selection and execution.

  Each has an explicit tool allowlist. The two agents that need a shell are confined by a
  per-agent `PreToolUse` guard script that allows only read-only `git` commands, or the project's
  own build and test commands.
- **Two CLMP Agent Skills** in `.claude/skills/`:
  - `clmp-change-workflow`: how a CLMP change is made.
  - `clmp-release-readiness`: how merge readiness is decided against the constitution's Quality
    Gates.

  The agents preload these skills, so the rules exist in one place.
- **One MCP integration**: GitHub's remote MCP server in `.mcp.json`. It authenticates with a
  developer-supplied fine-grained token (`${CLMP_GITHUB_PAT}`) and is limited by four layers: the
  token's permissions, a server-side tool allowlist, Claude Code `deny`/`ask` rules, and
  per-developer approval.
- **A two-part demonstration** recorded under `specs/002-agentic-engineering/evidence/`:
  - Part 1: a real, test-only change (unlinked-RECRUITER list scope) taken through analysis →
    implementation → independent review → correction → independent validation.
  - Part 2: a seeded-defect review on a discarded scratch branch.
- **Documentation**: `docs/agentic-engineering.md`, plus updates to `CLAUDE.md` and `README.md`.

Spec Kit-managed files are left unchanged. Research decisions are in [research.md](./research.md).

## Technical Context

**Language/Version**: Markdown with YAML frontmatter (agents, skills, docs); JSON (`.mcp.json`,
`.claude/settings.json`); JavaScript ES module on Node.js ≥ 20.19 (the guard script); Java 21
(the Part 1 test only).

**Primary Dependencies**:
- Claude Code ≥ 2.1, which provides the subagent, skill, hook, MCP, and permission mechanisms
  (2.1.288 installed)
- GitHub remote MCP server (`https://api.githubcopilot.com/mcp/`), an external service, not a
  project dependency
- No new application dependencies (backend `pom.xml` and frontend `package.json` are unchanged)

**Storage**: N/A. Evidence is stored as Markdown files under the feature's spec directory.

**Testing**:
- Guard script: the table-driven cases in [contracts/bash-guard.md](./contracts/bash-guard.md),
  piped through `node`
- Agent and skill behavior: the scripted scenarios in [quickstart.md](./quickstart.md)
- Part 1: JUnit 5 / Spring Boot Test / MockMvc integration test on the existing
  `IntegrationTestBase`
- Regression: the existing `./backend/mvnw -f backend/pom.xml verify` and frontend
  typecheck/test/build

**Target Platform**: Developer workstations running Claude Code. The current one is WSL2 Ubuntu
with the repository on `/mnt/c`, which has the known Vitest limitation (research R15).

**Project Type**: Developer tooling layered onto the existing web application repository
(`frontend/` + `backend/`)

**Performance Goals**: N/A for runtime. Validation should run the targeted backend tests before
the full verify, so that failures show up fast.

**Constraints**:
- No CLMP behavior, permission, or data change (FR-090)
- No Docker or other infrastructure (FR-091, constitution V)
- No credential in any committed file (FR-051)
- Spec Kit-managed files byte-identical (FR-060)
- At most 3 agents, 2 skills, 1 integration (FR-044, SC-010)

**Scale/Scope**:
- About 14 new files: 3 agents, 2 `SKILL.md` + 3 references, 1 guard, `.mcp.json`,
  `.claude/settings.json`, 1 doc, 1 test class, and evidence
- 3 edited files: `CLAUDE.md`, `README.md`, `.gitignore`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| # | Principle | How this plan complies | Pre | Post |
|---|-----------|------------------------|-----|------|
| I | Spec-Driven Development | The feature has an approved spec with clarifications. Part 1 traces to 001 FR-004/FR-015, the edge case, and the matrix ownership scope (R11). | ✅ | ✅ |
| II | Working Vertical Slices | No application layer is built ahead of need. Each user story is independently demonstrable (quickstart). The Part 1 change is test-only for an existing slice. | ✅ | ✅ |
| III | Technology Stack | The stack is unchanged. The guard uses Node, which is already a prerequisite. No database or stack change. | ✅ | ✅ |
| IV | Modular Monolith | No application architecture change. The layer *enforces* IV in review (DTO boundary, layering). | ✅ | ✅ |
| V | Scope Discipline | Only the requested layer. No Docker; the remote MCP server needs no runtime. No new app dependencies. Extension hooks deferred (R10). The guard script is the minimum mechanism for FR-003 (R2). | ✅ | ✅ |
| VI | Buildable, Tested Quality | The backend/frontend builds and tests stay green (SC-001). The guard has table-driven tests. The validator formalizes VI. | ✅ | ✅ |
| VII | Backend-Enforced Security | No application security change. The token is never committed (R14). GitHub access is least-privilege across four layers (R7–R9). The reviewer treats VII violations as blocking. | ✅ | ✅ |
| VIII | Traceability & Human Review | Evidence keeps agent reports verbatim, with a finding-resolution log (data-model §3). Agents produce evidence *for* the human reviewer and do not replace them. | ✅ | ✅ |

**Quality Gates**: Builds and tests pass (validated in Part 1 and final checks); the affected
workflow tests pass (Part 1); acceptance criteria are covered by the quickstart scenarios; no role
check is bypassed (the reviewer's rules); deviations are recorded below.

**Result**: PASS. No violations, so Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/002-agentic-engineering/
├── plan.md                 # This file
├── research.md             # Phase 0: R1–R15
├── data-model.md           # Phase 1: config, report, and evidence entities
├── quickstart.md           # Phase 1: validation guide (Q-V checks, story scenarios, final checks)
├── contracts/
│   ├── agent-contracts.md  # 3 agents: tools, skills, guard mode, inputs, outputs
│   ├── skill-contracts.md  # 2 skills: steps, check-selection rules, Spec Kit distinction
│   ├── github-mcp-access.md# MCP config shape, tool policy, token scope, failure behavior
│   └── bash-guard.md       # Guard interface, rules, test cases
├── checklists/requirements.md
├── evidence/               # Created during implementation (data-model §3)
└── tasks.md                # Phase 2 (/speckit-tasks — not created here)
```

### Source Code (repository root)

```text
.claude/
├── agents/
│   ├── clmp-codebase-analyst.md        # NEW
│   ├── clmp-code-reviewer.md           # NEW
│   └── clmp-test-validator.md          # NEW
├── skills/
│   ├── clmp-change-workflow/           # NEW
│   │   ├── SKILL.md
│   │   └── references/architecture-rules.md
│   ├── clmp-release-readiness/         # NEW
│   │   ├── SKILL.md
│   │   └── references/{check-selection.md, environment-blockers.md}
│   └── speckit-*/                      # UNCHANGED (Spec Kit-managed)
├── hooks/
│   └── clmp-bash-guard.mjs             # NEW
└── settings.json                       # NEW (shared permissions; no secrets)

.mcp.json                               # NEW (GitHub remote MCP; token via ${CLMP_GITHUB_PAT})
docs/agentic-engineering.md             # NEW (FR-080)
CLAUDE.md                               # EDIT: "Agentic workflow" section (FR-081)
README.md                               # EDIT: link + .claude/ description (FR-082)
.gitignore                              # EDIT: add .claude/settings.local.json (R14)

backend/src/test/java/com/ensar/clmp/it/
└── UnlinkedRecruiterScopeIT.java       # NEW (Part 1; final name set by the impact analysis)

.specify/                               # UNCHANGED except the git-ignored feature.json pointer
```

**Structure Decision**: Use Claude Code's native, auto-discovered project locations (R1), with a
`clmp-` prefix that keeps them separate from the Spec Kit-generated `speckit-*` skills. No
application source directory changes. The only code added is one backend integration test for
the Part 1 demonstration.

## Implementation Approach (for `/speckit-tasks`)

Ordered so that each story is demonstrable as soon as it is built:

1. **Foundation**: the guard script with its test table (Q-V1); `.gitignore` entry.
2. **US4 skills first**, because the agents preload them: `clmp-change-workflow`,
   `clmp-release-readiness` + references.
3. **US1 / US2 / US3 agents**: analyst, reviewer, validator. Then Q-V1b, and quickstart US1 and
   US3.
4. **US5 GitHub**: `.mcp.json`, `.claude/settings.json`. Then Q-V2–Q-V4 (developer-run, because
   they need the developer's token).
5. **US6 demonstration**: baseline → Part 1 loop → Part 2 seeded review → cleanup.
6. **US7 docs**: `docs/agentic-engineering.md`, `CLAUDE.md`, `README.md`. Then the SC-009 scenario
   check.
7. **Final checks**: SC-001, SC-003, SC-005, SC-006, SC-010 → `evidence/final-checks.md`.

## Deviations & Assumptions

These are surfaced for reviewer acknowledgement (constitution VIII):

1. **Spec Kit extension hook not created** (R10). FR-061 is satisfied vacuously. The lifecycle
   mapping is delivered through docs and `CLAUDE.md` instead. This is a future option, not a gap.
2. **`issue_write` includes an `update` method** that can't be denied separately (R7). Mitigation:
   an `ask` rule on every call, and a token with no other write scope.
3. **PR comments are top-level conversation comments** (`add_issue_comment`), not inline review
   comments. Inline review tools (`pull_request_review_write`) are denied, because they also
   expose delete and resolve methods.
4. **Assumptions to verify during implementation**:
   - Q-V1b: subagent-scoped hooks fire
   - Q-V2: the `X-MCP-Tools` allowlist is honored
   - Q-V3: deny precedence
   - Q-V4: PR comments work with Issues write only

   Each has a documented fallback. Any fallback used is recorded as a deviation in the evidence and
   the docs.
5. **GitHub write checks need the developer's token**. Q-V2–Q-V4 and US5 are run by the developer.
   The implementing agent prepares the configuration but never handles the token.
6. **`python3` is used in two final-check commands only** (SC-006 hashing, SC-010 count). These are
   verification conveniences, not part of the layer. Equivalent `sha256sum` commands are
   acceptable.
7. **Model choices** (R4) are defaults that the developer may override per call. Review quality is
   measured by SC-002, not assumed.

## Complexity Tracking

No constitution violations, so nothing to justify.
