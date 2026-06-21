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

public class EditTaskViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final ActivityLogRepository activityLogRepository;
    private final UserPreferencesManager preferencesManager;

    private final MutableLiveData<Task> taskLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<User>> membersLiveData = new MutableLiveData<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface ActionCallback {
        void onResult(boolean success, String message);
    }

    public EditTaskViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository(application);
        this.projectRepository = new ProjectRepository(application);
        this.activityLogRepository = new ActivityLogRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
    }

    public LiveData<Task> getTaskLiveData() {
        return taskLiveData;
    }

    public LiveData<List<Project>> getProjectsLiveData() {
        return projectsLiveData;
    }

    public LiveData<List<User>> getMembersLiveData() {
        return membersLiveData;
    }

    public void loadTaskAndProjects(long taskId) {
        executorService.execute(() -> {
            // Load Task
            Task task = taskRepository.getTaskById(taskId);
            taskLiveData.postValue(task);

            // Load Projects
            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId != -1) {
                List<Project> projects = projectRepository.getAllProjectsForUser(currentUserId);
                projectsLiveData.postValue(projects);
            }
        });
    }

    public void loadProjectMembers(long projectId) {
        executorService.execute(() -> {
            if (projectId > 0) {
                List<User> members = projectRepository.getMembersForProject(projectId);
                membersLiveData.postValue(members);
            } else {
                membersLiveData.postValue(new ArrayList<>());
            }
        });
    }

    public void updateTask(Task task, ActionCallback callback) {
        executorService.execute(() -> {
            int rowsAffected = taskRepository.updateTask(task);
            if (rowsAffected > 0) {
                long currentUserId = preferencesManager.getCurrentUserId();
                activityLogRepository.insertLog(currentUserId, "updated task: \"" + task.getTitle() + "\"", "Task", task.getId());
                postCallback(callback, true, "Task updated successfully");
            } else {
                postCallback(callback, false, "Failed to update task");
            }
        });
    }

    private void postCallback(ActionCallback callback, boolean success, String message) {
        if (callback != null) {
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(success, message));
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
