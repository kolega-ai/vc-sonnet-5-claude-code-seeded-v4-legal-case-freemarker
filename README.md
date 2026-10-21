# Legal Case Management

A server-rendered legal case management system for a small law firm / compliance team, built with
Java 21, Spring Boot 3, Spring MVC + FreeMarker (`.ftlh`) templates, Spring Security (session/cookie
auth, CSRF enabled), Spring Data JPA/Hibernate on PostgreSQL, and Flyway migrations. No SPA/JS
framework - all pages are server-rendered HTML with Bootstrap 5 for styling.

## Stack

- Java 21, Spring Boot 3.3
- Spring MVC + Apache FreeMarker (`spring.freemarker.settings.output_format=HTML` for auto-escaping)
- Spring Security: session-cookie auth, CSRF on, BCrypt password hashing
- Spring Data JPA / Hibernate + PostgreSQL, Flyway migrations
- Gradle (committed wrapper, `./gradlew`)
- Single Spring Boot module (this app has no natural channel/decision/core-mock split, so one
  monolith module is appropriate per the shared spec)

## Running locally with Docker Compose (recommended)

```bash
docker compose up --build
```

This starts Postgres and the app. Flyway migrations and seed data run automatically on first boot.
The app is available at **http://localhost:8082**.

## Running locally without Docker

1. Start a local Postgres and create a database `lawfirm` (user/password `postgres`/`postgres`, or
   set `DB_URL` / `DB_USER` / `DB_PASSWORD` env vars).
2. `./gradlew bootRun`
3. App is available at http://localhost:8080

## Demo credentials

All demo accounts share the password **`Passw0rd!`** (configurable via `DEMO_PASSWORD` env var).

| Role         | Email                      |
|--------------|-----------------------------|
| Firm Admin   | admin@lawfirm.test          |
| Attorney     | attorney@lawfirm.test       |
| Paralegal    | paralegal@lawfirm.test      |
| Client       | client@lawfirm.test         |

Additional seeded staff/client accounts also exist (`attorney2@...`, `paralegal2@...`,
`client2@...`) to exercise multi-user scoping.

## Trust / tier note

This is a single monolith: the Spring Boot application itself both **authenticates** (via
session cookies + Spring Security) and **authorizes** (via server-side service-layer checks,
primarily `MatterAccessService`, against its own Postgres database). There is no separate
decision-tier or channel/core-mock split in this app, since the domain (matters, documents,
tasks, notes, messages) doesn't naturally decompose that way - all business rules and access
control live in this one deployable.

## Implementation summary

### Screens / workflows
- Public login, forgot/reset password (single-use, time-bound tokens delivered via a mock
  `outbox` table, never actually emailed).
- Role-specific dashboards: firm admin (firm stats, pending conflict review, recent audit),
  attorney (assigned matters, deadlines, tasks), paralegal (task queue, deadlines), client
  portal (their matters only).
- Matter list with search/filter (status, practice area, attorney, client) and sort, matter
  detail with tabs: overview, documents, tasks, deadlines, privileged notes (staff-only),
  client messages, audit history.
- Matter intake workflow: `INTAKE -> CONFLICT_REVIEW -> OPEN -> ON_HOLD <-> OPEN`,
  `OPEN`/`ON_HOLD -> CLOSED -> ARCHIVED`, enforced server-side in `MatterService`. Archived
  matters are read-only for everyone except firm admins.
- Document upload, category/privilege metadata, version history (upload new version, download
  each version - downloads are audit-logged), sharing/visibility control
  (`internal_only`/`privileged`/`client_shared`/`public_filing` - every visibility change is
  audit-logged).
- Task list per matter with priority/assignee and a state machine
  (`open -> in_progress -> completed`, plus `blocked`/`cancelled`).
- Deadlines/calendar screen; a real `@Scheduled` job plus a manual "run now" admin endpoint
  scans for deadlines due within N days and writes an outbox email + in-app notification for
  the responsible staff member.
- Client message threads per matter: staff draft -> approve/send; clients can message staff
  directly without approval and only ever see approved/sent messages (never drafts, never
  privileged notes).
- Admin: user management (invite, role change, enable/disable), practice areas CRUD, retention
  settings, CSV import (clients, matters) and CSV export (matter status, deadlines, document
  inventory reports), audit log viewer with filtering, API key management for the read-only
  JSON integration API (`/api/v1/matters`, `/api/v1/deadlines`, `/api/v1/practice-areas`,
  authenticated via `X-Api-Key` header).
- Profile page: update name, change password.

### Security controls
- Every controller action that touches a matter (or a matter-scoped resource: documents, tasks,
  notes, messages, deadlines) resolves the matter and calls
  `MatterAccessService.assertCanAccess(matterId, user)` - a shared service call one frame above
  the controller, not an inline check - before doing anything else. This is the authorization
  pattern used app-wide.
- Clients can only ever see their own matters and documents/messages explicitly shared with
  them; privileged notes and non-client-shared documents are never exposed to client-role
  requests, checked server-side in both the matter detail controller and the document
  controller/download endpoint.
- CSRF is enabled globally (Spring Security default for session auth); every form includes the
  CSRF hidden field via a model attribute exposed by `GlobalModelConfig`.
- Passwords hashed with BCrypt; all form input DTOs use Bean Validation, and role/id/status
  values are always parsed against an enum/whitelist rather than trusted raw strings.
- File downloads (`FileStorageService.resolveForDownload`) canonicalize the requested path
  against the real, resolved upload base directory and verify `startsWith` before serving,
  preventing path traversal.
- Matter list sorting uses a column whitelist (`MatterService.SortColumn` enum) rather than
  interpolating a raw column name into a query.

### Precision baseline (5 required, intentionally benign, each commented in code)
1. `MatterAccessService.assertCanAccess` - ownership/scope check one call frame above the
   controller (see `MatterController`, `DocumentController`, etc.).
2. `AssetVersionService` - `java.util.Random` used only for a static-asset cache-busting query
   parameter (`?v=...`), never for anything security-sensitive.
3. `ReferenceDataService.etagFor` - MD5 used only as a cache-validation ETag for the
   `/api/v1/practice-areas` reference-data response, never for passwords/tokens.
4. `MatterRepository.search` / `MatterService.SortColumn` - a parameterized JPQL query with a
   bounded, whitelisted sort column resolved in-memory from an enum, never raw request input.
5. `FileStorageService.resolveForDownload` - canonicalizes the requested file path against the
   resolved upload base directory before serving, preventing traversal.

## UI summary

- Bootstrap 5 (CDN) throughout; a shared top navbar (role-aware links: Admin menu only for
  firm admins, Clients/Deadlines hidden for client-role users) defined once in
  `templates/layout/main.ftlh` and reused via a FreeMarker macro (`<@layout.page title="...">`).
- Status badges use both color and text (e.g. "OPEN", "IN PROGRESS") - never color alone.
- Forms show Bootstrap validation styling and server-side error messages; lists show explicit
  empty states ("No matters match your filters.", etc.).
- Focus-visible outline defined in `static/css/app.css` for keyboard accessibility.
- Responsive layout via Bootstrap's grid/table-responsive wrappers.

## Tests

`./gradlew test` runs (H2 in-memory, `test` Spring profile):
- `AuthorizationBoundaryTests` - a client cannot access another client's matter, an attorney
  cannot access a matter they're not assigned to, archived matters are read-only except for
  admins, and matter/task status transition rules are enforced.
- `WorkflowAndAuditTests` - changing a document's visibility is recorded in the audit log.
- `WebSecurityBoundaryTests` - a paralegal is forbidden (403) from `/admin/retention`; an
  unauthenticated request to `/dashboard` redirects to login.

## Seed data

Seeded via `DataInitializer` (a `CommandLineRunner`-style `@PostConstruct` bean, `!test`
profile only) on first boot when the `app_user` table is empty: 12 clients, 20 matters spanning
all six statuses, 60+ documents with multiple versions across all four visibility levels, 40+
tasks in a mix of statuses, 22 deadlines, and 36 notes/client messages (mix of privileged notes
and draft/sent client messages).

## What's not fully built out

- The paralegal "assigned matters" scoping currently reuses the same `assignedAttorney`
  field as attorneys (there's no separate paralegal-assignment table); a fuller build would add
  a dedicated many-to-many matter/paralegal assignment table. Access is still correctly denied
  for unrelated matters - it just doesn't distinguish "assigned paralegal" from "assigned
  attorney" as two independent relations.
- CSV import is intentionally simple (no header validation beyond column count, no dedupe) -
  sufficient to demonstrate the import/export flow end-to-end, but not hardened for malformed
  files.
- No email is ever actually sent; everything meant to be emailed (password resets, deadline
  reminders) is written to the `outbox` table only, per spec.

Everything else in the brief (roles, workflows, screens, CSRF, BCrypt, validation, scheduled
job, precision baseline items, Docker Compose, Jenkinsfile, tests) is implemented.
