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

public class LoginViewModel extends AndroidViewModel {

    private final UserRepository userRepository;
    private final UserPreferencesManager prefsManager;

    private final MutableLiveData<User> loginSuccessLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> loginErrorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
        this.prefsManager = new UserPreferencesManager(application);
    }

    public LiveData<User> getLoginSuccessLiveData() {
        return loginSuccessLiveData;
    }

    public LiveData<String> getLoginErrorLiveData() {
        return loginErrorLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public boolean isAlreadyLoggedIn() {
        return prefsManager.isLoggedIn();
    }

    public boolean isDarkModeEnabled() {
        return prefsManager.isDarkModeEnabled();
    }

    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            loginErrorLiveData.setValue("Username and password are required");
            return;
        }

        isLoadingLiveData.setValue(true);
        executorService.execute(() -> {
            User user = userRepository.login(username, password);
            isLoadingLiveData.postValue(false);
            if (user != null) {
                prefsManager.saveSession(user.getId(), user.getUsername());
                loginSuccessLiveData.postValue(user);
            } else {
                loginErrorLiveData.postValue("Incorrect username or password");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
