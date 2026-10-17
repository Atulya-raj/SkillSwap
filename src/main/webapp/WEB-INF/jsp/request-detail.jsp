<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Session Request #${sessionRequest.id} - SkillSwap" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>
<%@ include file="/WEB-INF/jsp/layout/nav.jspf" %>

<main class="container" style="max-width: 900px; margin: 2rem auto;">
    <div class="page-header" style="margin-bottom: 1.5rem;">
        <div>
            <h2>Session Details #<c:out value="${sessionRequest.id}"/></h2>
            <p class="text-muted">Lifecycle tracking, swap negotiation, and escrow settlement.</p>
        </div>
        <div style="display: flex; gap: 0.5rem;">
            <a href="${pageContext.request.contextPath}/requests" class="btn btn-outline">← Back to Requests</a>
        </div>
    </div>

    <!-- Status Header Card with Visual Stepper -->
    <div class="card" style="padding: 1.5rem; margin-bottom: 1.75rem; border-top: 5px solid ${sessionRequest.status == 'PENDING' ? '#F4A300' : (sessionRequest.status == 'ACCEPTED' ? '#1C7293' : (sessionRequest.status == 'COMPLETED' ? '#10B981' : '#64748b'))};">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 1rem;">
            <div>
                <span class="badge ${sessionRequest.status == 'PENDING' ? 'badge-warning' : (sessionRequest.status == 'ACCEPTED' ? 'badge-info' : (sessionRequest.status == 'COMPLETED' ? 'badge-success' : 'badge-secondary'))}"
                      style="font-size: 1rem; padding: 0.4rem 0.9rem;">
                    Status: <c:out value="${sessionRequest.status}"/>
                </span>
                <span style="font-weight: 700; color: var(--color-primary); font-size: 1.1rem; margin-left: 0.75rem;">
                    <c:out value="${sessionRequest.costCredits}"/> Credits in Escrow
                </span>
            </div>
            <div style="font-size: 0.88rem; color: #64748b;">
                Requested on: <c:out value="${sessionRequest.createdAt}"/>
            </div>
        </div>

        <!-- Visual Stepper -->
        <div style="display: flex; justify-content: space-between; margin-top: 1.75rem; position: relative;">
            <div style="flex: 1; text-align: center; position: relative;">
                <div style="width: 36px; height: 36px; border-radius: 50%; background: #10B981; color: white; display: flex; align-items: center; justify-content: center; margin: 0 auto 0.5rem auto; font-weight: bold;">
                    ✓
                </div>
                <div style="font-size: 0.85rem; font-weight: 600; color: #1e293b;">1. Requested</div>
                <div style="font-size: 0.75rem; color: #64748b;">Credits held</div>
            </div>

            <div style="flex: 1; text-align: center; position: relative;">
                <div style="width: 36px; height: 36px; border-radius: 50%; background: ${(sessionRequest.status == 'ACCEPTED' || sessionRequest.status == 'COMPLETED' || sessionRequest.status == 'NO_SHOW_LEARNER' || sessionRequest.status == 'NO_SHOW_PROVIDER') ? '#10B981' : (sessionRequest.status == 'PENDING' ? '#F4A300' : '#94a3b8')}; color: white; display: flex; align-items: center; justify-content: center; margin: 0 auto 0.5rem auto; font-weight: bold;">
                    <c:choose>
                        <c:when test="${sessionRequest.status == 'ACCEPTED' || sessionRequest.status == 'COMPLETED' || sessionRequest.status == 'NO_SHOW_LEARNER' || sessionRequest.status == 'NO_SHOW_PROVIDER'}">✓</c:when>
                        <c:when test="${sessionRequest.status == 'PENDING'}">2</c:when>
                        <c:otherwise>✕</c:otherwise>
                    </c:choose>
                </div>
                <div style="font-size: 0.85rem; font-weight: 600; color: #1e293b;">2. Accepted</div>
                <div style="font-size: 0.75rem; color: #64748b;">
                    <c:out value="${sessionRequest.respondedAt != null ? sessionRequest.respondedAt : 'Awaiting provider'}"/>
                </div>
            </div>

            <div style="flex: 1; text-align: center; position: relative;">
                <div style="width: 36px; height: 36px; border-radius: 50%; background: ${sessionRequest.status == 'COMPLETED' ? '#10B981' : ((sessionRequest.status == 'NO_SHOW_LEARNER' || sessionRequest.status == 'NO_SHOW_PROVIDER' || sessionRequest.status == 'CANCELLED' || sessionRequest.status == 'REJECTED' || sessionRequest.status == 'EXPIRED') ? '#ef4444' : '#cbd5e1')}; color: white; display: flex; align-items: center; justify-content: center; margin: 0 auto 0.5rem auto; font-weight: bold;">
                    <c:choose>
                        <c:when test="${sessionRequest.status == 'COMPLETED'}">✓</c:when>
                        <c:when test="${sessionRequest.status == 'NO_SHOW_LEARNER' || sessionRequest.status == 'NO_SHOW_PROVIDER' || sessionRequest.status == 'CANCELLED' || sessionRequest.status == 'REJECTED' || sessionRequest.status == 'EXPIRED'}">✕</c:when>
                        <c:otherwise>3</c:otherwise>
                    </c:choose>
                </div>
                <div style="font-size: 0.85rem; font-weight: 600; color: #1e293b;">3. Completed</div>
                <div style="font-size: 0.75rem; color: #64748b;">
                    <c:choose>
                        <c:when test="${sessionRequest.status == 'COMPLETED'}">Paid to provider</c:when>
                        <c:otherwise>Confirmation</c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>

    <!-- Participants Grid -->
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-bottom: 1.75rem;">
        <!-- Learner Card -->
        <div class="card" style="padding: 1.25rem;">
            <div style="font-size: 0.8rem; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 0.5rem;">
                Learner (Student)
            </div>
            <div style="display: flex; gap: 1rem; align-items: center;">
                <img src="${pageContext.request.contextPath}/avatar?id=${learner.id}"
                     alt="<c:out value='${learner.fullName}'/>"
                     style="width: 52px; height: 52px; border-radius: 50%; object-fit: cover; border: 2px solid var(--color-teal);" />
                <div>
                    <h4 style="margin: 0;">
                        <a href="${pageContext.request.contextPath}/users/view?id=${learner.id}" style="color: var(--color-primary); text-decoration: none;">
                            <c:out value="${learner.fullName}"/>
                        </a>
                    </h4>
                    <div style="font-size: 0.85rem; color: #64748b;">
                        <c:out value="${learner.department}"/> • Year <c:out value="${learner.studyYear}"/>
                    </div>
                </div>
            </div>
            <div style="margin-top: 0.75rem; font-size: 0.85rem;">
                <strong>Confirmation Status:</strong>
                <c:choose>
                    <c:when test="${sessionRequest.learnerConfirmed}">
                        <span style="color: #16a34a; font-weight: 600;">✅ Confirmed</span>
                    </c:when>
                    <c:otherwise>
                        <span style="color: #94a3b8;">⏳ Pending confirmation</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Provider Card -->
        <div class="card" style="padding: 1.25rem;">
            <div style="font-size: 0.8rem; font-weight: 700; color: #64748b; text-transform: uppercase; margin-bottom: 0.5rem;">
                Provider (Tutor)
            </div>
            <div style="display: flex; gap: 1rem; align-items: center;">
                <img src="${pageContext.request.contextPath}/avatar?id=${provider.id}"
                     alt="<c:out value='${provider.fullName}'/>"
                     style="width: 52px; height: 52px; border-radius: 50%; object-fit: cover; border: 2px solid var(--color-primary);" />
                <div>
                    <h4 style="margin: 0;">
                        <a href="${pageContext.request.contextPath}/users/view?id=${provider.id}" style="color: var(--color-primary); text-decoration: none;">
                            <c:out value="${provider.fullName}"/>
                        </a>
                    </h4>
                    <div style="font-size: 0.85rem; color: #64748b;">
                        <c:out value="${provider.department}"/> • Year <c:out value="${provider.studyYear}"/>
                    </div>
                </div>
            </div>
            <div style="margin-top: 0.75rem; font-size: 0.85rem;">
                <strong>Confirmation Status:</strong>
                <c:choose>
                    <c:when test="${sessionRequest.providerConfirmed}">
                        <span style="color: #16a34a; font-weight: 600;">✅ Confirmed</span>
                    </c:when>
                    <c:otherwise>
                        <span style="color: #94a3b8;">⏳ Pending confirmation</span>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>

    <!-- Appointment & Logistics Details -->
    <div class="card" style="padding: 1.5rem; margin-bottom: 1.75rem;">
        <h3 style="margin-top: 0; color: var(--color-primary); font-size: 1.15rem; border-bottom: 1px solid #e2e8f0; padding-bottom: 0.5rem;">
            Appointment Details
        </h3>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; margin-top: 1rem;">
            <div>
                <span class="text-muted" style="display: block; font-size: 0.85rem;">Skill:</span>
                <span style="font-weight: 600; font-size: 1rem; color: var(--color-primary);"><c:out value="${sessionRequest.skillName}"/></span>
            </div>
            <div>
                <span class="text-muted" style="display: block; font-size: 0.85rem;">Scheduled Date & Time:</span>
                <span style="font-weight: 600; font-size: 1rem;">📅 <c:out value="${sessionRequest.proposedStart}"/></span>
            </div>
            <div>
                <span class="text-muted" style="display: block; font-size: 0.85rem;">Duration:</span>
                <span style="font-weight: 600;">⏱️ <c:out value="${sessionRequest.durationMinutes}"/> Minutes</span>
            </div>
            <div>
                <span class="text-muted" style="display: block; font-size: 0.85rem;">Mode:</span>
                <span style="font-weight: 600;">
                    <c:choose>
                        <c:when test="${sessionRequest.mode == 'ONLINE'}">🌐 Online Video Swap</c:when>
                        <c:otherwise>📍 In-Person Campus Meeting</c:otherwise>
                    </c:choose>
                </span>
            </div>
        </div>

        <div style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid #f1f5f9;">
            <span class="text-muted" style="display: block; font-size: 0.85rem; margin-bottom: 0.25rem;">Meeting Location / Link:</span>
            <div style="background: #f8fafc; padding: 0.75rem; border-radius: 6px; font-weight: 500;">
                <c:choose>
                    <c:when test="${sessionRequest.meetingDetails.startsWith('http://') || sessionRequest.meetingDetails.startsWith('https://')}">
                        <a href="<c:out value='${sessionRequest.meetingDetails}'/>" target="_blank" rel="noopener noreferrer" style="color: var(--color-teal); word-break: break-all;">
                            🔗 <c:out value="${sessionRequest.meetingDetails}"/> (Open Link)
                        </a>
                    </c:when>
                    <c:otherwise>
                        📍 <c:out value="${sessionRequest.meetingDetails}"/>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <c:if test="${not empty sessionRequest.message}">
            <div style="margin-top: 1rem;">
                <span class="text-muted" style="display: block; font-size: 0.85rem; margin-bottom: 0.25rem;">Learner's Note:</span>
                <div style="background: #fffbeb; border-left: 3px solid #f59e0b; padding: 0.75rem; border-radius: 4px; font-size: 0.9rem; color: #92400e;">
                    <c:out value="${sessionRequest.message}"/>
                </div>
            </div>
        </c:if>
    </div>

    <!-- Actions Panel (strictly visible based on user role and state) -->
    <div class="card" style="padding: 1.5rem; background: #fdfdfd; border: 1px solid #cbd5e1;">
        <h3 style="margin-top: 0; font-size: 1.15rem; color: #1e293b;">Available Actions</h3>

        <div style="display: flex; gap: 1rem; flex-wrap: wrap; align-items: center; margin-top: 1rem;">
            <!-- PENDING actions -->
            <c:if test="${sessionRequest.status == 'PENDING'}">
                <c:if test="${isProvider}">
                    <!-- Provider Accept -->
                    <form method="POST" action="${pageContext.request.contextPath}/requests/respond" style="margin: 0;">
                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                        <input type="hidden" name="id" value="${sessionRequest.id}"/>
                        <input type="hidden" name="decision" value="accept"/>
                        <button type="submit" class="btn btn-primary" style="background-color: #10B981; border-color: #10B981;">
                            ✓ Accept Request
                        </button>
                    </form>
                    <!-- Provider Reject -->
                    <form method="POST" action="${pageContext.request.contextPath}/requests/respond" style="margin: 0;">
                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                        <input type="hidden" name="id" value="${sessionRequest.id}"/>
                        <input type="hidden" name="decision" value="reject"/>
                        <button type="submit" class="btn btn-outline" style="color: #dc2626; border-color: #dc2626;">
                            ✕ Reject Request (Release Escrow)
                        </button>
                    </form>
                </c:if>

                <c:if test="${isLearner}">
                    <!-- Learner Cancel Pending -->
                    <form method="POST" action="${pageContext.request.contextPath}/cancel" style="margin: 0;">
                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                        <input type="hidden" name="id" value="${sessionRequest.id}"/>
                        <button type="submit" class="btn btn-outline" style="color: #dc2626; border-color: #dc2626;">
                            Cancel Request (Full Refund)
                        </button>
                    </form>
                </c:if>
            </c:if>

            <!-- ACCEPTED actions -->
            <c:if test="${sessionRequest.status == 'ACCEPTED'}">
                <!-- Confirm Completion (Both sides) -->
                <c:choose>
                    <c:when test="${(isLearner and sessionRequest.learnerConfirmed) or (isProvider and sessionRequest.providerConfirmed)}">
                        <span class="badge badge-success" style="padding: 0.6rem 1rem; font-size: 0.9rem;">
                            ✓ You have confirmed completion. Waiting for your partner.
                        </span>
                    </c:when>
                    <c:otherwise>
                        <form method="POST" action="${pageContext.request.contextPath}/confirm" style="margin: 0;">
                            <input type="hidden" name="_csrf" value="${csrfToken}"/>
                            <input type="hidden" name="id" value="${sessionRequest.id}"/>
                            <button type="submit" class="btn btn-primary" style="background-color: #10B981; border-color: #10B981;">
                                ⭐ Confirm Session Completion
                            </button>
                        </form>
                    </c:otherwise>
                </c:choose>

                <!-- Cancel Accepted Session -->
                <form method="POST" action="${pageContext.request.contextPath}/cancel" style="margin: 0;">
                    <input type="hidden" name="_csrf" value="${csrfToken}"/>
                    <input type="hidden" name="id" value="${sessionRequest.id}"/>
                    <button type="submit" class="btn btn-outline" style="color: #b91c1c; border-color: #f87171;"
                            onclick="return confirm('Are you sure you want to cancel this session? If cancelled less than 2 hours before start, the late cancellation policy applies.');">
                        Cancel Session
                    </button>
                </form>

                <!-- Report No-Show -->
                <form method="POST" action="${pageContext.request.contextPath}/noshow" style="margin: 0;">
                    <input type="hidden" name="_csrf" value="${csrfToken}"/>
                    <input type="hidden" name="id" value="${sessionRequest.id}"/>
                    <button type="submit" class="btn btn-outline" style="color: #e11d48; border-color: #fda4af;"
                            onclick="return confirm('Report partner as no-show? Only valid 15+ minutes after start.');">
                        ⚠️ Report No-Show
                    </button>
                </form>
            </c:if>

            <!-- In-App Messaging Thread (available for active/completed sessions) -->
            <a href="${pageContext.request.contextPath}/messages?requestId=${sessionRequest.id}" class="btn btn-outline" id="btn-open-messages">
                💬 Open Messages
            </a>
        </div>
    </div>

    <!-- Review Section for COMPLETED Sessions -->
    <c:if test="${sessionRequest.status == 'COMPLETED'}">
        <div class="card" style="padding: 1.5rem; margin-top: 1.5rem; border: 1px solid #e2e8f0; background: #fafafa;">
            <h3 style="margin-top: 0; color: var(--color-primary); font-size: 1.15rem;">
                Session Review & Rating
            </h3>
            <c:choose>
                <c:when test="${hasReviewed}">
                    <div style="color: #16a34a; font-weight: 600; display: flex; align-items: center; gap: 0.5rem; padding: 0.5rem 0;">
                        <span>✅</span> You have already submitted your review for this session. Thank you for your feedback!
                    </div>
                </c:when>
                <c:otherwise>
                    <p class="text-muted" style="margin-bottom: 1rem; font-size: 0.95rem;">
                        How was your skill swap session with
                        <strong><c:out value="${isLearner ? provider.fullName : learner.fullName}"/></strong>?
                        Leave a rating and helpful feedback to help fellow students and unlock badges.
                    </p>
                    <form method="POST" action="${pageContext.request.contextPath}/reviews/new">
                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                        <input type="hidden" name="sessionId" value="${sessionRequest.id}"/>

                        <div class="form-group" style="margin-bottom: 1rem;">
                            <label for="ratingSelect" style="display: block; font-weight: 600; margin-bottom: 0.35rem;">Rating (Stars):</label>
                            <select id="ratingSelect" name="rating" class="form-control" style="max-width: 260px;" required>
                                <option value="5">⭐⭐⭐⭐⭐ (5 - Outstanding)</option>
                                <option value="4">⭐⭐⭐⭐ (4 - Very Good)</option>
                                <option value="3">⭐⭐⭐ (3 - Average)</option>
                                <option value="2">⭐⭐ (2 - Below Average)</option>
                                <option value="1">⭐ (1 - Unsatisfactory)</option>
                            </select>
                        </div>

                        <div class="form-group" style="margin-bottom: 1.25rem;">
                            <label for="reviewComment" style="display: block; font-weight: 600; margin-bottom: 0.35rem;">Review & Feedback (Optional):</label>
                            <textarea id="reviewComment" name="comment" class="form-control" rows="3" maxlength="500" placeholder="Share your experience (e.g. clarity, patience, teaching effectiveness)..."></textarea>
                        </div>

                        <button type="submit" class="btn btn-primary" id="btn-submit-review">⭐ Submit Review</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>
</main>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
