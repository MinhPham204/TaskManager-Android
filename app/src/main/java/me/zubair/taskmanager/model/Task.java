package me.zubair.taskmanager.model;

/**
 * Model class representing a Task with status, priority, project linkage, and assignees.
 */
public class Task {
    private long id;
    private String title;
    private String description;
    private long dueDate;
    private int priority;
    private boolean completed;
    private long createdAt;
    
    // Phase 4 fields
    private Long projectId;
    private Long assignedUserId;
    private String status;
    private long updatedAt;

    // Priority constants
    public static final int PRIORITY_LOW = 1;
    public static final int PRIORITY_MEDIUM = 2;
    public static final int PRIORITY_HIGH = 3;

    // Status constants
    public static final String STATUS_TODO = "TODO";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    // Default constructor
    public Task() {
        this.status = STATUS_TODO;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Constructor with parameters (Phase 1 legacy style)
    public Task(String title, String description, long dueDate, int priority, boolean completed) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = completed;
        this.status = completed ? STATUS_DONE : STATUS_TODO;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // New Constructor (Phase 4 style)
    public Task(String title, String description, long dueDate, int priority, String status, Long projectId, Long assignedUserId) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.status = status != null ? status : STATUS_TODO;
        this.completed = STATUS_DONE.equals(this.status);
        this.projectId = projectId;
        this.assignedUserId = assignedUserId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    /**
     * Set completion status. Keep status string in sync.
     */
    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.status = STATUS_DONE;
        } else if (STATUS_DONE.equals(this.status)) {
            this.status = STATUS_TODO;
        }
        this.updatedAt = System.currentTimeMillis();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getAssignedUserId() {
        return assignedUserId;
    }

    public void setAssignedUserId(Long assignedUserId) {
        this.assignedUserId = assignedUserId;
    }

    public String getStatus() {
        return status != null ? status : STATUS_TODO;
    }

    /**
     * Set task status. Syncs the completed boolean status too.
     */
    public void setStatus(String status) {
        this.status = status;
        this.completed = STATUS_DONE.equals(status);
        this.updatedAt = System.currentTimeMillis();
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
