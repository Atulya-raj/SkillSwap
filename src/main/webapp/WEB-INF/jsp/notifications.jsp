<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Notifications - SkillSwap" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>
<%@ include file="/WEB-INF/jsp/layout/nav.jspf" %>

<main class="container">
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
        <div>
            <h2>Notifications & Alerts</h2>
            <p class="text-muted">Stay informed on session milestones, smart matches, credit transfers, and badges.</p>
        </div>
        <c:if test="${unreadCount > 0}">
            <div>
                <form method="POST" action="${pageContext.request.contextPath}/notifications/read-all" style="display: inline;">
                    <input type="hidden" name="_csrf" value="${csrfToken}"/>
                    <button type="submit" class="btn btn-secondary" id="btn-mark-all-read">✓ Mark All as Read</button>
                </form>
            </div>
        </c:if>
    </div>

    <c:choose>
        <c:when test="${empty notifications}">
            <div class="card" style="text-align: center; padding: 3rem 1.5rem;">
                <div style="font-size: 3rem; margin-bottom: 1rem;">🔔</div>
                <h3>All caught up!</h3>
                <p class="text-muted" style="max-width: 480px; margin: 0 auto 1.5rem;">
                    You have no notifications right now. When students request sessions, confirm appointments, or when new skill matches appear, you'll see them here.
                </p>
                <div>
                    <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary">Browse Skills</a>
                    <a href="${pageContext.request.contextPath}/matches" class="btn btn-outline" style="margin-left: 0.5rem;">View Matches</a>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div style="display: flex; flex-direction: column; gap: 0.75rem;">
                <c:forEach var="notif" items="${notifications}">
                    <div class="card" style="padding: 1.25rem; border-left: 4px solid ${notif.read ? '#cbd5e1' : 'var(--color-primary)'}; background: ${notif.read ? '#ffffff' : '#f8fafc'}; transition: background 0.2s;">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 1rem; flex-wrap: wrap;">
                            <div style="flex: 1; min-width: 250px;">
                                <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.35rem;">
                                    <c:choose>
                                        <c:when test="${notif.type == 'REQUEST_RECEIVED'}">
                                            <span class="badge badge-primary">📩 Request</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'REQUEST_ACCEPTED'}">
                                            <span class="badge badge-success">✅ Accepted</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'REQUEST_REJECTED'}">
                                            <span class="badge badge-danger">❌ Rejected</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'REQUEST_CANCELLED' || notif.type == 'REQUEST_EXPIRED'}">
                                            <span class="badge badge-warning">🚫 Cancelled</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'SESSION_COMPLETED'}">
                                            <span class="badge badge-success">🎉 Completed</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'NEW_MATCH'}">
                                            <span class="badge badge-accent">💡 New Match</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'SESSION_REMINDER'}">
                                            <span class="badge badge-warning">⏰ Reminder</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'BADGE_AWARDED'}">
                                            <span class="badge badge-accent">🏆 Badge</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'NEW_MESSAGE'}">
                                            <span class="badge badge-info">💬 Message</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'NEW_REVIEW'}">
                                            <span class="badge badge-accent">⭐ Review</span>
                                        </c:when>
                                        <c:when test="${notif.type == 'NO_SHOW'}">
                                            <span class="badge badge-danger">⚠️ No-Show</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge badge-secondary">🔔 Notice</span>
                                        </c:otherwise>
                                    </c:choose>

                                    <h4 style="margin: 0; font-size: 1.05rem; color: #1e293b;">
                                        <c:out value="${notif.title}"/>
                                    </h4>

                                    <c:if test="${!notif.read}">
                                        <span class="pill" style="background: #e0e7ff; color: #3730a3; font-size: 0.72rem; font-weight: 700; padding: 0.15rem 0.5rem; border-radius: 999px;">NEW</span>
                                    </c:if>
                                </div>

                                <p style="margin: 0.35rem 0 0.5rem 0; color: #475569; font-size: 0.95rem; line-height: 1.45;">
                                    <c:out value="${notif.body}"/>
                                </p>

                                <div style="font-size: 0.8rem; color: #94a3b8;">
                                    <span>${notif.createdAt}</span>
                                </div>
                            </div>

                            <div style="display: flex; gap: 0.5rem; align-items: center;">
                                <c:if test="${not empty notif.linkUrl}">
                                    <form method="POST" action="${pageContext.request.contextPath}/notifications/read" style="display: inline;">
                                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                                        <input type="hidden" name="id" value="${notif.id}"/>
                                        <input type="hidden" name="target" value="<c:out value='${notif.linkUrl}'/>"/>
                                        <button type="submit" class="btn btn-sm btn-primary">Open →</button>
                                    </form>
                                </c:if>

                                <c:if test="${!notif.read}">
                                    <form method="POST" action="${pageContext.request.contextPath}/notifications/read" style="display: inline;">
                                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                                        <input type="hidden" name="id" value="${notif.id}"/>
                                        <button type="submit" class="btn btn-sm btn-outline" title="Mark as read">✓</button>
                                    </form>
                                </c:if>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <!-- Pagination -->
            <c:if test="${paginator.totalPages > 1}">
                <div class="pagination" style="display: flex; justify-content: center; gap: 0.5rem; margin-top: 2rem;">
                    <c:if test="${paginator.currentPage > 1}">
                        <a href="${pageContext.request.contextPath}/notifications?page=${paginator.currentPage - 1}" class="btn btn-sm btn-outline">« Previous</a>
                    </c:if>
                    <span style="display: flex; align-items: center; padding: 0 0.75rem; font-weight: 600; color: #64748b;">
                        Page ${paginator.currentPage} of ${paginator.totalPages}
                    </span>
                    <c:if test="${paginator.currentPage < paginator.totalPages}">
                        <a href="${pageContext.request.contextPath}/notifications?page=${paginator.currentPage + 1}" class="btn btn-sm btn-outline">Next »</a>
                    </c:if>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</main>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
