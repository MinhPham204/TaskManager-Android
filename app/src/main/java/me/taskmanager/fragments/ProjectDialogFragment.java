package me.taskmanager.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import me.taskmanager.R;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.model.Project;
import me.taskmanager.preferences.UserPreferencesManager;

public class ProjectDialogFragment extends DialogFragment {

    private ProjectRepository projectRepository;
    private UserPreferencesManager preferencesManager;
    private long projectId = -1;
    private Project projectToEdit;

    public static ProjectDialogFragment newInstance(long projectId) {
        ProjectDialogFragment fragment = new ProjectDialogFragment();
        Bundle args = new Bundle();
        args.putLong("PROJECT_ID", projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        projectRepository = new ProjectRepository(requireContext());
        preferencesManager = new UserPreferencesManager(requireContext());

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.fragment_project_dialog, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        EditText etName = dialogView.findViewById(R.id.et_project_name);
        EditText etDesc = dialogView.findViewById(R.id.et_project_desc);
        TextView tvError = dialogView.findViewById(R.id.tv_error);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Check Mode (Create vs Edit)
        if (getArguments() != null && getArguments().containsKey("PROJECT_ID")) {
            projectId = getArguments().getLong("PROJECT_ID");
            projectToEdit = projectRepository.getProjectById(projectId);
            if (projectToEdit != null) {
                tvTitle.setText("Edit Project");
                etName.setText(projectToEdit.getName());
                etDesc.setText(projectToEdit.getDescription());
            }
        }

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();

            if (name.isEmpty()) {
                tvError.setText("Project Name is required");
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            long currentUserId = preferencesManager.getCurrentUserId();
            if (currentUserId == -1) {
                Toast.makeText(requireContext(), "User session expired. Please relogin.", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                return;
            }

            if (projectToEdit == null) {
                // Create Mode
                Project newProject = new Project(name, desc, "ACTIVE", null);
                long newId = projectRepository.createProject(newProject, currentUserId);
                if (newId != -1) {
                    Toast.makeText(getContext(), "Project created successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    notifyParentRefresh();
                } else {
                    tvError.setText("Failed to create project");
                    tvError.setVisibility(View.VISIBLE);
                }
            } else {
                // Edit Mode
                projectToEdit.setName(name);
                projectToEdit.setDescription(desc);
                boolean success = projectRepository.updateProject(projectToEdit);
                if (success) {
                    Toast.makeText(getContext(), "Project updated successfully", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    notifyParentRefresh();
                } else {
                    tvError.setText("Failed to update project");
                    tvError.setVisibility(View.VISIBLE);
                }
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        return dialog;
    }

    private void notifyParentRefresh() {
        Fragment parent = getParentFragment();
        if (parent instanceof ProjectListFragment) {
            ((ProjectListFragment) parent).refreshProjects();
        } else {
            // Check list in backstack or activity
            Fragment listFrag = getParentFragmentManager().findFragmentById(R.id.fragment_container);
            if (listFrag instanceof ProjectListFragment) {
                ((ProjectListFragment) listFrag).refreshProjects();
            } else if (listFrag instanceof ProjectDetailsFragment) {
                ((ProjectDetailsFragment) listFrag).refreshDetails();
            }
        }
    }
}
