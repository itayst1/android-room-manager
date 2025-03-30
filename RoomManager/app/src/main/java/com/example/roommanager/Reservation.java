package com.example.roommanager;

public class Reservation {
    public String userEmail;
    public String startTime;
    public String duration;

    public Reservation() {
        // Default constructor required for Firebase
    }

    public Reservation(String userEmail, String startTime, String duration) {
        this.userEmail = userEmail;
        this.startTime = startTime;
        this.duration = duration;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getDuration() {
        return duration;
    }

    public String getUserEmail() {
        return userEmail;
    }
}

