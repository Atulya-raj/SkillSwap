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
