package com.skillswap.web.servlet;

import com.skillswap.model.CreditEntry;
import com.skillswap.model.SessionUser;
import com.skillswap.service.CreditService;
import com.skillswap.util.Paginator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller servlet for credit balance wallet, escrow breakdown, and ledger history.
 */
@WebServlet("/credits")
public class CreditServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CreditServlet.class.getName());

    private final CreditService creditService;

    /**
     * Default constructor using standard CreditService.
     */
    public CreditServlet() {
        this(new CreditService());
    }

    /**
     * Constructs CreditServlet with injected CreditService.
     *
     * @param creditService credit service
     */
    public CreditServlet(CreditService creditService) {
        this.creditService = creditService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        SessionUser user = null;
        if (session != null) {
            user = (SessionUser) session.getAttribute("currentUser");
            if (user == null) {
                user = (SessionUser) session.getAttribute("user");
            }
        }
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        try {
            long userId = user.getId();
            int balance = creditService.getBalance(userId);
            int heldCredits = creditService.getHeldCredits(userId);
            int totalEarned = creditService.getTotalEarned(userId);

            int page = Paginator.parsePage(req.getParameter("page"));
            int pageSize = 15;
            int totalEntries = creditService.countLedger(userId);
            Paginator paginator = new Paginator(totalEntries, pageSize, page);

            List<CreditEntry> ledger = creditService.getLedger(userId, paginator.getLimit(), paginator.getOffset());

            req.setAttribute("balance", balance);
            req.setAttribute("heldCredits", heldCredits);
            req.setAttribute("totalEarned", totalEarned);
            req.setAttribute("ledger", ledger);
            req.setAttribute("paginator", paginator);

            req.getRequestDispatcher("/WEB-INF/jsp/credits.jsp").forward(req, resp);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed loading credit wallet for user " + user.getId(), e);
            throw new ServletException(e);
        }
    }
}
