<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="My Dashboard — SkillSwap" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="container py-4">
    <!-- Header Hero Banner -->
    <div class="card mb-4" style="background: linear-gradient(135deg, var(--color-primary-dark) 0%, var(--color-primary) 100%); color: #FFFFFF; border: none; padding: 2rem;">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
            <div>
                <span class="badge badge-accent" style="margin-bottom: 0.5rem;">Student Dashboard</span>
                <h1 style="color: #FFFFFF; margin: 0.25rem 0 0.5rem 0; font-size: 1.85rem;">
                    Welcome back, <c:out value="${dashboard.user.fullName}" />!
                </h1>
                <p style="color: rgba(255, 255, 255, 0.85); margin: 0; font-size: 0.95rem;">
                    <c:out value="${dashboard.user.department}" /> • Year <c:out value="${dashboard.user.studyYear}" />
                </p>
            </div>
            <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
                <a href="${pageContext.request.contextPath}/browse" class="btn btn-accent btn-sm" id="btn-dashboard-browse">Find Mentors</a>
                <a href="${pageContext.request.contextPath}/skills" class="btn btn-outline btn-sm" id="btn-dashboard-skills" style="color:#FFF; border-color: rgba(255,255,255,0.4);">Manage Skills</a>
                <a href="${pageContext.request.contextPath}/history/export" class="btn btn-outline btn-sm" id="btn-dashboard-export" style="color:#FFF; border-color: rgba(255,255,255,0.4);" title="Export Session History CSV">Export History CSV ⬇</a>
            </div>
        </div>
    </div>

    <!-- Incoming Requests Alert -->
    <c:if test="${dashboard.pendingIncomingCount > 0}">
        <div class="alert alert-info mb-4" style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.75rem;">
            <div>
                <strong>Action Needed:</strong> You have <strong><c:out value="${dashboard.pendingIncomingCount}" /></strong> incoming session request(s) awaiting your response.
            </div>
            <a href="${pageContext.request.contextPath}/requests?tab=incoming" class="btn btn-primary btn-sm" id="btn-view-pending-requests">Review Requests</a>
        </div>
    </c:if>

    <!-- Top Metric Cards -->
    <div class="grid-4 mb-4">
        <!-- Credit Balance -->
        <div class="stat-card" id="card-credit-balance">
            <div class="stat-header">
                <span class="stat-label">Available Balance</span>
                <span class="stat-icon">🪙</span>
            </div>
            <div class="stat-value">
                <c:out value="${dashboard.availableCredits}" /> <span style="font-size: 1rem; font-weight: 500; color: var(--color-text-muted);">credits</span>
            </div>
            <div class="stat-subtext">
                <c:choose>
                    <c:when test="${dashboard.heldCredits > 0}">
                        <span class="badge badge-warning" style="font-size: 0.75rem;"><c:out value="${dashboard.heldCredits}" /> held in escrow</span>
                    </c:when>
                    <c:otherwise>
                        No credits currently in escrow
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Earned vs Spent -->
        <div class="stat-card" id="card-credits-flow">
            <div class="stat-header">
                <span class="stat-label">Credit Activity</span>
                <span class="stat-icon">📈</span>
            </div>
            <div class="stat-value" style="font-size: 1.45rem;">
                +<c:out value="${dashboard.totalEarned}" /> / -<c:out value="${dashboard.totalSpent}" />
            </div>
            <div class="stat-subtext">
                Total Earned vs Spent on campus
            </div>
        </div>

        <!-- Sessions Record -->
        <div class="stat-card" id="card-sessions-record">
            <div class="stat-header">
                <span class="stat-label">Sessions Completed</span>
                <span class="stat-icon">🤝</span>
            </div>
            <div class="stat-value" style="font-size: 1.45rem;">
                <c:out value="${dashboard.sessionsTaught}" /> taught • <c:out value="${dashboard.sessionsLearned}" /> learned
            </div>
            <div class="stat-subtext">
                <c:out value="${dashboard.sessionsTaught + dashboard.sessionsLearned}" /> total peer swaps
            </div>
        </div>

        <!-- Reputation & Rating -->
        <div class="stat-card" id="card-reputation">
            <div class="stat-header">
                <span class="stat-label">Mentor Reputation</span>
                <span class="stat-icon">⭐</span>
            </div>
            <div class="stat-value">
                <c:out value="${dashboard.avgRating}" /> <span style="font-size: 1.1rem; color: #EAB308;">★</span>
            </div>
            <div class="stat-subtext">
                Based on <c:out value="${dashboard.ratingCount}" /> review(s)
                <c:if test="${dashboard.reliabilityStrikes > 0}">
                    • <span style="color: var(--color-danger);"><c:out value="${dashboard.reliabilityStrikes}" /> strike(s)</span>
                </c:if>
            </div>
        </div>
    </div>

    <!-- Two Column Dashboard Layout -->
    <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 1.5rem; align-items: start;">
        <!-- Left / Main Column -->
        <div>
            <!-- Upcoming Confirmed Sessions -->
            <div class="card mb-4" id="section-upcoming-sessions">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                    <h2 style="font-size: 1.25rem; margin: 0;">📅 Upcoming Sessions</h2>
                    <a href="${pageContext.request.contextPath}/requests" class="btn btn-outline btn-sm">View All</a>
                </div>

                <c:choose>
                    <c:when test="${not empty dashboard.upcomingSessions}">
                        <div class="table-responsive">
                            <table class="table" style="margin-bottom: 0;">
                                <thead>
                                    <tr>
                                        <th>Role</th>
                                        <th>Partner</th>
                                        <th>Skill</th>
                                        <th>Date & Time</th>
                                        <th>Mode</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="req" items="${dashboard.upcomingSessions}">
                                        <c:set var="isLearner" value="${req.learnerId == sessionScope.currentUser.id}" />
                                        <tr>
                                            <td>
                                                <c:choose>
                                                    <c:when test="${isLearner}">
                                                        <span class="badge badge-info" style="font-size: 0.72rem;">LEARNER</span>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <span class="badge badge-secondary" style="font-size: 0.72rem;">MENTOR</span>
                                                    </c:otherwise>
                                                </c:choose>
                                            </td>
                                            <td>
                                                <strong>
                                                    <c:choose>
                                                        <c:when test="${isLearner}">
                                                            <a href="${pageContext.request.contextPath}/users/view?id=${req.providerId}">
                                                                <c:out value="${req.providerName}" />
                                                            </a>
                                                        </c:when>
                                                        <c:otherwise>
                                                            <a href="${pageContext.request.contextPath}/users/view?id=${req.learnerId}">
                                                                <c:out value="${req.learnerName}" />
                                                            </a>
                                                        </c:otherwise>
                                                    </c:choose>
                                                </strong>
                                            </td>
                                            <td><c:out value="${req.skillName}" /></td>
                                            <td>
                                                <span style="font-size: 0.88rem; font-weight: 500;">
                                                    <c:out value="${req.proposedStart}" />
                                                </span>
                                            </td>
                                            <td>
                                                <span class="badge badge-outline" style="font-size: 0.75rem;">
                                                    <c:out value="${req.mode}" />
                                                </span>
                                            </td>
                                            <td>
                                                <a href="${pageContext.request.contextPath}/requests/view?id=${req.id}" class="btn btn-primary btn-sm" style="padding: 0.2rem 0.5rem; font-size: 0.8rem;">
                                                    Details
                                                </a>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state" style="padding: 2rem 1rem;">
                            <div class="empty-state-icon">🗓️</div>
                            <h3 class="empty-state-title">No Upcoming Sessions</h3>
                            <p class="empty-state-desc">You don't have any confirmed sessions scheduled at the moment.</p>
                            <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary btn-sm">Find a Mentor</a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Recommended Mentors -->
            <div class="card" id="section-recommendations">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                    <h2 style="font-size: 1.25rem; margin: 0;">✨ Top Recommended Mentors</h2>
                    <a href="${pageContext.request.contextPath}/matches" class="btn btn-outline btn-sm">View All Matches</a>
                </div>

                <c:choose>
                    <c:when test="${not empty dashboard.recommendations}">
                        <div style="display: flex; flex-direction: column; gap: 0.85rem;">
                            <c:forEach var="match" items="${dashboard.recommendations}">
                                <div style="display: flex; justify-content: space-between; align-items: center; padding: 0.85rem 1rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background-color: var(--color-surface); flex-wrap: wrap; gap: 0.75rem;">
                                    <div style="display: flex; align-items: center; gap: 0.75rem;">
                                        <div class="nav-avatar" style="width: 40px; height: 40px; font-size: 1rem;">
                                            <c:choose>
                                                <c:when test="${not empty match.provider.avatarPath}">
                                                    <img src="${pageContext.request.contextPath}/avatar?id=${match.provider.id}" alt="Avatar" style="width:100%;height:100%;border-radius:50%;object-fit:cover;">
                                                </c:when>
                                                <c:otherwise>
                                                    <c:out value="${fn:substring(match.provider.fullName, 0, 1)}" />
                                                </c:otherwise>
                                            </c:choose>
                                        </div>
                                        <div>
                                            <div style="font-weight: 600; font-size: 0.95rem;">
                                                <a href="${pageContext.request.contextPath}/users/view?id=${match.provider.id}">
                                                    <c:out value="${match.provider.fullName}" />
                                                </a>
                                                <span class="badge badge-accent" style="font-size: 0.72rem; margin-left: 0.35rem;">
                                                    <fmt:formatNumber value="${match.score * 100}" maxFractionDigits="0"/>% Match
                                                </span>
                                            </div>
                                            <div style="font-size: 0.83rem; color: var(--color-text-muted);">
                                                Teaches <strong><c:out value="${match.listing.skillName}" /></strong> • <c:out value="${match.listing.creditsPerHour}" /> credits/hr
                                            </div>
                                        </div>
                                    </div>
                                    <div style="display: flex; align-items: center; gap: 0.5rem;">
                                        <span style="font-size: 0.88rem; font-weight: 600; color: #EAB308;">★ <c:out value="${match.provider.avgRating}" /></span>
                                        <a href="${pageContext.request.contextPath}/requests/new?listingId=${match.listing.id}" class="btn btn-primary btn-sm">Request</a>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state" style="padding: 1.5rem 1rem;">
                            <div class="empty-state-icon">🎯</div>
                            <h3 class="empty-state-title" style="font-size: 1.05rem;">No Recommendations Yet</h3>
                            <p class="empty-state-desc" style="font-size: 0.85rem;">Add skills you want to learn to your profile so our matching engine can pair you with peer mentors!</p>
                            <a href="${pageContext.request.contextPath}/skills" class="btn btn-outline btn-sm">Add Wanted Skills</a>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Right / Sidebar Column -->
        <div>
            <!-- Earned Badges Showcase -->
            <div class="card mb-4" id="section-badges">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
                    <h2 style="font-size: 1.15rem; margin: 0;">🏆 Earned Badges</h2>
                    <span class="badge badge-primary"><c:out value="${fn:length(dashboard.badges)}" />/6</span>
                </div>

                <c:choose>
                    <c:when test="${not empty dashboard.badges}">
                        <div style="display: flex; flex-direction: column; gap: 0.65rem;">
                            <c:forEach var="badge" items="${dashboard.badges}">
                                <div style="display: flex; align-items: center; gap: 0.65rem; padding: 0.65rem 0.75rem; background-color: rgba(45, 42, 91, 0.04); border-radius: var(--radius-sm); border-left: 3px solid var(--color-accent);">
                                    <span style="font-size: 1.4rem;">
                                        <c:choose>
                                            <c:when test="${badge.code == 'FIRST_SWAP'}">🌱</c:when>
                                            <c:when test="${badge.code == 'MENTOR_10'}">🎓</c:when>
                                            <c:when test="${badge.code == 'ETERNAL_LEARNER'}">📚</c:when>
                                            <c:when test="${badge.code == 'PERFECT_FIVE'}">🌟</c:when>
                                            <c:when test="${badge.code == 'CONNECTOR'}">🌐</c:when>
                                            <c:when test="${badge.code == 'RELIABLE'}">🛡️</c:when>
                                            <c:otherwise>🎖️</c:otherwise>
                                        </c:choose>
                                    </span>
                                    <div>
                                        <div style="font-weight: 700; font-size: 0.88rem; color: var(--color-primary-dark);"><c:out value="${badge.name}" /></div>
                                        <div style="font-size: 0.75rem; color: var(--color-text-muted);"><c:out value="${badge.description}" /></div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div style="text-align: center; padding: 1.5rem 0.5rem; color: var(--color-text-muted); font-size: 0.85rem;">
                            <div style="font-size: 1.8rem; margin-bottom: 0.35rem;">🌱</div>
                            <strong>No badges earned yet.</strong><br/>
                            Complete your first session to unlock the <em>First Swap</em> badge!
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Quick Navigation & Wallet Shortcuts -->
            <div class="card" id="section-shortcuts">
                <h2 style="font-size: 1.15rem; margin-top: 0; margin-bottom: 0.85rem;">⚡ Quick Actions</h2>
                <ul style="list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 0.5rem;">
                    <li>
                        <a href="${pageContext.request.contextPath}/credits" style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-decoration: none; color: var(--color-text); font-size: 0.88rem; font-weight: 500;">
                            <span>🪙 Credit Wallet & Ledger</span>
                            <span>→</span>
                        </a>
                    </li>
                    <li>
                        <a href="${pageContext.request.contextPath}/availability" style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-decoration: none; color: var(--color-text); font-size: 0.88rem; font-weight: 500;">
                            <span>🗓️ Manage Weekly Schedule</span>
                            <span>→</span>
                        </a>
                    </li>
                    <li>
                        <a href="${pageContext.request.contextPath}/messages" style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-decoration: none; color: var(--color-text); font-size: 0.88rem; font-weight: 500;">
                            <span>💬 In-App Messages</span>
                            <span>→</span>
                        </a>
                    </li>
                    <li>
                        <a href="${pageContext.request.contextPath}/history/export" style="display: flex; justify-content: space-between; align-items: center; padding: 0.6rem 0.75rem; border: 1px solid var(--color-border); border-radius: var(--radius-sm); text-decoration: none; color: var(--color-text); font-size: 0.88rem; font-weight: 500;">
                            <span>📥 Download CSV History</span>
                            <span>⬇</span>
                        </a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
