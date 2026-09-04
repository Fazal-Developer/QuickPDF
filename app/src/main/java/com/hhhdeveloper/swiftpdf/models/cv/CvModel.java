package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CvModel implements Serializable {
    private String id = "cv_" + System.currentTimeMillis();
    private String title = "Untitled CV";
    private String selectedTemplateId = "classic_professional";
    private long lastEdited = System.currentTimeMillis();

    private PersonalInfo personalInfo = new PersonalInfo();
    private String summary = "";
    private List<ExperienceItem> experienceList = new ArrayList<>();
    private List<EducationItem> educationList = new ArrayList<>();
    private List<String> skillsList = new ArrayList<>();
    private List<ProjectItem> projectsList = new ArrayList<>();
    private List<CertificationItem> certificationsList = new ArrayList<>();
    private DesignSettings designSettings = new DesignSettings();

    public CvModel() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSelectedTemplateId() { return selectedTemplateId; }
    public void setSelectedTemplateId(String selectedTemplateId) { this.selectedTemplateId = selectedTemplateId; }

    public long getLastEdited() { return lastEdited; }
    public void setLastEdited(long lastEdited) { this.lastEdited = lastEdited; }

    public PersonalInfo getPersonalInfo() { return personalInfo; }
    public void setPersonalInfo(PersonalInfo personalInfo) { this.personalInfo = personalInfo; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public List<ExperienceItem> getExperienceList() { return experienceList; }
    public void setExperienceList(List<ExperienceItem> experienceList) { this.experienceList = experienceList; }

    public List<EducationItem> getEducationList() { return educationList; }
    public void setEducationList(List<EducationItem> educationList) { this.educationList = educationList; }

    public List<String> getSkillsList() { return skillsList; }
    public void setSkillsList(List<String> skillsList) { this.skillsList = skillsList; }

    public List<ProjectItem> getProjectsList() { return projectsList; }
    public void setProjectsList(List<ProjectItem> projectsList) { this.projectsList = projectsList; }

    public List<CertificationItem> getCertificationsList() { return certificationsList; }
    public void setCertificationsList(List<CertificationItem> certificationsList) { this.certificationsList = certificationsList; }

    public DesignSettings getDesignSettings() { return designSettings; }
    public void setDesignSettings(DesignSettings designSettings) { this.designSettings = designSettings; }
}
