package me.taskmanager.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.taskmanager.R;
import me.taskmanager.model.User;

/**
 * Adapter for displaying members of a project in a RecyclerView
 */
public class ProjectMemberAdapter extends RecyclerView.Adapter<ProjectMemberAdapter.MemberViewHolder> {

    private List<User> memberList;
    private final Context context;
    private final boolean isCurrentUserLeader;
    private final long currentUserId;
    private final OnMemberRemoveClickListener listener;

    public interface OnMemberRemoveClickListener {
        void onRemoveClick(User member);
    }

    public ProjectMemberAdapter(Context context, List<User> memberList,
                                boolean isCurrentUserLeader, long currentUserId,
                                OnMemberRemoveClickListener listener) {
        this.context = context;
        this.memberList = memberList != null ? new ArrayList<>(memberList) : new ArrayList<>();
        this.isCurrentUserLeader = isCurrentUserLeader;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_project_member, parent, false);
        return new MemberViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        User member = memberList.get(position);
        holder.bind(member);
    }

    @Override
    public int getItemCount() {
        return memberList == null ? 0 : memberList.size();
    }

    public void updateMembers(List<User> newMembers) {
        this.memberList = new ArrayList<>(newMembers);
        notifyDataSetChanged();
    }

    public class MemberViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatarText;
        private final TextView tvName;
        private final TextView tvUsername;
        private final TextView tvRole;
        private final ImageButton btnRemove;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarText = itemView.findViewById(R.id.tv_member_avatar_text);
            tvName = itemView.findViewById(R.id.tv_member_name);
            tvUsername = itemView.findViewById(R.id.tv_member_username);
            tvRole = itemView.findViewById(R.id.tv_member_role);
            btnRemove = itemView.findViewById(R.id.btn_remove_member);
        }

        public void bind(final User member) {
            String displayName = member.getDisplayName();
            tvName.setText(displayName);
            tvUsername.setText("@" + member.getUsername());

            if (displayName != null && !displayName.isEmpty()) {
                tvAvatarText.setText(displayName.substring(0, 1).toUpperCase());
            } else {
                tvAvatarText.setText("M");
            }

            // Role Badge Styling
            String role = member.getProjectRole();
            if ("Leader".equalsIgnoreCase(role)) {
                tvRole.setText("LEADER");
                tvRole.setTextColor(ContextCompat.getColor(context, R.color.primary));
                tvRole.setBackgroundColor(Color.parseColor("#156200EA")); // Light purple tint
            } else {
                tvRole.setText("MEMBER");
                tvRole.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                tvRole.setBackgroundColor(Color.parseColor("#15757575")); // Light grey tint
            }

            // Remove Button visibility rules
            if (isCurrentUserLeader && member.getId() != currentUserId && !"Leader".equalsIgnoreCase(role)) {
                btnRemove.setVisibility(View.VISIBLE);
            } else {
                btnRemove.setVisibility(View.GONE);
            }

            btnRemove.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRemoveClick(member);
                }
            });
        }
    }
}
