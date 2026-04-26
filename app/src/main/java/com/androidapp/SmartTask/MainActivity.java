package com.androidapp.SmartTask;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private final List<Task> taskList = new ArrayList<>();
    private TextView tvComplete;
    private TextView tvPending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        tvComplete = findViewById(R.id.tvComplete);
        tvPending = findViewById(R.id.tvPending);
        TextView tvAddNew = findViewById(R.id.tvAddNew);

        taskList.add(new Task("Hoc Android Studio", "09:00 AM", false));
        taskList.add(new Task("Lam bai tap", "02:00 PM", false));
        taskList.add(new Task("Doc sach", "08:00 PM", true));

        adapter = new TaskAdapter(taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        tvAddNew.setOnClickListener(v -> showAddDialog());

        updateStats();
        Toast.makeText(this, "Chao ngay moi! Hay hoan thanh cong viec nhe", Toast.LENGTH_LONG).show();
    }

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);

        final EditText etTitle = view.findViewById(R.id.etTitle);
        final EditText etTime = view.findViewById(R.id.etTime);

        builder.setView(view);
        builder.setTitle("Them cong viec moi");
        builder.setPositiveButton("Them", (dialog, which) -> {
            String title = etTitle.getText().toString().trim();
            String time = etTime.getText().toString().trim();

            if (!title.isEmpty()) {
                if (time.isEmpty()) {
                    time = "Chua dat gio";
                }
                taskList.add(new Task(title, time, false));
                adapter.notifyItemInserted(taskList.size() - 1);
                recyclerView.scrollToPosition(taskList.size() - 1);
                updateStats();
                Toast.makeText(MainActivity.this, "Da them: " + title, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Huy", null);
        builder.show();
    }

    private void updateStats() {
        int complete = 0;
        for (Task t : taskList) {
            if (t.isCompleted()) {
                complete++;
            }
        }
        tvComplete.setText(String.valueOf(complete));
        tvPending.setText(String.valueOf(taskList.size() - complete));
    }

    private class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

        private final List<Task> tasks;

        public TaskAdapter(List<Task> tasks) {
            this.tasks = tasks;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Task task = tasks.get(position);
            holder.tvTitle.setText(task.getTitle());
            holder.tvTime.setText(task.getTime());

            holder.cbComplete.setOnCheckedChangeListener(null);
            holder.cbComplete.setChecked(task.isCompleted());

            if (task.isCompleted()) {
                holder.tvTitle.setPaintFlags(
                        holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.tvTitle.setPaintFlags(
                        holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            }

            holder.cbComplete.setOnCheckedChangeListener((buttonView, isChecked) -> {
                task.setCompleted(isChecked);
                notifyItemChanged(position);
                updateStats();
            });

            holder.tvDelete.setOnClickListener(v -> {
                tasks.remove(position);
                notifyItemRemoved(position);
                updateStats();
                Toast.makeText(MainActivity.this, "Da xoa", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbComplete;
            TextView tvTitle;
            TextView tvTime;
            TextView tvDelete;

            public ViewHolder(View itemView) {
                super(itemView);
                cbComplete = itemView.findViewById(R.id.cbComplete);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvDelete = itemView.findViewById(R.id.tvDelete);
            }
        }
    }
}//test