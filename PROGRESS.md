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
- [ ] `AuthService.register/login/changePassword`; grant the signup bonus (ledger `SIGNUP_BONUS`) inside one transaction; enforce `allowed.email.domain` when set.
- [ ] `AppContextListener` seeds the admin account from config if none exists.
- [ ] Filters: `EncodingFilter` (UTF-8), `AuthFilter` (redirect guests to `/login`, remember requested URL), `RoleFilter` (admin-only paths under `/admin/*`), `CsrfFilter` (token in session, hidden field on every POST form), `SecurityHeadersFilter`.
- [ ] `ActiveUsersListener` (`HttpSessionListener`) maintaining an `AtomicInteger` of active sessions in the `ServletContext`.
- [ ] `AuthServlet` (`/register`, `/login`, `/logout`, `/password`) using Post-Redirect-Get and `Flash`. Store a `SessionUser` in the session; call `request.changeSessionId()` on login (session fixation protection).
- [ ] Build `layout/header.jspf`, `nav.jspf` (role-aware), `footer.jspf`, `home.jsp`, `login.jsp`, `register.jsp`, `password.jsp`, `403.jsp`, `404.jsp`, `error.jsp`.
- [ ] Write `assets/css/style.css` with the design tokens and components from `blueprint.md` section 13.

### Phase 5 — Profile, skills, availability, avatar
- [ ] `CatalogCache` (`TreeMap<String, List<Skill>>` + `ReadWriteLock`) and `CatalogService` read methods.
- [ ] `ProfileService`, `ProfileServlet` (`/profile`, `/profile/avatar`, `/users/view`), `profile.jsp`, `public-profile.jsp`.
- [ ] `FileStorage` + `AvatarServlet` (`@MultipartConfig`, 2 MB limit, JPG/PNG check including magic bytes, random file names, delete old avatar, stream bytes with correct `Content-Type` and cache headers).
- [ ] `ListingService` + `ListingServlet` (`/skills`) + `my-skills.jsp`: offer/want/remove with `OfferedSkill.validate()`.
- [ ] `AvailabilityService` + `AvailabilityServlet` + `availability.jsp` (weekly grid rendered with JSTL).
- [ ] `ConnectorRule` data prerequisite: expose `countOfferedByUser`.

### Phase 6 — Discovery and matching engine
- [ ] `MatchEngine` interface, `MatchResult`, `WeightedMatchEngine`, `RatingFirstMatchEngine`, `MatchEngineFactory` (reads `match.engine`).
- [ ] Implement the scoring exactly as in `blueprint.md` section 8 using a bounded `PriorityQueue` for top-K.
- [ ] `SearchService` + `BrowseServlet` + `browse.jsp`: filters (keyword, category, skill, level, min rating, day), sort (BEST/RATING/PRICE), pagination (10/page), provider cards with rating, price and badges.
- [ ] `MatchService` + `MatchServlet` + `matches.jsp` (top 10 recommendations with score breakdown bars drawn with CSS).
- [ ] `NoMatchFoundException` thrown when no candidates; the UI shows a friendly empty state.

### Phase 7 — Sessions and credit economy
- [ ] `CreditService` (`hold`, `release`, `pay`, `adminAdjust`) with row locks in ascending user-id order and a ledger row for every balance change.
- [ ] `SessionService.create/accept/reject/cancel/confirm/markNoShow` implementing the transaction recipes in `database-schema.md` section 5 and the credit table in `blueprint.md`.
- [ ] A single private `transition(...)` method uses the guarded DAO update and `SessionStatus.canMoveTo`; invalid moves throw `InvalidStateTransitionException`.
- [ ] Throw `DuplicateRequestException`, `InsufficientCreditsException`, `SchedulingConflictException`, `AuthorizationException` where specified.
- [ ] `RequestServlet` (`/requests`, `/requests/new`, `/requests/view`, `/requests/respond`, `/cancel`, `/confirm`, `/noshow`) and JSPs `requests.jsp` (tabs: incoming/outgoing, status filter), `request-form.jsp`, `request-detail.jsp` (status timeline, action buttons shown only when legal).
- [ ] `CreditServlet` + `credits.jsp` (balance, held credits = sum of costs of PENDING/ACCEPTED outgoing requests, paginated ledger).
- [ ] Draft support: `DraftStore` (`ObjectOutputStream`/`ObjectInputStream` to `drafts.dir`), `DraftService`, "Save draft" button on the form, restore on load, delete on successful submit.

### Phase 8 — Background engine and notifications
- [ ] `NotificationEvent`, `NotificationDispatcher` (daemon thread, `LinkedBlockingQueue`, poison-pill shutdown, log and continue on DB errors).
- [ ] `NotificationService.notify(...)` publishes events; wire notifications into all `SessionService` transitions (request received/accepted/rejected/cancelled/expired/completed/no-show).
- [ ] `NotificationServlet` + `notifications.jsp`; unread count shown in `nav.jspf` through a request attribute set by a small filter or by `NotificationService.unreadCount` in the servlet base class.
- [ ] `SchedulerService` (`ScheduledExecutorService`, 2 threads) with `MatchScanTask`, `ExpiryTask`, `ReminderTask`, `AutoCompleteTask` per `blueprint.md` F-25 to F-28. Each task wraps its body in try/catch and logs.
- [ ] `AppContextListener` starts the dispatcher and scheduler after the DB is ready and shuts them down gracefully on stop (`shutdown`, `awaitTermination(10s)`, then `shutdownNow`).
- [ ] Add a clearly named debug config `scheduler.match.seconds=15` in a `demo` profile (second properties file) so the demo shows matches quickly.

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
