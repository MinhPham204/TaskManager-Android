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

import me.zubair.taskmanager.model.Invitation;

public class InvitationRepository {
    private static final String TAG = "InvitationRepository";
    private final TaskDbHelper dbHelper;

    public InvitationRepository(Context context) {
        this.dbHelper = new TaskDbHelper(context);
    }

    /**
     * Sends a new invitation with status 'PENDING'.
     */
    public long insertInvitation(long projectId, long senderId, long receiverId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(InvitationContract.InvitationEntry.COLUMN_PROJECT_ID, projectId);
        values.put(InvitationContract.InvitationEntry.COLUMN_SENDER_ID, senderId);
        values.put(InvitationContract.InvitationEntry.COLUMN_RECEIVER_ID, receiverId);
        values.put(InvitationContract.InvitationEntry.COLUMN_STATUS, "PENDING");
        
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        values.put(InvitationContract.InvitationEntry.COLUMN_CREATED_AT, createdAt);

        return db.insert(InvitationContract.InvitationEntry.TABLE_NAME, null, values);
    }

    /**
     * Checks if a pending invitation already exists for this project and receiver.
     */
    public boolean hasPendingInvitation(long projectId, long receiverId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = InvitationContract.InvitationEntry.COLUMN_PROJECT_ID + " = ? AND " +
                InvitationContract.InvitationEntry.COLUMN_RECEIVER_ID + " = ? AND " +
                InvitationContract.InvitationEntry.COLUMN_STATUS + " = 'PENDING'";
        String[] args = {String.valueOf(projectId), String.valueOf(receiverId)};

        try (Cursor cursor = db.query(InvitationContract.InvitationEntry.TABLE_NAME,
                new String[]{InvitationContract.InvitationEntry._ID},
                selection, args, null, null, null)) {
            return cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "hasPendingInvitation error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Updates status of an invitation ('ACCEPTED', 'REJECTED').
     */
    public boolean updateInvitationStatus(long invitationId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(InvitationContract.InvitationEntry.COLUMN_STATUS, status);

        String selection = InvitationContract.InvitationEntry._ID + " = ?";
        String[] args = {String.valueOf(invitationId)};

        return db.update(InvitationContract.InvitationEntry.TABLE_NAME, values, selection, args) > 0;
    }

    /**
     * Deletes an invitation.
     */
    public boolean deleteInvitation(long invitationId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = InvitationContract.InvitationEntry._ID + " = ?";
        String[] args = {String.valueOf(invitationId)};
        return db.delete(InvitationContract.InvitationEntry.TABLE_NAME, selection, args) > 0;
    }

    /**
     * Gets pending invitations for a specific receiver user, joining project and sender info.
     */
    public List<Invitation> getPendingInvitationsForUser(long userId) {
        List<Invitation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT i.*, p." + ProjectContract.ProjectEntry.COLUMN_NAME + " AS project_name, " +
                "u." + UserContract.UserEntry.COLUMN_FULL_NAME + " AS sender_full_name, " +
                "u." + UserContract.UserEntry.COLUMN_USERNAME + " AS sender_username " +
                "FROM " + InvitationContract.InvitationEntry.TABLE_NAME + " i " +
                "INNER JOIN " + ProjectContract.ProjectEntry.TABLE_NAME + " p ON i." + InvitationContract.InvitationEntry.COLUMN_PROJECT_ID + " = p." + ProjectContract.ProjectEntry._ID +
                "INNER JOIN " + UserContract.UserEntry.TABLE_NAME + " u ON i." + InvitationContract.InvitationEntry.COLUMN_SENDER_ID + " = u." + UserContract.UserEntry._ID +
                " WHERE i." + InvitationContract.InvitationEntry.COLUMN_RECEIVER_ID + " = ? " +
                "AND i." + InvitationContract.InvitationEntry.COLUMN_STATUS + " = 'PENDING' " +
                "ORDER BY i." + InvitationContract.InvitationEntry._ID + " DESC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)})) {
            while (cursor.moveToNext()) {
                Invitation invite = new Invitation();
                invite.setId(cursor.getLong(cursor.getColumnIndexOrThrow(InvitationContract.InvitationEntry._ID)));
                invite.setProjectId(cursor.getLong(cursor.getColumnIndexOrThrow(InvitationContract.InvitationEntry.COLUMN_PROJECT_ID)));
                invite.setSenderId(cursor.getLong(cursor.getColumnIndexOrThrow(InvitationContract.InvitationEntry.COLUMN_SENDER_ID)));
                invite.setReceiverId(cursor.getLong(cursor.getColumnIndexOrThrow(InvitationContract.InvitationEntry.COLUMN_RECEIVER_ID)));
                invite.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(InvitationContract.InvitationEntry.COLUMN_STATUS)));
                invite.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(InvitationContract.InvitationEntry.COLUMN_CREATED_AT)));
                invite.setProjectName(cursor.getString(cursor.getColumnIndexOrThrow("project_name")));
                invite.setSenderFullName(cursor.getString(cursor.getColumnIndexOrThrow("sender_full_name")));
                invite.setSenderUsername(cursor.getString(cursor.getColumnIndexOrThrow("sender_username")));
                list.add(invite);
            }
        } catch (Exception e) {
            Log.e(TAG, "getPendingInvitationsForUser error: " + e.getMessage());
        }
        return list;
    }
}
