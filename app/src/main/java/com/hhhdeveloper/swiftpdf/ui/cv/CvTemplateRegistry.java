package com.hhhdeveloper.swiftpdf.ui.cv;

import com.hhhdeveloper.swiftpdf.models.cv.CvTemplate;

import java.util.ArrayList;
import java.util.List;

public class CvTemplateRegistry {

    private static final List<CvTemplate> TEMPLATES = new ArrayList<>();

    static {
        TEMPLATES.add(new CvTemplate("classic_professional", "Classic Professional", "Single column with top banner. Recruiter approved & clean.", "Professional", true, false, "#6C5CE7"));
        TEMPLATES.add(new CvTemplate("modern_minimal", "Modern Minimal Sidebar", "Dark left sidebar for contact & skills with optional profile photo.", "Modern", true, true, "#2D3748"));
        TEMPLATES.add(new CvTemplate("ats_standard", "ATS Standard 100%", "Simple text structure, clear headings & 100% ATS scanner pass.", "ATS Friendly", true, false, "#1A202C"));
        TEMPLATES.add(new CvTemplate("executive", "Executive Leader", "Sophisticated serif typography for senior leaders & executives.", "Professional", false, false, "#2B6CB0"));
        TEMPLATES.add(new CvTemplate("creative", "Creative Photo Studio", "Vibrant accents, stylish layout & profile photo container.", "Creative", false, true, "#D69E2E"));
        TEMPLATES.add(new CvTemplate("tech", "Tech Developer", "Code aesthetic, project cards & prominent skill badges.", "Modern", false, false, "#0F172A"));
        TEMPLATES.add(new CvTemplate("academic", "Academic & Research", "Prioritizes education, research publications & academic credentials.", "Academic", true, false, "#319795"));
        TEMPLATES.add(new CvTemplate("two_column", "Two Column Modern", "Side-by-side balanced columns with dark sidebar & profile photo.", "Modern", true, true, "#4A5568"));
        TEMPLATES.add(new CvTemplate("elegant_corporate", "Elegant Corporate", "Premium double borders, burgundy accents & profile photo avatar.", "Professional", false, true, "#800020"));
        TEMPLATES.add(new CvTemplate("fresh_graduate", "Fresh Graduate", "Highlights education, projects, skills & certifications first.", "Fresh Graduate", true, false, "#2E7D32"));
    }

    public static List<CvTemplate> getAllTemplates() {
        return TEMPLATES;
    }

    public static List<CvTemplate> filterByCategory(String category) {
        if (category == null || category.equalsIgnoreCase("All") || category.equalsIgnoreCase("All Templates")) {
            return getAllTemplates();
        }
        List<CvTemplate> filtered = new ArrayList<>();
        for (CvTemplate t : TEMPLATES) {
            if (category.equalsIgnoreCase("ATS Friendly") && t.isAtsFriendly()) {
                filtered.add(t);
            } else if (t.getCategory().equalsIgnoreCase(category)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    public static CvTemplate getById(String id) {
        for (CvTemplate t : TEMPLATES) {
            if (t.getId().equalsIgnoreCase(id)) return t;
        }
        return TEMPLATES.get(0);
    }
}
