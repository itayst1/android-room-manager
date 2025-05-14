package com.example.roommanager;

public class Reservation {
    public String userEmail;
    public String startTime;
    public String endTime;

    public Reservation() {
        // Default constructor required for Firebase
    }

    public Reservation(String userEmail, String startTime, String endTime) {
        this.userEmail = userEmail;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getUserEmail() {
        return userEmail;
    }
}

