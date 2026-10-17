<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Admin Analytics — SkillSwap" />
<%@ include file="../layout/header.jspf" %>
<%@ include file="../layout/nav.jspf" %>

<main class="container py-4">
    <!-- Admin Page Header -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
        <div>
            <span class="badge badge-primary" style="margin-bottom: 0.35rem;">Platform Administration</span>
            <h1 style="margin: 0; font-size: 1.85rem; color: var(--color-primary-dark);">Platform Overview & Analytics</h1>
        </div>
        <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
            <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline btn-sm" id="btn-admin-manage-users">Manage Users</a>
            <a href="${pageContext.request.contextPath}/admin/catalog" class="btn btn-outline btn-sm" id="btn-admin-manage-catalog">Catalog & CSV</a>
            <a href="${pageContext.request.contextPath}/admin/reports" class="btn btn-outline btn-sm" id="btn-admin-view-reports">Reports Queue</a>
            <a href="${pageContext.request.contextPath}/admin/users/export" class="btn btn-primary btn-sm" id="btn-admin-export-users" title="Export all registered users">Export Users CSV ⬇</a>
        </div>
    </div>

    <!-- Stat Highlights Row -->
    <div class="grid-4 mb-4">
        <!-- Total Users -->
        <div class="stat-card" id="stat-total-users">
            <div class="stat-header">
                <span class="stat-label">Total Students</span>
                <span class="stat-icon">👥</span>
            </div>
            <div class="stat-value">
                <c:out value="${analytics.totalUsers}" />
            </div>
            <div class="stat-subtext">
                Registered student peers
            </div>
        </div>

        <!-- Users Online Now -->
        <div class="stat-card" id="stat-users-online">
            <div class="stat-header">
                <span class="stat-label">Users Online Now</span>
                <span class="stat-icon">🟢</span>
            </div>
            <div class="stat-value" style="color: var(--color-success);">
                <c:out value="${analytics.activeUsersOnline}" />
            </div>
            <div class="stat-subtext">
                Active HTTP sessions in memory
            </div>
        </div>

        <!-- Total Sessions & Completion Rate -->
        <div class="stat-card" id="stat-sessions-rate">
            <div class="stat-header">
                <span class="stat-label">Completion Rate</span>
                <span class="stat-icon">🎯</span>
            </div>
            <div class="stat-value">
                <c:out value="${analytics.completionRateFormatted}" />%
            </div>
            <div class="stat-subtext">
                <c:out value="${analytics.completedSessions}" /> of <c:out value="${analytics.totalSessions}" /> total sessions
            </div>
        </div>

        <!-- Credits in Circulation -->
        <div class="stat-card" id="stat-credits-circulation">
            <div class="stat-header">
                <span class="stat-label">Campus Economy</span>
                <span class="stat-icon">🪙</span>
            </div>
            <div class="stat-value">
                <c:out value="${analytics.totalCreditsInCirculation}" />
            </div>
            <div class="stat-subtext">
                Active credits in circulation
            </div>
        </div>
    </div>

    <!-- Session Status Distribution -->
    <div class="card mb-4" id="section-session-breakdown">
        <h2 style="font-size: 1.25rem; margin-top: 0; margin-bottom: 1rem;">📊 Sessions by Status</h2>
        <div class="grid-4" style="gap: 1rem;">
            <div style="padding: 0.85rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-align: center; background-color: var(--color-surface);">
                <span class="badge badge-warning" style="font-size: 0.75rem; margin-bottom: 0.35rem;">PENDING</span>
                <div style="font-size: 1.5rem; font-weight: 700; color: var(--color-primary-dark);"><c:out value="${analytics.sessionsByStatus['PENDING']}" /></div>
            </div>
            <div style="padding: 0.85rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-align: center; background-color: var(--color-surface);">
                <span class="badge badge-info" style="font-size: 0.75rem; margin-bottom: 0.35rem;">ACCEPTED</span>
                <div style="font-size: 1.5rem; font-weight: 700; color: var(--color-primary-dark);"><c:out value="${analytics.sessionsByStatus['ACCEPTED']}" /></div>
            </div>
            <div style="padding: 0.85rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-align: center; background-color: var(--color-surface);">
                <span class="badge badge-success" style="font-size: 0.75rem; margin-bottom: 0.35rem;">COMPLETED</span>
                <div style="font-size: 1.5rem; font-weight: 700; color: var(--color-primary-dark);"><c:out value="${analytics.sessionsByStatus['COMPLETED']}" /></div>
            </div>
            <div style="padding: 0.85rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-align: center; background-color: var(--color-surface);">
                <span class="badge badge-danger" style="font-size: 0.75rem; margin-bottom: 0.35rem;">CANCELLED / EXPIRED</span>
                <div style="font-size: 1.5rem; font-weight: 700; color: var(--color-primary-dark);">
                    <c:out value="${analytics.sessionsByStatus['CANCELLED'] + analytics.sessionsByStatus['EXPIRED']}" />
                </div>
            </div>
        </div>
    </div>

    <!-- Charts Row: Demand vs Supply (Pure CSS Horizontal Bar Charts) -->
    <div class="grid-2 mb-4">
        <!-- Top 10 Demanded Skills -->
        <div class="card" id="section-top-wanted">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <h2 style="font-size: 1.15rem; margin: 0;">🔥 Top 10 Wanted Skills (Demand)</h2>
                <span class="badge badge-accent" style="font-size: 0.75rem;">Wanted listings</span>
            </div>
            <p style="font-size: 0.82rem; color: var(--color-text-muted); margin-bottom: 1rem;">
                Skills students are most actively seeking to learn on campus.
            </p>

            <c:choose>
                <c:when test="${not empty analytics.topWanted}">
                    <div class="chart-container">
                        <c:forEach var="item" items="${analytics.topWanted}">
                            <div class="chart-bar-row">
                                <div class="chart-bar-label" title="${item.skillName}">
                                    <c:out value="${item.skillName}" />
                                </div>
                                <div class="chart-bar-track">
                                    <div class="chart-bar-fill chart-bar-fill-warm" style="width: ${item.percentWidth}%;"></div>
                                </div>
                                <div class="chart-bar-val">
                                    <c:out value="${item.count}" />
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="empty-state" style="padding: 2rem 1rem;">
                        <div class="empty-state-icon">📉</div>
                        <p class="empty-state-desc">No skill demands posted yet.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>

        <!-- Top 10 Offered Skills -->
        <div class="card" id="section-top-offered">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
                <h2 style="font-size: 1.15rem; margin: 0;">💡 Top 10 Offered Skills (Supply)</h2>
                <span class="badge badge-secondary" style="font-size: 0.75rem;">Offered listings</span>
            </div>
            <p style="font-size: 0.82rem; color: var(--color-text-muted); margin-bottom: 1rem;">
                Skills most frequently taught by registered peer mentors.
            </p>

            <c:choose>
                <c:when test="${not empty analytics.topOffered}">
                    <div class="chart-container">
                        <c:forEach var="item" items="${analytics.topOffered}">
                            <div class="chart-bar-row">
                                <div class="chart-bar-label" title="${item.skillName}">
                                    <c:out value="${item.skillName}" />
                                </div>
                                <div class="chart-bar-track">
                                    <div class="chart-bar-fill chart-bar-fill-cool" style="width: ${item.percentWidth}%;"></div>
                                </div>
                                <div class="chart-bar-val">
                                    <c:out value="${item.count}" />
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="empty-state" style="padding: 2rem 1rem;">
                        <div class="empty-state-icon">📉</div>
                        <p class="empty-state-desc">No skill offers posted yet.</p>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- Weekly Registration Growth (Pure CSS Column Bar Chart) -->
    <div class="card" id="section-weekly-growth">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.5rem;">
            <h2 style="font-size: 1.15rem; margin: 0;">📅 New Student Registrations (Last 8 Weeks)</h2>
            <span class="badge badge-info" style="font-size: 0.75rem;">Cohort Growth</span>
        </div>
        <p style="font-size: 0.82rem; color: var(--color-text-muted); margin-bottom: 0.5rem;">
            Weekly registration volume rendered via server-side calculated CSS percentage heights.
        </p>

        <c:choose>
            <c:when test="${not empty analytics.weeklyRegistrations}">
                <div class="col-chart-container">
                    <c:forEach var="week" items="${analytics.weeklyRegistrations}">
                        <div class="col-chart-bar-wrapper">
                            <span class="col-chart-val"><c:out value="${week.count}" /></span>
                            <div class="col-chart-bar" style="height: ${week.percent}%;"></div>
                            <span class="col-chart-label"><c:out value="${week.weekLabel}" /></span>
                        </div>
                    </c:forEach>
                </div>
            </c:when>
            <c:otherwise>
                <div class="empty-state" style="padding: 2rem 1rem;">
                    <div class="empty-state-icon">📊</div>
                    <p class="empty-state-desc">No user registration trends recorded yet.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="../layout/footer.jspf" %>
