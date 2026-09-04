package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;

public class ExperienceItem implements Serializable {
    private String jobTitle = "";
    private String company = "";
    private String location = "";
    private String startDate = "";
    private String endDate = "";
    private boolean isCurrent = false;
    private String description = "";

    public ExperienceItem() {}

    public ExperienceItem(String jobTitle, String company, String startDate, String endDate, String description) {
        this.jobTitle = jobTitle;
        this.company = company;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public ExperienceItem(String jobTitle, String company, String location, String startDate, String endDate, String description) {
        this.jobTitle = jobTitle;
        this.company = company;
        this.location = location;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
    }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public boolean isCurrent() { return isCurrent; }
    public void setCurrent(boolean current) { isCurrent = current; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
