package me.taskmanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.taskmanager.R;
import me.taskmanager.model.Invitation;

public class InvitationAdapter extends RecyclerView.Adapter<InvitationAdapter.ViewHolder> {

    public interface OnInvitationActionListener {
        void onAccept(Invitation invitation);
        void onDecline(Invitation invitation);
    }

    private final Context context;
    private final List<Invitation> invitations;
    private final OnInvitationActionListener listener;

    public InvitationAdapter(Context context, List<Invitation> invitations, OnInvitationActionListener listener) {
        this.context = context;
        this.invitations = invitations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_invitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Invitation invitation = invitations.get(position);

        String message = invitation.getSenderFullName() + " (@" + invitation.getSenderUsername() + 
                ") invited you to join \"" + invitation.getProjectName() + "\"";
        holder.tvMessage.setText(message);
        holder.tvTime.setText(invitation.getCreatedAt());

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAccept(invitation);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDecline(invitation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return invitations.size();
    }

    public void updateInvitations(List<Invitation> newInvitations) {
        this.invitations.clear();
        this.invitations.addAll(newInvitations);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        TextView tvTime;
        Button btnAccept;
        Button btnDecline;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_invite_message);
            tvTime = itemView.findViewById(R.id.tv_invite_time);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnDecline = itemView.findViewById(R.id.btn_decline);
        }
    }
}
