package me.zubair.taskmanager.database;

import android.provider.BaseColumns;

/**
 * Contract class for the Project database table
 */
public final class ProjectContract {
    private ProjectContract() {}

    public static class ProjectEntry implements BaseColumns {
        public static final String TABLE_NAME = "projects";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_STATUS = "status";
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_NAME + " TEXT NOT NULL," +
                        COLUMN_DESCRIPTION + " TEXT," +
                        COLUMN_STATUS + " TEXT," +
                        COLUMN_CREATED_AT + " TEXT)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
