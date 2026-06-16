package me.taskmanager.fragments;

import android.os.Bundle;
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

import me.taskmanager.R;
import me.taskmanager.database.UserRepository;
import me.taskmanager.preferences.UserPreferencesManager;

public class ChangePasswordFragment extends Fragment {

    private ImageButton btnBack;
    private TextInputEditText etOldPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private TextView tvError;
    private MaterialButton btnSave;
    private MaterialButton btnCancel;

    private UserPreferencesManager preferencesManager;
    private UserRepository userRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_change_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind managers
        preferencesManager = new UserPreferencesManager(requireContext());
        userRepository = new UserRepository(requireContext());

        // Bind views
        btnBack = view.findViewById(R.id.btn_back);
        etOldPassword = view.findViewById(R.id.et_old_password);
        etNewPassword = view.findViewById(R.id.et_new_password);
        etConfirmPassword = view.findViewById(R.id.et_confirm_password);
        tvError = view.findViewById(R.id.tv_error);
        btnSave = view.findViewById(R.id.btn_save);
        btnCancel = view.findViewById(R.id.btn_cancel);

        // Listeners
        btnBack.setOnClickListener(v -> navigateBack());
        btnCancel.setOnClickListener(v -> navigateBack());
        btnSave.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String oldPassword = etOldPassword.getText().toString();
        String newPassword = etNewPassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        // Validate fields
        if (oldPassword.isEmpty()) {
            showError("Current password is required");
            return;
        }

        if (newPassword.isEmpty()) {
            showError("New password is required");
            return;
        }

        if (newPassword.length() < 6) {
            showError("New password must be at least 6 characters");
            return;
        }

        if (confirmPassword.isEmpty()) {
            showError("Please confirm your new password");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showError("New passwords do not match");
            return;
        }

        long userId = preferencesManager.getCurrentUserId();
        if (userId == -1) {
            showError("Invalid session. Please relogin.");
            return;
        }

        // Call repository to verify and change password
        boolean success = userRepository.changePassword(userId, oldPassword, newPassword);
        if (success) {
            Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show();
            navigateBack();
        } else {
            showError("Incorrect current password or update failed");
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
