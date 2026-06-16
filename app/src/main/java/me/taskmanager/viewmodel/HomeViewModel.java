package me.taskmanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.taskmanager.database.ProjectRepository;
import me.taskmanager.database.TaskRepository;
import me.taskmanager.model.HomeStats;
import me.taskmanager.model.Project;
import me.taskmanager.model.Task;
import me.taskmanager.preferences.UserPreferencesManager;

public class HomeViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserPreferencesManager preferencesManager;
    
    private final MutableLiveData<HomeStats> homeStatsLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository(application);
        this.projectRepository = new ProjectRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
    }

    public LiveData<HomeStats> getHomeStatsLiveData() {
        return homeStatsLiveData;
    }

    public void loadDashboardStats() {
        executorService.execute(() -> {
            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId == -1) return;

            // 1. Query Project Count
            List<Project> userProjects = projectRepository.getAllProjectsForUser(currentUserId);
            int totalProjects = userProjects.size();

            // 2. Query and filter tasks related to current user
            List<Task> allTasks = taskRepository.getAllTasks();
            List<Task> userTasks = new ArrayList<>();
            for (Task t : allTasks) {
                if (t.getProjectId() == null || t.getProjectId() == 0) {
                    // Personal / unassigned task
                    if (t.getAssignedUserId() == null || t.getAssignedUserId() == currentUserId) {
                        userTasks.add(t);
                    }
                } else {
                    // Project-linked task
                    if (projectRepository.isUserMemberOfProject(t.getProjectId(), currentUserId)) {
                        userTasks.add(t);
                    }
                }
            }

            int totalTasks = userTasks.size();
            int completedTasks = 0;
            int pendingTasks = 0;
            int overdueTasks = 0;
            long now = System.currentTimeMillis();

            for (Task t : userTasks) {
                if (t.isCompleted() || Task.STATUS_DONE.equals(t.getStatus())) {
                    completedTasks++;
                } else {
                    pendingTasks++;
                    if (t.getDueDate() > 0 && t.getDueDate() < now) {
                        overdueTasks++;
                    }
                }
            }

            int progressPercent = totalTasks > 0 ? (completedTasks * 100 / totalTasks) : 0;
            String pendingText = pendingTasks == 1 
                ? "1 pending task" 
                : pendingTasks + " pending tasks";

            HomeStats stats = new HomeStats(
                totalProjects,
                totalTasks,
                completedTasks,
                pendingTasks,
                overdueTasks,
                progressPercent,
                pendingText
            );

            homeStatsLiveData.postValue(stats);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
