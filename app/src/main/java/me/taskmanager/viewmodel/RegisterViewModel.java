package me.taskmanager.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import me.taskmanager.database.UserRepository;

public class RegisterViewModel extends AndroidViewModel {

    private final UserRepository userRepository;

    private final MutableLiveData<Long> registerResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public RegisterViewModel(@NonNull Application application) {
        super(application);
        this.userRepository = new UserRepository(application);
    }

    public LiveData<Long> getRegisterResultLiveData() {
        return registerResultLiveData;
    }

    public LiveData<Boolean> getIsLoadingLiveData() {
        return isLoadingLiveData;
    }

    public void register(String username, String password, String fullName, String email) {
        isLoadingLiveData.setValue(true);
        executorService.execute(() -> {
            long result = userRepository.registerUser(username, password, fullName, email);
            isLoadingLiveData.postValue(false);
            registerResultLiveData.postValue(result);
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
