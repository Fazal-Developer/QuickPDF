package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;

public class CertificationItem implements Serializable {
    private String name = "";
    private String issuer = "";
    private String issueDate = "";

    public CertificationItem() {}

    public CertificationItem(String name, String issuer, String issueDate) {
        this.name = name;
        this.issuer = issuer;
        this.issueDate = issueDate;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }
}
