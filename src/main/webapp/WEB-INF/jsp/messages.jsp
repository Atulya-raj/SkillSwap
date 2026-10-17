<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="Messages - SkillSwap" />
<%@ include file="/WEB-INF/jsp/layout/header.jspf" %>
<%@ include file="/WEB-INF/jsp/layout/nav.jspf" %>

<main class="container">
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
        <div>
            <h2>In-App Messaging</h2>
            <p class="text-muted">Coordinate swap details, share meeting links, and ask questions before your session.</p>
        </div>
        <c:if test="${hasActiveThread}">
            <div>
                <a href="${pageContext.request.contextPath}/messages" class="btn btn-outline">« All Conversations</a>
                <a href="${pageContext.request.contextPath}/requests/view?id=${sessionRequest.id}" class="btn btn-secondary" style="margin-left: 0.5rem;">View Session Details</a>
            </div>
        </c:if>
    </div>

    <c:choose>
        <c:when test="${hasActiveThread}">
            <!-- Active Chat Thread View -->
            <div class="card" style="padding: 0; overflow: hidden; border: 1px solid #cbd5e1; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);">
                <!-- Chat Header -->
                <div style="padding: 1rem 1.5rem; background: #f8fafc; border-bottom: 1px solid #e2e8f0; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem;">
                    <div style="display: flex; align-items: center; gap: 1rem;">
                        <img src="${pageContext.request.contextPath}/avatar?id=${partner.id}"
                             alt="<c:out value='${partner.fullName}'/>"
                             style="width: 44px; height: 44px; border-radius: 50%; object-fit: cover; border: 2px solid var(--color-primary);" />
                        <div>
                            <h4 style="margin: 0; font-size: 1.1rem; color: #1e293b;">
                                <c:out value="${partner.fullName}"/>
                            </h4>
                            <div style="font-size: 0.85rem; color: #64748b;">
                                Session #<c:out value="${sessionRequest.id}"/> • <c:out value="${sessionRequest.skillName}"/> •
                                <span class="badge badge-secondary" style="font-size: 0.75rem;"><c:out value="${sessionRequest.status}"/></span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Messages Stream -->
                <div style="padding: 1.5rem; min-height: 340px; max-height: 520px; overflow-y: auto; background: #ffffff; display: flex; flex-direction: column; gap: 1rem;">
                    <c:choose>
                        <c:when test="${empty messages}">
                            <div style="text-align: center; color: #94a3b8; margin: auto; padding: 2rem 0;">
                                <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">💬</div>
                                <p style="margin: 0; font-weight: 500;">No messages in this conversation yet.</p>
                                <p style="font-size: 0.85rem; margin-top: 0.25rem;">Send a quick greeting or meeting details below to get started!</p>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <c:forEach var="msg" items="${messages}">
                                <c:set var="isMe" value="${msg.senderId == sessionScope.currentUser.id}"/>
                                <div style="display: flex; flex-direction: column; align-items: ${isMe ? 'flex-end' : 'flex-start'};">
                                    <div style="max-width: 70%; padding: 0.75rem 1.15rem; border-radius: ${isMe ? '18px 18px 4px 18px' : '18px 18px 18px 4px'}; background: ${isMe ? 'var(--color-primary)' : '#f1f5f9'}; color: ${isMe ? '#ffffff' : '#1e293b'}; font-size: 0.95rem; line-height: 1.45; word-break: break-word;">
                                        <c:if test="${!isMe}">
                                            <div style="font-size: 0.75rem; font-weight: 700; color: #0284c7; margin-bottom: 0.2rem;">
                                                <c:out value="${msg.senderName}"/>
                                            </div>
                                        </c:if>
                                        <c:out value="${msg.body}"/>
                                    </div>
                                    <span style="font-size: 0.72rem; color: #94a3b8; margin-top: 0.25rem; padding: 0 0.5rem;">
                                        ${msg.createdAt}
                                    </span>
                                </div>
                            </c:forEach>
                        </c:otherwise>
                    </c:choose>
                </div>

                <!-- Message Input Form -->
                <div style="padding: 1rem 1.5rem; background: #f8fafc; border-top: 1px solid #e2e8f0;">
                    <form method="POST" action="${pageContext.request.contextPath}/messages" style="display: flex; gap: 0.75rem; align-items: flex-end;">
                        <input type="hidden" name="_csrf" value="${csrfToken}"/>
                        <input type="hidden" name="requestId" value="${sessionRequest.id}"/>
                        <div style="flex: 1;">
                            <textarea name="body" class="form-control" rows="2" placeholder="Type your message here (e.g. sharing meeting links, timing adjustments)..." required maxlength="2000" style="resize: none;"></textarea>
                        </div>
                        <button type="submit" class="btn btn-primary" id="btn-send-message" style="height: 48px; padding: 0 1.5rem;">
                            Send ✈
                        </button>
                    </form>
                </div>
            </div>
        </c:when>
        <c:otherwise>
            <!-- All Conversations List View -->
            <c:choose>
                <c:when test="${empty conversations}">
                    <div class="card" style="text-align: center; padding: 3rem 1.5rem;">
                        <div style="font-size: 3rem; margin-bottom: 1rem;">💬</div>
                        <h3>No active message threads</h3>
                        <p class="text-muted" style="max-width: 480px; margin: 0 auto 1.5rem;">
                            Session chat threads are automatically created when you request or accept a skill swap session.
                        </p>
                        <div>
                            <a href="${pageContext.request.contextPath}/browse" class="btn btn-primary">Find a Mentor</a>
                            <a href="${pageContext.request.contextPath}/requests" class="btn btn-outline" style="margin-left: 0.5rem;">View Requests</a>
                        </div>
                    </div>
                </c:when>
                <c:otherwise>
                    <div style="display: flex; flex-direction: column; gap: 0.75rem;">
                        <c:forEach var="c" items="${conversations}">
                            <div class="card" style="padding: 1.25rem; display: flex; justify-content: space-between; align-items: center; gap: 1rem; flex-wrap: wrap;">
                                <div>
                                    <div style="display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.35rem;">
                                        <span class="badge badge-primary">Session #<c:out value="${c.id}"/></span>
                                        <h4 style="margin: 0; font-size: 1.05rem; color: #1e293b;">
                                            <c:out value="${c.skillName}"/>
                                        </h4>
                                        <span class="badge badge-secondary"><c:out value="${c.status}"/></span>
                                    </div>
                                    <div style="font-size: 0.88rem; color: #64748b;">
                                        Learner: <strong><c:out value="${c.learnerName}"/></strong> • Provider: <strong><c:out value="${c.providerName}"/></strong>
                                    </div>
                                    <div style="font-size: 0.8rem; color: #94a3b8; margin-top: 0.25rem;">
                                        Scheduled: <c:out value="${c.proposedStart}"/>
                                    </div>
                                </div>
                                <div>
                                    <a href="${pageContext.request.contextPath}/messages?requestId=${c.id}" class="btn btn-primary btn-sm">
                                        Open Chat →
                                    </a>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </c:otherwise>
    </c:choose>
</main>

<%@ include file="/WEB-INF/jsp/layout/footer.jspf" %>
