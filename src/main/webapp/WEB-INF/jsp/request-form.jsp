<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Request Session - SkillSwap" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>
<%@ include file="/WEB-INF/jsp/layout/nav.jspf" %>

<main class="container" style="max-width: 860px; margin: 2rem auto;">
    <div class="page-header" style="margin-bottom: 1.5rem;">
        <div>
            <h2>Request a SkillSwap Session</h2>
            <p class="text-muted">Schedule learning time with your peer tutor. Credits are held in escrow until completion.</p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/browse" class="btn btn-outline">← Back to Browse</a>
        </div>
    </div>

    <!-- Provider and Skill Summary Card -->
    <div class="card" style="margin-bottom: 2rem; padding: 1.5rem; display: flex; gap: 1.5rem; align-items: center; background: linear-gradient(135deg, #f8fafc 0%, #edf2f7 100%);">
        <div style="flex-shrink: 0;">
            <img src="${pageContext.request.contextPath}/avatar?id=${provider.id}"
                 alt="<c:out value='${provider.fullName}'/>"
                 style="width: 72px; height: 72px; border-radius: 50%; object-fit: cover; border: 3px solid var(--color-primary);" />
        </div>
        <div style="flex-grow: 1;">
            <div style="display: flex; justify-content: space-between; align-items: flex-start; flex-wrap: wrap;">
                <div>
                    <h3 style="margin: 0; color: var(--color-primary); font-size: 1.3rem;">
                        <c:out value="${listing.skillName}"/>
                    </h3>
                    <div style="font-size: 0.95rem; color: #475569; margin-top: 0.25rem;">
                        Taught by <strong><c:out value="${provider.fullName}"/></strong>
                        <c:if test="${not empty provider.department}">
                            • <c:out value="${provider.department}"/> (Year <c:out value="${provider.studyYear}"/>)
                        </c:if>
                    </div>
                </div>
                <div style="text-align: right;">
                    <span class="badge badge-primary" style="font-size: 0.9rem; padding: 0.35rem 0.75rem;">
                        <c:out value="${listing.level}"/>
                    </span>
                    <div style="font-weight: 700; color: var(--color-teal); margin-top: 0.3rem;">
                        <c:out value="${listing.creditsPerHour}"/> Credits / Hour
                    </div>
                </div>
            </div>

            <c:if test="${not empty listing.description}">
                <p style="margin: 0.75rem 0 0 0; font-size: 0.9rem; color: #334155;">
                    <c:out value="${listing.description}"/>
                </p>
            </c:if>
        </div>
    </div>

    <!-- Provider's Weekly Availability Card -->
    <c:if test="${not empty slots}">
        <div class="card" style="margin-bottom: 2rem; padding: 1.25rem; background: #faf5ff; border: 1px solid #e9d5ff;">
            <h4 style="margin: 0 0 0.5rem 0; color: #6b21a8; font-size: 1rem;">🗓️ Provider's Typical Weekly Availability:</h4>
            <div style="display: flex; flex-wrap: wrap; gap: 0.5rem;">
                <c:forEach var="slot" items="${slots}">
                    <span style="background: white; border: 1px solid #d8b4fe; border-radius: 6px; padding: 0.3rem 0.6rem; font-size: 0.85rem; color: #581c87;">
                        <strong>Day ${slot.dayOfWeek}:</strong> ${slot.startTime} - ${slot.endTime}
                    </span>
                </c:forEach>
            </div>
            <p style="margin: 0.5rem 0 0 0; font-size: 0.82rem; color: #7e22ce;">
                *Please propose a start time that respects your tutor's schedule.
            </p>
        </div>
    </c:if>

    <!-- Restored Draft Banner -->
    <c:if test="${not empty draft}">
        <div class="alert alert-info" style="margin-bottom: 1.5rem; display: flex; justify-content: space-between; align-items: center;">
            <div>
                📝 <strong>Draft Restored:</strong> You have an unsaved draft from <c:out value="${draft.savedAt}"/>.
            </div>
            <form method="POST" action="${pageContext.request.contextPath}/requests/new" style="margin: 0;">
                <input type="hidden" name="_csrf" value="${csrfToken}"/>
                <input type="hidden" name="listingId" value="${listing.id}"/>
                <input type="hidden" name="action" value="discard_draft"/>
                <button type="submit" class="btn btn-outline" style="font-size: 0.82rem; padding: 0.25rem 0.6rem; color: #b91c1c; border-color: #f87171;">
                    Discard Draft
                </button>
            </form>
        </div>
    </c:if>

    <!-- Main Request Form -->
    <div class="card" style="padding: 2rem;">
        <form method="POST" action="${pageContext.request.contextPath}/requests/new">
            <input type="hidden" name="_csrf" value="${csrfToken}"/>
            <input type="hidden" name="listingId" value="${listing.id}"/>

            <div class="form-group" style="margin-bottom: 1.25rem;">
                <label for="proposedStart" style="display: block; font-weight: 600; margin-bottom: 0.5rem;">
                    Proposed Date & Time <span style="color: #e11d48;">*</span>
                </label>
                <input type="datetime-local" id="proposedStart" name="proposedStart" class="form-control"
                       value="<c:out value='${not empty draft ? draft.proposedStart : ""}'/>"
                       required style="width: 100%; max-width: 340px;" />
                <small class="text-muted" style="display: block; margin-top: 0.35rem;">
                    Must be scheduled at least 2 hours in advance and within the next 30 days.
                </small>
            </div>

            <div class="form-row" style="display: flex; gap: 1.5rem; flex-wrap: wrap; margin-bottom: 1.25rem;">
                <div class="form-group" style="flex: 1; min-width: 200px;">
                    <label for="durationMinutes" style="display: block; font-weight: 600; margin-bottom: 0.5rem;">
                        Duration <span style="color: #e11d48;">*</span>
                    </label>
                    <select id="durationMinutes" name="durationMinutes" class="form-control" style="width: 100%;">
                        <option value="30" ${(not empty draft and draft.durationMinutes == 30) ? 'selected' : ''}>30 Minutes</option>
                        <option value="60" ${(empty draft or draft.durationMinutes == 60) ? 'selected' : ''}>60 Minutes (1 Hour)</option>
                        <option value="90" ${(not empty draft and draft.durationMinutes == 90) ? 'selected' : ''}>90 Minutes (1.5 Hours)</option>
                        <option value="120" ${(not empty draft and draft.durationMinutes == 120) ? 'selected' : ''}>120 Minutes (2 Hours)</option>
                    </select>
                </div>

                <div class="form-group" style="flex: 1; min-width: 200px;">
                    <label for="mode" style="display: block; font-weight: 600; margin-bottom: 0.5rem;">
                        Session Mode <span style="color: #e11d48;">*</span>
                    </label>
                    <select id="mode" name="mode" class="form-control" style="width: 100%;">
                        <option value="ONLINE" ${(empty draft or draft.mode == 'ONLINE') ? 'selected' : ''}>🌐 Online (Video / Link)</option>
                        <option value="IN_PERSON" ${(not empty draft and draft.mode == 'IN_PERSON') ? 'selected' : ''}>📍 In-Person (Campus Location)</option>
                    </select>
                </div>
            </div>

            <div class="form-group" style="margin-bottom: 1.25rem;">
                <label for="meetingDetails" style="display: block; font-weight: 600; margin-bottom: 0.5rem;">
                    Meeting Details (Link or Location) <span style="color: #e11d48;">*</span>
                </label>
                <input type="text" id="meetingDetails" name="meetingDetails" class="form-control"
                       placeholder="e.g. Google Meet link, or Campus Library Room 204"
                       value="<c:out value='${not empty draft ? draft.meetingDetails : ""}'/>"
                       maxlength="255" required style="width: 100%;" />
            </div>

            <div class="form-group" style="margin-bottom: 1.75rem;">
                <label for="message" style="display: block; font-weight: 600; margin-bottom: 0.5rem;">
                    Message to Provider (Optional)
                </label>
                <textarea id="message" name="message" class="form-control" rows="4" maxlength="500"
                          placeholder="Tell your tutor what specific topics, assignments, or goals you want to cover..."
                          style="width: 100%;"><c:out value='${not empty draft ? draft.message : ""}'/></textarea>
                <small class="text-muted" style="display: block; margin-top: 0.35rem;">Max 500 characters.</small>
            </div>

            <!-- Escrow Notice -->
            <div style="background: #f1f5f9; border-left: 4px solid var(--color-teal); padding: 1rem; border-radius: 4px; margin-bottom: 1.75rem; font-size: 0.9rem; color: #334155;">
                💡 <strong>Escrow & Refund Protection:</strong> The calculated credits will be held from your balance immediately when you submit. If the provider rejects, the request expires, or is cancelled 2+ hours prior, credits are refunded in full.
            </div>

            <!-- Submission Actions -->
            <div style="display: flex; gap: 1rem; align-items: center; flex-wrap: wrap;">
                <button type="submit" name="action" value="submit" class="btn btn-primary" style="padding: 0.75rem 1.75rem; font-size: 1rem;">
                    🚀 Send Session Request
                </button>
                <button type="submit" name="action" value="draft" class="btn btn-secondary" style="padding: 0.75rem 1.5rem;">
                    💾 Save as Draft
                </button>
                <a href="${pageContext.request.contextPath}/browse" class="btn btn-outline" style="padding: 0.75rem 1.5rem; text-decoration: none;">
                    Cancel
                </a>
            </div>
        </form>
    </div>
</main>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
