package me.taskmanager.model;

public class HomeStats {
    private final int totalProjects;
    private final int totalTasks;
    private final int completedTasks;
    private final int pendingTasks;
    private final int overdueTasks;
    private final int progressPercent;
    private final String pendingText;

    public HomeStats(int totalProjects, int totalTasks, int completedTasks, int pendingTasks, int overdueTasks, int progressPercent, String pendingText) {
        this.totalProjects = totalProjects;
        this.totalTasks = totalTasks;
        this.completedTasks = completedTasks;
        this.pendingTasks = pendingTasks;
        this.overdueTasks = overdueTasks;
        this.progressPercent = progressPercent;
        this.pendingText = pendingText;
    }

    public int getTotalProjects() { return totalProjects; }
    public int getTotalTasks() { return totalTasks; }
    public int getCompletedTasks() { return completedTasks; }
    public int getPendingTasks() { return pendingTasks; }
    public int getOverdueTasks() { return overdueTasks; }
    public int getProgressPercent() { return progressPercent; }
    public String getPendingText() { return pendingText; }
}
