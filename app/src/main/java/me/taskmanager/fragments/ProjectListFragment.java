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
import androidx.lifecycle.ViewModelProvider;
import me.taskmanager.model.Project;
import me.taskmanager.model.Invitation;
import me.taskmanager.viewmodel.ProjectListViewModel;

public class ProjectListFragment extends Fragment implements ProjectAdapter.OnProjectClickListener {

    private RecyclerView recyclerView;
    private TextView tvEmptyView;
    private ProjectAdapter adapter;

    private CardView cardInvitationsSection;
    private RecyclerView rvInvitations;
    private InvitationAdapter invitationAdapter;

    private ProjectListViewModel projectListViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_list, container, false);

        recyclerView = view.findViewById(R.id.rv_projects);
        tvEmptyView = view.findViewById(R.id.tv_empty_projects);
        FloatingActionButton fabAddProject = view.findViewById(R.id.fab_add_project);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        cardInvitationsSection = view.findViewById(R.id.card_invitations_section);
        rvInvitations = view.findViewById(R.id.rv_invitations);
        rvInvitations.setLayoutManager(new LinearLayoutManager(requireContext()));

        projectListViewModel = new ViewModelProvider(this).get(ProjectListViewModel.class);

        invitationAdapter = new InvitationAdapter(requireContext(), new ArrayList<>(), new InvitationAdapter.OnInvitationActionListener() {
            @Override
            public void onAccept(Invitation invitation) {
                projectListViewModel.acceptInvitation(invitation, (success, message) -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onDecline(Invitation invitation) {
                projectListViewModel.declineInvitation(invitation, (success, message) -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        });
        rvInvitations.setAdapter(invitationAdapter);

        adapter = new ProjectAdapter(requireContext(), new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);

        // Set up observers
        projectListViewModel.getProjectsLiveData().observe(getViewLifecycleOwner(), list -> {
            if (list == null) return;
            if (list.isEmpty()) {
                tvEmptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                tvEmptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            adapter.updateProjects(list);
        });

        projectListViewModel.getPendingInvitationsLiveData().observe(getViewLifecycleOwner(), pendingInvites -> {
            if (pendingInvites == null || pendingInvites.isEmpty()) {
                cardInvitationsSection.setVisibility(View.GONE);
            } else {
                cardInvitationsSection.setVisibility(View.VISIBLE);
                invitationAdapter.updateInvitations(pendingInvites);
            }
        });

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
        if (projectListViewModel != null) {
            projectListViewModel.loadProjectsAndInvitations();
        }
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
        projectListViewModel.checkMemberRole(project.getId(), role -> {
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
        });
    }

    private void confirmDeleteProject(Project project) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete '" + project.getName() + "'? This will delete all of its tasks and members.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    projectListViewModel.deleteProject(project.getId(), (success, message) -> {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
