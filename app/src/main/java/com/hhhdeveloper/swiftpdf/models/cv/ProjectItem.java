package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;

public class ProjectItem implements Serializable {
    private String name = "";
    private String description = "";
    private String techStack = "";
    private String url = "";

    public ProjectItem() {}

    public ProjectItem(String name, String description, String techStack) {
        this.name = name;
        this.description = description;
        this.techStack = techStack;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
