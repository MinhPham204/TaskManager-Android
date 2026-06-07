package me.zubair.taskmanager.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import me.zubair.taskmanager.R;
import me.zubair.taskmanager.fragments.HomeFragment;
import me.zubair.taskmanager.fragments.ProfileFragment;
import me.zubair.taskmanager.fragments.EditProfileFragment;
import me.zubair.taskmanager.fragments.ChangePasswordFragment;
import me.zubair.taskmanager.fragments.ProjectListFragment;
import me.zubair.taskmanager.fragments.ProjectDetailsFragment;
import me.zubair.taskmanager.fragments.TaskListFragment;
import me.zubair.taskmanager.fragments.CalendarFragment;
import me.zubair.taskmanager.fragments.TaskDetailsFragment;
import me.zubair.taskmanager.preferences.UserPreferencesManager;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private UserPreferencesManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefsManager = new UserPreferencesManager(this);

        // Apply dark mode preference on startup
        if (prefsManager.isDarkModeEnabled()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Gate: if not logged in, redirect to Login
        if (!prefsManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (itemId == R.id.nav_projects) {
                    selectedFragment = new ProjectListFragment();
                } else if (itemId == R.id.nav_tasks) {
                    selectedFragment = new TaskListFragment();
                } else if (itemId == R.id.nav_calendar) {
                    selectedFragment = new CalendarFragment();
                } else if (itemId == R.id.nav_profile) {
                    selectedFragment = new ProfileFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    // ─────────────────────────────────────────────────────────
    // OPTIONS MENU (Logout)
    // ─────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> logout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        prefsManager.clearSession();
        goToLogin();
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ─────────────────────────────────────────────────────────
    // BACK PRESS
    // ─────────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
            fragmentManager.popBackStack();
            if (currentFragment instanceof EditProfileFragment || currentFragment instanceof ChangePasswordFragment) {
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            } else if (currentFragment instanceof ProjectDetailsFragment) {
                bottomNavigationView.setSelectedItemId(R.id.nav_projects);
            } else if (currentFragment instanceof TaskDetailsFragment && 
                       currentFragment.getArguments() != null && 
                       currentFragment.getArguments().getBoolean("FROM_CALENDAR", false)) {
                bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
            } else {
                bottomNavigationView.setSelectedItemId(R.id.nav_tasks);
            }
            return;
        }

        int selectedItemId = bottomNavigationView.getSelectedItemId();
        if (selectedItemId != R.id.nav_home) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}
