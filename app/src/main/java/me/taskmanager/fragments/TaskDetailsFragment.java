package me.taskmanager.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.lifecycle.ViewModelProvider;
import me.taskmanager.R;
import me.taskmanager.adapter.CommentAdapter;
import me.taskmanager.adapter.ActivityLogAdapter;
import me.taskmanager.model.Comment;
import me.taskmanager.model.ActivityLog;
import me.taskmanager.model.Task;
import me.taskmanager.utils.FileHelper;
import me.taskmanager.viewmodel.TaskDetailsViewModel;

public class TaskDetailsFragment extends Fragment {

    private static final int REQUEST_CODE_PICK_FILE = 1001;

    private TextView tvTitle;
    private TextView tvDescription;
    private TextView tvDueDate;
    private TextView tvPriority;
    private TextView tvStatus;
    private Button btnMarkComplete;
    private Button btnEditTask;
    private Button btnAttachFile;
    private Button btnViewFiles;

    // Comments & Logs Views
    private EditText etNewComment;
    private Button btnPostComment;
    private RecyclerView rvComments;
    private RecyclerView rvLogs;
    private TextView tvNoComments;
    private TextView tvNoActivity;

    private TaskDetailsViewModel taskDetailsViewModel;
    private FileHelper fileHelper;
    private Task task;
    private long taskId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_details, container, false);

        // Initialize views
        tvTitle = view.findViewById(R.id.tv_task_title);
        tvDescription = view.findViewById(R.id.tv_task_description);
        tvDueDate = view.findViewById(R.id.tv_due_date);
        tvPriority = view.findViewById(R.id.tv_priority);
        tvStatus = view.findViewById(R.id.tv_status);
        btnMarkComplete = view.findViewById(R.id.btn_mark_complete);
        btnEditTask = view.findViewById(R.id.btn_edit_task);
        btnAttachFile = view.findViewById(R.id.btn_attach_file);
        btnViewFiles = view.findViewById(R.id.btn_view_files);

        // Comments & Logs views
        etNewComment = view.findViewById(R.id.et_new_comment);
        btnPostComment = view.findViewById(R.id.btn_add_comment);
        rvComments = view.findViewById(R.id.rv_task_comments);
        rvLogs = view.findViewById(R.id.rv_task_activity_logs);
        tvNoComments = view.findViewById(R.id.tv_no_comments);
        tvNoActivity = view.findViewById(R.id.tv_no_activity);

        fileHelper = new FileHelper();

        // Setup RecyclerViews
        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get task ID from arguments
        if (getArguments() != null && getArguments().containsKey("TASK_ID")) {
            taskId = getArguments().getLong("TASK_ID");
        }

        // Setup ViewModel
        taskDetailsViewModel = new ViewModelProvider(this).get(TaskDetailsViewModel.class);

        // Observers
        taskDetailsViewModel.getTaskLiveData().observe(getViewLifecycleOwner(), t -> {
            if (t == null) {
                Toast.makeText(requireContext(), "Task not found", Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().popBackStack();
                return;
            }
            task = t;
            displayTaskDetails(t);
        });

        taskDetailsViewModel.getCommentsLiveData().observe(getViewLifecycleOwner(), comments -> {
            if (comments == null) return;
            if (comments.isEmpty()) {
                tvNoComments.setVisibility(View.VISIBLE);
                rvComments.setVisibility(View.GONE);
            } else {
                tvNoComments.setVisibility(View.GONE);
                rvComments.setVisibility(View.VISIBLE);

                long currentUserId = taskDetailsViewModel.getCurrentUserId();
                String role = taskDetailsViewModel.getUserRoleLiveData().getValue();
                boolean isLeader = "Leader".equalsIgnoreCase(role);

                CommentAdapter commentAdapter = new CommentAdapter(requireContext(), comments, currentUserId, isLeader, this::deleteComment);
                rvComments.setAdapter(commentAdapter);
            }
        });

        taskDetailsViewModel.getLogsLiveData().observe(getViewLifecycleOwner(), logs -> {
            if (logs == null) return;
            if (logs.isEmpty()) {
                tvNoActivity.setVisibility(View.VISIBLE);
                rvLogs.setVisibility(View.GONE);
            } else {
                tvNoActivity.setVisibility(View.GONE);
                rvLogs.setVisibility(View.VISIBLE);

                ActivityLogAdapter logAdapter = new ActivityLogAdapter(requireContext(), logs);
                rvLogs.setAdapter(logAdapter);
            }
        });

        taskDetailsViewModel.getAttachedFilesLiveData().observe(getViewLifecycleOwner(), files -> {
            if (files != null) {
                btnViewFiles.setEnabled(files.length > 0);
            }
        });

        // Set up button click listeners
        btnMarkComplete.setOnClickListener(v -> toggleTaskCompletion());
        btnEditTask.setOnClickListener(v -> navigateToEditTask());
        btnAttachFile.setOnClickListener(v -> pickFile());
        btnViewFiles.setOnClickListener(v -> viewAttachedFiles());
        btnPostComment.setOnClickListener(v -> postComment());

        // Add proper back button handling
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    navigateBackToTaskList();
                }
            }
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        if (taskId > 0) {
            loadTask();
        }
    }

    private void navigateBackToTaskList() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void loadTask() {
        taskDetailsViewModel.loadTask(taskId);
    }

    private void displayTaskDetails(Task t) {
        tvTitle.setText(t.getTitle());

        // Handle description
        String description = t.getDescription();
        if (description != null && !description.isEmpty()) {
            tvDescription.setText(description);
            tvDescription.setVisibility(View.VISIBLE);
        } else {
            tvDescription.setVisibility(View.GONE);
        }

        // Format and display due date
        if (t.getDueDate() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy 'at' HH:mm", Locale.getDefault());
            String dueDateText = sdf.format(new Date(t.getDueDate()));
            tvDueDate.setText(dueDateText);
            tvDueDate.setVisibility(View.VISIBLE);
        } else {
            tvDueDate.setText("No due date set");
        }

        // Display priority
        String priorityText;
        switch (t.getPriority()) {
            case 3:
                priorityText = "High";
                break;
            case 2:
                priorityText = "Medium";
                break;
            default:
                priorityText = "Low";
                break;
        }
        tvPriority.setText(priorityText);

        // Display status
        if (t.isCompleted()) {
            tvStatus.setText("Completed (" + t.getStatus() + ")");
            btnMarkComplete.setText("Mark Incomplete");
        } else {
            tvStatus.setText("Pending (" + t.getStatus() + ")");
            btnMarkComplete.setText("Mark Complete");
        }
    }

    private void postComment() {
        String content = etNewComment.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(requireContext(), "Comment cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        taskDetailsViewModel.postComment(taskId, content, (success, message) -> {
            if (success) {
                etNewComment.setText("");
            } else {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteComment(Comment comment) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    taskDetailsViewModel.deleteComment(taskId, comment, (success, message) -> {
                        if (!success) {
                            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleTaskCompletion() {
        if (task == null) return;
        taskDetailsViewModel.toggleTaskCompletion(task, (success, message) -> {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void navigateToEditTask() {
        EditTaskFragment editFragment = new EditTaskFragment();
        Bundle args = new Bundle();
        args.putLong("TASK_ID", taskId);
        editFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, editFragment)
                .addToBackStack(null)
                .commit();
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
    }

    private void viewAttachedFiles() {
        File[] files = taskDetailsViewModel.getAttachedFilesLiveData().getValue();
        if (files == null || files.length == 0) {
            Toast.makeText(requireContext(), "No files attached", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence[] fileNames = new CharSequence[files.length];
        for (int i = 0; i < files.length; i++) {
            fileNames[i] = files[i].getName();
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Attached Files")
                .setItems(fileNames, (dialog, which) -> {
                    openFile(files[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openFile(File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file
        );

        String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        String mimeType;

        switch (extension) {
            case "pdf":
                mimeType = "application/pdf";
                break;
            case "jpg":
            case "jpeg":
            case "png":
                mimeType = "image/*";
                break;
            case "mp4":
                mimeType = "video/*";
                break;
            case "txt":
                mimeType = "text/plain";
                break;
            default:
                mimeType = "*/*";
                break;
        }

        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app available to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();

            if (uri != null) {
                if (uri.toString().startsWith("content://")) {
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    requireActivity().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                }
                
                taskDetailsViewModel.saveAttachedFile(taskId, uri, (success, message) -> {
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        }
    }
}
