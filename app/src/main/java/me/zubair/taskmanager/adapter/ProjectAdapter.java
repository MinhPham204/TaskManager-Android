package me.zubair.taskmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.zubair.taskmanager.R;
import me.zubair.taskmanager.model.Project;

/**
 * Adapter for displaying projects in a RecyclerView
 */
public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private final Context context;
    private final OnProjectClickListener listener;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
        void onProjectLongClick(Project project, View itemView);
    }

    public ProjectAdapter(Context context, List<Project> projectList, OnProjectClickListener listener) {
        this.context = context;
        this.projectList = projectList != null ? new ArrayList<>(projectList) : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        holder.bind(project);
    }

    @Override
    public int getItemCount() {
        return projectList == null ? 0 : projectList.size();
    }

    public void updateProjects(List<Project> newProjects) {
        final List<Project> oldList = new ArrayList<>(this.projectList);
        final List<Project> newList = new ArrayList<>(newProjects);

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return oldList.get(oldItemPosition).getId() == newList.get(newItemPosition).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Project oldProj = oldList.get(oldItemPosition);
                Project newProj = newList.get(newItemPosition);

                return oldProj.getName().equals(newProj.getName()) &&
                        oldProj.getDescription().equals(newProj.getDescription()) &&
                        oldProj.getStatus().equals(newProj.getStatus()) &&
                        oldProj.getCreatedAt().equals(newProj.getCreatedAt());
            }
        });

        this.projectList = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    public class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final TextView statusTextView;
        private final TextView createdAtTextView;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_view_project_title);
            descriptionTextView = itemView.findViewById(R.id.text_view_project_description);
            statusTextView = itemView.findViewById(R.id.text_view_project_status);
            createdAtTextView = itemView.findViewById(R.id.text_view_project_created_at);
        }

        public void bind(final Project project) {
            titleTextView.setText(project.getName());
            descriptionTextView.setText(project.getDescription());
            statusTextView.setText(project.getStatus());

            if (project.getCreatedAt() != null) {
                String date = project.getCreatedAt();
                if (date.length() >= 10) {
                    date = date.substring(0, 10);
                }
                createdAtTextView.setText("Created on " + date);
                createdAtTextView.setVisibility(View.VISIBLE);
            } else {
                createdAtTextView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project);
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onProjectLongClick(project, itemView);
                    return true;
                }
                return false;
            });
        }
    }
}
