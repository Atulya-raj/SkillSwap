# SkillSwap — Master Blueprint

> Read this file first. Then read `database-schema.md`, then execute `build-phases.md` one phase at a time.
> Project: **SkillSwap**, a peer-to-peer skill exchange web app for a college campus, built for the Java project lab (CSE1007).
> Team: Atulya Raj (24BBS0059), Tarun (24BBS0181), Atharva Aggarwal (24BBS0194).

---

## 0. Instructions for the IDE agent (Antigravity)

1. **Java only.** The only languages in the repo are Java, JSP/JSTL (Java's view technology), SQL, HTML, CSS, and XML/properties for config. No Node, no npm, no JS frameworks, no Python, no Spring, no Hibernate, no Lombok.
2. **Work one phase at a time** from `build-phases.md`. At the end of each phase: run the "Done when" checks, tick the boxes in `PROGRESS.md` (create it in Phase 0), print a 5-line summary, and **stop and wait** for the user to say "next phase".
3. **Do not add any dependency** outside the list in section 3. If you believe one is needed, stop and ask.
4. **Do not skip layers.** Servlet -> Service -> DAO -> Database. Servlets never touch JDBC. JSPs never contain business logic or scriptlets.
5. When a file in this blueprint is ambiguous, prefer the simpler option and note the decision in `PROGRESS.md` under "Decisions".
6. Every public class and method gets a short Javadoc line. Every phase ends with the project compiling (`mvn clean package` succeeds).

---

## 1. What SkillSwap does

Students list skills they can **teach** and skills they want to **learn**. A matching engine pairs them. Sessions are paid for with **credits**, not money: you earn credits by teaching and spend them learning. Credits are held in escrow when a session is requested and released when the session completes. After a session both sides rate each other, which feeds the matching score. A background engine (separate threads) finds matches, sends notifications, expires stale requests, and sends reminders.

**Problem it solves:** campus skills are scattered across WhatsApp groups, unverified, and unaccountable. SkillSwap adds discovery, trust (ratings, badges), a cashless exchange, and accountability (no-show tracking).

---

## 2. Hard constraints

| Constraint | Detail |
|---|---|
| Language | Java 17 (LTS) |
| Web | Jakarta Servlet 6.0 + JSP 3.1 + JSTL 3.0 on Apache Tomcat 10.1 (**`jakarta.*` imports, never `javax.servlet.*`**) |
| Persistence | Plain **JDBC** + MySQL 8. `PreparedStatement` only, never string-concatenated SQL |
| Build | Maven, WAR packaging (Maven is the standard Java build tool; no other build tooling) |
| Front end | Server-rendered JSP + plain HTML5/CSS3. No JavaScript is required for any feature. No CSS frameworks (no Bootstrap/Tailwind) |
| Logging | `java.util.logging` only (file handler + console) |
| Password hashing | `PBKDF2WithHmacSHA256` via `javax.crypto` (JDK built-in), 120k+ iterations, per-user random salt |
| Testing | JUnit 5 |
| Connection pool | Hand-written (see 4.4). No HikariCP/DBCP |

---

## 3. Tech stack and exact dependencies

| Purpose | Artifact | Scope |
|---|---|---|
| Servlet API | `jakarta.servlet:jakarta.servlet-api:6.0.0` | provided |
| JSP API | `jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.1` | provided |
| JSTL API | `jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0` | compile |
| JSTL impl | `org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1` | compile |
| MySQL driver | `com.mysql:mysql-connector-j:8.x` (latest stable 8) | compile |
| Tests | `org.junit.jupiter:junit-jupiter:5.10.x` | test |

Plugins: `maven-compiler-plugin` (release 17), `maven-war-plugin`, `maven-surefire-plugin`.
Runtime: Tomcat 10.1.x, MySQL 8.0.x. Maven coordinates: `com.skillswap:skillswap:1.0.0`, packaging `war`.

---

## 4. Architecture

### 4.1 Layers

```
Browser
  |  HTTP request / response
JSP views (WEB-INF/jsp, JSTL + EL only)
  |  forward / redirect (Post-Redirect-Get)
Filters (encoding, auth, role, CSRF, security headers)
  |
Servlets (controllers: parse input, call service, choose view)
  |
Services (business rules, transactions, calls MatchEngine, queues notifications)
  |
DAOs (JDBC, one per table group)
  |
MySQL

Side threads (started by AppContextListener):
  NotificationDispatcher (BlockingQueue consumer)
  SchedulerService (ScheduledExecutorService: match scan, expiry, reminders, auto-complete)
```

### 4.2 Package structure

```
skillswap/
  pom.xml
  PROGRESS.md
  blueprint.md  database-schema.md  build-phases.md
  src/main/java/com/skillswap/
    model/            User(abstract), Student, Admin, Category, Skill, SkillListing(abstract),
                      OfferedSkill, WantedSkill, AvailabilitySlot, SessionRequest, CreditEntry,
                      Review, Notification, Message, Badge, Report, RequestDraft(Serializable)
    model/enums/      Role, ListingType, SkillLevel, SessionStatus, SessionMode, LedgerType,
                      NotificationType, ReportStatus
    exception/        SkillSwapException(checked base) and subclasses (see 4.5), DataAccessException(unchecked)
    dao/              interfaces: UserDao, CategoryDao, SkillDao, ListingDao, AvailabilityDao,
                      SessionRequestDao, CreditDao, ReviewDao, NotificationDao, MessageDao,
                      BadgeDao, ReportDao, AnalyticsDao
    dao/jdbc/         JdbcUserDao ... (one implementation per interface)
    db/               Database (pool + ThreadLocal tx), PooledConnectionFactory, TransactionManager,
                      TxWork, DatabaseInitializer
    service/          AuthService, ProfileService, CatalogService, ListingService, AvailabilityService,
                      SearchService, MatchService, SessionService, CreditService, ReviewService,
                      NotificationService, MessageService, BadgeService, AnalyticsService,
                      AdminService, ExportService, DraftService
    matching/         MatchEngine(interface), MatchResult, WeightedMatchEngine, RatingFirstMatchEngine,
                      MatchEngineFactory, CatalogCache
    badge/            BadgeRule(interface), FirstSwapRule, Mentor10Rule, EternalLearnerRule,
                      PerfectFiveRule, ConnectorRule, ReliableRule
    concurrent/       NotificationDispatcher, NotificationEvent, SchedulerService,
                      MatchScanTask, ExpiryTask, ReminderTask, AutoCompleteTask
    io/               Exporter(interface), CsvExporter, CsvImporter, FileStorage, DraftStore
    web/servlet/      one servlet per area (see section 6 route table)
    web/filter/       EncodingFilter, AuthFilter, RoleFilter, CsrfFilter, SecurityHeadersFilter
    web/listener/     AppContextListener, ActiveUsersListener(HttpSessionListener)
    util/             Config, PasswordHasher, Validator, Flash, Paginator, DateUtil, Html
  src/main/resources/
    config.properties
    db/schema.sql  db/seed.sql
    logging.properties
  src/main/webapp/
    WEB-INF/web.xml
    WEB-INF/jsp/layout/header.jspf  footer.jspf  nav.jspf
    WEB-INF/jsp/...pages (see section 7)
    assets/css/style.css
  src/test/java/com/skillswap/...
```

### 4.3 Request flow (every feature follows this)

1. Browser sends request -> filters run (encoding, auth, CSRF, headers).
2. Servlet reads/validates parameters (`Validator`), gets the current user from `HttpSession`.
3. Servlet calls a **service** method. Services own business rules and transactions.
4. Service uses **DAOs** (and `TransactionManager` for multi-step writes).
5. Servlet sets request attributes and **forwards** to a JSP (GET) or **redirects** (POST, Post-Redirect-Get) with a flash message.
6. JSP renders with JSTL/EL and `<c:out>` for all user-supplied text (XSS safety).

### 4.4 Database access design (Java-only, hand-written)

- `Database` holds an `ArrayBlockingQueue<Connection>` pool. Size and timeout come from `config.properties`.
- Pooled connections are handed out as `java.lang.reflect.Proxy` instances of `Connection` whose `close()` returns the raw connection to the pool instead of closing it.
- `TransactionManager.execute(TxWork<T> work)` borrows one connection, sets `autoCommit=false`, stores it in a `ThreadLocal<Connection>`, runs the work, commits (or rolls back on any exception), clears the ThreadLocal, and returns the connection to the pool.
- DAOs call `Database.acquire()`. If the current thread has a transaction connection, they join it (and must not close it); otherwise they borrow a fresh one and close it (return to pool) at the end of the method. Use try-with-resources.
- Credit-moving code locks rows with `SELECT ... FOR UPDATE`, always in ascending `user_id` order to avoid deadlocks.
- Status changes use a guarded update: `UPDATE session_requests SET status=? WHERE id=? AND status=?` and check the affected row count (prevents double-accept races).

### 4.5 Exception hierarchy

```
Exception
  SkillSwapException (checked, abstract-ish base, has user-friendly message)
    ValidationException            (bad input; carries Map<String,String> fieldErrors)
    AuthenticationException        (bad login, inactive account)
    AuthorizationException         (wrong role / not your resource)
    ResourceNotFoundException
    DuplicateRequestException      (same learner+provider+skill already PENDING/ACCEPTED)
    InsufficientCreditsException
    NoMatchFoundException
    InvalidStateTransitionException
    SchedulingConflictException    (overlapping accepted session)
RuntimeException
  DataAccessException (unchecked wrapper around SQLException)
```

Servlets catch `SkillSwapException` and show a flash error; anything else goes to the `error-page` mapped in `web.xml` (`error.jsp`, `404.jsp`).

---

## 5. Roles and permissions

| Capability | Guest | STUDENT | ADMIN |
|---|---|---|---|
| View landing, register, login | yes | - | - |
| Browse/search providers, view public profiles | no | yes | yes |
| Manage own profile, skills, availability | no | yes | yes |
| Request, respond to, cancel, confirm sessions | no | yes | no |
| Review, message, report | no | yes | no |
| Admin panel (users, catalog, reports, analytics, credit adjust) | no | no | yes |

---

## 6. Feature catalogue

Each feature has an ID. Implement exactly these; do not invent extras.

### Accounts and profile
- **F-01 Register.** Name, email, password, department, year. Email unique, password >= 8 chars with a letter and a digit. Optional email-domain restriction via `allowed.email.domain` (blank = allow any). New users get `signup.bonus` credits (ledger entry `SIGNUP_BONUS`).
- **F-02 Login / logout.** Session created on login (`HttpSession` stores a lightweight `SessionUser`: id, name, role, avatar). Session timeout 30 min. Logout invalidates the session. `last_login_at` updated.
- **F-03 Change password.** Requires current password.
- **F-04 Profile.** Edit name, department, year, bio. Public profile page shows skills, rating, badges, recent reviews.
- **F-05 Avatar upload.** Multipart upload (`@MultipartConfig`), JPG/PNG only, max 2 MB, stored on disk under `upload.dir`, served by an `AvatarServlet`. Old file deleted on replace.

### Skills and discovery
- **F-06 Skill catalog.** Categories and skills managed by admin (F-30).
- **F-07 Offer a skill.** Pick a skill, level (BEGINNER/INTERMEDIATE/ADVANCED), `credits_per_hour` (1 to 5), short description. A user cannot offer the same skill twice.
- **F-08 Want a skill.** Pick a skill and the level they want to learn at.
- **F-09 Weekly availability.** Add/remove slots (day of week, start, end; end after start; no overlaps within a day).
- **F-10 Browse and search providers.** Filters: keyword, category, skill, level, min rating, day-of-week availability. Sort by best match / rating / lowest price. Pagination, 10 per page.
- **F-11 Recommended matches.** `/matches` shows top providers for the user's wanted skills using the match engine (section 8).

### Sessions and credits
- **F-12 Request a session.** Choose a provider's offered skill, proposed start (>= 2h in the future, <= 30 days), duration (30/60/90/120 min), mode (ONLINE with link / IN_PERSON with place), message. Cost = `ceil(credits_per_hour * minutes / 60)`. Credits are **held** immediately.
- **F-13 Respond.** Provider accepts or rejects a PENDING request. Accepting fails if the provider has an overlapping ACCEPTED session. Rejecting releases the hold.
- **F-14 Cancel.** Learner can cancel PENDING (full release) or ACCEPTED (see cancellation rules below). Provider can cancel ACCEPTED (full release to learner).
- **F-15 Confirm completion.** After the start time, both sides confirm. When both have confirmed, status becomes COMPLETED and credits are paid to the provider.
- **F-16 No-show reporting.** 15+ minutes after start, either side may mark the other as no-show. Learner no-show: provider is paid. Provider no-show: learner refunded and provider gets a reliability strike.
- **F-17 Credit wallet.** Balance, held credits, full ledger with pagination.
- **F-18 Draft requests.** "Save draft" on the request form serializes a `RequestDraft` object to disk; the form restores it next time and deletes it on submit.

### Trust and engagement
- **F-19 Reviews.** After COMPLETED, each side can leave one rating (1 to 5) plus optional comment about the other. Averages are updated in the same transaction.
- **F-20 Badges.** Awarded automatically by `BadgeRule` implementations after relevant events (see section 9).
- **F-21 Leaderboard.** Top mentors by rating (min 3 reviews) and by sessions taught.
- **F-22 In-app messaging.** A message thread per session request between the two participants. Unread counts shown.
- **F-23 Notifications.** In-app bell with unread count and a notifications page. Created asynchronously by `NotificationDispatcher`.
- **F-24 Report.** Report a user or a review with a reason; goes to the admin queue.

### Background engine (multithreading)
- **F-25 Match scan task.** Every `scheduler.match.seconds`: finds offered listings created since the last scan, matches them to wanted listings, notifies the learner ("New match: X teaches Y").
- **F-26 Expiry task.** PENDING requests older than `request.expiry.hours` become EXPIRED and holds are released.
- **F-27 Reminder task.** ACCEPTED sessions starting within 60 minutes (and `reminder_sent=false`) notify both users.
- **F-28 Auto-complete task.** If one side confirmed and the other did not within 48h, the session completes automatically.

### Dashboards, admin, files
- **F-29 User dashboard.** Credit balance and held credits, earned vs spent, sessions taught/learned, rating, upcoming sessions, pending incoming requests, top 3 recommendations, unread notifications, badges.
- **F-30 Admin catalog.** CRUD for categories and skills; bulk import skills from a CSV upload (`category,skill`).
- **F-31 Admin users.** List/search users, activate/deactivate, manual credit adjustment (ledger `ADMIN_ADJUST`, reason required).
- **F-32 Admin moderation.** Report queue (resolve/dismiss), hide a review (recomputes the average).
- **F-33 Admin analytics.** Total users, users online now, sessions by status, completion rate, top 10 demanded and top 10 offered skills, total credits in circulation, new users per week for the last 8 weeks (rendered as CSS bars, no JS).
- **F-34 CSV export.** A user exports their session history as CSV via `Exporter`/`CsvExporter`. Admin exports the user list.
- **F-35 Users online.** `ActiveUsersListener` counts active sessions with an `AtomicInteger` in the `ServletContext`.

### Cancellation and credit rules

| Event | Learner credits | Provider credits |
|---|---|---|
| Request created | `-cost` (HOLD) | 0 |
| Provider rejects / request expires / learner cancels PENDING | `+cost` (RELEASE) | 0 |
| Provider cancels ACCEPTED | `+cost` (RELEASE) | 0 |
| Learner cancels ACCEPTED, 2h or more before start | `+cost` (RELEASE) | 0 |
| Learner cancels ACCEPTED, less than 2h before start | `+cost/2` rounded down (RELEASE) | `+cost - refund` (EARN) |
| Session COMPLETED | 0 | `+cost` (EARN) |
| Learner no-show | 0 | `+cost` (EARN) |
| Provider no-show | `+cost` (RELEASE) | 0, plus `reliability_strikes + 1` |

### Session state machine

```
PENDING   -> ACCEPTED | REJECTED | CANCELLED | EXPIRED
ACCEPTED  -> COMPLETED | CANCELLED | NO_SHOW_LEARNER | NO_SHOW_PROVIDER
Terminal: REJECTED, CANCELLED, EXPIRED, COMPLETED, NO_SHOW_LEARNER, NO_SHOW_PROVIDER
```
Any other transition throws `InvalidStateTransitionException`. All transitions go through one `SessionService.transition(...)` method.

---

## 7. Routes and pages

One servlet per area, using `@WebServlet("/area/*")` and `getPathInfo()`.

| Path | Method | Role | Servlet | View |
|---|---|---|---|---|
| `/` | GET | any | HomeServlet | `home.jsp` |
| `/register` | GET/POST | guest | AuthServlet | `register.jsp` |
| `/login` | GET/POST | guest | AuthServlet | `login.jsp` |
| `/logout` | POST | user | AuthServlet | redirect |
| `/password` | GET/POST | user | AuthServlet | `password.jsp` |
| `/dashboard` | GET | student | DashboardServlet | `dashboard.jsp` |
| `/profile` | GET/POST | user | ProfileServlet | `profile.jsp` |
| `/profile/avatar` | POST | user | ProfileServlet | redirect |
| `/avatar?id=` | GET | user | AvatarServlet | image bytes |
| `/users/view?id=` | GET | user | ProfileServlet | `public-profile.jsp` |
| `/skills` | GET/POST | student | ListingServlet | `my-skills.jsp` |
| `/availability` | GET/POST | student | AvailabilityServlet | `availability.jsp` |
| `/browse` | GET | user | BrowseServlet | `browse.jsp` |
| `/matches` | GET | student | MatchServlet | `matches.jsp` |
| `/requests` | GET | student | RequestServlet | `requests.jsp` |
| `/requests/new` | GET/POST | student | RequestServlet | `request-form.jsp` |
| `/requests/view?id=` | GET | student | RequestServlet | `request-detail.jsp` |
| `/requests/respond` `/cancel` `/confirm` `/noshow` | POST | student | RequestServlet | redirect |
| `/requests/draft` | POST | student | RequestServlet | redirect |
| `/reviews/new` | POST | student | ReviewServlet | redirect |
| `/messages?requestId=` | GET/POST | student | MessageServlet | `messages.jsp` |
| `/notifications` | GET/POST | user | NotificationServlet | `notifications.jsp` |
| `/credits` | GET | student | CreditServlet | `credits.jsp` |
| `/history/export` | GET | student | ExportServlet | CSV download |
| `/leaderboard` | GET | user | LeaderboardServlet | `leaderboard.jsp` |
| `/report` | POST | student | ReportServlet | redirect |
| `/admin` | GET | admin | AdminServlet | `admin/dashboard.jsp` |
| `/admin/users` (+`/toggle`, `/credits`, `/export`) | GET/POST | admin | AdminServlet | `admin/users.jsp` |
| `/admin/catalog` (+`/import`) | GET/POST | admin | AdminServlet | `admin/catalog.jsp` |
| `/admin/reports` (+`/resolve`, `/hide-review`) | GET/POST | admin | AdminServlet | `admin/reports.jsp` |

Extra pages: `error.jsp`, `404.jsp`, `403.jsp`.
All JSPs live under `WEB-INF/jsp/` (not directly reachable); servlets forward to them.
Shared fragments: `layout/header.jspf` (head, CSS), `layout/nav.jspf` (role-aware nav with notification badge), `layout/footer.jspf`.

---

## 8. Matching engine

```java
public interface MatchEngine {
    List<MatchResult> findProviders(long learnerId, long skillId, int topK) throws NoMatchFoundException;
    List<MatchResult> recommendFor(long learnerId, int topK);
}
```

**`WeightedMatchEngine`** (default):
`score = 0.5 * skillRelevance + 0.3 * ratingScore + 0.2 * availabilityOverlap`

- `skillRelevance`: 1.0 exact skill match; 0.4 same category, different skill; otherwise excluded.
- `ratingScore`: `avg_rating / 5.0`; providers with fewer than 3 ratings use a neutral prior of 0.6.
- `availabilityOverlap`: overlapping minutes between learner and provider weekly slots divided by 300, capped at 1.0.
- Exclude: the learner, inactive users, listings that are inactive.
- Use a bounded min-heap `PriorityQueue<MatchResult>` to keep the top-K; return sorted descending.

**`RatingFirstMatchEngine`**: same candidate set, ranks by rating then price (second implementation to demonstrate polymorphism). Selected through `match.engine=weighted|rating` in config via `MatchEngineFactory`.

**`CatalogCache`**: a `TreeMap<String, List<Skill>>` (category name -> skills) built at startup and refreshed after admin catalog changes; access guarded with a `ReadWriteLock`.

---

## 9. Badges (Strategy pattern)

```java
public interface BadgeRule { String code(); boolean qualifies(User user, BadgeStats stats); }
```
`BadgeService.evaluate(userId)` loads stats and runs every rule, awarding missing badges and sending a notification.

| Code | Rule |
|---|---|
| FIRST_SWAP | first COMPLETED session as either side |
| MENTOR_10 | taught 10 sessions and avg rating >= 4.5 |
| ETERNAL_LEARNER | learned in 10 sessions |
| PERFECT_FIVE | five 5-star reviews received |
| CONNECTOR | offers 3 or more skills |
| RELIABLE | 5 completed sessions with 0 reliability strikes |

---

## 10. Concurrency design

- **`NotificationDispatcher`**: a single daemon thread consuming a `LinkedBlockingQueue<NotificationEvent>`. Services call `dispatcher.publish(event)`; the thread writes to the `notifications` table. Poison-pill shutdown.
- **`SchedulerService`**: `ScheduledExecutorService` with 2 threads running the four tasks (F-25 to F-28). Each task catches and logs all exceptions so one failure never kills the schedule. Shared state (last scan timestamp) is an `AtomicReference<LocalDateTime>`.
- **`AppContextListener`**: on `contextInitialized` loads config, builds the pool, runs `DatabaseInitializer` (if `db.autoinit=true`), seeds the admin account, builds `CatalogCache`, starts the dispatcher and scheduler. On `contextDestroyed` shuts both down gracefully (`shutdown()` then `awaitTermination`) and closes the pool.
- **Thread safety rules**: services are stateless singletons; no mutable static fields except those wrapped in atomics/concurrent collections; no `SimpleDateFormat` fields (use `java.time`).

---

## 11. File handling and serialization (explicit syllabus items)

| Use | Where |
|---|---|
| Read `config.properties` | `Config` |
| Avatar upload and serving | `FileStorage`, `AvatarServlet` |
| Session history / user list CSV export | `Exporter` interface, `CsvExporter` |
| Skill catalog CSV import | `CsvImporter` |
| Application log files | `logging.properties` with `FileHandler` |
| **Serialization**: `RequestDraft implements Serializable` saved/loaded with `ObjectOutputStream`/`ObjectInputStream` | `DraftStore`, `DraftService` |

---

## 12. Syllabus coverage map (for your reviews and viva)

| Topic from your notes | Where it lives |
|---|---|
| OOP, Inheritance | `User -> Student/Admin`; `SkillListing -> OfferedSkill/WantedSkill`; exception hierarchy |
| Polymorphism | `SkillListing.describe()/validate()` overrides; `MatchEngine` and `BadgeRule` implementations; `Exporter` |
| Interface | `MatchEngine`, `BadgeRule`, `Exporter`, `Notifiable` (implemented by `Student`), all DAO interfaces |
| Exception handling | Section 4.5, `TransactionManager` rollback, global error pages |
| Collections | `PriorityQueue` top-K, `TreeMap` catalog index, `HashMap` field errors, `ConcurrentHashMap`, `ArrayBlockingQueue`, `LinkedBlockingQueue`, `EnumMap` for status counts |
| File handling | Section 11 |
| Serialization | `RequestDraft` |
| MySQL DB | `database-schema.md` |
| JDBC | `db/` package + `dao/jdbc/` |
| User interface | JSP pages + `style.css` |
| Web app: JSP | Section 7 |
| Session management | `HttpSession`, `AuthFilter`, session timeout, flash messages, `ActiveUsersListener` |
| Request and response | Servlet flow (4.3), PRG pattern, CSV download response headers, multipart upload |
| Multithreading | Section 10 |

---

## 13. UI and design notes

- Server-rendered pages, mobile-friendly (single CSS file, CSS Grid/Flexbox, no framework).
- Palette (matches the review PPT): indigo `#2D2A5B` (primary), dark indigo `#1E1C42`, amber `#F4A300` (accent), teal `#1C7293` (secondary), off-white `#F7F7F4` (background), text `#242131`, muted `#6B6B76`. Define them as CSS custom properties in `:root`.
- Fonts: system font stack for body, `Cambria, Georgia, serif` for headings.
- Components in CSS: top nav, cards, forms, buttons (primary/secondary/danger), badges, pills for status, tables, pagination, flash messages, empty states, progress bars (for analytics).
- Every form has labels, inline field errors from `ValidationException.fieldErrors`, and a hidden CSRF token.
- No inline `<script>`. Destructive actions use a separate confirmation page or a clearly labeled danger button, not JS `confirm()`.

---

## 14. Non-functional requirements

- **Security:** PreparedStatements everywhere; `<c:out>` for all dynamic text; CSRF token on every POST (stored in session, checked by `CsrfFilter`); `AuthFilter`/`RoleFilter` on protected paths; session cookie `HttpOnly` and `SameSite=Lax` in `web.xml`; security headers filter (`X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`); upload validated by extension, size and magic bytes; never log passwords.
- **Reliability:** all multi-step writes (request, respond, complete, cancel, review) run inside `TransactionManager`.
- **Performance:** indexes in schema; pagination on lists; `CatalogCache` for the catalog.
- **Logging:** INFO for lifecycle events, WARNING for handled business errors, SEVERE for unexpected exceptions with stack traces.
- **Config:** every tunable (pool size, bonus, expiry hours, scheduler intervals, directories) lives in `config.properties`.

`config.properties` keys:
```
db.url=jdbc:mysql://localhost:3306/skillswap?useSSL=false&serverTimezone=Asia/Kolkata
db.user=root
db.password=change-me
db.autoinit=true
pool.size=10
pool.timeout.ms=5000
signup.bonus=10
allowed.email.domain=
admin.email=admin@skillswap.local
admin.password=ChangeMe123
upload.dir=${user.home}/skillswap/uploads
drafts.dir=${user.home}/skillswap/drafts
match.engine=weighted
request.expiry.hours=48
scheduler.match.seconds=60
scheduler.expiry.minutes=5
scheduler.reminder.minutes=5
scheduler.autocomplete.minutes=10
```

---

## 15. Out of scope (do not build)

Payments with real money, video calling, mobile apps, email/SMS sending, OAuth/social login, any JavaScript framework, WebSockets, microservices, Docker.