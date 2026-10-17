<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle" value="Manage Catalog — SkillSwap Admin" />
<%@ include file="../layout/header.jspf" %>
<%@ include file="../layout/nav.jspf" %>

<main class="container py-4">
    <!-- Header -->
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; flex-wrap: wrap; gap: 1rem;">
        <div>
            <span class="badge badge-primary" style="margin-bottom: 0.35rem;">Catalog Management</span>
            <h1 style="margin: 0; font-size: 1.85rem; color: var(--color-primary-dark);">Skills & Categories Catalog</h1>
            <p style="color: var(--color-text-muted); font-size: 0.9rem; margin: 0.25rem 0 0 0;">
                Configure campus disciplines, add skills, or perform bulk CSV imports.
            </p>
        </div>
        <div>
            <a href="${pageContext.request.contextPath}/admin" class="btn btn-outline btn-sm">← Back to Analytics</a>
        </div>
    </div>

    <!-- CSV Import Warnings Alert (if any from recent import) -->
    <c:if test="${not empty importWarnings}">
        <div class="alert alert-warning mb-4" id="alert-import-warnings">
            <h3 style="font-size: 1rem; margin: 0 0 0.5rem 0;">⚠️ CSV Import Notice</h3>
            <p style="font-size: 0.85rem; margin: 0 0 0.5rem 0;">Some rows were skipped during the last CSV import:</p>
            <ul style="margin: 0; padding-left: 1.25rem; font-size: 0.82rem;">
                <c:forEach var="w" items="${importWarnings}">
                    <li><c:out value="${w}" /></li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

    <!-- Top Action Cards: Add Category, Add Skill, Bulk CSV Import -->
    <div class="grid-3 mb-4">
        <!-- Add Category Form -->
        <div class="card" id="card-add-category">
            <h2 style="font-size: 1.1rem; margin-top: 0; margin-bottom: 0.75rem;">📁 Add Category</h2>
            <form action="${pageContext.request.contextPath}/admin/catalog/category/add" method="post">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <div class="form-group mb-2">
                    <label class="form-label" style="font-size: 0.82rem;">Category Name *</label>
                    <input type="text" name="name" required class="form-control" placeholder="e.g. Data Science">
                </div>
                <div class="form-group mb-3">
                    <label class="form-label" style="font-size: 0.82rem;">Description</label>
                    <input type="text" name="description" class="form-control" placeholder="Short description">
                </div>
                <button type="submit" class="btn btn-primary btn-sm" style="width: 100%;">Create Category</button>
            </form>
        </div>

        <!-- Add Skill Form -->
        <div class="card" id="card-add-skill">
            <h2 style="font-size: 1.1rem; margin-top: 0; margin-bottom: 0.75rem;">💡 Add Skill</h2>
            <form action="${pageContext.request.contextPath}/admin/catalog/skill/add" method="post">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <div class="form-group mb-2">
                    <label class="form-label" style="font-size: 0.82rem;">Parent Category *</label>
                    <select name="categoryId" required class="form-control">
                        <c:forEach var="cat" items="${categories}">
                            <option value="${cat.id}"><c:out value="${cat.name}" /></option>
                        </c:forEach>
                    </select>
                </div>
                <div class="form-group mb-2">
                    <label class="form-label" style="font-size: 0.82rem;">Skill Name *</label>
                    <input type="text" name="name" required class="form-control" placeholder="e.g. Docker & Kubernetes">
                </div>
                <div class="form-group mb-3">
                    <label class="form-label" style="font-size: 0.82rem;">Description</label>
                    <input type="text" name="description" class="form-control" placeholder="Optional description">
                </div>
                <button type="submit" class="btn btn-primary btn-sm" style="width: 100%;">Create Skill</button>
            </form>
        </div>

        <!-- Bulk CSV Import Form -->
        <div class="card" id="card-bulk-import">
            <h2 style="font-size: 1.1rem; margin-top: 0; margin-bottom: 0.5rem;">📥 Bulk CSV Import</h2>
            <p style="font-size: 0.8rem; color: var(--color-text-muted); margin-bottom: 0.75rem;">
                Upload a CSV file containing <code>category,skill</code> lines. Invalid rows are safely reported.
            </p>
            <form action="${pageContext.request.contextPath}/admin/catalog/import" method="post" enctype="multipart/form-data">
                <input type="hidden" name="_csrf" value="${csrfToken}">
                <div class="form-group mb-3">
                    <label class="form-label" style="font-size: 0.82rem;">Select CSV File (.csv) *</label>
                    <input type="file" name="file" accept=".csv,text/csv" required class="form-control" style="font-size: 0.82rem;">
                </div>
                <button type="submit" class="btn btn-accent btn-sm" style="width: 100%;" id="btn-submit-csv-import">
                    Upload & Import CSV
                </button>
            </form>
        </div>
    </div>

    <!-- Catalog Explorer: Categories and Skills Tree -->
    <div class="card">
        <h2 style="font-size: 1.25rem; margin-top: 0; margin-bottom: 1rem;">🗂️ Active Categories & Skills</h2>

        <div style="display: flex; flex-direction: column; gap: 1.25rem;">
            <c:forEach var="cat" items="${categories}">
                <div style="border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 1rem; background-color: var(--color-surface);">
                    <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); padding-bottom: 0.5rem; margin-bottom: 0.75rem; flex-wrap: wrap; gap: 0.5rem;">
                        <div>
                            <span style="font-size: 1.1rem; font-weight: 700; color: var(--color-primary-dark);">
                                📁 <c:out value="${cat.name}" />
                            </span>
                            <c:if test="${not empty cat.description}">
                                <span style="font-size: 0.83rem; color: var(--color-text-muted); margin-left: 0.5rem;">
                                    — <c:out value="${cat.description}" />
                                </span>
                            </c:if>
                        </div>
                        <div>
                            <!-- Delete Category Form -->
                            <form action="${pageContext.request.contextPath}/admin/catalog/category/delete" method="post" onsubmit="return confirm('Delete category \'${cat.name}\' and all associated skills?');" style="margin: 0;">
                                <input type="hidden" name="_csrf" value="${csrfToken}">
                                <input type="hidden" name="id" value="${cat.id}">
                                <button type="submit" class="btn btn-danger btn-sm" style="padding: 0.15rem 0.5rem; font-size: 0.75rem;">
                                    Delete Category
                                </button>
                            </form>
                        </div>
                    </div>

                    <!-- Skills in this Category -->
                    <c:set var="catSkills" value="${skillsByCategory[cat.name]}" />
                    <c:choose>
                        <c:when test="${not empty catSkills}">
                            <div style="display: flex; flex-wrap: wrap; gap: 0.5rem;">
                                <c:forEach var="skill" items="${catSkills}">
                                    <div style="display: inline-flex; align-items: center; gap: 0.4rem; background-color: #F1F1EC; border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 0.25rem 0.6rem; font-size: 0.82rem;">
                                        <strong><c:out value="${skill.name}" /></strong>
                                        <form action="${pageContext.request.contextPath}/admin/catalog/skill/delete" method="post" style="margin: 0; display: inline;">
                                            <input type="hidden" name="_csrf" value="${csrfToken}">
                                            <input type="hidden" name="id" value="${skill.id}">
                                            <button type="submit" style="background: none; border: none; color: var(--color-danger); cursor: pointer; padding: 0 0.2rem; font-weight: bold; font-size: 0.85rem;" title="Delete Skill">×</button>
                                        </form>
                                    </div>
                                </c:forEach>
                            </div>
                        </c:when>
                        <c:otherwise>
                            <span style="font-size: 0.82rem; color: var(--color-text-muted); font-style: italic;">
                                No skills added to this category yet.
                            </span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </c:forEach>
        </div>
    </div>
</main>

<%@ include file="../layout/footer.jspf" %>
