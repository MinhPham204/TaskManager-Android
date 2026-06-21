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
import me.taskmanager.model.Task;
import me.taskmanager.preferences.UserPreferencesManager;

public class CalendarViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserPreferencesManager preferencesManager;

    private final MutableLiveData<List<Task>> userTasksLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository(application);
        this.projectRepository = new ProjectRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
    }

    public LiveData<List<Task>> getUserTasksLiveData() {
        return userTasksLiveData;
    }

    public void loadUserTasks() {
        executorService.execute(() -> {
            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId == -1) {
                userTasksLiveData.postValue(new ArrayList<>());
                return;
            }

            List<Task> allTasks = taskRepository.getAllTasks();
            List<Task> userTasks = new ArrayList<>();
            for (Task t : allTasks) {
                if (t.getProjectId() == null || t.getProjectId() == 0) {
                    if (t.getAssignedUserId() == null || t.getAssignedUserId() == currentUserId) {
                        userTasks.add(t);
                    }
                } else {
                    if (projectRepository.isUserMemberOfProject(t.getProjectId(), currentUserId)) {
                        userTasks.add(t);
                    }
                }
            }
            userTasksLiveData.postValue(userTasks);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
