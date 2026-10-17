<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Sign In - SkillSwap" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="main-content">
    <div class="container">
        <div class="auth-wrapper">
            <div class="card">
                <div class="auth-header">
                    <h2>Sign In to SkillSwap</h2>
                    <p style="color:var(--color-text-muted);font-size:0.95rem;">Enter your university credentials to continue</p>
                </div>

                <form action="${pageContext.request.contextPath}/login" method="post" id="login-form">
                    <input type="hidden" name="_csrf" value="${csrfToken}">

                    <div class="form-group">
                        <label for="login-email" class="form-label required">Email Address</label>
                        <input type="email" id="login-email" name="email" class="form-control"
                               value="<c:out value="${email != null ? email : param.email}"/>" required autofocus autocomplete="email">
                    </div>

                    <div class="form-group">
                        <label for="login-password" class="form-label required">Password</label>
                        <input type="password" id="login-password" name="password" class="form-control" required autocomplete="current-password">
                    </div>

                    <button type="submit" class="btn btn-primary btn-block btn-lg" id="btn-submit-login">
                        Sign In
                    </button>
                </form>

                <div class="auth-footer">
                    Don't have an account yet?
                    <a href="${pageContext.request.contextPath}/register" id="link-goto-register">Create one now</a>
                </div>
            </div>
        </div>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
