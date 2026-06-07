package me.zubair.taskmanager.fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import me.zubair.taskmanager.R;
import me.zubair.taskmanager.activity.LoginActivity;
import me.zubair.taskmanager.database.UserRepository;
import me.zubair.taskmanager.model.User;
import me.zubair.taskmanager.preferences.UserPreferencesManager;
import me.zubair.taskmanager.services.NotificationService;

/**
 * Fragment for displaying user profile, edit profile options, changing password,
 * and managing account preferences (notifications, clearing data, logout).
 */
public class ProfileFragment extends Fragment {

    private TextView tvAvatarText;
    private TextView tvProfileName;
    private TextView tvProfileUsername;
    private TextView tvProfileJoined;
    private TextView tvEmailVal;
    private TextView tvFullNameVal;

    private View layoutEditProfile;
    private View layoutChangePassword;
    private Switch switchNotifications;
    private Switch switchDarkMode;
    private View layoutClearData;
    private View layoutLogout;

    private UserPreferencesManager preferencesManager;
    private UserRepository userRepository;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Managers
        preferencesManager = new UserPreferencesManager(requireContext());
        userRepository = new UserRepository(requireContext());

        // Bind views
        tvAvatarText = view.findViewById(R.id.tv_avatar_text);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileUsername = view.findViewById(R.id.tv_profile_username);
        tvProfileJoined = view.findViewById(R.id.tv_profile_joined);
        tvEmailVal = view.findViewById(R.id.tv_email_val);
        tvFullNameVal = view.findViewById(R.id.tv_fullname_val);

        layoutEditProfile = view.findViewById(R.id.layout_edit_profile);
        layoutChangePassword = view.findViewById(R.id.layout_change_password);
        switchNotifications = view.findViewById(R.id.switch_notifications);
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);
        layoutClearData = view.findViewById(R.id.layout_clear_data);
        layoutLogout = view.findViewById(R.id.layout_logout);

        // Load data
        loadUserProfile();

        // Bind events
        setupListeners();
    }

    private void loadUserProfile() {
        long currentUserId = preferencesManager.getCurrentUserId();
        if (currentUserId == -1) {
            forceLogout();
            return;
        }

        currentUser = userRepository.getUserById(currentUserId);
        if (currentUser == null) {
            forceLogout();
            return;
        }

        // Fill user details
        String displayName = currentUser.getDisplayName();
        tvProfileName.setText(displayName);
        tvProfileUsername.setText("@" + currentUser.getUsername());
        
        if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
            tvEmailVal.setText(currentUser.getEmail());
        } else {
            tvEmailVal.setText("No email set");
        }

        if (currentUser.getFullName() != null && !currentUser.getFullName().isEmpty()) {
            tvFullNameVal.setText(currentUser.getFullName());
        } else {
            tvFullNameVal.setText("No full name set");
        }

        // Avatar Letter
        if (displayName != null && !displayName.isEmpty()) {
            String initial = displayName.substring(0, 1).toUpperCase();
            tvAvatarText.setText(initial);
        } else {
            tvAvatarText.setText("U");
        }

        // Join Date
        if (currentUser.getCreatedAt() != null) {
            String date = currentUser.getCreatedAt();
            if (date.length() >= 10) {
                date = date.substring(0, 10); // Format: YYYY-MM-DD
            }
            tvProfileJoined.setText("Member since " + date);
        } else {
            tvProfileJoined.setText("Member");
        }
    }

    private void setupListeners() {
        // Edit Profile
        layoutEditProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EditProfileFragment())
                    .addToBackStack("profile_flow")
                    .commit();
        });

        // Change Password
        layoutChangePassword.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ChangePasswordFragment())
                    .addToBackStack("profile_flow")
                    .commit();
        });

        // Notifications Preference
        switchNotifications.setChecked(preferencesManager.areNotificationsEnabled());
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setNotificationsEnabled(isChecked);
            Intent serviceIntent = new Intent(requireContext(), NotificationService.class);

            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requireContext().startForegroundService(serviceIntent);
                } else {
                    requireContext().startService(serviceIntent);
                }
                Toast.makeText(requireContext(), "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                requireContext().stopService(serviceIntent);
                Toast.makeText(requireContext(), "Notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Dark Mode Preference
        switchDarkMode.setChecked(preferencesManager.isDarkModeEnabled());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDarkModeEnabled(isChecked);
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES 
                              : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(requireContext(), isChecked ? "Dark mode enabled" : "Light mode enabled", Toast.LENGTH_SHORT).show();
        });

        // Clear App Data (Reset settings only, keeping accounts/tasks intact)
        layoutClearData.setOnClickListener(v -> showClearDataConfirmation());

        // Logout
        layoutLogout.setOnClickListener(v -> confirmLogout());
    }

    private void showClearDataConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Reset App Settings")
                .setMessage("This will reset all preferences. Are you sure you want to continue?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    // Reset preferences (will disable notification toggle, etc.)
                    preferencesManager.clearPreferences();
                    
                    // Re-save session so user stays logged in
                    if (currentUser != null) {
                        preferencesManager.saveSession(currentUser.getId(), currentUser.getUsername());
                    }

                    // Reload switch UI state
                    switchNotifications.setChecked(preferencesManager.areNotificationsEnabled());
                    switchDarkMode.setChecked(preferencesManager.isDarkModeEnabled());

                    Toast.makeText(requireContext(), "Settings reset successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> forceLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void forceLogout() {
        preferencesManager.clearSession();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
