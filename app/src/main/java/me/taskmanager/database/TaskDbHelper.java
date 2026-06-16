package me.taskmanager.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Database helper class for managing SQLite operations.
 *
 * Version history:
 *  v1 → initial schema (tasks table)
 *  v2 → added created_at to tasks
 *  v3 → added users table (Phase 2)
 *  v4 → added projects and project_members tables (Phase 3)
 *  v5 → upgraded tasks table to support projects, assignees, status, and updated_at (Phase 4)
 */
public class TaskDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "TaskDbHelper";
    private static final String DATABASE_NAME = "tasks.db";
    private static final int DATABASE_VERSION = 7;

    public TaskDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Enable foreign key constraints (Decision 004)
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TaskContract.TaskEntry.SQL_CREATE_ENTRIES);
        db.execSQL(UserContract.UserEntry.SQL_CREATE_ENTRIES);
        db.execSQL(ProjectContract.ProjectEntry.SQL_CREATE_ENTRIES);
        db.execSQL(ProjectMemberContract.ProjectMemberEntry.SQL_CREATE_ENTRIES);
        db.execSQL(CommentContract.CommentEntry.SQL_CREATE_ENTRIES);
        db.execSQL(ActivityLogContract.ActivityLogEntry.SQL_CREATE_ENTRIES);
        db.execSQL(InvitationContract.InvitationEntry.SQL_CREATE_ENTRIES);
        Log.i(TAG, "Database created at version " + DATABASE_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Upgrading database from v" + oldVersion + " to v" + newVersion);

        if (oldVersion < 2) {
            // v1 → v2: add created_at to tasks
            try {
                db.execSQL("ALTER TABLE " + TaskContract.TaskEntry.TABLE_NAME +
                        " ADD COLUMN " + TaskContract.TaskEntry.COLUMN_CREATED_AT + " INTEGER;");
                Log.i(TAG, "v2: added created_at to tasks");
            } catch (Exception e) {
                Log.e(TAG, "v2 migration error: " + e.getMessage());
            }
        }

        if (oldVersion < 3) {
            // v2 → v3: create users table
            try {
                db.execSQL(UserContract.UserEntry.SQL_CREATE_ENTRIES);
                Log.i(TAG, "v3: created users table");
            } catch (Exception e) {
                Log.e(TAG, "v3 migration error: " + e.getMessage());
            }
        }

        if (oldVersion < 4) {
            // v3 → v4: create projects and project_members tables
            try {
                db.execSQL(ProjectContract.ProjectEntry.SQL_CREATE_ENTRIES);
                db.execSQL(ProjectMemberContract.ProjectMemberEntry.SQL_CREATE_ENTRIES);
                Log.i(TAG, "v4: created projects and project_members tables");
            } catch (Exception e) {
                Log.e(TAG, "v4 migration error: " + e.getMessage());
            }
        }

        if (oldVersion < 5) {
            // v4 → v5: migrate tasks table to add project_id, assigned_user_id, status, updated_at
            try {
                // 1. Rename existing tasks table
                db.execSQL("ALTER TABLE " + TaskContract.TaskEntry.TABLE_NAME + " RENAME TO tasks_old;");
                
                // 2. Create the new tasks table
                db.execSQL(TaskContract.TaskEntry.SQL_CREATE_ENTRIES);
                
                // 3. Copy data from old table to new table (handling status mapping based on completed column)
                db.execSQL("INSERT INTO " + TaskContract.TaskEntry.TABLE_NAME + " (" +
                        TaskContract.TaskEntry._ID + ", " +
                        TaskContract.TaskEntry.COLUMN_TITLE + ", " +
                        TaskContract.TaskEntry.COLUMN_DESCRIPTION + ", " +
                        TaskContract.TaskEntry.COLUMN_DUE_DATE + ", " +
                        TaskContract.TaskEntry.COLUMN_PRIORITY + ", " +
                        TaskContract.TaskEntry.COLUMN_COMPLETED + ", " +
                        TaskContract.TaskEntry.COLUMN_CREATED_AT + ", " +
                        TaskContract.TaskEntry.COLUMN_STATUS + ") " +
                        "SELECT " +
                        TaskContract.TaskEntry._ID + ", " +
                        TaskContract.TaskEntry.COLUMN_TITLE + ", " +
                        TaskContract.TaskEntry.COLUMN_DESCRIPTION + ", " +
                        TaskContract.TaskEntry.COLUMN_DUE_DATE + ", " +
                        TaskContract.TaskEntry.COLUMN_PRIORITY + ", " +
                        TaskContract.TaskEntry.COLUMN_COMPLETED + ", " +
                        TaskContract.TaskEntry.COLUMN_CREATED_AT + ", " +
                        "CASE WHEN " + TaskContract.TaskEntry.COLUMN_COMPLETED + " = 1 THEN 'DONE' ELSE 'TODO' END " +
                        "FROM tasks_old;");
                
                // 4. Drop the old table
                db.execSQL("DROP TABLE tasks_old;");
                Log.i(TAG, "v5: migrated tasks table successfully");
            } catch (Exception e) {
                Log.e(TAG, "v5 migration error: " + e.getMessage());
            }
        }

        if (oldVersion < 6) {
            // v5 → v6: create comments and activity_logs tables
            try {
                db.execSQL(CommentContract.CommentEntry.SQL_CREATE_ENTRIES);
                db.execSQL(ActivityLogContract.ActivityLogEntry.SQL_CREATE_ENTRIES);
                Log.i(TAG, "v6: created comments and activity_logs tables");
            } catch (Exception e) {
                Log.e(TAG, "v6 migration error: " + e.getMessage());
            }
        }

        if (oldVersion < 7) {
            // v6 → v7: create invitations table
            try {
                db.execSQL(InvitationContract.InvitationEntry.SQL_CREATE_ENTRIES);
                Log.i(TAG, "v7: created invitations table");
            } catch (Exception e) {
                Log.e(TAG, "v7 migration error: " + e.getMessage());
            }
        }
    }
}
