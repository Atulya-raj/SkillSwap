package com.skillswap.service;

import com.skillswap.util.Config;
import com.skillswap.dao.CreditDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcCreditDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.db.TransactionManager;
import com.skillswap.exception.AuthenticationException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.CreditEntry;
import com.skillswap.model.RegistrationForm;
import com.skillswap.model.Student;
import com.skillswap.model.User;
import com.skillswap.model.enums.LedgerType;
import com.skillswap.model.enums.Role;
import com.skillswap.util.PasswordHasher;
import com.skillswap.util.Validator;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication and credential management service.
 */
public class AuthService {

    private final UserDao userDao;
    private final CreditDao creditDao;

    /**
     * Default constructor using standard JDBC DAOs.
     */
    public AuthService() {
        this(new JdbcUserDao(), new JdbcCreditDao());
    }

    /**
     * Constructs AuthService with injected DAOs.
     *
     * @param userDao user DAO
     * @param creditDao credit ledger DAO
     */
    public AuthService(UserDao userDao, CreditDao creditDao) {
        this.userDao = userDao;
        this.creditDao = creditDao;
    }

    /**
     * Registers a new student user and grants the signup bonus atomically.
     *
     * @param form user registration form data
     * @return newly created User
     * @throws ValidationException if any form input fails validation
     */
    public User register(RegistrationForm form) throws ValidationException {
        Map<String, String> errors = new HashMap<>();

        if (form.getFullName() == null || form.getFullName().trim().length() < 2 || form.getFullName().trim().length() > 100) {
            errors.put("fullName", "Name must be between 2 and 100 characters");
        }

        String email = form.getEmail() != null ? form.getEmail().trim().toLowerCase() : "";
        if (!Validator.isValidEmail(email)) {
            errors.put("email", "Please provide a valid email address");
        } else {
            String allowedDomain = Config.get("allowed.email.domain");
            if (allowedDomain != null && !allowedDomain.trim().isEmpty()) {
                String domainSuffix = "@" + allowedDomain.trim().toLowerCase();
                if (!email.endsWith(domainSuffix)) {
                    errors.put("email", "Email must be from the @" + allowedDomain.trim() + " domain");
                }
            }
            if (userDao.emailExists(email)) {
                errors.put("email", "An account with this email address already exists");
            }
        }

        if (!Validator.isStrongPassword(form.getPassword())) {
            errors.put("password", "Password must be at least 8 characters long and contain both letters and digits");
        } else if (form.getConfirmPassword() == null || !form.getPassword().equals(form.getConfirmPassword())) {
            errors.put("confirmPassword", "Passwords do not match");
        }

        if (form.getStudyYear() != null && (form.getStudyYear() < 1 || form.getStudyYear() > 5)) {
            errors.put("studyYear", "Study year must be between 1 and 5");
        }

        if (form.getBio() != null && form.getBio().length() > 500) {
            errors.put("bio", "Bio cannot exceed 500 characters");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        try {
            return TransactionManager.execute(() -> {
                Student student = new Student();
                student.setFullName(form.getFullName().trim());
                student.setEmail(email);
                student.setPasswordHash(PasswordHasher.hash(form.getPassword()));
                student.setDepartment(form.getDepartment() != null ? form.getDepartment().trim() : null);
                student.setStudyYear(form.getStudyYear());
                student.setBio(form.getBio() != null ? form.getBio().trim() : null);
                student.setRole(Role.STUDENT);
                student.setActive(true);

                int signupBonus = Config.getInt("signup.bonus", 10);
                student.setCredits(signupBonus);

                long userId = userDao.insert(student);
                student.setId(userId);

                if (signupBonus > 0) {
                    CreditEntry bonusEntry = new CreditEntry();
                    bonusEntry.setUserId(userId);
                    bonusEntry.setEntryType(LedgerType.SIGNUP_BONUS);
                    bonusEntry.setAmount(signupBonus);
                    bonusEntry.setBalanceAfter(signupBonus);
                    bonusEntry.setNote("Welcome signup bonus");
                    creditDao.insert(bonusEntry);
                }

                return student;
            });
        } catch (com.skillswap.exception.ValidationException ve) {
            throw ve;
        } catch (com.skillswap.exception.SkillSwapException se) {
            throw new ValidationException(se.getMessage());
        }
    }

    /**
     * Authenticates a user with email and password.
     *
     * @param email login email
     * @param password raw password
     * @return authenticated User
     * @throws AuthenticationException if authentication fails or account is deactivated
     */
    public User login(String email, String password) throws AuthenticationException {
        if (email == null || email.trim().isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Email and password are required");
        }

        User user = userDao.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!user.isActive()) {
            throw new AuthenticationException("Your account has been deactivated. Please contact an administrator.");
        }

        if (!PasswordHasher.verify(password, user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        userDao.touchLogin(user.getId());
        return user;
    }

    /**
     * Changes password for an existing user.
     *
     * @param userId user ID
     * @param currentPassword current plain text password
     * @param newPassword new plain text password
     * @throws AuthenticationException if user not found or current password incorrect
     * @throws ValidationException if new password is weak
     */
    public void changePassword(long userId, String currentPassword, String newPassword)
            throws AuthenticationException, ValidationException {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found"));

        if (!PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        if (!Validator.isStrongPassword(newPassword)) {
            throw new ValidationException("newPassword", "Password must be at least 8 characters long and contain both letters and digits");
        }

        String newHash = PasswordHasher.hash(newPassword);
        userDao.updatePassword(userId, newHash);
    }
}
