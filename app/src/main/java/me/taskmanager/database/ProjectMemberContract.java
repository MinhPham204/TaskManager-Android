package me.taskmanager.database;

import android.provider.BaseColumns;

/**
 * Contract class for the ProjectMember database table
 */
public final class ProjectMemberContract {
    private ProjectMemberContract() {}

    public static class ProjectMemberEntry implements BaseColumns {
        public static final String TABLE_NAME = "project_members";
        public static final String COLUMN_PROJECT_ID = "project_id";
        public static final String COLUMN_USER_ID = "user_id";
        public static final String COLUMN_ROLE = "role";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_PROJECT_ID + " INTEGER NOT NULL," +
                        COLUMN_USER_ID + " INTEGER NOT NULL," +
                        COLUMN_ROLE + " TEXT," +
                        "FOREIGN KEY (" + COLUMN_PROJECT_ID + ") REFERENCES " +
                        ProjectContract.ProjectEntry.TABLE_NAME + "(" + ProjectContract.ProjectEntry._ID + ") ON DELETE CASCADE," +
                        "FOREIGN KEY (" + COLUMN_USER_ID + ") REFERENCES " +
                        UserContract.UserEntry.TABLE_NAME + "(" + UserContract.UserEntry._ID + ") ON DELETE CASCADE)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
