package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;

public class CvTemplate implements Serializable {
    private String id;
    private String name;
    private String description;
    private String category;
    private boolean isAtsFriendly;
    private boolean hasPhotoOption;
    private String accentColor;

    public CvTemplate(String id, String name, String description, String category, boolean isAtsFriendly, boolean hasPhotoOption, String accentColor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.isAtsFriendly = isAtsFriendly;
        this.hasPhotoOption = hasPhotoOption;
        this.accentColor = accentColor;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public boolean isAtsFriendly() { return isAtsFriendly; }
    public boolean hasPhotoOption() { return hasPhotoOption; }
    public String getAccentColor() { return accentColor; }
}
