# SkillSwap — Database and Domain Specification

> Companion to `blueprint.md`. Contains the complete MySQL schema, seed data, entity classes, DAO interfaces and service methods.
> Save the SQL below as `src/main/resources/db/schema.sql` and `seed.sql`. `DatabaseInitializer` runs them at startup when `db.autoinit=true`.

---

## 1. Schema (`schema.sql`)

```sql
CREATE DATABASE IF NOT EXISTS skillswap CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE skillswap;

CREATE TABLE IF NOT EXISTS users (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  full_name           VARCHAR(100) NOT NULL,
  email               VARCHAR(150) NOT NULL UNIQUE,
  password_hash       VARCHAR(255) NOT NULL,          -- format: iterations:saltBase64:hashBase64
  role                ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
  department          VARCHAR(100),
  study_year          TINYINT,
  bio                 VARCHAR(500),
  avatar_path         VARCHAR(255),
  credits             INT NOT NULL DEFAULT 0,
  avg_rating          DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  rating_count        INT NOT NULL DEFAULT 0,
  sessions_taught     INT NOT NULL DEFAULT 0,
  sessions_learned    INT NOT NULL DEFAULT 0,
  reliability_strikes INT NOT NULL DEFAULT 0,
  is_active           BOOLEAN NOT NULL DEFAULT TRUE,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at       DATETIME NULL,
  CONSTRAINT chk_credits CHECK (credits >= 0)
);

CREATE TABLE IF NOT EXISTS categories (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS skills (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  category_id INT NOT NULL,
  name        VARCHAR(80) NOT NULL,
  UNIQUE KEY uq_skill (category_id, name),
  CONSTRAINT fk_skill_cat FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);

-- One row per skill a user OFFERS or WANTS. 'type' is the discriminator for OfferedSkill / WantedSkill.
CREATE TABLE IF NOT EXISTS user_skills (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id          BIGINT NOT NULL,
  skill_id         INT NOT NULL,
  type             ENUM('OFFERED','WANTED') NOT NULL,
  level            ENUM('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL,
  credits_per_hour TINYINT NULL,                       -- 1..5, required when type = OFFERED
  description      VARCHAR(300),
  is_active        BOOLEAN NOT NULL DEFAULT TRUE,
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_user_skill_type (user_id, skill_id, type),
  KEY idx_skill_type (skill_id, type, is_active),
  CONSTRAINT fk_us_user  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
  CONSTRAINT fk_us_skill FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE,
  CONSTRAINT chk_cph CHECK (credits_per_hour IS NULL OR credits_per_hour BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS availability_slots (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  day_of_week TINYINT NOT NULL,                        -- 1 = Monday ... 7 = Sunday (java.time.DayOfWeek)
  start_time  TIME NOT NULL,
  end_time    TIME NOT NULL,
  KEY idx_avail_user (user_id, day_of_week),
  CONSTRAINT fk_av_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT chk_day  CHECK (day_of_week BETWEEN 1 AND 7),
  CONSTRAINT chk_time CHECK (end_time > start_time)
);

CREATE TABLE IF NOT EXISTS session_requests (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  learner_id         BIGINT NOT NULL,
  provider_id        BIGINT NOT NULL,
  skill_id           INT NOT NULL,
  listing_id         BIGINT NOT NULL,                  -- the provider's OFFERED user_skills row
  proposed_start     DATETIME NOT NULL,
  duration_minutes   SMALLINT NOT NULL,                -- 30, 60, 90 or 120
  mode               ENUM('ONLINE','IN_PERSON') NOT NULL,
  meeting_details    VARCHAR(255),                     -- link or place
  message            VARCHAR(500),
  cost_credits       INT NOT NULL,
  status             ENUM('PENDING','ACCEPTED','REJECTED','CANCELLED','EXPIRED',
                          'COMPLETED','NO_SHOW_LEARNER','NO_SHOW_PROVIDER') NOT NULL DEFAULT 'PENDING',
  learner_confirmed  BOOLEAN NOT NULL DEFAULT FALSE,
  provider_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
  reminder_sent      BOOLEAN NOT NULL DEFAULT FALSE,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  responded_at       DATETIME NULL,
  first_confirmed_at DATETIME NULL,                    -- used by auto-complete task
  completed_at       DATETIME NULL,
  KEY idx_sr_provider (provider_id, status),
  KEY idx_sr_learner  (learner_id, status),
  KEY idx_sr_status_created (status, created_at),
  KEY idx_sr_start (status, proposed_start),
  CONSTRAINT fk_sr_learner  FOREIGN KEY (learner_id)  REFERENCES users(id),
  CONSTRAINT fk_sr_provider FOREIGN KEY (provider_id) REFERENCES users(id),
  CONSTRAINT fk_sr_skill    FOREIGN KEY (skill_id)    REFERENCES skills(id),
  CONSTRAINT fk_sr_listing  FOREIGN KEY (listing_id)  REFERENCES user_skills(id),
  CONSTRAINT chk_not_self CHECK (learner_id <> provider_id)
);

-- Immutable ledger. users.credits is the available balance; every change to it writes a row here.
CREATE TABLE IF NOT EXISTS credits_ledger (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id            BIGINT NOT NULL,
  session_request_id BIGINT NULL,
  entry_type         ENUM('SIGNUP_BONUS','HOLD','RELEASE','EARN','ADMIN_ADJUST') NOT NULL,
  amount             INT NOT NULL,                     -- signed
  balance_after      INT NOT NULL,
  note               VARCHAR(255),
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ledger_user (user_id, created_at),
  CONSTRAINT fk_cl_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_cl_sr   FOREIGN KEY (session_request_id) REFERENCES session_requests(id)
);

CREATE TABLE IF NOT EXISTS reviews (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_request_id BIGINT NOT NULL,
  reviewer_id        BIGINT NOT NULL,
  reviewee_id        BIGINT NOT NULL,
  rating             TINYINT NOT NULL,
  comment            VARCHAR(500),
  is_hidden          BOOLEAN NOT NULL DEFAULT FALSE,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_review (session_request_id, reviewer_id),
  KEY idx_review_reviewee (reviewee_id, is_hidden),
  CONSTRAINT fk_rv_sr       FOREIGN KEY (session_request_id) REFERENCES session_requests(id),
  CONSTRAINT fk_rv_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  CONSTRAINT fk_rv_reviewee FOREIGN KEY (reviewee_id) REFERENCES users(id),
  CONSTRAINT chk_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS notifications (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  type       VARCHAR(40) NOT NULL,                     -- NotificationType enum name
  title      VARCHAR(120) NOT NULL,
  body       VARCHAR(300),
  link_url   VARCHAR(200),
  is_read    BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notif_user (user_id, is_read, created_at),
  CONSTRAINT fk_nt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS messages (
  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_request_id BIGINT NOT NULL,
  sender_id          BIGINT NOT NULL,
  body               VARCHAR(1000) NOT NULL,
  is_read            BOOLEAN NOT NULL DEFAULT FALSE,
  created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_msg_thread (session_request_id, created_at),
  CONSTRAINT fk_ms_sr     FOREIGN KEY (session_request_id) REFERENCES session_requests(id) ON DELETE CASCADE,
  CONSTRAINT fk_ms_sender FOREIGN KEY (sender_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS badges (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(40) NOT NULL UNIQUE,
  name        VARCHAR(80) NOT NULL,
  description VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_badges (
  user_id    BIGINT NOT NULL,
  badge_id   INT NOT NULL,
  awarded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, badge_id),
  CONSTRAINT fk_ub_user  FOREIGN KEY (user_id)  REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_ub_badge FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reports (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  reporter_id      BIGINT NOT NULL,
  target_user_id   BIGINT NULL,
  target_review_id BIGINT NULL,
  reason           VARCHAR(500) NOT NULL,
  status           ENUM('OPEN','RESOLVED','DISMISSED') NOT NULL DEFAULT 'OPEN',
  created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  resolved_by      BIGINT NULL,
  resolved_at      DATETIME NULL,
  KEY idx_report_status (status, created_at),
  CONSTRAINT fk_rp_reporter FOREIGN KEY (reporter_id)      REFERENCES users(id),
  CONSTRAINT fk_rp_target   FOREIGN KEY (target_user_id)   REFERENCES users(id),
  CONSTRAINT fk_rp_review   FOREIGN KEY (target_review_id) REFERENCES reviews(id),
  CONSTRAINT fk_rp_admin    FOREIGN KEY (resolved_by)      REFERENCES users(id)
);
```

Notes for `DatabaseInitializer`: read `schema.sql` from the classpath, split on `;` at end of statement (ignore blank statements and `--` comment lines), execute each with `Statement.execute`. Run `seed.sql` only if `categories` is empty. The admin account is **not** in SQL; `AppContextListener` creates it from `admin.email` / `admin.password` if no ADMIN exists (so the password is hashed by Java).

---

## 2. Seed data (`seed.sql`)

```sql
INSERT INTO categories (name, description) VALUES
 ('Programming','Languages, tools and engineering skills'),
 ('Design','UI/UX, graphics and visual design'),
 ('Music','Instruments, theory and production'),
 ('Languages','Spoken and written languages'),
 ('Academics','Course subjects and exam preparation'),
 ('Sports & Fitness','Sports coaching and fitness'),
 ('Creative Arts','Photography, video, writing and art'),
 ('Career & Soft Skills','Interviews, resumes, public speaking');

INSERT INTO skills (category_id, name) VALUES
 (1,'Java'),(1,'Python'),(1,'Data Structures & Algorithms'),(1,'Web Development'),(1,'SQL & Databases'),
 (2,'Figma / UI Design'),(2,'Graphic Design'),(2,'Logo & Branding'),
 (3,'Guitar'),(3,'Keyboard / Piano'),(3,'Music Production'),
 (4,'English Communication'),(4,'Hindi'),(4,'Tamil'),(4,'French'),(4,'Japanese'),
 (5,'Calculus'),(5,'Digital Logic'),(5,'Engineering Economics'),(5,'Physics'),
 (6,'Badminton'),(6,'Chess'),(6,'Gym Training'),
 (7,'Photography'),(7,'Video Editing'),(7,'Creative Writing'),
 (8,'Resume Review'),(8,'Mock Interviews'),(8,'Public Speaking');

INSERT INTO badges (code, name, description) VALUES
 ('FIRST_SWAP','First Swap','Completed your first session'),
 ('MENTOR_10','Mentor','Taught 10 sessions with a 4.5+ average rating'),
 ('ETERNAL_LEARNER','Eternal Learner','Learned in 10 sessions'),
 ('PERFECT_FIVE','Perfect Five','Received five 5-star reviews'),
 ('CONNECTOR','Connector','Offering 3 or more skills'),
 ('RELIABLE','Reliable','5 completed sessions with no strikes');
```

---

## 3. Entity classes (package `com.skillswap.model`)

Plain Java classes: private fields, constructors, getters/setters, `equals/hashCode` on `id`, `toString`. No frameworks.

| Class | Key fields / notes |
|---|---|
| `abstract User implements Notifiable` | id, fullName, email, passwordHash, role, department, studyYear, bio, avatarPath, credits, avgRating, ratingCount, sessionsTaught, sessionsLearned, reliabilityStrikes, active, createdAt, lastLoginAt. Abstract `String homePath()`; concrete `displayName()` |
| `Student extends User` | `homePath()` returns `/dashboard`; holds optional loaded lists `offered`, `wanted` |
| `Admin extends User` | `homePath()` returns `/admin` |
| `interface Notifiable` | `long getId(); String displayName();` |
| `Category` | id, name, description |
| `Skill` | id, categoryId, categoryName, name |
| `abstract SkillListing` | id, userId, skillId, skillName, categoryName, level, description, active, createdAt. Abstract `ListingType type()`, `String describe()`, `void validate() throws ValidationException` |
| `OfferedSkill extends SkillListing` | creditsPerHour; `validate()` requires 1..5; `describe()` e.g. "Teaches Java (Advanced) at 3 credits/hour" |
| `WantedSkill extends SkillListing` | `validate()` has no price; `describe()` e.g. "Wants to learn Java (Beginner)" |
| `AvailabilitySlot` | id, userId, dayOfWeek (`java.time.DayOfWeek`), startTime, endTime (`LocalTime`); `overlapMinutes(AvailabilitySlot other)` |
| `SessionRequest` | all columns of `session_requests` plus joined display names (learnerName, providerName, skillName) |
| `CreditEntry` | ledger row |
| `Review` | row + reviewerName |
| `Notification` | row |
| `Message` | row + senderName |
| `Badge` | id, code, name, description (+ `awardedAt` when loaded for a user) |
| `Report` | row + reporterName, targetName |
| `RequestDraft implements Serializable` | `serialVersionUID = 1L`; providerListingId, proposedStart (as `String` ISO to keep it simple), durationMinutes, mode, meetingDetails, message, savedAt |
| `MatchResult` | provider (`User`), listing (`OfferedSkill`), score, skillRelevance, ratingScore, availabilityOverlap; `Comparable<MatchResult>` by score |
| `SessionUser` (web) | id, name, role, avatarPath. Stored in `HttpSession` instead of the full `User` |

Enums (`com.skillswap.model.enums`): `Role{STUDENT,ADMIN}`, `ListingType{OFFERED,WANTED}`, `SkillLevel{BEGINNER,INTERMEDIATE,ADVANCED}`, `SessionStatus{PENDING,ACCEPTED,REJECTED,CANCELLED,EXPIRED,COMPLETED,NO_SHOW_LEARNER,NO_SHOW_PROVIDER}` (with `isTerminal()` and `canMoveTo(SessionStatus)`), `SessionMode{ONLINE,IN_PERSON}`, `LedgerType{SIGNUP_BONUS,HOLD,RELEASE,EARN,ADMIN_ADJUST}`, `NotificationType{WELCOME,NEW_MATCH,REQUEST_RECEIVED,REQUEST_ACCEPTED,REQUEST_REJECTED,REQUEST_CANCELLED,REQUEST_EXPIRED,SESSION_REMINDER,SESSION_COMPLETED,NO_SHOW,NEW_REVIEW,NEW_MESSAGE,BADGE_AWARDED,ADMIN_NOTICE}`, `ReportStatus{OPEN,RESOLVED,DISMISSED}`.

---

## 4. DAO interfaces (package `com.skillswap.dao`)

All methods may throw the unchecked `DataAccessException`. Implementations in `dao/jdbc/`.

```java
UserDao {
  long insert(User u);  Optional<User> findById(long id);  Optional<User> findByEmail(String email);
  boolean emailExists(String email);  void update(User u);  void updatePassword(long id, String hash);
  void updateAvatar(long id, String path);  void touchLogin(long id);  void setActive(long id, boolean active);
  User lockForUpdate(long id);                       // SELECT ... FOR UPDATE
  void setCredits(long id, int newBalance);
  void applyRatingStats(long id, BigDecimal avg, int count);
  void incrementTaught(long id);  void incrementLearned(long id);  void incrementStrikes(long id);
  List<User> search(String q, int limit, int offset);  int count(String q);
  List<User> topMentorsByRating(int minReviews, int limit);  List<User> topMentorsBySessions(int limit);
}
CategoryDao { List<Category> findAll(); long insert(Category c); void update(Category c); void delete(int id); Optional<Category> findByName(String n); }
SkillDao    { List<Skill> findAll(); List<Skill> findByCategory(int categoryId); long insert(Skill s); void update(Skill s); void delete(int id); Optional<Skill> findByCategoryAndName(int c, String n); }
ListingDao  {
  long insert(SkillListing l);  void setActive(long id, boolean a);  void delete(long id);
  List<OfferedSkill> findOfferedByUser(long userId);  List<WantedSkill> findWantedByUser(long userId);
  Optional<OfferedSkill> findOfferedById(long id);
  List<OfferedSkill> findOfferedBySkill(int skillId);  List<OfferedSkill> findOfferedByCategory(int categoryId);
  List<WantedSkill> findWantedCreatedSince(LocalDateTime t);  List<OfferedSkill> findOfferedCreatedSince(LocalDateTime t);
  List<OfferedSkill> searchOffered(SearchCriteria c, int limit, int offset);  int countOffered(SearchCriteria c);
  int countOfferedByUser(long userId);
}
AvailabilityDao { List<AvailabilitySlot> findByUser(long userId); long insert(AvailabilitySlot s); void delete(long id, long userId); }
SessionRequestDao {
  long insert(SessionRequest r);  Optional<SessionRequest> findById(long id);  Optional<SessionRequest> lockById(long id);
  boolean updateStatus(long id, SessionStatus from, SessionStatus to);       // guarded; false if no row matched
  void setConfirmed(long id, boolean learnerSide);  void markFirstConfirmed(long id);  void markCompleted(long id);
  void markResponded(long id);  void markReminderSent(long id);
  List<SessionRequest> findIncoming(long providerId, SessionStatus statusOrNull, int limit, int offset);
  List<SessionRequest> findOutgoing(long learnerId, SessionStatus statusOrNull, int limit, int offset);
  List<SessionRequest> findUpcomingAccepted(long userId, int limit);
  boolean existsActive(long learnerId, long providerId, int skillId);       // PENDING or ACCEPTED
  boolean hasOverlappingAccepted(long providerId, LocalDateTime start, int minutes, Long excludeRequestId);
  List<SessionRequest> findPendingOlderThan(LocalDateTime t);
  List<SessionRequest> findAcceptedStartingBetween(LocalDateTime from, LocalDateTime to);   // reminder_sent = false
  List<SessionRequest> findHalfConfirmedBefore(LocalDateTime t);
  List<SessionRequest> findAllForUser(long userId);                          // for CSV export
  int countPendingIncoming(long providerId);
}
CreditDao   { long insert(CreditEntry e); List<CreditEntry> findByUser(long userId, int limit, int offset); int countByUser(long userId);
              int sumByType(long userId, LedgerType t); int totalCreditsInCirculation(); }
ReviewDao   { long insert(Review r); boolean exists(long sessionId, long reviewerId); List<Review> findForUser(long userId, int limit);
              BigDecimal avgFor(long userId); int countFor(long userId); void setHidden(long id, boolean hidden);
              Optional<Review> findById(long id); int countFiveStar(long userId); }
NotificationDao { long insert(Notification n); List<Notification> findByUser(long userId, int limit, int offset);
                  int countUnread(long userId); void markAllRead(long userId); void markRead(long id, long userId); }
MessageDao  { long insert(Message m); List<Message> findThread(long sessionId); int countUnread(long userId);
              void markThreadRead(long sessionId, long readerId); }
BadgeDao    { List<Badge> findAll(); List<Badge> findForUser(long userId); boolean has(long userId, String code); void award(long userId, String code); }
ReportDao   { long insert(Report r); List<Report> findByStatus(ReportStatus s, int limit, int offset); void resolve(long id, long adminId, ReportStatus s); }
AnalyticsDao{ int totalUsers(); Map<SessionStatus,Integer> sessionsByStatus(); List<SkillCount> topWanted(int n); List<SkillCount> topOffered(int n);
              List<WeekCount> newUsersPerWeek(int weeks); }
```
`SearchCriteria`: keyword, categoryId, skillId, level, minRating, dayOfWeek, sort (`BEST|RATING|PRICE`).

---

## 5. Services (package `com.skillswap.service`)

Services are stateless; each multi-step method wraps its work in `TransactionManager.execute`.

| Service | Key methods |
|---|---|
| `AuthService` | `register(RegistrationForm)`, `login(email, password)`, `changePassword(userId, current, next)` |
| `ProfileService` | `get(userId)`, `update(userId, form)`, `saveAvatar(userId, Part)` |
| `CatalogService` | `categories()`, `skillsByCategory()` (from `CatalogCache`), admin CRUD, `importCsv(InputStream)` |
| `ListingService` | `offer(userId, form)`, `want(userId, form)`, `remove(userId, listingId)`, `listFor(userId)`; triggers `BadgeService.evaluate` |
| `AvailabilityService` | `slots(userId)`, `add(userId, slot)` (validates overlap), `remove(userId, slotId)` |
| `SearchService` | `search(SearchCriteria, page)` returns `Page<MatchResult>` or provider cards |
| `MatchService` | `recommend(userId, topK)`, `findFor(userId, skillId, topK)` using `MatchEngineFactory` |
| `SessionService` | `create(learnerId, form)`, `accept(providerId, id)`, `reject(providerId, id)`, `cancel(userId, id)`, `confirm(userId, id)`, `markNoShow(userId, id)`, plus scheduler entry points `expireStale()`, `autoCompleteHalfConfirmed()`, `sendReminders()` |
| `CreditService` | `grantSignupBonus`, `hold`, `release`, `pay`, `adminAdjust`; all use `lockForUpdate` in ascending id order and write ledger rows |
| `ReviewService` | `submit(reviewerId, sessionId, rating, comment)` (only after COMPLETED, once per side), `hide(adminId, reviewId)` recomputes average |
| `NotificationService` | `notify(userId, type, title, body, link)` publishes to `NotificationDispatcher`; `unreadCount`, `list`, `markAllRead` |
| `MessageService` | `send(senderId, sessionId, body)`, `thread(userId, sessionId)` (participant check) |
| `BadgeService` | `evaluate(userId)` runs all `BadgeRule`s |
| `AnalyticsService` | `userDashboard(userId)`, `adminOverview()` |
| `AdminService` | `listUsers`, `toggleActive`, `listReports`, `resolveReport`, `exportUsersCsv` |
| `ExportService` | `sessionHistoryCsv(userId, OutputStream)` through `Exporter` |
| `DraftService` | `save(userId, listingId, RequestDraft)`, `load(userId, listingId)`, `delete(...)` through `DraftStore` |

### Validation rules (centralised in `Validator`)
- Email: regex, max 150, unique. Password: min 8, at least one letter and one digit.
- Name: 2 to 100 chars. Bio: max 500. Year: 1 to 5.
- Session: `proposed_start` between now+2h and now+30d; duration in {30,60,90,120}; mode ONLINE requires a meeting link, IN_PERSON requires a place; must not request own listing; listing must be active; learner balance >= cost; must not duplicate an active request; provider's ACCEPTED sessions must not overlap.
- Review: rating 1 to 5, comment <= 500, session COMPLETED, reviewer is a participant, one review per reviewer per session.
- Availability: end after start; no overlap with the user's existing slot on the same day.
- Upload: extension jpg/jpeg/png, size <= 2 MB, magic bytes checked.

### Transaction recipes (must be atomic)
1. **create session**: validate -> lock learner row -> check balance -> insert request -> insert `HOLD` ledger row -> debit balance.
2. **accept**: lock request -> guarded `PENDING -> ACCEPTED` -> overlap check -> notify.
3. **reject / expire / learner-cancel-pending**: guarded transition -> credit `RELEASE` to learner.
4. **cancel accepted**: guarded transition -> refund/pay split per the cancellation table in `blueprint.md`.
5. **confirm**: set side flag -> if both confirmed, guarded `ACCEPTED -> COMPLETED` -> lock provider -> `EARN` -> increment taught/learned counters -> evaluate badges.
6. **no-show**: guarded transition -> pay or refund per table -> strike if provider.
7. **review**: insert review -> recompute reviewee avg/count -> update user row -> evaluate badges.