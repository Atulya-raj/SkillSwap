<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Create Account - SkillSwap" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="main-content">
    <div class="container">
        <div class="auth-wrapper" style="max-width: 580px;">
            <div class="card">
                <div class="auth-header">
                    <h2>Join SkillSwap</h2>
                    <p style="color:var(--color-text-muted);font-size:0.95rem;">
                        Sign up today and get <strong style="color:var(--color-primary);">10 free learning credits</strong>!
                    </p>
                </div>

                <form action="${pageContext.request.contextPath}/register" method="post" id="register-form">
                    <input type="hidden" name="_csrf" value="${csrfToken}">

                    <div class="form-group">
                        <label for="reg-fullName" class="form-label required">Full Name</label>
                        <input type="text" id="reg-fullName" name="fullName"
                               class="form-control ${not empty errors['fullName'] ? 'is-invalid' : ''}"
                               value="<c:out value="${form.fullName}"/>" required autofocus placeholder="e.g. Maya Chen">
                        <c:if test="${not empty errors['fullName']}">
                            <span class="form-error" id="err-fullName"><c:out value="${errors['fullName']}"/></span>
                        </c:if>
                    </div>

                    <div class="form-group">
                        <label for="reg-email" class="form-label required">Email Address</label>
                        <input type="email" id="reg-email" name="email"
                               class="form-control ${not empty errors['email'] ? 'is-invalid' : ''}"
                               value="<c:out value="${form.email}"/>" required placeholder="e.g. maya@vitstudent.ac.in">
                        <c:if test="${not empty errors['email']}">
                            <span class="form-error" id="err-email"><c:out value="${errors['email']}"/></span>
                        </c:if>
                        <span class="form-help">Use your campus email address.</span>
                    </div>

                    <div class="grid grid-2">
                        <div class="form-group">
                            <label for="reg-department" class="form-label">Department / Major</label>
                            <input type="text" id="reg-department" name="department"
                                   class="form-control ${not empty errors['department'] ? 'is-invalid' : ''}"
                                   value="<c:out value="${form.department}"/>" placeholder="e.g. Computer Science">
                            <c:if test="${not empty errors['department']}">
                                <span class="form-error" id="err-department"><c:out value="${errors['department']}"/></span>
                            </c:if>
                        </div>
                        <div class="form-group">
                            <label for="reg-studyYear" class="form-label">Year of Study</label>
                            <select id="reg-studyYear" name="studyYear" class="form-control">
                                <option value="">Select Year</option>
                                <option value="1" ${form.studyYear == 1 ? 'selected' : ''}>1st Year (Freshman)</option>
                                <option value="2" ${form.studyYear == 2 ? 'selected' : ''}>2nd Year (Sophomore)</option>
                                <option value="3" ${form.studyYear == 3 ? 'selected' : ''}>3rd Year (Junior)</option>
                                <option value="4" ${form.studyYear == 4 ? 'selected' : ''}>4th Year (Senior)</option>
                                <option value="5" ${form.studyYear == 5 ? 'selected' : ''}>5th Year / Postgrad</option>
                            </select>
                            <c:if test="${not empty errors['studyYear']}">
                                <span class="form-error" id="err-studyYear"><c:out value="${errors['studyYear']}"/></span>
                            </c:if>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="reg-bio" class="form-label">Short Bio</label>
                        <textarea id="reg-bio" name="bio" class="form-control ${not empty errors['bio'] ? 'is-invalid' : ''}"
                                  placeholder="Tell other students what you are studying or interested in sharing..."><c:out value="${form.bio}"/></textarea>
                        <c:if test="${not empty errors['bio']}">
                            <span class="form-error" id="err-bio"><c:out value="${errors['bio']}"/></span>
                        </c:if>
                        <span class="form-help">Max 500 characters.</span>
                    </div>

                    <div class="grid grid-2">
                        <div class="form-group">
                            <label for="reg-password" class="form-label required">Password</label>
                            <input type="password" id="reg-password" name="password"
                                   class="form-control ${not empty errors['password'] ? 'is-invalid' : ''}" required>
                            <c:if test="${not empty errors['password']}">
                                <span class="form-error" id="err-password"><c:out value="${errors['password']}"/></span>
                            </c:if>
                            <span class="form-help">Min 8 chars, 1 letter and 1 number.</span>
                        </div>
                        <div class="form-group">
                            <label for="reg-confirmPassword" class="form-label required">Confirm Password</label>
                            <input type="password" id="reg-confirmPassword" name="confirmPassword"
                                   class="form-control ${not empty errors['confirmPassword'] ? 'is-invalid' : ''}" required>
                            <c:if test="${not empty errors['confirmPassword']}">
                                <span class="form-error" id="err-confirmPassword"><c:out value="${errors['confirmPassword']}"/></span>
                            </c:if>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-primary btn-block btn-lg" id="btn-submit-register">
                        Create Account
                    </button>
                </form>

                <div class="auth-footer">
                    Already have an account?
                    <a href="${pageContext.request.contextPath}/login" id="link-goto-login">Sign in here</a>
                </div>
            </div>
        </div>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
