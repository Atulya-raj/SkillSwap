<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Manage My Skills" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<div class="container mt-4">
    <div class="d-flex justify-between align-center mb-4">
        <div>
            <h2>Manage My Skills</h2>
            <p class="text-muted">Configure the skills you can mentor peers in and the skills you are looking to learn.</p>
        </div>
        <a href="${pageContext.request.contextPath}/availability" class="btn btn-secondary">Manage Availability Schedule →</a>
    </div>

    <div class="row">
        <!-- Offered Skills Section -->
        <div class="col col-6">
            <div class="card mb-4">
                <div class="d-flex justify-between align-center mb-3">
                    <h3 class="card-title mb-0">Skills I Can Teach (Offered)</h3>
                    <span class="badge badge-primary"><c:out value="${offeredSkills.size()}" /> active</span>
                </div>

                <!-- Add Offered Skill Form -->
                <div class="p-3 mb-4 rounded" style="background: var(--color-background); border: 1px solid var(--color-border);">
                    <h4 class="mb-3">Offer a New Skill</h4>
                    <form action="${pageContext.request.contextPath}/skills" method="post">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                        <input type="hidden" name="action" value="offer" />

                        <div class="form-group mb-2">
                            <label for="offerSkillId" class="form-label">Skill *</label>
                            <select id="offerSkillId" name="skillId" class="form-control" required>
                                <option value="">Select a skill...</option>
                                <c:forEach var="entry" items="${skillsByCategory}">
                                    <optgroup label="<c:out value='${entry.key}'/>">
                                        <c:forEach var="skill" items="${entry.value}">
                                            <option value="${skill.id}"><c:out value="${skill.name}" /></option>
                                        </c:forEach>
                                    </optgroup>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="row mb-2">
                            <div class="col col-6">
                                <label for="offerLevel" class="form-label">Proficiency Level *</label>
                                <select id="offerLevel" name="level" class="form-control" required>
                                    <c:forEach var="lvl" items="${skillLevels}">
                                        <option value="${lvl}" <c:if test="${lvl == 'INTERMEDIATE'}">selected</c:if>>
                                            <c:out value="${lvl}" />
                                        </option>
                                    </c:forEach>
                                </select>
                            </div>
                            <div class="col col-6">
                                <label for="creditsPerHour" class="form-label">Rate (1-5 credits/hr) *</label>
                                <input type="number" id="creditsPerHour" name="creditsPerHour" class="form-control"
                                       min="1" max="5" value="1" required />
                            </div>
                        </div>

                        <div class="form-group mb-3">
                            <label for="offerDescription" class="form-label">What you can cover in sessions:</label>
                            <textarea id="offerDescription" name="description" class="form-control" rows="2"
                                      placeholder="e.g. Can explain pointers, dynamic memory allocation, and debugging tools." maxlength="255"></textarea>
                        </div>

                        <button type="submit" class="btn btn-primary btn-sm w-100">Add Offered Skill</button>
                    </form>
                </div>

                <!-- Existing Offered Skills -->
                <c:choose>
                    <c:when test="${empty offeredSkills}">
                        <div class="empty-state">
                            <p class="text-muted">You have not listed any skills to teach yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="d-flex flex-column gap-2">
                            <c:forEach var="listing" items="${offeredSkills}">
                                <div class="p-3 border rounded d-flex justify-between align-center">
                                    <div>
                                        <strong><c:out value="${listing.skillName}" /></strong>
                                        <span class="badge badge-info ms-2"><c:out value="${listing.level}" /></span>
                                        <span class="badge badge-warning ms-1"><c:out value="${listing.creditsPerHour}" /> credits/hr</span>
                                        <c:if test="${not empty listing.description}">
                                            <p class="text-muted mb-0 mt-1" style="font-size: 0.9rem;"><c:out value="${listing.description}" /></p>
                                        </c:if>
                                    </div>
                                    <form action="${pageContext.request.contextPath}/skills" method="post" style="margin: 0;">
                                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                                        <input type="hidden" name="action" value="remove" />
                                        <input type="hidden" name="id" value="<c:out value='${listing.id}'/>" />
                                        <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('Remove this offered skill?');">Remove</button>
                                    </form>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Wanted Skills Section -->
        <div class="col col-6">
            <div class="card mb-4">
                <div class="d-flex justify-between align-center mb-3">
                    <h3 class="card-title mb-0">Skills I Want to Learn</h3>
                    <span class="badge badge-primary"><c:out value="${wantedSkills.size()}" /> active</span>
                </div>

                <!-- Add Wanted Skill Form -->
                <div class="p-3 mb-4 rounded" style="background: var(--color-background); border: 1px solid var(--color-border);">
                    <h4 class="mb-3">Add a Skill to Learn</h4>
                    <form action="${pageContext.request.contextPath}/skills" method="post">
                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                        <input type="hidden" name="action" value="want" />

                        <div class="form-group mb-2">
                            <label for="wantSkillId" class="form-label">Skill *</label>
                            <select id="wantSkillId" name="skillId" class="form-control" required>
                                <option value="">Select a skill...</option>
                                <c:forEach var="entry" items="${skillsByCategory}">
                                    <optgroup label="<c:out value='${entry.key}'/>">
                                        <c:forEach var="skill" items="${entry.value}">
                                            <option value="${skill.id}"><c:out value="${skill.name}" /></option>
                                        </c:forEach>
                                    </optgroup>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="form-group mb-2">
                            <label for="wantLevel" class="form-label">Target Level *</label>
                            <select id="wantLevel" name="level" class="form-control" required>
                                <c:forEach var="lvl" items="${skillLevels}">
                                    <option value="${lvl}" <c:if test="${lvl == 'BEGINNER'}">selected</c:if>>
                                        <c:out value="${lvl}" />
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <div class="form-group mb-3">
                            <label for="wantDescription" class="form-label">Learning Goals / Notes:</label>
                            <textarea id="wantDescription" name="description" class="form-control" rows="2"
                                      placeholder="e.g. Preparing for exam; need help with dynamic programming." maxlength="255"></textarea>
                        </div>

                        <button type="submit" class="btn btn-secondary btn-sm w-100">Add Wanted Skill</button>
                    </form>
                </div>

                <!-- Existing Wanted Skills -->
                <c:choose>
                    <c:when test="${empty wantedSkills}">
                        <div class="empty-state">
                            <p class="text-muted">You have not listed any skills you want to learn yet.</p>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="d-flex flex-column gap-2">
                            <c:forEach var="listing" items="${wantedSkills}">
                                <div class="p-3 border rounded d-flex justify-between align-center">
                                    <div>
                                        <strong><c:out value="${listing.skillName}" /></strong>
                                        <span class="badge badge-info ms-2"><c:out value="${listing.level}" /></span>
                                        <c:if test="${not empty listing.description}">
                                            <p class="text-muted mb-0 mt-1" style="font-size: 0.9rem;"><c:out value="${listing.description}" /></p>
                                        </c:if>
                                    </div>
                                    <form action="${pageContext.request.contextPath}/skills" method="post" style="margin: 0;">
                                        <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                                        <input type="hidden" name="action" value="remove" />
                                        <input type="hidden" name="id" value="<c:out value='${listing.id}'/>" />
                                        <button type="submit" class="btn btn-danger btn-sm" onclick="return confirm('Remove this wanted skill?');">Remove</button>
                                    </form>
                                </div>
                            </c:forEach>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jspf" %>
