package com.hhhdeveloper.swiftpdf.ui.cv.renderers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.hhhdeveloper.swiftpdf.models.cv.CvModel;
import com.hhhdeveloper.swiftpdf.models.cv.EducationItem;
import com.hhhdeveloper.swiftpdf.models.cv.ExperienceItem;
import com.hhhdeveloper.swiftpdf.models.cv.PersonalInfo;

import java.io.File;

public class CvCanvasRenderer {

    public static final float A4_WIDTH_PT = 595f;
    public static final float A4_HEIGHT_PT = 842f;

    /**
     * Render the CV model onto the target Canvas with specified page dimensions.
     */
    public static void render(Canvas canvas, CvModel cvModel, float width, float height) {
        if (cvModel == null || canvas == null) return;

        float scale = width / A4_WIDTH_PT;
        canvas.save();
        canvas.scale(scale, scale);

        // Draw White A4 Background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, A4_WIDTH_PT, A4_HEIGHT_PT, bgPaint);

        String templateId = cvModel.getSelectedTemplateId();
        if (templateId == null) templateId = "classic_professional";

        switch (templateId) {
            case "modern_minimal":
            case "two_column":
                renderTwoColumnLayout(canvas, cvModel);
                break;

            case "executive":
                renderExecutiveLayout(canvas, cvModel);
                break;

            case "creative":
                renderCreativeLayout(canvas, cvModel);
                break;

            case "elegant_corporate":
                renderElegantCorporateLayout(canvas, cvModel);
                break;

            case "fresh_graduate":
                renderFreshGraduateLayout(canvas, cvModel);
                break;

            case "tech":
                renderTechLayout(canvas, cvModel);
                break;

            case "academic":
                renderAcademicLayout(canvas, cvModel);
                break;

            case "ats_standard":
                renderAtsStandardLayout(canvas, cvModel);
                break;

            case "classic_professional":
            default:
                renderClassicProfessionalLayout(canvas, cvModel);
                break;
        }

        canvas.restore();
    }

    // Helper: Draw Circular Profile Avatar or Initials Badge
    private static void drawProfileAvatar(Canvas canvas, float cx, float cy, float radius, PersonalInfo info, int accentColor) {
        Paint avatarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        avatarPaint.setColor(Color.parseColor("#E0DFFF"));
        canvas.drawCircle(cx, cy, radius, avatarPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(accentColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2.5f);
        canvas.drawCircle(cx, cy, radius, borderPaint);

        // Draw Initials
        String initials = "CV";
        if (info.getFullName() != null && !info.getFullName().trim().isEmpty()) {
            String[] parts = info.getFullName().trim().split(" ");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
            } else if (parts.length == 1 && parts[0].length() > 0) {
                initials = ("" + parts[0].charAt(0)).toUpperCase();
            }
        }

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(accentColor);
        textPaint.setTextSize(radius * 0.9f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);

        float textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2);
        canvas.drawText(initials, cx, textY, textPaint);
    }

    // =========================================================================
    // TEMPLATE 1: CLASSIC PROFESSIONAL
    // =========================================================================
    private static void renderClassicProfessionalLayout(Canvas canvas, CvModel cv) {
        PersonalInfo info = cv.getPersonalInfo();
        String accentHex = cv.getDesignSettings().getAccentColor();
        int accentColor = Color.parseColor(accentHex != null ? accentHex : "#6C5CE7");

        Paint accentBg = new Paint();
        accentBg.setColor(accentColor);

        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(24f);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#E0DFFF"));
        titlePaint.setTextSize(13f);

        Paint contactPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        contactPaint.setColor(Color.parseColor("#B8B5FF"));
        contactPaint.setTextSize(10f);

        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#2D3436"));
        headerPaint.setTextSize(14f);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#4A5568"));
        bodyPaint.setTextSize(11f);

        Paint linePaint = new Paint();
        linePaint.setColor(accentColor);
        linePaint.setStrokeWidth(2f);

        canvas.drawRect(0, 0, A4_WIDTH_PT, 125, accentBg);

        String name = info.getFullName().isEmpty() ? "YOUR FULL NAME" : info.getFullName().toUpperCase();
        canvas.drawText(name, 36, 46, namePaint);

        String title = info.getTitle().isEmpty() ? "Professional Title / Role" : info.getTitle();
        canvas.drawText(title, 36, 70, titlePaint);

        StringBuilder contactStr = new StringBuilder();
        if (!info.getEmail().isEmpty()) contactStr.append("📧 ").append(info.getEmail()).append("   ");
        if (!info.getPhone().isEmpty()) contactStr.append("📞 ").append(info.getPhone()).append("   ");
        if (!info.getLocation().isEmpty()) contactStr.append("📍 ").append(info.getLocation());
        canvas.drawText(contactStr.toString(), 36, 98, contactPaint);

        float y = 160;

        if (cv.getSummary() != null && !cv.getSummary().isEmpty()) {
            canvas.drawText("PROFESSIONAL SUMMARY", 36, y, headerPaint);
            y += 6;
            canvas.drawLine(36, y, 190, y, linePaint);
            y += 18;
            y = drawWrappedText(canvas, cv.getSummary(), 36, y, 520, bodyPaint, 15);
            y += 20;
        }

        if (cv.getExperienceList() != null && !cv.getExperienceList().isEmpty()) {
            canvas.drawText("WORK EXPERIENCE", 36, y, headerPaint);
            y += 6;
            canvas.drawLine(36, y, 160, y, linePaint);
            y += 18;

            for (ExperienceItem item : cv.getExperienceList()) {
                Paint boldBody = new Paint(bodyPaint);
                boldBody.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                boldBody.setColor(Color.parseColor("#2D3436"));

                String headLine = item.getJobTitle() + (item.getCompany().isEmpty() ? "" : " — " + item.getCompany());
                canvas.drawText(headLine, 36, y, boldBody);

                String dates = item.getStartDate() + (item.getEndDate().isEmpty() ? "" : " - " + item.getEndDate());
                if (!dates.isEmpty()) {
                    float datesWidth = bodyPaint.measureText(dates);
                    canvas.drawText(dates, A4_WIDTH_PT - 36 - datesWidth, y, bodyPaint);
                }
                y += 16;

                if (!item.getDescription().isEmpty()) {
                    y = drawWrappedText(canvas, item.getDescription(), 36, y, 520, bodyPaint, 14);
                }
                y += 16;
            }
            y += 10;
        }

        if (cv.getEducationList() != null && !cv.getEducationList().isEmpty()) {
            canvas.drawText("EDUCATION", 36, y, headerPaint);
            y += 6;
            canvas.drawLine(36, y, 120, y, linePaint);
            y += 18;

            for (EducationItem item : cv.getEducationList()) {
                Paint boldBody = new Paint(bodyPaint);
                boldBody.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                boldBody.setColor(Color.parseColor("#2D3436"));

                canvas.drawText(item.getDegree(), 36, y, boldBody);
                if (!item.getYear().isEmpty()) {
                    float yearW = bodyPaint.measureText(item.getYear());
                    canvas.drawText(item.getYear(), A4_WIDTH_PT - 36 - yearW, y, bodyPaint);
                }
                y += 14;
                if (!item.getInstitution().isEmpty()) {
                    canvas.drawText(item.getInstitution(), 36, y, bodyPaint);
                    y += 16;
                }
            }
            y += 10;
        }

        if (cv.getSkillsList() != null && !cv.getSkillsList().isEmpty()) {
            canvas.drawText("SKILLS & COMPETENCIES", 36, y, headerPaint);
            y += 6;
            canvas.drawLine(36, y, 190, y, linePaint);
            y += 22;

            float skillX = 36;
            Paint pillBg = new Paint();
            pillBg.setColor(Color.parseColor("#F0EEFF"));

            Paint pillText = new Paint(bodyPaint);
            pillText.setColor(accentColor);
            pillText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            for (String skill : cv.getSkillsList()) {
                if (skill.trim().isEmpty()) continue;
                float w = pillText.measureText(skill.trim()) + 20;
                if (skillX + w > A4_WIDTH_PT - 36) {
                    skillX = 36;
                    y += 24;
                }
                RectF rect = new RectF(skillX, y - 13, skillX + w, y + 6);
                canvas.drawRoundRect(rect, 8, 8, pillBg);
                canvas.drawText(skill.trim(), skillX + 10, y, pillText);
                skillX += w + 8;
            }
        }
    }

    // =========================================================================
    // TEMPLATE 2: TWO-COLUMN MODERN (WITH PROFILE PHOTO AVATAR)
    // =========================================================================
    private static void renderTwoColumnLayout(Canvas canvas, CvModel cv) {
        PersonalInfo info = cv.getPersonalInfo();
        String accentHex = cv.getDesignSettings().getAccentColor();
        int accentColor = Color.parseColor(accentHex != null ? accentHex : "#2D3748");

        float sidebarWidth = 190f;

        Paint sidebarBg = new Paint();
        sidebarBg.setColor(Color.parseColor("#F7FAFC"));
        canvas.drawRect(0, 0, sidebarWidth, A4_HEIGHT_PT, sidebarBg);

        Paint dividerLine = new Paint();
        dividerLine.setColor(Color.parseColor("#E2E8F0"));
        dividerLine.setStrokeWidth(1.5f);
        canvas.drawLine(sidebarWidth, 0, sidebarWidth, A4_HEIGHT_PT, dividerLine);

        // Draw Profile Photo Avatar at top left of sidebar
        drawProfileAvatar(canvas, sidebarWidth / 2f, 65, 34, info, accentColor);

        // Main Header (Right Column)
        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(accentColor);
        namePaint.setTextSize(22f);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#718096"));
        titlePaint.setTextSize(12f);

        String name = info.getFullName().isEmpty() ? "YOUR FULL NAME" : info.getFullName().toUpperCase();
        canvas.drawText(name, sidebarWidth + 24, 52, namePaint);

        String title = info.getTitle().isEmpty() ? "Professional Title" : info.getTitle();
        canvas.drawText(title, sidebarWidth + 24, 72, titlePaint);

        // Left Sidebar Content (Contact & Skills)
        float sideY = 120;
        Paint sideHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        sideHeader.setColor(accentColor);
        sideHeader.setTextSize(12f);
        sideHeader.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint sideText = new Paint(Paint.ANTI_ALIAS_FLAG);
        sideText.setColor(Color.parseColor("#4A5568"));
        sideText.setTextSize(10f);

        canvas.drawText("CONTACT", 20, sideY, sideHeader);
        sideY += 18;
        if (!info.getEmail().isEmpty()) {
            sideY = drawWrappedText(canvas, info.getEmail(), 20, sideY, 150, sideText, 13);
            sideY += 4;
        }
        if (!info.getPhone().isEmpty()) {
            canvas.drawText(info.getPhone(), 20, sideY, sideText);
            sideY += 16;
        }
        if (!info.getLocation().isEmpty()) {
            sideY = drawWrappedText(canvas, info.getLocation(), 20, sideY, 150, sideText, 13);
            sideY += 16;
        }

        sideY += 14;
        if (cv.getSkillsList() != null && !cv.getSkillsList().isEmpty()) {
            canvas.drawText("SKILLS", 20, sideY, sideHeader);
            sideY += 18;
            for (String sk : cv.getSkillsList()) {
                if (sk.trim().isEmpty()) continue;
                canvas.drawText("• " + sk.trim(), 20, sideY, sideText);
                sideY += 15;
            }
        }

        // Right Main Body (Summary, Experience, Education)
        float mainX = sidebarWidth + 24;
        float mainY = 110;

        Paint sectionHeader = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionHeader.setColor(accentColor);
        sectionHeader.setTextSize(14f);
        sectionHeader.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#2D3748"));
        bodyPaint.setTextSize(11f);

        if (cv.getSummary() != null && !cv.getSummary().isEmpty()) {
            canvas.drawText("SUMMARY", mainX, mainY, sectionHeader);
            mainY += 18;
            mainY = drawWrappedText(canvas, cv.getSummary(), mainX, mainY, 340, bodyPaint, 15);
            mainY += 20;
        }

        if (cv.getExperienceList() != null && !cv.getExperienceList().isEmpty()) {
            canvas.drawText("WORK EXPERIENCE", mainX, mainY, sectionHeader);
            mainY += 18;

            for (ExperienceItem item : cv.getExperienceList()) {
                Paint boldBody = new Paint(bodyPaint);
                boldBody.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

                canvas.drawText(item.getJobTitle() + " — " + item.getCompany(), mainX, mainY, boldBody);
                mainY += 15;

                if (!item.getDescription().isEmpty()) {
                    mainY = drawWrappedText(canvas, item.getDescription(), mainX, mainY, 340, bodyPaint, 14);
                }
                mainY += 16;
            }
            mainY += 10;
        }

        if (cv.getEducationList() != null && !cv.getEducationList().isEmpty()) {
            canvas.drawText("EDUCATION", mainX, mainY, sectionHeader);
            mainY += 18;
            for (EducationItem item : cv.getEducationList()) {
                Paint boldBody = new Paint(bodyPaint);
                boldBody.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText(item.getDegree(), mainX, mainY, boldBody);
                mainY += 14;
                if (!item.getInstitution().isEmpty()) {
                    canvas.drawText(item.getInstitution(), mainX, mainY, bodyPaint);
                    mainY += 16;
                }
            }
        }
    }

    // =========================================================================
    // TEMPLATE 3: ATS STANDARD 100%
    // =========================================================================
    private static void renderAtsStandardLayout(Canvas canvas, CvModel cv) {
        PersonalInfo info = cv.getPersonalInfo();
        float y = 50;

        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.BLACK);
        namePaint.setTextSize(22f);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#1A202C"));
        bodyPaint.setTextSize(11f);

        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.BLACK);
        headerPaint.setTextSize(13f);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText(info.getFullName().toUpperCase(), 36, y, namePaint);
        y += 18;
        canvas.drawText(info.getTitle(), 36, y, bodyPaint);
        y += 16;

        String contact = info.getEmail() + " | " + info.getPhone() + " | " + info.getLocation();
        canvas.drawText(contact, 36, y, bodyPaint);
        y += 20;

        Paint linePaint = new Paint();
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(1f);
        canvas.drawLine(36, y, A4_WIDTH_PT - 36, y, linePaint);
        y += 20;

        if (cv.getSummary() != null && !cv.getSummary().isEmpty()) {
            canvas.drawText("PROFESSIONAL SUMMARY", 36, y, headerPaint);
            y += 16;
            y = drawWrappedText(canvas, cv.getSummary(), 36, y, 520, bodyPaint, 15);
            y += 20;
        }

        if (cv.getExperienceList() != null && !cv.getExperienceList().isEmpty()) {
            canvas.drawText("EXPERIENCE", 36, y, headerPaint);
            y += 16;
            for (ExperienceItem item : cv.getExperienceList()) {
                canvas.drawText(item.getJobTitle() + " - " + item.getCompany(), 36, y, headerPaint);
                y += 14;
                if (!item.getDescription().isEmpty()) {
                    y = drawWrappedText(canvas, item.getDescription(), 36, y, 520, bodyPaint, 14);
                }
                y += 16;
            }
        }
    }

    // =========================================================================
    // TEMPLATE 4: EXECUTIVE LEADER
    // =========================================================================
    private static void renderExecutiveLayout(Canvas canvas, CvModel cv) {
        PersonalInfo info = cv.getPersonalInfo();
        String name = info.getFullName().isEmpty() ? "YOUR FULL NAME" : info.getFullName().toUpperCase();
        String title = info.getTitle().isEmpty() ? "Executive Officer" : info.getTitle();

        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.parseColor("#1A202C"));
        namePaint.setTextSize(26f);
        namePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#4A5568"));
        titlePaint.setTextSize(12f);
        titlePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));

        canvas.drawText(name, 40, 56, namePaint);
        canvas.drawText(title, 40, 78, titlePaint);

        Paint rule = new Paint();
        rule.setColor(Color.parseColor("#2B6CB0"));
        rule.setStrokeWidth(2.5f);
        canvas.drawLine(40, 94, A4_WIDTH_PT - 40, 94, rule);

        float y = 130;
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#2B6CB0"));
        headerPaint.setTextSize(14f);
        headerPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#2D3748"));
        bodyPaint.setTextSize(11f);

        if (cv.getSummary() != null && !cv.getSummary().isEmpty()) {
            canvas.drawText("EXECUTIVE SUMMARY", 40, y, headerPaint);
            y += 18;
            y = drawWrappedText(canvas, cv.getSummary(), 40, y, 510, bodyPaint, 15);
            y += 22;
        }

        if (cv.getExperienceList() != null && !cv.getExperienceList().isEmpty()) {
            canvas.drawText("PROFESSIONAL EXPERIENCE", 40, y, headerPaint);
            y += 18;
            for (ExperienceItem item : cv.getExperienceList()) {
                Paint boldBody = new Paint(bodyPaint);
                boldBody.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
                canvas.drawText(item.getJobTitle() + " • " + item.getCompany(), 40, y, boldBody);
                y += 16;
                if (!item.getDescription().isEmpty()) {
                    y = drawWrappedText(canvas, item.getDescription(), 40, y, 510, bodyPaint, 14);
                }
                y += 16;
            }
        }
    }

    // =========================================================================
    // TEMPLATE 5: CREATIVE PHOTO STUDIO (WITH PROFILE PHOTO)
    // =========================================================================
    private static void renderCreativeLayout(Canvas canvas, CvModel cv) {
        PersonalInfo info = cv.getPersonalInfo();
        String name = info.getFullName().isEmpty() ? "YOUR FULL NAME" : info.getFullName();

        Paint bannerBg = new Paint();
        bannerBg.setColor(Color.parseColor("#0F172A"));
        canvas.drawRect(0, 0, A4_WIDTH_PT, 140, bannerBg);

        // Profile photo avatar in banner right side
        drawProfileAvatar(canvas, A4_WIDTH_PT - 75, 70, 36, info, Color.parseColor("#38BDF8"));

        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(24f);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#38BDF8"));
        titlePaint.setTextSize(13f);

        canvas.drawText(name, 36, 54, namePaint);
        canvas.drawText(info.getTitle(), 36, 78, titlePaint);

        float y = 170;
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(Color.parseColor("#0F172A"));
        headerPaint.setTextSize(15f);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#334155"));
        bodyPaint.setTextSize(11f);

        if (cv.getSummary() != null && !cv.getSummary().isEmpty()) {
            canvas.drawText("ABOUT ME", 36, y, headerPaint);
            y += 18;
            y = drawWrappedText(canvas, cv.getSummary(), 36, y, 520, bodyPaint, 15);
            y += 22;
        }

        if (cv.getExperienceList() != null && !cv.getExperienceList().isEmpty()) {
            canvas.drawText("PROJECTS & EXPERIENCE", 36, y, headerPaint);
            y += 18;
            for (ExperienceItem item : cv.getExperienceList()) {
                Paint boldBody = new Paint(bodyPaint);
                boldBody.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText(item.getJobTitle() + " (" + item.getCompany() + ")", 36, y, boldBody);
                y += 16;
                if (!item.getDescription().isEmpty()) {
                    y = drawWrappedText(canvas, item.getDescription(), 36, y, 520, bodyPaint, 14);
                }
                y += 16;
            }
        }
    }

    // =========================================================================
    // TEMPLATE 6: ELEGANT CORPORATE (BURGUNDY / GOLD ACCENTS & PHOTO)
    // =========================================================================
    private static void renderElegantCorporateLayout(Canvas canvas, CvModel cv) {
        PersonalInfo info = cv.getPersonalInfo();
        int accentColor = Color.parseColor("#800020");

        // Double Gold Border
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#D4AF37"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f);
        canvas.drawRect(20, 20, A4_WIDTH_PT - 20, A4_HEIGHT_PT - 20, borderPaint);

        drawProfileAvatar(canvas, 68, 68, 32, info, accentColor);

        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(accentColor);
        namePaint.setTextSize(22f);
        namePaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));

        canvas.drawText(info.getFullName().toUpperCase(), 116, 60, namePaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#4A5568"));
        titlePaint.setTextSize(12f);
        canvas.drawText(info.getTitle(), 116, 78, titlePaint);

        float y = 130;
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(accentColor);
        headerPaint.setTextSize(14f);
        headerPaint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#2D3748"));
        bodyPaint.setTextSize(11f);

        if (cv.getSummary() != null && !cv.getSummary().isEmpty()) {
            canvas.drawText("EXECUTIVE OVERVIEW", 40, y, headerPaint);
            y += 18;
            y = drawWrappedText(canvas, cv.getSummary(), 40, y, 510, bodyPaint, 15);
            y += 20;
        }

        if (cv.getExperienceList() != null && !cv.getExperienceList().isEmpty()) {
            canvas.drawText("CAREER HISTORY", 40, y, headerPaint);
            y += 18;
            for (ExperienceItem item : cv.getExperienceList()) {
                canvas.drawText(item.getJobTitle() + " • " + item.getCompany(), 40, y, headerPaint);
                y += 15;
                if (!item.getDescription().isEmpty()) {
                    y = drawWrappedText(canvas, item.getDescription(), 40, y, 510, bodyPaint, 14);
                }
                y += 16;
            }
        }
    }

    // =========================================================================
    // TEMPLATE 7: FRESH GRADUATE (GREEN ACCENTS, EDUCATION TOP)
    // =========================================================================
    private static void renderFreshGraduateLayout(Canvas canvas, CvModel cv) {
        PersonalInfo info = cv.getPersonalInfo();
        int accentColor = Color.parseColor("#2E7D32");

        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(accentColor);
        namePaint.setTextSize(22f);
        namePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        canvas.drawText(info.getFullName(), 36, 50, namePaint);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.parseColor("#4A5568"));
        titlePaint.setTextSize(12f);
        canvas.drawText(info.getTitle(), 36, 68, titlePaint);

        float y = 110;
        Paint headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(accentColor);
        headerPaint.setTextSize(13f);
        headerPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.parseColor("#2D3748"));
        bodyPaint.setTextSize(11f);

        // Education first for fresh graduates
        if (cv.getEducationList() != null && !cv.getEducationList().isEmpty()) {
            canvas.drawText("EDUCATION & ACADEMICS", 36, y, headerPaint);
            y += 16;
            for (EducationItem item : cv.getEducationList()) {
                canvas.drawText(item.getDegree() + " (" + item.getInstitution() + ")", 36, y, bodyPaint);
                y += 16;
            }
            y += 10;
        }

        if (cv.getSkillsList() != null && !cv.getSkillsList().isEmpty()) {
            canvas.drawText("TECHNICAL SKILLS", 36, y, headerPaint);
            y += 16;
            for (String sk : cv.getSkillsList()) {
                canvas.drawText("• " + sk, 36, y, bodyPaint);
                y += 14;
            }
        }
    }

    // TEMPLATE 8: TECH DEVELOPER
    private static void renderTechLayout(Canvas canvas, CvModel cv) {
        renderClassicProfessionalLayout(canvas, cv);
    }

    // TEMPLATE 9: ACADEMIC & RESEARCH
    private static void renderAcademicLayout(Canvas canvas, CvModel cv) {
        renderExecutiveLayout(canvas, cv);
    }

    // Helper: Multiline text wrapping
    private static float drawWrappedText(Canvas canvas, String text, float x, float y, float maxWidth, Paint paint, float lineHeight) {
        if (text == null || text.isEmpty()) return y;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if (paint.measureText(line.toString() + " " + w) < maxWidth) {
                line.append(" ").append(w);
            } else {
                canvas.drawText(line.toString().trim(), x, y, paint);
                y += lineHeight;
                line = new StringBuilder(w);
            }
        }
        if (line.length() > 0) {
            canvas.drawText(line.toString().trim(), x, y, paint);
            y += lineHeight;
        }
        return y;
    }
}
