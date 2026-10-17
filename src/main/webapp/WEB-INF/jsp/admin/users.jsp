<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Manage Users — SkillSwap Admin" />
<%@ include file="../layout/header.jspf" %>
<%@ include file="../layout/nav.jspf" %>

<main class="container py-4">
    <!-- Header Row -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
        <div>
            <span class="badge badge-primary" style="margin-bottom: 0.35rem;">User Management</span>
            <h1 style="margin: 0; font-size: 1.85rem; color: var(--color-primary-dark);">Registered Users Roster</h1>
            <p style="color: var(--color-text-muted); font-size: 0.9rem; margin: 0.25rem 0 0 0;">
                Total users found: <strong><c:out value="${totalCount}" /></strong>
            </p>
        </div>
        <div style="display: flex; gap: 0.75rem; flex-wrap: wrap;">
            <a href="${pageContext.request.contextPath}/admin" class="btn btn-outline btn-sm">← Back to Analytics</a>
            <a href="${pageContext.request.contextPath}/admin/users/export" class="btn btn-primary btn-sm" id="btn-export-users-csv">Export Users CSV ⬇</a>
        </div>
    </div>

    <!-- Search Toolbar -->
    <div class="card mb-4">
        <form method="get" action="${pageContext.request.contextPath}/admin/users" style="display: flex; gap: 0.75rem; align-items: center; flex-wrap: wrap;">
            <div style="flex: 1; min-width: 250px;">
                <input type="text" name="q" value="${query}" placeholder="Search by name, email, or department..." class="form-control" id="input-admin-user-search">
            </div>
            <button type="submit" class="btn btn-primary" id="btn-admin-search-submit">Search</button>
            <c:if test="${not empty query}">
                <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline">Clear</a>
            </c:if>
        </form>
    </div>

    <!-- Users Table -->
    <div class="card">
        <c:choose>
            <c:when test="${not empty users}">
                <div class="table-responsive">
                    <table class="table" id="table-admin-users">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>User</th>
                                <th>Role</th>
                                <th>Department / Year</th>
                                <th>Credits</th>
                                <th>Rating</th>
                                <th>Sessions</th>
                                <th>Strikes</th>
                                <th>Status</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="u" items="${users}">
                                <tr>
                                    <td><strong>#<c:out value="${u.id}" /></strong></td>
                                    <td>
                                        <div style="font-weight: 600;">
                                            <a href="${pageContext.request.contextPath}/users/view?id=${u.id}">
                                                <c:out value="${u.fullName}" />
                                            </a>
                                        </div>
                                        <div style="font-size: 0.8rem; color: var(--color-text-muted);">
                                            <c:out value="${u.email}" />
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge ${u.role == 'ADMIN' ? 'badge-primary' : 'badge-info'}" style="font-size: 0.72rem;">
                                            <c:out value="${u.role}" />
                                        </span>
                                    </td>
                                    <td>
                                        <div style="font-size: 0.85rem;"><c:out value="${u.department}" /></div>
                                        <c:if test="${not empty u.studyYear}">
                                            <div style="font-size: 0.78rem; color: var(--color-text-muted);">Year <c:out value="${u.studyYear}" /></div>
                                        </c:if>
                                    </td>
                                    <td>
                                        <strong style="color: var(--color-primary-dark); font-size: 0.95rem;">
                                            <c:out value="${u.credits}" />
                                        </strong>
                                    </td>
                                    <td>
                                        <span style="color: #EAB308; font-weight: 600;">★ <c:out value="${u.avgRating}" /></span>
                                        <span style="font-size: 0.78rem; color: var(--color-text-muted);">(<c:out value="${u.ratingCount}" />)</span>
                                    </td>
                                    <td style="font-size: 0.85rem;">
                                        T: <c:out value="${u.sessionsTaught}" /> | L: <c:out value="${u.sessionsLearned}" />
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${u.reliabilityStrikes > 0}">
                                                <span class="badge badge-danger"><c:out value="${u.reliabilityStrikes}" /></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color: var(--color-text-muted);">0</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${u.active}">
                                                <span class="badge badge-success" style="font-size: 0.72rem;">ACTIVE</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-danger" style="font-size: 0.72rem;">INACTIVE</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td>
                                        <div style="display: flex; flex-direction: column; gap: 0.35rem;">
                                            <!-- Toggle Active Form -->
                                            <form action="${pageContext.request.contextPath}/admin/users/toggle" method="post" style="margin: 0;">
                                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                                <input type="hidden" name="userId" value="${u.id}">
                                                <input type="hidden" name="active" value="${!u.active}">
                                                <button type="submit" class="btn btn-sm ${u.active ? 'btn-outline' : 'btn-accent'}" style="padding: 0.2rem 0.5rem; font-size: 0.75rem; width: 100%;">
                                                    <c:out value="${u.active ? 'Deactivate' : 'Activate'}" />
                                                </button>
                                            </form>

                                            <!-- Adjust Credits Form Inline Toggle -->
                                            <details style="font-size: 0.8rem;">
                                                <summary style="cursor: pointer; color: var(--color-primary); font-weight: 600; padding: 0.15rem 0;">
                                                    Adjust Credits
                                                </summary>
                                                <div style="background-color: #FAFAFA; border: 1px solid var(--color-border); padding: 0.65rem; border-radius: var(--radius-sm); margin-top: 0.35rem; width: 220px;">
                                                    <form action="${pageContext.request.contextPath}/admin/users/credits" method="post">
                                                        <input type="hidden" name="_csrf" value="${csrfToken}">
                                                        <input type="hidden" name="userId" value="${u.id}">
                                                        
                                                        <label style="font-size: 0.75rem; font-weight: 600; display: block; margin-bottom: 0.2rem;">Amount (+ or -):</label>
                                                        <input type="number" name="amount" placeholder="+5 or -5" required class="form-control" style="font-size: 0.8rem; padding: 0.25rem 0.5rem; margin-bottom: 0.4rem;">

                                                        <label style="font-size: 0.75rem; font-weight: 600; display: block; margin-bottom: 0.2rem;">Required Reason:</label>
                                                        <input type="text" name="reason" placeholder="e.g. Compensation for issue" required class="form-control" style="font-size: 0.8rem; padding: 0.25rem 0.5rem; margin-bottom: 0.4rem;">

                                                        <button type="submit" class="btn btn-primary btn-sm" style="width: 100%; font-size: 0.75rem; padding: 0.25rem;">
                                                            Apply Adjustment
                                                        </button>
                                                    </form>
                                                </div>
                                            </details>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <!-- Pagination -->
                <c:if test="${totalPages > 1}">
                    <ul class="pagination">
                        <c:forEach var="p" begin="1" end="${totalPages}">
                            <li class="page-item ${p == currentPage ? 'active' : ''}">
                                <a href="${pageContext.request.contextPath}/admin/users?q=${query}&page=${p}">
                                    <c:out value="${p}" />
                                </a>
                            </li>
                        </c:forEach>
                    </ul>
                </c:if>
            </c:when>
            <c:otherwise>
                <div class="empty-state">
                    <div class="empty-state-icon">🔍</div>
                    <h3 class="empty-state-title">No Users Found</h3>
                    <p class="empty-state-desc">No accounts matched your search criteria.</p>
                    <a href="${pageContext.request.contextPath}/admin/users" class="btn btn-outline btn-sm">Clear Search</a>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="../layout/footer.jspf" %>
