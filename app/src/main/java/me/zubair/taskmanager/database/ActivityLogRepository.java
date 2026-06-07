package me.zubair.taskmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.zubair.taskmanager.model.ActivityLog;

public class ActivityLogRepository {
    private static final String TAG = "ActivityLogRepository";
    private final TaskDbHelper dbHelper;

    public ActivityLogRepository(Context context) {
        this.dbHelper = new TaskDbHelper(context);
    }

    public long insertLog(long userId, String action, String targetType, long targetId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ActivityLogContract.ActivityLogEntry.COLUMN_USER_ID, userId);
        values.put(ActivityLogContract.ActivityLogEntry.COLUMN_ACTION, action);
        values.put(ActivityLogContract.ActivityLogEntry.COLUMN_TARGET_TYPE, targetType);
        values.put(ActivityLogContract.ActivityLogEntry.COLUMN_TARGET_ID, targetId);
        
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put(ActivityLogContract.ActivityLogEntry.COLUMN_CREATED_AT, createdAt);

        return db.insert(ActivityLogContract.ActivityLogEntry.TABLE_NAME, null, values);
    }

    public List<ActivityLog> getLogsForTask(long taskId) {
        List<ActivityLog> logs = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query logs matching target_id = taskId and target_type = 'Task'
        String query = "SELECT l.*, u." + UserContract.UserEntry.COLUMN_USERNAME + 
                       ", u." + UserContract.UserEntry.COLUMN_FULL_NAME + 
                       " FROM " + ActivityLogContract.ActivityLogEntry.TABLE_NAME + " l" +
                       " INNER JOIN " + UserContract.UserEntry.TABLE_NAME + " u" +
                       " ON l." + ActivityLogContract.ActivityLogEntry.COLUMN_USER_ID + " = u." + UserContract.UserEntry._ID +
                       " WHERE l." + ActivityLogContract.ActivityLogEntry.COLUMN_TARGET_ID + " = ?" +
                       " AND l." + ActivityLogContract.ActivityLogEntry.COLUMN_TARGET_TYPE + " = 'Task'" +
                       " ORDER BY l." + ActivityLogContract.ActivityLogEntry._ID + " DESC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(taskId)})) {
            while (cursor.moveToNext()) {
                ActivityLog log = new ActivityLog();
                log.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ActivityLogContract.ActivityLogEntry._ID)));
                log.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(ActivityLogContract.ActivityLogEntry.COLUMN_USER_ID)));
                log.setAction(cursor.getString(cursor.getColumnIndexOrThrow(ActivityLogContract.ActivityLogEntry.COLUMN_ACTION)));
                log.setTargetType(cursor.getString(cursor.getColumnIndexOrThrow(ActivityLogContract.ActivityLogEntry.COLUMN_TARGET_TYPE)));
                log.setTargetId(cursor.getLong(cursor.getColumnIndexOrThrow(ActivityLogContract.ActivityLogEntry.COLUMN_TARGET_ID)));
                log.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(ActivityLogContract.ActivityLogEntry.COLUMN_CREATED_AT)));
                log.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_USERNAME)));
                log.setUserFullName(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_FULL_NAME)));
                logs.add(log);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting activity logs for task: " + e.getMessage());
        }

        return logs;
    }
}
