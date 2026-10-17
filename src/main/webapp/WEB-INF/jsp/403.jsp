<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="403 - Access Forbidden - SkillSwap" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="main-content">
    <div class="container">
        <div class="empty-state" style="max-width: 600px; margin: 3rem auto;">
            <div class="empty-state-icon" style="color: var(--color-danger);">⛔</div>
            <h1 class="empty-state-title">Access Forbidden</h1>
            <p class="empty-state-desc">
                <c:choose>
                    <c:when test="${not empty errorMessage}">
                        <c:out value="${errorMessage}"/>
                    </c:when>
                    <c:otherwise>
                        You do not have permission to view or perform operations on this resource.
                    </c:otherwise>
                </c:choose>
            </p>
            <div class="flex-center gap-2">
                <a href="${pageContext.request.contextPath}/" class="btn btn-primary" id="btn-forbidden-home">Back to Safety</a>
                <c:if test="${empty sessionScope.currentUser}">
                    <a href="${pageContext.request.contextPath}/login" class="btn btn-outline" id="btn-forbidden-login">Sign In</a>
                </c:if>
            </div>
        </div>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
