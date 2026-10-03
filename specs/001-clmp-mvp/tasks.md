---

description: "Task list for CLMP MVP — Consultant-to-Placement Lifecycle"
---

# Tasks: CLMP MVP — Consultant-to-Placement Lifecycle

**Input**: Design documents from `/specs/001-clmp-mvp/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ (rest-api.md,
authorization-matrix.md, ui-routes.md), quickstart.md

**Tests**: Included. Constitution VI requires automated tests for important business workflows,
and research R16 defines the test strategy. Within each story, write the test tasks first and
confirm they fail before implementing.

**Organization**: Tasks are grouped by user story (spec priority order) so each story can be
implemented, tested, and demonstrated as its own vertical slice (constitution II). Shared backend
and UI pieces are created **in the first story that consumes them**. Phase 2 contains only what
US1 needs (research R19-1).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: User story the task belongs to (US1–US8)

## Path Conventions

- Backend main: `backend/src/main/java/com/ensar/clmp/<module>/{web,service,domain}/`
- Backend tests: `backend/src/test/java/com/ensar/clmp/`
- Backend resources: `backend/src/main/resources/`
- Frontend: `frontend/src/`
- Every REST controller uses request/response DTOs (Java records) and never an `@Entity`
  (constitution IV). Every controller method carries `@PreAuthorize` per
  `contracts/authorization-matrix.md`. Ownership checks live in the module's `*AccessPolicy`
  service (research R7).
- Every mutating endpoint that updates an existing record takes `version` and returns
  `409 CONCURRENT_MODIFICATION` on mismatch, from US2 onward (research R10).
- Error responses use the codes in `contracts/rest-api.md § Errors`. Each story adds the codes it
  introduces to `ErrorCode`.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Scaffold both projects and get the builds green.

- [X] T001 Verify the active shell toolchain before any code: `java -version` reports 21 and `node -v` reports ≥ 20.19 (22 LTS preferred). If either is missing, stop and report it as a blocker (plan Deviations item 6). Record the versions found in `specs/001-clmp-mvp/quickstart.md` under Prerequisites only if they differ from the documented ones.
- [X] T002 Create the backend Maven project in `backend/pom.xml`: Spring Boot 4.0.x parent, `java.version` 21, groupId `com.ensar`, artifactId `clmp`. Dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `com.h2database:h2` (runtime), plus `spring-boot-starter-test` and `spring-security-test` (test). No other dependencies (research R2). Add the Maven Wrapper (`backend/mvnw`, `backend/mvnw.cmd`, `backend/.mvn/wrapper/maven-wrapper.properties`).
- [X] T003 Create `backend/src/main/java/com/ensar/clmp/ClmpApplication.java` (`@SpringBootApplication`).
- [X] T004 Create `backend/src/main/resources/application.yml` with:
  - `server.port: 8080`
  - `server.servlet.session.timeout: 30m`
  - `server.servlet.session.cookie.http-only: true`, `same-site: lax`
  - `spring.datasource.url: jdbc:h2:mem:clmp`
  - `spring.jpa.hibernate.ddl-auto: create`, `spring.jpa.open-in-view: false`
  - `clmp.time-zone: America/New_York`
  - Logging at INFO with no SQL parameter binding logs

  Create `application-dev.yml` (seeding on, `spring.h2.console.enabled: true`),
  `application-file.yml` (`jdbc:h2:file:./data/clmp`, `ddl-auto: update`), and
  `application-perf.yml` (`clmp.seed.perf: true`) in `backend/src/main/resources/` (research R4).
- [X] T005 [P] Create the frontend Vite project in `frontend/`.
  - `frontend/package.json`:
    - Dependencies: `react@19`, `react-dom@19`, `react-router@7`.
    - devDependencies: `typescript@5`, `vite@7`, `@vitejs/plugin-react`, `vitest`,
      `@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`,
      `jsdom`, `@types/react`, `@types/react-dom`. No UI kit or state/data library
      (research R3).
    - Scripts: `dev`, `build`, `typecheck` (`tsc --noEmit`), `test` (`vitest`).
  - Also create `frontend/tsconfig.json` (strict), `frontend/index.html`, and
    `frontend/src/main.tsx` (renders `<App/>`).
- [X] T006 [P] Create `frontend/vite.config.ts`: React plugin, dev `server.proxy` mapping `/api` → `http://localhost:8080` (research R6), and a Vitest `test` block (`environment: 'jsdom'`, `setupFiles: ['src/test/setup.ts']`). Also create `frontend/src/test/setup.ts`, which imports `@testing-library/jest-dom`.
- [X] T007 [P] Create `frontend/src/styles/tokens.css` (color, spacing, and type tokens) and `frontend/src/styles/global.css` (base layout, table, and form styles). Import both in `frontend/src/main.tsx`.
- [X] T008 [P] Create the root `.gitignore`, covering `backend/target/`, `backend/data/`, `frontend/node_modules/`, `frontend/dist/`, and IDE folders.
- [X] T009 Run `./mvnw verify` in `backend/` and `npm install && npm run typecheck && npm run build` in `frontend/`. Both MUST pass. Commit `frontend/package-lock.json`.

---

## Phase 2: Foundational (Blocking Prerequisites for US1)

**Purpose**: Only the pieces that US1 (sign-in and role shell) consumes: error responses, users,
recruiter profiles (for the signed-in user's `recruiterId`), security, demo users, the test
harness, and the frontend API client. Everything else is built inside the story that first needs
it (constitution II).

**⚠️ CRITICAL**: US1 cannot begin until this phase is complete.

- [X] T010 [P] Create `backend/src/main/java/com/ensar/clmp/common/error/ErrorCode.java`, an enum in which each code carries its HTTP status. Start with the codes US1 uses: `VALIDATION_FAILED` 400, `UNAUTHENTICATED` 401, `INVALID_CREDENTIALS` 401, `NOT_AUTHORIZED` 403, `NOT_FOUND` 404. Each later story adds its own codes from `contracts/rest-api.md § Errors`. Also create `common/error/BusinessException.java` (code, detail message, `Map<String,Object> extras`, optional `fieldErrors`).
- [X] T011 Create `backend/src/main/java/com/ensar/clmp/common/error/GlobalExceptionHandler.java` (`@RestControllerAdvice`). It returns `ProblemDetail` with a `code` property and `extras` merged in. It maps:
  - `MethodArgumentNotValidException`/`ConstraintViolationException` → 400 `VALIDATION_FAILED` with `fieldErrors[{field,message}]`
  - `BusinessException` → its code and status
  - `AccessDeniedException` → 403 `NOT_AUTHORIZED`
  - `EntityNotFoundException`/`NoSuchElementException` → 404 `NOT_FOUND`
  - Anything else → 500 with a generic message

  It MUST NOT include request bodies or sensitive values in responses or logs (FR-007,
  constitution VII).
- [X] T012 [P] Create `backend/src/main/java/com/ensar/clmp/auth/domain/Role.java` (enum `ADMIN, MANAGER, RECRUITER, HR_OPERATIONS`), `auth/domain/AppUser.java` (`@Entity app_user`: `id` Long PK; `username` String(60) required, unique case-insensitive; `password_hash` String(100) BCrypt, never serialized and no `toString` inclusion; `display_name` String(120) required; `role` enum required; `active` boolean), and `auth/domain/AppUserRepository.java` (`findByUsernameIgnoreCase`).
- [X] T013 [P] Create `backend/src/main/java/com/ensar/clmp/reference/domain/Team.java` and `Region.java` (`@Entity team`/`region`: `id` Long PK; `code` String(40) unique, immutable; `name` String(80); `active` boolean), plus `TeamRepository.java` and `RegionRepository.java`. These are needed now because `Recruiter` references them.
- [X] T014 [P] Create `backend/src/main/java/com/ensar/clmp/recruiter/domain/RecruiterStatus.java` (enum `ACTIVE, INACTIVE`), `recruiter/domain/Recruiter.java`, and `recruiter/domain/RecruiterRepository.java` (`JpaSpecificationExecutor`, `findByUserId`, `existsByEmailIgnoreCase`). `Recruiter` is `@Entity recruiter`:
  - `id` Long PK
  - `full_name` String(120) required
  - `email` String(254) required, valid email, unique case-insensitive
  - `phone` String(30) optional
  - `team_id` FK → team, required
  - `region_id` FK → region, required
  - `status` RecruiterStatus, default ACTIVE
  - `user_id` FK → app_user, nullable, unique
  - `@Version version`, `created_at`, `updated_at`
- [X] T015 Create `backend/src/main/java/com/ensar/clmp/auth/CurrentUser.java` (record `userId, username, displayName, role, Long recruiterId` nullable) and `auth/CurrentUserProvider.java`, which resolves `CurrentUser` from the `SecurityContext` and looks up the linked recruiter through `RecruiterRepository.findByUserId`. Also create `auth/service/ClmpUserDetailsService.java`, which loads `AppUser` by username (case-insensitive), maps the role to authority `ROLE_<role>`, and sets `isEnabled()` = `active` (FR-005).
- [X] T016 Create `backend/src/main/java/com/ensar/clmp/auth/config/SecurityConfig.java`:
  - `SecurityFilterChain` with session-based authentication (`SessionCreationPolicy.IF_REQUIRED`), no form login, no HTTP Basic, and `@EnableMethodSecurity`.
  - `BCryptPasswordEncoder` bean and `AuthenticationManager` bean (DaoAuthenticationProvider with `ClmpUserDetailsService`).
  - CSRF with `CookieCsrfTokenRepository.withHttpOnlyFalse()` and the SPA `CsrfTokenRequestHandler` pattern.
  - An `AuthenticationEntryPoint` that returns 401 `UNAUTHENTICATED` ProblemDetail JSON (no redirect), and an `AccessDeniedHandler` that returns 403 `NOT_AUTHORIZED` ProblemDetail.
  - `permitAll` for `POST /api/auth/login` and `GET /api/auth/csrf`. Everything else under `/api/**` requires authentication.
  - `/h2-console/**` requires ADMIN, with `frameOptions.sameOrigin()`, only when the `dev` profile is active (research R5, R7).
- [X] T017 Create `backend/src/main/java/com/ensar/clmp/seed/DemoDataSeeder.java` (`ApplicationRunner`, `@Profile("dev")`). It refuses to run, failing startup, if the `prod` profile is active. It seeds:
  - Teams: Java, .NET, Data & Analytics, DevOps & Cloud.
  - Regions: East, Central, West, Offshore.
  - Users, all with BCrypt password `Demo@123`: `admin` ADMIN, `manager` MANAGER, `recruiter1`/`recruiter2`/`recruiter3` RECRUITER, `hr` HR_OPERATIONS, and `inactive.user` RECRUITER with `active=false`.
  - Recruiters: "Riya Patel" (Java, East, linked to recruiter1), "Marcus Lee" (Data & Analytics, Central, linked to recruiter2), and two unlinked active recruiters (research R17). recruiter3 stays unlinked.
  - The seeder is structured as one private method per area (`seedReference`, `seedUsersAndRecruiters`), so stories can add `seedConsultants`, `seedMarketing`, `seedSubmissions`, and `seedPlacements`.
- [X] T018 Create the backend test harness in `backend/src/test/java/com/ensar/clmp/support/`:
  - `IntegrationTestBase.java`: `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("dev")`. Helpers: `loginAs(username)` returns a `MockHttpSession` by POSTing to `/api/auth/login` with CSRF; `json(...)`; `expectProblem(result, status, code)`.
  - `TestDataFactory.java`: a Spring bean that creates entities directly through repositories for test setup. It grows per story.
  - Each IT class uses `@DirtiesContext` or explicit cleanup so state stays isolated.
- [X] T019 [P] Create `frontend/src/api/types.ts` with only the types US1 needs: `Role`, `CurrentUser`, and `ApiProblem {status, code, title, detail, fieldErrors?, ...extras}`. Each later story appends the types it consumes.
- [X] T020 Create `frontend/src/api/client.ts`, a typed `fetch` wrapper (`get`, `post`, `put`, `patch`).
  - It sends `credentials: 'same-origin'`.
  - It reads the `XSRF-TOKEN` cookie and sends `X-XSRF-TOKEN` on every non-GET request. On the first mutating call without a cookie, it calls `GET /api/auth/csrf`.
  - It parses `application/problem+json` into a thrown `ApiError` (carrying `ApiProblem`).
  - On any 401 except from `/api/auth/login`, it dispatches a `clmp:session-expired` window event.

**Checkpoint**: The foundation for US1 is ready. `./mvnw verify` and `npm run build` pass.

---

## Phase 3: User Story 1 — Sign In and Role-Appropriate Experience (Priority: P1) 🎯 MVP

**Goal**: Users sign in, land on a dashboard shell, and see only their role's navigation.
Forbidden areas are refused by the backend, and sessions end on logout or after 30 minutes idle.

**Independent Test**: Sign in as each seeded demo user. Each should see only their permitted nav
items. Opening a forbidden page directly is refused, and API calls into forbidden areas return
403 (quickstart V1).

### Tests for User Story 1

- [X] T021 [P] [US1] Write `backend/src/test/java/com/ensar/clmp/it/US1SignInIT.java`:
  - AS 1.1: `GET /api/auth/me` after login returns the correct `role`, and `recruiterId` for recruiter1.
  - AS 1.2: a wrong password and an unknown username both return 401 `INVALID_CREDENTIALS` with an identical `detail`.
  - AS 1.3: as recruiter1, `GET /api/recruiters` and `GET /api/reports/consultant-pipeline` return 403 `NOT_AUTHORIZED` with no data.
  - AS 1.4: as hr, `POST /api/submissions` and `POST /api/marketing-assignments` return 403, and nothing is persisted.
  - AS 1.5: after `POST /api/auth/logout`, `GET /api/auth/me` returns 401. Also assert `server.servlet.session.timeout` resolves to 30 minutes.
  - AS 1.6: `inactive.user` login returns 401 `INVALID_CREDENTIALS`.
  - No response body contains `password` or `passwordHash` (FR-007).
- [X] T022 [P] [US1] Write `frontend/src/test/navigation.test.tsx`. It renders `NavBar` for each role and asserts the exact nav items from `contracts/authorization-matrix.md § Frontend navigation`. It also asserts that `RequireRole` renders `NotAuthorized` (and calls no area API) for a forbidden role, for example RECRUITER on `/recruiters` and `/reports`.
- [X] T023 [P] [US1] Write `backend/src/test/java/com/ensar/clmp/architecture/NoEntityInControllerSignatureTest.java`, a plain JUnit test that scans classes under `com.ensar.clmp` annotated with `@RestController`. It fails if any public method's return type, generic type arguments, or parameter types is annotated `@Entity` (constitution IV, research R12). It is added here because US1 introduces the first controller.

### Implementation for User Story 1

- [X] T024 [US1] Create `backend/src/main/java/com/ensar/clmp/auth/web/AuthController.java` and DTOs `auth/web/LoginRequest.java` (`username` and `password` `@NotBlank`) and `auth/web/CurrentUserResponse.java` (`id, username, displayName, role, recruiterId, sessionTimeoutMinutes`). Endpoints:
  - `GET /api/auth/csrf` returns 204 and loads the token so the cookie is set.
  - `POST /api/auth/login` calls `AuthenticationManager.authenticate`. On success it saves the `SecurityContext` through `HttpSessionSecurityContextRepository`, rotates the session id, and returns 200 `CurrentUserResponse`. On any `AuthenticationException`, including a disabled user, it returns 401 `INVALID_CREDENTIALS` with the same generic message, "Invalid username or password."
  - `POST /api/auth/logout` invalidates the session, clears the context, and returns 204.
  - `GET /api/auth/me`.
  - It never logs the request body.
- [X] T025 [US1] Extend `backend/src/main/java/com/ensar/clmp/auth/config/SecurityConfig.java` with URL-level role rules that mirror `contracts/authorization-matrix.md` as defense in depth, so forbidden areas return 403 before their controllers exist:
  - `/api/recruiters/**` and `/api/reports/**`: ADMIN, MANAGER
  - `/api/marketing-assignments/**`, `/api/submissions/**`, `/api/placements/**`, `/api/vendors/**`, `/api/clients/**`: ADMIN, MANAGER, RECRUITER
  - `/api/consultants/**`, `/api/dashboard`, `/api/reference`, `/api/auth/**`: any authenticated user

  Method-level `@PreAuthorize` adds per-action restrictions in later stories.
- [X] T026 [P] [US1] Create `frontend/src/auth/permissions.ts`, the single map of role → nav items, role → display label, and role → action capabilities, copied from `contracts/authorization-matrix.md`. It is UX only, for example `canEditConsultant`, `canManageRecruiters`, `canCreateSubmission`, `canEditPlacement`, `canAssignRecruiter`, `canChangeConsultantStatus`, `canCreateMarketing`, `canViewReports`, `canViewPlacements`, `canSeeCommercialDetails` (false for HR_OPERATIONS).
- [X] T027 [P] [US1] Create `frontend/src/api/auth.ts` with `login`, `logout`, `me`, and `csrf`.
- [X] T028 [P] [US1] Create the shared components US1 consumes:
  - `frontend/src/components/LoadingState.tsx`, used by the route guard while `/me` loads.
  - `frontend/src/components/FormField.tsx`: label, input slot, and an inline error from `fieldErrors` by field name.
  - `frontend/src/components/ErrorBanner.tsx`: shows the problem detail and `missingItems` as a list. For `CONCURRENT_MODIFICATION` it shows "This record was changed by someone else. Reload to continue." with a Reload button. For 403 it shows "Not authorized".
- [X] T029 [US1] Create `frontend/src/auth/AuthProvider.tsx` (context with `user`, `login`, `logout`, and `loading`; loads `/api/auth/me` on start; on the `clmp:session-expired` event it clears the user and navigates to `/login?expired=1`) and `frontend/src/auth/RequireRole.tsx` (shows `LoadingState` while loading, redirects to `/login` when there is no user, and renders `NotAuthorized` when the role isn't in `roles`).
- [X] T030 [P] [US1] Create `frontend/src/components/NavBar.tsx` (items from `permissions.ts`, the user's display name and role label, and a Sign out button) and `frontend/src/components/PageLayout.tsx` (NavBar plus title, actions slot, and content).
- [X] T031 [US1] Create `frontend/src/pages/login/LoginPage.tsx`, built from `FormField` and `ErrorBanner`, with username and password fields and a generic error on 401. It shows "Your session expired. Please sign in again." when `?expired=1` is present and redirects to `/` on success.
- [X] T032 [P] [US1] Create `frontend/src/pages/NotAuthorized.tsx` and `frontend/src/pages/NotFound.tsx`.
- [X] T033 [US1] Create `frontend/src/routes.tsx` and `frontend/src/App.tsx`. Declare every route in `contracts/ui-routes.md`, wrapped in `RequireRole` with that route's roles. Areas not yet built render a minimal `PageLayout` titled with the area name, to be replaced by later stories. Also create `frontend/src/pages/dashboard/DashboardPage.tsx` as a welcome shell that US7 replaces.
- [X] T034 [US1] Run `./mvnw verify` and `npm run typecheck && npm test -- --run && npm run build`, and walk through quickstart V1.1–V1.6 manually.

**Checkpoint**: Sign-in, role navigation, session expiry, and backend refusal work.

---

## Phase 4: User Story 2 — Maintain Consultant Profiles and Readiness (Priority: P1)

**Goal**: ADMIN and HR_OPERATIONS create and maintain consultant profiles (starting in Bench),
mark them Ready only when complete, and apply the manual status transitions. Every change is
recorded in history.

**Independent Test**: As hr, create a consultant (it lands in Bench), complete the profile, and
mark it Ready. The seeder supplies an assigned active recruiter for this, because the assignment
UI arrives in US3. Then view the history with user and time for each change (quickstart V2).

**First consumer of**: the org clock, paging, the version guard, the history module (write and
per-entity read), the reference API, and the shared list and table UI components, including the
unlinked-recruiter empty state.

### Tests for User Story 2

- [X] T035 [P] [US2] Write `backend/src/test/java/com/ensar/clmp/lifecycle/ConsultantManualTransitionTest.java`, a unit test with mocked repositories and HistoryService. It covers every allowed FR-031 transition, and refusal of everything else with `INVALID_TRANSITION` plus `allowedTransitions`:
  - BENCH→READY
  - READY→BENCH
  - BENCH/READY/MARKETING/INTERVIEWING→HOLD, reason required
  - HOLD→BENCH, HOLD→READY
  - any→INACTIVE, reason required
  - INACTIVE→BENCH
  - ACTIVE_PROJECT→BENCH

  It asserts MARKETING, INTERVIEWING, and PLACED can never be targets of a manual change
  (FR-032). It also checks that MANAGER and RECRUITER callers get `NOT_AUTHORIZED`. Placement and
  marketing guards are added in US4–US6.
- [X] T036 [P] [US2] Write `backend/src/test/java/com/ensar/clmp/consultant/ReadinessCheckerTest.java`. It asserts the `missingItems` keys `firstName, lastName, email, phone, primarySkill, yearsExperience, visaType, assignedActiveRecruiter`, and that an assigned but INACTIVE recruiter counts as missing (FR-035).
- [X] T037 [P] [US2] Write `backend/src/test/java/com/ensar/clmp/it/US2ConsultantProfileIT.java`:
  - AS 2.1: hr POST with only first name, last name, and email returns 201 with status BENCH.
  - AS 2.2: marking Ready returns 422 `READINESS_INCOMPLETE` with `missingItems`.
  - AS 2.3: on a complete profile with the seeded assignment, marking Ready returns 200, and the history has a STATUS row BENCH→READY with the hr display name and timestamp.
  - AS 2.4: list search by name and filters for status (multi), primarySkill, visaType, and recruiterId.
  - AS 2.5: a duplicate email (case-insensitive) on create and on update returns 409 `DUPLICATE_EMAIL`.
  - AS 2.6: READY→HOLD with a reason is recorded in history.
  - FR-024: list items never contain `email`, `phone`, `visaExpirationDate`, or `notes`.
  - FR-026: recruiter1 sees only consultants assigned to Riya Patel, gets 403 on a non-owned id, and gets 403 on PUT.
  - Unlinked recruiter3 gets an empty page.
  - FR-103: the detail `contact` is present for admin, manager, hr, and the assigned recruiter.
  - Validation: yearsExperience 51 returns 400 `VALIDATION_FAILED` with `fieldErrors[yearsExperience]`.
  - Stale `version` on PUT returns 409 `CONCURRENT_MODIFICATION`.
  - A profile edit writes PROFILE_UPDATED with field names only and no values.
  - `size=101` returns 400.
- [X] T038 [P] [US2] Write `frontend/src/test/consultantForm.test.tsx`. It renders `ConsultantFormPage` with a mocked `client` that rejects with a 400 `VALIDATION_FAILED` problem, and asserts each `fieldErrors` message renders next to its field.
- [X] T039 [P] [US2] Write `frontend/src/test/unlinkedRecruiterEmptyState.test.tsx`. With an auth context of a RECRUITER whose `recruiterId` is null, render `DataTable` with zero rows and assert it shows "Your account is not linked to a recruiter profile. Contact an Admin.". For any other user, assert the generic empty message (spec edge case, ui-routes cross-cutting).

### Shared pieces first consumed by US2

- [X] T040 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/common/time/ClockConfig.java`, which exposes a `java.time.Clock` bean in the zone from `clmp.time-zone`, and `common/time/OrgTime.java`, a service with `today()`, `currentMonthStart()`, `currentMonthEnd()`, and `toOrgDate(Instant)` (research R15). In `backend/src/test/java/com/ensar/clmp/support/IntegrationTestBase.java`, add a `@TestConfiguration` that overrides `Clock` with a fixed instant `2026-10-15T15:00:00Z`.
- [X] T041 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/common/web/PageResponse.java` (record `items, page, size, totalItems, totalPages`, with a `from(Page<T>)` factory) and `common/web/PageRequests.java`. `PageRequests` builds a `Pageable` from `page`/`size`/`sort` with default size 25 and maximum size 100. It accepts only `sort` fields from a per-list allowlist and returns `400 VALIDATION_FAILED` for anything else.
- [X] T042 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/common/domain/Versioned.java` (an interface with `getVersion()`) and `common/service/VersionGuard.java` with `check(Versioned entity, long requestVersion)`. A mismatch throws `BusinessException(CONCURRENT_MODIFICATION)` with extra `currentVersion`. Add `CONCURRENT_MODIFICATION` 409 and `DUPLICATE_EMAIL` 409 to `ErrorCode`, and map `ObjectOptimisticLockingFailureException` → 409 `CONCURRENT_MODIFICATION` in `GlobalExceptionHandler.java` (research R10).
- [X] T043 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/reference/domain/VisaType.java` (enum `US_CITIZEN, GREEN_CARD, H1B, H4_EAD, L2_EAD, OPT, STEM_OPT, CPT, TN, OTHER`, per FR-021).
- [X] T044 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/history/domain/HistoryEntityType.java` (`CONSULTANT, RECRUITER, MARKETING_ASSIGNMENT, SUBMISSION, PLACEMENT`) and `history/domain/ChangeType.java` (`CREATED, STATUS, RECRUITER_ASSIGNMENT, OWNER_TRANSFER, PROFILE_UPDATED, FIELD_EDIT, NOTE_ADDED`).
- [X] T045 [US2] Create `backend/src/main/java/com/ensar/clmp/history/domain/HistoryRecord.java`, an immutable `@Entity history_record` with no setters after construction. Fields from data-model.md:
  - `entity_type`, `entity_id`
  - `consultant_id` nullable, `owner_recruiter_id` nullable
  - `change_type`
  - `field_name` String(60) nullable
  - `old_value`/`new_value` String(255) nullable
  - `reason` String(500) nullable, `note` String(2000) nullable
  - `actor_user_id`, `actor_display_name` String(120)
  - `occurred_at` Instant

  The trigger columns are added in US4 (T083). Indexes: `(entity_type, entity_id, occurred_at)`,
  `(occurred_at)`, `(consultant_id)`.

  Also create `history/domain/HistoryRecordRepository.java`. It extends
  `org.springframework.data.repository.Repository` (not JpaRepository), so it exposes only `save`
  and query methods, with no delete or update (FR-101). It also implements
  `JpaSpecificationExecutor<HistoryRecord>`.
- [X] T046 [US2] Create `backend/src/main/java/com/ensar/clmp/history/service/HistoryService.java`, the write API: `recordCreated`, `recordStatusChange(entityType, entityId, consultantId, ownerRecruiterId, old, new, reason, note, actor)`, `recordRecruiterAssignment`, `recordOwnerTransfer`, `recordProfileUpdated(consultantId, List<String> changedFieldNames, actor)` (field *names* only, never values), and `recordFieldEdit`. It is `@Transactional(propagation = MANDATORY)` so history is always written in the caller's transaction (FR-100), and timestamps come from the injected `Clock`.
- [X] T047 [US2] Create `backend/src/main/java/com/ensar/clmp/history/web/HistoryEntry.java`, a response record matching `contracts/rest-api.md` HistoryEntry: `id, occurredAt, actor, changeType, field, oldValue, newValue, reason, note, description`. The `systemTriggered` and `trigger` fields are added in US4. Also create `history/service/HistoryQueryService.java` with `forEntity(entityType, entityId, viewer, pageable)`, which renders a plain `description` per change type. The recent-activity feed is added in US7.
- [X] T048 [US2] Create `backend/src/main/java/com/ensar/clmp/reference/web/ReferenceController.java` and `reference/web/ReferenceResponse.java`: `GET /api/reference`, open to all authenticated roles, returning `{teams[{id,code,name}], regions[...], visaTypes[], consultantStatuses[]}`. US4 adds `marketingStatuses` and US5 adds `submissionStatuses`.
- [X] T049 [P] [US2] Create these frontend pieces:
  - `frontend/src/hooks/useApi.ts`: `{data, error, loading, reload}` for a loader function.
  - `frontend/src/hooks/useUrlFilters.ts`: reads and writes list filters and `page`/`sort` in the URL query string, supporting repeated keys such as `status=A&status=B`, so dashboard and report `link.query` strings open pre-filtered lists (FR-083).
  - `frontend/src/labels.ts`: display-label maps for `ConsultantStatus` and `VisaType`, extended per story. For example `H4_EAD` → "H-4 EAD".
- [X] T050 [P] [US2] Create shared list and detail components:
  - `frontend/src/components/DataTable.tsx`: column defs, server paging controls, sortable headers wired to `useUrlFilters`, and a row link. Its empty state uses `EmptyState`.
  - `frontend/src/components/EmptyState.tsx`: when the signed-in user is a RECRUITER with `recruiterId === null`, it renders "Your account is not linked to a recruiter profile. Contact an Admin." instead of the generic message.
  - `frontend/src/components/StatusBadge.tsx`
  - `frontend/src/components/HistoryList.tsx`: renders `HistoryEntry[]` with actor, time, old → new, reason/note, and description.
- [X] T051 [P] [US2] Create `frontend/src/components/FilterBar.tsx` and `frontend/src/components/ReasonDialog.tsx` (required reason textarea, with an optional `required` flag).

### Implementation for User Story 2

- [X] T052 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/consultant/domain/ConsultantStatus.java` (enum `BENCH, READY, MARKETING, INTERVIEWING, PLACED, ACTIVE_PROJECT, HOLD, INACTIVE`) and `consultant/domain/Consultant.java`, `@Entity consultant` with these fields, exactly as in data-model.md:
  - `first_name`, `last_name` String(60), required at creation
  - `email` String(254), required, valid email, unique case-insensitive (store and compare lower-cased)
  - `phone` String(30) optional
  - `city` String(80), `state` String(40)
  - `primary_skill` String(80) optional
  - `additional_skills` String(500)
  - `years_experience` Integer 0–50 when present
  - `visa_type` VisaType optional
  - `visa_expiration_date` LocalDate optional
  - `notes` String(2000)
  - `status`, initial BENCH
  - `current_recruiter_id` nullable `@ManyToOne` Recruiter
  - `@Version version`, `created_at`, `created_by_user_id`, `updated_at`

  Indexes on `status` and `current_recruiter_id`. NO SSN, DOB, bank, or ID-document fields
  (FR-027). No delete.
- [X] T053 [US2] Create `backend/src/main/java/com/ensar/clmp/consultant/domain/ConsultantRepository.java` with:
  - `JpaSpecificationExecutor`
  - `existsByEmailIgnoreCaseAndIdNot`
  - `@Lock(PESSIMISTIC_WRITE) findByIdForUpdate(Long id)`
  - `countByStatus`, grouped
  - `findDistinctPrimarySkills`
  - `countByCurrentRecruiterIdIn`, grouped, used by the recruiter list in US3
- [X] T054 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantFilter.java` (record: `q, List<ConsultantStatus> statuses, primarySkill, visaType, recruiterId, Boolean needsReassignment`) and `consultant/service/ConsultantSpecifications.java`:
  - `q` matches case-insensitively on first, last, or full name.
  - `needsReassignment` = current recruiter not null AND recruiter.status = INACTIVE.
  - `scopeFor(CurrentUser)`: ADMIN, MANAGER, and HR see all. RECRUITER sees `current_recruiter_id = recruiterId`. A RECRUITER with no recruiterId matches nothing.
- [X] T055 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantAccessPolicy.java`:
  - `assertCanView`: ADMIN, MANAGER, HR, or the assigned RECRUITER.
  - `assertCanEditProfile`: ADMIN, HR.
  - `canSeeContact`: ADMIN, MANAGER, HR, or the assigned RECRUITER.
  - `canSeeCommercialSections`: not HR.
  - Refusals throw `AccessDeniedException`, which maps to 403 `NOT_AUTHORIZED`.
- [X] T056 [P] [US2] Create `backend/src/main/java/com/ensar/clmp/consultant/service/ReadinessChecker.java`, returning the ordered `List<String>` of missing items per FR-035 (keys as in T036).
- [X] T057 [US2] Create `backend/src/main/java/com/ensar/clmp/lifecycle/ConsultantLifecycleService.java`, the single owner of consultant status rules (research R9):
  - Implement `changeStatusManually(consultantId, target, reason, version, CurrentUser actor)` with the FR-031 transition table, the ADMIN/HR-only check, reason required for HOLD and INACTIVE (400 `VALIDATION_FAILED` on `reason`), and the readiness guard for →READY (422 `READINESS_INCOMPLETE` + `missingItems`). It uses `VersionGuard` and writes a STATUS history row with `reason`.
  - Implement `allowedManualTransitions(consultant, actor)`, which returns an empty list for MANAGER and RECRUITER.
  - Add `INVALID_TRANSITION` 422 and `READINESS_INCOMPLETE` 422 to `ErrorCode`.
  - Leave clearly named private hook methods `cascadeOnHold`, `cascadeOnInactive`, and `guardInactive` that US4/US5 fill in. They start as no-ops.
- [X] T058 [US2] Create the consultant DTOs in `backend/src/main/java/com/ensar/clmp/consultant/web/`:
  - `ConsultantRequest.java`: `firstName`/`lastName` `@NotBlank @Size(max=60)`, `email` `@NotBlank @Email @Size(max=254)`, `phone` `@Size(max=30)`, `city` `@Size(max=80)`, `state` `@Size(max=40)`, `primarySkill` `@Size(max=80)`, `additionalSkills` `@Size(max=500)`, `yearsExperience` `@Min(0) @Max(50)`, `visaType`, `visaExpirationDate`, `notes` `@Size(max=2000)`, `Long version` (required on PUT).
  - `ConsultantListItem.java`: `id, fullName, primarySkill, yearsExperience, visaType, assignedRecruiter{id,fullName}, status, needsReassignment`. NO contact fields.
  - `ConsultantDetail.java`, with `@JsonInclude(NON_NULL)`: profile fields, `status`, `needsReassignment`, `assignedRecruiter{id,fullName,status}`, `contact{email,phone,visaExpirationDate,notes}` (nullable), `allowedStatusTransitions`, `missingReadinessItems`, `version`. US4–US6 add `currentMarketingAssignment`, `submissions`, `placements`, and `openSubmissionsWhileOnHold`, which are always null for HR.
  - `StatusChangeRequest.java`: `targetStatus` `@NotNull`, `reason`, `version` `@NotNull`.
- [X] T059 [US2] Create `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantService.java`:
  - `create`: status BENCH, email uniqueness → 409 `DUPLICATE_EMAIL` with `fieldErrors[email]`, history CREATED.
  - `update`: version check, uniqueness, history PROFILE_UPDATED with changed field *names* only.
  - `getDetail(id, actor)`: role-shaped through `ConsultantAccessPolicy`.
  - `list(filter, pageable, actor)`: specification plus scope, mapping to `ConsultantListItem` with a fetch join on the recruiter to avoid N+1.
  - `distinctSkills()`.
- [X] T060 [US2] Create `backend/src/main/java/com/ensar/clmp/consultant/web/ConsultantController.java`:
  - `GET /api/consultants`: params `q, status (multi), primarySkill, visaType, recruiterId, needsReassignment, page, size, sort`, with sort allowlist `lastName, status, yearsExperience, primarySkill`. ADMIN, MANAGER, RECRUITER, HR.
  - `GET /api/consultants/{id}`
  - `POST /api/consultants`: ADMIN, HR.
  - `PUT /api/consultants/{id}`: ADMIN, HR.
  - `POST /api/consultants/{id}/status`: ADMIN, HR; delegates to `ConsultantLifecycleService`.
  - `GET /api/consultants/{id}/history`: through `HistoryQueryService`, after the view check.
  - `GET /api/consultants/skills`
- [X] T061 [US2] Extend `backend/src/main/java/com/ensar/clmp/seed/DemoDataSeeder.java` with `seedConsultants()`. It adds about 12 consultants across BENCH, READY, HOLD, and INACTIVE, with complete and incomplete profiles. Several are assigned directly to Riya Patel and Marcus Lee: at least one complete Bench consultant assigned to Riya, for AS 2.3, and one assigned to an unlinked recruiter.
- [X] T062 [P] [US2] Create `frontend/src/api/consultants.ts`, typed calls for every consultant endpoint in `contracts/rest-api.md`. Append `PageResponse<T>`, `HistoryEntry`, `ConsultantStatus`, `VisaType`, `ConsultantListItem`, `ConsultantDetail`, and `ConsultantRequest` to `frontend/src/api/types.ts`. Also create `frontend/src/api/reference.ts` (`GET /api/reference`, cached in module memory).
- [X] T063 [US2] Create `frontend/src/pages/consultants/ConsultantListPage.tsx`, using `DataTable` and `FilterBar` with search, status multi-select, skill, visa type, and recruiter, all driven by `useUrlFilters`.
  - Columns: name, primary skill, years, visa type, assigned recruiter, `StatusBadge`, and a "Needs reassignment" badge.
  - No email or phone columns (FR-024).
  - An "Add consultant" button shown only when `canEditConsultant`.
  - Loading, empty (including the unlinked-recruiter message), and error states.
- [X] T064 [US2] Create `frontend/src/pages/consultants/ConsultantFormPage.tsx`, handling create (`/consultants/new`) and edit (`/consultants/:id/edit`). Fields per `ConsultantRequest`, with a visa type select from `/api/reference`. Server `fieldErrors` render through `FormField`, and `ErrorBanner` handles 409 duplicate and concurrency errors. It navigates to the details page on save.
- [X] T065 [US2] Create `frontend/src/pages/consultants/ConsultantDetailPage.tsx`. It shows the profile, the `contact` section only when present, `StatusBadge`, and the assigned recruiter.
  - Status action buttons come from `allowedStatusTransitions`. HOLD and INACTIVE open a `ReasonDialog`.
  - On 422 `READINESS_INCOMPLETE`, `missingItems` render in `ErrorBanner`.
  - An Edit button appears when `canEditConsultant`.
  - `HistoryList` is loaded from `/api/consultants/{id}/history`.
  - Leave clearly separated panel slots for Marketing, Submissions, and Placements, filled in by US4–US6.
- [X] T066 [US2] Run `./mvnw verify` and `npm test -- --run && npm run build`, and walk through quickstart V2.

**Checkpoint**: The consultant profile and readiness slice works on its own with seeded
assignments.

---

## Phase 5: User Story 3 — Manage Recruiters and Consultant Assignment (Priority: P1)

**Goal**: ADMIN maintains the recruiter roster. ADMIN and MANAGER assign or reassign each
consultant's single primary recruiter. Inactive recruiters can't receive assignments, and
deactivating a recruiter flags their consultants as "needs reassignment".

**Independent Test**: As admin, add a recruiter and assign a consultant; the count goes up.
Deactivate the recruiter with confirmation; the consultants are flagged, and new assignments to
that recruiter are refused. As manager, reassign the consultant (quickstart V3).

**Depends on**: US2 (consultant entity and detail page).

### Tests for User Story 3

- [X] T067 [P] [US3] Write `backend/src/test/java/com/ensar/clmp/it/US3RecruiterAssignmentIT.java`:
  - AS 3.1: admin POST recruiter returns 201, ACTIVE, `assignedConsultantCount` 0.
  - AS 3.2: admin assign increments the count, and the consultant shows the recruiter.
  - AS 3.3: manager reassigns A→B. A's count goes down, B's goes up, and the history has a RECRUITER_ASSIGNMENT row (old and new names, manager, time).
  - AS 3.4: assigning to INACTIVE returns 422 `RECRUITER_INACTIVE`.
  - AS 3.5: deactivating with consultants and no `confirm` returns 409 `CONFIRMATION_REQUIRED` with `affectedConsultantCount`. With `confirm=true` it returns 200, and those consultants come back with `needsReassignment=true` from `GET /api/consultants?needsReassignment=true`.
  - AS 3.6: filters for team, region, and status, plus name search.
  - The manager gets 403 on POST/PUT/status of recruiters, and recruiter1 and hr get 403 on assignment.
  - A duplicate recruiter email returns 409 `DUPLICATE_EMAIL`.
  - `linkedUserId` must reference an unlinked RECRUITER user (400 otherwise).
  - SC-004: there is never more than one current recruiter.

### Implementation for User Story 3

- [X] T068 [P] [US3] Create `backend/src/main/java/com/ensar/clmp/recruiter/service/RecruiterFilter.java` (`q, teamId, regionId, status`) and `recruiter/service/RecruiterSpecifications.java`.
- [X] T069 [US3] Create `backend/src/main/java/com/ensar/clmp/recruiter/service/RecruiterService.java`:
  - `create`: team and region must exist and be active (FR-011), email unique case-insensitive → 409 `DUPLICATE_EMAIL`. The optional `linkedUserId` must be a RECRUITER user not already linked (FR-015). Status starts ACTIVE.
  - `update`: version check.
  - `changeStatus(id, status, confirm, version)`: when deactivating with `assignedConsultantCount > 0` and `confirm != true`, throw `CONFIRMATION_REQUIRED` (add 409 to `ErrorCode`) with `affectedConsultantCount`. On success, write a STATUS history row (`entity_type=RECRUITER`) and return `consultantsFlaggedForReassignment`. Marketing and submissions are NOT altered (FR-014, edge case).
  - `list`: fills `assignedConsultantCount` with ONE grouped count query (`ConsultantRepository.countByCurrentRecruiterIdIn`).
  - `linkableUsers()`.
- [X] T070 [US3] Create the recruiter DTOs in `backend/src/main/java/com/ensar/clmp/recruiter/web/`:
  - `RecruiterRequest.java`: `fullName` `@NotBlank @Size(max=120)`, `email` `@NotBlank @Email @Size(max=254)`, `phone` `@Size(max=30)`, `teamId` `@NotNull`, `regionId` `@NotNull`, `linkedUserId`, `version`.
  - `RecruiterListItem.java`
  - `RecruiterDetail.java`
  - `RecruiterStatusRequest.java`: `status`, `confirm`, `version`.
  - `LinkableUser.java`

  Then create `recruiter/web/RecruiterController.java`:
  - `GET /api/recruiters` and `GET /{id}`: ADMIN, MANAGER. Sort allowlist `fullName, status`.
  - `POST`, `PUT /{id}`, `POST /{id}/status`, `GET /linkable-users`: ADMIN.
- [X] T071 [US3] Create `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantAssignmentService.java` with `assign(consultantId, recruiterId, version, actor)`:
  - ADMIN or MANAGER only.
  - The target recruiter must be ACTIVE, else 422 `RECRUITER_INACTIVE` (add to `ErrorCode`, FR-034).
  - It replaces `current_recruiter_id`, so there is at most one primary recruiter (FR-033).
  - It writes a RECRUITER_ASSIGNMENT history row with the old and new recruiter names.
  - It leaves a named hook `transferOpenMarketingOwnership(consultant, newRecruiter, actor)` as a no-op, filled in by US4.

  Add `POST /api/consultants/{id}/recruiter` (body `{recruiterId @NotNull, version @NotNull}`,
  ADMIN and MANAGER) to `ConsultantController.java`.
- [X] T072 [P] [US3] Create `frontend/src/components/ConfirmDialog.tsx`, first used here for recruiter deactivation.
- [X] T073 [P] [US3] Create `frontend/src/api/recruiters.ts` (all recruiter endpoints and types, appended to `types.ts`), and add `assignRecruiter` to `frontend/src/api/consultants.ts`.
- [X] T074 [US3] Create `frontend/src/pages/recruiters/RecruiterListPage.tsx`, with search, team, region, and status filters. Columns: name, team, region, `StatusBadge`, and assigned consultant count. An "Add recruiter" button is shown for ADMIN only.
- [X] T075 [US3] Create `frontend/src/pages/recruiters/RecruiterFormPage.tsx`, handling create and edit, with team and region selects from `/api/reference` and a linked user select from `/linkable-users`. Field errors and duplicate email are handled.
- [X] T076 [US3] Create `frontend/src/pages/recruiters/RecruiterDetailPage.tsx`, showing profile fields and the assigned count. For ADMIN it has Activate and Deactivate buttons. On 409 `CONFIRMATION_REQUIRED` it opens a `ConfirmDialog` reading "N consultants are still assigned and will be flagged for reassignment", then re-sends with `confirm=true`.
- [X] T077 [US3] Create `frontend/src/pages/consultants/AssignRecruiterDialog.tsx`, offering active recruiters only, and wire it into `ConsultantDetailPage.tsx` as an "Assign / Reassign recruiter" action visible when `canAssignRecruiter`. Show the "Needs reassignment" badge on the detail page.
- [X] T078 [US3] Run `./mvnw verify` and `npm test -- --run && npm run build`, and walk through quickstart V3.

**Checkpoint**: US1–US3 (all P1) complete. The roster, assignment, and readiness flow works end
to end.

---

## Phase 6: User Story 4 — Market a Ready Consultant (Priority: P2)

**Goal**: A recruiter creates a marketing assignment for a **Ready** consultant and runs it
(Draft → Active → Hold → Closed) with append-only notes. Activation, reopening, and closing drive
the consultant's status automatically. A MANAGER may only hold, reopen, or close.

**Independent Test**: As recruiter1, create a Draft for a Ready own consultant and activate it
(the consultant becomes Marketing). Add a note, hold it, reopen it, and close it (the consultant
returns to Ready). As manager, only hold, reopen, and close are allowed. The consultant detail
page shows the marketing panel (quickstart V4).

**Depends on**: US2, US3. **First consumer of**: system-triggered history (trigger columns and
`TriggerRef`).

### Tests for User Story 4

- [ ] T079 [P] [US4] Write `backend/src/test/java/com/ensar/clmp/marketing/MarketingTransitionsTest.java`, a unit test covering the FR-041 table per role:
  - DRAFT→ACTIVE: ADMIN and the owning RECRUITER, not MANAGER.
  - DRAFT→CLOSED
  - ACTIVE→HOLD, reason required
  - HOLD→ACTIVE
  - ACTIVE or HOLD→CLOSED, reason required
  - CLOSED→ACTIVE: ADMIN and MANAGER only; refused when `close_reason` = "Placed".
  - Invalid transitions return `INVALID_TRANSITION`.
- [ ] T080 [P] [US4] Write `backend/src/test/java/com/ensar/clmp/it/US4MarketingIT.java`:
  - AS 4.1: create for a READY consultant returns DRAFT with the owner recruiter, team, and dates.
  - FR-040: create for BENCH, MARKETING, INTERVIEWING, HOLD, or INACTIVE returns 422 `CONSULTANT_NOT_ELIGIBLE`.
  - AS 4.2: activating returns ACTIVE, the consultant becomes MARKETING, and both history rows exist. The consultant row has `systemTriggered=true`, `trigger.event=MARKETING_ACTIVATED`, and the actor.
  - AS 4.3: activating when the consultant has since become BENCH, HOLD, or INACTIVE returns 422 `CONSULTANT_NOT_ELIGIBLE`.
  - AS 4.4: a second open assignment returns 409 `OPEN_ASSIGNMENT_EXISTS` with `existingRecordId`.
  - AS 4.5: hold with a reason, then reopen.
  - AS 4.6: close with a reason, and the consultant returns to READY.
  - AS 4.7: recruiter1 lists only own assignments, and `overdue=true` appears when `targetDate` < the fixed clock date and the status isn't CLOSED.
  - AS 4.8: the manager sees all teams and can hold, reopen, and close, but gets 403 on create, PUT dates, and notes.
  - A target date before the start date returns 400.
  - Notes have no update or delete endpoint.
  - Edge cases: a consultant manually set to HOLD moves an ACTIVE assignment to HOLD (system-triggered, `CONSULTANT_HOLD`). A consultant set to INACTIVE closes the open assignment with reason "Consultant inactive". Reassigning the consultant transfers the open assignment's owner and team (OWNER_TRANSFER history), and the new recruiter can then update it.
  - As hr, `GET /api/consultants/{id}` has no `currentMarketingAssignment` key.
- [ ] T081 [P] [US4] Write `frontend/src/test/consultantDetailPanels.test.tsx` (extended in US5 and US6). With a mocked `ConsultantDetail`, the Marketing panel renders status, target date, overdue flag, and a link for ADMIN, MANAGER, and RECRUITER. For HR_OPERATIONS, the Marketing panel isn't rendered even if the data were present.

### Implementation for User Story 4

- [ ] T082 [P] [US4] Create `backend/src/main/java/com/ensar/clmp/history/domain/TriggerEvent.java` (`MARKETING_ACTIVATED, MARKETING_REOPENED, MARKETING_CLOSED, SUBMISSION_INTERVIEW_SCHEDULED, SUBMISSION_LEFT_INTERVIEW_STAGES, PLACEMENT_CREATED, CONSULTANT_HOLD, CONSULTANT_INACTIVE`).
- [ ] T083 [US4] Add system-triggered history support:
  - Add `system_triggered` boolean, `trigger_event` nullable, `trigger_entity_type` nullable, and `trigger_entity_id` nullable to `backend/src/main/java/com/ensar/clmp/history/domain/HistoryRecord.java`.
  - Add a `TriggerRef(event, entityType, entityId)` record and `TriggerRef` overloads of each status/close method in `history/service/HistoryService.java` that set `system_triggered=true` (FR-036).
  - Add `systemTriggered` and `trigger{event,entityType,entityId}` to `history/web/HistoryEntry.java`.
  - Make `HistoryQueryService` render a generic description per `TriggerEvent`, for example "Marketing activated".
  - Show the "system" marker in `frontend/src/components/HistoryList.tsx`.
- [ ] T084 [P] [US4] Create `backend/src/main/java/com/ensar/clmp/marketing/domain/MarketingStatus.java` (`DRAFT, ACTIVE, HOLD, CLOSED`) and `marketing/domain/MarketingAssignment.java`, `@Entity marketing_assignment`:
  - `consultant_id` FK required
  - `owner_recruiter_id` FK, defaulting to the consultant's current recruiter
  - `owner_team_id` FK, derived from the owner recruiter
  - `start_date` LocalDate required
  - `target_date` LocalDate required, ≥ start_date
  - `status`, initial DRAFT
  - `hold_reason` String(500)
  - `close_reason` String(500), with system values "Placed" and "Consultant inactive"
  - `closed_at` Instant
  - `@Version version`, `created_at`, `created_by_user_id`

  Add a derived `isOverdue(LocalDate today)`. Also create
  `marketing/domain/MarketingAssignmentRepository.java` (`JpaSpecificationExecutor`,
  `findFirstByConsultantIdAndStatusIn(consultantId, {DRAFT, ACTIVE, HOLD})`,
  `existsByConsultantIdAndStatus`).
- [ ] T085 [P] [US4] Create `backend/src/main/java/com/ensar/clmp/marketing/domain/MarketingNote.java` (`@Entity marketing_note`: `marketing_assignment_id` FK required, `body` String(2000) required and non-blank, `author_user_id`, `created_at` set by the server; no setters) and `marketing/domain/MarketingNoteRepository.java` (extends `Repository`, with only `save` and `findByMarketingAssignmentIdOrderByCreatedAtAsc`; append-only per FR-045).
- [ ] T086 [P] [US4] Create these in `backend/src/main/java/com/ensar/clmp/marketing/service/`:
  - `MarketingTransitions.java`: the transition table and `allowedFor(assignment, actorRole, isOwningRecruiter)`.
  - `MarketingAccessPolicy.java`: view allowed for ADMIN, MANAGER, and the owning RECRUITER, where owning means the consultant's current recruiter is the caller. Create, edit, and notes allowed for ADMIN and the owning RECRUITER.
  - `MarketingFilter.java` + `MarketingSpecifications.java`: `statuses, recruiterId, teamId, overdue`, with recruiter scope by the consultant's current recruiter.
- [ ] T087 [US4] Extend `backend/src/main/java/com/ensar/clmp/lifecycle/ConsultantLifecycleService.java`:
  - Add `onMarketingActivatedOrReopened(assignment, event, actor)`: a READY consultant becomes MARKETING. No change if already MARKETING or INTERVIEWING.
  - Add `onMarketingClosed(assignment, actor)`: a MARKETING consultant becomes READY. No change if INTERVIEWING.
  - Implement `cascadeOnHold`: an ACTIVE assignment becomes HOLD with a system-triggered `CONSULTANT_HOLD` history row.
  - Implement `cascadeOnInactive`: an open assignment becomes CLOSED with reason "Consultant inactive" and a `CONSULTANT_INACTIVE` row.
  - Every automatic change writes a history row with `TriggerRef` (FR-032, FR-036).
- [ ] T088 [US4] Create `backend/src/main/java/com/ensar/clmp/marketing/service/MarketingService.java`:
  - `create(request, actor)`: lock the consultant (`findByIdForUpdate`). The consultant status MUST be READY, else 422 `CONSULTANT_NOT_ELIGIBLE` with `consultantStatus` (FR-040). FR-042 is checked, else 409 `OPEN_ASSIGNMENT_EXISTS` with `existingRecordId`. Add both codes to `ErrorCode`. The owner defaults to the current recruiter and the team is derived. `target ≥ start` (400 on `targetDate`). Writes a CREATED history row.
  - `updateDates`
  - `transition(id, target, reason, version, actor)`, under the consultant lock:
    - DRAFT→ACTIVE requires the consultant to be READY.
    - HOLD→ACTIVE requires READY, MARKETING, or INTERVIEWING.
    - CLOSED→ACTIVE requires READY, close_reason ≠ "Placed", and FR-042.
    - Then it calls the lifecycle hooks.
  - `addNote`, `getDetail` (with `allowedTransitions` and notes), and `list`.
- [ ] T089 [US4] Fill in `ConsultantAssignmentService.transferOpenMarketingOwnership` in `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantAssignmentService.java`. It sets the open assignment's `owner_recruiter_id` and `owner_team_id` to the new recruiter and writes an OWNER_TRANSFER history row in the same transaction.
- [ ] T090 [US4] Create the marketing DTOs in `backend/src/main/java/com/ensar/clmp/marketing/web/`: `MarketingCreateRequest.java` (`consultantId` `@NotNull`, `ownerRecruiterId`, `startDate` `@NotNull`, `targetDate` `@NotNull`), `MarketingDatesRequest.java`, `MarketingTransitionRequest.java` (`targetStatus` `@NotNull`, `reason`, `version` `@NotNull`), `NoteRequest.java` (`body` `@NotBlank @Size(max=2000)`), `NoteResponse.java`, `MarketingListItem.java`, and `MarketingDetail.java`. Then create `marketing/web/MarketingController.java` with every `/api/marketing-assignments` endpoint and the `@PreAuthorize` rules from the matrix (create, PUT, and notes exclude MANAGER; transition allows MANAGER, with the service enforcing which transitions). Sort allowlist: `targetDate, startDate, status`. Add `marketingStatuses` to `ReferenceController`.
- [ ] T091 [US4] Fill `currentMarketingAssignment` (`id, status, targetDate, overdue`) in `ConsultantDetail` for non-HR viewers in `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantService.java`, and extend `DemoDataSeeder.seedMarketing()` with Active, Hold, and Closed assignments, including one overdue.
- [ ] T092 [P] [US4] Create `frontend/src/api/marketing.ts`, covering all marketing endpoints and types (appended to `types.ts`), and add `MarketingStatus` labels to `frontend/src/labels.ts`.
- [ ] T093 [US4] Create `frontend/src/pages/marketing/MarketingListPage.tsx`, with status, recruiter, and team filters. Columns: consultant, owner recruiter, team, start date, target date, `StatusBadge`, and an Overdue flag.
- [ ] T094 [US4] Create `frontend/src/pages/marketing/MarketingFormPage.tsx` (`/marketing/new?consultantId=`, with start and target dates and inline errors; on 409 `OPEN_ASSIGNMENT_EXISTS` it shows a link to `/marketing/{existingRecordId}`; 422 `CONSULTANT_NOT_ELIGIBLE` shows "Consultant must be Ready").
- [ ] T095 [US4] Create `frontend/src/pages/marketing/MarketingDetailPage.tsx`. Transition buttons come from `allowedTransitions`, and HOLD and CLOSED use a `ReasonDialog`. Editing dates and the append-only notes list with an add form are hidden unless `canCreateMarketing`. It also shows `HistoryList` and the hold and close reasons.
- [ ] T096 [US4] Render the **Marketing panel** in `frontend/src/pages/consultants/ConsultantDetailPage.tsx`. It shows `currentMarketingAssignment` (status badge, target date, Overdue flag, link to `/marketing/{id}`), or "No open marketing assignment", plus a "Start marketing" action when `canCreateMarketing`, the consultant is READY, and there is no open assignment. The panel is not rendered for HR_OPERATIONS (`canSeeCommercialDetails` false).
- [ ] T097 [US4] Run `./mvnw verify` and `npm test -- --run && npm run build`, and walk through quickstart V4.

**Checkpoint**: The Ready → Marketing → Ready loop works with automatic consultant transitions.

---

## Phase 7: User Story 5 — Submit Consultants and Track Progress (Priority: P2)

**Goal**: A recruiter submits an eligible consultant to a vendor or client job, advances it
through the FR-058 state machine with a full timeline and append-only notes, gets duplicate
warnings, and sees Interviewing applied automatically. Submissions support **create, status
progression, and notes only**. There is no general edit (research R19-7).

**Independent Test**: As recruiter1, create a submission for a Marketing consultant and move it
through Submitted → Under Review → Interview Scheduled (the consultant becomes Interviewing) →
Interview Cleared → Offer. Add notes and view the timeline. A second identical submission
triggers the duplicate warning, and the consultant detail page shows the submissions panel
(quickstart V5).

**Depends on**: US2, US3. US4 is needed for the Marketing/Interviewing interplay (AS 5.6).
**First consumer of**: trigger labels that carry commercial detail, and HR-safe history
redaction.

### Tests for User Story 5

- [ ] T098 [P] [US5] Write `backend/src/test/java/com/ensar/clmp/submission/SubmissionTransitionsTest.java`, a unit test covering every FR-058 edge. It checks that REJECTED, WITHDRAWN, and PLACED are terminal (FR-057), that a manual →PLACED is always refused, and the groupings ACTIVE, INTERVIEW_STAGE, INTERVIEW_OR_OFFER, and OPEN.
- [ ] T099 [P] [US5] Write `backend/src/test/java/com/ensar/clmp/reference/NameNormalizerTest.java`. It checks that `"  Acme   Staffing "` and `"acme staffing"` normalize equal, and the same for job titles (FR-055, edge case).
- [ ] T100 [P] [US5] Write `backend/src/test/java/com/ensar/clmp/it/US5SubmissionIT.java`:
  - AS 5.1: create with `submitNow=false` gives DRAFT with no date. With `submitNow=true` it gives SUBMITTED with today's date (fixed clock).
  - AS 5.2: BENCH, HOLD, INACTIVE, PLACED, or ACTIVE_PROJECT returns 422 `CONSULTANT_NOT_ELIGIBLE`.
  - AS 5.3: a duplicate with case and whitespace variants returns 409 `DUPLICATE_SUBMISSION` with `duplicates[]` (including WITHDRAWN and REJECTED originals). With `acknowledgeDuplicate=true` it returns 201, and the detail shows `duplicateAcknowledgement` with the user, time, and earlier ids.
  - AS 5.4: invalid transitions return 422 with `allowedTransitions`, and each change appears in the timeline with old, new, user, time, and note.
  - AS 5.5: → INTERVIEW_SCHEDULED makes the consultant INTERVIEWING with a system-triggered history row whose admin-visible description names the client and job title.
  - AS 5.6: rejecting the last interview/offer-stage submission returns the consultant to MARKETING when an ACTIVE assignment exists, else READY.
  - AS 5.7: notes are append-only.
  - AS 5.8: filters for status, recruiter, vendor, client, and the submitted date range.
  - AS 5.9: the manager can view but gets 403 on create, status, and notes.
  - FR-063: recruiter1 can view and update a submission with recruiter2 as recruiter when the consultant is now assigned to recruiter1. recruiter2 still sees it as the submission recruiter.
  - `canCreatePlacement` is true only for ADMIN and the submission's recruiter at OFFER, never for the consultant's current recruiter alone (FR-070).
  - `PUT /api/submissions/{id}` is not mapped (405).
  - FR-053: bill rate ≤ 0 returns 400.
  - FR-054: a future submitted date returns 400.
  - FR-051: vendor and client find-or-create, normalized.
  - Edge cases: setting a consultant with open submissions to INACTIVE returns 422 `OPEN_SUBMISSIONS_EXIST`. A consultant on HOLD keeps existing submissions changeable, but new ones return 422 `CONSULTANT_NOT_ELIGIBLE`.
  - FR-064: as hr, `GET /api/consultants/{id}` has no `submissions` or `openSubmissionsWhileOnHold` keys, and the consultant history description for the Interviewing change reads "Submission moved to Interview Scheduled" with null trigger ids and no client name.
- [ ] T101 [P] [US5] Write `frontend/src/test/submissionDuplicate.test.tsx`. With a mocked client, the first POST rejects with 409 `DUPLICATE_SUBMISSION`. Assert that the warning dialog lists the duplicates, that Cancel sends nothing more, and that Confirm re-POSTs with `acknowledgeDuplicate: true`.
- [ ] T102 [P] [US5] Extend `frontend/src/test/consultantDetailPanels.test.tsx`:
  - The Submissions panel lists vendor, client, job title, status, and submitted date with links for ADMIN, MANAGER, and RECRUITER.
  - When the consultant is HOLD and `openSubmissionsWhileOnHold` is non-empty, a warning banner lists those submissions.
  - For HR_OPERATIONS, neither renders.

### Implementation for User Story 5

- [ ] T103 [P] [US5] Create `backend/src/main/java/com/ensar/clmp/reference/service/NameNormalizer.java` (lower, trim, collapse internal whitespace), `reference/domain/Vendor.java`, and `reference/domain/Client.java`, both `@Entity` (`vendor`/`client`) with `name` String(120) required and trimmed, `normalized_name` String(120) unique, `created_at`, and `created_by_user_id`. Also create `VendorRepository.java` and `ClientRepository.java` (`findByNormalizedName`, `findTop20ByNormalizedNameContainingOrderByName`).
- [ ] T104 [US5] Create `backend/src/main/java/com/ensar/clmp/reference/service/CounterpartyService.java` (`resolveVendor(Long id, String name, actor)` and `resolveClient(...)`). Exactly one of id or name must be given, else 400. A name does find-or-create on its normalized form. It also has `searchVendors(q)` and `searchClients(q)`. Then create `reference/web/CounterpartyController.java` with `GET /api/vendors?q=` and `GET /api/clients?q=` (ADMIN, MANAGER, RECRUITER), returning `[{id,name}]`, at most 20.
- [ ] T105 [P] [US5] Create `backend/src/main/java/com/ensar/clmp/submission/domain/SubmissionStatus.java` (`DRAFT, SUBMITTED, UNDER_REVIEW, INTERVIEW_SCHEDULED, INTERVIEW_CLEARED, REJECTED, OFFER, PLACED, WITHDRAWN`, with static sets `ACTIVE`, `INTERVIEW_STAGE`, `INTERVIEW_OR_OFFER`, `OPEN`, and `TERMINAL` per data-model.md) and `submission/domain/Submission.java`, `@Entity submission`:
  - `consultant_id` FK required
  - `recruiter_id` FK required
  - `vendor_id` FK required, `client_id` FK required
  - `job_title` String(120) required and trimmed
  - `job_title_normalized` String(120)
  - `submitted_date` LocalDate, null while DRAFT and required once ≥ SUBMITTED
  - `bill_rate` `BigDecimal(10,2)` required, > 0
  - `status`
  - `duplicate_acknowledged` boolean, `duplicate_acknowledged_by_user_id`, `duplicate_acknowledged_at`
  - `@Version version`, `created_at`, `created_by_user_id`

  Indexes on `status`, `recruiter_id`, `consultant_id`, `submitted_date`, and
  `(consultant_id, vendor_id, client_id, job_title_normalized)`.
- [ ] T106 [P] [US5] Create `backend/src/main/java/com/ensar/clmp/submission/domain/SubmissionNote.java` (`@Entity submission_note`: `submission_id` FK, `body` String(2000) required and non-blank, `author_user_id`, `created_at`; append-only) and `SubmissionDuplicateRef.java` (`@Entity submission_duplicate_ref`: `submission_id`, `earlier_submission_id`). Also create the repositories: `SubmissionRepository.java` (`JpaSpecificationExecutor`, `findDuplicates(consultantId, vendorId, clientId, jobTitleNormalized)`, `existsByConsultantIdAndStatusIn`, `findByConsultantIdAndStatusIn`), `SubmissionNoteRepository.java` (extends `Repository`, save and find only), and `SubmissionDuplicateRefRepository.java`.
- [ ] T107 [P] [US5] Create these in `backend/src/main/java/com/ensar/clmp/submission/service/`:
  - `SubmissionTransitions.java`: the FR-058 map, with →PLACED excluded from manual changes.
  - `SubmissionAccessPolicy.java`:
    - View and update allowed for ADMIN, and for a RECRUITER where `submission.recruiter_id = me` OR the consultant's current recruiter = me. MANAGER can view only. HR is refused (FR-063, FR-064).
    - `canCreatePlacementFrom(submission, actor)`: ADMIN, or a RECRUITER where `submission.recruiter_id = me` (FR-070).
  - `SubmissionFilter.java` + `SubmissionSpecifications.java`: `statuses, recruiterId, vendorId, clientId, consultantId, submittedFrom, submittedTo`, plus `scopeFor(actor)` using the FR-063 rule.
- [ ] T108 [US5] Extend `backend/src/main/java/com/ensar/clmp/lifecycle/ConsultantLifecycleService.java`:
  - Add `onSubmissionStatusChanged(submission, oldStatus, newStatus, actor)`. On →INTERVIEW_SCHEDULED, a READY or MARKETING consultant becomes INTERVIEWING (`SUBMISSION_INTERVIEW_SCHEDULED`). Otherwise, if the consultant is INTERVIEWING and no submission remains in INTERVIEW_OR_OFFER, the consultant becomes MARKETING if an ACTIVE marketing assignment exists, else READY (`SUBMISSION_LEFT_INTERVIEW_STAGES`). A HOLD consultant is never changed by submission status changes.
  - Implement `guardInactive`: if any submission for the consultant is OPEN, throw 422 `OPEN_SUBMISSIONS_EXIST` with `openSubmissionIds` (add to `ErrorCode`).
- [ ] T109 [US5] Create `backend/src/main/java/com/ensar/clmp/submission/service/SubmissionService.java`. It has **no update-details method**.
  - `create(request, actor)`:
    - The consultant must be READY, MARKETING, or INTERVIEWING, else 422 `CONSULTANT_NOT_ELIGIBLE` with `consultantStatus` (FR-052).
    - The recruiter defaults to the consultant's current recruiter.
    - Vendor and client are resolved through `CounterpartyService`.
    - The duplicate lookup (FR-055) throws 409 `DUPLICATE_SUBMISSION` (add to `ErrorCode`) with `duplicates[{id,status,submittedDate,createdAt,recruiterName}]` unless `acknowledgeDuplicate`. When acknowledged, it stores the ack fields and `SubmissionDuplicateRef` rows.
    - With `submitNow`, the status is SUBMITTED and `submittedDate` defaults to today and must be ≤ today (FR-054).
    - It writes CREATED and STATUS history (`owner_recruiter_id` = submission recruiter, `consultant_id`) and an optional first note.
  - `changeStatus(id, target, note, submittedDate, version, actor)`: validate the transition, set `submittedDate` on →SUBMITTED, write a STATUS history row with the note, then call `onSubmissionStatusChanged`.
  - `addNote`
  - `getDetail`: timeline from `HistoryQueryService`, notes, `allowedTransitions`, and `canCreatePlacement` = status OFFER AND `SubmissionAccessPolicy.canCreatePlacementFrom`.
  - `list`
- [ ] T110 [US5] Add viewer-aware trigger labels:
  - Create `backend/src/main/java/com/ensar/clmp/history/service/TriggerLabelResolver.java`, an interface with `supports(HistoryEntityType)` and `Map<Long,String> labels(Set<Long> ids)`.
  - Create `submission/service/SubmissionTriggerLabelResolver.java`, implementing it for SUBMISSION and returning "Submission for {client} / {jobTitle}" in one batch query.
  - Create `history/service/HistoryDescriptionRenderer.java` and use it from `HistoryQueryService`. Non-HR viewers get labelled descriptions, for example "Submission for Acme / Java Developer moved to Interview Scheduled". HR_OPERATIONS viewers get generic descriptions ("Submission moved to Interview Scheduled") with `trigger.entityType`/`entityId` null (research R8, FR-064, FR-082).
- [ ] T111 [US5] Create the submission DTOs in `backend/src/main/java/com/ensar/clmp/submission/web/`:
  - `SubmissionCreateRequest.java`: `consultantId` `@NotNull`, `recruiterId`, `vendorId`/`vendorName` `@Size(max=120)`, `clientId`/`clientName` `@Size(max=120)`, `jobTitle` `@NotBlank @Size(max=120)`, `billRate` `@NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=8, fraction=2)`, `submitNow`, `submittedDate`, `note` `@Size(max=2000)`, `acknowledgeDuplicate`.
  - `SubmissionStatusRequest.java`
  - `SubmissionListItem.java`
  - `SubmissionDetail.java`
  - `DuplicateSummary.java`

  Then create `submission/web/SubmissionController.java` with exactly these endpoints:
  `GET /api/submissions`, `GET /{id}`, `POST`, `POST /{id}/status`, and `POST /{id}/notes`. There
  is **no PUT**. GET allows ADMIN, MANAGER, and RECRUITER. Mutations allow ADMIN and RECRUITER.
  Sort allowlist: `submittedDate, status, billRate`. Add `submissionStatuses` to
  `ReferenceController`.
- [ ] T112 [US5] Fill `submissions` and `openSubmissionsWhileOnHold` (when the consultant is HOLD) in `ConsultantDetail` for non-HR viewers, scoped by `SubmissionAccessPolicy`, in `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantService.java`. Extend `DemoDataSeeder.seedSubmissions()` with:
  - Vendors, clients, and submissions across all statuses for recruiter1 and recruiter2.
  - One submission at OFFER for US6.
  - One OFFER submission for a consultant on HOLD, for the US6 Hold → Placed demo.
- [ ] T113 [P] [US5] Create `frontend/src/api/submissions.ts` and `frontend/src/api/counterparties.ts` (vendor and client search), with types appended to `types.ts` and `SubmissionStatus` labels added to `labels.ts`.
- [ ] T114 [P] [US5] Create `frontend/src/components/CounterpartyPicker.tsx`, a typeahead over `/api/vendors` or `/api/clients` with an "Add '{typed}'" option that yields `{name}` instead of `{id}`.
- [ ] T115 [US5] Create `frontend/src/pages/submissions/SubmissionListPage.tsx`, with status (multi), recruiter, vendor, client, and submitted date range filters. Columns: consultant, recruiter, vendor, client, job title, submitted date, bill rate (USD/hr), and `StatusBadge`.
- [ ] T116 [US5] Create `frontend/src/pages/submissions/SubmissionFormPage.tsx` (`/submissions/new?consultantId=`), with `CounterpartyPicker` for vendor and client, job title, bill rate, a "Submit now" checkbox with submitted date, and an optional note. On 409 `DUPLICATE_SUBMISSION` it opens a `ConfirmDialog` listing the earlier submissions (status and date); Confirm re-POSTs with `acknowledgeDuplicate: true`.
- [ ] T117 [US5] Create `frontend/src/pages/submissions/SubmissionDetailPage.tsx`. It shows all fields (read-only, with no edit form), the duplicate acknowledgement, and a timeline (`HistoryList`). It has the append-only notes list and add form, plus a status change control limited to `allowedTransitions`, with an optional note and a submitted date when moving to SUBMITTED. These are hidden for MANAGER (read-only).
- [ ] T118 [US5] Render the **Submissions panel** and the **on-Hold open-submissions banner** in `frontend/src/pages/consultants/ConsultantDetailPage.tsx`.
  - The panel is a table of the consultant's visible `submissions` (vendor, client, job title, status, submitted date, linking to `/submissions/{id}`), with a "New submission" action when `canCreateSubmission` and the consultant is READY, MARKETING, or INTERVIEWING.
  - When the consultant is HOLD and `openSubmissionsWhileOnHold` is non-empty, a warning banner reads "Consultant is on Hold with N open submissions" and lists them.
  - Neither is rendered for HR_OPERATIONS.
- [ ] T119 [US5] Run `./mvnw verify` and `npm test -- --run && npm run build`, and walk through quickstart V5.

**Checkpoint**: Submissions and interview tracking work with automatic Interviewing transitions.

---

## Phase 8: User Story 6 — Convert an Offer into a Placement (Priority: P3)

**Goal**: ADMIN, or the RECRUITER who is the submission's recruiter, converts an OFFER submission
into a placement in one atomic action. The submission, consultant (including from HOLD), and
open marketing assignment all change, each recorded with its trigger. The placement's recruiter
is the submission's recruiter. ADMIN can edit placements, and start dates are validated against
the submitted date. ADMIN or HR can mark the consultant Active Project. HR never sees the
Placements workspace or commercial details.

**Independent Test**: Convert an OFFER submission. Check the carried-over fields and that the
submission, consultant, and marketing assignment all changed. Then mark the consultant Active
Project. The consultant detail page shows the placements panel. As hr, see only the status
(quickstart V6).

**Depends on**: US5, and US4 for the marketing close.

### Tests for User Story 6

- [ ] T120 [P] [US6] Write `backend/src/test/java/com/ensar/clmp/it/US6PlacementIT.java`:
  - AS 6.1: `GET /api/placements/draft?submissionId=` is pre-filled; a non-OFFER submission returns 422 `SUBMISSION_NOT_AT_OFFER`.
  - AS 6.2: POST returns 201. The submission is PLACED, the consultant PLACED, and marketing CLOSED with reason "Placed", each with a system-triggered history row whose trigger is `PLACEMENT_CREATED` and this placement id.
  - AS 6.3: a non-OFFER submission returns 422 `SUBMISSION_NOT_AT_OFFER`.
  - AS 6.4: `otherOpenSubmissions` is listed and the others are unchanged.
  - AS 6.5: PLACED→ACTIVE_PROJECT before the start date returns 422 `BUSINESS_RULE`. On or after the start date (fixed clock) it returns 200 for hr.
  - AS 6.6: recruiter2 doesn't see recruiter1's placement.
  - AS 6.7: hr `GET /api/placements` returns 403. The hr consultant detail has no `placements`, `submissions`, `vendor`, `client`, or `billRate` keys anywhere, and the history description reads "Placement created" with null trigger ids.
  - **Ownership (FR-070):**
    - recruiter1, as the submission's recruiter, creates the placement, and `placement.recruiter` = recruiter1.
    - A RECRUITER who is only the consultant's current recruiter (not the submission's) gets 403 on `/draft` and `POST`.
    - ADMIN can place any OFFER submission, and the placement recruiter is still the submission's recruiter.
    - The creating recruiter can `GET /api/placements/{id}`.
    - The manager can view all placements, but POST and PATCH return 403 (FR-017).
  - **Hold → Placed:** a HOLD consultant with an existing OFFER submission is placed. The consultant becomes PLACED with a system-triggered row from HOLD, and the HOLD marketing assignment is CLOSED ("Placed"). A new submission for another HOLD consultant still returns 422.
  - FR-075: a second placement for a PLACED or ACTIVE_PROJECT consultant returns 422 `ALREADY_PLACED`.
  - A create start date before the submitted date returns 400.
  - **FR-074 edit:** admin PATCH writes a FIELD_EDIT history row per field. A PATCH `startDate` earlier than the source submission's `submittedDate` returns 400 `VALIDATION_FAILED` with `fieldErrors[startDate]` and changes nothing. Recruiter PATCH returns 403.
  - `contractTermMonths` 0 or 61 returns 400.
  - `expectedEndDate` = start + term.
  - Atomicity: force a failure after the submission update (for example a stale consultant version) and assert nothing changed.
  - Reopening a marketing assignment closed with "Placed" returns 422.
- [ ] T121 [P] [US6] Extend `frontend/src/test/consultantDetailPanels.test.tsx`. The Placements panel shows client, start date, and a link for ADMIN, MANAGER, and RECRUITER. For HR_OPERATIONS with a PLACED consultant, the page shows the status badge and history but renders **no** Marketing, Submissions, or Placements panel and no vendor, client, bill rate, or contract term text.

### Implementation for User Story 6

- [ ] T122 [P] [US6] Create `backend/src/main/java/com/ensar/clmp/placement/domain/Placement.java`, `@Entity placement`:
  - `submission_id` FK required, unique
  - `consultant_id`, `recruiter_id`, `vendor_id`, `client_id` FKs, copied from the submission (`recruiter_id` is always the submission's recruiter)
  - `job_title` String(120)
  - `start_date` LocalDate required, ≥ submission.submitted_date
  - `bill_rate` `BigDecimal(10,2)` required, > 0
  - `contract_term_months` Integer required, whole number 1–60
  - `created_at`, `created_by_user_id`, `@Version version`

  Add a derived `expectedEndDate()` = `startDate.plusMonths(contractTermMonths)`, and indexes on
  `recruiter_id` and `created_at`. Also create `placement/domain/PlacementRepository.java`
  (`JpaSpecificationExecutor`, `findFirstByConsultantIdOrderByCreatedAtDesc`).
- [ ] T123 [P] [US6] Create these in `backend/src/main/java/com/ensar/clmp/placement/service/`:
  - `PlacementAccessPolicy.java`:
    - View: ADMIN, MANAGER, and a RECRUITER where `placement.recruiter_id = me`.
    - Create: delegates to `SubmissionAccessPolicy.canCreatePlacementFrom`, which allows ADMIN, or the RECRUITER who is `submission.recruiter_id`.
    - Edit: ADMIN only.
    - HR is always refused.
  - `PlacementFilter.java` + `PlacementSpecifications.java`: `recruiterId, clientId, vendorId, startFrom, startTo, createdFrom, createdTo`, plus scope.
- [ ] T124 [US6] Extend `backend/src/main/java/com/ensar/clmp/lifecycle/ConsultantLifecycleService.java`:
  - Add `onPlacementCreated(placement, submission, actor)`: the submission goes OFFER→PLACED, the consultant goes from **READY, MARKETING, INTERVIEWING, or HOLD** to PLACED, and an open (DRAFT, ACTIVE, or HOLD) marketing assignment becomes CLOSED with reason "Placed". Each change writes a history row with `TriggerRef(PLACEMENT_CREATED, PLACEMENT, placementId)` (FR-032, FR-072).
  - Add the PLACED→ACTIVE_PROJECT guard to manual transitions: today ≥ the latest placement's `start_date`, else 422 `BUSINESS_RULE`.
- [ ] T125 [US6] Create `backend/src/main/java/com/ensar/clmp/placement/service/PlacementService.java`:
  - `draft(submissionId, actor)`: refused unless the caller may create from this submission and it is OFFER.
  - `create(request, actor)`, as ONE `@Transactional`:
    - Lock the consultant.
    - The caller must pass `PlacementAccessPolicy` create, else 403.
    - The submission must be OFFER, else 422 `SUBMISSION_NOT_AT_OFFER`.
    - The consultant must not be PLACED or ACTIVE_PROJECT, else 422 `ALREADY_PLACED`. HOLD is allowed.
    - `startDate ≥ submittedDate`.
    - `recruiter_id` = `submission.recruiter_id`.
    - Copy the other fields, save, write a CREATED history row, and call `onPlacementCreated`.
    - Return `otherOpenSubmissions` (the consultant's OPEN submissions other than the source). None are changed (FR-076).
    - Add `SUBMISSION_NOT_AT_OFFER`, `ALREADY_PLACED`, and `BUSINESS_RULE` to `ErrorCode`.
  - `edit(id, patch, version, actor)`, ADMIN only:
    - An edited `startDate` MUST be ≥ the source submission's `submittedDate`, else 400 `VALIDATION_FAILED` with `fieldErrors[startDate]` (FR-074).
    - `billRate` > 0, and `contractTermMonths` 1–60.
    - One FIELD_EDIT history row per changed field.
  - `getDetail` (with history) and `list`.
- [ ] T126 [US6] Create `backend/src/main/java/com/ensar/clmp/placement/service/PlacementTriggerLabelResolver.java` ("Placement at {client} via {vendor}"; HR gets the generic "Placement created" through `HistoryDescriptionRenderer`).
  - DTOs in `placement/web/`:
    - `PlacementCreateRequest.java`: `submissionId` `@NotNull`, `startDate` `@NotNull`, `billRate` `@NotNull @DecimalMin(value="0", inclusive=false) @Digits(integer=8, fraction=2)`, `contractTermMonths` `@NotNull @Min(1) @Max(60)`.
    - `PlacementPatchRequest.java`
    - `PlacementDraft.java`
    - `PlacementListItem.java`
    - `PlacementDetail.java`
    - `PlacementCreated.java`
  - `placement/web/PlacementController.java`:
    - `GET /api/placements` and `GET /{id}`: ADMIN, MANAGER, RECRUITER.
    - `GET /draft` and `POST`: ADMIN, RECRUITER.
    - `PATCH /{id}`: ADMIN.
    - Sort allowlist: `startDate, createdAt`.
- [ ] T127 [US6] Fill `placements` in `ConsultantDetail` for non-HR viewers, scoped by `PlacementAccessPolicy`, in `backend/src/main/java/com/ensar/clmp/consultant/service/ConsultantService.java`. Extend `DemoDataSeeder.seedPlacements()` with one placement in the current month and one older placement whose consultant is ACTIVE_PROJECT.
- [ ] T128 [P] [US6] Create `frontend/src/api/placements.ts`, with types appended to `types.ts`.
- [ ] T129 [US6] Create `frontend/src/pages/placements/PlacementListPage.tsx`, with recruiter, client, vendor, and start date range filters (and accepting `createdFrom`/`createdTo` from dashboard links). Columns: consultant, recruiter, client, vendor, start date, bill rate, term, and expected end date.
- [ ] T130 [US6] Create `frontend/src/pages/placements/PlacementFormPage.tsx` (`/placements/new?submissionId=`). It loads `/draft` and shows the pre-filled read-only consultant, recruiter, vendor, client, and job title, an editable bill rate, a required start date, and a term of 1–60 with the expected end date preview. After a 201 it shows an "Other open submissions" panel with a Withdraw button per row (`POST /api/submissions/{id}/status` WITHDRAWN). Also add a "Create Placement" button to `frontend/src/pages/submissions/SubmissionDetailPage.tsx` shown only when the API returns `canCreatePlacement: true`.
- [ ] T131 [US6] Create `frontend/src/pages/placements/PlacementDetailPage.tsx`, showing the fields, history, and for ADMIN only an edit form for start date, bill rate, and term with `version`. The start-date `fieldErrors` from the API render inline.
- [ ] T132 [US6] Render the **Placements panel** in `frontend/src/pages/consultants/ConsultantDetailPage.tsx`, listing the visible `placements` (client, start date, expected end date, linking to `/placements/{id}`). It is not rendered for HR_OPERATIONS. HR sees only the consultant's Placed or Active Project status and history (FR-064, AS 6.7).
- [ ] T133 [US6] Run `./mvnw verify` and `npm test -- --run && npm run build`, and walk through quickstart V6.

**Checkpoint**: The full consultant-to-placement lifecycle (SC-001) can be demonstrated.

---

## Phase 9: User Story 7 — Operational Dashboard (Priority: P3)

**Goal**: A role-shaped dashboard with linked counts, the recruiter performance summary
(ADMIN/MANAGER), and the 20 most recent activity events within scope.

**Independent Test**: With seeded data, sign in as each role. Each dashboard figure should equal
the total of the list it links to (quickstart V7).

**Depends on**: US2–US6. **First consumer of**: the scoped recent-activity feed.

### Tests for User Story 7

- [ ] T134 [P] [US7] Write `backend/src/test/java/com/ensar/clmp/it/US7DashboardIT.java`:
  - AS 7.1: admin and manager get `scope=ORGANIZATION`, with all six counts including NEEDS_REASSIGNMENT, `recruiterPerformance`, and ≤ 20 `recentActivity`. Manager figures equal admin figures (organization-wide, FR-016).
  - AS 7.2: recruiter1 gets `scope=OWN`, with no `recruiterPerformance`, and activity limited to their consultants and submissions.
  - AS 7.3: hr gets `consultantsByStatus` and consultant-only activity, and the body contains no `billRate`, `vendor`, `client`, or `recruiterPerformance` keys.
  - recruiter3 (unlinked) gets all zeros and the contact-admin `message`.
- [ ] T135 [P] [US7] Write `backend/src/test/java/com/ensar/clmp/consistency/DashboardListConsistencyIT.java`. For each of admin, manager, recruiter1, and hr, and for every count, it calls the list endpoint named in `link.list` with `link.query` and asserts `totalItems == value` (AS 7.4, SC-006, FR-083).

### Implementation for User Story 7

- [ ] T136 [US7] Add `recentActivity(viewer, limit=20)` to `backend/src/main/java/com/ensar/clmp/history/service/HistoryQueryService.java`, rendered through `HistoryDescriptionRenderer`. Activity scope (FR-102):
  - ADMIN and MANAGER see all rows.
  - RECRUITER sees rows where `consultant_id` ∈ consultants currently assigned to me OR `owner_recruiter_id` = me.
  - HR_OPERATIONS sees `entity_type = CONSULTANT` only.
  - A RECRUITER with no `recruiterId` gets an empty result.
- [ ] T137 [US7] Create `backend/src/main/java/com/ensar/clmp/dashboard/service/DashboardService.java`. It computes every count by calling the same filters and specifications plus the caller scope used by the lists (`ConsultantSpecifications`, `SubmissionSpecifications`, `PlacementSpecifications`) with `count` queries (research R13). Placements this month use `createdFrom`/`createdTo` = `OrgTime` current month.
  - `recruiterPerformance`: per ACTIVE recruiter, assigned consultants, active submissions, interviews (INTERVIEW_STAGE), and placements this month, using grouped count queries only (research R14).
  - Role shapes follow `contracts/rest-api.md § Dashboard`, including the unlinked-recruiter `message`.
- [ ] T138 [US7] Create `backend/src/main/java/com/ensar/clmp/dashboard/web/DashboardResponse.java`, `CountTile.java` (`key, label, value, link{list, query}`), `RecruiterPerformanceRow.java`, `StatusCount.java`, and `dashboard/web/DashboardController.java` (`GET /api/dashboard`, all roles).
- [ ] T139 [P] [US7] Create `frontend/src/api/dashboard.ts`, with types appended to `types.ts`.
- [ ] T140 [US7] Replace `frontend/src/pages/dashboard/DashboardPage.tsx` with role-shaped panels:
  - `CountTile` components (in `frontend/src/pages/dashboard/CountTile.tsx`) linking to `/{link.list}?{link.query}`
  - A `RecruiterPerformanceTable` when present
  - A `ConsultantsByStatus` panel for HR
  - An `ActivityFeed` using `HistoryList`
  - The `message` empty state for unlinked recruiters
  - Loading and error states
- [ ] T141 [US7] Run `./mvnw verify` and `npm test -- --run && npm run build`, and walk through quickstart V7.1–V7.3.

**Checkpoint**: The dashboard reflects the workflow per role, and its counts match the lists.

---

## Phase 10: User Story 8 — Operational Reports (Priority: P4)

**Goal**: ADMIN and MANAGER view five on-screen reports with date ranges (defaulting to the
current month) and explicit empty states. Every report figure can be reproduced from a filtered
list.

**Independent Test**: Open each report for a date range and check the totals against the
underlying lists (quickstart V8).

**Depends on**: US5 and US6 (data sources).

### Tests for User Story 8

- [ ] T142 [P] [US8] Write `backend/src/test/java/com/ensar/clmp/it/US8ReportsIT.java`:
  - AS 8.1: Submissions by Recruiter, counted by `submitted_date` in range, with the current-status breakdown.
  - AS 8.2: Placements by Recruiter, by `created_at` date.
  - AS 8.3: Consultant Pipeline returns all 8 statuses, including zeros.
  - AS 8.4: Vendor/Client Activity:
    - `submissions` is counted by `submitted_date` in range.
    - `interviewsScheduled` is those submissions whose **current** status is INTERVIEW_SCHEDULED. A submission that moved on to Interview Cleared isn't counted.
    - `placements` is counted by `created_at` in range.
  - AS 8.5: an empty range gives `empty: true`.
  - Defaults to the current month when `from`/`to` are omitted (FR-091).
  - `from > to` returns 400.
  - Recruiter and hr get 403.
- [ ] T143 [P] [US8] Write `backend/src/test/java/com/ensar/clmp/consistency/ReportListConsistencyIT.java`. For every report cell with a `link`, including Vendor/Client `interviewsScheduled` (`status=INTERVIEW_SCHEDULED&vendorId|clientId=…&submittedFrom=…&submittedTo=…`), the linked list's `totalItems` must equal the cell's count (FR-092, SC-006).

### Implementation for User Story 8

- [ ] T144 [US8] Create `backend/src/main/java/com/ensar/clmp/report/service/ReportService.java` with `submissionsByRecruiter(from, to)`, `placementsByRecruiter(from, to)`, `consultantPipeline()`, `benchReady()`, and `vendorClientActivity(from, to)`.
  - All of them use grouped queries over the same filter definitions as the lists (research R13).
  - Vendor/Client `interviewsScheduled` = submissions with `submitted_date` in range AND current status INTERVIEW_SCHEDULED (research R19-6), not history events.
  - Every cell carries a `link` query string into the matching list, and responses set `empty` when there are no rows.
- [ ] T145 [US8] Create the report DTOs in `backend/src/main/java/com/ensar/clmp/report/web/` (`SubmissionsByRecruiterReport.java`, `PlacementsByRecruiterReport.java`, `ConsultantPipelineReport.java`, `BenchReadyReport.java`, `VendorClientActivityReport.java`, `DateRangeParams.java` with the current-month defaults from `OrgTime` and `from ≤ to` validation) and `report/web/ReportController.java` (all `GET /api/reports/*` endpoints, ADMIN and MANAGER).
- [ ] T146 [P] [US8] Create `frontend/src/api/reports.ts`, with types appended to `types.ts`.
- [ ] T147 [US8] Create `frontend/src/pages/reports/ReportsPage.tsx`, with a report picker (tabs), a from/to date range (default current month) for the three date-ranged reports, and a table per report with drill-down links from `link`. `EmptyState` reads "No data for this period" when `empty` is true.
- [ ] T148 [US8] Run `./mvnw verify` and `npm test -- --run && npm run build`, and walk through quickstart V8.

**Checkpoint**: All eight user stories are complete.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: Success-criteria verification and hardening across stories (plan slice S9).

- [ ] T149 [P] Write `backend/src/test/java/com/ensar/clmp/security/AuthorizationMatrixTest.java`, a JUnit `@ParameterizedTest` over every row of `contracts/authorization-matrix.md § Endpoint × Role` × {admin, manager, recruiter1, hr} (plus recruiter3 for empty scope).
  - Allowed cells don't return 403. Refused cells return 403 `NOT_AUTHORIZED` with no state change, checked through row counts before and after.
  - "Own" cells are covered with an owned and a non-owned record.
  - "Own submission for placement" is covered with a submission where the caller is only the consultant's current recruiter.
  - Assert that `PUT /api/submissions/{id}` is not exposed (SC-002, FR-004).
- [ ] T150 [P] Write `backend/src/test/java/com/ensar/clmp/security/SensitiveFieldExposureTest.java`. It walks every GET endpoint's JSON for each role and asserts:
  - No `password`/`passwordHash` anywhere.
  - Consultant list items never have `email`, `phone`, `visaExpirationDate`, or `notes`.
  - hr responses contain no `vendor`, `client`, `billRate`, or `contractTermMonths` keys, and no history `description` mentions a seeded vendor or client name.
  - History `oldValue`/`newValue` never contain a consultant email or phone (FR-007, FR-024, FR-064, FR-103).
- [ ] T151 [P] Write `backend/src/test/java/com/ensar/clmp/it/ConcurrencyIT.java`:
  - A stale `version` on consultant, recruiter, marketing, submission status, and placement updates returns 409 `CONCURRENT_MODIFICATION`.
  - Two concurrent `POST /api/marketing-assignments` for the same consultant (two threads, `CountDownLatch`) produce exactly one 201 and one 409 `OPEN_ASSIGNMENT_EXISTS`.
  - Two concurrent placements for the same consultant produce exactly one 201 (research R10).
- [ ] T152 Create `backend/src/main/java/com/ensar/clmp/seed/PerfDataSeeder.java` (`@Profile("perf")`, runs after `DemoDataSeeder`), seeding 50 recruiters, 500 consultants, and 2,000 submissions across statuses with realistic dates. Also create `backend/src/test/java/com/ensar/clmp/perf/PerformanceSmokeIT.java`, tagged `@Tag("perf")` and excluded from default `verify` through surefire `excludedGroups`. It asserts that the consultant, submission, and placement list endpoints, `/api/dashboard`, and each report respond in < 2 s as admin (SC-007). Add the surefire configuration to `backend/pom.xml`.
- [ ] T153 Review logging across `backend/src/main/java/com/ensar/clmp/**` and `backend/src/main/resources/application*.yml`. Confirm that no log statement prints request bodies, passwords, consultant contact fields, or bill rates (ids only), and that Hibernate SQL parameter logging is off. Fix any finding (constitution VII).
- [ ] T154 [P] Create the root `README.md`: the project purpose, a pointer to `specs/001-clmp-mvp/quickstart.md` for build and run, the demo users, and a note that demo passwords are dev-only.
- [ ] T155 Run the full quality gate: `./mvnw verify` in `backend/` and `npm run typecheck && npm test -- --run && npm run build` in `frontend/`. Optionally run `./mvnw verify -Dgroups=perf` with the perf profile. Then execute quickstart V1–V10 and record the results, including the SC-001 timing and any deviations, in the change description (constitution Quality Gates, VIII).

---

## Dependencies & Execution Order

### Phase dependencies

- **Setup (Phase 1)**: none. T001 (toolchain check) gates everything.
- **Foundational (Phase 2)**: depends on Setup. It contains only what US1 consumes and blocks US1.
- **User stories (Phases 3–10)**: in spec priority order, as below.
- **Polish (Phase 11)**: depends on all user stories.

### User story dependencies

```text
Foundational
   └─ US1 (sign-in, shell)  ← MVP
        └─ US2 (consultants, readiness, lifecycle core, history, shared list UI)
             └─ US3 (recruiters, assignment)
                  └─ US4 (marketing, system-triggered history)
                       └─ US5 (submissions, HR-safe trigger labels)
                            └─ US6 (placements)
                                 ├─ US7 (dashboard, activity feed)
                                 └─ US8 (reports)   ← US7 and US8 can run in parallel
```

- US1: no story dependencies.
- US2: needs the US1 shell (routes, auth). Uses seeded recruiter assignments so it can be tested before US3.
- US3: needs the US2 consultant entity and detail page.
- US4: needs US2 and US3 (ownership through the current recruiter, and owner transfer on reassignment).
- US5: needs US2 and US3. US4 is needed for AS 5.6 (return to Marketing).
- US6: needs US5 (OFFER submissions and `canCreatePlacementFrom`) and US4 (marketing close).
- US7 and US8: read-only over US2–US6 data. They are independent of each other.

### Within each story

- Write the tests first and see them fail.
- Then build the shared pieces this story first consumes → entities/enums → repositories → policies/filters → lifecycle extensions → services → DTOs/controllers → seed → frontend API → pages.
- Finish with the story's build and quickstart walkthrough task.

### Shared-file touch points (avoid parallel edits)

- `lifecycle/ConsultantLifecycleService.java`: T057 → T087 → T108 → T124
- `history/domain/HistoryRecord.java`, `history/service/HistoryService.java`, `history/web/HistoryEntry.java`: T045–T047 → T083
- `history/service/HistoryQueryService.java`: T047 → T083 → T110 → T136
- `consultant/service/ConsultantService.java`: T059 → T091 → T112 → T127
- `consultant/service/ConsultantAssignmentService.java`: T071 → T089
- `seed/DemoDataSeeder.java`: T017 → T061 → T091 → T112 → T127
- `reference/web/ReferenceController.java`: T048 → T090 → T111
- `common/error/ErrorCode.java`: T010 → T042 → T057 → T069/T071 → T088 → T108/T109 → T125
- `pages/consultants/ConsultantDetailPage.tsx`: T065 → T077 → T096 → T118 → T132
- `test/consultantDetailPanels.test.tsx`: T081 → T102 → T121
- `pages/submissions/SubmissionDetailPage.tsx`: T117 → T130
- `api/types.ts` and `labels.ts`: appended by each story's frontend API task; do these in sequence.

---

## Parallel Examples

### Phase 2 (Foundational)

```text
T010 ErrorCode/BusinessException ‖ T012 AppUser/Role ‖ T013 Team/Region ‖ T014 Recruiter ‖ T019 types.ts
```

### User Story 1

```text
T021 US1SignInIT ‖ T022 navigation.test.tsx ‖ T023 architecture test
then T026 permissions.ts ‖ T027 api/auth.ts ‖ T028 LoadingState/FormField/ErrorBanner ‖ T030 NavBar/PageLayout ‖ T032 NotAuthorized/NotFound
```

### User Story 2

```text
T035 ‖ T036 ‖ T037 ‖ T038 ‖ T039 (tests)
then T040 Clock ‖ T041 paging ‖ T042 VersionGuard ‖ T043 VisaType ‖ T044 history enums ‖ T049 hooks/labels ‖ T050 list components ‖ T051 FilterBar/ReasonDialog
then T052 Consultant entity, then T054 filter/specs ‖ T055 access policy ‖ T056 ReadinessChecker ‖ T062 api/consultants.ts
```

### User Story 4

```text
T079 ‖ T080 ‖ T081 (tests)
then T082 TriggerEvent ‖ T084 MarketingAssignment ‖ T085 MarketingNote ‖ T086 transitions/policy/filter ‖ T092 api/marketing.ts
```

### User Story 5

```text
T098 ‖ T099 ‖ T100 ‖ T101 ‖ T102 (tests)
then T103 Vendor/Client ‖ T105 Submission ‖ T106 notes/dup refs ‖ T107 transitions/policy/filter ‖ T113 api ‖ T114 CounterpartyPicker
```

### User Story 6

```text
T120 ‖ T121 (tests)  then  T122 Placement ‖ T123 policy/filter ‖ T128 api/placements.ts
```

### User Stories 7 and 8 (in parallel after US6)

```text
Developer A: T134–T141 (dashboard)      Developer B: T142–T148 (reports)
```

### Polish

```text
T149 AuthorizationMatrixTest ‖ T150 SensitiveFieldExposureTest ‖ T151 ConcurrencyIT ‖ T154 README
```

---

## Implementation Strategy

### MVP first (User Story 1)

1. Phase 1 Setup (T001–T009). Stop if the toolchain check fails.
2. Phase 2 Foundational (T010–T020).
3. Phase 3 US1 (T021–T034).
4. **Stop and validate**: run quickstart V1 and demo the sign-in and role navigation.

### Incremental delivery

1. P1 block: US1 → US2 → US3. This gives a usable consultant roster with readiness and
   assignment, and is the first business demo.
2. P2 block: US4 → US5. Marketing and submissions.
3. P3 block: US6, then the full lifecycle demo (SC-001), then US7 (dashboard).
4. P4: US8 reports.
5. Phase 11 hardening and the final quality gate.

Each story ends with a green build and its quickstart walkthrough, so the product is always
demonstrable.

### Parallel team strategy

- Everyone does Setup and Foundational together.
- US1–US6 form a dependency chain, so parallelize *within* each story: backend tests ‖ backend
  domain ‖ frontend API and pages.
- After US6, split US7 and US8 across developers. Polish tests T149–T151 can be written in
  parallel.

---

## Notes

- [P] tasks touch different files and have no dependency on incomplete tasks.
- [USn] labels trace each task to a spec user story (constitution VIII).
- Don't add dependencies beyond T002/T005 without a spec-backed reason (constitution V).
- Commit after each task or logical group, referencing the task ID.
- Report any deviation from the spec, plan, or contracts in the change description instead of
  changing requirements silently.
