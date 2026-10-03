# Implementation Plan: CLMP Agentic Engineering Layer

**Branch**: `002-agentic-engineering` | **Date**: 2026-10-03 (simplified) | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-agentic-engineering/spec.md`

## Summary

This plan adds a small, project-owned agentic layer to CLMP, without changing CLMP behavior:

- **Three Claude Code subagents** in `.claude/agents/`:
  - `clmp-codebase-analyst` (read-only impact analysis);
  - `clmp-code-reviewer` (independent read-only review);
  - `clmp-test-validator` (independently runs the existing tests, typecheck, and build).

  Each has an explicit tool allowlist with no edit tools. Shell use is limited by instruction.
- **Two Agent Skills** in `.claude/skills/`:
  - `clmp-change-workflow`;
  - `clmp-release-readiness`.

  The agents preload them, so each rule exists in one place.
- **One read-only GitHub MCP integration** in `.mcp.json`: the hosted read-only endpoint, with a
  developer-supplied read-only token (`${CLMP_GITHUB_PAT}`).
- **One real demonstration**: the missing unlinked-RECRUITER list-scope test, taken through the
  full loop (analyze, implement, review, validate) and kept. The reports are recorded in `demo.md`.
- **Docs**: `docs/agentic-engineering.md`, with pointers in `CLAUDE.md` and `README.md`.

Research decisions are in [research.md](./research.md).

## Technical Context

**Language/Version**:
- Markdown with YAML frontmatter (agents, skills, docs)
- JSON (`.mcp.json`)
- Java 21 (the demonstration test only)

**Primary Dependencies**:
- Claude Code ≥ 2.1, which provides subagents, skills, and MCP (2.1.288 installed)
- The GitHub-hosted MCP server, an external service
- No new application dependencies

**Storage**: N/A

**Testing**:
- The scripted checks in [quickstart.md](./quickstart.md)
- Demonstration: JUnit 5 / MockMvc on the existing `IntegrationTestBase`
- Regression: the existing backend `verify` and the frontend typecheck/build

**Target Platform**: Developer workstations running Claude Code. The current one is WSL2 with the
repository on `/mnt/c`, where Vitest hits the known timeout.

**Project Type**: Developer tooling within the existing web-application repository

**Performance Goals**: N/A

**Constraints**:
- No CLMP behavior, permission, or data change
- No infrastructure
- No committed credentials
- Spec Kit files unchanged
- Exactly 3 agents, 2 skills, and 1 integration

**Scale/Scope**:
- 9 new files: 3 agents, 2 skills, `.mcp.json`, 1 doc, 1 test class, and `demo.md`
- 2 edited files: `CLAUDE.md` and `README.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| # | Principle | Compliance | Pre | Post |
|---|-----------|------------|-----|------|
| I | Spec-Driven Development | Approved spec. The demo test traces to 001 FR-004/FR-015, the edge case, and the matrix (R7). | ✅ | ✅ |
| II | Working Vertical Slices | No application layer is built ahead of need. The demo is test-only for an existing slice. | ✅ | ✅ |
| III | Technology Stack | Unchanged. | ✅ | ✅ |
| IV | Modular Monolith | No architecture change. The reviewer checks IV. | ✅ | ✅ |
| V | Scope Discipline | Simplified to the requested minimum. No new dependencies, hooks, Docker, or extra integrations. | ✅ | ✅ |
| VI | Buildable, Tested Quality | Existing builds and tests stay green. The validator puts VI into practice. | ✅ | ✅ |
| VII | Backend-Enforced Security | No application change. GitHub is read-only at both the server and the token level, and no token is committed. | ✅ | ✅ |
| VIII | Traceability & Human Review | `demo.md` keeps the agent reports verbatim, with the finding outcomes. The human reviewer stays accountable. | ✅ | ✅ |

**Result**: PASS. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/002-agentic-engineering/
├── spec.md, plan.md, research.md, data-model.md, quickstart.md
├── contracts/
│   ├── agent-contracts.md
│   ├── skill-contracts.md
│   └── github-mcp-access.md
├── checklists/requirements.md
├── demo.md          # Created during implementation (demonstration record)
└── tasks.md
```

### Source Code (repository root)

```text
.claude/
├── agents/
│   ├── clmp-codebase-analyst.md       # NEW
│   ├── clmp-code-reviewer.md          # NEW
│   └── clmp-test-validator.md         # NEW
└── skills/
    ├── clmp-change-workflow/SKILL.md  # NEW
    ├── clmp-release-readiness/SKILL.md# NEW
    └── speckit-*/                     # UNCHANGED (Spec Kit-managed)

.mcp.json                              # NEW (read-only GitHub; token via ${CLMP_GITHUB_PAT})
docs/agentic-engineering.md            # NEW
CLAUDE.md, README.md                   # EDIT (pointers)

backend/src/test/java/com/ensar/clmp/it/
└── UnlinkedRecruiterScopeIT.java      # NEW (demonstration; final name per impact report)
```

**Structure Decision**: Use Claude Code's native project locations, with a `clmp-` prefix to
separate the layer from the Spec Kit skills. No application source changes.

## Deviations & Assumptions

1. **Restrictions are enforced by tool allowlists plus instructions, not a sandbox.** The
   analyst can't write at all. The reviewer and validator have no edit tools, but they do have a
   shell, which is limited by instruction and checked with `git status` around each
   demonstration run. This trade-off was accepted in the 2026-10-03 simplification.
2. **The GitHub read-only endpoint path** follows GitHub's documentation. If it behaves
   differently, the documented `X-MCP-Readonly` header is used instead (research R5).
3. **Frontend tests are blocked** on this `/mnt/c` checkout by the known Vitest limitation. The
   frontend is unchanged, so the frontend typecheck and build are the frontend evidence.

## Complexity Tracking

No constitution violations, so nothing to justify.
