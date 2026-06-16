package me.taskmanager.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import me.taskmanager.R;
import me.taskmanager.model.ActivityLog;

public class ActivityLogAdapter extends RecyclerView.Adapter<ActivityLogAdapter.LogViewHolder> {

    private final Context context;
    private final List<ActivityLog> logList;

    public ActivityLogAdapter(Context context, List<ActivityLog> logList) {
        this.context = context;
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_activity_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        ActivityLog log = logList.get(position);
        holder.bind(log);
    }

    @Override
    public int getItemCount() {
        return logList != null ? logList.size() : 0;
    }

    public class LogViewHolder extends RecyclerView.ViewHolder {
        private final View viewBullet;
        private final TextView tvMessage;
        private final TextView tvTime;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            viewBullet = itemView.findViewById(R.id.view_bullet);
            tvMessage = itemView.findViewById(R.id.tv_log_message);
            tvTime = itemView.findViewById(R.id.tv_log_time);
        }

        public void bind(final ActivityLog log) {
            String actor = log.getActorDisplayName();
            String formattedMessage = actor + " " + log.getAction();
            tvMessage.setText(formattedMessage);
            tvTime.setText(log.getCreatedAt());

            // Style circle bullet point
            GradientDrawable bulletBg = new GradientDrawable();
            bulletBg.setShape(GradientDrawable.OVAL);
            bulletBg.setColor(Color.parseColor("#BDBDBD")); // standard grey bullet
            viewBullet.setBackground(bulletBg);
        }
    }
}
