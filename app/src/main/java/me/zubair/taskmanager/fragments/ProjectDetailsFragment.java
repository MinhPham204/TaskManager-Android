package me.zubair.taskmanager.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.zubair.taskmanager.R;
import me.zubair.taskmanager.adapter.ProjectMemberAdapter;
import me.zubair.taskmanager.adapter.TaskAdapter;
import me.zubair.taskmanager.database.ProjectRepository;
import me.zubair.taskmanager.database.TaskRepository;
import me.zubair.taskmanager.database.UserRepository;
import me.zubair.taskmanager.database.ActivityLogRepository;
import me.zubair.taskmanager.database.InvitationRepository;
import me.zubair.taskmanager.model.Project;
import me.zubair.taskmanager.model.Task;
import me.zubair.taskmanager.model.User;
import me.zubair.taskmanager.preferences.UserPreferencesManager;
import me.zubair.taskmanager.utils.FileHelper;

public class ProjectDetailsFragment extends Fragment {

    private long projectId = -1;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private ActivityLogRepository activityLogRepository;
    private InvitationRepository invitationRepository;
    private UserPreferencesManager preferencesManager;
    private FileHelper fileHelper;
    private Project currentProject;

    private ImageButton btnBack;
    private TextView tvProjectTitle;
    private TextView tvProjectStatus;
    private TextView tvProjectDescription;
    private TextView tvProjectCreatedAt;
    private CardView cardActions;
    private View layoutEdit;
    private View layoutDelete;

    // Member management views
    private View layoutAddMember;
    private EditText etMemberUsername;
    private Button btnAddMember;
    private RecyclerView rvMembers;
    private ProjectMemberAdapter memberAdapter;

    // Task management views
    private RecyclerView rvTasks;
    private TaskAdapter taskAdapter;
    private View btnAddTask;
    private TextView tvNoTasks;

    public static ProjectDetailsFragment newInstance(long projectId) {
        ProjectDetailsFragment fragment = new ProjectDetailsFragment();
        Bundle args = new Bundle();
        args.putLong("PROJECT_ID", projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_project_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            projectId = getArguments().getLong("PROJECT_ID");
        }

        projectRepository = new ProjectRepository(requireContext());
        userRepository = new UserRepository(requireContext());
        taskRepository = new TaskRepository(requireContext());
        activityLogRepository = new ActivityLogRepository(requireContext());
        invitationRepository = new InvitationRepository(requireContext());
        preferencesManager = new UserPreferencesManager(requireContext());
        fileHelper = new FileHelper();

        // Bind Views
        btnBack = view.findViewById(R.id.btn_back);
        tvProjectTitle = view.findViewById(R.id.tv_project_title);
        tvProjectStatus = view.findViewById(R.id.tv_project_status);
        tvProjectDescription = view.findViewById(R.id.tv_project_description);
        tvProjectCreatedAt = view.findViewById(R.id.tv_project_created_at);
        cardActions = view.findViewById(R.id.card_project_actions);
        layoutEdit = view.findViewById(R.id.layout_edit_project);
        layoutDelete = view.findViewById(R.id.layout_delete_project);

        // Bind Member Views
        layoutAddMember = view.findViewById(R.id.layout_add_member);
        etMemberUsername = view.findViewById(R.id.et_member_username);
        btnAddMember = view.findViewById(R.id.btn_add_member);
        rvMembers = view.findViewById(R.id.rv_project_members);

        rvMembers.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Bind Task Views
        rvTasks = view.findViewById(R.id.rv_project_tasks);
        btnAddTask = view.findViewById(R.id.btn_add_project_task);
        tvNoTasks = view.findViewById(R.id.tv_no_tasks);

        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Listeners
        btnBack.setOnClickListener(v -> navigateBack());
        
        layoutEdit.setOnClickListener(v -> {
            ProjectDialogFragment dialog = ProjectDialogFragment.newInstance(projectId);
            dialog.show(getChildFragmentManager(), "EditProjectDialog");
        });

        layoutDelete.setOnClickListener(v -> confirmDeleteProject());

        btnAddMember.setOnClickListener(v -> addMemberToProject());

        btnAddTask.setOnClickListener(v -> showAddTaskDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle("Project Details");
        refreshDetails();
    }

    public void refreshDetails() {
        currentProject = projectRepository.getProjectById(projectId);
        if (currentProject == null) {
            Toast.makeText(requireContext(), "Project not found", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        // Fill Details
        tvProjectTitle.setText(currentProject.getName());
        tvProjectStatus.setText(currentProject.getStatus());
        
        if (currentProject.getDescription() != null && !currentProject.getDescription().isEmpty()) {
            tvProjectDescription.setText(currentProject.getDescription());
        } else {
            tvProjectDescription.setText("No description provided.");
        }

        if (currentProject.getCreatedAt() != null) {
            String date = currentProject.getCreatedAt();
            if (date.length() >= 10) {
                date = date.substring(0, 10);
            }
            tvProjectCreatedAt.setText("Created on " + date);
        } else {
            tvProjectCreatedAt.setText("Created on unknown date");
        }

        // Role authorization check
        long currentUserId = preferencesManager.getCurrentUserId();
        String role = projectRepository.getMemberRole(projectId, currentUserId);
        boolean isLeader = "Leader".equalsIgnoreCase(role);
        
        if (isLeader) {
            cardActions.setVisibility(View.VISIBLE);
            layoutAddMember.setVisibility(View.VISIBLE);
            btnAddTask.setVisibility(View.VISIBLE);
        } else {
            cardActions.setVisibility(View.GONE);
            layoutAddMember.setVisibility(View.GONE);
            btnAddTask.setVisibility(View.GONE);
        }

        // Load project members
        List<User> members = projectRepository.getMembersForProject(projectId);
        memberAdapter = new ProjectMemberAdapter(requireContext(), members, isLeader, currentUserId, this::confirmRemoveMember);
        rvMembers.setAdapter(memberAdapter);

        // Load project tasks
        List<Task> tasks = taskRepository.getTasksForProject(projectId);
        if (tasks.isEmpty()) {
            tvNoTasks.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            tvNoTasks.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
            
            taskAdapter = new TaskAdapter(requireContext(), tasks, new TaskAdapter.OnTaskClickListener() {
                @Override
                public void onTaskClick(Task task) {
                    TaskDetailsFragment detailsFragment = new TaskDetailsFragment();
                    Bundle args = new Bundle();
                    args.putLong("TASK_ID", task.getId());
                    detailsFragment.setArguments(args);
                    
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, detailsFragment)
                            .addToBackStack(null)
                            .commit();
                }

                @Override
                public void onTaskLongClick(Task task, View view) {
                    showTaskOptionsMenu(task, view);
                }
            });
            rvTasks.setAdapter(taskAdapter);
        }
    }

    private void showAddTaskDialog() {
        TaskDialogFragment dialog = TaskDialogFragment.newInstance(projectId);
        dialog.show(getChildFragmentManager(), "AddTaskDialog");
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

        getParentFragmentManager().beginTransaction()
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
                refreshDetails();
            } else {
                Toast.makeText(requireContext(), "Error deleting task", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addMemberToProject() {
        String usernameInput = etMemberUsername.getText().toString().trim();
        if (usernameInput.isEmpty()) {
            Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Verify User Exists
        User targetUser = userRepository.getUserByUsername(usernameInput.toLowerCase());
        if (targetUser == null) {
            Toast.makeText(requireContext(), "User '" + usernameInput + "' not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. Verify Membership Conflict
        boolean isAlreadyMember = projectRepository.isUserMemberOfProject(projectId, targetUser.getId());
        if (isAlreadyMember) {
            Toast.makeText(requireContext(), "User '" + usernameInput + "' is already a member of this project", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. Verify Pending Invitation Conflict
        boolean isAlreadyInvited = invitationRepository.hasPendingInvitation(projectId, targetUser.getId());
        if (isAlreadyInvited) {
            Toast.makeText(requireContext(), "An invitation is already pending for this user", Toast.LENGTH_SHORT).show();
            return;
        }

        // 4. Send Invitation
        long senderUserId = preferencesManager.getCurrentUserId();
        long result = invitationRepository.insertInvitation(projectId, senderUserId, targetUser.getId());
        if (result != -1) {
            Toast.makeText(requireContext(), "Invitation sent to @" + usernameInput, Toast.LENGTH_SHORT).show();
            etMemberUsername.setText("");
        } else {
            Toast.makeText(requireContext(), "Failed to send invitation", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmRemoveMember(User member) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove '" + member.getDisplayName() + "' from this project?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    boolean success = projectRepository.removeMemberFromProject(projectId, member.getId());
                    if (success) {
                        Toast.makeText(requireContext(), "Removed '" + member.getDisplayName() + "'", Toast.LENGTH_SHORT).show();
                        refreshDetails();
                    } else {
                        Toast.makeText(requireContext(), "Failed to remove member", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDeleteProject() {
        if (currentProject == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete '" + currentProject.getName() + "'? This will permanently delete this project, all linked tasks, and all memberships.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    projectRepository.deleteProject(projectId);
                    Toast.makeText(requireContext(), "Project deleted", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateBack() {
        if (getParentFragmentManager() != null) {
            getParentFragmentManager().popBackStack();
        }
    }
}
