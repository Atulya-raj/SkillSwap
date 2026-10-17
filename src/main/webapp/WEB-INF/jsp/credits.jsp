<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Credit Wallet & Ledger - SkillSwap" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>
<%@ include file="/WEB-INF/jsp/layout/nav.jspf" %>

<main class="container" style="max-width: 960px; margin: 2rem auto;">
    <div class="page-header" style="margin-bottom: 2rem;">
        <div>
            <h2>Credit Wallet & Ledger</h2>
            <p class="text-muted">Track your balance, funds in escrow, and complete audit trail of time-banking transactions.</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary">Find Skills to Learn</a>
        </div>
    </div>

    <!-- Balance Summary Metric Cards -->
    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 1.5rem; margin-bottom: 2rem;">
        <!-- Available Balance -->
        <div class="card" style="padding: 1.5rem; border-top: 4px solid var(--color-primary); background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);">
            <div style="font-size: 0.85rem; font-weight: 700; color: #64748b; text-transform: uppercase;">
                Available Balance
            </div>
            <div style="font-size: 2.4rem; font-weight: 800; color: var(--color-primary); margin: 0.5rem 0;">
                <c:out value="${balance}"/> <span style="font-size: 1.1rem; font-weight: 600; color: #64748b;">Credits</span>
            </div>
            <div style="font-size: 0.82rem; color: #475569;">
                Ready to spend on learning sessions
            </div>
        </div>

        <!-- Held in Escrow -->
        <div class="card" style="padding: 1.5rem; border-top: 4px solid var(--color-amber); background: linear-gradient(135deg, #ffffff 0%, #fffbeb 100%);">
            <div style="font-size: 0.85rem; font-weight: 700; color: #b45309; text-transform: uppercase;">
                Held in Escrow
            </div>
            <div style="font-size: 2.4rem; font-weight: 800; color: #d97706; margin: 0.5rem 0;">
                <c:out value="${heldCredits}"/> <span style="font-size: 1.1rem; font-weight: 600; color: #b45309;">Credits</span>
            </div>
            <div style="font-size: 0.82rem; color: #92400e;">
                Reserved for outgoing pending/accepted sessions
            </div>
        </div>

        <!-- Total Earned -->
        <div class="card" style="padding: 1.5rem; border-top: 4px solid var(--color-teal); background: linear-gradient(135deg, #ffffff 0%, #f0fdfa 100%);">
            <div style="font-size: 0.85rem; font-weight: 700; color: #0f766e; text-transform: uppercase;">
                Total Earned
            </div>
            <div style="font-size: 2.4rem; font-weight: 800; color: var(--color-teal); margin: 0.5rem 0;">
                <c:out value="${totalEarned}"/> <span style="font-size: 1.1rem; font-weight: 600; color: #0f766e;">Credits</span>
            </div>
            <div style="font-size: 0.82rem; color: #115e59;">
                Earned by teaching peer students
            </div>
        </div>
    </div>

    <!-- Ledger Table Card -->
    <div class="card" style="padding: 1.5rem;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.25rem;">
            <h3 style="margin: 0; color: var(--color-primary); font-size: 1.2rem;">Transaction History</h3>
            <span class="text-muted" style="font-size: 0.85rem;">Immutable audit trail</span>
        </div>

        <c:choose>
            <c:when test="${empty ledger}">
                <div class="empty-state" style="text-align: center; padding: 2.5rem 1rem;">
                    <div style="font-size: 2.5rem; margin-bottom: 0.75rem;">💳</div>
                    <h4>No transactions yet</h4>
                    <p class="text-muted">Your credit activity will be logged here as you teach and learn.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div style="overflow-x: auto;">
                    <table class="table" style="width: 100%; border-collapse: collapse; text-align: left;">
                        <thead>
                            <tr style="border-bottom: 2px solid #e2e8f0; color: #475569; font-size: 0.85rem; text-transform: uppercase;">
                                <th style="padding: 0.75rem 0.5rem;">Date & Time</th>
                                <th style="padding: 0.75rem 0.5rem;">Type</th>
                                <th style="padding: 0.75rem 0.5rem; text-align: right;">Amount</th>
                                <th style="padding: 0.75rem 0.5rem; text-align: right;">Balance After</th>
                                <th style="padding: 0.75rem 0.5rem;">Note / Reference</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="entry" items="${ledger}">
                                <tr style="border-bottom: 1px solid #f1f5f9; font-size: 0.9rem;">
                                    <td style="padding: 0.85rem 0.5rem; color: #64748b; white-space: nowrap;">
                                        <c:out value="${entry.createdAt}"/>
                                    </td>
                                    <td style="padding: 0.85rem 0.5rem;">
                                        <c:choose>
                                            <c:when test="${entry.entryType == 'SIGNUP_BONUS'}">
                                                <span class="badge badge-success">SIGNUP BONUS</span>
                                            </c:when>
                                            <c:when test="${entry.entryType == 'HOLD'}">
                                                <span class="badge badge-warning">ESCROW HOLD</span>
                                            </c:when>
                                            <c:when test="${entry.entryType == 'RELEASE'}">
                                                <span class="badge badge-info">HOLD RELEASE</span>
                                            </c:when>
                                            <c:when test="${entry.entryType == 'EARN'}">
                                                <span class="badge badge-success">EARNED</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-secondary"><c:out value="${entry.entryType}"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="padding: 0.85rem 0.5rem; text-align: right; font-weight: 700; color: ${entry.amount > 0 ? '#10B981' : '#E11D48'};">
                                        <c:choose>
                                            <c:when test="${entry.amount > 0}">+<c:out value="${entry.amount}"/></c:when>
                                            <c:otherwise><c:out value="${entry.amount}"/></c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="padding: 0.85rem 0.5rem; text-align: right; font-weight: 600; color: #1e293b;">
                                        <c:out value="${entry.balanceAfter}"/>
                                    </td>
                                    <td style="padding: 0.85rem 0.5rem; color: #475569;">
                                        <c:out value="${entry.note}"/>
                                        <c:if test="${entry.sessionRequestId != null}">
                                            <a href="${pageContext.request.contextPath}/requests/view?id=${entry.sessionRequestId}"
                                               style="margin-left: 0.5rem; color: var(--color-teal); font-weight: 600; font-size: 0.85rem;">
                                                View Session #${entry.sessionRequestId} →
                                            </a>
                                        </c:if>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>

                <!-- Pagination -->
                <c:if test="${paginator.totalPages > 1}">
                    <div class="pagination" style="margin-top: 1.5rem; display: flex; justify-content: center; gap: 0.5rem;">
                        <c:if test="${paginator.hasPrev()}">
                            <a href="${pageContext.request.contextPath}/credits?page=${paginator.currentPage - 1}" class="btn btn-outline">← Prev</a>
                        </c:if>
                        <span style="display: inline-flex; align-items: center; padding: 0 1rem; font-weight: 600; color: #475569;">
                            Page ${paginator.currentPage} of ${paginator.totalPages}
                        </span>
                        <c:if test="${paginator.hasNext()}">
                            <a href="${pageContext.request.contextPath}/credits?page=${paginator.currentPage + 1}" class="btn btn-outline">Next →</a>
                        </c:if>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
