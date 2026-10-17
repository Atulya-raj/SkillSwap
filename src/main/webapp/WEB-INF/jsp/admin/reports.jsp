<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Moderation Reports — SkillSwap Admin" />
<%@ include file="../layout/header.jspf" %>
<%@ include file="../layout/nav.jspf" %>

<main class="container py-4">
    <!-- Header -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
        <div>
            <span class="badge badge-primary" style="margin-bottom: 0.35rem;">Trust & Moderation</span>
            <h1 style="margin: 0; font-size: 1.85rem; color: var(--color-primary-dark);">Community Moderation Queue</h1>
            <p style="color: var(--color-text-muted); font-size: 0.9rem; margin: 0.25rem 0 0 0;">
                Review student safety reports, handle reported accounts, and moderate inappropriate reviews.
            </p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin" class="btn btn-outline btn-sm">← Back to Analytics</a>
        </div>
    </div>

    <!-- Status Filter Tabs -->
    <div class="card mb-4" style="padding: 0.75rem 1rem;">
        <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
            <a href="${pageContext.request.contextPath}/admin/reports?status=OPEN" class="btn btn-sm ${currentStatus == 'OPEN' ? 'btn-primary' : 'btn-outline'}">
                Pending Reports (OPEN)
            </a>
            <a href="${pageContext.request.contextPath}/admin/reports?status=RESOLVED" class="btn btn-sm ${currentStatus == 'RESOLVED' ? 'btn-primary' : 'btn-outline'}">
                Resolved Reports
            </a>
            <a href="${pageContext.request.contextPath}/admin/reports?status=DISMISSED" class="btn btn-sm ${currentStatus == 'DISMISSED' ? 'btn-primary' : 'btn-outline'}">
                Dismissed Reports
            </a>
        </div>
    </div>

    <!-- Reports Table -->
    <div class="card">
        <c:choose>
            <c:when test="${not empty reports}">
                <div class="table-responsive">
                    <table class="table" id="table-admin-reports">
                        <thead>
                            <tr>
                                <th>Report ID</th>
                                <th>Reported On</th>
                                <th>Reporter</th>
                                <th>Target</th>
                                <th>Reported Reason</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="rp" items="${reports}">
                                <tr>
                                    <td><strong>#<c:out value="${rp.id}" /></strong></td>
                                    <td style="font-size: 0.85rem;">
                                        <c:out value="${rp.createdAt}" />
                                    </td>
                                    <td>
                                        <div style="font-weight: 600;">
                                            <a href="${pageContext.request.contextPath}/users/view?id=${rp.reporterId}">
                                                <c:out value="${rp.reporterName}" />
                                            </a>
                                        </div>
                                        <div style="font-size: 0.75rem; color: var(--color-text-muted);">
                                            User #<c:out value="${rp.reporterId}" />
                                        </div>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty rp.targetUserId}">
                                                <div>
                                                    <span class="badge badge-info" style="font-size: 0.72rem;">USER</span>
                                                    <a href="${pageContext.request.contextPath}/users/view?id=${rp.targetUserId}" style="font-weight: 600; margin-left: 0.25rem;">
                                                        <c:out value="${rp.targetName}" />
                                                    </a>
                                                </div>
                                            </c:when>
                                            <c:when test="${not empty rp.targetReviewId}">
                                                <div>
                                                    <span class="badge badge-warning" style="font-size: 0.72rem;">REVIEW</span>
                                                    <span style="font-size: 0.82rem; margin-left: 0.25rem;">Review #<c:out value="${rp.targetReviewId}" /></span>
                                                </div>
                                                <div style="margin-top: 0.25rem;">
                                                    <!-- Hide Review Action -->
                                                    <form action="${pageContext.request.contextPath}/admin/reports/hide-review" method="post" style="display: inline; margin: 0;">
                                                        <input type="hidden" name="_csrf" value="${csrfToken}">
                                                        <input type="hidden" name="reviewId" value="${rp.targetReviewId}">
                                                        <button type="submit" class="btn btn-danger btn-sm" style="font-size: 0.72rem; padding: 0.15rem 0.4rem;">
                                                            Hide Review
                                                        </button>
                                                    </form>
                                                </div>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color: var(--color-text-muted);">General</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="max-width: 320px;">
                                        <div style="font-size: 0.85rem; background-color: #FAFAFA; border: 1px solid var(--color-border); padding: 0.45rem 0.65rem; border-radius: var(--radius-sm);">
                                            "<c:out value="${rp.reason}" />"
                                        </div>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${rp.status == 'OPEN'}">
                                                <span class="badge badge-warning" style="font-size: 0.72rem;">OPEN</span>
                                            </c:when>
                                            <c:when test="${rp.status == 'RESOLVED'}">
                                                <span class="badge badge-success" style="font-size: 0.72rem;">RESOLVED</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-secondary" style="font-size: 0.72rem;">DISMISSED</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:if test="${rp.status == 'OPEN'}">
                                            <div style="display: flex; gap: 0.35rem;">
                                                <form action="${pageContext.request.contextPath}/admin/reports/resolve" method="post" style="margin: 0;">
                                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                                    <input type="hidden" name="reportId" value="${rp.id}">
                                                    <input type="hidden" name="action" value="RESOLVE">
                                                    <button type="submit" class="btn btn-success btn-sm" style="font-size: 0.75rem; padding: 0.2rem 0.5rem;">
                                                        Resolve
                                                    </button>
                                                </form>

                                                <form action="${pageContext.request.contextPath}/admin/reports/resolve" method="post" style="margin: 0;">
                                                    <input type="hidden" name="_csrf" value="${csrfToken}">
                                                    <input type="hidden" name="reportId" value="${rp.id}">
                                                    <input type="hidden" name="action" value="DISMISS">
                                                    <button type="submit" class="btn btn-outline btn-sm" style="font-size: 0.75rem; padding: 0.2rem 0.5rem;">
                                                        Dismiss
                                                    </button>
                                                </form>
                                            </div>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <div class="empty-state-icon">🛡️</div>
                    <h3 class="empty-state-title">No Reports In This Queue</h3>
                    <p class="empty-state-desc">There are no <c:out value="${currentStatus}" /> moderation reports at this time.</p>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="../layout/footer.jspf" %>
