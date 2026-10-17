<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Campus Leaderboard - SkillSwap" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>
<%@ include file="/WEB-INF/jsp/layout/nav.jspf" %>

<main class="container">
    <div class="page-header" style="margin-bottom: 2rem;">
        <h2>Campus Mentorship Leaderboard</h2>
        <p class="text-muted">Celebrating top student mentors and community skill teachers across our campus.</p>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(420px, 1fr)); gap: 1.5rem; margin-bottom: 2rem;">
        <!-- Top by Rating Column -->
        <div class="card" style="padding: 1.5rem;">
            <div style="display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid #e2e8f0; padding-bottom: 0.75rem; margin-bottom: 1rem;">
                <h3 style="margin: 0; font-size: 1.2rem; color: var(--color-primary); display: flex; align-items: center; gap: 0.5rem;">
                    <span>⭐</span> Highest Rated Mentors
                </h3>
                <span class="badge badge-secondary" style="font-size: 0.75rem;">Min. 3 reviews</span>
            </div>

            <c:choose>
                <c:when test="${empty topByRating}">
                    <div style="text-align: center; color: #94a3b8; padding: 2rem 0;">
                        <p style="margin: 0;">No mentors with 3+ reviews yet.</p>
                        <small>Complete sessions and leave reviews to see mentors appear here!</small>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="display: flex; flex-direction: column; gap: 0.75rem;">
                        <c:forEach var="m" items="${topByRating}" varStatus="status">
                            <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 1rem; border-radius: 8px; background: ${status.index == 0 ? '#fffbeb' : '#f8fafc'}; border: 1px solid ${status.index == 0 ? '#fde68a' : '#e2e8f0'};">
                                <div style="display: flex; align-items: center; gap: 0.75rem;">
                                    <div style="font-size: 1.25rem; font-weight: 700; width: 32px; text-align: center; color: #475569;">
                                        <c:choose>
                                            <c:when test="${status.index == 0}">🥇</c:when>
                                            <c:when test="${status.index == 1}">🥈</c:when>
                                            <c:when test="${status.index == 2}">🥉</c:when>
                                            <c:otherwise>#${status.index + 1}</c:otherwise>
                                        </c:choose>
                                    </div>
                                    <img src="${pageContext.request.contextPath}/avatar?id=${m.id}"
                                         alt="<c:out value='${m.fullName}'/>"
                                         style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover; border: 2px solid var(--color-primary);" />
                                    <div>
                                        <a href="${pageContext.request.contextPath}/users/view?id=${m.id}" style="font-weight: 600; color: #1e293b; text-decoration: none;">
                                            <c:out value="${m.fullName}"/>
                                        </a>
                                        <div style="font-size: 0.8rem; color: #64748b;">
                                            <c:out value="${m.department}"/> • Year <c:out value="${m.studyYear}"/>
                                        </div>
                                    </div>
                                </div>
                                <div style="text-align: right;">
                                    <div style="font-weight: 700; color: #d97706; font-size: 1rem;">
                                        ★ <c:out value="${m.avgRating}"/>
                                    </div>
                                    <div style="font-size: 0.75rem; color: #94a3b8;">
                                        <c:out value="${m.ratingCount}"/> reviews
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- Top by Sessions Taught Column -->
        <div class="card" style="padding: 1.5rem;">
            <div style="display: flex; align-items: center; justify-content: space-between; border-bottom: 2px solid #e2e8f0; padding-bottom: 0.75rem; margin-bottom: 1rem;">
                <h3 style="margin: 0; font-size: 1.2rem; color: var(--color-primary); display: flex; align-items: center; gap: 0.5rem;">
                    <span>🎓</span> Most Sessions Taught
                </h3>
                <span class="badge badge-secondary" style="font-size: 0.75rem;">Community leaders</span>
            </div>

            <c:choose>
                <c:when test="${empty topBySessions}">
                    <div style="text-align: center; color: #94a3b8; padding: 2rem 0;">
                        <p style="margin: 0;">No completed sessions recorded yet.</p>
                        <small>Start exchanging skills to populate the taught leaderboard!</small>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="display: flex; flex-direction: column; gap: 0.75rem;">
                        <c:forEach var="m" items="${topBySessions}" varStatus="status">
                            <div style="display: flex; align-items: center; justify-content: space-between; padding: 0.75rem 1rem; border-radius: 8px; background: ${status.index == 0 ? '#f0fdf4' : '#f8fafc'}; border: 1px solid ${status.index == 0 ? '#bbf7d0' : '#e2e8f0'};">
                                <div style="display: flex; align-items: center; gap: 0.75rem;">
                                    <div style="font-size: 1.25rem; font-weight: 700; width: 32px; text-align: center; color: #475569;">
                                        <c:choose>
                                            <c:when test="${status.index == 0}">🥇</c:when>
                                            <c:when test="${status.index == 1}">🥈</c:when>
                                            <c:when test="${status.index == 2}">🥉</c:when>
                                            <c:otherwise>#${status.index + 1}</c:otherwise>
                                        </c:choose>
                                    </div>
                                    <img src="${pageContext.request.contextPath}/avatar?id=${m.id}"
                                         alt="<c:out value='${m.fullName}'/>"
                                         style="width: 40px; height: 40px; border-radius: 50%; object-fit: cover; border: 2px solid var(--color-primary);" />
                                    <div>
                                        <a href="${pageContext.request.contextPath}/users/view?id=${m.id}" style="font-weight: 600; color: #1e293b; text-decoration: none;">
                                            <c:out value="${m.fullName}"/>
                                        </a>
                                        <div style="font-size: 0.8rem; color: #64748b;">
                                            <c:out value="${m.department}"/> • Year <c:out value="${m.studyYear}"/>
                                        </div>
                                    </div>
                                </div>
                                <div style="text-align: right;">
                                    <div style="font-weight: 700; color: #16a34a; font-size: 1.05rem;">
                                        <c:out value="${m.sessionsTaught}"/>
                                    </div>
                                    <div style="font-size: 0.75rem; color: #94a3b8;">
                                        sessions taught
                                    </div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
