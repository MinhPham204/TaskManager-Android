package me.taskmanager.model;

public class ActivityLog {
    private long id;
    private long userId;
    private String action;
    private String targetType;
    private long targetId;
    private String createdAt;

    // Transient fields for actor metadata
    private String username;
    private String userFullName;

    public ActivityLog() {}

    public ActivityLog(long userId, String action, String targetType, long targetId) {
        this.userId = userId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.createdAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public long getTargetId() {
        return targetId;
    }

    public void setTargetId(long targetId) {
        this.targetId = targetId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getActorDisplayName() {
        if (userFullName != null && !userFullName.trim().isEmpty()) {
            return userFullName;
        }
        return username != null ? "@" + username : "Unknown";
    }
}
