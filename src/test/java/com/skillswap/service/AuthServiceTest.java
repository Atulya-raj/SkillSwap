package com.skillswap.service;

import com.skillswap.dao.CreditDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcCreditDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.exception.AuthenticationException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.CreditEntry;
import com.skillswap.model.RegistrationForm;
import com.skillswap.model.User;
import com.skillswap.model.enums.Role;
import com.skillswap.test.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit and integration tests for AuthService business logic and credential management.
 */
public class AuthServiceTest {

    private final AuthService authService = new AuthService();
    private final UserDao userDao = new JdbcUserDao();
    private final CreditDao creditDao = new JdbcCreditDao();

    @BeforeEach
    public void setUp() {
        TestData.cleanData();
    }

    @Test
    @DisplayName("Register happy path: creates user, awards 10 signup bonus, writes ledger row, and logs in")
    public void testRegisterAndLoginHappyPath() throws Exception {
        RegistrationForm form = new RegistrationForm();
        form.setFullName("David Miller");
        form.setEmail("david.miller@example.com");
        form.setPassword("StrongPass123");
        form.setConfirmPassword("StrongPass123");
        form.setDepartment("Electrical Engineering");
        form.setStudyYear(2);
        form.setBio("Passionate about robotics and circuitry.");

        User registered = authService.register(form);
        assertNotNull(registered);
        assertTrue(registered.getId() > 0);
        assertEquals("David Miller", registered.getFullName());
        assertEquals("david.miller@example.com", registered.getEmail());
        assertEquals(Role.STUDENT, registered.getRole());
        assertEquals(10, registered.getCredits());

        // Verify credit ledger has SIGNUP_BONUS row
        List<CreditEntry> ledger = creditDao.findByUser(registered.getId(), 10, 0);
        assertEquals(1, ledger.size());
        assertEquals(10, ledger.get(0).getAmount());
        assertEquals(10, ledger.get(0).getBalanceAfter());

        // Verify login
        User loggedIn = authService.login("david.miller@example.com", "StrongPass123");
        assertEquals(registered.getId(), loggedIn.getId());

        // Verify password change
        authService.changePassword(registered.getId(), "StrongPass123", "NewSecret456");
        User reLoggedIn = authService.login("david.miller@example.com", "NewSecret456");
        assertEquals(registered.getId(), reLoggedIn.getId());
    }

    @Test
    @DisplayName("Duplicate email rejection: register throws ValidationException")
    public void testDuplicateEmailRejected() {
        TestData.insertStudent("Existing User", "existing@example.com", "Secret123!", 10);

        RegistrationForm form = new RegistrationForm();
        form.setFullName("Duplicate Tester");
        form.setEmail("existing@example.com");
        form.setPassword("Secret123!");
        form.setConfirmPassword("Secret123!");

        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(form));
        assertTrue(ex.getFieldErrors().containsKey("email"));
        assertTrue(ex.getFieldErrors().get("email").contains("already exists"));
    }

    @Test
    @DisplayName("Weak password rejection: register throws ValidationException")
    public void testWeakPasswordRejected() {
        RegistrationForm form = new RegistrationForm();
        form.setFullName("Weak Pass User");
        form.setEmail("weak@example.com");
        form.setPassword("short");
        form.setConfirmPassword("short");

        ValidationException ex = assertThrows(ValidationException.class, () -> authService.register(form));
        assertTrue(ex.getFieldErrors().containsKey("password"));
    }

    @Test
    @DisplayName("Inactive account rejection: login throws AuthenticationException")
    public void testInactiveAccountRejected() {
        User user = TestData.insertStudent("Inactive Student", "inactive@example.com", "Secret123!", 10);
        userDao.setActive(user.getId(), false);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login("inactive@example.com", "Secret123!"));
        assertTrue(ex.getMessage().contains("deactivated"));
    }

    @Test
    @DisplayName("Invalid password rejection: login throws AuthenticationException")
    public void testInvalidPasswordRejected() {
        TestData.insertStudent("Valid Student", "valid@example.com", "Secret123!", 10);

        AuthenticationException ex = assertThrows(AuthenticationException.class,
                () -> authService.login("valid@example.com", "WrongPassword999"));
        assertTrue(ex.getMessage().contains("Invalid email or password"));
    }
}
