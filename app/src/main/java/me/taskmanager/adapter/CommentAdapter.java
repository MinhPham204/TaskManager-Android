package me.taskmanager.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.taskmanager.R;
import me.taskmanager.model.Comment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final Context context;
    private final List<Comment> commentList;
    private final long currentUserId;
    private final boolean isProjectLeader;
    private final OnCommentDeleteListener deleteListener;

    public interface OnCommentDeleteListener {
        void onCommentDelete(Comment comment);
    }

    public CommentAdapter(Context context, List<Comment> commentList, long currentUserId, boolean isProjectLeader, OnCommentDeleteListener deleteListener) {
        this.context = context;
        this.commentList = commentList;
        this.currentUserId = currentUserId;
        this.isProjectLeader = isProjectLeader;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return commentList != null ? commentList.size() : 0;
    }

    public class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatar;
        private final TextView tvName;
        private final TextView tvUsername;
        private final TextView tvTime;
        private final TextView tvContent;
        private final ImageButton btnDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tv_commenter_avatar);
            tvName = itemView.findViewById(R.id.tv_commenter_name);
            tvUsername = itemView.findViewById(R.id.tv_commenter_username);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvContent = itemView.findViewById(R.id.tv_comment_content);
            btnDelete = itemView.findViewById(R.id.btn_delete_comment);
        }

        public void bind(final Comment comment) {
            tvName.setText(comment.getUserFullName());
            tvUsername.setText("@" + comment.getUsername());
            tvContent.setText(comment.getContent());
            tvTime.setText(comment.getCreatedAt());

            // Generate initials for avatar
            String initials = "";
            String fullName = comment.getUserFullName();
            if (fullName != null && !fullName.trim().isEmpty()) {
                String[] parts = fullName.trim().split("\\s+");
                if (parts.length > 0) {
                    initials += parts[0].substring(0, 1).toUpperCase();
                    if (parts.length > 1) {
                        initials += parts[parts.length - 1].substring(0, 1).toUpperCase();
                    }
                }
            } else if (comment.getUsername() != null && !comment.getUsername().isEmpty()) {
                initials = comment.getUsername().substring(0, 1).toUpperCase();
            } else {
                initials = "?";
            }
            tvAvatar.setText(initials);

            // Style circle avatar background programmatically
            int[] avatarColors = {
                Color.parseColor("#E91E63"), Color.parseColor("#9C27B0"),
                Color.parseColor("#673AB7"), Color.parseColor("#3F51B5"),
                Color.parseColor("#2196F3"), Color.parseColor("#009688"),
                Color.parseColor("#4CAF50"), Color.parseColor("#FF9800")
            };
            int colorIndex = 0;
            if (comment.getUsername() != null && !comment.getUsername().isEmpty()) {
                colorIndex = Math.abs(comment.getUsername().hashCode()) % avatarColors.length;
            }
            int avatarBgColor = avatarColors[colorIndex];

            GradientDrawable avatarBg = new GradientDrawable();
            avatarBg.setShape(GradientDrawable.OVAL);
            avatarBg.setColor(avatarBgColor);
            tvAvatar.setBackground(avatarBg);

            // Show delete button only if commenter is current user OR user is project leader
            if (comment.getUserId() == currentUserId || isProjectLeader) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> {
                    if (deleteListener != null) {
                        deleteListener.onCommentDelete(comment);
                    }
                });
            } else {
                btnDelete.setVisibility(View.GONE);
            }
        }
    }
}
