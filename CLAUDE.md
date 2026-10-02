# Consultant Lifecycle Management Platform (CLMP)

Internal staffing and recruiting application for managing recruiters, consultants, marketing
assignments, client/vendor submissions, placements, and operational reporting.

The governing source of truth is `.specify/memory/constitution.md`. If this file and the
constitution disagree, the constitution wins.

## Repository Structure

- `frontend/` — React + TypeScript + Vite
- `backend/` — Java + Spring Boot
- `specs/` — Spec Kit feature specifications and planning artifacts
- `.specify/` — Spec Kit project infrastructure
- `.claude/` — Claude Code skills, agents, and agent configuration

## Architecture

- Modular monolith; the frontend talks to the backend only through REST APIs.
- Backend layering: Controller → Service → Repository. Business rules live in services.
- API boundaries use request/response DTOs. Never return JPA entities from REST controllers.
- H2 is the initial persistence layer.
- Infrastructure changes (database, containers, messaging, etc.) require a later approved spec.

## Frontend Conventions

- Functional React components in TypeScript.
- Reuse shared table, form, status, and layout components where practical.
- Handle loading, empty, validation, and error states.
- Navigation and visible actions respect the current user's role (UX only; the backend enforces
  access).

## Backend Conventions

- Enforce business authorization server-side.
- Keep controllers thin; services hold workflow and business logic; repositories handle
  persistence.
- Make meaningful state transitions explicit.
- Never expose sensitive values in API responses or logs.

## Agent Workflow

- Read the constitution before making architectural decisions.
- Read the active spec and plan under `specs/` before implementing a feature.
- Prefer complete vertical slices over infrastructure-first work.
- Do not broaden feature scope without reporting it.
- Do not add dependencies or infrastructure unless they solve a current approved requirement.
- Run relevant tests and builds after changes.
- Report deviations, blockers, and important assumptions clearly.
