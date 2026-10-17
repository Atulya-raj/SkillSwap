# SkillSwap — Implementation Progress

## Environment Versions
- **JDK**: Eclipse Adoptium Temurin OpenJDK 17.0.20.1 (release 17)
- **Maven**: Apache Maven 3.9.9
- **MySQL**: MySQL Server 8.4.11 LTS (Port 3306, databases `skillswap` and `skillswap_test`)
- **Tomcat**: Apache Tomcat 10.1.34 (Servlet 6.0, JSP 3.1, JSTL 3.0)

---

## Phase Checklists

### Phase 0 — Environment and project skeleton
- [x] Verify JDK 17, Maven 3.9+, MySQL 8, Tomcat 10.1 are installed; list versions in `PROGRESS.md`.
- [x] Create the Maven WAR project `com.skillswap:skillswap:1.0.0` with the dependencies and plugins from `blueprint.md` section 3 (Java release 17, UTF-8).
- [x] Create the folder/package tree from `blueprint.md` section 4.2 (empty packages get a `package-info.java`).
- [x] Create `web.xml` (Servlet 6.0): welcome file, session timeout 30, cookie config (`HttpOnly`, `SameSite=Lax`), error pages (`404`, `403`, `500` and `java.lang.Throwable`).
- [x] Create `config.properties` with all keys from `blueprint.md` section 14 and the `Config` utility (loads from classpath, resolves `${user.home}`, typed getters `getInt`, `getString`, `getBoolean`).
- [x] Create `logging.properties` (console + rolling `FileHandler` at `${user.home}/skillswap/logs/skillswap-%g.log`).
- [x] Create `PROGRESS.md` with a checklist copied from this file and a "Decisions" section.
- [x] Add a temporary `PingServlet` at `/ping` returning "SkillSwap OK".

### Phase 1 — Database and connection layer
- [x] Save `schema.sql` and `seed.sql` from `database-schema.md` under `src/main/resources/db/`.
- [x] Implement `Database` (singleton): `ArrayBlockingQueue<Connection>` pool, `init(Config)`, `acquire()`, `shutdown()`. Hand out `java.lang.reflect.Proxy` connections whose `close()` returns to the pool. Borrow with `poll(timeout)`; throw `DataAccessException` on timeout. Validate a connection with `isValid(2)` before handing it out and replace it if dead.
- [x] Implement `TransactionManager.execute(TxWork<T>)` with a `ThreadLocal<Connection>`; commit on success, rollback on any `Throwable`, always clear the ThreadLocal. `TxWork<T>` is a functional interface `T run() throws Exception`.
- [x] Implement `DatabaseInitializer` (reads schema, splits statements, runs seed only if `categories` is empty).
- [x] Implement `AppContextListener` (`contextInitialized`: load config, init pool, run initializer; `contextDestroyed`: shutdown pool). Register via `@WebListener`.
- [x] Update `PingServlet` to also report the number of categories from the DB.

### Phase 2 — Domain model, exceptions, utilities
- [x] Create all enums in `model/enums` (with `SessionStatus.isTerminal()` and `canMoveTo()` encoding the state machine from `blueprint.md`).
- [x] Create model classes from `database-schema.md` section 3: `User` (abstract), `Student`, `Admin`, `Notifiable`, `Category`, `Skill`, `SkillListing` (abstract), `OfferedSkill`, `WantedSkill`, `AvailabilitySlot`, `SessionRequest`, `CreditEntry`, `Review`, `Notification`, `Message`, `Badge`, `Report`, `RequestDraft`, `MatchResult`, `SessionUser`.
- [x] Create the full exception hierarchy from `blueprint.md` section 4.5 (`ValidationException` carries a `Map<String,String>`).
- [x] Create utilities: `PasswordHasher` (PBKDF2WithHmacSHA256, 120000 iterations, 16-byte salt, `iterations:salt:hash` format, constant-time compare), `Validator`, `Flash` (session-scoped one-shot message), `Paginator`/`Page<T>`, `DateUtil` (`java.time` helpers, `Asia/Kolkata`), `Html` (escape helper for places JSTL cannot cover).

### Phase 3 — DAO layer
- [x] Create all DAO interfaces from `database-schema.md` section 4 and `SearchCriteria`.
- [x] Implement them in `dao/jdbc/` using only `PreparedStatement`, try-with-resources, and `Database.acquire()` (joins the current transaction when present).
- [x] Map `user_skills` rows to `OfferedSkill`/`WantedSkill` by the `type` column (polymorphic mapping).
- [x] `SessionRequestDao.updateStatus(id, from, to)` must be a guarded UPDATE returning whether a row changed.
- [x] `lockForUpdate` / `lockById` use `SELECT ... FOR UPDATE`.
- [x] Wrap `SQLException` into `DataAccessException` everywhere.
- [x] Write a small `TestData` helper to insert sample users/skills for tests.

### Phase 4 — Auth, security filters, base UI shell
- [x] `AuthService.register/login/changePassword`; grant the signup bonus (ledger `SIGNUP_BONUS`) inside one transaction; enforce `allowed.email.domain` when set.
- [x] `AppContextListener` seeds the admin account from config if none exists.
- [x] Filters: `EncodingFilter` (UTF-8), `AuthFilter` (redirect guests to `/login`, remember requested URL), `RoleFilter` (admin-only paths under `/admin/*`), `CsrfFilter` (token in session, hidden field on every POST form), `SecurityHeadersFilter`.
- [x] `ActiveUsersListener` (`HttpSessionListener`) maintaining an `AtomicInteger` of active sessions in the `ServletContext`.
- [x] `AuthServlet` (`/register`, `/login`, `/logout`, `/password`) using Post-Redirect-Get and `Flash`. Store a `SessionUser` in the session; call `request.changeSessionId()` on login (session fixation protection).
- [x] Build `layout/header.jspf`, `nav.jspf` (role-aware), `footer.jspf`, `home.jsp`, `login.jsp`, `register.jsp`, `password.jsp`, `403.jsp`, `404.jsp`, `error.jsp`.
- [x] Write `assets/css/style.css` with the design tokens and components from `blueprint.md` section 13.

### Phase 5 — Profile, skills, availability, avatar
- [x] `CatalogCache` (`TreeMap<String, List<Skill>>` + `ReadWriteLock`) and `CatalogService` read methods.
- [x] `ProfileService`, `ProfileServlet` (`/profile`, `/profile/avatar`, `/users/view`), `profile.jsp`, `public-profile.jsp`.
- [x] `FileStorage` + `AvatarServlet` (`@MultipartConfig`, 2 MB limit, JPG/PNG check including magic bytes, random file names, delete old avatar, stream bytes with correct `Content-Type` and cache headers).
- [x] `ListingService` + `ListingServlet` (`/skills`) + `my-skills.jsp`: offer/want/remove with `OfferedSkill.validate()`.
- [x] `AvailabilityService` + `AvailabilityServlet` + `availability.jsp` (weekly grid rendered with JSTL).
- [x] `ConnectorRule` data prerequisite: expose `countOfferedByUser`.

### Phase 6 — Discovery and matching engine
- [x] `MatchEngine` interface, `MatchResult`, `WeightedMatchEngine`, `RatingFirstMatchEngine`, `MatchEngineFactory` (reads `match.engine`).
- [x] Implement the scoring exactly as in `blueprint.md` section 8 using a bounded `PriorityQueue` for top-K.
- [x] `SearchService` + `BrowseServlet` + `browse.jsp`: filters (keyword, category, skill, level, min rating, day), sort (BEST/RATING/PRICE), pagination (10/page), provider cards with rating, price and badges.
- [x] `MatchService` + `MatchServlet` + `matches.jsp` (top 10 recommendations with score breakdown bars drawn with CSS).
- [x] `NoMatchFoundException` thrown when no candidates; the UI shows a friendly empty state.

### Phase 7 — Sessions and credit economy
- [x] `CreditService` (`hold`, `release`, `pay`, `adminAdjust`) with row locks in ascending user-id order and a ledger row for every balance change.
- [x] `SessionService.create/accept/reject/cancel/confirm/markNoShow` implementing the transaction recipes in `database-schema.md` section 5 and the credit table in `blueprint.md`.
- [x] A single private `transition(...)` method uses the guarded DAO update and `SessionStatus.canMoveTo`; invalid moves throw `InvalidStateTransitionException`.
- [x] Throw `DuplicateRequestException`, `InsufficientCreditsException`, `SchedulingConflictException`, `AuthorizationException` where specified.
- [x] `RequestServlet` (`/requests`, `/requests/new`, `/requests/view`, `/requests/respond`, `/cancel`, `/confirm`, `/noshow`) and JSPs `requests.jsp` (tabs: incoming/outgoing, status filter), `request-form.jsp`, `request-detail.jsp` (status timeline, action buttons shown only when legal).
- [x] `CreditServlet` + `credits.jsp` (balance, held credits = sum of costs of PENDING/ACCEPTED outgoing requests, paginated ledger).
- [x] Draft support: `DraftStore` (`ObjectOutputStream`/`ObjectInputStream` to `drafts.dir`), `DraftService`, "Save draft" button on the form, restore on load, delete on successful submit.

### Phase 8 — Background engine and notifications
- [x] `NotificationEvent`, `NotificationDispatcher` (daemon thread, `LinkedBlockingQueue`, poison-pill shutdown, log and continue on DB errors).
- [x] `NotificationService.notify(...)` publishes events; wire notifications into all `SessionService` transitions (request received/accepted/rejected/cancelled/expired/completed/no-show).
- [x] `NotificationServlet` + `notifications.jsp`; unread count shown in `nav.jspf` through a request attribute set by a small filter or by `NotificationService.unreadCount` in the servlet base class.
- [x] `SchedulerService` (`ScheduledExecutorService`, 2 threads) with `MatchScanTask`, `ExpiryTask`, `ReminderTask`, `AutoCompleteTask` per `blueprint.md` F-25 to F-28. Each task wraps its body in try/catch and logs.
- [x] `AppContextListener` starts the dispatcher and scheduler after the DB is ready and shuts them down gracefully on stop (`shutdown`, `awaitTermination(10s)`, then `shutdownNow`).
- [x] Add a clearly named debug config `scheduler.match.seconds=15` in a `demo` profile (second properties file) so the demo shows matches quickly.

### Phase 9 — Reviews, badges, messaging, leaderboard
- [ ] `ReviewService.submit` per the recipe (rating, stats update in one transaction), `ReviewServlet`, review form on `request-detail.jsp` after COMPLETED, reviews displayed on `public-profile.jsp`.
- [ ] `BadgeRule` interface and the six rules from `blueprint.md` section 9, `BadgeService.evaluate` (called after listing changes, completion, and reviews), badge notifications, badge display on profile/browse cards.
- [ ] `MessageService` + `MessageServlet` + `messages.jsp` (thread per request, participant check, mark-as-read, unread count in nav).
- [ ] `LeaderboardServlet` + `leaderboard.jsp` (top mentors by rating with min 3 reviews; by sessions taught).
- [ ] `ReportServlet` and report buttons on public profiles and reviews.

### Phase 10 — Dashboards, admin panel, analytics, exports
- [ ] `AnalyticsService.userDashboard` and `DashboardServlet` + `dashboard.jsp` (F-29).
- [ ] `AdminService`, `AdminServlet` and `admin/dashboard.jsp`, `admin/users.jsp`, `admin/catalog.jsp`, `admin/reports.jsp`:
  - analytics (F-33) with CSS bar charts (percentage widths computed in the servlet),
  - user search, activate/deactivate, manual credit adjustment with required reason,
  - catalog CRUD and CSV import via `CsvImporter` (refreshes `CatalogCache`),
  - report queue and review hiding (recomputes average).
- [ ] `Exporter` interface and `CsvExporter` (proper quoting/escaping, UTF-8 with BOM for Excel), `ExportServlet` (`/history/export`, `/admin/users/export`) with `Content-Disposition: attachment`.
- [ ] Show "users online now" from the `ServletContext` counter on the admin dashboard.

### Phase 11 — Hardening, tests, demo data, documentation
- [ ] Security pass: grep for any string-concatenated SQL; ensure every dynamic JSP value uses `<c:out>`; verify CSRF on all POST forms; verify authorization checks (a student cannot view another's request or messages by changing the `id`).
- [ ] Error handling pass: every servlet catches `SkillSwapException`; unexpected errors land on `error.jsp` and are logged at SEVERE.
- [ ] Add a `DemoDataLoader` (run only when `demo.data=true`) creating ~12 users, listings, availability, a few sessions in each status, reviews, and messages.
- [ ] Test pass: `mvn test` green; add missing tests for `SessionService` happy and failure paths; run the concurrency tests 20 times.
- [ ] Performance pass: confirm indexes are used (`EXPLAIN` on search and request listing queries) and pagination everywhere.
- [ ] UI pass: every page works at phone width; consistent empty states and flash messages.
- [ ] Documentation: `README.md` (prerequisites, DB setup, config keys, how to run on Tomcat, default admin login, demo script), a class diagram in text (mermaid or ASCII), ER diagram in text, and a one-page demo script for the review.
- [ ] Final `mvn clean package`; verify the WAR runs on a fresh Tomcat and an empty database.

---

## Decisions
1. **JDK and Build**: Eclipse Adoptium Temurin OpenJDK 17.0.20.1 installed and configured with `maven-compiler-plugin` release 17.
2. **Local Tooling**: Project-local Maven 3.9.9 and Tomcat 10.1.34 downloaded into `tools/` (git-ignored) with `run.bat` and `run.sh` automation scripts.
3. **Database**: MySQL 8.4 Server running locally via WSL on default port 3306 with databases `skillswap` and `skillswap_test`.

---

## Log
- **Phase 0 completed**: Installed and verified JDK 17, downloaded Maven 3.9.9 and Tomcat 10.1.34 into `tools/`, configured MySQL 8.4 with databases `skillswap` and `skillswap_test`. Initialized git repo, package tree, pom.xml, web.xml, config.properties, logging.properties, PingServlet, run scripts, and PROGRESS.md. Verified with `mvn clean package` and runtime deployment to Tomcat returning "SkillSwap OK" at `/ping`.
- **Phase 1 completed**: Installed native MySQL 8.4 via winget and initialized self-contained data directory in `tools/mysql-data`. Implemented Database connection pool with reflection Proxy, TransactionManager with ThreadLocal, DatabaseInitializer for schema.sql and seed.sql, AppContextListener, updated PingServlet to report category count. All 3 JUnit tests in DatabaseTest passed (`mvn test`). Tomcat runtime verified returning "SkillSwap OK - Categories: 8" at `/ping`.
- **Phase 2 completed**: Implemented all enums in `model.enums` including SessionStatus state machine (`isTerminal()`, `canMoveTo()`), all model entities (`User`, `Student`, `Admin`, `Category`, `Skill`, `SkillListing`, `OfferedSkill`, `WantedSkill`, `AvailabilitySlot`, `SessionRequest`, `CreditEntry`, `Review`, `Notification`, `Message`, `Badge`, `Report`, `RequestDraft`, `MatchResult`, `SessionUser`), full exception hierarchy with `ValidationException` field errors, and utilities (`PasswordHasher` with PBKDF2/120k iterations/constant-time check, `Validator`, `Flash`, `Paginator`, `DateUtil`, `Html`). All 7 tests in `DatabaseTest` and `DomainModelTest` passed.
- **Phase 3 completed**: Implemented all 12 DAO interfaces and SearchCriteria in `com.skillswap.dao` and their JDBC implementations in `com.skillswap.dao.jdbc` (`JdbcUserDao`, `JdbcCategoryDao`, `JdbcSkillDao`, `JdbcListingDao`, `JdbcAvailabilityDao`, `JdbcSessionRequestDao`, `JdbcCreditDao`, `JdbcReviewDao`, `JdbcNotificationDao`, `JdbcMessageDao`, `JdbcBadgeDao`, `JdbcReportDao`, `JdbcAnalyticsDao`). Configured test `config.properties` for `skillswap_test` schema. Implemented `TestData` fixture generator and `DaoIntegrationTest`. Verified all 12 tests passing in `mvn test` covering polymorphic listings, guarded state transitions, edge-case overlap checks, user CRUD, and availability checks.
- **Phase 4 completed**: Implemented `RegistrationForm`, `AuthService` (register with signup bonus in single tx, login with PBKDF2 verification, changePassword), updated `AppContextListener` with admin account seeding, created security filters (`EncodingFilter`, `SecurityHeadersFilter`, `CsrfFilter`, `AuthFilter`, `RoleFilter`), `ActiveUsersListener` session counter, `AuthServlet` (PRG pattern, session fixation protection via `changeSessionId`), `HomeServlet`, JSP pages (`header.jspf`, `nav.jspf`, `footer.jspf`, `home.jsp`, `login.jsp`, `register.jsp`, `password.jsp`, `403.jsp`, `404.jsp`, `error.jsp`), and master CSS `style.css`. All 17 tests passed in `mvn test`. Verified live deployment on Tomcat: landing page, guest redirect to login on `/dashboard`, 403 on missing CSRF token, successful user registration and login, and student access to `/admin` blocked with 403.
- **Phase 5 completed**: Implemented `CatalogCache` (thread-safe `TreeMap` guarded with `ReadWriteLock`), `CatalogService` (cached read methods, admin CRUD, CSV import), `FileStorage` (magic bytes verification for JPEG/PNG, 2MB size limit, UUID filenames, old avatar cleanup), `ProfileService` (profile updates, avatar upload, public profile data aggregation), `ListingService` (offer/want with duplicate prevention and bounds validation, remove, trigger `BadgeService`), `AvailabilityService` (weekly schedule, intra-day overlap rejection), all 6 `BadgeRule` strategy implementations and `BadgeService`, `AvatarServlet` (streaming image bytes with HTTP cache headers, SVG fallback), `ProfileServlet` (`/profile`, `/profile/avatar`, `/users/view`), `ListingServlet` (`/skills`), `AvailabilityServlet` (`/availability`), and JSPs (`profile.jsp`, `public-profile.jsp`, `my-skills.jsp`, `availability.jsp`). All 22 tests in `mvn test` passed. Deployed to Tomcat and verified live: user registration, login, profile view/edit, skill management, availability grid, and avatar streaming.
- **Phase 6 completed**: Implemented `MatchEngine` interface, `WeightedMatchEngine` (score = 0.5*relevance + 0.3*rating + 0.2*availability with PriorityQueue min-heap top-K, exact match 1.0 vs category 0.4, cold-start prior 0.6, learner/inactive exclusions), `RatingFirstMatchEngine`, and `MatchEngineFactory`. Implemented `SearchService` (with `ProviderCard` and pagination) and `MatchService`. Created `BrowseServlet` (`/browse`), `MatchServlet` (`/matches`), `browse.jsp` with responsive filters/cards/badges/sort/pagination, and `matches.jsp` with CSS score breakdown meters. All 28 tests in `mvn test` passed. Deployed to Tomcat and verified live: `/browse` and `/matches` render correctly with 200 OK.
- **Phase 7 completed**: Implemented `CreditService` (escrow `hold`, `release`, `pay`, `adminAdjust` with deadlock-safe ascending user ID locks and signed `credits_ledger` audit rows), `SessionService` (full transaction recipes, guarded status transitions, conflict and duplicate validation, credit table rules for reject/cancel/no-show, auto-completion and auto-expiry), `DraftStore` (disk serialization via ObjectOutputStream) and `DraftService`, `RequestServlet` (`/requests`, `/requests/new`, `/requests/view`, `/requests/respond`, `/cancel`, `/confirm`, `/noshow`, `/requests/draft`), `CreditServlet` (`/credits`), and JSPs (`requests.jsp` with tabs/filters, `request-form.jsp` with draft recovery, `request-detail.jsp` with visual stepper and role-based action buttons, and `credits.jsp` with balance meters and paginated audit ledger). All 38 JUnit tests passed (`mvn test`), including multi-threaded race conditions (double accept and balance overdraft prevention) and full credit table lifecycle tests. Deployed to Tomcat and verified live: `/requests` and `/credits` with user registration and session escrow.
- **Phase 8 completed**: Implemented multithreaded asynchronous background engine and notifications: `NotificationEvent`, `NotificationDispatcher` (daemon thread with `LinkedBlockingQueue`, poison-pill termination, and graceful shutdown queue draining), `NotificationService` (wired into all session lifecycle state transitions and badge awards), `SchedulerService` (`ScheduledExecutorService` with 2 daemon threads managing `MatchScanTask` [F-25], `ExpiryTask` [F-26], `ReminderTask` [F-27], and `AutoCompleteTask` [F-28]), lifecycle wiring in `AppContextListener`, demo profile `config-demo.properties` (`scheduler.match.seconds=15`), unread notification badge in `AuthFilter` and `nav.jspf`, `NotificationServlet` (`/notifications`, `/notifications/read-all`, `/notifications/read`), and `notifications.jsp` with unread highlights and actions. All 44 tests passed (`mvn test`), including async dispatcher processing, queue drain on shutdown, match scanning, reminder alerts, request auto-expiry with refund, and task idempotency. Deployed to Tomcat and verified live: `/notifications` returns 200 OK with clean thread startup and shutdown.


