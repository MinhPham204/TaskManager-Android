package me.taskmanager.preferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manager class for user preferences and session management.
 *
 * Session is stored as current_user_id (-1 = no session).
 */
public class UserPreferencesManager {

    private static final String PREF_NAME = "task_manager_preferences";

    // Notification
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    // Session (Phase 2)
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_CURRENT_USERNAME = "current_username";
    private static final long NO_USER = -1L;

    private final SharedPreferences sharedPreferences;

    public UserPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─────────────────────────────────────────────────────────
    // NOTIFICATION SETTINGS
    // ─────────────────────────────────────────────────────────

    public boolean areNotificationsEnabled() {
        return sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    // ─────────────────────────────────────────────────────────
    // DARK MODE SETTINGS
    // ─────────────────────────────────────────────────────────

    public boolean isDarkModeEnabled() {
        return sharedPreferences.getBoolean("dark_mode_enabled", false);
    }

    public void setDarkModeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean("dark_mode_enabled", enabled).apply();
    }

    // ─────────────────────────────────────────────────────────
    // SESSION MANAGEMENT (Phase 2)
    // ─────────────────────────────────────────────────────────

    /** Returns true if a user is currently logged in */
    public boolean isLoggedIn() {
        return sharedPreferences.getLong(KEY_CURRENT_USER_ID, NO_USER) != NO_USER;
    }

    /** Returns the current logged-in user's ID, or -1 if not logged in */
    public long getCurrentUserId() {
        return sharedPreferences.getLong(KEY_CURRENT_USER_ID, NO_USER);
    }

    /** Returns the current logged-in user's username, or empty string */
    public String getCurrentUsername() {
        return sharedPreferences.getString(KEY_CURRENT_USERNAME, "");
    }

    /** Saves the session after a successful login */
    public void saveSession(long userId, String username) {
        sharedPreferences.edit()
                .putLong(KEY_CURRENT_USER_ID, userId)
                .putString(KEY_CURRENT_USERNAME, username)
                .apply();
    }

    /** Clears the session (logout) */
    public void clearSession() {
        sharedPreferences.edit()
                .remove(KEY_CURRENT_USER_ID)
                .remove(KEY_CURRENT_USERNAME)
                .apply();
    }

    // ─────────────────────────────────────────────────────────
    // CLEAR ALL
    // ─────────────────────────────────────────────────────────

    public void clearPreferences() {
        sharedPreferences.edit().clear().apply();
    }
}