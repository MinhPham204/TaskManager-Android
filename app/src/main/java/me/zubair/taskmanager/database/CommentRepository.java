package me.zubair.taskmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import me.zubair.taskmanager.model.Comment;

public class CommentRepository {
    private static final String TAG = "CommentRepository";
    private final TaskDbHelper dbHelper;

    public CommentRepository(Context context) {
        this.dbHelper = new TaskDbHelper(context);
    }

    public long insertComment(Comment comment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CommentContract.CommentEntry.COLUMN_TASK_ID, comment.getTaskId());
        values.put(CommentContract.CommentEntry.COLUMN_USER_ID, comment.getUserId());
        values.put(CommentContract.CommentEntry.COLUMN_CONTENT, comment.getContent());
        values.put(CommentContract.CommentEntry.COLUMN_CREATED_AT, comment.getCreatedAt());
        return db.insert(CommentContract.CommentEntry.TABLE_NAME, null, values);
    }

    public boolean deleteComment(long commentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = CommentContract.CommentEntry._ID + " = ?";
        String[] args = {String.valueOf(commentId)};
        return db.delete(CommentContract.CommentEntry.TABLE_NAME, selection, args) > 0;
    }

    public List<Comment> getCommentsForTask(long taskId) {
        List<Comment> comments = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query joining comments table with users table
        String query = "SELECT c.*, u." + UserContract.UserEntry.COLUMN_USERNAME + 
                       ", u." + UserContract.UserEntry.COLUMN_FULL_NAME + 
                       " FROM " + CommentContract.CommentEntry.TABLE_NAME + " c" +
                       " INNER JOIN " + UserContract.UserEntry.TABLE_NAME + " u" +
                       " ON c." + CommentContract.CommentEntry.COLUMN_USER_ID + " = u." + UserContract.UserEntry._ID +
                       " WHERE c." + CommentContract.CommentEntry.COLUMN_TASK_ID + " = ?" +
                       " ORDER BY c." + CommentContract.CommentEntry._ID + " ASC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(taskId)})) {
            while (cursor.moveToNext()) {
                Comment comment = new Comment();
                comment.setId(cursor.getLong(cursor.getColumnIndexOrThrow(CommentContract.CommentEntry._ID)));
                comment.setTaskId(cursor.getLong(cursor.getColumnIndexOrThrow(CommentContract.CommentEntry.COLUMN_TASK_ID)));
                comment.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(CommentContract.CommentEntry.COLUMN_USER_ID)));
                comment.setContent(cursor.getString(cursor.getColumnIndexOrThrow(CommentContract.CommentEntry.COLUMN_CONTENT)));
                comment.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(CommentContract.CommentEntry.COLUMN_CREATED_AT)));
                comment.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_USERNAME)));
                comment.setUserFullName(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_FULL_NAME)));
                comments.add(comment);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting comments for task: " + e.getMessage());
        }

        return comments;
    }
}
