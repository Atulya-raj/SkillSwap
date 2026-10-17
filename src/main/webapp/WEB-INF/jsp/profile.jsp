<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="My Profile" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<div class="container mt-4">
    <div class="row">
        <!-- Profile Overview & Avatar Card -->
        <div class="col col-4">
            <div class="card text-center mb-4">
                <div class="avatar-container mb-3" style="margin: 0 auto; width: 120px; height: 120px; border-radius: 50%; overflow: hidden; border: 3px solid var(--color-amber);">
                    <img src="${pageContext.request.contextPath}/avatar?id=${profileUser.id}" alt="Profile Avatar" style="width: 100%; height: 100%; object-fit: cover;" />
                </div>
                <h3><c:out value="${profileUser.fullName}" /></h3>
                <p class="text-muted"><c:out value="${profileUser.email}" /></p>
                <div class="badge badge-primary mb-3"><c:out value="${profileUser.role}" /></div>

                <!-- Avatar Upload Form -->
                <form action="${pageContext.request.contextPath}/profile/avatar" method="post" enctype="multipart/form-data" class="mt-2 text-start">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                    <div class="form-group mb-2">
                        <label for="avatarFile" class="form-label" style="font-size: 0.85rem;">Change Avatar (JPG/PNG, max 2MB):</label>
                        <input type="file" id="avatarFile" name="avatar" class="form-control" accept=".jpg,.jpeg,.png" required />
                    </div>
                    <button type="submit" class="btn btn-secondary btn-sm w-100">Upload New Photo</button>
                </form>
            </div>

            <!-- Stats Card -->
            <div class="card mb-4">
                <h4 class="card-title">My Learning Stats</h4>
                <div class="d-flex justify-between align-center py-2 border-bottom">
                    <span class="text-muted">Credit Balance:</span>
                    <strong class="text-amber"><c:out value="${profileUser.credits}" /> credits</strong>
                </div>
                <div class="d-flex justify-between align-center py-2 border-bottom">
                    <span class="text-muted">Average Rating:</span>
                    <span>★ <c:out value="${profileUser.avgRating}" /> (<c:out value="${profileUser.ratingCount}" /> reviews)</span>
                </div>
                <div class="d-flex justify-between align-center py-2 border-bottom">
                    <span class="text-muted">Sessions Taught:</span>
                    <strong><c:out value="${profileUser.sessionsTaught}" /></strong>
                </div>
                <div class="d-flex justify-between align-center py-2 border-bottom">
                    <span class="text-muted">Sessions Learned:</span>
                    <strong><c:out value="${profileUser.sessionsLearned}" /></strong>
                </div>
                <div class="d-flex justify-between align-center py-2">
                    <span class="text-muted">Reliability Strikes:</span>
                    <span class="<c:if test='${profileUser.reliabilityStrikes > 0}'>text-danger</c:if>">
                        <c:out value="${profileUser.reliabilityStrikes}" />
                    </span>
                </div>
            </div>

            <!-- Earned Badges -->
            <div class="card mb-4">
                <h4 class="card-title">Earned Badges</h4>
                <c:choose>
                    <c:when test="${empty userBadges}">
                        <p class="text-muted" style="font-size: 0.9rem;">No badges earned yet. Complete sessions and teach skills to unlock achievements!</p>
                    </c:when>
                    <c:otherwise>
                        <div class="d-flex flex-wrap gap-2">
                            <c:forEach var="b" items="${userBadges}">
                                <div class="badge badge-success p-2" title="<c:out value='${b.description}'/>">
                                    🏆 <c:out value="${b.name}" />
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Edit Profile Form -->
        <div class="col col-8">
            <div class="card mb-4">
                <h3 class="card-title">Edit Profile Information</h3>
                <p class="text-muted mb-4">Keep your university department, academic year, and bio up to date so peers can easily connect with you.</p>

                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />

                    <div class="form-group mb-3">
                        <label for="fullName" class="form-label">Full Name *</label>
                        <input type="text" id="fullName" name="fullName" class="form-control"
                               value="<c:out value='${profileUser.fullName}'/>" required minlength="2" maxlength="100" />
                    </div>

                    <div class="form-group mb-3">
                        <label for="email" class="form-label">Email Address (Read-Only)</label>
                        <input type="email" id="email" class="form-control"
                               value="<c:out value='${profileUser.email}'/>" disabled />
                        <small class="text-muted">Email is linked to your campus credentials and cannot be changed.</small>
                    </div>

                    <div class="row">
                        <div class="col col-6">
                            <div class="form-group mb-3">
                                <label for="department" class="form-label">Department / Major</label>
                                <input type="text" id="department" name="department" class="form-control"
                                       value="<c:out value='${profileUser.department}'/>" placeholder="e.g. Computer Science" maxlength="100" />
                            </div>
                        </div>
                        <div class="col col-6">
                            <div class="form-group mb-3">
                                <label for="studyYear" class="form-label">Year of Study</label>
                                <select id="studyYear" name="studyYear" class="form-control">
                                    <option value="">Select academic year...</option>
                                    <c:forEach var="yr" begin="1" end="5">
                                        <option value="${yr}" <c:if test="${profileUser.studyYear == yr}">selected</c:if>>
                                            Year <c:out value="${yr}" />
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                    </div>

                    <div class="form-group mb-4">
                        <label for="bio" class="form-label">About Me (Bio)</label>
                        <textarea id="bio" name="bio" class="form-control" rows="4" maxlength="500"
                                  placeholder="Introduce yourself, your academic interests, and what you enjoy learning or teaching..."><c:out value="${profileUser.bio}"/></textarea>
                        <small class="text-muted">Maximum 500 characters.</small>
                    </div>

                    <div class="d-flex justify-between align-center">
                        <button type="submit" class="btn btn-primary">Save Changes</button>
                        <a href="${pageContext.request.contextPath}/users/view?id=${profileUser.id}" class="btn btn-secondary">Preview Public Profile</a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jspf" %>
