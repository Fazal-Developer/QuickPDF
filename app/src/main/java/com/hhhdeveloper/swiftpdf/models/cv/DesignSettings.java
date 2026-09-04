package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;

public class DesignSettings implements Serializable {
    private String accentColor = "#6C5CE7"; // Primary Purple/Indigo default
    private String fontFamily = "SANS";      // SANS, SERIF, MONO
    private String spacing = "BALANCED";     // COMPACT, BALANCED, COMFORTABLE
    private float marginScale = 1.0f;

    public DesignSettings() {}

    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public String getSpacing() { return spacing; }
    public void setSpacing(String spacing) { this.spacing = spacing; }

    public float getMarginScale() { return marginScale; }
    public void setMarginScale(float marginScale) { this.marginScale = marginScale; }
}
