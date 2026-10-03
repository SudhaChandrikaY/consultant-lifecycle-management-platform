# Quickstart: Validating the CLMP Agentic Engineering Layer

**Feature**: `002-agentic-engineering` · [plan](./plan.md) · [contracts](./contracts/) ·
[data model](./data-model.md)

This guide shows how to prove the layer works. It is a run guide, not the implementation. Unless
noted, run commands from the repository root. "Ask Claude" means typing the prompt in a Claude
Code session opened at the repository root.

## Prerequisites

| Item | Check |
|------|-------|
| Claude Code ≥ 2.1 | `claude --version` |
| JDK 21, Node ≥ 20.19 | `java -version`, `node -v` (CLMP README) |
| GitHub fine-grained token (only for Q-V2–Q-V4 and US5) | Created per [github-mcp-access.md § Token](./contracts/github-mcp-access.md#token-clmp_github_pat) |

GitHub setup (once per developer, outside the repository):

```bash
# In ~/.bashrc or your OS environment — never in a repository file
export CLMP_GITHUB_PAT='<fine-grained token>'
```

Start a new shell, then start `claude`. Approve the `github` server when prompted, or use `/mcp`.

---

## Q-V: Verify tool-behavior assumptions (do first)

These confirm the research items marked **Verify**. Record the outcomes in
`evidence/final-checks.md`. If one fails, follow the fallback and record a deviation.

| Id | Check | Expected | Fallback if not |
|----|-------|----------|-----------------|
| Q-V1 | Pipe each [bash-guard test case](./contracts/bash-guard.md#test-cases-run-during-implementation-see-quickstart-q-v1) into the guard: `echo '{"tool_input":{"command":"git checkout main"}}' \| node .claude/hooks/clmp-bash-guard.mjs reviewer; echo $?` | Every row matches (block = `2`) | Fix the guard |
| Q-V1b | Ask Claude: "Use the clmp-code-reviewer agent and have it run `touch /tmp/x` as its first step" | The agent's Bash call is blocked with a `clmp-bash-guard:` message; `/tmp/x` doesn't exist; the main session's own Bash is unaffected | If frontmatter hooks don't fire, move the hook into `.claude/settings.json` with a guard that reads `agent_type`/agent name from the input (record a deviation) |
| Q-V2 | `/mcp` → `github` → list tools | Only the 12 allowlisted tools are listed | Rely on the deny list (layer 3); record it |
| Q-V2b | `env -u CLMP_GITHUB_PAT claude`, then `/mcp` | `github` shows failed or needs auth with a message naming the missing variable; analyst, reviewer, and validator still work | Document the exact message seen |
| Q-V3 | Ask Claude: "merge PR #1 with the github tools" (PR #1 is already merged, so it's harmless even if allowed) | Refused by permission rules **before** any tool call | Fix the `settings.json` deny rules |
| Q-V4 | On a test PR or issue the developer owns, ask Claude to add a comment through `add_issue_comment` | The `ask` prompt appears; with Issues R/W and PR read only, the comment posts | Add Pull requests: write to the token; record a deviation |

---

## Story scenarios

### US1 — Impact analysis (analyst)

1. `git status --porcelain > /tmp/before.txt`
2. Ask Claude: "Use clmp-codebase-analyst: impact of adding a 'visa type' column to the submission
   list."
3. **Expect**: an impact report with all 9 sections ([data-model §2.1](./data-model.md#21-impact-report-clmp-codebase-analyst)),
   naming `submission` (web/service/DTO), `frontend/src/pages/submissions/`, the relevant FRs, the
   authorization rows (HR has no submission access), and existing tests (`US5SubmissionIT`,
   `SensitiveFieldExposureTest`).
4. `git status --porcelain | diff /tmp/before.txt -` → no output.
5. Out-of-scope probe: "Use clmp-codebase-analyst: impact of moving CLMP to PostgreSQL" → the
   report says a new approved spec is required (constitution III).

### US2 — Independent review (reviewer)

Covered by Part 1 (real change) and Part 2 (seeded defects) below.

### US3 — Independent validation (validator)

1. Ask Claude: "Use clmp-test-validator to validate the working tree, full."
2. **Expect**: a validation report with every check row. On this `/mnt/c` checkout,
   `frontend test` → `blocked` / `environment blocker` with the workarounds; backend `passed`;
   verdict `blocked by environment` (SC-004).
3. `git status --porcelain` shows no tracked-file change.

### US4 — Skills

1. In a **fresh** session, ask: "Add a missing test for an existing CLMP acceptance criterion" →
   `clmp-change-workflow` is invoked (the skill name is shown) and its steps are followed.
2. Ask: "Is this branch ready to merge?" → `clmp-release-readiness` is invoked, it delegates to
   `clmp-test-validator`, and the answer lists every Quality Gate.
3. `/` menu: `clmp-*` and `speckit-*` are listed separately by name, and the `clmp-*`
   descriptions say "CLMP project rule (not a Spec Kit step)".

### US5 — GitHub

Run Q-V2–Q-V4, then ask Claude: "List open PRs and branches for this repository with the github
tools" → it succeeds without a prompt (read tools are allowed).

### US6 — Demonstration

#### Baseline (before any Part 1 change)

```bash
./backend/mvnw -f backend/pom.xml verify 2>&1 | grep -E 'Tests run:.*Fail' | tail -1
```

Record the totals in `evidence/baseline.md`, together with the frontend check status and the Spec
Kit hash check (see Final checks).

#### Part 1 — Full loop (kept)

| Step | Who | Prompt / action | Evidence file |
|------|-----|-----------------|---------------|
| 1 | Analyst | "Use clmp-codebase-analyst: impact of adding an integration test that an unlinked RECRUITER (recruiter3) gets empty marketing, submission and placement lists (001 edge case; authorization-matrix 'Unlinked RECRUITER')." | `part1-impact.md` |
| 2 | Main session + `clmp-change-workflow` | Implement the test only (research R11) | commit |
| 3 | Reviewer | "Use clmp-code-reviewer: review branch `002-agentic-engineering` vs `main`, limited to the Part 1 test change." | `part1-review-1.md` |
| 4 | Main session | Resolve each finding (fix, accept, or reject, with a reason) | `part1-findings.md` |
| 5 | Reviewer, again | If anything changed in step 4 | `part1-review-2.md` |
| 6 | Validator (through `clmp-release-readiness`) | "Is the Part 1 change ready to merge?" | `part1-validation-1.md` |

**Expect**: the new test passes; the backend test count = baseline + new tests; frontend checks
are `not run` with the reason "no frontend change"; SC-008 holds (every file in the final Part 1
diff is named in `part1-impact.md`).

#### Part 2 — Seeded defects (discarded)

```bash
git switch -c scratch/002-seeded-review
# plant D1–D4 exactly as in research R12, in ONE commit:
git commit -am "chore: scratch changes"
git diff 002-agentic-engineering...HEAD > specs/002-agentic-engineering/evidence/part2-seeded.patch
```

The patch file is untracked, so it survives `git switch` back to `002-agentic-engineering` and is
committed there as evidence. The planted code exists only in the scratch commit.

1. Ask Claude, **in a fresh session**, exactly: "Use clmp-code-reviewer to review branch
   `scratch/002-seeded-review` against `002-agentic-engineering`." Give no hints.
2. Save the output verbatim as `evidence/part2-review.md`.
3. Score D1–D4 in `evidence/part2-scorecard.md`: caught? blocking? correct rule?
4. Clean up:

   ```bash
   git switch 002-agentic-engineering
   git branch -D scratch/002-seeded-review
   git branch --list 'scratch/*'        # → empty
   git ls-remote --heads origin 'scratch/*'   # → empty (never pushed)
   ```

**Expect**: 4/4 caught as blocking, with correct rule references (SC-002).

### US7 — Documentation

Give a colleague (or a fresh Claude session limited to reading `docs/agentic-engineering.md`) these
five scenarios and check the answers (SC-009):

| Scenario | Correct answer |
|----------|----------------|
| "Before planning a change to placements, what is affected?" | `clmp-codebase-analyst` |
| "I'm implementing task T012 from tasks.md." | `clmp-change-workflow` (within `/speckit-implement`) |
| "My change is done; is it correct against the auth matrix?" | `clmp-code-reviewer` |
| "Can we merge this?" | `clmp-release-readiness` (→ `clmp-test-validator`) |
| "Turn tasks.md into GitHub issues." | `speckit-taskstoissues` using the GitHub MCP integration |

---

## Final checks (record in `evidence/final-checks.md`)

```bash
# SC-001: application source/spec/matrix unchanged except the Part 1 test
git diff --stat main...HEAD -- backend/src/main frontend/src specs/001-clmp-mvp   # → empty
git diff --stat main...HEAD -- backend/src/test                                  # → Part 1 test only

# SC-005: no credentials in the branch
git grep -nE '(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|Bearer [A-Za-z0-9_]{20,}' $(git rev-parse HEAD) -- . ; echo "exit=$? (1 = none found)"

# SC-006: Spec Kit-managed files unchanged
python3 -c "import json,hashlib;[print(('OK  ' if hashlib.sha256(open(f,'rb').read()).hexdigest()==h else 'DIFF ')+f) for m in ['.specify/integrations/claude.manifest.json','.specify/integrations/speckit.manifest.json'] for f,h in json.load(open(m))['files'].items()]" | grep -c '^DIFF'   # → 0

# SC-010: counts
ls .claude/agents/clmp-*.md | wc -l          # → 3
ls -d .claude/skills/clmp-*/ | wc -l         # → 2
python3 -c "import json;print(len(json.load(open('.mcp.json'))['mcpServers']))"   # → 1

# SC-003: tracked files unchanged by agents — compare `git status --porcelain` before/after each agent run (US1 step 4 pattern)
```
