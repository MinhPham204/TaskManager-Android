package me.taskmanager.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import me.taskmanager.R;
import me.taskmanager.activity.MainActivity;
import me.taskmanager.adapter.TaskAdapter;
import me.taskmanager.database.ProjectRepository;
import me.taskmanager.database.TaskRepository;
import me.taskmanager.model.Task;
import me.taskmanager.preferences.UserPreferencesManager;

public class CalendarFragment extends Fragment implements TaskAdapter.OnTaskClickListener {

    private ImageButton btnPrevMonth;
    private ImageButton btnNextMonth;
    private TextView tvMonthYear;
    private GridView gridCalendar;
    private TextView tvSelectedDateHeader;
    private RecyclerView rvTasks;
    private TextView tvEmptyTasks;

    private TaskRepository taskRepository;
    private ProjectRepository projectRepository;
    private UserPreferencesManager preferencesManager;

    private Calendar currentMonthCal; // Controls displayed month grid
    private Calendar selectedDateCal; // Controls selected date highlights
    private List<Calendar> dayCells = new ArrayList<>();
    private List<Task> userTasks = new ArrayList<>();

    private CalendarDayAdapter calendarDayAdapter;
    private TaskAdapter taskAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Repos and preferences
        taskRepository = new TaskRepository(requireContext());
        projectRepository = new ProjectRepository(requireContext());
        preferencesManager = new UserPreferencesManager(requireContext());

        // Bind Views
        btnPrevMonth = view.findViewById(R.id.btn_prev_month);
        btnNextMonth = view.findViewById(R.id.btn_next_month);
        tvMonthYear = view.findViewById(R.id.tv_month_year);
        gridCalendar = view.findViewById(R.id.grid_calendar);
        tvSelectedDateHeader = view.findViewById(R.id.tv_selected_date_header);
        rvTasks = view.findViewById(R.id.rv_calendar_tasks);
        tvEmptyTasks = view.findViewById(R.id.tv_empty_calendar_tasks);

        rvTasks.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Setup Month & Year Calendar defaults to June 2026
        currentMonthCal = Calendar.getInstance();
        currentMonthCal.set(Calendar.YEAR, 2026);
        currentMonthCal.set(Calendar.MONTH, Calendar.JUNE);
        currentMonthCal.set(Calendar.DAY_OF_MONTH, 1);

        selectedDateCal = Calendar.getInstance();
        selectedDateCal.set(Calendar.YEAR, 2026);
        selectedDateCal.set(Calendar.MONTH, Calendar.JUNE);
        selectedDateCal.set(Calendar.DAY_OF_MONTH, 7); // Default highlighted day

        // Wires month navigation listeners
        btnPrevMonth.setOnClickListener(v -> changeMonth(-1));
        btnNextMonth.setOnClickListener(v -> changeMonth(1));

        // Grid Click listener
        gridCalendar.setOnItemClickListener((parent, v, position, id) -> {
            Calendar clickedDay = dayCells.get(position);
            selectedDateCal.setTimeInMillis(clickedDay.getTimeInMillis());
            calendarDayAdapter.notifyDataSetChanged();
            updateSelectedTasksList();
        });

        // Load data and draw views
        loadUserDataAndRefresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle("Calendar");

        // Sync Bottom Navigation View UI state checked item
        if (getActivity() instanceof MainActivity) {
            BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) {
                nav.getMenu().findItem(R.id.nav_calendar).setChecked(true);
            }
        }

        loadUserDataAndRefresh();
    }

    private void loadUserDataAndRefresh() {
        // 1. Fetch user-centric tasks
        long currentUserId = preferencesManager.getCurrentUserId();
        if (currentUserId == -1) return;

        List<Task> allTasks = taskRepository.getAllTasks();
        userTasks.clear();
        for (Task t : allTasks) {
            if (t.getProjectId() == null || t.getProjectId() == 0) {
                if (t.getAssignedUserId() == null || t.getAssignedUserId() == currentUserId) {
                    userTasks.add(t);
                }
            } else {
                if (projectRepository.isUserMemberOfProject(t.getProjectId(), currentUserId)) {
                    userTasks.add(t);
                }
            }
        }

        // 2. Refresh Calendar Grid
        rebuildCalendarGrid();
    }

    private void rebuildCalendarGrid() {
        // Set header label (e.g. "June 2026")
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthYear.setText(sdf.format(currentMonthCal.getTime()));

        // Calculate days logic
        dayCells.clear();
        Calendar tempCal = (Calendar) currentMonthCal.clone();
        tempCal.set(Calendar.DAY_OF_MONTH, 1);

        // Monday start offset calculations
        int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK);
        int offset = firstDayOfWeek - Calendar.MONDAY;
        if (offset < 0) {
            offset += 7;
        }

        // Add leading buffer days from previous month
        tempCal.add(Calendar.DAY_OF_MONTH, -offset);
        for (int i = 0; i < 42; i++) {
            dayCells.add((Calendar) tempCal.clone());
            tempCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Setup Day grid adapter
        if (calendarDayAdapter == null) {
            calendarDayAdapter = new CalendarDayAdapter(requireContext());
            gridCalendar.setAdapter(calendarDayAdapter);
        } else {
            calendarDayAdapter.notifyDataSetChanged();
        }

        // Update list of tasks for selected day
        updateSelectedTasksList();
    }

    private void changeMonth(int step) {
        currentMonthCal.add(Calendar.MONTH, step);
        rebuildCalendarGrid();
    }

    private void updateSelectedTasksList() {
        // Format selection label (e.g. "Tasks due on June 15, 2026")
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
        tvSelectedDateHeader.setText("Tasks due on " + sdf.format(selectedDateCal.getTime()));

        // Filter tasks due on selected day
        List<Task> filtered = new ArrayList<>();
        for (Task t : userTasks) {
            if (t.getDueDate() > 0) {
                Calendar taskCal = Calendar.getInstance();
                taskCal.setTimeInMillis(t.getDueDate());
                if (taskCal.get(Calendar.YEAR) == selectedDateCal.get(Calendar.YEAR) &&
                    taskCal.get(Calendar.DAY_OF_YEAR) == selectedDateCal.get(Calendar.DAY_OF_YEAR)) {
                    filtered.add(t);
                }
            }
        }

        // Show or hide recycler view list empty indicators
        if (filtered.isEmpty()) {
            tvEmptyTasks.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
        } else {
            tvEmptyTasks.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);
            
            taskAdapter = new TaskAdapter(requireContext(), filtered, this);
            rvTasks.setAdapter(taskAdapter);
        }
    }

    // Task click transitions to detail fragment
    @Override
    public void onTaskClick(Task task) {
        TaskDetailsFragment detailsFragment = new TaskDetailsFragment();
        Bundle args = new Bundle();
        args.putLong("TASK_ID", task.getId());
        args.putBoolean("FROM_CALENDAR", true); // pass backpress selection key
        detailsFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack("calendar_flow")
                .commit();
    }

    @Override
    public void onTaskLongClick(Task task, View view) {
        // No task option popup menu actions on calendar to preserve visual cluttering
        Toast.makeText(requireContext(), "Tap task to view details", Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────
    // CUSTOM GRID ADAPTER
    // ─────────────────────────────────────────────────────────

    private class CalendarDayAdapter extends BaseAdapter {

        private final Context context;

        public CalendarDayAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getCount() {
            return dayCells.size();
        }

        @Override
        public Object getItem(int position) {
            return dayCells.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);
            }

            Calendar day = dayCells.get(position);

            TextView tvDayNumber = convertView.findViewById(R.id.tv_day_number);
            View viewSelectionCircle = convertView.findViewById(R.id.view_selection_circle);
            View viewDotHigh = convertView.findViewById(R.id.view_dot_high);
            View viewDotMedium = convertView.findViewById(R.id.view_dot_medium);
            View viewDotLow = convertView.findViewById(R.id.view_dot_low);

            // Set day number label
            tvDayNumber.setText(String.valueOf(day.get(Calendar.DAY_OF_MONTH)));

            // Style day cell checks
            boolean isCurrentMonth = day.get(Calendar.MONTH) == currentMonthCal.get(Calendar.MONTH) &&
                                     day.get(Calendar.YEAR) == currentMonthCal.get(Calendar.YEAR);
            
            boolean isSelected = day.get(Calendar.YEAR) == selectedDateCal.get(Calendar.YEAR) &&
                                 day.get(Calendar.DAY_OF_YEAR) == selectedDateCal.get(Calendar.DAY_OF_YEAR);

            if (isSelected) {
                viewSelectionCircle.setVisibility(View.VISIBLE);
                viewSelectionCircle.getBackground().setTint(context.getResources().getColor(R.color.primary));
                tvDayNumber.setTextColor(context.getResources().getColor(R.color.white));
            } else {
                viewSelectionCircle.setVisibility(View.INVISIBLE);
                if (isCurrentMonth) {
                    tvDayNumber.setTextColor(context.getResources().getColor(R.color.text_primary));
                } else {
                    // Dim days of adjacent months
                    tvDayNumber.setTextColor(context.getResources().getColor(R.color.text_secondary) & 0x60FFFFFF | 0x40000000);
                }
            }

            // Bind task priority deadline dot indicators
            boolean hasHigh = false;
            boolean hasMedium = false;
            boolean hasLow = false;

            for (Task t : userTasks) {
                if (t.getDueDate() > 0) {
                    Calendar taskCal = Calendar.getInstance();
                    taskCal.setTimeInMillis(t.getDueDate());
                    if (taskCal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                        taskCal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
                        if (t.getPriority() == 3) {
                            hasHigh = true;
                        } else if (t.getPriority() == 2) {
                            hasMedium = true;
                        } else {
                            hasLow = true;
                        }
                    }
                }
            }

            viewDotHigh.setVisibility(hasHigh ? View.VISIBLE : View.GONE);
            viewDotMedium.setVisibility(hasMedium ? View.VISIBLE : View.GONE);
            viewDotLow.setVisibility(hasLow ? View.VISIBLE : View.GONE);

            return convertView;
        }
    }
}
