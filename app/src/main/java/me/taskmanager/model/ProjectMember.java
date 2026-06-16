package me.taskmanager.model;

/**
 * Model class representing a member of a project and their role
 */
public class ProjectMember {
    private long id;
    private long projectId;
    private long userId;
    private String role; // e.g., "Leader", "Member"

    public ProjectMember() {}

    public ProjectMember(long projectId, long userId, String role) {
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }

    public ProjectMember(long id, long projectId, long userId, String role) {
        this.id = id;
        this.projectId = projectId;
        this.userId = userId;
        this.role = role;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getProjectId() { return projectId; }
    public void setProjectId(long projectId) { this.projectId = projectId; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
