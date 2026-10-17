<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Weekly Availability Schedule" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<div class="container mt-4">
    <div class="d-flex justify-between align-center mb-4">
        <div>
            <h2>Weekly Availability Schedule</h2>
            <p class="text-muted">Set the recurring weekly time slots when you are free to conduct mentoring sessions.</p>
        </div>
        <a href="${pageContext.request.contextPath}/skills" class="btn btn-secondary">← Back to My Skills</a>
    </div>

    <div class="row">
        <!-- Add Slot Card -->
        <div class="col col-4">
            <div class="card mb-4">
                <h3 class="card-title">Add Free Time Slot</h3>
                <p class="text-muted mb-3" style="font-size: 0.9rem;">Slots will be used by the matching engine to pair you with learners.</p>

                <form action="${pageContext.request.contextPath}/availability" method="post">
                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                    <input type="hidden" name="action" value="add" />

                    <div class="form-group mb-3">
                        <label for="dayOfWeek" class="form-label">Day of Week *</label>
                        <select id="dayOfWeek" name="dayOfWeek" class="form-control" required>
                            <c:forEach var="day" items="${daysOfWeek}">
                                <option value="${day}"><c:out value="${day}" /></option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="row mb-3">
                        <div class="col col-6">
                            <label for="startTime" class="form-label">Start Time *</label>
                            <input type="time" id="startTime" name="startTime" class="form-control" required value="09:00" />
                        </div>
                        <div class="col col-6">
                            <label for="endTime" class="form-label">End Time *</label>
                            <input type="time" id="endTime" name="endTime" class="form-control" required value="11:00" />
                        </div>
                    </div>

                    <div class="form-group mb-4">
                        <label for="sessionMode" class="form-label">Preferred Session Mode *</label>
                        <select id="sessionMode" name="mode" class="form-control" required>
                            <c:forEach var="mode" items="${sessionModes}">
                                <option value="${mode}"><c:out value="${mode}" /></option>
                            </c:forEach>
                        </select>
                    </div>

                    <button type="submit" class="btn btn-primary w-100">Add Availability Slot</button>
                </form>
            </div>
        </div>

        <!-- Weekly Schedule Grid View -->
        <div class="col col-8">
            <div class="card mb-4">
                <h3 class="card-title">My Recurring Weekly Slots</h3>
                <div class="d-flex flex-column gap-3 mt-3">
                    <c:forEach var="day" items="${daysOfWeek}">
                        <div class="p-3 border rounded">
                            <div class="d-flex justify-between align-center mb-2">
                                <h4 class="mb-0 text-primary"><strong><c:out value="${day}" /></strong></h4>
                                <span class="badge badge-info"><c:out value="${weeklyGrid[day].size()}" /> slot(s)</span>
                            </div>

                            <c:choose>
                                <c:when test="${empty weeklyGrid[day]}">
                                    <span class="text-muted" style="font-size: 0.9rem;">No availability scheduled.</span>
                                </c:when>
                                <c:otherwise>
                                    <div class="d-flex flex-wrap gap-2 mt-2">
                                        <c:forEach var="slot" items="${weeklyGrid[day]}">
                                            <div class="p-2 border rounded d-flex align-center gap-2" style="background: var(--color-background);">
                                                <div>
                                                    <strong><c:out value="${slot.startTime}" /> - <c:out value="${slot.endTime}" /></strong>
                                                    <span class="badge badge-secondary ms-1" style="font-size: 0.75rem;"><c:out value="${slot.mode}" /></span>
                                                </div>
                                                <form action="${pageContext.request.contextPath}/availability" method="post" style="margin: 0;">
                                                    <input type="hidden" name="_csrf" value="<c:out value='${csrfToken}'/>" />
                                                    <input type="hidden" name="action" value="remove" />
                                                    <input type="hidden" name="id" value="<c:out value='${slot.id}'/>" />
                                                    <button type="submit" class="btn btn-danger btn-sm p-1" style="line-height: 1; font-size: 0.8rem;" title="Remove slot" onclick="return confirm('Remove slot?');">✕</button>
                                                </form>
                                            </div>
                                        </c:forEach>
                                    </div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </div>
</div>

<%@ include file="layout/footer.jspf" %>
