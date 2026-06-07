package me.zubair.taskmanager.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import me.zubair.taskmanager.R;
import me.zubair.taskmanager.adapter.TaskAdapter;
import me.zubair.taskmanager.database.ProjectRepository;
import me.zubair.taskmanager.database.TaskRepository;
import me.zubair.taskmanager.database.UserRepository;
import me.zubair.taskmanager.database.ActivityLogRepository;
import me.zubair.taskmanager.model.Project;
import me.zubair.taskmanager.model.Task;
import me.zubair.taskmanager.model.User;
import me.zubair.taskmanager.preferences.UserPreferencesManager;
import me.zubair.taskmanager.utils.FileHelper;

public class TaskListFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private ActivityLogRepository activityLogRepository;
    private UserPreferencesManager preferencesManager;
    private FileHelper fileHelper;

    private EditText etSearch;
    private Spinner spinnerProject;
    private Spinner spinnerAssignee;
    private Spinner spinnerStatus;
    private Spinner spinnerPriority;
    private Spinner spinnerSort;
    private TextView tvEmptyTasks;

    private List<Long> projectFilterIds = new ArrayList<>();
    private List<Long> assigneeFilterIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        // Initialize repositories and preferences
        taskRepository = new TaskRepository(requireContext());
        projectRepository = new ProjectRepository(requireContext());
        userRepository = new UserRepository(requireContext());
        activityLogRepository = new ActivityLogRepository(requireContext());
        preferencesManager = new UserPreferencesManager(requireContext());
        fileHelper = new FileHelper();

        // Initialize components
        recyclerView = view.findViewById(R.id.rv_tasks);
        FloatingActionButton fabAddTask = view.findViewById(R.id.fab_add_task);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup filter header components
        setupFilters(view);

        // Set up FAB click listener
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh the task list and filters when returning to this fragment
        // We should reload filters in case projects/users were added or updated
        long currentUserId = preferencesManager.getCurrentUserId();
        
        // Save current selections
        int projectSel = spinnerProject.getSelectedItemPosition();
        int statusSel = spinnerStatus.getSelectedItemPosition();
        int prioritySel = spinnerPriority.getSelectedItemPosition();
        int sortSel = spinnerSort.getSelectedItemPosition();
        
        // Refresh Project Filter
        List<String> projectNames = new ArrayList<>();
        projectFilterIds.clear();
        projectNames.add("All Projects");
        projectFilterIds.add(-1L);

        List<Project> projects = projectRepository.getAllProjectsForUser(currentUserId);
        for (Project p : projects) {
            projectNames.add(p.getName());
            projectFilterIds.add(p.getId());
        }

        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, projectNames);
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(projectAdapter);
        
        // Restore project selection if valid
        if (projectSel >= 0 && projectSel < projectNames.size()) {
            spinnerProject.setSelection(projectSel);
        } else {
            spinnerProject.setSelection(0);
        }

        // Restore other selections
        if (statusSel >= 0 && statusSel < spinnerStatus.getAdapter().getCount()) spinnerStatus.setSelection(statusSel);
        if (prioritySel >= 0 && prioritySel < spinnerPriority.getAdapter().getCount()) spinnerPriority.setSelection(prioritySel);
        if (sortSel >= 0 && sortSel < spinnerSort.getAdapter().getCount()) spinnerSort.setSelection(sortSel);

        performFiltering();
    }

    private void setupFilters(View view) {
        etSearch = view.findViewById(R.id.et_search_task);
        spinnerProject = view.findViewById(R.id.spinner_filter_project);
        spinnerAssignee = view.findViewById(R.id.spinner_filter_assignee);
        spinnerStatus = view.findViewById(R.id.spinner_filter_status);
        spinnerPriority = view.findViewById(R.id.spinner_filter_priority);
        spinnerSort = view.findViewById(R.id.spinner_filter_sort);
        tvEmptyTasks = view.findViewById(R.id.tv_empty_tasks);

        // Project selection change listener triggers assignee reloading and filtering
        spinnerProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < projectFilterIds.size()) {
                    long selectedProjectId = projectFilterIds.get(position);
                    updateAssigneeFilterOptions(selectedProjectId);
                }
                performFiltering();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Assignee selector listener
        spinnerAssignee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Status Filter
        String[] statusDisplay = {"All Statuses", "TODO", "IN PROGRESS", "DONE", "CANCELLED"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, statusDisplay);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Priority Filter
        String[] priorityDisplay = {"All Priorities", "Low", "Medium", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, priorityDisplay);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Sort Spinner
        String[] sortDisplay = {"Sort by Deadline (Asc)", "Sort by Deadline (Desc)", "Sort by Created (Asc)", "Sort by Created (Desc)"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, sortDisplay);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);
        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                performFiltering();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Search text watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performFiltering();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateAssigneeFilterOptions(long projectId) {
        List<String> userNames = new ArrayList<>();
        assigneeFilterIds.clear();

        userNames.add("All Assignees");
        assigneeFilterIds.add(-1L);
        userNames.add("Unassigned");
        assigneeFilterIds.add(0L);

        if (projectId > 0) {
            List<User> members = projectRepository.getMembersForProject(projectId);
            for (User u : members) {
                userNames.add(u.getFullName() + " (@" + u.getUsername() + ")");
                assigneeFilterIds.add(u.getId());
            }
        } else {
            long currentUserId = preferencesManager.getCurrentUserId();
            List<Project> projects = projectRepository.getAllProjectsForUser(currentUserId);
            List<Long> addedUserIds = new ArrayList<>();
            for (Project p : projects) {
                List<User> members = projectRepository.getMembersForProject(p.getId());
                for (User u : members) {
                    if (!addedUserIds.contains(u.getId())) {
                        addedUserIds.add(u.getId());
                        userNames.add(u.getFullName() + " (@" + u.getUsername() + ")");
                        assigneeFilterIds.add(u.getId());
                    }
                }
            }
        }

        ArrayAdapter<String> assigneeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, userNames);
        assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignee.setAdapter(assigneeAdapter);
    }

    private void performFiltering() {
        if (taskRepository == null || spinnerProject == null || spinnerAssignee == null || spinnerStatus == null || spinnerPriority == null || spinnerSort == null) {
            return;
        }

        int projectPos = spinnerProject.getSelectedItemPosition();
        Long projectId = null;
        if (projectPos > 0 && projectPos < projectFilterIds.size()) {
            projectId = projectFilterIds.get(projectPos);
        }

        int assigneePos = spinnerAssignee.getSelectedItemPosition();
        Long assignedUserId = null;
        if (assigneePos > 0 && assigneePos < assigneeFilterIds.size()) {
            assignedUserId = assigneeFilterIds.get(assigneePos);
        }

        int statusPos = spinnerStatus.getSelectedItemPosition();
        String status = "ALL";
        switch (statusPos) {
            case 1: status = Task.STATUS_TODO; break;
            case 2: status = Task.STATUS_IN_PROGRESS; break;
            case 3: status = Task.STATUS_DONE; break;
            case 4: status = Task.STATUS_CANCELLED; break;
        }

        int priorityPos = spinnerPriority.getSelectedItemPosition();
        Integer priority = priorityPos > 0 ? priorityPos : null;

        int sortPos = spinnerSort.getSelectedItemPosition();
        String sortBy = "DEADLINE_ASC";
        switch (sortPos) {
            case 0: sortBy = "DEADLINE_ASC"; break;
            case 1: sortBy = "DEADLINE_DESC"; break;
            case 2: sortBy = "CREATED_ASC"; break;
            case 3: sortBy = "CREATED_DESC"; break;
        }

        String query = etSearch.getText().toString().trim();

        List<Task> filteredTasks = taskRepository.getSearchFilteredTasks(
                projectId,
                assignedUserId,
                priority,
                status,
                query,
                sortBy
        );

        if (taskAdapter == null) {
            taskAdapter = new TaskAdapter(requireContext(), filteredTasks, this);
            recyclerView.setAdapter(taskAdapter);
        } else {
            taskAdapter.updateTasks(filteredTasks);
        }

        if (filteredTasks.isEmpty()) {
            tvEmptyTasks.setVisibility(View.VISIBLE);
        } else {
            tvEmptyTasks.setVisibility(View.GONE);
        }
    }

    private void showAddTaskDialog() {
        TaskDialogFragment dialog = new TaskDialogFragment();
        dialog.show(getChildFragmentManager(), "AddTaskDialog");
    }

    @Override
    public void onTaskClick(Task task) {
        TaskDetailsFragment detailsFragment = new TaskDetailsFragment();
        Bundle args = new Bundle();
        args.putLong("TASK_ID", task.getId());
        detailsFragment.setArguments(args);
        
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onTaskLongClick(Task task, View view) {
        showTaskOptionsMenu(task, view);
    }

    private void showTaskOptionsMenu(final Task task, View view) {
        PopupMenu popup = new PopupMenu(requireContext(), view);
        popup.inflate(R.menu.menu_task_options);
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit) {
                navigateToEditTask(task.getId());
                return true;
            } else if (itemId == R.id.action_delete) {
                deleteTask(task);
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void navigateToEditTask(long taskId) {
        EditTaskFragment editFragment = new EditTaskFragment();
        Bundle args = new Bundle();
        args.putLong("TASK_ID", taskId);
        editFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void deleteTask(Task task) {
        try {
            if (fileHelper != null) {
                fileHelper.deleteTaskFiles(requireContext(), task.getId());
            }
            
            long currentUserId = preferencesManager.getCurrentUserId();
            activityLogRepository.insertLog(currentUserId, "deleted task: \"" + task.getTitle() + "\"", "Task", task.getId());

            int result = taskRepository.deleteTask(task.getId());
            if (result > 0) {
                Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                performFiltering(); // Refresh the list using filters
            } else {
                Toast.makeText(requireContext(), "Error deleting task", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
