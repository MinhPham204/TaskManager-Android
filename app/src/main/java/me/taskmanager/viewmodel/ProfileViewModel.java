package me.taskmanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.taskmanager.database.UserRepository;
import me.taskmanager.model.User;
import me.taskmanager.preferences.UserPreferencesManager;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final UserPreferencesManager preferencesManager;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> logoutLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.preferencesManager = new UserPreferencesManager(application);
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getLogoutLiveData() {
        return logoutLiveData;
    }

    public void loadUserProfile() {
        executorService.execute(() -> {
            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId == -1) {
                logoutLiveData.postValue(true);
                return;
            }

            User user = userRepository.getUserById(currentUserId);
            if (user == null) {
                logoutLiveData.postValue(true);
            } else {
                userLiveData.postValue(user);
            }
        });
    }

    public void clearPreferences(User currentUser) {
        executorService.execute(() -> {
            preferencesManager.clearPreferences();
            if (currentUser != null) {
                preferencesManager.saveSession(currentUser.getId(), currentUser.getUsername());
            }
        });
    }

    public void setNotificationsEnabled(boolean enabled) {
        preferencesManager.setNotificationsEnabled(enabled);
    }

    public boolean areNotificationsEnabled() {
        return preferencesManager.areNotificationsEnabled();
    }

    public void setDarkModeEnabled(boolean enabled) {
        preferencesManager.setDarkModeEnabled(enabled);
    }

    public boolean isDarkModeEnabled() {
        return preferencesManager.isDarkModeEnabled();
    }

    public void clearSession() {
        preferencesManager.clearSession();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
