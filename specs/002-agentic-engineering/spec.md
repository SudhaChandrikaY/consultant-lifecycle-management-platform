# Feature Specification: CLMP Agentic Engineering Layer

**Feature Branch**: `002-agentic-engineering`

**Created**: 2026-10-03

**Status**: Draft (simplified 2026-10-03)

**Input**: User description: "Create feature 002-agentic-engineering for CLMP. Add a project-specific agentic engineering layer to the existing Consultant Lifecycle Management Platform so recurring analysis, implementation review, testing, and repository workflows can be delegated consistently and independently. This feature must not change CLMP business behavior or application functionality. Requirements: (1) custom Claude Code subagents for codebase analysis, independent code review, and test validation; (2) reusable project-owned Agent Skills for the CLMP feature/change workflow and for validation/release readiness, clearly separated from the Spec Kit-generated speckit-* skills, which must not be modified; (3) one MCP integration with a concrete current need — preferably GitHub — that is repository-safe, commits no credentials, takes secrets from environment/user configuration, has least-privilege permissions, and is documented; (4) a demonstration of the layer on the real CLMP repository through a small, controlled engineering task or review exercise, with evidence that analysis is separated from independent review, that validation is independent, and that findings and corrections are traceable; (5) documentation of each subagent, when to use each skill, how custom skills differ from Spec Kit skills, what the MCP integration provides, how access and secrets are controlled, and how the pieces fit the Spec-Driven Development lifecycle. Constraints: no change to CLMP business requirements or role permissions; no deployment, PostgreSQL, Flyway, Docker, SSO, or other production infrastructure; no unnecessary agents or skills; preserve Spec Kit-managed files unless the supported extension mechanism requires otherwise; comply with the constitution and CLAUDE.md."

## Overview

CLMP was built through Spec-Driven Development, with an AI coding agent doing most of the
implementation. This feature adds a small, project-owned **agentic engineering layer** so that
the recurring parts of that work are done consistently, and by separate roles:

- **Three specialist agents**:
  - an analyst that finds the impact of a change before it is made;
  - a reviewer that judges a change independently of whoever made it;
  - a validator that independently runs the project's existing checks.
- **Two reusable workflow skills**: one for making a CLMP change the project's way, and one for
  deciding whether a change is ready to merge.
- **One read-only GitHub integration** that gives agents repository, branch, pull request, and
  issue context.

The layer is shown working on one small, real CLMP improvement, which is kept in the repository.

The goal is a small, understandable, working layer. Hardening the agent framework itself (custom
command guards, adversarial exercises, write-capable integrations) is out of scope.

The layer is developer tooling only. It changes **no** CLMP business behavior, role permission,
data, or user-facing functionality.

### Separation of Responsibilities

| Responsibility | Who does it | May modify files? |
|----------------|-------------|-------------------|
| Understand scope and impact before a change | Codebase analyst agent | No |
| Make the change | Main (implementing) agent, following the change workflow skill | Yes, within task scope |
| Judge the change | Code reviewer agent | No |
| Run the existing checks | Test validator agent, via the readiness skill | No application code or tests |
| Fix findings | Main agent | Yes, within task scope |
| Accept the change | Human reviewer | — |

## Clarifications

### Session 2026-10-03

- Q: Should the GitHub integration be read-only, or allow limited writes? → A (original): Read,
  plus create issues and comment on pull requests. **Superseded by the simplification below.**
- Q: What should the controlled demonstration exercise be? → A (original): A real
  behavior-preserving improvement plus a seeded-defect review. **Superseded by the simplification
  below.**

### Simplification 2026-10-03

- The feature was reduced to its purpose: interview learning, and making the agentic-engineering
  claims real and demonstrable.
- **GitHub integration**: read-only. No issue creation, PR comments, merge, push, delete,
  settings, or secrets operations. Use the simplest supported configuration.
- **Demonstration**: one real improvement only (the missing unlinked-recruiter list-scope test),
  taken through analysis → implementation → independent review → fixes → independent validation.
  No planted-defect exercise.
- **Removed**:
  - the custom command guard and complex hook logic;
  - OAuth fallback experiments;
  - exhaustive framework-testing evidence;
  - timing requirements.
- **How restrictions are enforced**: by each agent's tool allowlist plus its written
  instructions, with the main agent confirming afterwards that nothing changed (`git status`).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Analyze Impact Before Changing CLMP (Priority: P1)

Before a change, a developer (or the main agent) asks the codebase analyst what the change
touches. The analyst reads the relevant spec, constitution, and code and returns an impact report
covering:

- affected modules and layers;
- relevant requirements;
- the role and ownership rules and lifecycle rules involved;
- existing tests;
- risks.

It cannot modify files.

**Why this priority**: Correct scoping is the cheapest place to prevent architecture,
authorization, and scope mistakes.

**Independent Test**: Ask the analyst about a described change to a real CLMP area. Confirm that
the report covers the items above with file paths, and that `git status` is unchanged afterwards.

**Acceptance Scenarios**:

1. **Given** a described change, **When** the analyst runs, **Then** it reports the affected
   modules, layers, and files, the relevant requirements, the applicable role/ownership and
   lifecycle rules, existing tests, and risks.
2. **Given** a change that needs new infrastructure or is outside the active spec, **When** the
   analyst runs, **Then** it says that a separate approved specification is needed.
3. **Given** any analysis, **When** it finishes, **Then** no repository file has changed.

---

### User Story 2 - Review a Change Independently (Priority: P1)

After a change is made, the developer asks the code reviewer to review it. Working from the diff
and the project's governing documents (not the implementer's reasoning), the reviewer checks the
change against:

- the spec and task scope;
- the constitution;
- the architecture conventions (layering, the DTO boundary);
- the authorization matrix;
- the lifecycle rules;
- the sensitive-data rules.

It returns findings and does not edit anything.

**Why this priority**: Constitution VIII requires generated changes to be reviewed before they
are accepted. An independent reviewer is the core of the layer.

**Independent Test**: Ask the reviewer to review an existing commit. Confirm that it returns a
findings report in the agreed format (or explicitly "No findings") and that `git status` is
unchanged afterwards.

**Acceptance Scenarios**:

1. **Given** a change, **When** it is reviewed, **Then** each finding has a severity, a location,
   a description, the rule it relates to, and a suggested direction for correction.
2. **Given** a change that breaks the DTO boundary, a server-side role or ownership rule, a
   lifecycle rule, or a sensitive-data rule, **When** it is reviewed, **Then** that finding is
   marked blocking.
3. **Given** work outside the task scope, **When** it is reviewed, **Then** it is reported as a
   scope deviation.
4. **Given** a change with no defects, **When** it is reviewed, **Then** the report says
   "No findings" and lists what was checked.

---

### User Story 3 - Validate a Change Independently (Priority: P1)

After a change (or after review fixes), the developer asks the test validator to validate it.
The validator picks the appropriate existing checks for the changed areas and runs them:

- backend: targeted tests and the full verification;
- frontend: type check, tests, and the production build.

It then reports each result. It recognizes the known frontend test start-up timeout on WSL
`/mnt/c` checkouts as an environment problem, not a product failure. It does not modify code or
tests.

**Why this priority**: Constitution VI makes passing builds and tests the minimum bar. An
independent validator removes the risk that the implementer runs only convenient checks.

**Independent Test**: Ask the validator for a full validation. Confirm that it runs every
backend and frontend check, and reports each one's command, outcome, and classification
(including the WSL frontend-test behavior on a `/mnt/c` checkout). The demonstration (US6) shows
the changed-area selection.

**Acceptance Scenarios**:

1. **Given** a backend-only change, **When** validation runs, **Then** the targeted backend
   tests and the full backend verification run, and the report states why the frontend checks
   were skipped.
2. **Given** a frontend change, **When** validation runs, **Then** the frontend type check, tests,
   and build run.
3. **Given** the frontend tests fail with the known worker start-up timeout on a `/mnt/` WSL
   checkout, **When** the validator reports, **Then** it classifies this as an environment
   blocker, says the tests were not exercised, and names the documented workarounds.
4. **Given** a real test failure, **When** the validator reports, **Then** it names the failing
   test with a short excerpt and does not change code or tests to make it pass.

---

### User Story 4 - Make and Ship a CLMP Change the Project's Way (Priority: P2)

Two reusable skills capture how this project works:

- **Change workflow**: confirm the spec/task scope, identify the impact (optionally via the
  analyst), follow the layering and DTO boundaries, respect the lifecycle and authorization
  rules, build a vertical slice with tests, and avoid unrelated changes.
- **Release readiness**: have the validator run the right checks, include the
  authorization/security checks where relevant, and report against the constitution's Quality
  Gates.

They are clearly separate from the Spec Kit-generated `speckit-*` skills.

**Why this priority**: The skills make the project's conventions reusable, and they are what
keeps the three agents consistent with each other.

**Independent Test**: In a fresh session, a change request triggers the change workflow skill,
and "is this ready to merge?" triggers the readiness skill. The skill list shows `clmp-*` and
`speckit-*` separately.

**Acceptance Scenarios**:

1. **Given** a change request, **When** the main agent starts work, **Then** it uses the change
   workflow skill.
2. **Given** a readiness question, **When** the main agent answers, **Then** it uses the
   readiness skill, delegates execution to the validator, and reports against every Quality Gate.
3. **Given** the skill list, **When** a developer views it, **Then** the CLMP skills are
   distinguishable from the Spec Kit skills by name and description, and no Spec Kit file has
   changed.

---

### User Story 5 - Read GitHub Context Safely (Priority: P2)

A developer supplies their own read-only GitHub token from their own environment. Agents can then
read this repository's branches, commits, pull requests, and issues. The integration cannot
change anything on GitHub, and no credential is stored in the repository.

**Why this priority**: Pull request and issue context is useful for analysis and review, but the
other stories work without it.

**Independent Test**: With the token set, ask for this repository's open pull requests and
branches. The answer comes back, and no write tool (for example merge or create issue) is
available. The repository contains no credential.

**Acceptance Scenarios**:

1. **Given** the token is set, **When** an agent asks for pull requests, branches, or issues,
   **Then** they are returned.
2. **Given** the integration, **When** its available tools are listed, **Then** none can write to
   GitHub.
3. **Given** the repository, **When** it is searched for credentials, **Then** none are found.

---

### User Story 6 - Demonstrate the Layer on Real CLMP Work (Priority: P2)

The layer is used for one small, genuine CLMP improvement: adding the missing automated test that
an unlinked RECRUITER (one with no recruiter profile) gets empty Marketing, Submissions, and
Placements lists. The improvement goes through this loop:

1. The analyst identifies the impact.
2. The main agent implements the change.
3. The reviewer reviews it independently.
4. The validator validates it independently.
5. Any findings are fixed, then re-reviewed and revalidated.

The test is kept, and the reports are recorded together so that a human reviewer can follow
the loop.

**Why this priority**: Configuration alone doesn't prove that the layer works. This is the
feature's acceptance test, and it leaves real value behind.

**Independent Test**: Read the demonstration record. It contains the analyst's impact report, the
reviewer's report(s), the validator's report(s), and the outcome of every finding. The new test
passes in the repository.

**Acceptance Scenarios**:

1. **Given** the improvement, **When** it is made, **Then** the impact analysis, the review, and
   the validation are each produced by their own agent, and the record shows which agent produced
   which report.
2. **Given** review or validation findings, **When** they are addressed, **Then** each one is
   recorded as fixed, accepted, or rejected with a reason, and the change is re-reviewed and
   revalidated.
3. **Given** the finished demonstration, **When** the project checks run, **Then** they pass
   (apart from a documented environment blocker) and no CLMP behavior has changed.

---

### User Story 7 - Understand and Adopt the Layer (Priority: P3)

A developer new to the project can read one document to learn:

- what each agent does;
- when to use each skill;
- how the CLMP skills differ from the Spec Kit skills;
- what the GitHub integration provides and how its token is kept out of the repository;
- where each piece fits in the Specify → Plan → Task → Implement → Review lifecycle.

**Why this priority**: The layer only helps if people use it correctly. It depends on the other
stories.

**Independent Test**: Using only the document, choose the right agent or skill for a few
lifecycle situations.

**Acceptance Scenarios**:

1. **Given** the documentation, **When** a developer reads it, **Then** each agent, each skill,
   the GitHub integration, the token setup, and the lifecycle mapping are described in one place.

---

### Edge Cases

- **No active spec**: The analyst and the change workflow skill say so. For a trivial fix
  (constitution I) they proceed and note it; otherwise they recommend specifying first.
- **Nothing to review**: If there is no diff, branch, or commit to review, the reviewer says so
  rather than reviewing the whole repository.
- **Missing toolchain**: If Java 21 or Node is missing, the validator reports an environment
  blocker and installs nothing.
- **GitHub unavailable** (token unset, expired, or unreachable): The GitHub tools are unavailable
  or return errors. All agents and skills still work from the local repository.
- **Agent asked to edit**: The analyst and reviewer decline and hand the edit back to the main
  agent. The validator does not change code or tests to make checks pass.

## Requirements *(mandatory)*

### Functional Requirements

#### Agents

- **FR-001**: The repository MUST provide exactly three project-owned agents: a codebase
  analyst, a code reviewer, and a test validator.
- **FR-002**: Each agent MUST describe when to delegate to it, and MUST be granted only the tools
  its role needs:
  - The analyst MUST NOT have any file-writing or shell tool.
  - The reviewer and validator MUST NOT have file-editing tools.
  - The reviewer's shell use is limited by its instructions to read-only version-control
    commands.
  - The validator's shell use is limited by its instructions to the project's existing test,
    type-check, and build commands.
- **FR-003**: Each agent MUST ground its work in the constitution, the project agent guidance,
  and the active spec and plan, plus the authorization matrix, API contracts, and data model where
  relevant, and MUST cite repository paths for its claims.
- **FR-004**: Each agent MUST return a report in a fixed, documented format.

#### Analyst

- **FR-010**: The impact report MUST list:
  - the affected modules, layers, and files;
  - the relevant requirements;
  - the applicable role/ownership rules and lifecycle rules;
  - the affected API contracts or data;
  - the existing tests;
  - risks and open questions.
- **FR-011**: The analyst MUST flag work that is out of scope or needs new infrastructure as
  requiring a separate approved spec.

#### Reviewer

- **FR-020**: The reviewer MUST review a specified change (a diff, a branch against its base, a
  commit, or uncommitted working-tree changes) without relying on the implementer's explanation as
  evidence.
- **FR-021**: The reviewer MUST check the change against:
  - the spec and task scope;
  - the constitution;
  - the layering and DTO-boundary conventions;
  - the authorization matrix (server-side role and ownership rules);
  - the lifecycle rules;
  - the sensitive-data rules.
- **FR-022**: Each finding MUST have a severity (blocking, major, minor, or note), a location, a
  description, a rule reference, and a suggested correction. Violations of the DTO boundary,
  authorization, lifecycle, or sensitive-data rules MUST be blocking.
- **FR-023**: Scope deviations MUST be reported separately. A report with no findings MUST say so
  and list what was checked.

#### Validator

- **FR-030**: The validator MUST select checks by changed area:
  - backend → targeted tests plus the full backend verification;
  - frontend → type check, tests, and production build;
  - authorization or sensitive-data code → additionally the existing authorization and
    data-exposure tests.

  When explicitly asked for full or all validation, it MUST run every backend and frontend check
  regardless of changed paths (the performance test still only if asked). It MUST state why each
  check was run or skipped.
- **FR-031**: The validator MUST report each check's command, outcome, and classification
  (passed, product failure, test failure, environment blocker, not run), followed by a verdict
  (ready, not ready, or blocked by environment).
- **FR-032**: The validator MUST classify the known WSL `/mnt/` frontend test worker timeout as an
  environment blocker and name the documented workarounds.
- **FR-033**: The validator MUST NOT modify application code or tests, or install tools. It MUST
  NOT run the performance test unless asked.

#### Skills

- **FR-040**: The repository MUST provide a CLMP change workflow skill covering, in order:
  1. confirming the spec/task scope (or that it is a trivial fix);
  2. identifying the impact;
  3. following layering and DTO boundaries;
  4. respecting the lifecycle and server-side authorization rules;
  5. building a vertical slice with tests;
  6. avoiding unrelated changes;
  7. handing off to review and validation;
  8. reporting deviations.
- **FR-041**: The repository MUST provide a CLMP release-readiness skill that:
  - delegates the checks to the validator;
  - includes the authorization/security checks where relevant;
  - reports against every constitution Quality Gate.
- **FR-042**: The agents MUST use the same rules as the skills, so the two cannot drift apart.
- **FR-043**: The CLMP skills MUST be distinguishable from the Spec Kit skills by name and
  description, and MUST NOT duplicate the Spec Kit steps.

#### GitHub integration

- **FR-050**: The repository MUST provide one GitHub integration, committed and shared, using the
  simplest supported configuration.
- **FR-051**: The integration MUST be read-only: repository, branch, commit, pull request, and
  issue context only. It MUST NOT be able to:
  - create or edit issues;
  - comment;
  - merge, push, delete, change settings, or manage secrets.
- **FR-052**: No credential may be committed. The token MUST come from each developer's own
  environment, and it MUST be a read-only token scoped to this repository.

#### Spec Kit coexistence

- **FR-060**: This feature MUST NOT modify any Spec Kit-managed file (the `speckit-*` skills and
  the `.specify/` templates, scripts, and manifests), apart from the active-feature pointer that
  Spec Kit itself maintains.
- **FR-061**: CLMP agents, skills, and integration settings MUST live outside the Spec
  Kit-managed paths.

#### Demonstration

- **FR-070**: The layer MUST be demonstrated on the improvement described in User Story 6, and
  the resulting test MUST be kept.
- **FR-071**: One demonstration record MUST contain the impact report, the review report(s), the
  validation report(s), and the outcome of each finding, each labelled with the agent that
  produced it.

#### Documentation

- **FR-080**: One project document MUST describe:
  - each agent;
  - when to use each skill;
  - how the CLMP skills differ from the Spec Kit skills;
  - what the GitHub integration provides, and how its token is supplied and kept out of the
    repository;
  - the lifecycle mapping.
- **FR-081**: `CLAUDE.md` and the README MUST point to the agents, skills, and that document.

#### No business change

- **FR-090**: This feature MUST NOT change CLMP application source code, business requirements,
  role permissions, data, or seed content. The only exception is the new test added by the
  demonstration.
- **FR-091**: This feature MUST NOT add application dependencies or infrastructure (deployment,
  database, containers, SSO, and so on).

### Key Entities

- **Agent definition**: a project-owned agent with a name, a delegation description, allowed
  tools, preloaded skills, and instructions. There are three.
- **CLMP skill**: a project-owned workflow with a name, a "when to use" description, and ordered
  steps. There are two.
- **GitHub integration configuration**: the shared, read-only server definition. It references,
  but never contains, the developer's token.
- **Impact, review, and validation reports**: the fixed-format agent outputs.
- **Demonstration record**: the collected reports and finding outcomes for the User Story 6
  improvement.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After the feature:
  - all existing backend tests pass, and the only new test is the demonstration test;
  - the frontend type check and build pass;
  - no application source file, business spec, or authorization matrix has changed.
- **SC-002**: The demonstration record contains one impact report, at least one review report,
  and at least one validation report, each from its own agent, plus an outcome for 100% of the
  findings.
- **SC-003**: During the demonstration, the analyst and the reviewer change 0 repository files,
  and the validator changes 0 tracked files (`git status` before and after).
- **SC-004**: The demonstration's final validation verdict is "ready", or "blocked by
  environment" solely because of the documented WSL frontend-test limitation.
- **SC-005**: A credential scan of the repository finds 0 credentials.
- **SC-006**: 0 Spec Kit-managed files differ from `main`.
- **SC-007**: The layer contains exactly 3 agents, 2 skills, and 1 integration.
- **SC-008**: Using only the documentation, a reader picks the right agent or skill for at least
  4 of 5 lifecycle situations.

## Assumptions

- **Audience**: The users are CLMP developers and reviewers, so naming Claude Code agents, skills,
  and the GitHub integration describes the requested capability.
- **Tooling**: Claude Code's native project-level agents, skills, and MCP configuration. Exact file
  locations and formats are decided in the plan.
- **Enforcement level**: Tool allowlists are enforced by Claude Code. Limits on what a permitted
  shell may run are enforced by the agent's instructions and checked afterwards with `git status`.
  This is deliberately not a hardened sandbox. The human reviewer remains accountable
  (constitution VIII).
- **Independence**: Each agent runs in its own context with its own instructions, starting from
  the change and the governing documents.
- **GitHub**: The repository is on GitHub. Each developer can create a fine-grained, read-only
  token for it. No container runtime is needed.
- **Environment**: The current checkout is on WSL `/mnt/c`, where frontend tests hit the
  documented worker timeout. Backend tests, the frontend type check, and the build run normally.
- **Out of scope**:
  - custom command guards or hooks;
  - adversarial or planted-defect exercises;
  - GitHub writes;
  - CI pipelines;
  - Spec Kit extension hooks;
  - additional integrations;
  - any CLMP behavior change.
