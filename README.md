# Project Management Tool — Collaborative Project Management Backend

A Trello/Asana-style collaborative tool built with Spring Boot as an internship project.
Users can register/login, create projects, invite teammates, create and assign task
cards, move them across statuses, comment on tasks, and see live updates + notifications
over WebSockets — no page refresh needed.

> Built step-by-step, one phase at a time, to understand every layer rather than
> generating it wholesale. See [Build Log](#build-log--phases) for the order things
> were implemented and tested in.

---

## Tech Stack

- **Java 17**, **Spring Boot 3.3**
- **Spring Web** — REST API
- **Spring Data JPA** — database access
- **PostgreSQL** — database
- **Spring Security + JJWT** — stateless JWT authentication
- **Spring Validation** — request validation (`@NotBlank`, `@Email`, etc.)
- **Spring WebSocket (STOMP over SockJS)** — real-time board updates & private notifications
- **Lombok** — reduces boilerplate (getters/setters/constructors)
- **Plain HTML/JS** — minimal test frontend, served as static files by Spring Boot itself

---

## Project Structure

```
src/main/java/com/pmtool/
├── PmToolApplication.java
├── model/              Entities: User, Project, ProjectMember, TaskCard, Comment, Notification
│   └── type/            Enums: ProjectRole, TaskStatus
├── repository/          Spring Data JPA repositories
├── security/            JWT plumbing: JwtUtil, JwtAuthFilter, CurrentUserProvider
├── config/              SecurityConfig, WebSocketConfig
├── dto/                 Request/response objects — entities are never returned directly
├── service/             Business logic: AuthService, CustomUserDetailsService,
│                        ProjectService, TaskService, CommentService, NotificationService
├── controller/          REST endpoints
└── exception/           ApiException + GlobalExceptionHandler for clean JSON errors

src/main/resources/
├── application.yml
└── static/              Minimal test frontend (served at http://localhost:8080/)
    ├── index.html
    └── app.js
```

**Package conventions used in this project:**
- All business logic lives in `service/`, including `CustomUserDetailsService`
  (even though it implements a Spring Security interface, it's still "business logic":
  looking up a user).
- `security/` is reserved for pure security-framework plumbing that has no reason to
  exist outside an auth context (`JwtUtil`, `JwtAuthFilter`, `CurrentUserProvider`).
- Enums live in their own `model/type` sub-package, separate from the entities that use them.
- The frontend lives in `static/`, NOT `templates/` — `templates/` is for server-rendered
  views (Thymeleaf etc.); this app's HTML is a static file that calls the REST API via JS.

---

## Setup

### 1. Prerequisites
- JDK 17+
- Maven
- PostgreSQL running locally

### 2. Create the database
```sql
CREATE DATABASE pmtool;
```

### 3. Configure `src/main/resources/application.yml`
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pmtool
    username: postgres
    password: yourpassword
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

app:
  jwt:
    secret: this-is-a-demo-secret-key-change-it-before-going-to-production-1234
    expiration-ms: 86400000   # 24 hours
```
Note: `app.jwt.secret` must be **32+ characters** (required for HMAC-SHA256 signing).
Never commit a real production secret — this demo value is fine for local dev only.

### 4. Add JJWT to `pom.xml`
Spring Initializr doesn't include this — add manually inside `<dependencies>`:
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

### 5. Run
```bash
mvn spring-boot:run
```
This starts **both** the API and the static test frontend on `http://localhost:8080`
(no separate server needed — Spring Boot serves everything in `static/` automatically).

Open `http://localhost:8080/index.html` to use the test client, or drive the API
directly with Postman.

Tables are auto-created on first run (`ddl-auto: update`) — verify with
`psql`/pgAdmin/DBeaver that `users`, `projects`, `project_members`, `task_cards`,
`comments`, and `notifications` all exist.

---

## Authentication

All endpoints except `/api/auth/**`, `/ws/**`, and the static frontend files require:
```
Authorization: Bearer <token>
```
Get a token from `/api/auth/register` or `/api/auth/login`.

Auth is **stateless** — no server-side session is stored. The JWT (signed, 24h expiry)
is the only proof of identity, verified fresh on every request by `JwtAuthFilter`.
The user's **email** doubles as their Spring Security username — this matters for
WebSocket routing too (see below).

---

## API Reference

### Auth — public

| Method | Endpoint | Body | Notes |
|---|---|---|---|
| POST | `/api/auth/register` | `{ fullName, email, password }` | password min 6 chars |
| POST | `/api/auth/login` | `{ email, password }` | same error message for "no such user" and "wrong password" — prevents user enumeration |

Both return:
```json
{ "token": "...", "userId": 1, "fullName": "...", "email": "..." }
```

### Projects — requires token

| Method | Endpoint | Body | Who can call it |
|---|---|---|---|
| POST | `/api/projects` | `{ name, description }` | any authenticated user (becomes OWNER) |
| GET | `/api/projects` | — | any authenticated user (their own projects) |
| POST | `/api/projects/{id}/members` | `{ email }` | project OWNER only → also sends a notification to the invited user |
| GET | `/api/projects/{id}/members` | — | any project MEMBER |

### Tasks — requires token, caller must be a project MEMBER

| Method | Endpoint | Body | Notes |
|---|---|---|---|
| POST | `/api/projects/{projectId}/tasks` | `{ title, description?, assigneeEmail? }` | assignee must already be a project member; sends notification + broadcasts board update |
| GET | `/api/projects/{projectId}/tasks` | — | lists all tasks on the board |
| PUT | `/api/tasks/{taskId}/status` | `{ status: "TODO" \| "IN_PROGRESS" \| "DONE" }` | broadcasts board update |
| PUT | `/api/tasks/{taskId}/assignee` | `{ email }` | assignee must be a project member; sends notification + broadcasts board update |

### Comments — requires token, caller must be a project MEMBER

| Method | Endpoint | Body |
|---|---|---|
| POST | `/api/tasks/{taskId}/comments` | `{ content }` |
| GET | `/api/tasks/{taskId}/comments` | — (oldest first) |

### Notifications — requires token

| Method | Endpoint | Notes |
|---|---|---|
| GET | `/api/notifications` | past notifications for the logged-in user, newest first |

---

## Real-time layer (WebSocket / STOMP)

Connects to `ws://localhost:8080/ws` via SockJS (falls back to polling transports if
raw WebSockets are blocked by a network).

| Destination | Scope | Fires on |
|---|---|---|
| `/topic/projects/{projectId}` | Broadcast — everyone subscribed to that project | task created, task status changed, task reassigned, new comment posted |
| `/user/queue/notifications` | Private — just the intended user | added to a project, assigned a task, someone commented on your assigned task |

Design choice: board-update messages carry no payload, just a signal
(`"BOARD_UPDATED"`) — the frontend reacts by re-fetching the task list rather than
trying to merge partial state. Simpler and more reliable than diffing client-side
state by hand, at the cost of one extra GET per update.

`convertAndSendToUser(email, "/queue/notifications", payload)` relies on the fact that
the user's **email** is what Spring Security treats as their username (set in
`CustomUserDetailsService`) — this is what lets the server target a specific
person's open WebSocket session by email alone.

---

## Error format

All errors come back as consistent JSON via `GlobalExceptionHandler`:
```json
{
  "message": "Only the project owner can do this",
  "status": 403,
  "timestamp": "2026-08-09T12:00:00"
}
```

| Status | Meaning in this app |
|---|---|
| 400 | Validation failed (`@Valid` on a DTO) |
| 401 | Bad login credentials, or invalid/missing token |
| 403 | Authenticated, but not allowed to do this (wrong role/not a member) |
| 404 | Resource doesn't exist |
| 409 | Conflict (duplicate email, already a member, etc.) |

---

## Key design decisions

- **DTOs everywhere, entities never returned directly** — keeps the password hash out
  of responses and decouples the API shape from the DB schema.
- **`ProjectMember` as an explicit join entity** (not a raw `@ManyToMany`) — needed
  to store a `role` (OWNER/MEMBER) on the relationship itself.
- **No bidirectional JPA relationships** (no `@OneToMany` lists on the "one" side) —
  avoids infinite JSON recursion and lazy-loading foot-guns; related data is fetched
  via repository queries instead (e.g. `taskRepository.findByProject(project)`).
- **Authorization checks live in the service layer** (`assertIsMember`,
  `assertIsOwner` in `ProjectService`), reused by `TaskService`/`CommentService`
  rather than duplicated — each throws `ApiException` and lets
  `GlobalExceptionHandler` turn it into a clean response.
- **Assigning a task checks membership twice**: first that the assignee's email
  belongs to a real account (404 if not), then that they're actually a member of
  *this* project (403 if not) — deliberately two different status codes for two
  different failure reasons.
- **Board updates are "refetch" signals, not data payloads** — simpler and more
  robust than syncing partial state across clients.
- **Frontend lives in `static/`, not `templates/`** — it's a static file calling the
  REST API via `fetch()`, not server-rendered HTML, so `templates/` (reserved for
  engines like Thymeleaf) would be the wrong location and wouldn't serve correctly.

---

## Build Log / Phases

1. DONE — Project setup — Spring Initializr, dependencies, `application.yml`, boots successfully
2. DONE — Entities — User, Project, ProjectMember, TaskCard, Comment, Notification
3. DONE — Repositories — Spring Data JPA interfaces
4. DONE — Security & JWT — password hashing, `JwtAuthFilter`, register/login working end-to-end
5. DONE — Project & membership APIs — create project, invite members, owner-only checks tested (403 confirmed for non-owners)
6. DONE — Task APIs — create/list/move/assign, tested including 404 (no such user) vs 403 (not a project member) on assignment
7. DONE — Comments API — tested with two different authors, 403 for non-members, 400 for empty content
8. DONE — WebSockets — `WebSocketConfig`, board broadcast on task/comment changes, private notifications on invite/assign
9. IN PROGRESS — Minimal test frontend (static HTML/JS) — login confirmed working, live two-tab
   board-update + notification test in progress
10. TODO — Polish & final end-to-end walkthrough

---

## Testing notes

Tested manually via Postman + the browser test client:
- Register/login, including negative cases (duplicate email → 409, wrong password → 401)
- Create project → creator becomes OWNER
- Invite member as owner → succeeds; as non-owner → 403 confirmed
- Create task (unassigned and pre-assigned), list tasks, move status, reassign
- Assign to nonexistent email → 404; assign to real user not on the project → 403 (both confirmed as distinct cases)
- Comments from multiple distinct users on the same task, oldest-first ordering
- Comment as non-member → 403; empty comment → 400
- WebSocket connects successfully from the browser test client (confirmed via "WebSocket connected" in the live log)
- Two-tab live board-update and private-notification test — in progress

## Known gotchas hit during development (worth remembering)

- `.orElseThrow(() -> new X(...))` — the lambda **returns** the exception, it never
  contains a `throw` statement itself.
- JSON keys must match Java field names **exactly** (`fullName`, not `full_name`) —
  Jackson doesn't auto-convert casing without an explicit `@JsonProperty`.
- A stray trailing space in a Postman URL gets encoded as `%20`, silently breaking
  the route match and producing a confusing 500 instead of a clean 404.
- `templates/` vs `static/` — static assets (plain HTML/JS/CSS meant to be served
  as-is) belong in `static/`; `templates/` is only for server-side rendering engines.
- Double-check which token is actually loaded in Postman before debugging "wrong
  owner" 403s — an unexpected owner name in a response is often just a stale/wrong
  token, not a logic bug.
