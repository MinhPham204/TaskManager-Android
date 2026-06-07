package me.zubair.taskmanager.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import me.zubair.taskmanager.R;
import me.zubair.taskmanager.database.ProjectRepository;
import me.zubair.taskmanager.database.TaskRepository;
import me.zubair.taskmanager.database.UserRepository;
import me.zubair.taskmanager.database.ActivityLogRepository;
import me.zubair.taskmanager.model.Project;
import me.zubair.taskmanager.model.Task;
import me.zubair.taskmanager.model.User;
import me.zubair.taskmanager.preferences.UserPreferencesManager;

public class TaskDialogFragment extends DialogFragment {

    private static final String ARG_PROJECT_ID = "project_id";

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private UserPreferencesManager preferencesManager;

    private List<String> projectNames = new ArrayList<>();
    private List<Long> projectIds = new ArrayList<>();
    private List<String> assigneeNames = new ArrayList<>();
    private List<Long> assigneeIds = new ArrayList<>();

    public static TaskDialogFragment newInstance(Long projectId) {
        TaskDialogFragment fragment = new TaskDialogFragment();
        if (projectId != null) {
            Bundle args = new Bundle();
            args.putLong(ARG_PROJECT_ID, projectId);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        taskRepository = new TaskRepository(requireContext());
        projectRepository = new ProjectRepository(requireContext());
        userRepository = new UserRepository(requireContext());
        preferencesManager = new UserPreferencesManager(requireContext());

        long currentUserId = preferencesManager.getCurrentUserId();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_task, null);

        EditText etTitle = dialogView.findViewById(R.id.et_task_title);
        EditText etDescription = dialogView.findViewById(R.id.et_task_description);
        Spinner spinnerProject = dialogView.findViewById(R.id.spinner_task_project);
        Spinner spinnerAssignee = dialogView.findViewById(R.id.spinner_task_assignee);
        Spinner spinnerPriority = dialogView.findViewById(R.id.spinner_task_priority);
        Spinner spinnerStatus = dialogView.findViewById(R.id.spinner_task_status);

        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // 1. Populate Projects Spinner
        projectNames.add("General / None");
        projectIds.add(0L);

        List<Project> projects = projectRepository.getAllProjectsForUser(currentUserId);
        for (Project p : projects) {
            projectNames.add(p.getName());
            projectIds.add(p.getId());
        }

        ArrayAdapter<String> projectAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, projectNames);
        projectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProject.setAdapter(projectAdapter);

        // 2. Setup Assignee Spinner listener depending on Project Selection
        spinnerProject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                long selectedProjectId = projectIds.get(position);
                updateAssignees(selectedProjectId, spinnerAssignee);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // If pre-linked to a project from arguments
        if (getArguments() != null && getArguments().containsKey(ARG_PROJECT_ID)) {
            long preLinkedProjectId = getArguments().getLong(ARG_PROJECT_ID);
            int idx = projectIds.indexOf(preLinkedProjectId);
            if (idx != -1) {
                spinnerProject.setSelection(idx);
                spinnerProject.setEnabled(false); // Lock Project Selection
            }
        }

        // 3. Populate Priority Spinner
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(priorityAdapter);
        spinnerPriority.setSelection(1); // Default to Medium

        // 4. Populate Status Spinner
        String[] statuses = {"TODO", "IN PROGRESS", "DONE", "CANCELLED"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);
        spinnerStatus.setSelection(0); // Default to TODO

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Title cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            int projectPos = spinnerProject.getSelectedItemPosition();
            long projectId = (projectPos >= 0 && projectPos < projectIds.size()) ? projectIds.get(projectPos) : 0L;

            int assigneePos = spinnerAssignee.getSelectedItemPosition();
            long assigneeId = (assigneePos >= 0 && assigneePos < assigneeIds.size()) ? assigneeIds.get(assigneePos) : 0L;

            int priorityVal = spinnerPriority.getSelectedItemPosition() + 1; // 1 = Low, 2 = Medium, 3 = High

            int statusPos = spinnerStatus.getSelectedItemPosition();
            String statusVal = Task.STATUS_TODO;
            switch (statusPos) {
                case 0: statusVal = Task.STATUS_TODO; break;
                case 1: statusVal = Task.STATUS_IN_PROGRESS; break;
                case 2: statusVal = Task.STATUS_DONE; break;
                case 3: statusVal = Task.STATUS_CANCELLED; break;
            }

            Task newTask = new Task();
            newTask.setTitle(title);
            newTask.setDescription(description);
            newTask.setProjectId(projectId > 0 ? projectId : null);
            newTask.setAssignedUserId(assigneeId > 0 ? assigneeId : null);
            newTask.setPriority(priorityVal);
            newTask.setStatus(statusVal);
            newTask.setCreatedAt(System.currentTimeMillis());
            newTask.setUpdatedAt(System.currentTimeMillis());

            long result = taskRepository.insertTask(newTask);
            if (result > 0) {
                new ActivityLogRepository(requireContext()).insertLog(currentUserId, "created task: \"" + title + "\"", "Task", result);
                Toast.makeText(getContext(), "Task added successfully", Toast.LENGTH_SHORT).show();
                
                // Notify parent fragments to refresh
                Fragment parentFragment = getParentFragment();
                if (parentFragment instanceof TaskListFragment) {
                    ((TaskListFragment) parentFragment).onResume();
                } else if (parentFragment instanceof ProjectDetailsFragment) {
                    ((ProjectDetailsFragment) parentFragment).onResume(); // Or direct load tasks method
                }
                
                dialog.dismiss();
            } else {
                Toast.makeText(getContext(), "Failed to add task", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private void updateAssignees(long projectId, Spinner spinnerAssignee) {
        assigneeNames.clear();
        assigneeIds.clear();

        assigneeNames.add("Unassigned");
        assigneeIds.add(0L);

        if (projectId > 0) {
            List<User> members = projectRepository.getMembersForProject(projectId);
            for (User u : members) {
                assigneeNames.add(u.getFullName() + " (@" + u.getUsername() + ")");
                assigneeIds.add(u.getId());
            }
        }

        ArrayAdapter<String> assigneeAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, assigneeNames);
        assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAssignee.setAdapter(assigneeAdapter);
    }
}
