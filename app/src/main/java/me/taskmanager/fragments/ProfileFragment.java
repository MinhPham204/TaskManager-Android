package me.taskmanager.fragments;

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

import me.taskmanager.R;
import me.taskmanager.activity.LoginActivity;
import me.taskmanager.model.User;
import me.taskmanager.services.NotificationService;
import me.taskmanager.viewmodel.ProfileViewModel;

import androidx.lifecycle.ViewModelProvider;

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

    private ProfileViewModel profileViewModel;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

        // Setup ViewModel
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        // Observe data
        profileViewModel.getUserLiveData().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            updateUserProfile(user);
        });

        profileViewModel.getLogoutLiveData().observe(getViewLifecycleOwner(), shouldLogout -> {
            if (shouldLogout != null && shouldLogout) {
                forceLogout();
            }
        });

        // Load data
        profileViewModel.loadUserProfile();

        // Bind events
        setupListeners();
    }

    private void updateUserProfile(User user) {
        if (user == null) return;

        // Fill user details
        String displayName = user.getDisplayName();
        tvProfileName.setText(displayName);
        tvProfileUsername.setText("@" + user.getUsername());
        
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            tvEmailVal.setText(user.getEmail());
        } else {
            tvEmailVal.setText("No email set");
        }

        if (user.getFullName() != null && !user.getFullName().isEmpty()) {
            tvFullNameVal.setText(user.getFullName());
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
        if (user.getCreatedAt() != null) {
            String date = user.getCreatedAt();
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
        switchNotifications.setChecked(profileViewModel.areNotificationsEnabled());
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            profileViewModel.setNotificationsEnabled(isChecked);
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
        switchDarkMode.setChecked(profileViewModel.isDarkModeEnabled());
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            profileViewModel.setDarkModeEnabled(isChecked);
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
                    // Reset preferences via VM
                    profileViewModel.clearPreferences(currentUser);

                    // Reload switch UI state after clear
                    switchNotifications.setChecked(profileViewModel.areNotificationsEnabled());
                    switchDarkMode.setChecked(profileViewModel.isDarkModeEnabled());

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
        profileViewModel.clearSession();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
