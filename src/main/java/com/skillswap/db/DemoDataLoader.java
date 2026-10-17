package com.skillswap.db;

import com.skillswap.dao.*;
import com.skillswap.dao.jdbc.*;
import com.skillswap.model.*;
import com.skillswap.model.enums.*;
import com.skillswap.util.PasswordHasher;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Seeds a comprehensive demo dataset for demonstrations, viva defense, and testing.
 */
public class DemoDataLoader {

    private static final Logger LOGGER = Logger.getLogger(DemoDataLoader.class.getName());

    private final UserDao userDao;
    private final ListingDao listingDao;
    private final AvailabilityDao availabilityDao;
    private final SessionRequestDao sessionRequestDao;
    private final CreditDao creditDao;
    private final ReviewDao reviewDao;
    private final MessageDao messageDao;
    private final BadgeDao badgeDao;
    private final ReportDao reportDao;

    public DemoDataLoader() {
        this.userDao = new JdbcUserDao();
        this.listingDao = new JdbcListingDao();
        this.availabilityDao = new JdbcAvailabilityDao();
        this.sessionRequestDao = new JdbcSessionRequestDao();
        this.creditDao = new JdbcCreditDao();
        this.reviewDao = new JdbcReviewDao();
        this.messageDao = new JdbcMessageDao();
        this.badgeDao = new JdbcBadgeDao();
        this.reportDao = new JdbcReportDao();
    }

    /**
     * Checks if demo data is already populated. If empty, loads the complete demo dataset.
     */
    public synchronized void loadIfEmpty() {
        try (Connection con = Database.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM users WHERE role = 'STUDENT'");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                LOGGER.log(Level.INFO, "Student records already present ({0} users). Skipping demo data loading.", rs.getInt(1));
                return;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to check existing user count for demo loader", e);
            return;
        }

        LOGGER.log(Level.INFO, "Loading SkillSwap campus demo dataset (~12 users, listings, sessions, reviews)...");
        try {
            load();
            LOGGER.log(Level.INFO, "Demo data loaded successfully.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load demo data", e);
        }
    }

    /**
     * Executes the demo data population.
     */
    public void load() {
        String passwordHash = PasswordHasher.hash("Demo1234!");

        // 1. Create 12 diverse student accounts across departments
        List<Student> students = new ArrayList<>();

        students.add(createStudent("Aarav Sharma", "aarav.sharma@vitstudent.ac.in", passwordHash, "Computer Science & Engineering", 3,
                "Passionate about backend architectures, Java, and competitive coding. Happy to help juniors master algorithms.", 26, new BigDecimal("4.80"), 2, 2, 1));

        students.add(createStudent("Ananya Iyer", "ananya.iyer@vitstudent.ac.in", passwordHash, "Electronics & Communication", 4,
                "Embedded systems nerd and VLSI enthusiast. Love mentoring students in digital logic and microcontrollers.", 32, new BigDecimal("5.00"), 3, 3, 0));

        students.add(createStudent("Rohan Verma", "rohan.verma@vitstudent.ac.in", passwordHash, "Mechanical Engineering", 2,
                "CAD design and 3D printing enthusiast. Looking to learn web development and Python for automation.", 18, new BigDecimal("4.50"), 2, 1, 2));

        students.add(createStudent("Priya Nair", "priya.nair@vitstudent.ac.in", passwordHash, "Computer Science & Engineering", 3,
                "UI/UX designer and frontend web developer. I run design sprints and love typography, color theory, and Figma.", 35, new BigDecimal("4.95"), 4, 4, 1));

        students.add(createStudent("Vikram Malhotra", "vikram.malhotra@vitstudent.ac.in", passwordHash, "Biotechnology", 4,
                "Bioinformatics researcher. Can teach scientific computing, Python data manipulation, and calculus.", 20, new BigDecimal("4.00"), 1, 1, 1));

        students.add(createStudent("Sneha Patel", "sneha.patel@vitstudent.ac.in", passwordHash, "Information Technology", 2,
                "Full-stack web developer and open source enthusiast. Passionate about SQL performance and clean APIs.", 22, new BigDecimal("4.70"), 2, 2, 2));

        students.add(createStudent("Aditya Joshi", "aditya.joshi@vitstudent.ac.in", passwordHash, "Computer Science & Engineering", 4,
                "ACM ICPC regionalist. Love teaching algorithms, dynamic programming, and Java concurrency patterns.", 40, new BigDecimal("5.00"), 5, 6, 1));

        students.add(createStudent("Meera Krishnan", "meera.krishnan@vitstudent.ac.in", passwordHash, "Civil Engineering", 3,
                "Structural engineering and mathematics enthusiast. Interested in learning French and photography.", 20, BigDecimal.ZERO, 0, 0, 1));

        students.add(createStudent("Kavya Menon", "kavya.menon@vitstudent.ac.in", passwordHash, "Electrical & Electronics", 2,
                "Guitarist and amateur music producer. Happy to teach acoustic fingerstyle and music production basics.", 24, new BigDecimal("4.60"), 2, 2, 1));

        students.add(createStudent("Devendra Rao", "devendra.rao@vitstudent.ac.in", passwordHash, "Computer Science & Engineering", 3,
                "Cloud native developer, Docker and Linux enthusiast. Always happy to guide students in web architectures.", 28, new BigDecimal("4.50"), 2, 2, 1));

        students.add(createStudent("Tanvi Deshmukh", "tanvi.deshmukh@vitstudent.ac.in", passwordHash, "Design & Media", 3,
                "Graphic designer, visual illustrator, and photographer. Looking to pick up Java and basic backend scripting.", 30, new BigDecimal("4.85"), 3, 3, 1));

        students.add(createStudent("Rahul Gupta", "rahul.gupta@vitstudent.ac.in", passwordHash, "Computer Science & Engineering", 1,
                "First-year CS student eager to master competitive programming, web fundamentals, and algorithms.", 16, BigDecimal.ZERO, 0, 0, 3));

        for (Student s : students) {
            userDao.insert(s);
            // Record initial signup bonus in credits_ledger
            CreditEntry signupBonus = new CreditEntry();
            signupBonus.setUserId(s.getId());
            signupBonus.setEntryType(LedgerType.SIGNUP_BONUS);
            signupBonus.setAmount(20);
            signupBonus.setBalanceAfter(20);
            signupBonus.setNote("Welcome signup bonus");
            signupBonus.setCreatedAt(LocalDateTime.now().minusDays(14));
            creditDao.insert(signupBonus);
        }

        Student aarav = students.get(0);
        Student ananya = students.get(1);
        Student rohan = students.get(2);
        Student priya = students.get(3);
        Student vikram = students.get(4);
        Student sneha = students.get(5);
        Student aditya = students.get(6);
        Student meera = students.get(7);
        Student kavya = students.get(8);
        Student devendra = students.get(9);
        Student tanvi = students.get(10);
        Student rahul = students.get(11);

        // 2. Add Skill Listings (Offered and Wanted)
        // Skill IDs from seed.sql:
        // 1: Java, 2: Python, 3: Data Structures & Algorithms, 4: Web Development, 5: SQL & Databases
        // 6: Figma / UI Design, 7: Graphic Design, 8: Logo & Branding
        // 9: Guitar, 10: Keyboard / Piano, 11: Music Production
        // 12: English Communication, 13: Hindi, 14: Tamil, 15: French, 16: Japanese
        // 17: Calculus, 18: Digital Logic, 19: Engineering Economics, 20: Physics
        // 21: Badminton, 22: Chess, 23: Gym Training
        // 24: Photography, 25: Video Editing, 26: Creative Writing
        // 27: Resume Review, 28: Mock Interviews, 29: Public Speaking

        long aaravJavaListing = addOffer(aarav.getId(), 1, SkillLevel.ADVANCED, 2, "Core Java, OOP principles, Streams, and concurrency patterns.");
        long aaravDsaListing = addOffer(aarav.getId(), 3, SkillLevel.INTERMEDIATE, 2, "Binary trees, graphs, and greedy algorithms.");
        addWant(aarav.getId(), 4, SkillLevel.BEGINNER, "Looking to learn React and modern CSS.");

        long ananyaLogicListing = addOffer(ananya.getId(), 18, SkillLevel.ADVANCED, 2, "Boolean algebra, combinational & sequential circuit design, Verilog basics.");
        long ananyaPhysicsListing = addOffer(ananya.getId(), 20, SkillLevel.ADVANCED, 2, "Electromagnetism and semiconductor physics.");
        addWant(ananya.getId(), 2, SkillLevel.BEGINNER, "Want to learn Python for scientific simulation.");

        long rohanCalcListing = addOffer(rohan.getId(), 17, SkillLevel.INTERMEDIATE, 1, "Differential equations and multivariable calculus.");
        long rohanEconListing = addOffer(rohan.getId(), 19, SkillLevel.INTERMEDIATE, 1, "Engineering economics and project feasibility.");
        addWant(rohan.getId(), 4, SkillLevel.BEGINNER, "Interested in learning frontend web dev.");

        long priyaFigmaListing = addOffer(priya.getId(), 6, SkillLevel.ADVANCED, 3, "Wireframing, prototyping, design systems, and auto-layout in Figma.");
        long priyaGraphicListing = addOffer(priya.getId(), 7, SkillLevel.ADVANCED, 2, "Vector graphics, color palettes, and visual hierarchy.");
        addWant(priya.getId(), 2, SkillLevel.BEGINNER, "Want to script design tokens in Python.");

        long vikramPythonListing = addOffer(vikram.getId(), 2, SkillLevel.ADVANCED, 2, "Python programming for data analysis, Pandas, and NumPy.");
        long vikramBioCalcListing = addOffer(vikram.getId(), 17, SkillLevel.INTERMEDIATE, 1, "Applied calculus in biological systems.");
        addWant(vikram.getId(), 12, SkillLevel.INTERMEDIATE, "Public speaking and communication practice.");

        long snehaWebListing = addOffer(sneha.getId(), 4, SkillLevel.ADVANCED, 2, "HTML5, CSS3, modern JavaScript, and building responsive layouts.");
        long snehaSqlListing = addOffer(sneha.getId(), 5, SkillLevel.ADVANCED, 2, "Relational schema design, normalization, indexing, and complex queries.");
        addWant(sneha.getId(), 29, SkillLevel.BEGINNER, "Speech delivery and presentation confidence.");

        long adityaDsaListing = addOffer(aditya.getId(), 3, SkillLevel.ADVANCED, 3, "Advanced algorithms, dynamic programming, segment trees, and graph theory.");
        long adityaJavaListing = addOffer(aditya.getId(), 1, SkillLevel.ADVANCED, 3, "Enterprise Java, performance tuning, and JVM profiling.");
        long adityaPythonListing = addOffer(aditya.getId(), 2, SkillLevel.ADVANCED, 3, "Python automation, scripts, and competitive problem solving.");
        addWant(aditya.getId(), 16, SkillLevel.BEGINNER, "Basic Japanese conversation for exchange program.");

        long meeraPhysicsListing = addOffer(meera.getId(), 20, SkillLevel.INTERMEDIATE, 1, "Classical mechanics and structural kinematics.");
        long meeraEconListing = addOffer(meera.getId(), 19, SkillLevel.INTERMEDIATE, 1, "Cost-benefit analysis and cash flow analysis.");
        addWant(meera.getId(), 15, SkillLevel.BEGINNER, "Beginner French language fundamentals.");

        long kavyaGuitarListing = addOffer(kavya.getId(), 9, SkillLevel.ADVANCED, 2, "Acoustic chords, fingerstyle patterns, and rhythm fundamentals.");
        long kavyaMusicListing = addOffer(kavya.getId(), 11, SkillLevel.INTERMEDIATE, 2, "DAW basics, audio mixing, and MIDI arrangement.");
        addWant(kavya.getId(), 2, SkillLevel.BEGINNER, "Python scripting for audio signal processing.");

        long devendraWebListing = addOffer(devendra.getId(), 4, SkillLevel.ADVANCED, 2, "Node.js, REST APIs, microservices, and server deployments.");
        long devendraSqlListing = addOffer(devendra.getId(), 5, SkillLevel.ADVANCED, 2, "Database optimization, transaction isolation, and tuning.");
        addWant(devendra.getId(), 28, SkillLevel.INTERMEDIATE, "Mock interview prep for tech internships.");

        long tanviPhotoListing = addOffer(tanvi.getId(), 24, SkillLevel.ADVANCED, 2, "DSLR composition, lighting, portraits, and Lightroom editing.");
        long tanviGraphicListing = addOffer(tanvi.getId(), 7, SkillLevel.ADVANCED, 2, "Brand identities, posters, and typography.");
        long tanviFigmaListing = addOffer(tanvi.getId(), 6, SkillLevel.ADVANCED, 3, "Interactive prototypes and usability heuristics.");
        addWant(tanvi.getId(), 1, SkillLevel.BEGINNER, "Introductory Java programming.");

        long rahulCalcListing = addOffer(rahul.getId(), 17, SkillLevel.INTERMEDIATE, 1, "First year calculus, limits, and integration tricks.");
        addWant(rahul.getId(), 1, SkillLevel.BEGINNER, "Java fundamentals.");
        addWant(rahul.getId(), 3, SkillLevel.BEGINNER, "Data structures and coding practice.");

        // 3. Add Weekly Availability Slots
        addSlot(aarav.getId(), DayOfWeek.MONDAY, LocalTime.of(16, 0), LocalTime.of(18, 0));
        addSlot(aarav.getId(), DayOfWeek.WEDNESDAY, LocalTime.of(16, 0), LocalTime.of(18, 0));
        addSlot(aarav.getId(), DayOfWeek.SATURDAY, LocalTime.of(10, 0), LocalTime.of(13, 0));

        addSlot(ananya.getId(), DayOfWeek.TUESDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        addSlot(ananya.getId(), DayOfWeek.THURSDAY, LocalTime.of(15, 0), LocalTime.of(17, 0));
        addSlot(ananya.getId(), DayOfWeek.SUNDAY, LocalTime.of(11, 0), LocalTime.of(14, 0));

        addSlot(priya.getId(), DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(17, 0));
        addSlot(priya.getId(), DayOfWeek.FRIDAY, LocalTime.of(14, 0), LocalTime.of(17, 0));
        addSlot(priya.getId(), DayOfWeek.SATURDAY, LocalTime.of(14, 0), LocalTime.of(18, 0));

        addSlot(sneha.getId(), DayOfWeek.WEDNESDAY, LocalTime.of(17, 0), LocalTime.of(20, 0));
        addSlot(sneha.getId(), DayOfWeek.FRIDAY, LocalTime.of(17, 0), LocalTime.of(20, 0));

        addSlot(aditya.getId(), DayOfWeek.TUESDAY, LocalTime.of(18, 0), LocalTime.of(21, 0));
        addSlot(aditya.getId(), DayOfWeek.THURSDAY, LocalTime.of(18, 0), LocalTime.of(21, 0));
        addSlot(aditya.getId(), DayOfWeek.SUNDAY, LocalTime.of(15, 0), LocalTime.of(19, 0));

        addSlot(kavya.getId(), DayOfWeek.SATURDAY, LocalTime.of(16, 0), LocalTime.of(19, 0));
        addSlot(devendra.getId(), DayOfWeek.MONDAY, LocalTime.of(18, 0), LocalTime.of(20, 0));
        addSlot(tanvi.getId(), DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(16, 0));

        // 4. Session Requests in Every Status

        // Status 1: PENDING (Rahul -> Aarav: Java, cost: 4)
        SessionRequest sr1 = new SessionRequest();
        sr1.setLearnerId(rahul.getId());
        sr1.setProviderId(aarav.getId());
        sr1.setSkillId(1);
        sr1.setListingId(aaravJavaListing);
        sr1.setProposedStart(LocalDateTime.now().plusDays(2).withHour(16).withMinute(0).withSecond(0));
        sr1.setDurationMinutes(120);
        sr1.setMode(SessionMode.ONLINE);
        sr1.setMeetingDetails("Google Meet link will be shared");
        sr1.setMessage("Hi Aarav! I'm struggling with Java Collections and Generics. Could you help me with a 2-hour deep dive?");
        sr1.setCostCredits(4);
        sr1.setStatus(SessionStatus.PENDING);
        sr1.setCreatedAt(LocalDateTime.now().minusHours(3));
        long sr1Id = sessionRequestDao.insert(sr1);
        recordLedger(rahul.getId(), sr1Id, LedgerType.HOLD, -4, 16, "Escrow hold for session request #" + sr1Id);

        // Status 1: PENDING (Rohan -> Priya: Figma, cost: 3)
        SessionRequest sr2 = new SessionRequest();
        sr2.setLearnerId(rohan.getId());
        sr2.setProviderId(priya.getId());
        sr2.setSkillId(6);
        sr2.setListingId(priyaFigmaListing);
        sr2.setProposedStart(LocalDateTime.now().plusDays(3).withHour(14).withMinute(0).withSecond(0));
        sr2.setDurationMinutes(60);
        sr2.setMode(SessionMode.IN_PERSON);
        sr2.setMeetingDetails("Library 2nd Floor Discussion Room 4");
        sr2.setMessage("Hello Priya, would love to learn Figma prototyping basics for our project presentation.");
        sr2.setCostCredits(3);
        sr2.setStatus(SessionStatus.PENDING);
        sr2.setCreatedAt(LocalDateTime.now().minusHours(6));
        long sr2Id = sessionRequestDao.insert(sr2);
        recordLedger(rohan.getId(), sr2Id, LedgerType.HOLD, -3, 15, "Escrow hold for session request #" + sr2Id);

        // Status 2: ACCEPTED (Kavya -> Sneha: Web Dev, cost: 4)
        SessionRequest sr3 = new SessionRequest();
        sr3.setLearnerId(kavya.getId());
        sr3.setProviderId(sneha.getId());
        sr3.setSkillId(4);
        sr3.setListingId(snehaWebListing);
        sr3.setProposedStart(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0));
        sr3.setDurationMinutes(120);
        sr3.setMode(SessionMode.ONLINE);
        sr3.setMeetingDetails("https://meet.google.com/abc-demo-xyz");
        sr3.setMessage("Hey Sneha, can you help me build a personal audio portfolio website using HTML/CSS?");
        sr3.setCostCredits(4);
        sr3.setStatus(SessionStatus.ACCEPTED);
        sr3.setCreatedAt(LocalDateTime.now().minusDays(1));
        sr3.setRespondedAt(LocalDateTime.now().minusHours(18));
        long sr3Id = sessionRequestDao.insert(sr3);
        recordLedger(kavya.getId(), sr3Id, LedgerType.HOLD, -4, 20, "Escrow hold for session request #" + sr3Id);
        addMessage(sr3Id, kavya.getId(), "Hi Sneha! Looking forward to our web dev session tomorrow. Should I have VS Code installed?", LocalDateTime.now().minusHours(15));
        addMessage(sr3Id, sneha.getId(), "Hey Kavya! Yes, VS Code and Node.js LTS would be great. We'll start with HTML/CSS and layout concepts!", LocalDateTime.now().minusHours(12));
        addMessage(sr3Id, kavya.getId(), "Awesome, downloaded and configured. See you tomorrow at 6 PM!", LocalDateTime.now().minusHours(10));

        // Status 2: ACCEPTED (Tanvi -> Aditya: Python, cost: 6)
        SessionRequest sr4 = new SessionRequest();
        sr4.setLearnerId(tanvi.getId());
        sr4.setProviderId(aditya.getId());
        sr4.setSkillId(2);
        sr4.setListingId(adityaPythonListing);
        sr4.setProposedStart(LocalDateTime.now().plusDays(2).withHour(19).withMinute(0).withSecond(0));
        sr4.setDurationMinutes(120);
        sr4.setMode(SessionMode.ONLINE);
        sr4.setMeetingDetails("https://meet.google.com/qwe-python-demo");
        sr4.setMessage("Hi Aditya, I want to learn Python scripting to automate file renaming and image processing workflows.");
        sr4.setCostCredits(6);
        sr4.setStatus(SessionStatus.ACCEPTED);
        sr4.setCreatedAt(LocalDateTime.now().minusDays(2));
        sr4.setRespondedAt(LocalDateTime.now().minusDays(1));
        long sr4Id = sessionRequestDao.insert(sr4);
        recordLedger(tanvi.getId(), sr4Id, LedgerType.HOLD, -6, 24, "Escrow hold for session request #" + sr4Id);
        addMessage(sr4Id, tanvi.getId(), "Hi Aditya! I have Python 3.11 installed. Do we need any packages for the session?", LocalDateTime.now().minusHours(20));
        addMessage(sr4Id, aditya.getId(), "Just the standard library `os` and `pathlib` for our first hour. We'll explore Pillow for images next!", LocalDateTime.now().minusHours(16));

        // Status 3: COMPLETED (Sneha -> Priya: Figma, cost: 6)
        SessionRequest sr5 = new SessionRequest();
        sr5.setLearnerId(sneha.getId());
        sr5.setProviderId(priya.getId());
        sr5.setSkillId(6);
        sr5.setListingId(priyaFigmaListing);
        sr5.setProposedStart(LocalDateTime.now().minusDays(4).withHour(15).withMinute(0).withSecond(0));
        sr5.setDurationMinutes(120);
        sr5.setMode(SessionMode.ONLINE);
        sr5.setMeetingDetails("https://meet.google.com/figma-mentor-session");
        sr5.setMessage("Hi Priya, looking to master auto-layout and responsive design components in Figma.");
        sr5.setCostCredits(6);
        sr5.setStatus(SessionStatus.COMPLETED);
        sr5.setLearnerConfirmed(true);
        sr5.setProviderConfirmed(true);
        sr5.setCreatedAt(LocalDateTime.now().minusDays(7));
        sr5.setRespondedAt(LocalDateTime.now().minusDays(6));
        sr5.setFirstConfirmedAt(LocalDateTime.now().minusDays(4).plusHours(2));
        sr5.setCompletedAt(LocalDateTime.now().minusDays(4).plusHours(2).plusMinutes(15));
        long sr5Id = sessionRequestDao.insert(sr5);
        recordLedger(sneha.getId(), sr5Id, LedgerType.HOLD, -6, 16, "Escrow hold for session request #" + sr5Id);
        recordLedger(priya.getId(), sr5Id, LedgerType.EARN, 6, 35, "Session payout for completing #" + sr5Id);
        addMessage(sr5Id, sneha.getId(), "Hi Priya, can we focus on building a cohesive design system with tokens?", LocalDateTime.now().minusDays(6));
        addMessage(sr5Id, priya.getId(), "Definitely! I've prepped a starter Figma template with typography and color palettes.", LocalDateTime.now().minusDays(5));
        addMessage(sr5Id, sneha.getId(), "Thanks for the incredible mentorship today! Learned so much about responsive layouts.", LocalDateTime.now().minusDays(4));
        long rev1Id = addReview(sr5Id, sneha.getId(), priya.getId(), 5, "Priya is an exceptional mentor! She patiently explained auto-layout, nested frames, and design tokens. Truly top-tier.");
        addReview(sr5Id, priya.getId(), sneha.getId(), 5, "Sneha picked up Figma components rapidly and asked great architectural questions. Fantastic peer learner!");

        // Status 3: COMPLETED (Rahul -> Aditya: DSA, cost: 6)
        SessionRequest sr6 = new SessionRequest();
        sr6.setLearnerId(rahul.getId());
        sr6.setProviderId(aditya.getId());
        sr6.setSkillId(3);
        sr6.setListingId(adityaDsaListing);
        sr6.setProposedStart(LocalDateTime.now().minusDays(6).withHour(18).withMinute(0).withSecond(0));
        sr6.setDurationMinutes(120);
        sr6.setMode(SessionMode.IN_PERSON);
        sr6.setMeetingDetails("Tech Park Block 2 - Study Lounge");
        sr6.setMessage("Hi Aditya! Could we go through recursion trees and dynamic programming memoization?");
        sr6.setCostCredits(6);
        sr6.setStatus(SessionStatus.COMPLETED);
        sr6.setLearnerConfirmed(true);
        sr6.setProviderConfirmed(true);
        sr6.setCreatedAt(LocalDateTime.now().minusDays(9));
        sr6.setRespondedAt(LocalDateTime.now().minusDays(8));
        sr6.setFirstConfirmedAt(LocalDateTime.now().minusDays(6).plusHours(2));
        sr6.setCompletedAt(LocalDateTime.now().minusDays(6).plusHours(2).plusMinutes(20));
        long sr6Id = sessionRequestDao.insert(sr6);
        recordLedger(rahul.getId(), sr6Id, LedgerType.HOLD, -6, 14, "Escrow hold for session request #" + sr6Id);
        recordLedger(aditya.getId(), sr6Id, LedgerType.EARN, 6, 40, "Session payout for completing #" + sr6Id);
        addReview(sr6Id, rahul.getId(), aditya.getId(), 5, "Aditya demystified difficult dynamic programming problems effortlessly. Best mentor on campus!");
        addReview(sr6Id, aditya.getId(), rahul.getId(), 5, "Rahul grasped the state transition equations very quickly and showed great problem-solving discipline.");

        // Status 3: COMPLETED (Aarav -> Ananya: Digital Logic, cost: 4)
        SessionRequest sr7 = new SessionRequest();
        sr7.setLearnerId(aarav.getId());
        sr7.setProviderId(ananya.getId());
        sr7.setSkillId(18);
        sr7.setListingId(ananyaLogicListing);
        sr7.setProposedStart(LocalDateTime.now().minusDays(8).withHour(15).withMinute(0).withSecond(0));
        sr7.setDurationMinutes(120);
        sr7.setMode(SessionMode.ONLINE);
        sr7.setMeetingDetails("https://meet.google.com/logic-circuits-ananya");
        sr7.setMessage("Hi Ananya, I need help understanding finite state machines and sequential counter designs.");
        sr7.setCostCredits(4);
        sr7.setStatus(SessionStatus.COMPLETED);
        sr7.setLearnerConfirmed(true);
        sr7.setProviderConfirmed(true);
        sr7.setCreatedAt(LocalDateTime.now().minusDays(11));
        sr7.setRespondedAt(LocalDateTime.now().minusDays(10));
        sr7.setFirstConfirmedAt(LocalDateTime.now().minusDays(8).plusHours(2));
        sr7.setCompletedAt(LocalDateTime.now().minusDays(8).plusHours(2).plusMinutes(10));
        long sr7Id = sessionRequestDao.insert(sr7);
        recordLedger(aarav.getId(), sr7Id, LedgerType.HOLD, -4, 16, "Escrow hold for session request #" + sr7Id);
        recordLedger(ananya.getId(), sr7Id, LedgerType.EARN, 4, 32, "Session payout for completing #" + sr7Id);
        addReview(sr7Id, aarav.getId(), ananya.getId(), 5, "Ananya is a genius at digital systems. She drew state diagrams and timing diagrams that made everything click.");

        // Status 4: CANCELLED (Vikram -> Aarav: Java, cost: 2)
        SessionRequest sr8 = new SessionRequest();
        sr8.setLearnerId(vikram.getId());
        sr8.setProviderId(aarav.getId());
        sr8.setSkillId(1);
        sr8.setListingId(aaravJavaListing);
        sr8.setProposedStart(LocalDateTime.now().minusDays(3).withHour(17).withMinute(0).withSecond(0));
        sr8.setDurationMinutes(60);
        sr8.setMode(SessionMode.ONLINE);
        sr8.setMeetingDetails("Online meet");
        sr8.setMessage("Need quick help setting up Maven multimodule build.");
        sr8.setCostCredits(2);
        sr8.setStatus(SessionStatus.CANCELLED);
        sr8.setCreatedAt(LocalDateTime.now().minusDays(4));
        long sr8Id = sessionRequestDao.insert(sr8);
        recordLedger(vikram.getId(), sr8Id, LedgerType.HOLD, -2, 18, "Escrow hold for session request #" + sr8Id);
        recordLedger(vikram.getId(), sr8Id, LedgerType.RELEASE, 2, 20, "Refund for cancelled request #" + sr8Id);

        // Status 5: NO_SHOW_LEARNER (Meera -> Sneha: Web Dev, cost: 2)
        SessionRequest sr9 = new SessionRequest();
        sr9.setLearnerId(meera.getId());
        sr9.setProviderId(sneha.getId());
        sr9.setSkillId(4);
        sr9.setListingId(snehaWebListing);
        sr9.setProposedStart(LocalDateTime.now().minusDays(5).withHour(17).withMinute(0).withSecond(0));
        sr9.setDurationMinutes(60);
        sr9.setMode(SessionMode.ONLINE);
        sr9.setMeetingDetails("Google Meet Link");
        sr9.setMessage("Intro to CSS flexbox and grid.");
        sr9.setCostCredits(2);
        sr9.setStatus(SessionStatus.NO_SHOW_LEARNER);
        sr9.setCreatedAt(LocalDateTime.now().minusDays(7));
        sr9.setRespondedAt(LocalDateTime.now().minusDays(6));
        long sr9Id = sessionRequestDao.insert(sr9);
        recordLedger(meera.getId(), sr9Id, LedgerType.HOLD, -2, 18, "Escrow hold for session request #" + sr9Id);
        recordLedger(sneha.getId(), sr9Id, LedgerType.EARN, 2, 22, "Compensation for learner no-show #" + sr9Id);

        // Status 6: REJECTED (Rahul -> Tanvi: Photography, cost: 2)
        SessionRequest sr10 = new SessionRequest();
        sr10.setLearnerId(rahul.getId());
        sr10.setProviderId(tanvi.getId());
        sr10.setSkillId(24);
        sr10.setListingId(tanviPhotoListing);
        sr10.setProposedStart(LocalDateTime.now().minusDays(2).withHour(15).withMinute(0).withSecond(0));
        sr10.setDurationMinutes(60);
        sr10.setMode(SessionMode.IN_PERSON);
        sr10.setMeetingDetails("Campus Amphitheatre");
        sr10.setMessage("Looking to learn camera manual settings and shutter speed basics.");
        sr10.setCostCredits(2);
        sr10.setStatus(SessionStatus.REJECTED);
        sr10.setCreatedAt(LocalDateTime.now().minusDays(3));
        sr10.setRespondedAt(LocalDateTime.now().minusDays(2));
        long sr10Id = sessionRequestDao.insert(sr10);
        recordLedger(rahul.getId(), sr10Id, LedgerType.HOLD, -2, 18, "Escrow hold for session request #" + sr10Id);
        recordLedger(rahul.getId(), sr10Id, LedgerType.RELEASE, 2, 20, "Refund for rejected request #" + sr10Id);

        // 5. Award Badges
        badgeDao.award(aarav.getId(), "FIRST_SWAP");
        badgeDao.award(ananya.getId(), "FIRST_SWAP");
        badgeDao.award(ananya.getId(), "RELIABLE");
        badgeDao.award(priya.getId(), "FIRST_SWAP");
        badgeDao.award(priya.getId(), "PERFECT_FIVE");
        badgeDao.award(priya.getId(), "CONNECTOR");
        badgeDao.award(sneha.getId(), "FIRST_SWAP");
        badgeDao.award(sneha.getId(), "CONNECTOR");
        badgeDao.award(aditya.getId(), "FIRST_SWAP");
        badgeDao.award(aditya.getId(), "PERFECT_FIVE");
        badgeDao.award(aditya.getId(), "CONNECTOR");
        badgeDao.award(aditya.getId(), "RELIABLE");
        badgeDao.award(rahul.getId(), "FIRST_SWAP");

        // 6. Moderation Reports (for Admin moderation queue)
        Report rep1 = new Report();
        rep1.setReporterId(aarav.getId());
        rep1.setTargetUserId(rohan.getId());
        rep1.setReason("User has not been active on scheduling confirmed sessions.");
        rep1.setStatus(ReportStatus.OPEN);
        rep1.setCreatedAt(LocalDateTime.now().minusDays(1));
        reportDao.insert(rep1);

        Report rep2 = new Report();
        rep2.setReporterId(sneha.getId());
        rep2.setTargetReviewId(rev1Id);
        rep2.setReason("Testing administrator report resolution flow.");
        rep2.setStatus(ReportStatus.OPEN);
        rep2.setCreatedAt(LocalDateTime.now().minusHours(12));
        reportDao.insert(rep2);
    }

    private Student createStudent(String name, String email, String passwordHash, String dept, int year,
                                  String bio, int credits, BigDecimal avgRating, int ratingCount, int taught, int learned) {
        Student s = new Student();
        s.setFullName(name);
        s.setEmail(email);
        s.setPasswordHash(passwordHash);
        s.setDepartment(dept);
        s.setStudyYear(year);
        s.setBio(bio);
        s.setCredits(credits);
        s.setAvgRating(avgRating);
        s.setRatingCount(ratingCount);
        s.setSessionsTaught(taught);
        s.setSessionsLearned(learned);
        s.setActive(true);
        s.setCreatedAt(LocalDateTime.now().minusDays(14));
        return s;
    }

    private long addOffer(long userId, int skillId, SkillLevel level, int rate, String description) {
        OfferedSkill offer = new OfferedSkill();
        offer.setUserId(userId);
        offer.setSkillId(skillId);
        offer.setLevel(level);
        offer.setCreditsPerHour(rate);
        offer.setDescription(description);
        offer.setActive(true);
        return listingDao.insert(offer);
    }

    private long addWant(long userId, int skillId, SkillLevel level, String description) {
        WantedSkill want = new WantedSkill();
        want.setUserId(userId);
        want.setSkillId(skillId);
        want.setLevel(level);
        want.setDescription(description);
        want.setActive(true);
        return listingDao.insert(want);
    }

    private void addSlot(long userId, DayOfWeek day, LocalTime start, LocalTime end) {
        AvailabilitySlot slot = new AvailabilitySlot();
        slot.setUserId(userId);
        slot.setDayOfWeek(day);
        slot.setStartTime(start);
        slot.setEndTime(end);
        availabilityDao.insert(slot);
    }

    private void recordLedger(long userId, long sessionId, LedgerType type, int amount, int balanceAfter, String note) {
        CreditEntry entry = new CreditEntry();
        entry.setUserId(userId);
        entry.setSessionRequestId(sessionId);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter);
        entry.setNote(note);
        entry.setCreatedAt(LocalDateTime.now().minusDays(1));
        creditDao.insert(entry);
    }

    private void addMessage(long sessionId, long senderId, String body, LocalDateTime time) {
        Message msg = new Message();
        msg.setSessionRequestId(sessionId);
        msg.setSenderId(senderId);
        msg.setBody(body);
        msg.setRead(true);
        msg.setCreatedAt(time);
        messageDao.insert(msg);
    }

    private long addReview(long sessionId, long reviewerId, long revieweeId, int rating, String comment) {
        Review r = new Review();
        r.setSessionRequestId(sessionId);
        r.setReviewerId(reviewerId);
        r.setRevieweeId(revieweeId);
        r.setRating(rating);
        r.setComment(comment);
        r.setHidden(false);
        r.setCreatedAt(LocalDateTime.now().minusDays(3));
        return reviewDao.insert(r);
    }
}
