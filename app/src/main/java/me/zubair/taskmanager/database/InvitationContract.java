package me.zubair.taskmanager.database;

import android.provider.BaseColumns;

public final class InvitationContract {
    private InvitationContract() {}

    public static class InvitationEntry implements BaseColumns {
        public static final String TABLE_NAME = "invitations";
        public static final String COLUMN_PROJECT_ID = "project_id";
        public static final String COLUMN_SENDER_ID = "sender_id";
        public static final String COLUMN_RECEIVER_ID = "receiver_id";
        public static final String COLUMN_STATUS = "status"; // PENDING, ACCEPTED, REJECTED
        public static final String COLUMN_CREATED_AT = "created_at";

        public static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        COLUMN_PROJECT_ID + " INTEGER NOT NULL," +
                        COLUMN_SENDER_ID + " INTEGER NOT NULL," +
                        COLUMN_RECEIVER_ID + " INTEGER NOT NULL," +
                        COLUMN_STATUS + " TEXT NOT NULL," +
                        COLUMN_CREATED_AT + " TEXT," +
                        "FOREIGN KEY (" + COLUMN_PROJECT_ID + ") REFERENCES " +
                        ProjectContract.ProjectEntry.TABLE_NAME + "(" + ProjectContract.ProjectEntry._ID + ") ON DELETE CASCADE," +
                        "FOREIGN KEY (" + COLUMN_SENDER_ID + ") REFERENCES " +
                        UserContract.UserEntry.TABLE_NAME + "(" + UserContract.UserEntry._ID + ") ON DELETE CASCADE," +
                        "FOREIGN KEY (" + COLUMN_RECEIVER_ID + ") REFERENCES " +
                        UserContract.UserEntry.TABLE_NAME + "(" + UserContract.UserEntry._ID + ") ON DELETE CASCADE)";

        public static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
