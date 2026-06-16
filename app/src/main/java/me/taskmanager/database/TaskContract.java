package me.taskmanager.database;

import android.provider.BaseColumns;

/**
 * Contract class for the Task database table
 */
public final class TaskContract {
    private TaskContract() {}

    public static class TaskEntry implements BaseColumns {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_DUE_DATE = "due_date";
        public static final String COLUMN_PRIORITY = "priority";
        public static final String COLUMN_COMPLETED = "completed";
        public static final String COLUMN_CREATED_AT = "created_at";
        
        // Redesigned columns (Phase 4)
        public static final String COLUMN_PROJECT_ID = "project_id";
        public static final String COLUMN_ASSIGNED_USER_ID = "assigned_user_id";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_UPDATED_AT = "updated_at";
        
        // SQL statement to create the table
        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_TITLE + " TEXT NOT NULL," +
                        COLUMN_DESCRIPTION + " TEXT," +
                        COLUMN_DUE_DATE + " INTEGER," +
                        COLUMN_PRIORITY + " INTEGER DEFAULT 1," +
                        COLUMN_COMPLETED + " INTEGER DEFAULT 0," +
                        COLUMN_CREATED_AT + " INTEGER," +
                        COLUMN_PROJECT_ID + " INTEGER," +
                        COLUMN_ASSIGNED_USER_ID + " INTEGER," +
                        COLUMN_STATUS + " TEXT DEFAULT 'TODO'," +
                        COLUMN_UPDATED_AT + " INTEGER," +
                        "FOREIGN KEY (" + COLUMN_PROJECT_ID + ") REFERENCES " +
                        ProjectContract.ProjectEntry.TABLE_NAME + "(" + ProjectContract.ProjectEntry._ID + ") ON DELETE CASCADE," +
                        "FOREIGN KEY (" + COLUMN_ASSIGNED_USER_ID + ") REFERENCES " +
                        UserContract.UserEntry.TABLE_NAME + "(" + UserContract.UserEntry._ID + ") ON DELETE SET NULL)";
        
        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
