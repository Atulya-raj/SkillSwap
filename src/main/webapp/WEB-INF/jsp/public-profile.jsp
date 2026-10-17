<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="${profileData.user.fullName} - Public Profile" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<div class="container mt-4">
    <!-- Header Banner Card -->
    <div class="card mb-4">
        <div class="d-flex align-center gap-4 flex-wrap">
            <div style="width: 110px; height: 110px; border-radius: 50%; overflow: hidden; border: 3px solid var(--color-amber); flex-shrink: 0;">
                <img src="${pageContext.request.contextPath}/avatar?id=${profileData.user.id}" alt="Avatar" style="width: 100%; height: 100%; object-fit: cover;" />
            </div>
            <div style="flex-grow: 1;">
                <div class="d-flex align-center gap-3">
                    <h2><c:out value="${profileData.user.fullName}" /></h2>
                    <span class="badge badge-primary"><c:out value="${profileData.user.role}" /></span>
                </div>
                <p class="text-muted mb-2">
                    <c:if test="${not empty profileData.user.department}">
                        <c:out value="${profileData.user.department}" /> •
                    </c:if>
                    <c:if test="${not empty profileData.user.studyYear}">
                        Year <c:out value="${profileData.user.studyYear}" /> •
                    </c:if>
                    ★ <c:out value="${profileData.user.avgRating}" /> (<c:out value="${profileData.user.ratingCount}" /> ratings)
                </p>
                <c:if test="${not empty profileData.user.bio}">
                    <p class="mb-0" style="max-width: 750px;"><c:out value="${profileData.user.bio}" /></p>
                </c:if>
            </div>
            <div>
                <!-- Report button -->
                <c:if test="${sessionScope.currentUser != null && sessionScope.currentUser.id != profileData.user.id}">
                    <form action="${pageContext.request.contextPath}/report" method="post" style="display:inline;">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                        <input type="hidden" name="reportedUserId" value="<c:out value='${profileData.user.id}'/>" />
                        <button type="button" class="btn btn-secondary btn-sm" onclick="this.nextElementSibling.style.display='block';">Report User</button>
                        <div style="display:none; margin-top: 8px;">
                            <input type="text" name="reason" placeholder="Reason for report..." class="form-control mb-1" style="font-size:0.85rem;" required />
                            <button type="submit" class="btn btn-danger btn-sm">Confirm Report</button>
                        </div>
                    </form>
                </c:if>
            </div>
        </div>
    </div>

    <!-- Badges Row -->
    <c:if test="${not empty profileData.badges}">
        <div class="card mb-4">
            <h4 class="card-title">Earned Achievements</h4>
            <div class="d-flex flex-wrap gap-2">
                <c:forEach var="badge" items="${profileData.badges}">
                    <div class="badge badge-success p-2" title="<c:out value='${badge.description}'/>">
                        🏆 <c:out value="${badge.name}" /> - <small><c:out value="${badge.description}" /></small>
                    </div>
                </c:forEach>
            </div>
        </div>
    </c:if>

    <div class="row">
        <!-- Offered Skills -->
        <div class="col col-6">
            <div class="card mb-4">
                <h3 class="card-title">Skills Offered (Can Teach)</h3>
                <c:choose>
                    <c:when test="${empty profileData.offeredSkills}">
                        <div class="empty-state">
                            <p class="text-muted">No skills offered currently.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="d-flex flex-column gap-3">
                            <c:forEach var="offered" items="${profileData.offeredSkills}">
                                <div class="p-3 border rounded">
                                    <div class="d-flex justify-between align-center mb-1">
                                        <h4 class="mb-0"><c:out value="${offered.skillName}" /></h4>
                                        <span class="badge badge-warning"><c:out value="${offered.creditsPerHour}" /> credits/hr</span>
                                    </div>
                                    <div class="text-muted mb-2">
                                        <small>Level: <span class="badge badge-info"><c:out value="${offered.level}" /></span></small>
                                    </div>
                                    <c:if test="${not empty offered.description}">
                                        <p class="text-muted mb-2"><c:out value="${offered.description}" /></p>
                                    </c:if>
                                    <c:if test="${sessionScope.currentUser != null && sessionScope.currentUser.id != profileData.user.id && sessionScope.currentUser.role == 'STUDENT'}">
                                        <a href="${pageContext.request.contextPath}/requests/new?listingId=${offered.id}" class="btn btn-primary btn-sm">Request Session</a>
                                    </c:if>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Wanted Skills -->
        <div class="col col-6">
            <div class="card mb-4">
                <h3 class="card-title">Skills Wanted (Wants to Learn)</h3>
                <c:choose>
                    <c:when test="${empty profileData.wantedSkills}">
                        <div class="empty-state">
                            <p class="text-muted">No wanted skills listed.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="d-flex flex-column gap-3">
                            <c:forEach var="wanted" items="${profileData.wantedSkills}">
                                <div class="p-3 border rounded">
                                    <div class="d-flex justify-between align-center mb-1">
                                        <h4 class="mb-0"><c:out value="${wanted.skillName}" /></h4>
                                        <span class="badge badge-info"><c:out value="${wanted.level}" /></span>
                                    </div>
                                    <c:if test="${not empty wanted.description}">
                                        <p class="text-muted mb-0"><c:out value="${wanted.description}" /></p>
                                    </c:if>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <!-- Weekly Availability Schedule -->
    <div class="card mb-4">
        <h3 class="card-title">Weekly Availability</h3>
        <c:choose>
            <c:when test="${empty profileData.availabilitySlots}">
                <p class="text-muted">No specific availability schedule listed.</p>
            </c:when>
            <c:otherwise>
                <div class="d-flex flex-wrap gap-2">
                    <c:forEach var="slot" items="${profileData.availabilitySlots}">
                        <div class="p-2 border rounded" style="background: var(--color-background); min-width: 140px;">
                            <strong><c:out value="${slot.dayOfWeek}" /></strong><br/>
                            <span class="text-muted"><c:out value="${slot.startTime}" /> - <c:out value="${slot.endTime}" /></span><br/>
                            <span class="badge badge-secondary" style="font-size: 0.75rem;"><c:out value="${slot.mode}" /></span>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Recent Reviews Received -->
    <div class="card mb-4">
        <h3 class="card-title">Recent Feedback & Reviews (<c:out value="${profileData.user.ratingCount}" />)</h3>
        <c:choose>
            <c:when test="${empty profileData.reviews}">
                <div class="empty-state">
                    <p class="text-muted">No reviews yet for this user.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="d-flex flex-column gap-3">
                    <c:forEach var="review" items="${profileData.reviews}">
                        <div class="p-3 border rounded">
                            <div class="d-flex justify-between align-center mb-1">
                                <strong>★ <c:out value="${review.rating}" /> / 5</strong>
                                <div style="display: flex; gap: 0.5rem; align-items: center;">
                                    <small class="text-muted"><c:out value="${review.createdAt}" /></small>
                                    <c:if test="${sessionScope.currentUser != null && sessionScope.currentUser.id != review.reviewerId}">
                                        <form action="${pageContext.request.contextPath}/report" method="post" style="display:inline; margin:0;">
                                            <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                                            <input type="hidden" name="reportedReviewId" value="<c:out value='${review.id}'/>" />
                                            <input type="hidden" name="reason" value="Inappropriate review content" />
                                            <button type="submit" class="btn btn-outline btn-sm" style="font-size:0.75rem; padding: 0.15rem 0.5rem; color: #dc2626; border-color: #fca5a5;" title="Report review">🚩 Report</button>
                                        </form>
                                    </c:if>
                                </div>
                            </div>
                            <c:if test="${not empty review.comment}">
                                <p class="mb-0 text-muted">"<c:out value="${review.comment}" />"</p>
                            </c:if>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="layout/footer.jspf" %>
