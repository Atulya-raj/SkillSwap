<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Change Password - SkillSwap" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="main-content">
    <div class="container">
        <div class="auth-wrapper" style="max-width: 480px;">
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">Change Password</h2>
                </div>

                <form action="${pageContext.request.contextPath}/password" method="post" id="password-form">
                    <input type="hidden" name="_csrf" value="${csrfToken}">

                    <div class="form-group">
                        <label for="pwd-current" class="form-label required">Current Password</label>
                        <input type="password" id="pwd-current" name="currentPassword" class="form-control" required autocomplete="current-password">
                    </div>

                    <div class="form-group">
                        <label for="pwd-new" class="form-label required">New Password</label>
                        <input type="password" id="pwd-new" name="newPassword"
                               class="form-control ${not empty errors['newPassword'] ? 'is-invalid' : ''}" required autocomplete="new-password">
                        <c:if test="${not empty errors['newPassword']}">
                            <span class="form-error" id="err-newPassword"><c:out value="${errors['newPassword']}"/></span>
                        </c:if>
                        <span class="form-help">Must be at least 8 characters long and contain both letters and digits.</span>
                    </div>

                    <div class="form-group">
                        <label for="pwd-confirm" class="form-label required">Confirm New Password</label>
                        <input type="password" id="pwd-confirm" name="confirmPassword" class="form-control" required autocomplete="new-password">
                    </div>

                    <div class="flex gap-2" style="margin-top: 1.5rem;">
                        <button type="submit" class="btn btn-primary" id="btn-submit-password">Update Password</button>
                        <a href="${pageContext.request.contextPath}/profile" class="btn btn-outline" id="btn-cancel-password">Cancel</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
