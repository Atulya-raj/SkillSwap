package com.skillswap.web.servlet;

import com.skillswap.dao.AvailabilityDao;
import com.skillswap.dao.ListingDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcAvailabilityDao;
import com.skillswap.dao.jdbc.JdbcListingDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.exception.AuthorizationException;
import com.skillswap.exception.SkillSwapException;
import com.skillswap.exception.ValidationException;
import com.skillswap.model.AvailabilitySlot;
import com.skillswap.model.RequestDraft;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.SessionUser;
import com.skillswap.model.SkillListing;
import com.skillswap.model.User;
import com.skillswap.model.enums.ListingType;
import com.skillswap.model.enums.Role;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.model.enums.SessionStatus;
import com.skillswap.service.DraftService;
import com.skillswap.service.SessionService;
import com.skillswap.util.Flash;
import com.skillswap.util.Paginator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Controller servlet for session requests, negotiations, lifecycle updates, and drafts.
 */
@WebServlet(urlPatterns = {"/requests", "/requests/*", "/cancel", "/confirm", "/noshow"})
public class RequestServlet extends HttpServlet {

    private final SessionService sessionService;
    private final ListingDao listingDao;
    private final UserDao userDao;
    private final AvailabilityDao availabilityDao;
    private final DraftService draftService;

    /**
     * Default constructor with production dependencies.
     */
    public RequestServlet() {
        this(new SessionService(), new JdbcListingDao(), new JdbcUserDao(), new JdbcAvailabilityDao(), new DraftService());
    }

    /**
     * Constructs RequestServlet with injected services.
     */
    public RequestServlet(SessionService sessionService, ListingDao listingDao, UserDao userDao,
                          AvailabilityDao availabilityDao, DraftService draftService) {
        this.sessionService = sessionService;
        this.listingDao = listingDao;
        this.userDao = userDao;
        this.availabilityDao = availabilityDao;
        this.draftService = draftService;
    }

    private SessionUser getCurrentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        SessionUser su = (SessionUser) session.getAttribute("currentUser");
        if (su == null) {
            su = (SessionUser) session.getAttribute("user");
        }
        return su;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = getCurrentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String path = req.getServletPath();
        String pathInfo = req.getPathInfo();

        if ("/requests/new".equals(path + (pathInfo != null ? pathInfo : "")) || "/requests/new".equals(path)) {
            handleNewForm(req, resp, user);
        } else if ("/requests/view".equals(path + (pathInfo != null ? pathInfo : "")) || "/requests/view".equals(path)) {
            handleViewDetail(req, resp, user);
        } else {
            handleList(req, resp, user);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        SessionUser user = getCurrentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        String path = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String fullPath = path + (pathInfo != null ? pathInfo : "");

        try {
            if ("/requests/new".equals(fullPath) || "/requests/draft".equals(fullPath)) {
                handleSubmitOrDraft(req, resp, user);
            } else if ("/requests/respond".equals(fullPath)) {
                handleRespond(req, resp, user);
            } else if ("/cancel".equals(path)) {
                handleCancel(req, resp, user);
            } else if ("/confirm".equals(path)) {
                handleConfirm(req, resp, user);
            } else if ("/noshow".equals(path)) {
                handleNoShow(req, resp, user);
            } else {
                resp.sendRedirect(req.getContextPath() + "/requests");
            }
        } catch (SkillSwapException e) {
            Flash.error(req, e.getMessage());
            String referer = req.getHeader("Referer");
            if (referer != null && !referer.isEmpty()) {
                resp.sendRedirect(referer);
            } else {
                resp.sendRedirect(req.getContextPath() + "/requests");
            }
        }
    }

    private void handleList(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        String tab = req.getParameter("tab");
        if (tab == null || (!"incoming".equalsIgnoreCase(tab) && !"outgoing".equalsIgnoreCase(tab))) {
            tab = "incoming";
        }
        tab = tab.toLowerCase();

        String statusParam = req.getParameter("status");
        SessionStatus filterStatus = null;
        if (statusParam != null && !statusParam.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusParam.trim())) {
            try {
                filterStatus = SessionStatus.valueOf(statusParam.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        int page = Paginator.parsePage(req.getParameter("page"));
        int pageSize = 10;

        List<SessionRequest> list;
        int total;

        if ("outgoing".equals(tab)) {
            total = sessionService.countOutgoing(user.getId(), filterStatus);
            Paginator paginator = new Paginator(total, pageSize, page);
            list = sessionService.getOutgoing(user.getId(), filterStatus, paginator.getLimit(), paginator.getOffset());
            req.setAttribute("paginator", paginator);
        } else {
            total = sessionService.countIncoming(user.getId(), filterStatus);
            Paginator paginator = new Paginator(total, pageSize, page);
            list = sessionService.getIncoming(user.getId(), filterStatus, paginator.getLimit(), paginator.getOffset());
            req.setAttribute("paginator", paginator);
        }

        req.setAttribute("tab", tab);
        req.setAttribute("statusFilter", filterStatus != null ? filterStatus.name() : "ALL");
        req.setAttribute("requests", list);
        req.setAttribute("pendingIncomingCount", sessionService.countPendingIncoming(user.getId()));

        req.getRequestDispatcher("/WEB-INF/jsp/requests.jsp").forward(req, resp);
    }

    private void handleNewForm(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        String listingIdStr = req.getParameter("listingId");
        if (listingIdStr == null || listingIdStr.trim().isEmpty()) {
            Flash.error(req, "No skill listing selected for session request");
            resp.sendRedirect(req.getContextPath() + "/browse");
            return;
        }

        long listingId;
        try {
            listingId = Long.parseLong(listingIdStr.trim());
        } catch (NumberFormatException e) {
            Flash.error(req, "Invalid listing ID");
            resp.sendRedirect(req.getContextPath() + "/browse");
            return;
        }

        Optional<SkillListing> listingOpt = listingDao.findById(listingId);
        if (listingOpt.isEmpty() || !listingOpt.get().isActive() || listingOpt.get().getType() != ListingType.OFFERED) {
            Flash.error(req, "Skill listing is not available for session requests");
            resp.sendRedirect(req.getContextPath() + "/browse");
            return;
        }

        SkillListing listing = listingOpt.get();
        if (listing.getUserId() == user.getId()) {
            Flash.warning(req, "You cannot request a session with yourself");
            resp.sendRedirect(req.getContextPath() + "/browse");
            return;
        }

        Optional<User> providerOpt = userDao.findById(listing.getUserId());
        List<AvailabilitySlot> slots = availabilityDao.findByUser(listing.getUserId());
        Optional<RequestDraft> draftOpt = draftService.load(user.getId(), listingId);

        req.setAttribute("listing", listing);
        req.setAttribute("provider", providerOpt.orElse(null));
        req.setAttribute("slots", slots);
        req.setAttribute("draft", draftOpt.orElse(null));

        req.getRequestDispatcher("/WEB-INF/jsp/request-form.jsp").forward(req, resp);
    }

    private void handleViewDetail(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/requests");
            return;
        }

        long id;
        try {
            id = Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/requests");
            return;
        }

        Optional<SessionRequest> opt = sessionService.getById(id);
        if (opt.isEmpty()) {
            Flash.error(req, "Session request not found");
            resp.sendRedirect(req.getContextPath() + "/requests");
            return;
        }

        SessionRequest sr = opt.get();
        boolean isLearner = (user.getId() == sr.getLearnerId());
        boolean isProvider = (user.getId() == sr.getProviderId());
        boolean isAdmin = (user.getRole() == Role.ADMIN);

        if (!isLearner && !isProvider && !isAdmin) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied to session request #" + id);
            return;
        }

        req.setAttribute("sessionRequest", sr);
        req.setAttribute("learner", userDao.findById(sr.getLearnerId()).orElse(null));
        req.setAttribute("provider", userDao.findById(sr.getProviderId()).orElse(null));
        req.setAttribute("isLearner", isLearner);
        req.setAttribute("isProvider", isProvider);

        req.getRequestDispatcher("/WEB-INF/jsp/request-detail.jsp").forward(req, resp);
    }

    private void handleSubmitOrDraft(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws ServletException, IOException, SkillSwapException {
        long listingId = Long.parseLong(req.getParameter("listingId"));
        String action = req.getParameter("action");

        if ("discard_draft".equalsIgnoreCase(action)) {
            draftService.delete(user.getId(), listingId);
            Flash.info(req, "Saved draft discarded");
            resp.sendRedirect(req.getContextPath() + "/requests/new?listingId=" + listingId);
            return;
        }

        String startStr = req.getParameter("proposedStart");
        String durationStr = req.getParameter("durationMinutes");
        String modeStr = req.getParameter("mode");
        String meetingDetails = req.getParameter("meetingDetails");
        String message = req.getParameter("message");

        int duration = 60;
        try {
            if (durationStr != null && !durationStr.trim().isEmpty()) {
                duration = Integer.parseInt(durationStr.trim());
            }
        } catch (NumberFormatException ignored) {
        }

        SessionMode mode = SessionMode.ONLINE;
        if (modeStr != null) {
            try {
                mode = SessionMode.valueOf(modeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        if ("draft".equalsIgnoreCase(action)) {
            RequestDraft draft = new RequestDraft();
            draft.setProviderListingId(listingId);
            draft.setProposedStart(startStr);
            draft.setDurationMinutes(duration);
            draft.setMode(mode);
            draft.setMeetingDetails(meetingDetails);
            draft.setMessage(message);
            draft.setSavedAt(LocalDateTime.now());

            draftService.save(user.getId(), listingId, draft);
            Flash.success(req, "Draft saved successfully. You can return later to submit your request.");
            resp.sendRedirect(req.getContextPath() + "/requests/new?listingId=" + listingId);
            return;
        }

        LocalDateTime proposedStart;
        try {
            if (startStr == null || startStr.trim().isEmpty()) {
                throw new ValidationException(java.util.Map.of("proposedStart", "Scheduled date and time is required"));
            }
            proposedStart = LocalDateTime.parse(startStr.trim());
        } catch (DateTimeParseException e) {
            throw new ValidationException(java.util.Map.of("proposedStart", "Invalid date and time format"));
        }

        SessionRequest sr = sessionService.create(user.getId(), listingId, proposedStart, duration, mode, meetingDetails, message);
        Flash.success(req, "Session request sent successfully! " + sr.getCostCredits() + " credits held in escrow.");
        resp.sendRedirect(req.getContextPath() + "/requests/view?id=" + sr.getId());
    }

    private void handleRespond(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws IOException, SkillSwapException {
        long id = Long.parseLong(req.getParameter("id"));
        String decision = req.getParameter("decision");

        if ("accept".equalsIgnoreCase(decision)) {
            sessionService.accept(user.getId(), id);
            Flash.success(req, "Session request accepted!");
        } else if ("reject".equalsIgnoreCase(decision)) {
            sessionService.reject(user.getId(), id);
            Flash.info(req, "Session request rejected. Escrow credits released back to learner.");
        } else {
            Flash.error(req, "Invalid decision");
        }
        resp.sendRedirect(req.getContextPath() + "/requests/view?id=" + id);
    }

    private void handleCancel(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws IOException, SkillSwapException {
        long id = Long.parseLong(req.getParameter("id"));
        sessionService.cancel(user.getId(), id);
        Flash.info(req, "Session request cancelled per credit refund policy.");
        resp.sendRedirect(req.getContextPath() + "/requests/view?id=" + id);
    }

    private void handleConfirm(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws IOException, SkillSwapException {
        long id = Long.parseLong(req.getParameter("id"));
        SessionRequest sr = sessionService.confirm(user.getId(), id);

        if (sr.getStatus() == SessionStatus.COMPLETED) {
            Flash.success(req, "Both parties confirmed! Session completed and " + sr.getCostCredits() + " credits transferred.");
        } else {
            Flash.info(req, "Your completion confirmation was recorded. Waiting for your swap partner to confirm.");
        }
        resp.sendRedirect(req.getContextPath() + "/requests/view?id=" + id);
    }

    private void handleNoShow(HttpServletRequest req, HttpServletResponse resp, SessionUser user)
            throws IOException, SkillSwapException {
        long id = Long.parseLong(req.getParameter("id"));
        SessionRequest sr = sessionService.markNoShow(user.getId(), id);
        Flash.warning(req, "No-show report recorded. Status updated to " + sr.getStatus());
        resp.sendRedirect(req.getContextPath() + "/requests/view?id=" + id);
    }
}
