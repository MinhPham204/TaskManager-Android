package me.taskmanager.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import me.taskmanager.R;
import me.taskmanager.adapter.ProjectAdapter;
import me.taskmanager.adapter.InvitationAdapter;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.database.InvitationRepository;
import me.taskmanager.model.Project;
import me.taskmanager.model.Invitation;
import me.taskmanager.preferences.UserPreferencesManager;

public class ProjectListFragment extends Fragment implements ProjectAdapter.OnProjectClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyView;
    private ProjectAdapter adapter;
    private ProjectRepository projectRepository;
    private UserPreferencesManager preferencesManager;

    private CardView cardInvitationsSection;
    private RecyclerView rvInvitations;
    private InvitationAdapter invitationAdapter;
    private InvitationRepository invitationRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_list, container, false);

        recyclerView = view.findViewById(R.id.rv_projects);
        tvEmptyView = view.findViewById(R.id.tv_empty_projects);
        FloatingActionButton fabAddProject = view.findViewById(R.id.fab_add_project);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        projectRepository = new ProjectRepository(requireContext());
        preferencesManager = new UserPreferencesManager(requireContext());

        cardInvitationsSection = view.findViewById(R.id.card_invitations_section);
        rvInvitations = view.findViewById(R.id.rv_invitations);
        rvInvitations.setLayoutManager(new LinearLayoutManager(requireContext()));
        invitationRepository = new InvitationRepository(requireContext());

        invitationAdapter = new InvitationAdapter(requireContext(), new ArrayList<>(), new InvitationAdapter.OnInvitationActionListener() {
            @Override
            public void onAccept(Invitation invitation) {
                boolean statusOk = invitationRepository.updateInvitationStatus(invitation.getId(), "ACCEPTED");
                if (statusOk) {
                    long currentUserId = preferencesManager.getCurrentUserId();
                    boolean joinOk = projectRepository.addMemberToProject(invitation.getProjectId(), currentUserId, "Member");
                    if (joinOk) {
                        Toast.makeText(requireContext(), "Joined project: " + invitation.getProjectName(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Accepted invitation but failed to update membership table", Toast.LENGTH_SHORT).show();
                    }
                    refreshProjects();
                } else {
                    Toast.makeText(requireContext(), "Failed to accept invitation", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDecline(Invitation invitation) {
                boolean statusOk = invitationRepository.updateInvitationStatus(invitation.getId(), "REJECTED");
                if (statusOk) {
                    Toast.makeText(requireContext(), "Declined invitation to join " + invitation.getProjectName(), Toast.LENGTH_SHORT).show();
                    refreshProjects();
                } else {
                    Toast.makeText(requireContext(), "Failed to decline invitation", Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvInvitations.setAdapter(invitationAdapter);

        adapter = new ProjectAdapter(requireContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        fabAddProject.setOnClickListener(v -> {
            ProjectDialogFragment dialog = new ProjectDialogFragment();
            dialog.show(getChildFragmentManager(), "CreateProjectDialog");
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle("Projects");
        refreshProjects();
    }

    public void refreshProjects() {
        long currentUserId = preferencesManager.getCurrentUserId();
        if (currentUserId == -1) return;

        // 1. Refresh Pending Invitations
        List<Invitation> pendingInvites = invitationRepository.getPendingInvitationsForUser(currentUserId);
        if (pendingInvites.isEmpty()) {
            cardInvitationsSection.setVisibility(View.GONE);
        } else {
            cardInvitationsSection.setVisibility(View.VISIBLE);
            invitationAdapter.updateInvitations(pendingInvites);
        }

        // 2. Refresh Projects
        List<Project> list = projectRepository.getAllProjectsForUser(currentUserId);
        if (list.isEmpty()) {
            tvEmptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
        adapter.updateProjects(list);
    }

    @Override
    public void onProjectClick(Project project) {
        ProjectDetailsFragment detailsFragment = ProjectDetailsFragment.newInstance(project.getId());
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack("project_flow")
                .commit();
    }

    @Override
    public void onProjectLongClick(Project project, View itemView) {
        long userId = preferencesManager.getCurrentUserId();
        String role = projectRepository.getMemberRole(project.getId(), userId);

        // Security check: Only Leader can edit/delete
        if (!"Leader".equalsIgnoreCase(role)) {
            return;
        }

        PopupMenu popup = new PopupMenu(requireContext(), itemView);
        popup.getMenu().add(0, 1, 0, "Edit Project");
        popup.getMenu().add(0, 2, 1, "Delete Project");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                ProjectDialogFragment dialog = ProjectDialogFragment.newInstance(project.getId());
                dialog.show(getChildFragmentManager(), "EditProjectDialog");
                return true;
            } else if (item.getItemId() == 2) {
                confirmDeleteProject(project);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmDeleteProject(Project project) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete '" + project.getName() + "'? This will delete all of its tasks and members.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    projectRepository.deleteProject(project.getId());
                    refreshProjects();
                    Toast.makeText(requireContext(), "Project deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
