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

import me.taskmanager.database.InvitationRepository;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.model.Invitation;
import me.taskmanager.model.Project;
import me.taskmanager.preferences.UserPreferencesManager;

public class ProjectListViewModel extends AndroidViewModel {

    private final ProjectRepository projectRepository;
    private final InvitationRepository invitationRepository;
    private final UserPreferencesManager preferencesManager;

    private final MutableLiveData<List<Project>> projectsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Invitation>> pendingInvitationsLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface ActionCallback {
        void onResult(boolean success, String message);
    }

    public interface RoleCallback {
        void onResult(String role);
    }

    public ProjectListViewModel(@NonNull Application application) {
        super(application);
        this.projectRepository = new ProjectRepository(application);
        this.invitationRepository = new InvitationRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
    }

    public LiveData<List<Project>> getProjectsLiveData() {
        return projectsLiveData;
    }

    public LiveData<List<Invitation>> getPendingInvitationsLiveData() {
        return pendingInvitationsLiveData;
    }

    public long getCurrentUserId() {
        return preferencesManager.getCurrentUserId();
    }

    public void loadProjectsAndInvitations() {
        executorService.execute(() -> {
            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId == -1) return;

            // Load Pending Invitations
            List<Invitation> pendingInvites = invitationRepository.getPendingInvitationsForUser(currentUserId);
            pendingInvitationsLiveData.postValue(pendingInvites);

            // Load Projects
            List<Project> list = projectRepository.getAllProjectsForUser(currentUserId);
            projectsLiveData.postValue(list);
        });
    }

    public void acceptInvitation(Invitation invitation, ActionCallback callback) {
        executorService.execute(() -> {
            boolean statusOk = invitationRepository.updateInvitationStatus(invitation.getId(), "ACCEPTED");
            if (statusOk) {
                long currentUserId = preferencesManager.getCurrentUserId();
                boolean joinOk = projectRepository.addMemberToProject(invitation.getProjectId(), currentUserId, "Member");
                if (joinOk) {
                    postCallback(callback, true, "Joined project: " + invitation.getProjectName());
                } else {
                    postCallback(callback, false, "Accepted invitation but failed to update membership table");
                }
            } else {
                postCallback(callback, false, "Failed to accept invitation");
            }
            loadProjectsAndInvitations();
        });
    }

    public void declineInvitation(Invitation invitation, ActionCallback callback) {
        executorService.execute(() -> {
            boolean statusOk = invitationRepository.updateInvitationStatus(invitation.getId(), "REJECTED");
            if (statusOk) {
                postCallback(callback, true, "Declined invitation to join " + invitation.getProjectName());
            } else {
                postCallback(callback, false, "Failed to decline invitation");
            }
            loadProjectsAndInvitations();
        });
    }

    public void checkMemberRole(long projectId, RoleCallback callback) {
        executorService.execute(() -> {
            long userId = preferencesManager.getCurrentUserId();
            String role = projectRepository.getMemberRole(projectId, userId);
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(role));
        });
    }

    public void deleteProject(long projectId, ActionCallback callback) {
        executorService.execute(() -> {
            boolean deleted = projectRepository.deleteProject(projectId);
            postCallback(callback, deleted, deleted ? "Project deleted" : "Failed to delete project");
            loadProjectsAndInvitations();
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
