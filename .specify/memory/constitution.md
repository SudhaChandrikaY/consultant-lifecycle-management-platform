# Consultant Lifecycle Management Platform (CLMP) Constitution

The Consultant Lifecycle Management Platform (CLMP) is an internal staffing and recruiting
application for managing recruiters, consultants, marketing assignments, client/vendor
submissions, placements, and operational reporting. The priority is a complete, working
business application first; engineering maturity is layered in only where it adds value.

## Core Principles

### I. Spec-Driven Development

- Every meaningful feature MUST start from an approved specification before implementation.
- Implementation MUST trace back to the spec's user stories and acceptance criteria.
- Trivial fixes (typos, small bug fixes, dependency bumps) MAY proceed without a spec.

Rationale: specs keep human intent and agent output aligned and make changes reviewable.

### II. Working Vertical Slices

- Features MUST be delivered as complete flows: UI → REST API → business logic → persistence.
- Infrastructure or framework layers MUST NOT be built ahead of a slice that needs them.
- Each slice SHOULD be independently demonstrable.

Rationale: an end-to-end working workflow proves business value earlier than isolated layers.

### III. Technology Stack

- Frontend: React + TypeScript, built with Vite.
- Backend: Java + Spring Boot.
- Persistence: H2 for the initial working prototype.
- PostgreSQL (or any other stack change) MUST be introduced through a separate specification.

Rationale: a fixed, mainstream stack keeps the codebase easy to follow and maintain.

### IV. Modular Monolith Architecture

- The backend MUST be a single deployable organized into business-domain modules.
- Business rules MUST live in service classes, not in controllers, repositories, or the UI.
- REST endpoints MUST use dedicated request/response DTOs; persistence entities MUST NOT be
  exposed directly in API contracts.
- The frontend MUST communicate with the backend only through REST APIs.

Rationale: clear module and layer boundaries keep the code testable and allow later extraction
without premature distribution.

### V. Scope Discipline

- Choose the simplest implementation that demonstrates the required business workflow.
- Docker, Kubernetes, Kafka, microservices, advanced observability, and similar infrastructure
  MUST NOT be introduced unless a specification explicitly requires it.
- New dependencies MUST serve a concrete need in the current spec.

Rationale: unrequested infrastructure adds cost and review burden without business value.

### VI. Buildable, Tested Quality

- Every change, including agent-generated changes, MUST build successfully (frontend and backend).
- Important business workflows MUST have automated tests covering their acceptance criteria.
- Testing effort SHOULD target acceptance criteria rather than elaborate test infrastructure.

Rationale: a working build and focused tests are the minimum bar for trust in the application.

### VII. Backend-Enforced Security

- Role-based access control MUST be enforced by the backend; frontend checks are UX only.
- Sensitive data (credentials, tokens, personal or financial details) MUST NOT appear in logs
  or API responses beyond what the authorized caller needs.

Rationale: the client is untrusted; the server is the only reliable enforcement point.

### VIII. Change Traceability and Human Review

- AI-assisted or automated changes MUST remain traceable to an approved specification and task
  scope.
- Material deviations, assumptions, and blockers MUST be surfaced explicitly; requirements MUST
  NOT be changed silently.
- Generated changes MUST remain reviewable and MUST be reviewed before they are accepted into the
  project.

Rationale: humans remain accountable for what ships; traceability makes that accountability
practical.

## Quality Gates

A change is ready to merge when:

- Frontend and backend builds pass.
- Automated tests for affected business workflows pass.
- The change satisfies the acceptance criteria of its spec (when one applies).
- No role-based access check is bypassed and no sensitive data is newly logged or exposed.
- Any deviation from the spec or this constitution is documented in the change description.

## Development Workflow

1. Specify: write or update the feature spec and resolve open questions.
2. Plan: produce the implementation plan and check it against this constitution.
3. Task: break the plan into vertical-slice tasks.
4. Implement: build and test slice by slice.
5. Review: a human reviews the change against the spec and the Quality Gates.

## Governance

- This constitution takes precedence over other project practices. Plans and reviews MUST check
  compliance; any justified exception MUST be recorded in the plan's complexity tracking.
- Amendments are made by updating this file through a reviewed change that states the reason
  and the version bump.
- Versioning follows semantic versioning: MAJOR for removing or redefining a principle, MINOR
  for adding a principle or materially expanding guidance, PATCH for clarifications.
- The constitution is intentionally lightweight and SHOULD be amended as the project matures.

**Version**: 2.0.1 | **Ratified**: 2026-10-02 | **Last Amended**: 2026-10-02
