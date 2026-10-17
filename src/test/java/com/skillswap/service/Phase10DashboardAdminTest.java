package com.skillswap.service;

import com.skillswap.dao.CategoryDao;
import com.skillswap.dao.CreditDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.ReportDao;
import com.skillswap.dao.ReviewDao;
import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.SkillDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcCategoryDao;
import com.skillswap.dao.jdbc.JdbcCreditDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcReportDao;
import com.skillswap.dao.jdbc.JdbcReviewDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcSkillDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.Database;
import com.skillswap.db.DatabaseInitializer;
import com.skillswap.exception.AuthenticationException;
import com.skillswap.exception.ValidationException;
import com.skillswap.io.CsvExporter;
import com.skillswap.io.CsvImporter;
import com.skillswap.model.CreditEntry;
import com.skillswap.model.OfferedSkill;
import com.skillswap.model.Report;
import com.skillswap.model.Review;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.enums.LedgerType;
import com.skillswap.model.enums.ReportStatus;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.util.Config;
import com.skillswap.util.PasswordHasher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration and unit tests for Phase 10: Dashboards, Admin Panel, Analytics, and CSV Exports.
 */
public class Phase10DashboardAdminTest {

    private static UserDao userDao;
    private static SessionRequestDao sessionRequestDao;
    private static ListingDao listingDao;
    private static CreditDao creditDao;
    private static ReportDao reportDao;
    private static ReviewDao reviewDao;
    private static CategoryDao categoryDao;
    private static SkillDao skillDao;

    private static AnalyticsService analyticsService;
    private static AdminService adminService;
    private static CatalogService catalogService;
    private static AuthService authService;
    private static ExportService exportService;

    @BeforeAll
    public static void setUp() {
        Config.loadDefault();
        Database.init();
        new DatabaseInitializer().initialize();

        userDao = new JdbcUserDao();
        listingDao = new JdbcListingDao();
        sessionRequestDao = new JdbcSessionRequestDao();
        creditDao = new JdbcCreditDao();
        reportDao = new JdbcReportDao();
        reviewDao = new JdbcReviewDao();
        categoryDao = new JdbcCategoryDao();
        skillDao = new JdbcSkillDao();

        analyticsService = new AnalyticsService();
        adminService = new AdminService();
        catalogService = new CatalogService();
        authService = new AuthService();
        exportService = new ExportService();
    }

    private User createTestUser(String prefix, int credits) {
        Student u = new Student();
        String idStr = UUID.randomUUID().toString().substring(0, 8);
        u.setFullName(prefix + " " + idStr);
        u.setEmail(prefix.toLowerCase() + "." + idStr + "@vitstudent.ac.in");
        u.setPasswordHash(PasswordHasher.hash("Pass1234"));
        u.setRole(Role.STUDENT);
        u.setDepartment("Computer Science");
        u.setStudyYear(3);
        u.setCredits(credits);
        u.setActive(true);
        u.setAvgRating(new BigDecimal("4.80"));
        u.setRatingCount(5);
        u.setSessionsTaught(4);
        u.setSessionsLearned(2);
        long id = userDao.insert(u);
        u.setId(id);
        for (int i = 0; i < 4; i++) {
            userDao.incrementTaught(id);
        }
        for (int i = 0; i < 2; i++) {
            userDao.incrementLearned(id);
        }
        userDao.applyRatingStats(id, new BigDecimal("4.80"), 5);
        return u;
    }

    @Test
    public void testUserDashboardDataAggregation() throws Exception {
        User user = createTestUser("DashUser", 25);

        // Record some credit movements
        CreditEntry hold = new CreditEntry();
        hold.setUserId(user.getId());
        hold.setEntryType(LedgerType.HOLD);
        hold.setAmount(-5);
        hold.setBalanceAfter(20);
        hold.setNote("Escrow hold");
        creditDao.insert(hold);

        CreditEntry earn = new CreditEntry();
        earn.setUserId(user.getId());
        earn.setEntryType(LedgerType.EARN);
        earn.setAmount(10);
        earn.setBalanceAfter(30);
        earn.setNote("Session payout");
        creditDao.insert(earn);

        AnalyticsService.UserDashboardData data = analyticsService.userDashboard(user.getId());
        assertNotNull(data);
        assertEquals(user.getId(), data.getUser().getId());
        assertEquals(25, data.getAvailableCredits());
        assertEquals(10, data.getTotalEarned());
        assertEquals(5, data.getTotalSpent());
        assertEquals(4, data.getSessionsTaught());
        assertEquals(2, data.getSessionsLearned());
        assertEquals(new BigDecimal("4.80"), data.getAvgRating());
        assertEquals(5, data.getRatingCount());
        assertNotNull(data.getRecommendations());
        assertNotNull(data.getBadges());
    }

    @Test
    public void testAdminAnalyticsOverviewAndPercentages() {
        int simulatedActiveUsers = 4;
        AnalyticsService.AdminAnalyticsData data = analyticsService.adminOverview(simulatedActiveUsers);

        assertNotNull(data);
        assertTrue(data.getTotalUsers() > 0);
        assertEquals(4, data.getActiveUsersOnline());
        assertTrue(data.getTotalCreditsInCirculation() >= 0);
        assertNotNull(data.getSessionsByStatus());
        assertNotNull(data.getCompletionRateFormatted());

        // Validate top wanted percentage bounds
        if (!data.getTopWanted().isEmpty()) {
            for (AnalyticsService.SkillDemandBar bar : data.getTopWanted()) {
                assertTrue(bar.getPercentWidth() >= 1 && bar.getPercentWidth() <= 100);
            }
        }

        // Validate top offered percentage bounds
        if (!data.getTopOffered().isEmpty()) {
            for (AnalyticsService.SkillDemandBar bar : data.getTopOffered()) {
                assertTrue(bar.getPercentWidth() >= 1 && bar.getPercentWidth() <= 100);
            }
        }
    }

    @Test
    public void testCsvExporterQuotingAndBom() throws Exception {
        // Test RFC 4180 escaping
        assertEquals("simple", CsvExporter.escapeField("simple"));
        assertEquals("\"hello, world\"", CsvExporter.escapeField("hello, world"));
        assertEquals("\"quotes \"\"inside\"\"\"", CsvExporter.escapeField("quotes \"inside\""));
        assertEquals("\"line1\nline2\"", CsvExporter.escapeField("line1\nline2"));

        // Test export with UTF-8 BOM
        User u = createTestUser("ExportUser", 15);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CsvExporter<User> exporter = CsvExporter.forUsers();
        exporter.export(List.of(u), baos);

        byte[] bytes = baos.toByteArray();
        assertTrue(bytes.length >= 3);
        // Verify UTF-8 BOM bytes 0xEF, 0xBB, 0xBF
        assertEquals((byte) 0xEF, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xBF, bytes[2]);

        String csvText = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(csvText.contains("User ID,Full Name,Email"));
        assertTrue(csvText.contains(u.getFullName()));
        assertTrue(csvText.contains(u.getEmail()));
    }

    @Test
    public void testCsvImporterBadRowHandlingAndCache() throws Exception {
        String catName = "Robotics " + UUID.randomUUID().toString().substring(0, 6);
        String csvContent = "Category,Skill\n"
                + catName + ",ROS 2 Navigation\n"
                + catName + ",Computer Vision with OpenCV\n"
                + "BadRowWithoutComma\n"
                + ",EmptyCategory\n"
                + "EmptySkill,\n"
                + catName + ",ROS 2 Navigation\n"; // duplicate skill

        ByteArrayInputStream bais = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
        CsvImporter.ImportResult result = catalogService.importCatalogCsv(bais);

        assertNotNull(result);
        assertEquals(2, result.getImportedCount());
        assertTrue(result.getSkippedCount() >= 4);
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().size() >= 4);

        // Verify cache was refreshed and contains the newly imported skills
        assertTrue(catalogService.skillsByCategory().containsKey(catName));
    }

    @Test
    public void testAdminUserToggleAndDeactivationLoginBlock() throws Exception {
        User u = createTestUser("DeactUser", 10);
        assertTrue(u.isActive());

        // Deactivate user via AdminService
        adminService.toggleActive(u.getId(), false);

        User refreshed = userDao.findById(u.getId()).orElseThrow();
        assertFalse(refreshed.isActive());

        // Deactivated user cannot log in
        assertThrows(AuthenticationException.class, () -> {
            authService.login(u.getEmail(), "Pass1234");
        });

        // Reactivate user
        adminService.toggleActive(u.getId(), true);
        User activeAgain = authService.login(u.getEmail(), "Pass1234");
        assertNotNull(activeAgain);
        assertTrue(activeAgain.isActive());
    }

    @Test
    public void testAdminCreditAdjustmentWithReasonAudit() throws Exception {
        User admin = createTestUser("AdminTester", 50);
        User student = createTestUser("StudentCredit", 20);

        // Adjust +15 credits with reason
        String reason = "Resolved lab attendance compensation";
        adminService.adjustCredits(admin.getId(), student.getId(), 15, reason);

        User updated = userDao.findById(student.getId()).orElseThrow();
        assertEquals(35, updated.getCredits());

        // Verify audit ledger row
        List<CreditEntry> ledger = creditDao.findByUser(student.getId(), 10, 0);
        assertFalse(ledger.isEmpty());
        CreditEntry entry = ledger.get(0);
        assertEquals(LedgerType.ADMIN_ADJUST, entry.getEntryType());
        assertEquals(15, entry.getAmount());
        assertEquals(35, entry.getBalanceAfter());
        assertEquals(reason, entry.getNote());

        // Adjust negative amount
        adminService.adjustCredits(admin.getId(), student.getId(), -10, "Manual correction");
        User afterDeduct = userDao.findById(student.getId()).orElseThrow();
        assertEquals(25, afterDeduct.getCredits());

        // Empty reason must throw ValidationException
        assertThrows(ValidationException.class, () -> {
            adminService.adjustCredits(admin.getId(), student.getId(), 5, "   ");
        });
    }

    @Test
    public void testAdminReportResolutionAndReviewHiding() throws Exception {
        User reporter = createTestUser("Reporter", 10);
        User reviewee = createTestUser("Reviewee", 10);
        User reviewer = createTestUser("Reviewer", 10);

        // Insert provider offered skill listing
        OfferedSkill off = new OfferedSkill();
        off.setUserId(reviewee.getId());
        off.setSkillId(1);
        off.setLevel(SkillLevel.INTERMEDIATE);
        off.setCreditsPerHour(3);
        off.setDescription("Test listing for admin review moderation");
        off.setActive(true);
        long listingId = listingDao.insert(off);

        // Insert a completed session request
        SessionRequest sr = new SessionRequest();
        sr.setLearnerId(reviewer.getId());
        sr.setProviderId(reviewee.getId());
        sr.setSkillId(1);
        sr.setListingId(listingId);
        sr.setProposedStart(LocalDateTime.now().minusDays(1));
        sr.setDurationMinutes(60);
        sr.setMode(SessionMode.ONLINE);
        sr.setMeetingDetails("https://meet.google.com/test");
        sr.setCostCredits(3);
        sr.setStatus(SessionStatus.COMPLETED);
        long reqId = sessionRequestDao.insert(sr);

        // Reviewer leaves a review
        Review r = new Review();
        r.setSessionRequestId(reqId);
        r.setReviewerId(reviewer.getId());
        r.setRevieweeId(reviewee.getId());
        r.setRating(5);
        r.setComment("Excellent session!");
        long reviewId = reviewDao.insert(r);

        // Reporter files a moderation report against the review
        Report report = new Report();
        report.setReporterId(reporter.getId());
        report.setTargetReviewId(reviewId);
        report.setReason("Inappropriate language in review comment");
        report.setStatus(ReportStatus.OPEN);
        long reportId = reportDao.insert(report);

        // Admin lists reports
        List<Report> openReports = adminService.listReports(ReportStatus.OPEN, 20, 0);
        assertTrue(openReports.stream().anyMatch(rep -> rep.getId() == reportId));

        // Admin hides the review
        adminService.hideReview(reporter.getId(), reviewId);
        Review hiddenReview = reviewDao.findById(reviewId).orElseThrow();
        assertTrue(hiddenReview.isHidden());

        // Admin resolves report
        adminService.resolveReport(reportId, reporter.getId(), ReportStatus.RESOLVED);
        List<Report> resolvedReports = adminService.listReports(ReportStatus.RESOLVED, 20, 0);
        assertTrue(resolvedReports.stream().anyMatch(rep -> rep.getId() == reportId));
    }
}
