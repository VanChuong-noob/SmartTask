package com.androidapp.SmartTask;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private TextView tvComplete, tvPending, tvStreak, tvSuggestion, tvEmpty;
    private DatabaseHelper dbHelper;
    private SmartFeatureManager smartFeature;
    private AlarmScheduler alarmScheduler;
    private FirebaseSyncManager firebaseSync;
    private SharedPreferences prefs;
    private String currentUser;
    private String selectedTime = "";
    private int selectedHour = 9, selectedMinute = 0;
    private String selectedLocationName = "";
    private double selectedLocationLat = 0;
    private double selectedLocationLng = 0;
    private boolean locationReminderEnabled = false;

    private final ActivityResultLauncher<Intent> locationPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedLocationName = data.getStringExtra("location_name");
                    selectedLocationLat = data.getDoubleExtra("location_lat", 0);
                    selectedLocationLng = data.getDoubleExtra("location_lng", 0);
                    locationReminderEnabled = !selectedLocationName.isEmpty();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        smartFeature = new SmartFeatureManager(this);
        alarmScheduler = new AlarmScheduler(this);
        firebaseSync = new FirebaseSyncManager();
        firebaseSync.setDbHelper(dbHelper);
        prefs = getSharedPreferences("SmartTask", MODE_PRIVATE);

        if (!prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUser = prefs.getString("currentUser", "");
        requestPermissions();
        initViews();
        setupRecyclerView();
        smartFeature.checkFirstOpenToday(currentUser);
        loadTasks();

        // Sync Firebase
        firebaseSync.syncTasks(currentUser, () -> {
            loadTasks();
            Toast.makeText(this, "Da dong bo!", Toast.LENGTH_SHORT).show();
        });
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }
        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1001);
        } else {
            startLocationService();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            startLocationService();
        }
    }

    private void startLocationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Intent serviceIntent = new Intent(this, LocationService.class);
                serviceIntent.putExtra("user_email", currentUser);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            }
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        tvComplete = findViewById(R.id.tvComplete);
        tvPending = findViewById(R.id.tvPending);
        tvStreak = findViewById(R.id.tvStreak);
        tvSuggestion = findViewById(R.id.tvSuggestion);
        tvEmpty = findViewById(R.id.tvEmpty);
        TextView tvAddNew = findViewById(R.id.tvAddNew);
        TextView tvLogout = findViewById(R.id.tvLogout);
        TextView tvDeleteCompleted = findViewById(R.id.tvDeleteCompleted);
        TextView tvStats = findViewById(R.id.tvStats);

        tvLogout.setOnClickListener(v -> {
            stopService(new Intent(this, LocationService.class));
            prefs.edit().clear().apply();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        tvAddNew.setOnClickListener(v -> showAddDialog());
        tvDeleteCompleted.setOnClickListener(v -> deleteCompletedTasks());
        tvStats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
    }

    private void setupRecyclerView() {
        adapter = new TaskAdapter(taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void loadTasks() {
        taskList.clear();
        taskList.addAll(dbHelper.getAllTasks(currentUser));
        adapter.notifyDataSetChanged();
        updateUI();
    }

    private void updateUI() {
        tvComplete.setText(String.valueOf(dbHelper.getCompletedCount(currentUser)));
        tvPending.setText(String.valueOf(dbHelper.getPendingCount(currentUser)));
        tvStreak.setText(String.valueOf(smartFeature.getStreak()));
        tvSuggestion.setText(smartFeature.getSmartSuggestion(currentUser));
        boolean empty = taskList.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showTimePicker(TextView tvDisplay) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_time_picker, null);
        NumberPicker npHour = view.findViewById(R.id.npHour);
        NumberPicker npMinute = view.findViewById(R.id.npMinute);
        npHour.setMinValue(0); npHour.setMaxValue(23); npHour.setValue(selectedHour);
        npMinute.setMinValue(0); npMinute.setMaxValue(59); npMinute.setValue(selectedMinute);

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("OK", (d, w) -> {
                    selectedHour = npHour.getValue();
                    selectedMinute = npMinute.getValue();
                    int dh = selectedHour % 12;
                    if (dh == 0) dh = 12;
                    String amPm = selectedHour < 12 ? "AM" : "PM";
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d %s", dh, selectedMinute, amPm);
                    tvDisplay.setText(selectedTime);
                })
                .setNegativeButton("Huy", null).show();
    }

    private void openLocationPicker() {
        locationPickerLauncher.launch(new Intent(this, LocationPickerActivity.class));
    }

    private void showAddDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etDesc = view.findViewById(R.id.etDescription);
        TextView tvTime = view.findViewById(R.id.tvTimePicker);
        TextView tvLocation = view.findViewById(R.id.tvLocationPicker);
        CheckBox cbReminder = view.findViewById(R.id.cbSetReminder);
        CheckBox cbLocation = view.findViewById(R.id.cbLocationReminder);

        selectedTime = "Chua dat gio";
        selectedLocationName = "";
        selectedLocationLat = 0;
        selectedLocationLng = 0;
        locationReminderEnabled = false;

        tvTime.setText("Cham de chon gio");
        tvLocation.setText("Cham de chon vi tri");
        tvTime.setOnClickListener(v -> showTimePicker(tvTime));
        tvLocation.setOnClickListener(v -> openLocationPicker());
        cbLocation.setOnCheckedChangeListener((btn, checked) -> {
            locationReminderEnabled = checked;
            if (checked && selectedLocationName.isEmpty()) openLocationPicker();
        });

        new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Them cong viec")
                .setPositiveButton("Them", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(this, "Nhap ten", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
                    Task task = new Task(title, etDesc.getText().toString().trim(), selectedTime, date, currentUser);
                    task.setLocationName(selectedLocationName);
                    task.setLocationLat(selectedLocationLat);
                    task.setLocationLng(selectedLocationLng);
                    task.setLocationReminder(locationReminderEnabled && !selectedLocationName.isEmpty());
                    long id = dbHelper.addTask(task);
                    if (id != -1) {
                        task.setId((int) id);
                        firebaseSync.uploadTask(task);
                        if (cbReminder.isChecked() && !selectedTime.equals("Chua dat gio")) {
                            alarmScheduler.scheduleTaskReminder((int) id, title, selectedHour, selectedMinute);
                        }
                        loadTasks();
                    }
                })
                .setNegativeButton("Huy", null).show();
    }

    private void showEditDialog(Task task) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etDesc = view.findViewById(R.id.etDescription);
        TextView tvTime = view.findViewById(R.id.tvTimePicker);
        TextView tvLocation = view.findViewById(R.id.tvLocationPicker);
        CheckBox cbReminder = view.findViewById(R.id.cbSetReminder);
        CheckBox cbLocation = view.findViewById(R.id.cbLocationReminder);

        etTitle.setText(task.getTitle());
        etDesc.setText(task.getDescription());
        selectedTime = task.getTime();
        selectedLocationName = task.getLocationName();
        selectedLocationLat = task.getLocationLat();
        selectedLocationLng = task.getLocationLng();
        locationReminderEnabled = task.isLocationReminder();

        tvTime.setText(selectedTime);
        tvLocation.setText(selectedLocationName.isEmpty() ? "Cham de chon vi tri" : selectedLocationName);
        cbLocation.setChecked(locationReminderEnabled);
        try {
            String[] parts = selectedTime.split(" ");
            String[] tp = parts[0].split(":");
            selectedHour = Integer.parseInt(tp[0]);
            selectedMinute = Integer.parseInt(tp[1]);
            if (parts.length > 1 && parts[1].equals("PM") && selectedHour < 12) selectedHour += 12;
            if (parts.length > 1 && parts[1].equals("AM") && selectedHour == 12) selectedHour = 0;
        } catch (Exception e) { selectedHour = 9; selectedMinute = 0; }
        tvTime.setOnClickListener(v -> showTimePicker(tvTime));
        tvLocation.setOnClickListener(v -> openLocationPicker());
        cbLocation.setOnCheckedChangeListener((btn, checked) -> {
            locationReminderEnabled = checked;
            if (checked && selectedLocationName.isEmpty()) openLocationPicker();
        });

        new AlertDialog.Builder(this)
                .setView(view)
                .setTitle("Sua cong viec")
                .setPositiveButton("Cap nhat", (d, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (TextUtils.isEmpty(title)) {
                        Toast.makeText(this, "Nhap ten", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    task.setTitle(title);
                    task.setDescription(etDesc.getText().toString().trim());
                    task.setTime(selectedTime);
                    task.setLocationName(selectedLocationName);
                    task.setLocationLat(selectedLocationLat);
                    task.setLocationLng(selectedLocationLng);
                    task.setLocationReminder(locationReminderEnabled && !selectedLocationName.isEmpty());
                    dbHelper.updateTask(task);
                    firebaseSync.uploadTask(task);
                    alarmScheduler.cancelTaskReminder(task.getId());
                    if (cbReminder.isChecked() && !selectedTime.equals("Chua dat gio")) {
                        alarmScheduler.scheduleTaskReminder(task.getId(), title, selectedHour, selectedMinute);
                    }
                    loadTasks();
                })
                .setNegativeButton("Huy", null).show();
    }

    private void toggleComplete(Task task, int pos) {
        task.setCompleted(!task.isCompleted());
        dbHelper.toggleTaskComplete(task.getId(), task.isCompleted());
        adapter.notifyItemChanged(pos);
        firebaseSync.uploadTask(task);
        if (task.isCompleted()) alarmScheduler.cancelTaskReminder(task.getId());
        updateUI();
        checkAchievement();
    }

    private void deleteTask(Task task, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Xoa").setMessage("Xoa: " + task.getTitle() + "?")
                .setPositiveButton("Xoa", (d, w) -> {
                    alarmScheduler.cancelTaskReminder(task.getId());
                    dbHelper.deleteTask(task.getId());
                    firebaseSync.deleteTask(task.getId());
                    taskList.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    updateUI();
                })
                .setNegativeButton("Huy", null).show();
    }

    private void deleteCompletedTasks() {
        int count = dbHelper.getCompletedCount(currentUser);
        if (count == 0) {
            Toast.makeText(this, "Khong co task hoan thanh", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xoa task hoan thanh").setMessage("Xoa " + count + " task?")
                .setPositiveButton("Xoa", (d, w) -> { dbHelper.deleteCompletedTasks(currentUser); loadTasks(); })
                .setNegativeButton("Huy", null).show();
    }

    private void checkAchievement() {
        AchievementManager am = new AchievementManager(MainActivity.this);
        Achievement newAch = am.checkNewAchievement(currentUser);
        if (newAch != null) {
            new AlertDialog.Builder(this)
                    .setTitle("Mo khoa thanh tuu!")
                    .setMessage(newAch.getIcon() + " " + newAch.getTitle() + "\n" + newAch.getDescription())
                    .setPositiveButton("TUYET VOI!", null).show();
        }
    }

    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {
        private final List<Task> list;
        TaskAdapter(List<Task> list) { this.list = list; }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false));
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Task t = list.get(pos);
            h.tvTitle.setText(t.getTitle());
            h.tvTime.setText(t.getTime());
            h.tvDate.setText(t.getDate());
            if (!t.getLocationName().isEmpty()) {
                h.tvLocation.setVisibility(View.VISIBLE);
                h.tvLocation.setText(t.getLocationName());
            } else {
                h.tvLocation.setVisibility(View.GONE);
            }
            h.cb.setOnCheckedChangeListener(null);
            h.cb.setChecked(t.isCompleted());
            if (t.isCompleted()) {
                h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                h.tvTitle.setAlpha(0.5f);
            } else {
                h.tvTitle.setPaintFlags(h.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                h.tvTitle.setAlpha(1f);
            }
            int p = h.getBindingAdapterPosition();
            h.cb.setOnCheckedChangeListener((btn, checked) -> toggleComplete(t, p));
            h.tvEdit.setOnClickListener(v -> showEditDialog(t));
            h.tvDelete.setOnClickListener(v -> deleteTask(t, p));
        }
        @Override public int getItemCount() { return list.size(); }
        class VH extends RecyclerView.ViewHolder {
            CheckBox cb;
            TextView tvTitle, tvTime, tvDate, tvLocation, tvEdit, tvDelete;
            VH(View v) {
                super(v);
                cb = v.findViewById(R.id.cbComplete);
                tvTitle = v.findViewById(R.id.tvTitle);
                tvTime = v.findViewById(R.id.tvTime);
                tvDate = v.findViewById(R.id.tvDate);
                tvLocation = v.findViewById(R.id.tvLocation);
                tvEdit = v.findViewById(R.id.tvEdit);
                tvDelete = v.findViewById(R.id.tvDelete);
            }
        }
    }
}
//test comment