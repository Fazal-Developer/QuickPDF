package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;

public class EducationItem implements Serializable {
    private String degree = "";
    private String institution = "";
    private String location = "";
    private String startDate = "";
    private String endDate = "";
    private String year = "";
    private String description = "";

    public EducationItem() {}

    public EducationItem(String degree, String institution, String year) {
        this.degree = degree;
        this.institution = institution;
        this.year = year;
    }

    public EducationItem(String degree, String institution, String location, String startDate, String endDate, String description) {
        this.degree = degree;
        this.institution = institution;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getYear() {
        if (year != null && !year.isEmpty()) return year;
        if (!endDate.isEmpty()) return endDate;
        return startDate;
    }
    public void setYear(String year) { this.year = year; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
