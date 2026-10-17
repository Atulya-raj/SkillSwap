package com.skillswap.web.servlet;

import com.skillswap.exception.AuthenticationException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.RegistrationForm;
import com.skillswap.model.SessionUser;
import com.skillswap.model.User;
import com.skillswap.model.enums.Role;
import com.skillswap.service.AuthService;
import com.skillswap.util.Flash;
import com.skillswap.web.filter.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Controller handling user registration, authentication, logout, and credential updates.
 */
@WebServlet(urlPatterns = {"/register", "/login", "/logout", "/password"})
public class AuthServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        HttpSession session = req.getSession(false);
        SessionUser currentUser = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;

        if ("/login".equals(path)) {
            if (currentUser != null) {
                resp.sendRedirect(req.getContextPath() + (currentUser.getRole() == Role.ADMIN ? "/admin" : "/dashboard"));
                return;
            }
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
        } else if ("/register".equals(path)) {
            if (currentUser != null) {
                resp.sendRedirect(req.getContextPath() + (currentUser.getRole() == Role.ADMIN ? "/admin" : "/dashboard"));
                return;
            }
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
        } else if ("/password".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/jsp/password.jsp").forward(req, resp);
        } else if ("/logout".equals(path)) {
            doPost(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/login");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if ("/register".equals(path)) {
            handleRegister(req, resp);
        } else if ("/login".equals(path)) {
            handleLogin(req, resp);
        } else if ("/logout".equals(path)) {
            handleLogout(req, resp);
        } else if ("/password".equals(path)) {
            handlePasswordChange(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/login");
        }
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        RegistrationForm form = new RegistrationForm();
        form.setFullName(req.getParameter("fullName"));
        form.setEmail(req.getParameter("email"));
        form.setPassword(req.getParameter("password"));
        form.setConfirmPassword(req.getParameter("confirmPassword"));
        form.setDepartment(req.getParameter("department"));
        String yearStr = req.getParameter("studyYear");
        if (yearStr != null && !yearStr.trim().isEmpty()) {
            try {
                form.setStudyYear(Integer.parseInt(yearStr.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        form.setBio(req.getParameter("bio"));

        try {
            authService.register(form);
            Flash.success(req, "Registration successful! You have received 10 welcome credits. Please sign in.");
            resp.sendRedirect(req.getContextPath() + "/login");
        } catch (ValidationException ve) {
            req.setAttribute("form", form);
            req.setAttribute("errors", ve.getFieldErrors());
            Flash.error(req, "Please correct the highlighted errors.");
            req.getRequestDispatcher("/WEB-INF/jsp/register.jsp").forward(req, resp);
        }
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String password = req.getParameter("password");

        try {
            User user = authService.login(email, password);

            // Session fixation protection
            req.changeSessionId();

            SessionUser sessionUser = new SessionUser(user.getId(), user.displayName(), user.getRole(), user.getAvatarPath());
            HttpSession session = req.getSession(true);
            session.setAttribute(AuthFilter.SESSION_USER_ATTR, sessionUser);

            Flash.success(req, "Welcome back, " + user.displayName() + "!");

            String target = (String) session.getAttribute(AuthFilter.REDIRECT_ATTR);
            if (target != null && !target.isEmpty()) {
                session.removeAttribute(AuthFilter.REDIRECT_ATTR);
                resp.sendRedirect(target);
            } else {
                resp.sendRedirect(req.getContextPath() + user.homePath());
            }
        } catch (AuthenticationException ae) {
            Flash.error(req, ae.getMessage());
            req.setAttribute("email", email);
            req.getRequestDispatcher("/WEB-INF/jsp/login.jsp").forward(req, resp);
        }
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        Flash.info(req, "You have been logged out successfully.");
        resp.sendRedirect(req.getContextPath() + "/login");
    }

    private void handlePasswordChange(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser currentUser = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;
        if (currentUser == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String currentPassword = req.getParameter("currentPassword");
        String newPassword = req.getParameter("newPassword");
        String confirmPassword = req.getParameter("confirmPassword");

        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            Flash.error(req, "New passwords do not match.");
            req.getRequestDispatcher("/WEB-INF/jsp/password.jsp").forward(req, resp);
            return;
        }

        try {
            authService.changePassword(currentUser.getId(), currentPassword, newPassword);
            Flash.success(req, "Your password has been changed successfully.");
            resp.sendRedirect(req.getContextPath() + (currentUser.getRole() == Role.ADMIN ? "/admin" : "/dashboard"));
        } catch (ValidationException ve) {
            req.setAttribute("errors", ve.getFieldErrors());
            Flash.error(req, "Please correct the highlighted errors.");
            req.getRequestDispatcher("/WEB-INF/jsp/password.jsp").forward(req, resp);
        } catch (AuthenticationException ae) {
            Flash.error(req, ae.getMessage());
            req.getRequestDispatcher("/WEB-INF/jsp/password.jsp").forward(req, resp);
        }
    }
}
