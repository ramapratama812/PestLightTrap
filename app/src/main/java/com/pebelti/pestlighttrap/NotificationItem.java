package com.pebelti.pestlighttrap;

public class NotificationItem {
    private String title;
    private String time;
    private String type; // "CRITICAL", "WARNING", "INFO"

    public NotificationItem(String title, String time, String type) {
        this.title = title;
        this.time = time;
        this.type = type;
    }

    public String getTitle() { return title; }
    public String getTime() { return time; }
    public String getType() { return type; }
}
