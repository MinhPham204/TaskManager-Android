package me.taskmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import me.taskmanager.R;
import me.taskmanager.database.TaskRepository;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.database.UserRepository;
import me.taskmanager.model.Task;
import me.taskmanager.model.Project;
import me.taskmanager.model.User;

/**
 * Adapter for displaying tasks in a RecyclerView
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private final Context context;
    private final OnTaskClickListener listener;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // Interface for handling item clicks
    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onTaskLongClick(Task task, View itemView);
    }

    public TaskAdapter(Context context, List<Task> taskList, OnTaskClickListener listener) {
        this.context = context;
        this.taskList = taskList != null ? new ArrayList<>(taskList) : new ArrayList<>();
        this.listener = listener;
        this.taskRepository = new TaskRepository(context);
        this.projectRepository = new ProjectRepository(context);
        this.userRepository = new UserRepository(context);
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return taskList == null ? 0 : taskList.size();
    }

    public void updateTasks(List<Task> newTasks) {
        final List<Task> oldList = new ArrayList<>(this.taskList);
        final List<Task> newList = new ArrayList<>(newTasks);
        
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
                Task oldTask = oldList.get(oldItemPosition);
                Task newTask = newList.get(newItemPosition);
                
                boolean projectSame = (oldTask.getProjectId() == null && newTask.getProjectId() == null) ||
                                      (oldTask.getProjectId() != null && oldTask.getProjectId().equals(newTask.getProjectId()));
                boolean assigneeSame = (oldTask.getAssignedUserId() == null && newTask.getAssignedUserId() == null) ||
                                       (oldTask.getAssignedUserId() != null && oldTask.getAssignedUserId().equals(newTask.getAssignedUserId()));
                boolean statusSame = oldTask.getStatus().equals(newTask.getStatus());

                return oldTask.getTitle().equals(newTask.getTitle()) &&
                       oldTask.getDescription().equals(newTask.getDescription()) &&
                       oldTask.getDueDate() == newTask.getDueDate() &&
                       oldTask.getPriority() == newTask.getPriority() &&
                       oldTask.isCompleted() == newTask.isCompleted() &&
                       projectSame &&
                       assigneeSame &&
                       statusSame;
            }
        });
        
        this.taskList = newList;
        diffResult.dispatchUpdatesTo(this);
    }

    public Task getTaskAt(int position) {
        return taskList.get(position);
    }

    public class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView descriptionTextView;
        private final TextView dueDateTextView;
        private final CheckBox completedCheckBox;
        private final CardView taskCardView;
        private final ImageView priorityIndicator;
        private final TextView projectTextView;
        private final TextView assigneeTextView;
        private final TextView statusTextView;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.text_view_task_title);
            descriptionTextView = itemView.findViewById(R.id.text_view_task_description);
            dueDateTextView = itemView.findViewById(R.id.text_view_task_due_date);
            completedCheckBox = itemView.findViewById(R.id.checkbox_task_completed);
            taskCardView = itemView.findViewById(R.id.card_view_task);
            priorityIndicator = itemView.findViewById(R.id.image_view_priority_indicator);
            projectTextView = itemView.findViewById(R.id.text_view_task_project);
            assigneeTextView = itemView.findViewById(R.id.text_view_task_assignee);
            statusTextView = itemView.findViewById(R.id.text_view_task_status);
        }

        public void bind(final Task task) {
            titleTextView.setText(task.getTitle());
            descriptionTextView.setText(task.getDescription());
            
            // Format and set due date if available
            if (task.getDueDate() != 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = sdf.format(new Date(task.getDueDate()));
                dueDateTextView.setText(formattedDate);
                dueDateTextView.setVisibility(View.VISIBLE);
            } else {
                dueDateTextView.setVisibility(View.GONE);
            }

            // Set completion status
            completedCheckBox.setChecked(task.isCompleted());
            
            // Set card background based on completion status
            if (task.isCompleted()) {
                taskCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.task_completed_background));
                titleTextView.setAlpha(0.7f);
                descriptionTextView.setAlpha(0.7f);
            } else {
                taskCardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.task_card_background));
                titleTextView.setAlpha(1.0f);
                descriptionTextView.setAlpha(1.0f);
            }
            
            // Set priority indicator
            setPriorityIndicator(task.getPriority(), priorityIndicator);

            // Bind Project name
            if (task.getProjectId() != null && task.getProjectId() > 0) {
                Project project = projectRepository.getProjectById(task.getProjectId());
                if (project != null) {
                    projectTextView.setText(project.getName());
                    projectTextView.setVisibility(View.VISIBLE);
                } else {
                    projectTextView.setVisibility(View.GONE);
                }
            } else {
                projectTextView.setVisibility(View.GONE);
            }

            // Bind Assignee name
            if (task.getAssignedUserId() != null && task.getAssignedUserId() > 0) {
                User user = userRepository.getUserById(task.getAssignedUserId());
                if (user != null) {
                    assigneeTextView.setText("@" + user.getUsername());
                    assigneeTextView.setVisibility(View.VISIBLE);
                } else {
                    assigneeTextView.setVisibility(View.GONE);
                }
            } else {
                assigneeTextView.setVisibility(View.GONE);
            }

            // Bind and Style Status Badge
            String status = task.getStatus();
            statusTextView.setText(status.replace("_", " "));
            
            int textColor = Color.parseColor("#757575");
            int bgColor = Color.parseColor("#E0E0E0");
            
            switch (status) {
                case Task.STATUS_TODO:
                    textColor = Color.parseColor("#757575");
                    bgColor = Color.parseColor("#E0E0E0");
                    break;
                case Task.STATUS_IN_PROGRESS:
                    textColor = Color.parseColor("#1976D2");
                    bgColor = Color.parseColor("#E3F2FD");
                    break;
                case Task.STATUS_DONE:
                    textColor = Color.parseColor("#388E3C");
                    bgColor = Color.parseColor("#E8F5E9");
                    break;
                case Task.STATUS_CANCELLED:
                    textColor = Color.parseColor("#D32F2F");
                    bgColor = Color.parseColor("#FFEBEE");
                    break;
            }
            
            float density = context.getResources().getDisplayMetrics().density;
            GradientDrawable badge = new GradientDrawable();
            badge.setShape(GradientDrawable.RECTANGLE);
            badge.setCornerRadius(6 * density);
            badge.setColor(bgColor);
            
            statusTextView.setBackground(badge);
            statusTextView.setTextColor(textColor);

            // Setup listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTaskClick(task);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onTaskLongClick(task, itemView);
                    return true;
                }
                return false;
            });
            
            completedCheckBox.setOnClickListener(v -> {
                boolean isChecked = completedCheckBox.isChecked();
                task.setCompleted(isChecked);
                taskRepository.updateTask(task);
                notifyItemChanged(getAdapterPosition());
            });
        }
        
        private void setPriorityIndicator(int priority, ImageView indicator) {
            indicator.setVisibility(View.VISIBLE);
            switch (priority) {
                case Task.PRIORITY_HIGH:
                    indicator.setColorFilter(ContextCompat.getColor(context, R.color.priority_high));
                    break;
                case Task.PRIORITY_MEDIUM:
                    indicator.setColorFilter(ContextCompat.getColor(context, R.color.priority_medium));
                    break;
                case Task.PRIORITY_LOW:
                    indicator.setColorFilter(ContextCompat.getColor(context, R.color.priority_low));
                    break;
                default:
                    indicator.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
