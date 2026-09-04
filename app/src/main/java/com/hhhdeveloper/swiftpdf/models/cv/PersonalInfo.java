package com.hhhdeveloper.swiftpdf.models.cv;

import java.io.Serializable;

public class PersonalInfo implements Serializable {
    private String fullName = "";
    private String title = "";
    private String email = "";
    private String phone = "";
    private String location = "";
    private String linkedin = "";
    private String website = "";
    private String photoUri = "";

    public PersonalInfo() {}

    public PersonalInfo(String fullName, String title, String email, String phone, String location, String linkedin, String website) {
        this.fullName = fullName;
        this.title = title;
        this.email = email;
        this.phone = phone;
        this.location = location;
        this.linkedin = linkedin;
        this.website = website;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
}
