# Data Model: CLMP Agentic Engineering Layer

**Feature**: `002-agentic-engineering` | **Date**: 2026-10-03 (simplified)

This feature stores no application data. Its "entities" are configuration files and the reports
the agents produce. File-level details are in [contracts/](./contracts/).

## 1. Configuration

| Entity | File(s) | Key rules |
|--------|---------|-----------|
| Agent definition (×3) | `.claude/agents/clmp-*.md` | Frontmatter has `name` (matches the filename), `description` ("Use when …"), `tools` (an explicit allowlist with no `Edit`/`Write`/`NotebookEdit`), and `skills`. The body holds the role, inputs, prohibitions, and report format. |
| CLMP skill (×2) | `.claude/skills/clmp-*/SKILL.md` | Frontmatter has `name` (matches the directory), `description` starting "CLMP project rule (not a Spec Kit step): …", and `metadata.owner: clmp`. There is no `allowed-tools`. The body holds the ordered steps and links to the governing docs. |
| GitHub integration (×1) | `.mcp.json` | One server, `github`, using the read-only endpoint and an `Authorization` header that references `${CLMP_GITHUB_PAT}`. The file contains no secret. |

## 2. Reports

Every report starts with this header:

```text
Report: <Impact|Review|Validation> · Agent: <agent name> · Target: <change / branch vs base / commit>
```

### 2.1 Impact report (analyst)

1. Summary
2. Scope check: in scope, out of scope, or needs a new spec
3. Affected modules, layers, and files (with paths)
4. Relevant requirements (FR, story, and acceptance ids)
5. Authorization and ownership rules (quoted from the matrix)
6. Lifecycle rules involved
7. API contracts and data involved
8. Existing tests and gaps
9. Risks and open questions

### 2.2 Review report (reviewer)

1. **Checked**: which rule areas and files were reviewed.
2. **Findings**: one row each, `id | severity | location | description | rule | suggestion`.
   - The severities are `blocking`, `major`, `minor`, and `note`.
   - A violation of the DTO boundary, of authorization, of lifecycle rules, or of sensitive-data
     rules is always `blocking`.
3. **Scope deviations**.
4. **Verdict**: `approve` or `changes requested`. When there are no findings, the report says
   "No findings."

### 2.3 Validation report (validator)

1. **Changed areas**: the areas detected, with the paths as evidence.
2. **Checks**: one row each, `check | command | reason | outcome | classification | detail`.
   - `outcome`: `passed`, `failed`, `not run`, or `blocked`.
   - `classification`: `passed`, `product failure`, `test failure`, `environment blocker`, or
     `not run`.
3. **Test counts**, taken from the tool output.
4. **Verdict**: `ready`, `not ready`, or `blocked by environment`.

## 3. Demonstration record

There is one short file, `specs/002-agentic-engineering/demo.md`: a table with one row per
component, giving the component invoked, the example request, the result (summarized), and what it
demonstrated.
