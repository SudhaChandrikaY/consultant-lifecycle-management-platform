# Feature Specification: CLMP Agentic Engineering Layer

**Feature Branch**: `002-agentic-engineering`

**Created**: 2026-10-03

**Status**: Draft

**Input**: User description: "Create feature 002-agentic-engineering for CLMP. Add a project-specific agentic engineering layer to the existing Consultant Lifecycle Management Platform so recurring analysis, implementation review, testing, and repository workflows can be delegated consistently and independently. This feature must not change CLMP business behavior or application functionality. Requirements: (1) custom Claude Code subagents for codebase analysis, independent code review, and test validation; (2) reusable project-owned Agent Skills for the CLMP feature/change workflow and for validation/release readiness, clearly separated from the Spec Kit-generated speckit-* skills, which must not be modified; (3) one MCP integration with a concrete current need — preferably GitHub — that is repository-safe, commits no credentials, takes secrets from environment/user configuration, has least-privilege permissions, and is documented; (4) a demonstration of the layer on the real CLMP repository through a small, controlled engineering task or review exercise, with evidence that analysis is separated from independent review, that validation is independent, and that findings and corrections are traceable; (5) documentation of each subagent, when to use each skill, how custom skills differ from Spec Kit skills, what the MCP integration provides, how access and secrets are controlled, and how the pieces fit the Spec-Driven Development lifecycle. Constraints: no change to CLMP business requirements or role permissions; no deployment, PostgreSQL, Flyway, Docker, SSO, or other production infrastructure; no unnecessary agents or skills; preserve Spec Kit-managed files unless the supported extension mechanism requires otherwise; comply with the constitution and CLAUDE.md."

## Overview

CLMP was built through Spec-Driven Development with an AI coding agent doing most of the
implementation work. Today, the same agent session that writes a change also inspects the
codebase, judges its own work, and decides which tests to run. The project's rules — the
constitution, the authorization matrix, the lifecycle transition rules, the DTO boundary, the
sensitive-data rules, and the known local-environment test limitation — live in documents that
each session must rediscover.

This feature adds a small, project-owned **agentic engineering layer** to the repository so that
the recurring parts of that work are done consistently and by separate, independent roles:

- **Three specialist agents** — one that analyzes impact before a change, one that reviews a
  change independently of whoever made it, and one that independently decides on and runs the
  right validation.
- **Two reusable workflow skills** — one for making a CLMP change correctly, and one for
  deciding whether a change is ready to merge.
- **One repository-hosting integration** (GitHub) that lets agents work with branches, pull
  requests, and issues under tightly limited, user-supplied access.

The layer is developer tooling only. It changes **no** CLMP business behavior, role permission,
data, or user-facing functionality.

### Actors

| Actor | Description |
|-------|-------------|
| **Developer** | A person working on CLMP who directs an AI coding agent and is accountable for what ships. |
| **Implementing agent** | The main AI coding session that makes a change on the developer's behalf. |
| **Specialist agent** | One of the three project-owned agents (analysis, review, validation) that the developer or the implementing agent delegates to. |
| **Human reviewer** | The person who approves a change before it merges (constitution VIII). |

### Separation of Responsibilities

| Responsibility | Who does it | May modify application code? |
|----------------|-------------|------------------------------|
| Understand scope and impact before a change | Codebase analysis agent | No |
| Make the change | Implementing agent (following the change workflow skill) | Yes, within task scope |
| Judge the change | Independent code review agent | No |
| Decide on and run validation | Test validation agent (following the readiness skill) | No |
| Correct findings | Implementing agent | Yes, within task scope |
| Accept the change | Human reviewer | — |

## Clarifications

### Session 2026-10-03

- Q: Should the GitHub integration be read-only, or allow limited writes? → A: Read pull
  requests, branches, issues, and check results, plus create issues and comment on pull requests.
  No merge, push, deletion, settings, or secrets actions.
- Q: What should the controlled demonstration exercise be? → A: Both (C) a small, real,
  behavior-preserving improvement carried through the full loop and kept, and (B) a seeded-defect
  review on a scratch branch that is then discarded.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Analyze Impact Before Changing CLMP (Priority: P1)

Before starting a change, a developer (or the implementing agent) asks the codebase analysis
agent what the change touches. The agent reads the relevant specification, constitution, and
code, and returns a concise impact report: which business modules and layers are affected, which
lifecycle rules and role permissions apply, which API contracts and data shapes are involved,
which existing tests cover the area, and what risks to watch. It does not change any files.

**Why this priority**: Correct scoping is the cheapest place to prevent architecture, authorization,
and scope mistakes. It also gives the reviewer and the validator a shared baseline.

**Independent Test**: Give the analysis agent a described change to a real CLMP area (for example,
"add a field to the submission list") and confirm that it returns an impact report covering the
items below, and that the working tree is unchanged afterwards.

**Acceptance Scenarios**:

1. **Given** a described change and an active specification, **When** the developer requests an
   impact analysis, **Then** the report names the affected business modules, the affected layers
   (UI, API, business logic, persistence), the relevant requirements and acceptance criteria, the
   applicable role permissions and lifecycle rules, the existing tests that cover the area, and
   any risks or open questions.
2. **Given** an analysis is requested, **When** the agent finishes, **Then** no file in the
   repository has been created, modified, or deleted.
3. **Given** a described change that falls outside the active specification's scope, **When** the
   analysis runs, **Then** the report states that the change is out of scope and cites the
   relevant scope boundary instead of planning it.
4. **Given** a described change that would need new infrastructure (for example a different
   database or containers), **When** the analysis runs, **Then** the report flags that a separate
   approved specification is required (constitution III and V).

---

### User Story 2 - Review a Change Independently (Priority: P1)

After a change is made, the developer asks the independent code review agent to review it. The
reviewer works from the change itself and the project's governing documents — not from the
implementing agent's reasoning — and checks the change against the approved specification and task
scope, the constitution, the authorization matrix, the layering and data-boundary conventions, the
lifecycle rules, and the sensitive-data rules. It returns a findings report and changes nothing.

**Why this priority**: Constitution VIII requires generated changes to be reviewed before they are
accepted. An independent reviewer that cannot edit code is the core of this feature's value.

**Independent Test**: Give the reviewer a change on a scratch branch that contains known,
deliberately planted defects of different kinds, and confirm that it reports each of them with a
location, a rule reference, and a severity, without modifying any file.

**Acceptance Scenarios**:

1. **Given** a change to review, **When** the reviewer finishes, **Then** it returns a report in
   which every finding has a severity, a location (file and line or area), a description of the
   defect or risk, the rule or requirement it violates, and a suggested direction for correction.
2. **Given** a change that exposes a persistence entity in an API contract, bypasses a
   server-side role or ownership check, performs a lifecycle transition the rules do not allow,
   or logs or returns a sensitive value, **When** it is reviewed, **Then** each such defect is
   reported as a blocking finding.
3. **Given** a change that includes work outside its task or specification scope, **When** it is
   reviewed, **Then** the reviewer reports it as a scope deviation, even if the extra work is
   correct.
4. **Given** a change with no defects, **When** it is reviewed, **Then** the report says so
   explicitly and lists what was checked, rather than inventing findings.
5. **Given** any review, **When** the reviewer finishes, **Then** no file in the repository has
   been created, modified, or deleted by the reviewer.

---

### User Story 3 - Validate a Change Independently (Priority: P1)

After a change is made (or after review findings are corrected), the developer asks the test
validation agent to validate it. The agent determines which checks the change actually needs —
targeted backend tests, the full backend verification, frontend type checking, frontend tests,
and the production build — runs them, and produces a concise validation report. It distinguishes
failures caused by the product or its tests from failures caused by the local environment, such
as the known frontend test start-up timeout when the repository lives on the Windows drive under
WSL.

**Why this priority**: Constitution VI makes a passing build and passing tests the minimum bar
for any change. An independent validator removes the risk that the implementing agent runs only
the checks that suit it.

**Independent Test**: Ask the validator to validate a small change in this repository and confirm
that it selects checks that match the changed areas, runs them, and reports each check's outcome
and classification, including correctly classifying the known WSL frontend test limitation as an
environment blocker when it occurs.

**Acceptance Scenarios**:

1. **Given** a backend-only change, **When** validation runs, **Then** the agent runs the
   targeted backend tests for the affected area and the full backend verification, and states
   why frontend checks were or were not run.
2. **Given** a change that touches the frontend, **When** validation runs, **Then** the agent
   runs frontend type checking, the frontend tests, and the production build.
3. **Given** a change that touches role permissions, ownership, or sensitive data, **When**
   validation runs, **Then** the agent also runs the existing authorization and data-exposure
   tests.
4. **Given** the frontend test run fails with the known worker start-up timeout on a repository
   located on the Windows drive under WSL, **When** the agent reports, **Then** it classifies the
   result as an environment blocker (not a product or test failure), states that the frontend
   tests were not actually exercised, and names the documented workarounds.
5. **Given** a genuine test failure, **When** the agent reports, **Then** it names the failing
   test, the related user story or acceptance criterion where one applies, and a short excerpt of
   the failure — and does not modify the code or the tests to make them pass.
6. **Given** any validation run, **When** it finishes, **Then** the report lists every check
   with its command, outcome (passed, failed, not run, blocked), classification, and duration.

---

### User Story 4 - Make a CLMP Change the Project's Way (Priority: P2)

When an implementing agent makes a change, it follows a reusable CLMP change workflow skill that
codifies how this project expects changes to be made: confirm the change is within an approved
specification and task, identify the impacted modules, keep business rules in the business-logic
layer, keep data shapes at the API boundary separate from stored records, respect lifecycle and
authorization rules on the server, build a complete vertical slice, and avoid unrelated changes.
A second reusable skill, the validation and release-readiness skill, codifies how to decide
whether a change is ready to merge against the constitution's Quality Gates.

**Why this priority**: Skills make the project's conventions reusable by any session and by the
specialist agents. They are what keeps the three agents consistent with each other.

**Independent Test**: Start a fresh agent session, give it a small change request, and confirm
that the change workflow skill is used and that the agent's plan follows each step in the skill;
separately, ask whether a change is ready to merge and confirm that the readiness skill is used
and that its checklist maps to every Quality Gate.

**Acceptance Scenarios**:

1. **Given** a change request in a fresh session, **When** the agent starts work, **Then** it
   uses the CLMP change workflow skill and follows its steps in order.
2. **Given** a question about whether a change is ready to merge, **When** the agent answers,
   **Then** it uses the validation and release-readiness skill and reports against every Quality
   Gate in the constitution.
3. **Given** the list of available skills, **When** a developer looks at it, **Then** the CLMP
   skills are visibly distinct from the Spec Kit-generated skills by name and description.
4. **Given** this feature is complete, **When** the Spec Kit-managed files are compared with their
   recorded integrity values, **Then** none of them has changed.

---

### User Story 5 - Work With GitHub Under Limited Access (Priority: P2)

A developer configures the GitHub integration once, with their own access token supplied from
their own environment. Agents can then read pull requests, branches, and issues for the CLMP
repository and perform only the write actions the agreed workflow needs. No credential is ever
stored in the repository.

**Why this priority**: The project is hosted on GitHub and already uses branches and pull
requests; the existing task-to-issue Spec Kit step already expects a GitHub integration that is
not configured. It is useful but not required for the other stories.

**Independent Test**: Clone the repository fresh, confirm that it contains no credentials, set
the documented environment variable with a limited token, and confirm that an agent can list this
repository's open pull requests; then remove the variable and confirm that the integration fails
with a clear message rather than falling back to any stored credential.

**Acceptance Scenarios**:

1. **Given** a fresh clone, **When** the repository is searched for credentials, **Then** none are
   found in any committed file related to this feature.
2. **Given** a developer has supplied a token through the documented environment or user
   configuration, **When** an agent requests this repository's pull requests, branches, or issues,
   **Then** the request succeeds.
3. **Given** no token is supplied, **When** the integration is used, **Then** it fails with a clear
   message that names the missing setting, and no other agent capability is affected.
4. **Given** the integration is configured, **When** an agent attempts an action outside the
   permitted set, **Then** the action is not available or is refused.
5. **Given** the integration is used for the first time in a session, **When** it starts, **Then**
   the developer is asked to approve it rather than it being enabled silently.

**Permitted action set**: read pull requests, branches, issues, and check results for the CLMP
repository; create issues (for the existing Spec Kit task-to-issue step); and post comments on
pull requests (for review findings). No other write action is permitted — in particular no merge,
push, branch or repository deletion, settings change, or secrets management.

---

### User Story 6 - Prove the Layer Works on Real CLMP Work (Priority: P2)

The feature is demonstrated on the real CLMP repository with a small, controlled exercise that
runs the full agentic loop: analysis, implementation, independent review, correction where
needed, and independent validation. The evidence is recorded so that a human reviewer can
see that each role was performed separately and that every finding and correction can be traced.

**The demonstration has two parts**:

- **Part 1 — Full loop on real work (kept)**: a small, real, behavior-preserving improvement to
  CLMP (for example, a missing automated test for an existing acceptance criterion) is carried
  through analysis, implementation, independent review, correction, and independent validation,
  and is kept in the repository.
- **Part 2 — Seeded-defect review (discarded)**: on a scratch branch, a change containing
  deliberately planted defects (see SC-002) is given to the review agent, which must catch them;
  the scratch branch is then discarded, and only the evidence is kept.

**Why this priority**: Configuration files alone do not prove that the layer works. The
demonstration is the acceptance test for the whole feature.

**Independent Test**: Read the recorded demonstration evidence and confirm that it contains a
separate analysis report, a separate review report, a separate validation report, and a trace
from each finding to its resolution.

**Acceptance Scenarios**:

1. **Given** the full-loop exercise (Part 1), **When** it is run, **Then** the analysis, review,
   and validation are each performed by their own specialist agent, and the evidence shows which
   agent produced which report.
2. **Given** the review produced findings, **When** they are corrected, **Then** each correction
   is recorded against its finding, and the change is reviewed and validated again.
3. **Given** the demonstration is complete, **When** the full CLMP verification is run, **Then**
   all existing backend and frontend checks still pass (or any environment blocker is reported as
   such), and no CLMP business behavior has changed.
4. **Given** the seeded-defect exercise (Part 2), **When** the review agent reviews the scratch
   change, **Then** it reports every planted defect as a blocking finding, and after the exercise
   the scratch branch no longer exists and no planted defect is present on any kept branch.

---

### User Story 7 - Understand and Adopt the Layer (Priority: P3)

A developer who is new to the project reads the project documentation and understands what each
specialist agent does, when to use each skill, how the CLMP skills differ from the Spec Kit
skills, what the GitHub integration provides and what access it has, how secrets are controlled,
and where each piece fits in the Specify → Plan → Task → Implement → Review lifecycle.

**Why this priority**: The layer only creates value if future developers use it correctly. It
depends on the other stories existing first.

**Independent Test**: Give a developer who has not seen the layer a set of short scenarios and
the documentation, and confirm that they choose the right agent or skill for each.

**Acceptance Scenarios**:

1. **Given** the project documentation, **When** a new developer reads it, **Then** they can find,
   in one place, a description of each agent, each skill, the GitHub integration, the secrets
   setup, and the lifecycle mapping.
2. **Given** the lifecycle mapping, **When** a developer is at a given lifecycle step, **Then**
   the documentation tells them which agent or skill applies at that step, if any.

---

### Edge Cases

- **No active specification**: The analysis agent and the change workflow skill report that no
  approved specification applies. For a trivial fix (typo, small bug fix, dependency bump —
  constitution I), they proceed and say so; for anything larger, they stop and recommend
  specifying first.
- **Reviewing a change the reviewer cannot see**: If there is no diff, branch, or pull request to
  review, the reviewer says so and does not review the whole repository by default.
- **Very large change**: The reviewer states which parts it reviewed in depth and which it only
  skimmed, rather than implying complete coverage.
- **Reviewer disagrees with the specification**: The reviewer reports the concern as a
  specification question for the human reviewer; it does not treat the spec as wrong or suggest
  silently changing requirements (constitution VIII).
- **Required tool missing**: If the Java or Node toolchain is missing or the wrong version, the
  validator reports an environment blocker naming the missing tool and does not attempt to
  install anything.
- **Long-running check**: The validator does not run the performance smoke test unless the change
  affects performance-sensitive areas or the developer asks; when skipped, the report says so.
- **Flaky or intermittent failure**: The validator may re-run a failing check once; if results
  differ between runs, it reports the check as unstable, with both outcomes.
- **Build output and caches**: Running checks produces build output and caches that are already
  ignored by version control; these do not count as the validator modifying the repository.
- **GitHub unreachable or token expired**: The integration fails with a clear message; review and
  validation continue to work without it, using local version control.
- **Specialist agent asked to edit code**: The analysis and review agents refuse and hand the edit
  back to the implementing agent; the validator refuses to change code or tests to make checks
  pass.
- **Spec Kit upgrade**: Upgrading Spec Kit must not overwrite or remove the CLMP agents, skills, or
  integration settings.

## Requirements *(mandatory)*

### Functional Requirements

#### Specialist agents (general)

- **FR-001**: The repository MUST provide exactly three project-owned specialist agents: codebase
  analysis, independent code review, and test validation.
- **FR-002**: Each specialist agent MUST have a description that states when to delegate to it,
  so that both developers and the implementing agent can choose it correctly.
- **FR-003**: Each specialist agent MUST be granted only the capabilities its role needs. The
  analysis and review agents MUST NOT be able to create, modify, or delete repository files. The
  validation agent MAY run build and test commands but MUST NOT edit source code, tests,
  specifications, or configuration.
- **FR-004**: Each specialist agent MUST ground its work in the project's governing documents —
  the constitution, the project agent guidance, the active specification and plan, and, where
  relevant, the authorization matrix, API contracts, and data model — rather than in assumptions.
- **FR-005**: Each specialist agent MUST return a structured report with a fixed set of sections
  (defined per agent below), so that reports are comparable across runs.

#### Codebase analysis agent

- **FR-010**: The analysis agent MUST produce an impact report that lists: the affected business
  modules; the affected layers (UI, API, business logic, persistence); the relevant requirements,
  user stories, and acceptance criteria; the applicable role permissions and ownership scopes;
  the applicable lifecycle states and transitions; the affected API contracts and data shapes;
  the existing tests covering the area; and risks and open questions.
- **FR-011**: The analysis agent MUST flag work that is outside the active specification's scope,
  or that would require new infrastructure or a stack change, as needing a separate approved
  specification.
- **FR-012**: The analysis agent MUST reference concrete repository locations (files and, where
  helpful, symbols) for every affected area it names.

#### Independent code review agent

- **FR-020**: The review agent MUST review a specified change (a diff, a branch compared with its
  base, or a pull request) and MUST NOT rely on the implementing agent's own explanation of the
  change as evidence of correctness.
- **FR-021**: The review agent MUST check the change against: the approved specification and task
  scope; the constitution's principles and Quality Gates; the authorization matrix (role gates and
  ownership scopes, enforced on the server); the layering convention (business rules only in the
  business-logic layer); the API data-boundary convention (no stored records exposed in API
  contracts); the lifecycle transition rules, including automatic transitions and their history
  records; and the sensitive-data rules for logs and API responses.
- **FR-022**: Every review finding MUST include a severity (blocking, major, minor, or note), a
  location, a description, the rule or requirement reference, and a suggested correction direction.
- **FR-023**: The review agent MUST classify work outside the task or specification scope as a
  scope deviation, separately from defects.
- **FR-024**: The review agent MUST state what it checked and, when there are no findings, say so
  explicitly.
- **FR-025**: The review agent MUST NOT modify any repository file; corrections are the
  implementing agent's responsibility.
- **FR-026**: The review agent MUST run in a context that is separate from the implementing
  agent's working context, so that its judgment is independent.

#### Test validation agent

- **FR-030**: The validation agent MUST select the checks that a change needs based on the areas it
  touches, using this rule set: backend changes → targeted backend tests for the affected area and
  the full backend verification; frontend changes → frontend type checking, frontend tests, and the
  production build; changes to role permissions, ownership, or sensitive data → additionally the
  existing authorization and data-exposure tests; changes to both → all of the above. It MUST state
  the reason for each check it runs or skips.
- **FR-031**: The validation agent MUST support running targeted backend tests by test name or
  area, as well as the full backend verification.
- **FR-032**: The validation agent MUST classify each check outcome as one of: passed; product
  failure; test failure; environment blocker; not run (with reason).
- **FR-033**: The validation agent MUST recognize the known frontend test worker start-up timeout
  that occurs when the repository is on the Windows drive under WSL, classify it as an environment
  blocker, state that the frontend tests were not exercised, and name the documented workarounds.
- **FR-034**: The validation agent MUST NOT modify source code or tests, and MUST NOT install or
  upgrade tools.
- **FR-035**: The validation report MUST list every check with its command, outcome,
  classification, and duration, followed by a one-line overall verdict (ready, not ready, or
  blocked by environment).
- **FR-036**: The validation agent MUST NOT run the performance smoke test by default; it MUST run
  it only when the change affects performance-sensitive list or report behavior or when asked.

#### Reusable CLMP skills

- **FR-040**: The repository MUST provide a CLMP change workflow skill that codifies, in order:
  confirming the change is within an approved specification and task (or is a trivial fix);
  identifying impacted modules (optionally by delegating to the analysis agent); following the
  layering and API data-boundary conventions; respecting lifecycle and authorization rules on the
  server; implementing a complete vertical slice; writing or updating tests for affected acceptance
  criteria; avoiding unrelated changes; and reporting deviations, assumptions, and blockers.
- **FR-041**: The repository MUST provide a CLMP validation and release-readiness skill that
  codifies: choosing targeted tests; authorization and sensitive-data checks where relevant;
  frontend validation; build checks; the environment-blocker rule; and reporting results against
  every Quality Gate in the constitution.
- **FR-042**: The CLMP skills MUST be usable both directly by a developer or the implementing agent
  and by the specialist agents, and the specialist agents MUST apply the same rules as the skills
  so that they do not drift apart.
- **FR-043**: The CLMP skills MUST be clearly distinguishable from the Spec Kit-generated skills by
  name and description, and MUST NOT duplicate the Spec Kit workflow steps (specify, clarify,
  plan, tasks, analyze, implement).
- **FR-044**: The total number of new agents and skills MUST be no more than three agents and two
  skills unless a later specification approves more.

#### GitHub integration

- **FR-050**: The repository MUST provide exactly one external tool integration, for GitHub,
  configured in a form that is committed to the repository and shared by all developers.
- **FR-051**: The committed configuration MUST NOT contain any credential. The credential MUST be
  supplied from each developer's own environment or user-level configuration, and the
  documentation MUST name the exact setting.
- **FR-052**: The integration MUST be limited to the CLMP repository's needs and to the permitted
  action set defined in User Story 5 (read pull requests, branches, issues, and check results;
  create issues; comment on pull requests). Actions to merge, push, delete branches or repositories,
  change repository settings, or manage secrets MUST NOT be available.
- **FR-053**: The integration MUST require each developer's approval before it is first used, and
  MUST NOT be enabled silently.
- **FR-054**: The integration MUST NOT require a container runtime or any other new local
  infrastructure.
- **FR-055**: When the credential is missing, invalid, or lacks a permission, the integration MUST
  fail with a clear message, and the specialist agents and skills MUST continue to work without it.

#### Spec Kit coexistence

- **FR-060**: This feature MUST NOT modify any Spec Kit-managed file (the generated speckit skills,
  Spec Kit templates, scripts, and integration manifests), except the active-feature pointer that
  the Spec Kit workflow itself maintains.
- **FR-061**: If any CLMP step is connected to the Spec Kit workflow, it MUST be connected only
  through Spec Kit's supported extension mechanism, and any such connection MUST be optional
  (it must not block Spec Kit commands).
- **FR-062**: CLMP agents, skills, and integration settings MUST be stored where a Spec Kit
  upgrade does not overwrite or remove them.

#### Demonstration and evidence

- **FR-070**: The feature MUST be demonstrated on the real CLMP repository through the controlled
  exercise defined in User Story 6: Part 1 (full loop on a kept, behavior-preserving
  improvement) and Part 2 (seeded-defect review on a discarded scratch branch).
- **FR-071**: The demonstration evidence MUST include the analysis report, the review report(s),
  the validation report(s), a record of each finding and its resolution (fixed, accepted with
  reason, or rejected with reason), and which agent produced each report.
- **FR-072**: The demonstration evidence MUST be stored with this feature's specification so that
  it is reviewable alongside the change.
- **FR-073**: The demonstration MUST NOT change CLMP business requirements, role permissions, or
  user-visible behavior; after it, all existing automated checks MUST pass (or any environment
  blocker must be reported as such).

#### Documentation

- **FR-080**: The project documentation MUST describe: what each specialist agent does, when to
  use it, and what it may and may not do; when to use each CLMP skill; how the CLMP skills differ
  from the Spec Kit skills; what the GitHub integration provides and what access it has; how the
  credential is supplied and kept out of the repository; and how each piece maps to the
  Specify → Plan → Task → Implement → Review lifecycle.
- **FR-081**: The project's agent guidance file MUST point agents to the specialist agents and
  CLMP skills at the right lifecycle steps, without contradicting the constitution.
- **FR-082**: The README MUST link to the agentic layer documentation and MUST update its
  description of the agent-configuration folder to cover the new contents.

#### No business change

- **FR-090**: This feature MUST NOT change CLMP application source code, business requirements,
  role permissions, data, or seeded demo content, except where the demonstration exercise
  explicitly and traceably adds a behavior-preserving change approved under User Story 6.
- **FR-091**: This feature MUST NOT introduce deployment, a production database, schema migration
  tooling, containers, single sign-on, or other production infrastructure, and MUST NOT add
  application dependencies.

### Key Entities

- **Specialist agent definition**: A project-owned agent with a name, a delegation description,
  a granted capability set, and its working instructions. Three exist: analysis, review,
  validation.
- **CLMP skill**: A project-owned, reusable workflow with a name, a description of when it applies,
  ordered steps, and any reference material it needs. Two exist: change workflow, release
  readiness.
- **GitHub integration configuration**: The shared, committed definition of the GitHub connection,
  which references — but never contains — a developer-supplied credential, and which defines the
  permitted action set.
- **Impact report**: Output of the analysis agent: affected modules, layers, requirements,
  permissions, lifecycle rules, contracts, tests, risks, and file references.
- **Review report**: Output of the review agent: scope checked, findings (severity, location,
  description, rule reference, suggested correction), scope deviations, and verdict.
- **Validation report**: Output of the validation agent: each check (command, reason, outcome,
  classification, duration) and an overall verdict.
- **Finding resolution record**: Links a review finding to its outcome (fixed with change
  reference, accepted with reason, or rejected with reason) and to the re-review and
  re-validation that confirmed it.
- **Demonstration evidence**: The collected reports and resolution records for the controlled
  exercise, stored with this feature's specification.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After this feature merges, 100% of the pre-existing CLMP automated backend and
  frontend tests still pass (environment blockers excepted and reported); the only change in test
  count is the tests added by the Part 1 demonstration change; and no application source file,
  business specification, or authorization matrix has changed outside that change.
- **SC-002**: In a seeded-defect check, the review agent reports 100% of at least four planted
  defects — one each of: a stored record exposed in an API contract, a missing server-side role or
  ownership check, a disallowed lifecycle transition, and a sensitive value written to a log or
  response — each as a blocking finding with a correct rule reference.
- **SC-003**: Across all demonstration runs, the analysis and review agents modify 0 repository
  files, and the validation agent modifies 0 tracked files.
- **SC-004**: When the frontend tests are run on a repository located on the Windows drive under
  WSL and hit the known start-up timeout, the validation agent classifies 100% of those
  occurrences as environment blockers rather than test failures.
- **SC-005**: A credential scan of all files added or changed by this feature finds 0 credentials.
- **SC-006**: 100% of Spec Kit-managed files match their recorded integrity values after the
  feature is complete.
- **SC-007**: The demonstration evidence traces 100% of review findings to a recorded resolution.
- **SC-008**: For the demonstration change, the analysis agent's impact report names every file
  that the final Part 1 change actually modified.
- **SC-009**: Given five short "which tool do I use?" scenarios that span the lifecycle steps, a
  developer new to the layer selects the correct agent or skill for at least four of five using
  only the documentation, within 15 minutes.
- **SC-010**: The layer adds no more than three agents, two skills, and one external integration.

## Assumptions

- **Audience**: The users of this feature are developers and reviewers of CLMP, not CLMP business
  users. "Stakeholder-facing" language in this specification therefore refers to engineering
  stakeholders, and naming agents, skills, and the GitHub integration describes the capability
  requested rather than an implementation choice.
- **Tooling**: The layer targets Claude Code, the agent tool already used to build CLMP, and its
  native project-level agent, skill, and integration mechanisms. File locations and formats are
  decided in the plan.
- **Independence**: "Independent" review and validation mean that they run in their own agent
  context with their own instructions and restricted capabilities, starting from the change and the
  governing documents. They are not a replacement for the human reviewer required by constitution
  VIII; they produce evidence for that reviewer.
- **GitHub hosting**: The repository's remote is on GitHub, and each developer can create their own
  fine-grained access token scoped to this repository.
- **No container runtime**: Because the constitution excludes Docker without an approving spec, the
  GitHub integration will use a connection method that needs no container runtime (for example,
  GitHub's hosted integration endpoint or a standalone tool the developer already has).
- **Environment**: The known WSL limitation applies to frontend tests only; backend verification
  runs normally on the Windows drive under WSL. The documented workarounds are cloning the
  repository into the WSL file system or running the frontend tests with Windows-native Node.
- **Feature scope pointer**: Updating the Spec Kit active-feature pointer to this feature is part
  of the normal Spec Kit workflow and does not count as modifying a managed file.
- **Existing tests are the baseline**: Validation uses the existing CLMP test suites; this feature
  does not add new test infrastructure. The Part 1 demonstration change may add tests to the
  existing suites, but adds no new test tooling or dependencies.
- **Constitution**: No constitution amendment is needed. The feature implements principle VIII
  (traceability and review) and VI (buildable, tested quality) more rigorously without changing
  them.
- **Out of scope**: Continuous-integration pipelines, automated pull request bots, scheduled
  agents, additional integrations (issue trackers other than GitHub, chat, documentation tools),
  and any change to the CLMP application's behavior.
