package me.zubair.taskmanager.fragments;

import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import me.zubair.taskmanager.R;
import me.zubair.taskmanager.database.UserRepository;
import me.zubair.taskmanager.model.User;
import me.zubair.taskmanager.preferences.UserPreferencesManager;

public class EditProfileFragment extends Fragment {

    private ImageButton btnBack;
    private TextInputEditText etFullName;
    private TextInputEditText etEmail;
    private TextView tvError;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;

    private UserPreferencesManager preferencesManager;
    private UserRepository userRepository;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind managers
        preferencesManager = new UserPreferencesManager(requireContext());
        userRepository = new UserRepository(requireContext());

        // Bind views
        btnBack = view.findViewById(R.id.btn_back);
        etFullName = view.findViewById(R.id.et_fullname);
        etEmail = view.findViewById(R.id.et_email);
        tvError = view.findViewById(R.id.tv_error);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);

        // Load current data
        loadCurrentData();

        // Listeners
        btnBack.setOnClickListener(v -> navigateBack());
        btnCancel.setOnClickListener(v -> navigateBack());
        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadCurrentData() {
        long userId = preferencesManager.getCurrentUserId();
        if (userId != -1) {
            currentUser = userRepository.getUserById(userId);
            if (currentUser != null) {
                etFullName.setText(currentUser.getFullName());
                etEmail.setText(currentUser.getEmail());
            }
        }
    }

    private void saveProfileChanges() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        // Validate
        if (fullName.isEmpty()) {
            showError("Full name is required");
            return;
        }

        if (email.isEmpty()) {
            showError("Email address is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Please enter a valid email address");
            return;
        }

        long userId = preferencesManager.getCurrentUserId();
        if (userId == -1 || currentUser == null) {
            showError("Invalid user session. Please relogin.");
            return;
        }

        boolean success = userRepository.updateProfile(userId, fullName, email);
        if (success) {
            Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
            navigateBack();
        } else {
            showError("Failed to update profile. Please try again.");
        }
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void navigateBack() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().popBackStack();
        }
    }
}
