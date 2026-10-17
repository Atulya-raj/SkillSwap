<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Error Occurred - SkillSwap" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="main-content">
    <div class="container">
        <div class="empty-state" style="max-width: 600px; margin: 3rem auto;">
            <div class="empty-state-icon" style="color: var(--color-danger);">⚠️</div>
            <h1 class="empty-state-title">Something went wrong</h1>
            <p class="empty-state-desc">
                An unexpected condition was encountered while processing your request. Our system administrators have been notified.
            </p>
            <c:if test="${not empty pageContext.exception}">
                <div style="background:#FFF3E0;padding:0.75rem;border-radius:4px;margin-bottom:1.5rem;font-family:monospace;font-size:0.85rem;color:#E65100;">
                    <c:out value="${pageContext.exception.message}"/>
                </div>
            </c:if>
            <div class="flex-center gap-2">
                <a href="${pageContext.request.contextPath}/" class="btn btn-primary" id="btn-error-home">Back to Safety</a>
            </div>
        </div>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
