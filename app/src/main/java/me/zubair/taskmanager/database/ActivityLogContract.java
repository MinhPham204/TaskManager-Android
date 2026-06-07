package me.zubair.taskmanager.database;

import android.provider.BaseColumns;

public final class ActivityLogContract {
    private ActivityLogContract() {}

    public static class ActivityLogEntry implements BaseColumns {
        public static final String TABLE_NAME = "activity_logs";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_ACTION = "action";
        public static final String COLUMN_TARGET_TYPE = "target_type";
        public static final String COLUMN_TARGET_ID = "target_id";
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_USER_ID + " INTEGER NOT NULL," +
                        COLUMN_ACTION + " TEXT NOT NULL," +
                        COLUMN_TARGET_TYPE + " TEXT NOT NULL," +
                        COLUMN_TARGET_ID + " INTEGER NOT NULL," +
                        COLUMN_CREATED_AT + " TEXT," +
                        "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " +
                        UserContract.UserEntry.TABLE_NAME + "(" + UserContract.UserEntry._ID + ") ON DELETE CASCADE)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
