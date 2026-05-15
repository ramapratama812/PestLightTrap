package com.pebelti.pestlighttrap;

public class NotificationItem {
    private String title;
    private String description;
    private String time;
    private String type; // "CRITICAL", "WARNING", "INFO"
    private int iconResId;

    public NotificationItem(String title, String description, String time, String type, int iconResId) {
        this.title = title;
        this.description = description;
        this.time = time;
        this.type = type;
        this.iconResId = iconResId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTime() { return time; }
    public String getType() { return type; }
    public int getIconResId() { return iconResId; }
}
