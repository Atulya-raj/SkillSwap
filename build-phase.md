# SkillSwap — Build Phases

> Execute **one phase at a time**, in order. Read `blueprint.md` and `database-schema.md` first.
> After each phase: run the "Done when" checks, update `PROGRESS.md`, summarise in 5 lines, and **wait for the user** before the next phase.
> Suggested owner (from the review plan): **A** = Atulya (OOP, matching, concurrency), **T** = Tarun (DB, JDBC, credits, exceptions), **H** = Atharva (JSP, servlets, sessions). Everyone reviews everyone's code.

Review mapping: **Review 2** target = end of Phase 8. **Review 3** target = Phase 11 complete.

---

## Phase 0 — Environment and project skeleton (A + T + H)

**Goal:** an empty but deployable Java web app.

Tasks
- [ ] Verify JDK 17, Maven 3.9+, MySQL 8, Tomcat 10.1 are installed; list versions in `PROGRESS.md`.
- [ ] Create the Maven WAR project `com.skillswap:skillswap:1.0.0` with the dependencies and plugins from `blueprint.md` section 3 (Java release 17, UTF-8).
- [ ] Create the folder/package tree from `blueprint.md` section 4.2 (empty packages get a `package-info.java`).
- [ ] Create `web.xml` (Servlet 6.0): welcome file, session timeout 30, cookie config (`HttpOnly`, `SameSite=Lax`), error pages (`404`, `403`, `500` and `java.lang.Throwable`).
- [ ] Create `config.properties` with all keys from `blueprint.md` section 14 and the `Config` utility (loads from classpath, resolves `${user.home}`, typed getters `getInt`, `getString`, `getBoolean`).
- [ ] Create `logging.properties` (console + rolling `FileHandler` at `${user.home}/skillswap/logs/skillswap-%g.log`).
- [ ] Create `PROGRESS.md` with a checklist copied from this file and a "Decisions" section.
- [ ] Add a temporary `PingServlet` at `/ping` returning "SkillSwap OK".

Done when
- `mvn clean package` succeeds and the WAR deploys to Tomcat 10.1.
- `http://localhost:8080/skillswap/ping` returns "SkillSwap OK".

---

## Phase 1 — Database and connection layer (T)

**Goal:** schema in MySQL and a working hand-written pool with transactions.

Tasks
- [ ] Save `schema.sql` and `seed.sql` from `database-schema.md` under `src/main/resources/db/`.
- [ ] Implement `Database` (singleton): `ArrayBlockingQueue<Connection>` pool, `init(Config)`, `acquire()`, `shutdown()`. Hand out `java.lang.reflect.Proxy` connections whose `close()` returns to the pool. Borrow with `poll(timeout)`; throw `DataAccessException` on timeout. Validate a connection with `isValid(2)` before handing it out and replace it if dead.
- [ ] Implement `TransactionManager.execute(TxWork<T>)` with a `ThreadLocal<Connection>`; commit on success, rollback on any `Throwable`, always clear the ThreadLocal. `TxWork<T>` is a functional interface `T run() throws Exception`.
- [ ] Implement `DatabaseInitializer` (reads schema, splits statements, runs seed only if `categories` is empty).
- [ ] Implement `AppContextListener` (`contextInitialized`: load config, init pool, run initializer; `contextDestroyed`: shutdown pool). Register via `@WebListener`.
- [ ] Update `PingServlet` to also report the number of categories from the DB.

Done when
- Starting Tomcat creates all tables and seed rows in MySQL (check with `SHOW TABLES;`).
- `/ping` shows the category count (8).
- A JUnit test proves: pool returns the same underlying connections, a failed transaction rolls back, and the pool blocks then times out when exhausted.

---

## Phase 2 — Domain model, exceptions, utilities (A)

**Goal:** all types the rest of the app depends on.

Tasks
- [ ] Create all enums in `model/enums` (with `SessionStatus.isTerminal()` and `canMoveTo()` encoding the state machine from `blueprint.md`).
- [ ] Create model classes from `database-schema.md` section 3: `User` (abstract), `Student`, `Admin`, `Notifiable`, `Category`, `Skill`, `SkillListing` (abstract), `OfferedSkill`, `WantedSkill`, `AvailabilitySlot`, `SessionRequest`, `CreditEntry`, `Review`, `Notification`, `Message`, `Badge`, `Report`, `RequestDraft`, `MatchResult`, `SessionUser`.
- [ ] Create the full exception hierarchy from `blueprint.md` section 4.5 (`ValidationException` carries a `Map<String,String>`).
- [ ] Create utilities: `PasswordHasher` (PBKDF2WithHmacSHA256, 120000 iterations, 16-byte salt, `iterations:salt:hash` format, constant-time compare), `Validator`, `Flash` (session-scoped one-shot message), `Paginator`/`Page<T>`, `DateUtil` (`java.time` helpers, `Asia/Kolkata`), `Html` (escape helper for places JSTL cannot cover).

Done when
- Everything compiles.
- JUnit tests pass for: `PasswordHasher` (hash/verify, wrong password, different salts), `SessionStatus.canMoveTo` for every valid and invalid pair, `OfferedSkill.validate()` bounds, `AvailabilitySlot.overlapMinutes`.

---

## Phase 3 — DAO layer (T)

**Goal:** every table reachable through an interface and a JDBC implementation.

Tasks
- [ ] Create all DAO interfaces from `database-schema.md` section 4 and `SearchCriteria`.
- [ ] Implement them in `dao/jdbc/` using only `PreparedStatement`, try-with-resources, and `Database.acquire()` (joins the current transaction when present).
- [ ] Map `user_skills` rows to `OfferedSkill`/`WantedSkill` by the `type` column (polymorphic mapping).
- [ ] `SessionRequestDao.updateStatus(id, from, to)` must be a guarded UPDATE returning whether a row changed.
- [ ] `lockForUpdate` / `lockById` use `SELECT ... FOR UPDATE`.
- [ ] Wrap `SQLException` into `DataAccessException` everywhere.
- [ ] Write a small `TestData` helper to insert sample users/skills for tests.

Done when
- Integration tests (against a separate `skillswap_test` schema configured through a test `config.properties`) pass: insert/find user, insert listings of both types and read them back as the right subclass, guarded status update returns false on a stale status, `hasOverlappingAccepted` works on edge times.

---

## Phase 4 — Auth, security filters, base UI shell (H)

**Goal:** register, log in, log out, protected pages, and a working layout.

Tasks
- [ ] `AuthService.register/login/changePassword`; grant the signup bonus (ledger `SIGNUP_BONUS`) inside one transaction; enforce `allowed.email.domain` when set.
- [ ] `AppContextListener` seeds the admin account from config if none exists.
- [ ] Filters: `EncodingFilter` (UTF-8), `AuthFilter` (redirect guests to `/login`, remember requested URL), `RoleFilter` (admin-only paths under `/admin/*`), `CsrfFilter` (token in session, hidden field on every POST form), `SecurityHeadersFilter`.
- [ ] `ActiveUsersListener` (`HttpSessionListener`) maintaining an `AtomicInteger` of active sessions in the `ServletContext`.
- [ ] `AuthServlet` (`/register`, `/login`, `/logout`, `/password`) using Post-Redirect-Get and `Flash`. Store a `SessionUser` in the session; call `request.changeSessionId()` on login (session fixation protection).
- [ ] Build `layout/header.jspf`, `nav.jspf` (role-aware), `footer.jspf`, `home.jsp`, `login.jsp`, `register.jsp`, `password.jsp`, `403.jsp`, `404.jsp`, `error.jsp`.
- [ ] Write `assets/css/style.css` with the design tokens and components from `blueprint.md` section 13.

Done when
- A new user can register (gets 10 credits), log out, log in, and change password.
- Visiting `/dashboard` as a guest redirects to `/login`; a student hitting `/admin` gets the 403 page.
- A POST without a CSRF token is rejected.
- Tests: `AuthService` happy path, duplicate email, weak password, inactive account.

---

## Phase 5 — Profile, skills, availability, avatar (H + A)

**Goal:** users can describe who they are and what they offer/want.

Tasks
- [ ] `CatalogCache` (`TreeMap<String, List<Skill>>` + `ReadWriteLock`) and `CatalogService` read methods.
- [ ] `ProfileService`, `ProfileServlet` (`/profile`, `/profile/avatar`, `/users/view`), `profile.jsp`, `public-profile.jsp`.
- [ ] `FileStorage` + `AvatarServlet` (`@MultipartConfig`, 2 MB limit, JPG/PNG check including magic bytes, random file names, delete old avatar, stream bytes with correct `Content-Type` and cache headers).
- [ ] `ListingService` + `ListingServlet` (`/skills`) + `my-skills.jsp`: offer/want/remove with `OfferedSkill.validate()`.
- [ ] `AvailabilityService` + `AvailabilityServlet` + `availability.jsp` (weekly grid rendered with JSTL).
- [ ] `ConnectorRule` data prerequisite: expose `countOfferedByUser`.

Done when
- A user can edit their profile, upload an avatar, add offered/wanted skills (with price and level), and manage weekly availability.
- Duplicate listings and invalid slots show inline errors.
- Tests: listing validation, availability overlap rejection, upload rejects a renamed non-image file.

---

## Phase 6 — Discovery and matching engine (A)

**Goal:** browse, search, and recommendations.

Tasks
- [ ] `MatchEngine` interface, `MatchResult`, `WeightedMatchEngine`, `RatingFirstMatchEngine`, `MatchEngineFactory` (reads `match.engine`).
- [ ] Implement the scoring exactly as in `blueprint.md` section 8 using a bounded `PriorityQueue` for top-K.
- [ ] `SearchService` + `BrowseServlet` + `browse.jsp`: filters (keyword, category, skill, level, min rating, day), sort (BEST/RATING/PRICE), pagination (10/page), provider cards with rating, price and badges.
- [ ] `MatchService` + `MatchServlet` + `matches.jsp` (top 10 recommendations with score breakdown bars drawn with CSS).
- [ ] `NoMatchFoundException` thrown when no candidates; the UI shows a friendly empty state.

Done when
- Browse filters and sorting return correct results on seeded data.
- JUnit tests for `WeightedMatchEngine`: exact beats same-category, higher rating beats lower at equal relevance, cold-start prior (0.6) applied under 3 ratings, learner excluded, inactive excluded, top-K ordering stable.
- Switching `match.engine=rating` changes ordering without code changes.

---

## Phase 7 — Sessions and credit economy (T + A)

**Goal:** the core transaction flow with escrow.

Tasks
- [ ] `CreditService` (`hold`, `release`, `pay`, `adminAdjust`) with row locks in ascending user-id order and a ledger row for every balance change.
- [ ] `SessionService.create/accept/reject/cancel/confirm/markNoShow` implementing the transaction recipes in `database-schema.md` section 5 and the credit table in `blueprint.md`.
- [ ] A single private `transition(...)` method uses the guarded DAO update and `SessionStatus.canMoveTo`; invalid moves throw `InvalidStateTransitionException`.
- [ ] Throw `DuplicateRequestException`, `InsufficientCreditsException`, `SchedulingConflictException`, `AuthorizationException` where specified.
- [ ] `RequestServlet` (`/requests`, `/requests/new`, `/requests/view`, `/requests/respond`, `/cancel`, `/confirm`, `/noshow`) and JSPs `requests.jsp` (tabs: incoming/outgoing, status filter), `request-form.jsp`, `request-detail.jsp` (status timeline, action buttons shown only when legal).
- [ ] `CreditServlet` + `credits.jsp` (balance, held credits = sum of costs of PENDING/ACCEPTED outgoing requests, paginated ledger).
- [ ] Draft support: `DraftStore` (`ObjectOutputStream`/`ObjectInputStream` to `drafts.dir`), `DraftService`, "Save draft" button on the form, restore on load, delete on successful submit.

Done when
- End-to-end by hand: A requests B (credits held) -> B accepts -> both confirm -> B is paid, ledger shows HOLD then EARN.
- Reject, cancel (both timings), provider-cancel, and both no-show paths produce the exact credit effects in the table.
- Concurrency test: two threads accepting the same request leaves exactly one winner; two threads creating requests that together exceed the balance leaves balance >= 0.
- Draft round trip works (save, reload page, values restored, file removed after submit).

---

## Phase 8 — Background engine and notifications (A)

**Goal:** the multithreading showpiece for the demo.

Tasks
- [ ] `NotificationEvent`, `NotificationDispatcher` (daemon thread, `LinkedBlockingQueue`, poison-pill shutdown, log and continue on DB errors).
- [ ] `NotificationService.notify(...)` publishes events; wire notifications into all `SessionService` transitions (request received/accepted/rejected/cancelled/expired/completed/no-show).
- [ ] `NotificationServlet` + `notifications.jsp`; unread count shown in `nav.jspf` through a request attribute set by a small filter or by `NotificationService.unreadCount` in the servlet base class.
- [ ] `SchedulerService` (`ScheduledExecutorService`, 2 threads) with `MatchScanTask`, `ExpiryTask`, `ReminderTask`, `AutoCompleteTask` per `blueprint.md` F-25 to F-28. Each task wraps its body in try/catch and logs.
- [ ] `AppContextListener` starts the dispatcher and scheduler after the DB is ready and shuts them down gracefully on stop (`shutdown`, `awaitTermination(10s)`, then `shutdownNow`).
- [ ] Add a clearly named debug config `scheduler.match.seconds=15` in a `demo` profile (second properties file) so the demo shows matches quickly.

Done when
- Creating a new offered skill that matches someone's wanted skill produces a "New match" notification within one scan interval, without any page request triggering it.
- A PENDING request older than `request.expiry.hours` (set to 1 minute for the test) becomes EXPIRED and the learner is refunded.
- Stopping Tomcat shows clean thread shutdown in the log; no leaked threads on redeploy.
- Tests: tasks idempotent when run twice; dispatcher drains the queue on shutdown.

---

## Phase 9 — Reviews, badges, messaging, leaderboard (A + H)

**Goal:** trust and engagement features.

Tasks
- [ ] `ReviewService.submit` per the recipe (rating, stats update in one transaction), `ReviewServlet`, review form on `request-detail.jsp` after COMPLETED, reviews displayed on `public-profile.jsp`.
- [ ] `BadgeRule` interface and the six rules from `blueprint.md` section 9, `BadgeService.evaluate` (called after listing changes, completion, and reviews), badge notifications, badge display on profile/browse cards.
- [ ] `MessageService` + `MessageServlet` + `messages.jsp` (thread per request, participant check, mark-as-read, unread count in nav).
- [ ] `LeaderboardServlet` + `leaderboard.jsp` (top mentors by rating with min 3 reviews; by sessions taught).
- [ ] `ReportServlet` and report buttons on public profiles and reviews.

Done when
- After completing a session both parties can review exactly once; average rating and counts update on the user row.
- Badges are awarded automatically and only once; unit tests cover each `BadgeRule` boundary.
- Non-participants cannot read or post in a thread (403).

---

## Phase 10 — Dashboards, admin panel, analytics, exports (H + T)

**Goal:** the finishing features that make the demo look complete.

Tasks
- [ ] `AnalyticsService.userDashboard` and `DashboardServlet` + `dashboard.jsp` (F-29).
- [ ] `AdminService`, `AdminServlet` and `admin/dashboard.jsp`, `admin/users.jsp`, `admin/catalog.jsp`, `admin/reports.jsp`:
  - analytics (F-33) with CSS bar charts (percentage widths computed in the servlet),
  - user search, activate/deactivate, manual credit adjustment with required reason,
  - catalog CRUD and CSV import via `CsvImporter` (refreshes `CatalogCache`),
  - report queue and review hiding (recomputes average).
- [ ] `Exporter` interface and `CsvExporter` (proper quoting/escaping, UTF-8 with BOM for Excel), `ExportServlet` (`/history/export`, `/admin/users/export`) with `Content-Disposition: attachment`.
- [ ] Show "users online now" from the `ServletContext` counter on the admin dashboard.

Done when
- Admin can manage the catalog (including a CSV import with a bad row reported, not crashing), deactivate a user (they cannot log in), adjust credits, resolve reports.
- Exported CSV opens correctly in Excel/LibreOffice with commas and quotes in fields.
- Dashboard numbers match the ledger and session tables (spot-check three users).

---

## Phase 11 — Hardening, tests, demo data, documentation (A + T + H)

**Goal:** a project you can demo and defend in the viva.

Tasks
- [ ] Security pass: grep for any string-concatenated SQL; ensure every dynamic JSP value uses `<c:out>`; verify CSRF on all POST forms; verify authorization checks (a student cannot view another's request or messages by changing the `id`).
- [ ] Error handling pass: every servlet catches `SkillSwapException`; unexpected errors land on `error.jsp` and are logged at SEVERE.
- [ ] Add a `DemoDataLoader` (run only when `demo.data=true`) creating ~12 users, listings, availability, a few sessions in each status, reviews, and messages.
- [ ] Test pass: `mvn test` green; add missing tests for `SessionService` happy and failure paths; run the concurrency tests 20 times.
- [ ] Performance pass: confirm indexes are used (`EXPLAIN` on search and request listing queries) and pagination everywhere.
- [ ] UI pass: every page works at phone width; consistent empty states and flash messages.
- [ ] Documentation: `README.md` (prerequisites, DB setup, config keys, how to run on Tomcat, default admin login, demo script), a class diagram in text (mermaid or ASCII), ER diagram in text, and a one-page demo script for the review (login as A, request, B accepts, notification arrives, complete, review, admin analytics).
- [ ] Final `mvn clean package`; verify the WAR runs on a fresh Tomcat and an empty database.

Done when
- Fresh machine setup from the README works in under 10 minutes.
- The demo script runs end to end without errors.
- `PROGRESS.md` shows every box ticked.

---

## Feature-to-phase traceability

| Features | Phase |
|---|---|
| F-01, F-02, F-03 | 4 |
| F-04, F-05, F-06 (read), F-07, F-08, F-09 | 5 |
| F-10, F-11 | 6 |
| F-12 to F-18 | 7 |
| F-23, F-25 to F-28 | 8 |
| F-19 to F-22, F-24 | 9 |
| F-29 to F-35 | 10 |