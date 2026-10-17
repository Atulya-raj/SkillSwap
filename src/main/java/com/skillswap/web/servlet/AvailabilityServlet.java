package com.skillswap.web.servlet;

import com.skillswap.exception.ValidationException;
import com.skillswap.model.AvailabilitySlot;
import com.skillswap.model.SessionUser;
import com.skillswap.model.enums.SessionMode;
import com.skillswap.service.AvailabilityService;
import com.skillswap.util.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for viewing and configuring weekly availability slots.
 */
@WebServlet("/availability")
public class AvailabilityServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AvailabilityServlet.class.getName());

    private final AvailabilityService availabilityService;

    public AvailabilityServlet() {
        this(AvailabilityService.getInstance());
    }

    public AvailabilityServlet(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = getSessionUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        List<AvailabilitySlot> allSlots = availabilityService.slots(sessionUser.getId());

        // Group slots by DayOfWeek for convenient display in weekly grid
        Map<DayOfWeek, List<AvailabilitySlot>> weeklyGrid = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            weeklyGrid.put(day, new ArrayList<>());
        }
        for (AvailabilitySlot slot : allSlots) {
            if (slot.getDayOfWeek() != null) {
                weeklyGrid.get(slot.getDayOfWeek()).add(slot);
            }
        }

        request.setAttribute("slots", allSlots);
        request.setAttribute("weeklyGrid", weeklyGrid);
        request.setAttribute("daysOfWeek", DayOfWeek.values());
        request.setAttribute("sessionModes", SessionMode.values());

        request.getRequestDispatcher("/WEB-INF/jsp/availability.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SessionUser sessionUser = getSessionUser(request);
        if (sessionUser == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String action = request.getParameter("action");
        try {
            if ("add".equalsIgnoreCase(action)) {
                handleAdd(request, sessionUser);
            } else if ("remove".equalsIgnoreCase(action)) {
                handleRemove(request, sessionUser);
            } else {
                Flash.error(request, "Unknown action requested.");
            }
        } catch (ValidationException e) {
            Flash.error(request, e.getUserMessage());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error updating availability: " + action, e);
            Flash.error(request, "Operation failed: " + e.getMessage());
        }

        response.sendRedirect(request.getContextPath() + "/availability");
    }

    private void handleAdd(HttpServletRequest request, SessionUser sessionUser) throws ValidationException {
        String dayStr = request.getParameter("dayOfWeek");
        String startStr = request.getParameter("startTime");
        String endStr = request.getParameter("endTime");
        String modeStr = request.getParameter("mode");

        DayOfWeek day;
        try {
            day = DayOfWeek.valueOf(dayStr.trim().toUpperCase());
        } catch (Exception e) {
            throw new ValidationException("dayOfWeek", "Please select a valid day of the week.");
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(startStr.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ValidationException("startTime", "Please provide a valid start time (HH:mm).");
        }

        try {
            end = LocalTime.parse(endStr.trim());
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ValidationException("endTime", "Please provide a valid end time (HH:mm).");
        }

        SessionMode mode = SessionMode.ONLINE;
        if (modeStr != null && !modeStr.trim().isEmpty()) {
            try {
                mode = SessionMode.valueOf(modeStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        availabilityService.add(sessionUser.getId(), day, start, end, mode);
        Flash.success(request, "Availability slot added successfully!");
    }

    private void handleRemove(HttpServletRequest request, SessionUser sessionUser) {
        String idStr = request.getParameter("id");
        try {
            long slotId = Long.parseLong(idStr.trim());
            availabilityService.remove(sessionUser.getId(), slotId);
            Flash.success(request, "Availability slot removed.");
        } catch (Exception e) {
            Flash.error(request, "Invalid slot identifier.");
        }
    }

    private SessionUser getSessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        return (SessionUser) session.getAttribute(AuthServlet.SESSION_USER_KEY);
    }
}
