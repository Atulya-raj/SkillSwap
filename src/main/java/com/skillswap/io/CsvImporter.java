package com.skillswap.io;

import com.skillswap.dao.CategoryDao;
import com.skillswap.dao.SkillDao;
import com.skillswap.model.Category;
import com.skillswap.model.Skill;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Robust CSV importer for bulk category and skill catalogs with error reporting and graceful bad row handling.
 */
public class CsvImporter {

    /**
     * DTO containing the result of a catalog CSV import operation.
     */
    public static class ImportResult {
        private final int importedCount;
        private final int skippedCount;
        private final List<String> warnings;

        public ImportResult(int importedCount, int skippedCount, List<String> warnings) {
            this.importedCount = importedCount;
            this.skippedCount = skippedCount;
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }

        public int getImportedCount() {
            return importedCount;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Imports categories and skills from an input stream containing CSV lines of format 'category,skill'.
     * Malformed or duplicate rows are recorded as warnings without crashing the import.
     *
     * @param is input stream of CSV data
     * @param categoryDao category DAO for lookup and creation
     * @param skillDao skill DAO for lookup and creation
     * @return ImportResult with total imported count and list of warning messages
     * @throws IOException on stream reading failure
     */
    public ImportResult importCatalog(InputStream is, CategoryDao categoryDao, SkillDao skillDao) throws IOException {
        int imported = 0;
        int skipped = 0;
        List<String> warnings = new ArrayList<>();

        if (is == null) {
            warnings.add("No CSV data provided");
            return new ImportResult(0, 0, warnings);
        }

        // Cache existing categories by lower-case name for quick lookup
        Map<String, Category> categoryMap = new HashMap<>();
        for (Category c : categoryDao.findAll()) {
            if (c.getName() != null) {
                categoryMap.put(c.getName().trim().toLowerCase(), c);
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Strip UTF-8 BOM if present on first line
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Parse CSV fields (handles simple quotes or standard commas)
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 2) {
                    warnings.add("Line " + lineNumber + ": Invalid format (expected 'category,skill'): \"" + line + "\"");
                    skipped++;
                    continue;
                }

                String catName = parts.get(0).trim();
                String skillName = parts.get(1).trim();

                // Skip header line if present
                if (lineNumber == 1 && "category".equalsIgnoreCase(catName) && "skill".equalsIgnoreCase(skillName)) {
                    continue;
                }

                if (catName.isEmpty()) {
                    warnings.add("Line " + lineNumber + ": Category name is blank");
                    skipped++;
                    continue;
                }
                if (skillName.isEmpty()) {
                    warnings.add("Line " + lineNumber + ": Skill name is blank");
                    skipped++;
                    continue;
                }

                try {
                    // Resolve or create Category
                    String catKey = catName.toLowerCase();
                    Category category = categoryMap.get(catKey);
                    if (category == null) {
                        Category newCat = new Category();
                        newCat.setName(catName);
                        newCat.setDescription("Imported category: " + catName);
                        long newCatId = categoryDao.insert(newCat);
                        newCat.setId((int) newCatId);
                        categoryMap.put(catKey, newCat);
                        category = newCat;
                    }

                    // Check if skill already exists under this category
                    Optional<Skill> existingSkill = skillDao.findByCategoryAndName(category.getId(), skillName);
                    if (existingSkill.isPresent()) {
                        warnings.add("Line " + lineNumber + ": Skill '" + skillName + "' already exists under category '" + category.getName() + "' (skipped)");
                        skipped++;
                        continue;
                    }

                    // Insert new skill
                    Skill s = new Skill();
                    s.setCategoryId(category.getId());
                    s.setName(skillName);
                    s.setDescription("Imported skill: " + skillName);
                    skillDao.insert(s);
                    imported++;
                } catch (Exception ex) {
                    warnings.add("Line " + lineNumber + ": Failed to insert skill '" + skillName + "': " + ex.getMessage());
                    skipped++;
                }
            }
        }

        return new ImportResult(imported, skipped, warnings);
    }

    /**
     * Parses a single CSV line taking double quotes into account.
     */
    private List<String> parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}
