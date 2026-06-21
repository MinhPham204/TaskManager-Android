package me.taskmanager.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.taskmanager.database.ActivityLogRepository;
import me.taskmanager.database.CommentRepository;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.database.TaskRepository;
import me.taskmanager.model.ActivityLog;
import me.taskmanager.model.Comment;
import me.taskmanager.model.Task;
import me.taskmanager.preferences.UserPreferencesManager;
import me.taskmanager.utils.FileHelper;

public class TaskDetailsViewModel extends AndroidViewModel {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ActivityLogRepository logRepository;
    private final ProjectRepository projectRepository;
    private final UserPreferencesManager preferencesManager;
    private final FileHelper fileHelper;

    private final MutableLiveData<Task> taskLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> userRoleLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Comment>> commentsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<ActivityLog>> logsLiveData = new MutableLiveData<>();
    private final MutableLiveData<File[]> attachedFilesLiveData = new MutableLiveData<>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public interface ActionCallback {
        void onResult(boolean success, String message);
    }

    public TaskDetailsViewModel(@NonNull Application application) {
        super(application);
        this.taskRepository = new TaskRepository(application);
        this.commentRepository = new CommentRepository(application);
        this.logRepository = new ActivityLogRepository(application);
        this.projectRepository = new ProjectRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
        this.fileHelper = new FileHelper();
    }

    public LiveData<Task> getTaskLiveData() {
        return taskLiveData;
    }

    public LiveData<String> getUserRoleLiveData() {
        return userRoleLiveData;
    }

    public LiveData<List<Comment>> getCommentsLiveData() {
        return commentsLiveData;
    }

    public LiveData<List<ActivityLog>> getLogsLiveData() {
        return logsLiveData;
    }

    public LiveData<File[]> getAttachedFilesLiveData() {
        return attachedFilesLiveData;
    }

    public long getCurrentUserId() {
        return preferencesManager.getCurrentUserId();
    }

    public void loadTask(long taskId) {
        executorService.execute(() -> {
            Task task = taskRepository.getTaskById(taskId);
            taskLiveData.postValue(task);

            if (task != null) {
                long currentUserId = preferencesManager.getCurrentUserId();
                if (task.getProjectId() != null && task.getProjectId() > 0) {
                    String role = projectRepository.getMemberRole(task.getProjectId(), currentUserId);
                    userRoleLiveData.postValue(role);
                } else {
                    userRoleLiveData.postValue("");
                }

                // Load comments
                List<Comment> comments = commentRepository.getCommentsForTask(taskId);
                commentsLiveData.postValue(comments);

                // Load logs
                List<ActivityLog> logs = logRepository.getLogsForTask(taskId);
                logsLiveData.postValue(logs);

                // Load attached files
                File[] attachedFiles = fileHelper.getTaskFiles(getApplication(), taskId);
                attachedFilesLiveData.postValue(attachedFiles);
            }
        });
    }

    public void postComment(long taskId, String content, ActionCallback callback) {
        executorService.execute(() -> {
            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId == -1) {
                postCallback(callback, false, "Session expired");
                return;
            }

            Comment newComment = new Comment(taskId, currentUserId, content);
            long result = commentRepository.insertComment(newComment);
            if (result > 0) {
                logRepository.insertLog(currentUserId, "added comment: \"" + content + "\"", "Task", taskId);
                postCallback(callback, true, "Comment added");
                
                // Reload comments & logs
                commentsLiveData.postValue(commentRepository.getCommentsForTask(taskId));
                logsLiveData.postValue(logRepository.getLogsForTask(taskId));
            } else {
                postCallback(callback, false, "Failed to add comment");
            }
        });
    }

    public void deleteComment(long taskId, Comment comment, ActionCallback callback) {
        executorService.execute(() -> {
            boolean success = commentRepository.deleteComment(comment.getId());
            if (success) {
                long currentUserId = preferencesManager.getCurrentUserId();
                logRepository.insertLog(currentUserId, "deleted a comment", "Task", taskId);
                postCallback(callback, true, "Comment deleted");

                // Reload comments & logs
                commentsLiveData.postValue(commentRepository.getCommentsForTask(taskId));
                logsLiveData.postValue(logRepository.getLogsForTask(taskId));
            } else {
                postCallback(callback, false, "Failed to delete comment");
            }
        });
    }

    public void toggleTaskCompletion(Task task, ActionCallback callback) {
        executorService.execute(() -> {
            boolean nextState = !task.isCompleted();
            task.setCompleted(nextState);
            int rowsAffected = taskRepository.updateTask(task);

            if (rowsAffected > 0) {
                long currentUserId = preferencesManager.getCurrentUserId();
                String actionMessage = nextState ? "marked task as completed" : "marked task as incomplete";
                logRepository.insertLog(currentUserId, actionMessage, "Task", task.getId());

                String feedbackMessage = nextState ? "Task marked as complete" : "Task marked as incomplete";
                postCallback(callback, true, feedbackMessage);

                // Reload task & logs
                taskLiveData.postValue(task);
                logsLiveData.postValue(logRepository.getLogsForTask(task.getId()));
            } else {
                postCallback(callback, false, "Failed to update task");
            }
        });
    }

    public void saveAttachedFile(long taskId, Uri uri, ActionCallback callback) {
        // Runs saveFile asynchronously using the helper's callback, then updates DB log and Livedata
        fileHelper.saveFile(getApplication(), uri, taskId, new FileHelper.FileOperationCallback() {
            @Override
            public void onSuccess(String filePath) {
                executorService.execute(() -> {
                    long currentUserId = preferencesManager.getCurrentUserId();
                    logRepository.insertLog(currentUserId, "attached file: " + uri.getLastPathSegment(), "Task", taskId);

                    // Reload logs and files list
                    logsLiveData.postValue(logRepository.getLogsForTask(taskId));
                    attachedFilesLiveData.postValue(fileHelper.getTaskFiles(getApplication(), taskId));
                    
                    postCallback(callback, true, "File attached successfully");
                });
            }

            @Override
            public void onError(String errorMessage) {
                postCallback(callback, false, errorMessage);
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
