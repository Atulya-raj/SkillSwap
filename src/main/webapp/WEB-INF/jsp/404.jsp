<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="404 - Page Not Found - SkillSwap" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="main-content">
    <div class="container">
        <div class="empty-state" style="max-width: 600px; margin: 3rem auto;">
            <div class="empty-state-icon">🔍</div>
            <h1 class="empty-state-title">Page Not Found</h1>
            <p class="empty-state-desc">
                The page or resource you are looking for might have been moved, deleted, or does not exist.
            </p>
            <div class="flex-center gap-2">
                <a href="${pageContext.request.contextPath}/" class="btn btn-primary" id="btn-notfound-home">Return to Home</a>
                <a href="${pageContext.request.contextPath}/browse" class="btn btn-outline" id="btn-notfound-browse">Browse Skills</a>
            </div>
        </div>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
