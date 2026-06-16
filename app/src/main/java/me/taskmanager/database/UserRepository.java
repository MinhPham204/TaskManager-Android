package me.taskmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.taskmanager.model.User;
import me.taskmanager.utils.HashUtils;

/**
 * Repository for all User CRUD + authentication operations.
 * Passwords are never stored in plain-text (Decision 003).
 */
public class UserRepository {

    private static final String TAG = "UserRepository";
    private final TaskDbHelper dbHelper;

    public UserRepository(Context context) {
        this.dbHelper = new TaskDbHelper(context);
    }

    // ─────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────

    /**
     * Registers a new user. Returns the new row ID, or -1 on failure,
     * or -2 if the username already exists.
     */
    public long registerUser(String username, String password, String fullName, String email) {
        if (isUsernameTaken(username)) {
            return -2; // username conflict
        }

        String salt = HashUtils.generateSalt();
        String hash = HashUtils.hashPassword(password, salt);
        if (hash == null) {
            Log.e(TAG, "Hashing failed");
            return -1;
        }

        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        ContentValues values = new ContentValues();
        values.put(UserContract.UserEntry.COLUMN_USERNAME, username.trim().toLowerCase());
        values.put(UserContract.UserEntry.COLUMN_PASSWORD_HASH, hash);
        values.put(UserContract.UserEntry.COLUMN_SALT, salt);
        values.put(UserContract.UserEntry.COLUMN_FULL_NAME, fullName.trim());
        values.put(UserContract.UserEntry.COLUMN_EMAIL, email.trim().toLowerCase());
        values.put(UserContract.UserEntry.COLUMN_CREATED_AT, createdAt);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.insert(UserContract.UserEntry.TABLE_NAME, null, values);
    }

    // ─────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────

    /**
     * Attempts login. Returns the User on success, or null on failure.
     */
    public User login(String username, String password) {
        User user = getUserByUsername(username.trim().toLowerCase());
        if (user == null) return null;

        if (HashUtils.verifyPassword(password, user.getPasswordHash(), user.getSalt())) {
            return user;
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────
    // QUERIES
    // ─────────────────────────────────────────────────────────

    public User getUserById(long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = UserContract.UserEntry._ID + " = ?";
        String[] args = {String.valueOf(userId)};

        try (Cursor cursor = db.query(UserContract.UserEntry.TABLE_NAME,
                null, selection, args, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return extractUserFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getUserById error: " + e.getMessage());
        }
        return null;
    }

    public User getUserByUsername(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = UserContract.UserEntry.COLUMN_USERNAME + " = ?";
        String[] args = {username.toLowerCase()};

        try (Cursor cursor = db.query(UserContract.UserEntry.TABLE_NAME,
                null, selection, args, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return extractUserFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getUserByUsername error: " + e.getMessage());
        }
        return null;
    }

    public boolean isUsernameTaken(String username) {
        return getUserByUsername(username) != null;
    }

    // ─────────────────────────────────────────────────────────
    // UPDATE PROFILE
    // ─────────────────────────────────────────────────────────

    /**
     * Updates full_name and email for the given user ID.
     */
    public boolean updateProfile(long userId, String fullName, String email) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserContract.UserEntry.COLUMN_FULL_NAME, fullName.trim());
        values.put(UserContract.UserEntry.COLUMN_EMAIL, email.trim().toLowerCase());

        String selection = UserContract.UserEntry._ID + " = ?";
        String[] args = {String.valueOf(userId)};
        return db.update(UserContract.UserEntry.TABLE_NAME, values, selection, args) > 0;
    }

    /**
     * Changes the password for the given user ID.
     * Verifies the old password before updating.
     * Returns true if successful.
     */
    public boolean changePassword(long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) return false;

        if (!HashUtils.verifyPassword(oldPassword, user.getPasswordHash(), user.getSalt())) {
            return false; // old password wrong
        }

        String newSalt = HashUtils.generateSalt();
        String newHash = HashUtils.hashPassword(newPassword, newSalt);
        if (newHash == null) return false;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserContract.UserEntry.COLUMN_PASSWORD_HASH, newHash);
        values.put(UserContract.UserEntry.COLUMN_SALT, newSalt);

        String selection = UserContract.UserEntry._ID + " = ?";
        String[] args = {String.valueOf(userId)};
        return db.update(UserContract.UserEntry.TABLE_NAME, values, selection, args) > 0;
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private User extractUserFromCursor(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(UserContract.UserEntry._ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_USERNAME)));
        user.setPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_PASSWORD_HASH)));
        user.setSalt(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_SALT)));
        user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_FULL_NAME)));
        user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_EMAIL)));
        user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_CREATED_AT)));
        return user;
    }
}
