package me.taskmanager.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import me.taskmanager.R;
import me.taskmanager.model.HomeStats;
import me.taskmanager.viewmodel.HomeViewModel;

import androidx.lifecycle.ViewModelProvider;

public class HomeFragment extends Fragment {

    private TextView tvCurrentDate;
    private TextView tvCurrentTime;
    private TextView tvQuote;
    private TextView tvQuoteAuthor;
    private TextView tvTaskCount;
    private Button btnViewTasks;

    // Dashboard statistics views
    private TextView tvProgressPercent;
    private ProgressBar progressBarCompletion;
    private TextView tvStatProjects;
    private TextView tvStatTotalTasks;
    private TextView tvStatCompleted;
    private TextView tvStatPending;
    private TextView tvStatOverdue;

    private HomeViewModel homeViewModel;

    private Handler timeHandler;
    private Runnable timeRunnable;

    private final String[][] quotes = {
            {"The secret of getting ahead is getting started.", "Mark Twain"},
            {"Don't watch the clock; do what it does. Keep going.", "Sam Levenson"},
            {"The way to get started is to quit talking and begin doing.", "Walt Disney"},
            {"The future depends on what you do today.", "Mahatma Gandhi"},
            {"You don't have to see the whole staircase, just take the first step.", "Martin Luther King, Jr."},
            {"It always seems impossible until it's done.", "Nelson Mandela"}
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize views
        tvCurrentDate = view.findViewById(R.id.text_view_current_date);
        tvCurrentTime = view.findViewById(R.id.text_view_current_time);
        tvQuote = view.findViewById(R.id.text_view_quote);
        tvQuoteAuthor = view.findViewById(R.id.text_view_quote_author);
        tvTaskCount = view.findViewById(R.id.text_view_task_count);
        btnViewTasks = view.findViewById(R.id.button_view_tasks);

        // Statistics views
        tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        progressBarCompletion = view.findViewById(R.id.progress_bar_task_completion);
        tvStatProjects = view.findViewById(R.id.tv_stat_projects);
        tvStatTotalTasks = view.findViewById(R.id.tv_stat_total_tasks);
        tvStatCompleted = view.findViewById(R.id.tv_stat_completed);
        tvStatPending = view.findViewById(R.id.tv_stat_pending);
        tvStatOverdue = view.findViewById(R.id.tv_stat_overdue);

        // Setup ViewModel
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Observe ViewModel live data
        homeViewModel.getHomeStatsLiveData().observe(getViewLifecycleOwner(), this::updateDashboardUi);

        // Set random motivational quote
        setRandomQuote();

        // Setup clock updates
        setupClock();

        // Set button click listener
        btnViewTasks.setOnClickListener(v -> navigateToTaskList());

        return view;
    }

    private void setupClock() {
        timeHandler = new Handler(Looper.getMainLooper());
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateTime();
                timeHandler.postDelayed(this, 1000);
            }
        };
    }

    private void updateDateTime() {
        Date currentDate = new Date();
        
        // Format and set current date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(currentDate);
        tvCurrentDate.setText(formattedDate);
        
        // Format and set current time
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        String formattedTime = timeFormat.format(currentDate);
        tvCurrentTime.setText(formattedTime);
    }

    private void setRandomQuote() {
        int randomIndex = new Random().nextInt(quotes.length);
        tvQuote.setText(quotes[randomIndex][0]);
        tvQuoteAuthor.setText("- " + quotes[randomIndex][1]);
    }

    private void updateDashboardUi(HomeStats stats) {
        if (stats == null) return;

        tvStatProjects.setText(String.valueOf(stats.getTotalProjects()));
        tvStatTotalTasks.setText(String.valueOf(stats.getTotalTasks()));
        tvStatCompleted.setText(String.valueOf(stats.getCompletedTasks()));
        tvStatPending.setText(String.valueOf(stats.getPendingTasks()));
        tvStatOverdue.setText(String.valueOf(stats.getOverdueTasks()));

        tvProgressPercent.setText("Progress: " + stats.getProgressPercent() + "%");
        progressBarCompletion.setProgress(stats.getProgressPercent());

        tvTaskCount.setText(stats.getPendingText());
    }

    private void navigateToTaskList() {
        TaskListFragment taskListFragment = new TaskListFragment();
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, taskListFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (homeViewModel != null) {
            homeViewModel.loadDashboardStats();
        }
        updateDateTime();
        timeHandler.post(timeRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        timeHandler.removeCallbacks(timeRunnable);
    }
}
