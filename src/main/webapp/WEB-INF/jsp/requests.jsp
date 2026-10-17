<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Session Requests - SkillSwap" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>
<%@ include file="/WEB-INF/jsp/layout/nav.jspf" %>

<main class="container">
    <div class="page-header">
        <div>
            <h2>Session Requests & Swaps</h2>
            <p class="text-muted">Manage your teaching and learning appointments and track status changes.</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary">+ Request New Session</a>
        </div>
    </div>

    <!-- Tab navigation -->
    <div class="tab-nav" style="display: flex; gap: 1rem; border-bottom: 2px solid #e2e8f0; margin-bottom: 1.5rem;">
        <a href="${pageContext.request.contextPath}/requests?tab=incoming&status=${statusFilter}"
           class="tab-link ${tab == 'incoming' ? 'active' : ''}"
           style="padding: 0.75rem 1.25rem; font-weight: 600; text-decoration: none; border-bottom: 3px solid ${tab == 'incoming' ? 'var(--color-primary)' : 'transparent'}; color: ${tab == 'incoming' ? 'var(--color-primary)' : '#64748b'};">
            📥 Incoming (Teaching)
            <c:if test="${pendingIncomingCount > 0}">
                <span class="badge badge-warning" style="margin-left: 0.4rem;">${pendingIncomingCount} new</span>
            </c:if>
        </a>
        <a href="${pageContext.request.contextPath}/requests?tab=outgoing&status=${statusFilter}"
           class="tab-link ${tab == 'outgoing' ? 'active' : ''}"
           style="padding: 0.75rem 1.25rem; font-weight: 600; text-decoration: none; border-bottom: 3px solid ${tab == 'outgoing' ? 'var(--color-primary)' : 'transparent'}; color: ${tab == 'outgoing' ? 'var(--color-primary)' : '#64748b'};">
            📤 Outgoing (Learning)
        </a>
    </div>

    <!-- Filter Bar -->
    <div class="card" style="margin-bottom: 1.5rem; padding: 1rem;">
        <form method="GET" action="${pageContext.request.contextPath}/requests" style="display: flex; gap: 1rem; align-items: center; flex-wrap: wrap;">
            <input type="hidden" name="tab" value="<c:out value='${tab}'/>"/>
            <label for="statusSelect" style="font-weight: 600; color: #334155;">Filter by Status:</label>
            <select id="statusSelect" name="status" class="form-control" style="width: auto; min-width: 180px;">
                <option value="ALL" ${statusFilter == 'ALL' ? 'selected' : ''}>All Statuses</option>
                <option value="PENDING" ${statusFilter == 'PENDING' ? 'selected' : ''}>Pending</option>
                <option value="ACCEPTED" ${statusFilter == 'ACCEPTED' ? 'selected' : ''}>Accepted</option>
                <option value="COMPLETED" ${statusFilter == 'COMPLETED' ? 'selected' : ''}>Completed</option>
                <option value="CANCELLED" ${statusFilter == 'CANCELLED' ? 'selected' : ''}>Cancelled</option>
                <option value="REJECTED" ${statusFilter == 'REJECTED' ? 'selected' : ''}>Rejected</option>
                <option value="EXPIRED" ${statusFilter == 'EXPIRED' ? 'selected' : ''}>Expired</option>
                <option value="NO_SHOW_LEARNER" ${statusFilter == 'NO_SHOW_LEARNER' ? 'selected' : ''}>Learner No-Show</option>
                <option value="NO_SHOW_PROVIDER" ${statusFilter == 'NO_SHOW_PROVIDER' ? 'selected' : ''}>Provider No-Show</option>
            </select>
            <button type="submit" class="btn btn-secondary">Apply Filter</button>
            <c:if test="${statusFilter != 'ALL'}">
                <a href="${pageContext.request.contextPath}/requests?tab=<c:out value='${tab}'/>" class="btn btn-outline" style="text-decoration: none;">Clear Filter</a>
            </c:if>
        </form>
    </div>

    <!-- Request items -->
    <c:choose>
        <c:when test="${empty requests}">
            <div class="card empty-state" style="text-align: center; padding: 3rem 1.5rem;">
                <div style="font-size: 3rem; margin-bottom: 1rem;">📭</div>
                <h3>No requests found</h3>
                <p class="text-muted">
                    <c:choose>
                        <c:when test="${tab == 'incoming'}">
                            You don't have any incoming teaching requests in this view. Make sure your offered skills and weekly availability are up to date!
                        </c:when>
                        <c:otherwise>
                            You have not submitted any session requests yet. Discover peer tutors in the campus catalog to get started.
                        </c:otherwise>
                    </c:choose>
                </p>
                <div style="margin-top: 1.25rem;">
                    <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary">Browse Skills</a>
                    <a href="${pageContext.request.contextPath}/skills" class="btn btn-outline" style="margin-left: 0.5rem;">Manage Skills</a>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div style="display: flex; flex-direction: column; gap: 1rem;">
                <c:forEach var="r" items="${requests}">
                    <div class="card" style="padding: 1.25rem; display: flex; flex-direction: column; gap: 0.75rem; border-left: 5px solid ${r.status == 'PENDING' ? '#F4A300' : (r.status == 'ACCEPTED' ? '#1C7293' : (r.status == 'COMPLETED' ? '#10B981' : '#94A3B8'))};">
                        <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap; gap: 0.5rem;">
                            <div>
                                <h3 style="margin: 0; font-size: 1.25rem;">
                                    <a href="${pageContext.request.contextPath}/requests/view?id=${r.id}" style="text-decoration: none; color: var(--color-primary);">
                                        <c:out value="${r.skillName}"/>
                                    </a>
                                </h3>
                                <div style="display: flex; align-items: center; gap: 0.75rem; margin-top: 0.4rem; font-size: 0.9rem; color: #475569;">
                                    <span>
                                        <strong>
                                            <c:choose>
                                                <c:when test="${tab == 'incoming'}">Learner:</c:when>
                                                <c:otherwise>Provider:</c:otherwise>
                                            </c:choose>
                                        </strong>
                                        <a href="${pageContext.request.contextPath}/users/view?id=${tab == 'incoming' ? r.learnerId : r.providerId}" style="color: var(--color-teal); font-weight: 600;">
                                            <c:out value="${tab == 'incoming' ? r.learnerName : r.providerName}"/>
                                        </a>
                                    </span>
                                    <span>•</span>
                                    <span>📅 <c:out value="${r.proposedStart}"/></span>
                                    <span>•</span>
                                    <span>⏱️ <c:out value="${r.durationMinutes}"/> mins</span>
                                    <span>•</span>
                                    <span>
                                        <c:choose>
                                            <c:when test="${r.mode == 'ONLINE'}">🌐 Online</c:when>
                                            <c:otherwise>📍 In-Person</c:otherwise>
                                        </c:choose>
                                    </span>
                                </div>
                            </div>
                            <div style="text-align: right;">
                                <span class="badge ${r.status == 'PENDING' ? 'badge-warning' : (r.status == 'ACCEPTED' ? 'badge-info' : (r.status == 'COMPLETED' ? 'badge-success' : 'badge-secondary'))}"
                                      style="font-size: 0.85rem; padding: 0.35rem 0.75rem;">
                                    <c:out value="${r.status}"/>
                                </span>
                                <div style="font-weight: 700; color: var(--color-primary); margin-top: 0.3rem;">
                                    <c:out value="${r.costCredits}"/> Credits
                                </div>
                            </div>
                        </div>

                        <c:if test="${not empty r.message}">
                            <div style="background: #f8fafc; border-radius: 6px; padding: 0.6rem 0.8rem; font-size: 0.88rem; color: #334155; font-style: italic;">
                                "<c:out value="${r.message}"/>"
                            </div>
                        </c:if>

                        <div style="display: flex; justify-content: space-between; align-items: center; margin-top: 0.5rem; padding-top: 0.75rem; border-top: 1px solid #f1f5f9; font-size: 0.85rem; color: #64748b;">
                            <div>
                                Request #<c:out value="${r.id}"/> • Submitted <c:out value="${r.createdAt}"/>
                            </div>
                            <div style="display: flex; gap: 0.5rem;">
                                <a href="${pageContext.request.contextPath}/requests/view?id=${r.id}" class="btn btn-outline" style="padding: 0.35rem 0.75rem; font-size: 0.85rem;">
                                    View Details & Actions →
                                </a>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>

            <!-- Pagination -->
            <c:if test="${paginator.totalPages > 1}">
                <div class="pagination" style="margin-top: 2rem; display: flex; justify-content: center; gap: 0.5rem;">
                    <c:if test="${paginator.hasPrev()}">
                        <a href="${pageContext.request.contextPath}/requests?tab=<c:out value='${tab}'/>&status=<c:out value='${statusFilter}'/>&page=${paginator.currentPage - 1}" class="btn btn-outline">← Prev</a>
                    </c:if>
                    <span style="display: inline-flex; align-items: center; padding: 0 1rem; font-weight: 600; color: #475569;">
                        Page ${paginator.currentPage} of ${paginator.totalPages}
                    </span>
                    <c:if test="${paginator.hasNext()}">
                        <a href="${pageContext.request.contextPath}/requests?tab=<c:out value='${tab}'/>&status=<c:out value='${statusFilter}'/>&page=${paginator.currentPage + 1}" class="btn btn-outline">Next →</a>
                    </c:if>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</main>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
