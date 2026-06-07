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

import me.zubair.taskmanager.model.Project;
import me.zubair.taskmanager.model.User;

/**
 * Repository for managing Project operations and linkages.
 * Relies on foreign key cascade delete configurations (Decision 005).
 */
public class ProjectRepository {

    private static final String TAG = "ProjectRepository";
    private final TaskDbHelper dbHelper;

    public ProjectRepository(Context context) {
        this.dbHelper = new TaskDbHelper(context);
    }

    /**
     * Creates a new Project and registers its creator as the "Leader" of the project.
     * Performed atomically in a transaction.
     */
    public long createProject(Project project, long creatorUserId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long projectId = -1;

        db.beginTransaction();
        try {
            String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());

            // 1. Insert Project
            ContentValues projectValues = new ContentValues();
            projectValues.put(ProjectContract.ProjectEntry.COLUMN_NAME, project.getName().trim());
            projectValues.put(ProjectContract.ProjectEntry.COLUMN_DESCRIPTION, project.getDescription() != null ? project.getDescription().trim() : "");
            projectValues.put(ProjectContract.ProjectEntry.COLUMN_STATUS, project.getStatus() != null ? project.getStatus() : "ACTIVE");
            projectValues.put(ProjectContract.ProjectEntry.COLUMN_CREATED_AT, createdAt);

            projectId = db.insert(ProjectContract.ProjectEntry.TABLE_NAME, null, projectValues);

            if (projectId != -1) {
                // 2. Insert creator membership as Leader
                ContentValues memberValues = new ContentValues();
                memberValues.put(ProjectMemberContract.ProjectMemberEntry.COLUMN_PROJECT_ID, projectId);
                memberValues.put(ProjectMemberContract.ProjectMemberEntry.COLUMN_USER_ID, creatorUserId);
                memberValues.put(ProjectMemberContract.ProjectMemberEntry.COLUMN_ROLE, "Leader");

                long memberId = db.insert(ProjectMemberContract.ProjectMemberEntry.TABLE_NAME, null, memberValues);
                if (memberId == -1) {
                    throw new Exception("Failed to insert creator membership");
                }
                db.setTransactionSuccessful();
            }
        } catch (Exception e) {
            Log.e(TAG, "createProject error: " + e.getMessage());
            projectId = -1;
        } finally {
            db.endTransaction();
        }

        return projectId;
    }

    /**
     * Updates project metadata (name, description, status).
     */
    public boolean updateProject(Project project) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ProjectContract.ProjectEntry.COLUMN_NAME, project.getName().trim());
        values.put(ProjectContract.ProjectEntry.COLUMN_DESCRIPTION, project.getDescription() != null ? project.getDescription().trim() : "");
        values.put(ProjectContract.ProjectEntry.COLUMN_STATUS, project.getStatus());

        String selection = ProjectContract.ProjectEntry._ID + " = ?";
        String[] args = {String.valueOf(project.getId())};

        return db.update(ProjectContract.ProjectEntry.TABLE_NAME, values, selection, args) > 0;
    }

    /**
     * Deletes a Project by ID. Associated members cascade delete automatically.
     */
    public boolean deleteProject(long projectId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = ProjectContract.ProjectEntry._ID + " = ?";
        String[] args = {String.valueOf(projectId)};
        return db.delete(ProjectContract.ProjectEntry.TABLE_NAME, selection, args) > 0;
    }

    /**
     * Fetches a project by its ID.
     */
    public Project getProjectById(long projectId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = ProjectContract.ProjectEntry._ID + " = ?";
        String[] args = {String.valueOf(projectId)};

        try (Cursor cursor = db.query(ProjectContract.ProjectEntry.TABLE_NAME,
                null, selection, args, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return extractProjectFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "getProjectById error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets all projects where the given user is a member (Leader or Member).
     */
    public List<Project> getAllProjectsForUser(long userId) {
        List<Project> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // SELECT p.* FROM projects p JOIN project_members pm ON p._id = pm.project_id WHERE pm.user_id = ?
        String query = "SELECT p.* FROM " + ProjectContract.ProjectEntry.TABLE_NAME + " p " +
                "INNER JOIN " + ProjectMemberContract.ProjectMemberEntry.TABLE_NAME + " pm " +
                "ON p." + ProjectContract.ProjectEntry._ID + " = pm." + ProjectMemberContract.ProjectMemberEntry.COLUMN_PROJECT_ID +
                " WHERE pm." + ProjectMemberContract.ProjectMemberEntry.COLUMN_USER_ID + " = ? " +
                "ORDER BY p." + ProjectContract.ProjectEntry._ID + " DESC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)})) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    list.add(extractProjectFromCursor(cursor));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "getAllProjectsForUser error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Returns the membership role of a user in a project ("Leader", "Member", or null if not a member).
     */
    public String getMemberRole(long projectId, long userId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = ProjectMemberContract.ProjectMemberEntry.COLUMN_PROJECT_ID + " = ? AND " +
                ProjectMemberContract.ProjectMemberEntry.COLUMN_USER_ID + " = ?";
        String[] args = {String.valueOf(projectId), String.valueOf(userId)};

        try (Cursor cursor = db.query(ProjectMemberContract.ProjectMemberEntry.TABLE_NAME,
                new String[]{ProjectMemberContract.ProjectMemberEntry.COLUMN_ROLE},
                selection, args, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ProjectMemberContract.ProjectMemberEntry.COLUMN_ROLE));
            }
        } catch (Exception e) {
            Log.e(TAG, "getMemberRole error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets all users belonging to a project, populating their projectRole transient field.
     */
    public List<User> getMembersForProject(long projectId) {
        List<User> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT u.*, pm." + ProjectMemberContract.ProjectMemberEntry.COLUMN_ROLE + " FROM " +
                UserContract.UserEntry.TABLE_NAME + " u " +
                "INNER JOIN " + ProjectMemberContract.ProjectMemberEntry.TABLE_NAME + " pm " +
                "ON u." + UserContract.UserEntry._ID + " = pm." + ProjectMemberContract.ProjectMemberEntry.COLUMN_USER_ID +
                " WHERE pm." + ProjectMemberContract.ProjectMemberEntry.COLUMN_PROJECT_ID + " = ? " +
                "ORDER BY pm." + ProjectMemberContract.ProjectMemberEntry._ID + " ASC";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(projectId)})) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.setId(cursor.getLong(cursor.getColumnIndexOrThrow(UserContract.UserEntry._ID)));
                    user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_USERNAME)));
                    user.setFullName(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_FULL_NAME)));
                    user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_EMAIL)));
                    user.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(UserContract.UserEntry.COLUMN_CREATED_AT)));
                    user.setProjectRole(cursor.getString(cursor.getColumnIndexOrThrow(ProjectMemberContract.ProjectMemberEntry.COLUMN_ROLE)));
                    list.add(user);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "getMembersForProject error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Adds a member to a project with a role.
     */
    public boolean addMemberToProject(long projectId, long userId, String role) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ProjectMemberContract.ProjectMemberEntry.COLUMN_PROJECT_ID, projectId);
        values.put(ProjectMemberContract.ProjectMemberEntry.COLUMN_USER_ID, userId);
        values.put(ProjectMemberContract.ProjectMemberEntry.COLUMN_ROLE, role);

        return db.insert(ProjectMemberContract.ProjectMemberEntry.TABLE_NAME, null, values) > 0;
    }

    /**
     * Removes a member from a project.
     */
    public boolean removeMemberFromProject(long projectId, long userId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String selection = ProjectMemberContract.ProjectMemberEntry.COLUMN_PROJECT_ID + " = ? AND " +
                ProjectMemberContract.ProjectMemberEntry.COLUMN_USER_ID + " = ?";
        String[] args = {String.valueOf(projectId), String.valueOf(userId)};

        return db.delete(ProjectMemberContract.ProjectMemberEntry.TABLE_NAME, selection, args) > 0;
    }

    /**
     * Checks if a user is a member of a project.
     */
    public boolean isUserMemberOfProject(long projectId, long userId) {
        return getMemberRole(projectId, userId) != null;
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private Project extractProjectFromCursor(Cursor cursor) {
        Project project = new Project();
        project.setId(cursor.getLong(cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry._ID)));
        project.setName(cursor.getString(cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_NAME)));
        project.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_DESCRIPTION)));
        project.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_STATUS)));
        project.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow(ProjectContract.ProjectEntry.COLUMN_CREATED_AT)));
        return project;
    }
}
