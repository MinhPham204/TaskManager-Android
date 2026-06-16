package me.taskmanager.fragments;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import me.taskmanager.R;
import me.taskmanager.adapter.TaskAdapter;
import me.taskmanager.model.Project;
import me.taskmanager.model.Task;
import me.taskmanager.model.User;
import me.taskmanager.utils.FileHelper;
import me.taskmanager.viewmodel.TaskListViewModel;

public class TaskListFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private TaskListViewModel taskListViewModel;
    private FileHelper fileHelper;

    private EditText etSearch;
    private Spinner spinnerProject;
    private Spinner spinnerAssignee;
    private Spinner spinnerStatus;
    private Spinner spinnerPriority;
    private Spinner spinnerSort;
    private TextView tvEmptyTasks;

    private final List<Long> projectFilterIds = new ArrayList<>();
    private final List<Long> assigneeFilterIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_list, container, false);

        fileHelper = new FileHelper();

        // Initialize components
        recyclerView = view.findViewById(R.id.rv_tasks);
        FloatingActionButton fabAddTask = view.findViewById(R.id.fab_add_task);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup filter header components
        setupFilters(view);

        // Setup ViewModel
        taskListViewModel = new ViewModelProvider(this).get(TaskListViewModel.class);

        // Observe projects
        taskListViewModel.getProjectsLiveData().observe(getViewLifecycleOwner(), projects -> {
            if (projects == null) return;
            
            int projectSel = spinnerProject.getSelectedItemPosition();
            
            List<String> projectNames = new ArrayList<>();
            projectFilterIds.clear();
            projectNames.add("All Projects");
            projectFilterIds.add(-1L);

            for (Project p : projects) {
                projectNames.add(p.getName());
                projectFilterIds.add(p.getId());
            }

            ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, projectNames);
            projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerProject.setAdapter(projectAdapter);
            
            if (projectSel >= 0 && projectSel < projectNames.size()) {
                spinnerProject.setSelection(projectSel);
            } else {
                spinnerProject.setSelection(0);
            }
        });

        // Observe assignees
        taskListViewModel.getAssigneesLiveData().observe(getViewLifecycleOwner(), members -> {
            if (members == null) return;

            int assigneeSel = spinnerAssignee.getSelectedItemPosition();

            List<String> userNames = new ArrayList<>();
            assigneeFilterIds.clear();

            userNames.add("All Assignees");
            assigneeFilterIds.add(-1L);
            userNames.add("Unassigned");
            assigneeFilterIds.add(0L);

            for (User u : members) {
                userNames.add(u.getFullName() + " (@" + u.getUsername() + ")");
                assigneeFilterIds.add(u.getId());
            }

            ArrayAdapter<String> assigneeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, userNames);
            assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerAssignee.setAdapter(assigneeAdapter);

            if (assigneeSel >= 0 && assigneeSel < userNames.size()) {
                spinnerAssignee.setSelection(assigneeSel);
            } else {
                spinnerAssignee.setSelection(0);
            }
        });

        // Observe tasks
        taskListViewModel.getTasksLiveData().observe(getViewLifecycleOwner(), filteredTasks -> {
            if (filteredTasks == null) return;

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
        });

        // Set up FAB click listener
        fabAddTask.setOnClickListener(v -> showAddTaskDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (taskListViewModel != null) {
            taskListViewModel.loadProjects();
        }
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
        if (taskListViewModel != null) {
            taskListViewModel.loadAssignees(projectId);
        }
    }

    private void performFiltering() {
        if (taskListViewModel == null || spinnerProject == null || spinnerAssignee == null || spinnerStatus == null || spinnerPriority == null || spinnerSort == null) {
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

        taskListViewModel.filterTasks(
                projectId,
                assignedUserId,
                priority,
                status,
                query,
                sortBy
        );
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
            
            taskListViewModel.deleteTask(task, success -> {
                if (success) {
                    Toast.makeText(requireContext(), "Task deleted", Toast.LENGTH_SHORT).show();
                    performFiltering();
                } else {
                    Toast.makeText(requireContext(), "Error deleting task", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
