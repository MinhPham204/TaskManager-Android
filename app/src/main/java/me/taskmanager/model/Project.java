package me.taskmanager.model;

/**
 * Model class representing a Project
 */
public class Project {
    private long id;
    private String name;
    private String description;
    private String status;
    private String createdAt;

    public Project() {}

    public Project(String name, String description, String status, String createdAt) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Project(long id, String name, String description, String status, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
