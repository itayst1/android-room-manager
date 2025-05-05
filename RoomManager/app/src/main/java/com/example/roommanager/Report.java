package com.example.roommanager;

public class Report {
    private String reportId;
    private String userEmail;
    private String message;
    private String imageUrl;

    // Constructor
    public Report(String reportId, String userEmail, String message, String imageUrl) {
        this.reportId = reportId;
        this.userEmail = userEmail;
        this.message = message;
        this.imageUrl = imageUrl;
    }

    public String getReportId() {
        return reportId;
    }

    // Getters
    public String getUserEmail() {
        return userEmail;
    }

    public String getMessage() {
        return message;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Setters (optional)
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
