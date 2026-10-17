package com.skillswap.web.servlet;

import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.exception.AuthorizationException;
import com.skillswap.exception.ResourceNotFoundException;
import com.skillswap.exception.SkillSwapException;
import com.skillswap.model.Message;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.SessionUser;
import com.skillswap.model.User;
import com.skillswap.service.MessageService;
import com.skillswap.util.Flash;
import com.skillswap.web.filter.AuthFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servlet handling in-app messaging between session participants (F-22).
 */
@WebServlet(urlPatterns = {"/messages", "/messages/*"})
public class MessageServlet extends HttpServlet {

    private final MessageService messageService;
    private final SessionRequestDao sessionRequestDao;
    private final UserDao userDao;

    /**
     * Default constructor wiring standard services.
     */
    public MessageServlet() {
        this(MessageService.getInstance(), new JdbcSessionRequestDao(), new JdbcUserDao());
    }

    /**
     * Constructs MessageServlet with injected dependencies.
     *
     * @param messageService message service
     * @param sessionRequestDao session request DAO
     * @param userDao user DAO
     */
    public MessageServlet(MessageService messageService, SessionRequestDao sessionRequestDao, UserDao userDao) {
        this.messageService = messageService;
        this.sessionRequestDao = sessionRequestDao;
        this.userDao = userDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String reqIdStr = req.getParameter("requestId");
        if (reqIdStr != null && !reqIdStr.trim().isEmpty()) {
            try {
                long reqId = Long.parseLong(reqIdStr.trim());
                List<Message> messages = messageService.thread(user.getId(), reqId);
                Optional<SessionRequest> opt = sessionRequestDao.findById(reqId);
                if (opt.isEmpty()) {
                    Flash.error(req, "Session request not found.");
                    resp.sendRedirect(req.getContextPath() + "/requests");
                    return;
                }

                SessionRequest sr = opt.get();
                long partnerId = (user.getId() == sr.getLearnerId()) ? sr.getProviderId() : sr.getLearnerId();
                User partner = userDao.findById(partnerId).orElse(null);

                req.setAttribute("messages", messages);
                req.setAttribute("sessionRequest", sr);
                req.setAttribute("partner", partner);
                req.setAttribute("hasActiveThread", true);
            } catch (AuthorizationException e) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
                return;
            } catch (Exception e) {
                Flash.error(req, "Could not open message thread: " + e.getMessage());
                resp.sendRedirect(req.getContextPath() + "/requests");
                return;
            }
        } else {
            // No specific thread chosen - show active conversation sessions
            List<SessionRequest> conversations = new ArrayList<>();
            conversations.addAll(sessionRequestDao.findIncoming(user.getId(), null, 15, 0));
            conversations.addAll(sessionRequestDao.findOutgoing(user.getId(), null, 15, 0));
            req.setAttribute("conversations", conversations);
            req.setAttribute("hasActiveThread", false);
        }

        req.getRequestDispatcher("/WEB-INF/jsp/messages.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = session != null ? (SessionUser) session.getAttribute(AuthFilter.SESSION_USER_ATTR) : null;
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String reqIdStr = req.getParameter("requestId");
        if (reqIdStr == null || reqIdStr.trim().isEmpty()) {
            Flash.error(req, "No session thread specified.");
            resp.sendRedirect(req.getContextPath() + "/messages");
            return;
        }

        long reqId;
        try {
            reqId = Long.parseLong(reqIdStr.trim());
        } catch (NumberFormatException e) {
            Flash.error(req, "Invalid session thread ID.");
            resp.sendRedirect(req.getContextPath() + "/messages");
            return;
        }

        String body = req.getParameter("body");
        try {
            messageService.send(user.getId(), reqId, body);
        } catch (AuthorizationException e) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            return;
        } catch (SkillSwapException e) {
            Flash.error(req, e.getMessage());
        } catch (Exception e) {
            Flash.error(req, "Failed to send message: " + e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/messages?requestId=" + reqId);
    }
}
