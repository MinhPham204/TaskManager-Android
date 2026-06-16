package me.taskmanager.database;

import android.provider.BaseColumns;

/**
 * Contract class for the User database table
 */
public final class UserContract {
    private UserContract() {}

    public static class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_PASSWORD_HASH = "password_hash";
        public static final String COLUMN_SALT = "salt";
        public static final String COLUMN_FULL_NAME = "full_name";
        public static final String COLUMN_EMAIL = "email";
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_USERNAME + " TEXT NOT NULL UNIQUE," +
                        COLUMN_PASSWORD_HASH + " TEXT NOT NULL," +
                        COLUMN_SALT + " TEXT NOT NULL," +
                        COLUMN_FULL_NAME + " TEXT," +
                        COLUMN_EMAIL + " TEXT," +
                        COLUMN_CREATED_AT + " TEXT)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
