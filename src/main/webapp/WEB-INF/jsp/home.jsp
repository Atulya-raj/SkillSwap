<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle" value="SkillSwap - Learn Anything, Teach What You Know" />
<%@ include file="layout/header.jspf" %>
<%@ include file="layout/nav.jspf" %>

<main class="main-content">
    <div class="container">
        <section class="hero">
            <div class="container">
                <h1>Share Knowledge.<br>Earn Campus Credits.</h1>
                <p>
                    SkillSwap is a student-driven peer learning network. Teach your favorite subjects—from Python and Data Structures to Guitar and Graphic Design—and use your earned credits to master new skills from fellow peers.
                </p>
                <div class="hero-actions">
                    <c:choose>
                        <c:when test="${empty sessionScope.currentUser}">
                            <a href="${pageContext.request.contextPath}/register" class="btn btn-accent btn-lg" id="btn-hero-register">Get Started (10 Free Credits)</a>
                            <a href="${pageContext.request.contextPath}/browse" class="btn btn-outline btn-lg" style="color:#FFFFFF;border-color:rgba(255,255,255,0.4);" id="btn-hero-browse">Explore Skills</a>
                        </c:when>
                        <c:otherwise>
                            <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-accent btn-lg" id="btn-hero-dashboard">Go to Dashboard</a>
                            <a href="${pageContext.request.contextPath}/browse" class="btn btn-outline btn-lg" style="color:#FFFFFF;border-color:rgba(255,255,255,0.4);" id="btn-hero-browse">Browse Listings</a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </section>

        <section class="grid grid-3" style="margin-bottom: 3rem;">
            <div class="card">
                <h3 style="color:var(--color-primary);margin-bottom:0.75rem;">1. Offer What You Know</h3>
                <p style="color:var(--color-text-muted);">
                    List your skills in programming, design, languages, academics, or music. Set your hourly rate between 1 and 5 credits and your weekly availability.
                </p>
            </div>
            <div class="card">
                <h3 style="color:var(--color-secondary);margin-bottom:0.75rem;">2. Connect &amp; Swap</h3>
                <p style="color:var(--color-text-muted);">
                    Discover fellow students using our multi-criteria matching engine. Send session proposals, coordinate online or in-person, and learn 1-on-1.
                </p>
            </div>
            <div class="card">
                <h3 style="color:var(--color-accent);margin-bottom:0.75rem;">3. Build Your Reputation</h3>
                <p style="color:var(--color-text-muted);">
                    Earn peer reviews and unlock milestone achievement badges like <em>Mentor</em>, <em>Eternal Learner</em>, and <em>Connector</em>.
                </p>
            </div>
        </section>

        <section class="card" style="text-align: center; padding: 2.5rem 1.5rem; background: #FFFFFF;">
            <h2 style="margin-bottom: 0.5rem;">Ready to upgrade your skillset?</h2>
            <p style="color: var(--color-text-muted); max-width: 550px; margin: 0 auto 1.5rem auto;">
                Join hundreds of students collaborating and growing together without spending real money.
            </p>
            <a href="${pageContext.request.contextPath}/register" class="btn btn-primary btn-lg" id="btn-cta-register">Create Your Free Account</a>
        </section>
    </div>
</main>

<%@ include file="layout/footer.jspf" %>
