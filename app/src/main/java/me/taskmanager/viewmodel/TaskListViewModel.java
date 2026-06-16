package me.taskmanager.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.taskmanager.database.ActivityLogRepository;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.database.TaskRepository;
import me.taskmanager.model.Project;
import me.taskmanager.model.Task;
import me.taskmanager.model.User;
import me.taskmanager.preferences.UserPreferencesManager;

public class TaskListViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserPreferencesManager preferencesManager;

    private final MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<User>> assigneesLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface DeleteCallback {
        void onResult(boolean success);
    }

    public TaskListViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository(application);
        this.projectRepository = new ProjectRepository(application);
        this.activityLogRepository = new ActivityLogRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
    }

    public LiveData<List<Project>> getProjectsLiveData() {
        return projectsLiveData;
    }

    public LiveData<List<User>> getAssigneesLiveData() {
        return assigneesLiveData;
    }

    public LiveData<List<Task>> getTasksLiveData() {
        return tasksLiveData;
    }

    public long getCurrentUserId() {
        return preferencesManager.getCurrentUserId();
    }

    public void loadProjects() {
        executorService.execute(() -> {
            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId == -1) return;
            List<Project> projects = projectRepository.getAllProjectsForUser(currentUserId);
            projectsLiveData.postValue(projects);
        });
    }

    public void loadAssignees(long projectId) {
        executorService.execute(() -> {
            List<User> assignees = new ArrayList<>();
            if (projectId > 0) {
                assignees = projectRepository.getMembersForProject(projectId);
            } else {
                long currentUserId = preferencesManager.getCurrentUserId();
                if (currentUserId != -1) {
                    List<Project> projects = projectRepository.getAllProjectsForUser(currentUserId);
                    List<Long> addedUserIds = new ArrayList<>();
                    for (Project p : projects) {
                        List<User> members = projectRepository.getMembersForProject(p.getId());
                        for (User u : members) {
                            if (!addedUserIds.contains(u.getId())) {
                                addedUserIds.add(u.getId());
                                assignees.add(u);
                            }
                        }
                    }
                }
            }
            assigneesLiveData.postValue(assignees);
        });
    }

    public void filterTasks(Long projectId, Long assignedUserId, Integer priority, String status, String query, String sortBy) {
        executorService.execute(() -> {
            List<Task> filteredTasks = taskRepository.getSearchFilteredTasks(
                    projectId,
                    assignedUserId,
                    priority,
                    status,
                    query,
                    sortBy
            );
            tasksLiveData.postValue(filteredTasks);
        });
    }

    public void deleteTask(Task task, DeleteCallback callback) {
        executorService.execute(() -> {
            long currentUserId = preferencesManager.getCurrentUserId();
            activityLogRepository.insertLog(currentUserId, "deleted task: \"" + task.getTitle() + "\"", "Task", task.getId());
            int result = taskRepository.deleteTask(task.getId());
            boolean success = result > 0;
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onResult(success));
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
