package com.skillswap.web.servlet;

import com.skillswap.dao.SearchCriteria;
import com.skillswap.model.enums.SkillLevel;
import com.skillswap.service.CatalogService;
import com.skillswap.service.SearchService;
import com.skillswap.util.Paginator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for browsing and searching available skill providers.
 */
@WebServlet("/browse")
public class BrowseServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(BrowseServlet.class.getName());

    private final SearchService searchService;
    private final CatalogService catalogService;

    public BrowseServlet() {
        this(SearchService.getInstance(), CatalogService.getInstance());
    }

    public BrowseServlet(SearchService searchService, CatalogService catalogService) {
        this.searchService = searchService;
        this.catalogService = catalogService;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        SearchCriteria criteria = new SearchCriteria();

        String keyword = request.getParameter("keyword");
        if (keyword != null && !keyword.trim().isEmpty()) {
            criteria.setKeyword(keyword.trim());
        }

        String catParam = request.getParameter("categoryId");
        if (catParam != null && !catParam.trim().isEmpty()) {
            try {
                int catId = Integer.parseInt(catParam.trim());
                if (catId > 0) criteria.setCategoryId(catId);
            } catch (NumberFormatException ignored) {
            }
        }

        String skillParam = request.getParameter("skillId");
        if (skillParam != null && !skillParam.trim().isEmpty()) {
            try {
                int sId = Integer.parseInt(skillParam.trim());
                if (sId > 0) criteria.setSkillId(sId);
            } catch (NumberFormatException ignored) {
            }
        }

        String levelParam = request.getParameter("level");
        if (levelParam != null && !levelParam.trim().isEmpty()) {
            try {
                criteria.setLevel(SkillLevel.valueOf(levelParam.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String minRatingParam = request.getParameter("minRating");
        if (minRatingParam != null && !minRatingParam.trim().isEmpty()) {
            try {
                criteria.setMinRating(new BigDecimal(minRatingParam.trim()));
            } catch (Exception ignored) {
            }
        }

        String dayParam = request.getParameter("dayOfWeek");
        if (dayParam != null && !dayParam.trim().isEmpty()) {
            try {
                criteria.setDayOfWeek(DayOfWeek.valueOf(dayParam.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        String sortParam = request.getParameter("sort");
        if (sortParam != null && !sortParam.trim().isEmpty()) {
            criteria.setSort(sortParam.trim().toUpperCase());
        } else {
            criteria.setSort("BEST");
        }

        int page = 1;
        String pageParam = request.getParameter("page");
        if (pageParam != null && !pageParam.trim().isEmpty()) {
            try {
                page = Integer.parseInt(pageParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            Paginator.Page<SearchService.ProviderCard> pageResult = searchService.search(criteria, page, 10);

            request.setAttribute("pageResult", pageResult);
            request.setAttribute("criteria", criteria);
            request.setAttribute("categories", catalogService.categories());
            request.setAttribute("skillsByCategory", catalogService.skillsByCategory());
            request.setAttribute("skillLevels", SkillLevel.values());
            request.setAttribute("daysOfWeek", DayOfWeek.values());

            request.getRequestDispatcher("/WEB-INF/jsp/browse.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Search and browse failed", e);
            throw new ServletException(e);
        }
    }
}
