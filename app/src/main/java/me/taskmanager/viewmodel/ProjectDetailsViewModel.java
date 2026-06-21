package me.taskmanager.viewmodel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.taskmanager.database.ActivityLogRepository;
import me.taskmanager.database.InvitationRepository;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.database.TaskRepository;
import me.taskmanager.database.UserRepository;
import me.taskmanager.model.Project;
import me.taskmanager.model.Task;
import me.taskmanager.model.User;
import me.taskmanager.preferences.UserPreferencesManager;
import me.taskmanager.utils.FileHelper;

public class ProjectDetailsViewModel extends AndroidViewModel {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ActivityLogRepository activityLogRepository;
    private final InvitationRepository invitationRepository;
    private final UserPreferencesManager preferencesManager;
    private final FileHelper fileHelper;

    private final MutableLiveData<Project> projectLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> userRoleLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<User>> membersLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Task>> tasksLiveData = new MutableLiveData<>();
    
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface ActionCallback {
        void onResult(boolean success, String message);
    }

    public ProjectDetailsViewModel(@NonNull Application application) {
        super(application);
        this.projectRepository = new ProjectRepository(application);
        this.userRepository = new UserRepository(application);
        this.taskRepository = new TaskRepository(application);
        this.activityLogRepository = new ActivityLogRepository(application);
        this.invitationRepository = new InvitationRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
        this.fileHelper = new FileHelper();
    }

    public LiveData<Project> getProjectLiveData() {
        return projectLiveData;
    }

    public LiveData<String> getUserRoleLiveData() {
        return userRoleLiveData;
    }

    public LiveData<List<User>> getMembersLiveData() {
        return membersLiveData;
    }

    public LiveData<List<Task>> getTasksLiveData() {
        return tasksLiveData;
    }

    public long getCurrentUserId() {
        return preferencesManager.getCurrentUserId();
    }

    public void loadProjectDetails(long projectId) {
        executorService.execute(() -> {
            Project project = projectRepository.getProjectById(projectId);
            projectLiveData.postValue(project);

            if (project != null) {
                long currentUserId = preferencesManager.getCurrentUserId();
                String role = projectRepository.getMemberRole(projectId, currentUserId);
                userRoleLiveData.postValue(role);

                List<User> members = projectRepository.getMembersForProject(projectId);
                membersLiveData.postValue(members);

                List<Task> tasks = taskRepository.getTasksForProject(projectId);
                tasksLiveData.postValue(tasks);
            }
        });
    }

    public void addMember(long projectId, String username, ActionCallback callback) {
        executorService.execute(() -> {
            // 1. Verify User Exists
            User targetUser = userRepository.getUserByUsername(username.toLowerCase().trim());
            if (targetUser == null) {
                postCallback(callback, false, "User '" + username + "' not found");
                return;
            }

            // 2. Verify Membership Conflict
            boolean isAlreadyMember = projectRepository.isUserMemberOfProject(projectId, targetUser.getId());
            if (isAlreadyMember) {
                postCallback(callback, false, "User '" + username + "' is already a member of this project");
                return;
            }

            // 3. Verify Pending Invitation Conflict
            boolean isAlreadyInvited = invitationRepository.hasPendingInvitation(projectId, targetUser.getId());
            if (isAlreadyInvited) {
                postCallback(callback, false, "An invitation is already pending for this user");
                return;
            }

            // 4. Send Invitation
            long senderUserId = preferencesManager.getCurrentUserId();
            long result = invitationRepository.insertInvitation(projectId, senderUserId, targetUser.getId());
            if (result != -1) {
                postCallback(callback, true, "Invitation sent to @" + username);
            } else {
                postCallback(callback, false, "Failed to send invitation");
            }
        });
    }

    public void removeMember(long projectId, long userId, ActionCallback callback) {
        executorService.execute(() -> {
            boolean success = projectRepository.removeMemberFromProject(projectId, userId);
            if (success) {
                postCallback(callback, true, "Member removed");
                // Reload list of members
                List<User> members = projectRepository.getMembersForProject(projectId);
                membersLiveData.postValue(members);
            } else {
                postCallback(callback, false, "Failed to remove member");
            }
        });
    }

    public void deleteTask(Task task, ActionCallback callback) {
        executorService.execute(() -> {
            try {
                fileHelper.deleteTaskFiles(getApplication(), task.getId());
                long currentUserId = preferencesManager.getCurrentUserId();
                activityLogRepository.insertLog(currentUserId, "deleted task: \"" + task.getTitle() + "\"", "Task", task.getId());

                int result = taskRepository.deleteTask(task.getId());
                if (result > 0) {
                    postCallback(callback, true, "Task deleted");
                    // Reload list of tasks
                    List<Task> tasks = taskRepository.getTasksForProject(task.getProjectId());
                    tasksLiveData.postValue(tasks);
                } else {
                    postCallback(callback, false, "Error deleting task");
                }
            } catch (Exception e) {
                postCallback(callback, false, "Error: " + e.getMessage());
            }
        });
    }

    public void deleteProject(long projectId, ActionCallback callback) {
        executorService.execute(() -> {
            boolean success = projectRepository.deleteProject(projectId);
            postCallback(callback, success, success ? "Project deleted" : "Failed to delete project");
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
