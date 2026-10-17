<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Browse Skill Providers" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<div class="container mt-4">
    <div class="d-flex justify-between align-center mb-4">
        <div>
            <h2>Browse & Search Skill Providers</h2>
            <p class="text-muted">Find peers offering mentorship in programming, mathematics, languages, design, and more.</p>
        </div>
        <a href="${pageContext.request.contextPath}/matches" class="btn btn-secondary">⚡ View AI Recommended Matches</a>
    </div>

    <!-- Filter Bar Card -->
    <div class="card mb-4" style="background: var(--color-background); border: 1px solid var(--color-border);">
        <form action="${pageContext.request.contextPath}/browse" method="get">
            <div class="row mb-3">
                <div class="col col-4">
                    <label for="keyword" class="form-label">Keyword Search</label>
                    <input type="text" id="keyword" name="keyword" class="form-control"
                           placeholder="Search skill, topic, or tutor..."
                           value="<c:out value='${criteria.keyword}'/>" />
                </div>
                <div class="col col-4">
                    <label for="categoryId" class="form-label">Category</label>
                    <select id="categoryId" name="categoryId" class="form-control">
                        <option value="">All Categories</option>
                        <c:forEach var="cat" items="${categories}">
                            <option value="${cat.id}" <c:if test="${criteria.categoryId == cat.id}">selected</c:if>>
                                <c:out value="${cat.name}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col col-4">
                    <label for="level" class="form-label">Proficiency Level</label>
                    <select id="level" name="level" class="form-control">
                        <option value="">Any Level</option>
                        <c:forEach var="lvl" items="${skillLevels}">
                            <option value="${lvl}" <c:if test="${criteria.level == lvl}">selected</c:if>>
                                <c:out value="${lvl}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
            </div>

            <div class="row align-center">
                <div class="col col-3">
                    <label for="minRating" class="form-label">Minimum Rating</label>
                    <select id="minRating" name="minRating" class="form-control">
                        <option value="">Any Rating</option>
                        <option value="4.5" <c:if test="${criteria.minRating == '4.5'}">selected</c:if>>★ 4.5 & up</option>
                        <option value="4.0" <c:if test="${criteria.minRating == '4.0'}">selected</c:if>>★ 4.0 & up</option>
                        <option value="3.0" <c:if test="${criteria.minRating == '3.0'}">selected</c:if>>★ 3.0 & up</option>
                    </select>
                </div>
                <div class="col col-3">
                    <label for="dayOfWeek" class="form-label">Available On</label>
                    <select id="dayOfWeek" name="dayOfWeek" class="form-control">
                        <option value="">Any Day</option>
                        <c:forEach var="d" items="${daysOfWeek}">
                            <option value="${d}" <c:if test="${criteria.dayOfWeek == d}">selected</c:if>>
                                <c:out value="${d}" />
                            </option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col col-3">
                    <label for="sort" class="form-label">Sort By</label>
                    <select id="sort" name="sort" class="form-control">
                        <option value="BEST" <c:if test="${criteria.sort == 'BEST'}">selected</c:if>>Best Match</option>
                        <option value="RATING" <c:if test="${criteria.sort == 'RATING'}">selected</c:if>>Highest Rating</option>
                        <option value="PRICE" <c:if test="${criteria.sort == 'PRICE'}">selected</c:if>>Lowest Credit Rate</option>
                    </select>
                </div>
                <div class="col col-3 d-flex gap-2" style="margin-top: 24px;">
                    <button type="submit" class="btn btn-primary w-100">Apply Filters</button>
                    <a href="${pageContext.request.contextPath}/browse" class="btn btn-secondary">Reset</a>
                </div>
            </div>
        </form>
    </div>

    <!-- Search Results Header -->
    <div class="d-flex justify-between align-center mb-3">
        <h4>Showing <c:out value="${pageResult.totalItems}" /> available skill listing(s)</h4>
        <span class="text-muted">Page <c:out value="${pageResult.page}" /> of <c:out value="${pageResult.totalPages}" /></span>
    </div>

    <!-- Results Cards -->
    <c:choose>
        <c:when test="${empty pageResult.items}">
            <div class="card text-center py-5">
                <div style="font-size: 3rem;">🔍</div>
                <h3>No providers matched your filter criteria</h3>
                <p class="text-muted">Try clearing some filters, expanding your search terms, or checking back later.</p>
                <div class="mt-3">
                    <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary">Clear All Filters</a>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <div class="d-flex flex-column gap-3 mb-4">
                <c:forEach var="card" items="${pageResult.items}">
                    <div class="card p-3">
                        <div class="d-flex justify-between align-center flex-wrap gap-3">
                            <!-- Left: Provider info & Avatar -->
                            <div class="d-flex align-center gap-3">
                                <div style="width: 70px; height: 70px; border-radius: 50%; overflow: hidden; border: 2px solid var(--color-amber); flex-shrink: 0;">
                                    <img src="${pageContext.request.contextPath}/avatar?id=${card.provider.id}" alt="Avatar" style="width: 100%; height: 100%; object-fit: cover;" />
                                </div>
                                <div>
                                    <div class="d-flex align-center gap-2">
                                        <h4 class="mb-0">
                                            <a href="${pageContext.request.contextPath}/users/view?id=${card.provider.id}" style="text-decoration:none; color:inherit;">
                                                <c:out value="${card.provider.fullName}" />
                                            </a>
                                        </h4>
                                        <c:if test="${not empty card.provider.department}">
                                            <small class="text-muted">• <c:out value="${card.provider.department}" /></small>
                                        </c:if>
                                    </div>
                                    <div class="text-muted mb-1" style="font-size: 0.9rem;">
                                        ★ <strong class="text-amber"><c:out value="${card.provider.avgRating}" /></strong>
                                        (<c:out value="${card.provider.ratingCount}" /> ratings) •
                                        <c:out value="${card.provider.sessionsTaught}" /> sessions taught
                                    </div>
                                    <!-- Badges -->
                                    <c:if test="${not empty card.badges}">
                                        <div class="d-flex flex-wrap gap-1">
                                            <c:forEach var="badge" items="${card.badges}">
                                                <span class="badge badge-success" style="font-size: 0.75rem;" title="<c:out value='${badge.description}'/>">
                                                    🏆 <c:out value="${badge.name}" />
                                                </span>
                                            </c:forEach>
                                        </div>
                                    </c:if>
                                </div>
                            </div>

                            <!-- Right: Skill Details & Action -->
                            <div class="text-end">
                                <div class="mb-1">
                                    <h3 class="mb-0 text-primary"><c:out value="${card.listing.skillName}" /></h3>
                                </div>
                                <div class="mb-2">
                                    <span class="badge badge-info"><c:out value="${card.listing.level}" /></span>
                                    <span class="badge badge-warning"><c:out value="${card.listing.creditsPerHour}" /> credits/hr</span>
                                </div>
                                <div class="d-flex gap-2 justify-end">
                                    <a href="${pageContext.request.contextPath}/users/view?id=${card.provider.id}" class="btn btn-secondary btn-sm">Profile</a>
                                    <c:if test="${sessionScope.currentUser != null && sessionScope.currentUser.id != card.provider.id && sessionScope.currentUser.role == 'STUDENT'}">
                                        <a href="${pageContext.request.contextPath}/requests/new?listingId=${card.listing.id}" class="btn btn-primary btn-sm">Request Session</a>
                                    </c:if>
                                </div>
                            </div>
                        </div>

                        <c:if test="${not empty card.listing.description}">
                            <div class="mt-2 pt-2 border-top text-muted" style="font-size: 0.9rem;">
                                <c:out value="${card.listing.description}" />
                            </div>
                        </c:if>
                    </div>
                </c:forEach>
            </div>

            <!-- Pagination Bar -->
            <c:if test="${pageResult.totalPages > 1}">
                <div class="d-flex justify-between align-center p-3 border rounded" style="background: var(--color-background);">
                    <div>
                        <c:if test="${pageResult.hasPrevious}">
                            <a href="${pageContext.request.contextPath}/browse?keyword=${criteria.keyword}&categoryId=${criteria.categoryId}&skillId=${criteria.skillId}&level=${criteria.level}&minRating=${criteria.minRating}&dayOfWeek=${criteria.dayOfWeek}&sort=${criteria.sort}&page=${pageResult.page - 1}" class="btn btn-secondary btn-sm">← Previous</a>
                        </c:if>
                    </div>
                    <div>
                        <span class="text-muted">Page <strong><c:out value="${pageResult.page}" /></strong> of <c:out value="${pageResult.totalPages}" /></span>
                    </div>
                    <div>
                        <c:if test="${pageResult.hasNext}">
                            <a href="${pageContext.request.contextPath}/browse?keyword=${criteria.keyword}&categoryId=${criteria.categoryId}&skillId=${criteria.skillId}&level=${criteria.level}&minRating=${criteria.minRating}&dayOfWeek=${criteria.dayOfWeek}&sort=${criteria.sort}&page=${pageResult.page + 1}" class="btn btn-secondary btn-sm">Next →</a>
                        </c:if>
                    </div>
                </div>
            </c:if>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="layout/footer.jspf" %>
