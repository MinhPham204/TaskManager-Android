package me.zubair.taskmanager.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import me.zubair.taskmanager.model.Task;

/**
 * Repository for task CRUD operations, support search, filter, and project linkages.
 */
public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private final TaskDbHelper dbHelper;

    public TaskRepository(Context context) {
        this.dbHelper = new TaskDbHelper(context);
    }

    /**
     * Get all tasks from the database
     */
    public List<Task> getAllTasks() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Task> tasks = new ArrayList<>();

        try (Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                null, // null projection returns all columns
                null,
                null,
                null,
                null,
                TaskContract.TaskEntry.COLUMN_DUE_DATE + " ASC"
        )) {
            while (cursor.moveToNext()) {
                tasks.add(extractTaskFromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting all tasks: " + e.getMessage());
        }

        return tasks;
    }

    /**
     * Get task by ID
     */
    public Task getTaskById(long taskId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Task task = null;

        String selection = TaskContract.TaskEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(taskId) };

        try (Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                task = extractTaskFromCursor(cursor);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting task by ID: " + e.getMessage());
        }

        return task;
    }

    /**
     * Insert a new task
     */
    public long insertTask(Task task) {
        return addTask(task);
    }

    /**
     * Add a task
     */
    public long addTask(Task task) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TaskContract.TaskEntry.COLUMN_TITLE, task.getTitle());
        values.put(TaskContract.TaskEntry.COLUMN_DESCRIPTION, task.getDescription());
        values.put(TaskContract.TaskEntry.COLUMN_DUE_DATE, task.getDueDate());
        values.put(TaskContract.TaskEntry.COLUMN_PRIORITY, task.getPriority());
        values.put(TaskContract.TaskEntry.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        
        if (task.getCreatedAt() > 0) {
            values.put(TaskContract.TaskEntry.COLUMN_CREATED_AT, task.getCreatedAt());
        } else {
            values.put(TaskContract.TaskEntry.COLUMN_CREATED_AT, System.currentTimeMillis());
        }

        // Phase 4 columns
        if (task.getProjectId() != null && task.getProjectId() > 0) {
            values.put(TaskContract.TaskEntry.COLUMN_PROJECT_ID, task.getProjectId());
        } else {
            values.putNull(TaskContract.TaskEntry.COLUMN_PROJECT_ID);
        }

        if (task.getAssignedUserId() != null && task.getAssignedUserId() > 0) {
            values.put(TaskContract.TaskEntry.COLUMN_ASSIGNED_USER_ID, task.getAssignedUserId());
        } else {
            values.putNull(TaskContract.TaskEntry.COLUMN_ASSIGNED_USER_ID);
        }

        values.put(TaskContract.TaskEntry.COLUMN_STATUS, task.getStatus());
        values.put(TaskContract.TaskEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());

        return db.insert(TaskContract.TaskEntry.TABLE_NAME, null, values);
    }

    /**
     * Update an existing task
     */
    public int updateTask(Task task) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(TaskContract.TaskEntry.COLUMN_TITLE, task.getTitle());
        values.put(TaskContract.TaskEntry.COLUMN_DESCRIPTION, task.getDescription());
        values.put(TaskContract.TaskEntry.COLUMN_DUE_DATE, task.getDueDate());
        values.put(TaskContract.TaskEntry.COLUMN_PRIORITY, task.getPriority());
        values.put(TaskContract.TaskEntry.COLUMN_COMPLETED, task.isCompleted() ? 1 : 0);
        values.put(TaskContract.TaskEntry.COLUMN_CREATED_AT, task.getCreatedAt());
        
        // Phase 4 columns
        if (task.getProjectId() != null && task.getProjectId() > 0) {
            values.put(TaskContract.TaskEntry.COLUMN_PROJECT_ID, task.getProjectId());
        } else {
            values.putNull(TaskContract.TaskEntry.COLUMN_PROJECT_ID);
        }

        if (task.getAssignedUserId() != null && task.getAssignedUserId() > 0) {
            values.put(TaskContract.TaskEntry.COLUMN_ASSIGNED_USER_ID, task.getAssignedUserId());
        } else {
            values.putNull(TaskContract.TaskEntry.COLUMN_ASSIGNED_USER_ID);
        }

        values.put(TaskContract.TaskEntry.COLUMN_STATUS, task.getStatus());
        values.put(TaskContract.TaskEntry.COLUMN_UPDATED_AT, System.currentTimeMillis());
        
        String selection = TaskContract.TaskEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(task.getId()) };
        
        return db.update(
            TaskContract.TaskEntry.TABLE_NAME,
            values,
            selection,
            selectionArgs
        );
    }
    
    /**
     * Delete a task by its ID
     */
    public int deleteTask(long taskId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        String selection = TaskContract.TaskEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(taskId) };
        
        return db.delete(
            TaskContract.TaskEntry.TABLE_NAME,
            selection,
            selectionArgs
        );
    }
    
    /**
     * Get all tasks due between two timestamps
     */
    public List<Task> getTasksDueBetween(long startTime, long endTime) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Task> tasks = new ArrayList<>();
        
        String selection = TaskContract.TaskEntry.COLUMN_DUE_DATE + " >= ? AND " +
                           TaskContract.TaskEntry.COLUMN_DUE_DATE + " <= ?";
        String[] selectionArgs = { String.valueOf(startTime), String.valueOf(endTime) };
        
        try (Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                TaskContract.TaskEntry.COLUMN_DUE_DATE + " ASC"
        )) {
            while (cursor.moveToNext()) {
                tasks.add(extractTaskFromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting tasks due between timestamps: " + e.getMessage());
        }
        
        return tasks;
    }

    /**
     * Get all tasks associated with a specific project ID.
     */
    public List<Task> getTasksForProject(long projectId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Task> tasks = new ArrayList<>();
        
        String selection = TaskContract.TaskEntry.COLUMN_PROJECT_ID + " = ?";
        String[] selectionArgs = { String.valueOf(projectId) };

        try (Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                TaskContract.TaskEntry._ID + " DESC"
        )) {
            while (cursor.moveToNext()) {
                tasks.add(extractTaskFromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting tasks for project: " + e.getMessage());
        }
        
        return tasks;
    }

    /**
     * Search, filter, and sort tasks dynamically.
     */
    public List<Task> getSearchFilteredTasks(Long projectId, Long assignedUserId, Integer priority, String status, String searchQuery, String sortBy) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Task> tasks = new ArrayList<>();
        
        StringBuilder selectionBuilder = new StringBuilder();
        List<String> selectionArgs = new ArrayList<>();
        
        if (projectId != null && projectId > 0) {
            selectionBuilder.append(TaskContract.TaskEntry.COLUMN_PROJECT_ID).append(" = ?");
            selectionArgs.add(String.valueOf(projectId));
        }
        
        if (assignedUserId != null && assignedUserId > 0) {
            if (selectionBuilder.length() > 0) selectionBuilder.append(" AND ");
            selectionBuilder.append(TaskContract.TaskEntry.COLUMN_ASSIGNED_USER_ID).append(" = ?");
            selectionArgs.add(String.valueOf(assignedUserId));
        }
        
        if (priority != null && priority > 0) {
            if (selectionBuilder.length() > 0) selectionBuilder.append(" AND ");
            selectionBuilder.append(TaskContract.TaskEntry.COLUMN_PRIORITY).append(" = ?");
            selectionArgs.add(String.valueOf(priority));
        }
        
        if (status != null && !status.isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            if (selectionBuilder.length() > 0) selectionBuilder.append(" AND ");
            selectionBuilder.append(TaskContract.TaskEntry.COLUMN_STATUS).append(" = ?");
            selectionArgs.add(status);
        }
        
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            if (selectionBuilder.length() > 0) selectionBuilder.append(" AND ");
            selectionBuilder.append(TaskContract.TaskEntry.COLUMN_TITLE).append(" LIKE ?");
            selectionArgs.add("%" + searchQuery.trim() + "%");
        }
        
        String selection = selectionBuilder.length() > 0 ? selectionBuilder.toString() : null;
        String[] args = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);
        
        // Sort mapping
        String orderBy = TaskContract.TaskEntry.COLUMN_DUE_DATE + " ASC"; // default
        if (sortBy != null) {
            switch (sortBy) {
                case "DEADLINE_ASC":
                    orderBy = TaskContract.TaskEntry.COLUMN_DUE_DATE + " ASC";
                    break;
                case "DEADLINE_DESC":
                    orderBy = TaskContract.TaskEntry.COLUMN_DUE_DATE + " DESC";
                    break;
                case "CREATED_ASC":
                    orderBy = TaskContract.TaskEntry.COLUMN_CREATED_AT + " ASC";
                    break;
                case "CREATED_DESC":
                    orderBy = TaskContract.TaskEntry.COLUMN_CREATED_AT + " DESC";
                    break;
            }
        }
        
        try (Cursor cursor = db.query(
                TaskContract.TaskEntry.TABLE_NAME,
                null,
                selection,
                args,
                null,
                null,
                orderBy
        )) {
            while (cursor.moveToNext()) {
                tasks.add(extractTaskFromCursor(cursor));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error filtering tasks: " + e.getMessage());
        }
        
        return tasks;
    }
    
    /**
     * Extract a Task object from a cursor
     */
    private Task extractTaskFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry._ID));
        String title = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_TITLE));
        String description = cursor.getString(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_DESCRIPTION));
        long dueDate = cursor.getLong(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_DUE_DATE));
        int priority = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_PRIORITY));
        boolean completed = cursor.getInt(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_COMPLETED)) == 1;
        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(TaskContract.TaskEntry.COLUMN_CREATED_AT));
        
        Task task = new Task();
        task.setId(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setDueDate(dueDate);
        task.setPriority(priority);
        task.setCompleted(completed);
        task.setCreatedAt(createdAt);

        // Extract Phase 4 columns if present in cursor
        int projectIdIndex = cursor.getColumnIndex(TaskContract.TaskEntry.COLUMN_PROJECT_ID);
        if (projectIdIndex != -1 && !cursor.isNull(projectIdIndex)) {
            task.setProjectId(cursor.getLong(projectIdIndex));
        }

        int assignedUserIndex = cursor.getColumnIndex(TaskContract.TaskEntry.COLUMN_ASSIGNED_USER_ID);
        if (assignedUserIndex != -1 && !cursor.isNull(assignedUserIndex)) {
            task.setAssignedUserId(cursor.getLong(assignedUserIndex));
        }

        int statusIndex = cursor.getColumnIndex(TaskContract.TaskEntry.COLUMN_STATUS);
        if (statusIndex != -1) {
            task.setStatus(cursor.getString(statusIndex));
        }

        int updatedAtIndex = cursor.getColumnIndex(TaskContract.TaskEntry.COLUMN_UPDATED_AT);
        if (updatedAtIndex != -1) {
            task.setUpdatedAt(cursor.getLong(updatedAtIndex));
        }
        
        return task;
    }
}
