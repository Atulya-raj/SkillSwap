<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Recommended Matches" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<div class="container mt-4">
    <div class="d-flex justify-between align-center mb-4">
        <div>
            <h2>⚡ Recommended Matches For You</h2>
            <p class="text-muted">Ranked by our intelligent matching engine combining topic relevance, mentor credibility, and weekly schedule overlap.</p>
        </div>
        <a href="${pageContext.request.contextPath}/skills" class="btn btn-secondary">+ Update Wanted Skills</a>
    </div>

    <!-- Wanted Skills Filter Pills -->
    <c:if test="${not empty wantedSkills}">
        <div class="d-flex align-center gap-2 flex-wrap mb-4 p-2 rounded" style="background: var(--color-background); border: 1px solid var(--color-border);">
            <span class="text-muted ms-2 me-1" style="font-size: 0.9rem;">Filter by your wanted skill:</span>
            <a href="${pageContext.request.contextPath}/matches"
               class="btn btn-sm <c:choose><c:when test='${empty selectedSkillId}'>btn-primary</c:when><c:otherwise>btn-secondary</c:otherwise></c:choose>">
                All Recommendations
            </a>
            <c:forEach var="ws" items="${wantedSkills}">
                <a href="${pageContext.request.contextPath}/matches?skillId=${ws.skillId}"
                   class="btn btn-sm <c:choose><c:when test='${selectedSkillId == ws.skillId}'>btn-primary</c:when><c:otherwise>btn-secondary</c:otherwise></c:choose>">
                    <c:out value="${ws.skillName}" />
                </a>
            </c:forEach>
        </div>
    </c:if>

    <!-- Match Results -->
    <c:choose>
        <c:when test="${noMatchFound or empty matches}">
            <div class="card text-center py-5">
                <div style="font-size: 3.5rem;">🤝</div>
                <h3>No Matching Mentors Available Right Now</h3>
                <p class="text-muted" style="max-width: 500px; margin: 0 auto 1.5rem auto;">
                    We couldn't find active tutors offering this skill with compatible criteria. Our background match engine continuously scans for new tutors and will notify you when a match joins!
                </p>
                <div class="d-flex justify-center gap-3">
                    <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary">Browse All Skill Providers</a>
                    <a href="${pageContext.request.contextPath}/skills" class="btn btn-secondary">Manage Wanted Skills</a>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="d-flex flex-column gap-4 mb-5">
                <c:forEach var="match" items="${matches}">
                    <div class="card p-4">
                        <div class="row align-center">
                            <!-- Left: Provider & Skill Details -->
                            <div class="col col-7">
                                <div class="d-flex align-center gap-3 mb-2">
                                    <div style="width: 75px; height: 75px; border-radius: 50%; overflow: hidden; border: 3px solid var(--color-amber); flex-shrink: 0;">
                                        <img src="${pageContext.request.contextPath}/avatar?id=${match.provider.id}" alt="Avatar" style="width: 100%; height: 100%; object-fit: cover;" />
                                    </div>
                                    <div>
                                        <h3 class="mb-0">
                                            <a href="${pageContext.request.contextPath}/users/view?id=${match.provider.id}" style="text-decoration:none; color:inherit;">
                                                <c:out value="${match.provider.fullName}" />
                                            </a>
                                        </h3>
                                        <div class="text-muted" style="font-size: 0.9rem;">
                                            <c:if test="${not empty match.provider.department}">
                                                <c:out value="${match.provider.department}" /> •
                                            </c:if>
                                            ★ <strong class="text-amber"><c:out value="${match.provider.avgRating}" /></strong> (<c:out value="${match.provider.ratingCount}" /> ratings) •
                                            <c:out value="${match.provider.sessionsTaught}" /> taught
                                        </div>
                                    </div>
                                </div>

                                <div class="p-2 rounded mb-2" style="background: var(--color-background);">
                                    <div class="d-flex justify-between align-center">
                                        <strong class="text-primary" style="font-size: 1.1rem;"><c:out value="${match.listing.skillName}" /></strong>
                                        <div>
                                            <span class="badge badge-info"><c:out value="${match.listing.level}" /></span>
                                            <span class="badge badge-warning"><c:out value="${match.listing.creditsPerHour}" /> credits/hr</span>
                                        </div>
                                    </div>
                                    <c:if test="${not empty match.listing.description}">
                                        <p class="text-muted mb-0 mt-1" style="font-size: 0.85rem;"><c:out value="${match.listing.description}" /></p>
                                    </c:if>
                                </div>
                            </div>

                            <!-- Right: Score Breakdown Bars & Actions -->
                            <div class="col col-5">
                                <div class="p-3 border rounded" style="background: #FAFAFA;">
                                    <div class="d-flex justify-between align-center mb-1">
                                        <span style="font-weight: 600; font-size: 0.95rem;">Overall Match Score</span>
                                        <span class="badge badge-primary" style="font-size: 0.9rem;"><c:out value="${match.scorePercent}" />%</span>
                                    </div>
                                    <!-- Master Score Bar -->
                                    <div style="background: #E2E2E6; height: 10px; border-radius: 5px; overflow: hidden; margin-bottom: 12px;">
                                        <div style="background: linear-gradient(90deg, var(--color-primary), var(--color-amber)); width: ${match.scorePercent}%; height: 100%;"></div>
                                    </div>

                                    <!-- Sub components breakdown -->
                                    <div class="d-flex flex-column gap-1" style="font-size: 0.8rem;">
                                        <div>
                                            <div class="d-flex justify-between text-muted">
                                                <span>Skill Relevance (50% weight):</span>
                                                <strong><c:out value="${match.relevancePercent}" />%</strong>
                                            </div>
                                            <div style="background: #E2E2E6; height: 4px; border-radius: 2px; overflow: hidden;">
                                                <div style="background: var(--color-teal); width: ${match.relevancePercent}%; height: 100%;"></div>
                                            </div>
                                        </div>
                                        <div>
                                            <div class="d-flex justify-between text-muted">
                                                <span>Tutor Rating (30% weight):</span>
                                                <strong><c:out value="${match.ratingPercent}" />%</strong>
                                            </div>
                                            <div style="background: #E2E2E6; height: 4px; border-radius: 2px; overflow: hidden;">
                                                <div style="background: var(--color-amber); width: ${match.ratingPercent}%; height: 100%;"></div>
                                            </div>
                                        </div>
                                        <div>
                                            <div class="d-flex justify-between text-muted">
                                                <span>Schedule Overlap (20% weight):</span>
                                                <strong><c:out value="${match.availabilityPercent}" />%</strong>
                                            </div>
                                            <div style="background: #E2E2E6; height: 4px; border-radius: 2px; overflow: hidden;">
                                                <div style="background: #4CAF50; width: ${match.availabilityPercent}%; height: 100%;"></div>
                                            </div>
                                        </div>
                                    </div>

                                    <div class="d-flex gap-2 mt-3">
                                        <a href="${pageContext.request.contextPath}/requests/new?listingId=${match.listing.id}" class="btn btn-primary btn-sm w-100">Request Session</a>
                                        <a href="${pageContext.request.contextPath}/users/view?id=${match.provider.id}" class="btn btn-secondary btn-sm">Profile</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="layout/footer.jspf" %>
