package me.taskmanager.database;

import android.provider.BaseColumns;

public final class CommentContract {
    private CommentContract() {}

    public static class CommentEntry implements BaseColumns {
        public static final String TABLE_NAME = "comments";
        public static final String COLUMN_TASK_ID = "task_id";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_CONTENT = "content";
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_TASK_ID + " INTEGER NOT NULL," +
                        COLUMN_USER_ID + " INTEGER NOT NULL," +
                        COLUMN_CONTENT + " TEXT NOT NULL," +
                        COLUMN_CREATED_AT + " TEXT," +
                        "FOREIGN KEY (" + COLUMN_TASK_ID + ") REFERENCES " +
                        TaskContract.TaskEntry.TABLE_NAME + "(" + TaskContract.TaskEntry._ID + ") ON DELETE CASCADE," +
                        "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " +
                        UserContract.UserEntry.TABLE_NAME + "(" + UserContract.UserEntry._ID + ") ON DELETE CASCADE)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
