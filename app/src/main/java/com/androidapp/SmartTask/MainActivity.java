package com.androidapp.SmartTask;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private final List<Task> taskList = new ArrayList<>();
    private TextView tvComplete;
    private TextView tvPending;
    private TextView tvStreak;
    private TextView tvSuggestion;
    private DatabaseHelper dbHelper;
    private SmartFeatureManager smartFeature;
    private AlarmScheduler alarmScheduler;
    private SharedPreferences prefs;
    private String currentUser;
    private String selectedTime = "";
    private int selectedHour = 9;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        smartFeature = new SmartFeatureManager(this);
        alarmScheduler = new AlarmScheduler(this);
        prefs = getSharedPreferences("SmartTask", MODE_PRIVATE);

        if (!prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUser = prefs.getString("currentUser", "");

        recyclerView = findViewById(R.id.recyclerView);
        tvComplete = findViewById(R.id.tvComplete);
        tvPending = findViewById(R.id.tvPending);
        tvStreak = findViewById(R.id.tvStreak);
        tvSuggestion = findViewById(R.id.tvSuggestion);
        TextView tvAddNew = findViewById(R.id.tvAddNew);
        TextView tvLogout = findViewById(R.id.tvLogout);
        TextView tvDeleteCompleted = findViewById(R.id.tvDeleteCompleted);

        alarmScheduler.scheduleMorningReminder(8, 0);
        smartFeature.checkFirstOpenToday(currentUser);
        loadTasks();

        adapter = new TaskAdapter(taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        updateSuggestion();

        tvStreak.setText(String.valueOf(smartFeature.getStreak()));

        tvLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        tvAddNew.setOnClickListener(v -> showAddTaskDialog());
        tvDeleteCompleted.setOnClickListener(v -> deleteCompletedTasks());
    }

    @Override
    protected void onResume() {
        super.onResume();
        smartFeature.checkEveningReminder(currentUser);
        updateSuggestion();
    }

    private void loadTasks() {
        taskList.clear();
        taskList.addAll(dbHelper.getAllTasks(currentUser));
        updateStats();
    }

    private void updateSuggestion() {
        String suggestion = smartFeature.getSmartSuggestion(currentUser);
        tvSuggestion.setText("💡 " + suggestion);
    }

    private void showTimePickerDialog(TextView tvTimeDisplay) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null);

        NumberPicker npHour = view.findViewById(R.id.npHour);
        NumberPicker npMinute = view.findViewById(R.id.npMinute);

        npHour.setMinValue(0);
        npHour.setMaxValue(23);
        npHour.setValue(selectedHour);
        npHour.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value) + " giờ");

        npMinute.setMinValue(0);
        npMinute.setMaxValue(59);
        npMinute.setValue(selectedMinute);
        npMinute.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value) + " phut");

        builder.setView(view);
        builder.setPositiveButton("✅ OK", (dialog, which) -> {
            selectedHour = npHour.getValue();
            selectedMinute = npMinute.getValue();
            String amPm = selectedHour < 12 ? "AM" : "PM";
            int displayHour = selectedHour > 12 ? selectedHour - 12 : selectedHour;
            if (displayHour == 0) displayHour = 12;
            selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, selectedMinute, amPm);
            tvTimeDisplay.setText("⏰ " + selectedTime);
        });
        builder.setNegativeButton("❌ Huy", null);
        builder.show();
    }

    private void showAddTaskDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);

        final EditText etTitle = view.findViewById(R.id.etTitle);
        final EditText etDescription = view.findViewById(R.id.etDescription);
        final TextView tvTimePicker = view.findViewById(R.id.tvTimePicker);
        final CheckBox cbSetReminder = view.findViewById(R.id.cbSetReminder);

        selectedTime = "Chua dat gio";
        tvTimePicker.setText("👆 Cham de chon gio");

        tvTimePicker.setOnClickListener(v -> showTimePickerDialog(tvTimePicker));

        builder.setView(view);
        builder.setPositiveButton("✅ Them", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (!title.isEmpty()) {
                String todayDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                long taskId = dbHelper.addTask(title, description, selectedTime, todayDate, currentUser);
                if (taskId != -1) {
                    if (cbSetReminder.isChecked() && !selectedTime.equals("Chua dat gio")) {
                        alarmScheduler.scheduleTaskReminder((int) taskId, title, selectedHour, selectedMinute);
                        Toast.makeText(this, "🔔 Da dat nhac luc " + selectedTime, Toast.LENGTH_SHORT).show();
                    }
                    loadTasks();
                    adapter.notifyItemInserted(taskList.size() - 1);
                    updateSuggestion();
                    Toast.makeText(MainActivity.this, "✅ Da them: " + title, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("❌ Huy", null);
        builder.show();
    }

    private void showEditTaskDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);

        final EditText etTitle = view.findViewById(R.id.etTitle);
        final EditText etDescription = view.findViewById(R.id.etDescription);
        final TextView tvTimePicker = view.findViewById(R.id.tvTimePicker);
        final CheckBox cbSetReminder = view.findViewById(R.id.cbSetReminder);

        etTitle.setText(task.getTitle());
        etDescription.setText(task.getDescription());
        selectedTime = task.getTime();
        tvTimePicker.setText("⏰ " + selectedTime);

        try {
            String[] parts = selectedTime.split(" ");
            if (parts.length > 0) {
                String[] timeParts = parts[0].split(":");
                selectedHour = Integer.parseInt(timeParts[0]);
                selectedMinute = Integer.parseInt(timeParts[1]);
                if (parts.length > 1 && parts[1].equals("PM") && selectedHour < 12) selectedHour += 12;
            }
        } catch (Exception e) {
            selectedHour = 9;
            selectedMinute = 0;
        }

        tvTimePicker.setOnClickListener(v -> showTimePickerDialog(tvTimePicker));

        builder.setView(view);
        builder.setPositiveButton("✅ Cap nhat", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (!title.isEmpty()) {
                int rows = dbHelper.updateTask(task.getId(), title, description, selectedTime, task.getDate());
                if (rows > 0) {
                    if (cbSetReminder.isChecked() && !selectedTime.equals("Chua dat gio")) {
                        alarmScheduler.cancelTaskReminder(task.getId());
                        alarmScheduler.scheduleTaskReminder(task.getId(), title, selectedHour, selectedMinute);
                    }
                    loadTasks();
                    adapter.notifyDataSetChanged();
                    updateSuggestion();
                    Toast.makeText(MainActivity.this, "✅ Da cap nhat: " + title, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("❌ Huy", null);
        builder.show();
    }

    private void toggleTaskComplete(Task task, int position) {
        boolean newStatus = !task.isCompleted();
        dbHelper.toggleTaskComplete(task.getId(), newStatus);
        task.setCompleted(newStatus);
        adapter.notifyItemChanged(position);
        updateStats();
        updateSuggestion();

        if (newStatus) {
            alarmScheduler.cancelTaskReminder(task.getId());
            Toast.makeText(this, "🎉 Hoan thanh: " + task.getTitle(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteTask(Task task, int position) {
        new AlertDialog.Builder(this)
                .setTitle("🗑️ Xoa cong viec")
                .setMessage("Ban co chac muon xoa:\n" + task.getTitle() + "?")
                .setPositiveButton("✅ Xoa", (dialog, which) -> {
                    alarmScheduler.cancelTaskReminder(task.getId());
                    dbHelper.deleteTask(task.getId());
                    taskList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateStats();
                    updateSuggestion();
                    Toast.makeText(MainActivity.this, "🗑️ Da xoa!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("❌ Huy", null)
                .show();
    }

    private void deleteCompletedTasks() {
        int completedCount = dbHelper.getCompletedCount(currentUser);
        if (completedCount == 0) {
            Toast.makeText(this, "Khong co task nao da hoan thanh!", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("🗑️ Xoa task hoan thanh")
                .setMessage("Xoa tat ca " + completedCount + " task da hoan thanh?")
                .setPositiveButton("✅ Xoa", (dialog, which) -> {
                    dbHelper.deleteCompletedTasks(currentUser);
                    loadTasks();
                    adapter.notifyDataSetChanged();
                    updateSuggestion();
                    Toast.makeText(MainActivity.this, "✅ Da xoa task hoan thanh!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("❌ Huy", null)
                .show();
    }

    private void updateStats() {
        int completed = dbHelper.getCompletedCount(currentUser);
        int pending = dbHelper.getPendingCount(currentUser);
        tvComplete.setText(String.valueOf(completed));
        tvPending.setText(String.valueOf(pending));
        tvStreak.setText(String.valueOf(smartFeature.getStreak()));
    }

    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

        private final List<Task> tasks;

        public TaskAdapter(List<Task> tasks) {
            this.tasks = tasks;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Task task = tasks.get(position);
            holder.tvTitle.setText(task.getTitle());
            holder.tvTime.setText(task.getTime());
            holder.tvDate.setText(task.getDate());

            holder.cbComplete.setOnCheckedChangeListener(null);
            holder.cbComplete.setChecked(task.isCompleted());

            if (task.isCompleted()) {
                holder.tvTitle.setPaintFlags(
                        holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTitle.setAlpha(0.4f);
            } else {
                holder.tvTitle.setPaintFlags(
                        holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvTitle.setAlpha(1.0f);
            }

            final int pos = holder.getBindingAdapterPosition();

            holder.cbComplete.setOnCheckedChangeListener((buttonView, isChecked) ->
                    toggleTaskComplete(task, pos));

            holder.itemView.setOnClickListener(v -> showEditTaskDialog(task));

            holder.tvEdit.setOnClickListener(v -> showEditTaskDialog(task));

            holder.tvDelete.setOnClickListener(v -> deleteTask(task, pos));
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbComplete;
            TextView tvTitle;
            TextView tvTime;
            TextView tvDate;
            TextView tvEdit;
            TextView tvDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cbComplete = itemView.findViewById(R.id.cbComplete);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvEdit = itemView.findViewById(R.id.tvEdit);
                tvDelete = itemView.findViewById(R.id.tvDelete);
            }
        }
    }
}