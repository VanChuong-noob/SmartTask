package com.androidapp.SmartTask;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatsActivity extends AppCompatActivity {

    private PieChart pieChart;
    private BarChart barChart;
    private LinearLayout llAchievements;
    private DatabaseHelper dbHelper;
    private AchievementManager achievementManager;
    private SharedPreferences prefs;
    private String currentUser;
    private TextView tvTotalAchievements;
    private TextView tvStreakBig;
    private TextView tvCompletionRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        dbHelper = new DatabaseHelper(this);
        achievementManager = new AchievementManager(this);
        prefs = getSharedPreferences("SmartTask", MODE_PRIVATE);

        if (!prefs.getBoolean("isLoggedIn", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        currentUser = prefs.getString("currentUser", "");

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        llAchievements = findViewById(R.id.llAchievements);
        tvTotalAchievements = findViewById(R.id.tvTotalAchievements);
        tvStreakBig = findViewById(R.id.tvStreakBig);
        tvCompletionRate = findViewById(R.id.tvCompletionRate);
        TextView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        loadStats();
        setupPieChart();
        setupBarChart();
        loadAchievements();
    }

    private void loadStats() {
        int completed = dbHelper.getCompletedCount(currentUser);
        int pending = dbHelper.getPendingCount(currentUser);
        int total = completed + pending;
        int streak = prefs.getInt("streak", 0);

        tvStreakBig.setText(String.valueOf(streak));

        if (total > 0) {
            int rate = (completed * 100) / total;
            tvCompletionRate.setText(rate + "%");
        } else {
            tvCompletionRate.setText("0%");
        }

        tvTotalAchievements.setText(achievementManager.getUnlockedCount(currentUser) + "/10");
    }

    private void setupPieChart() {
        int completed = dbHelper.getCompletedCount(currentUser);
        int pending = dbHelper.getPendingCount(currentUser);

        ArrayList<PieEntry> entries = new ArrayList<>();
        if (completed > 0) {
            entries.add(new PieEntry(completed, "Hoan thanh"));
        }
        if (pending > 0) {
            entries.add(new PieEntry(pending, "Dang cho"));
        }

        if (entries.isEmpty()) {
            pieChart.setNoDataText("Chua co du lieu");
            pieChart.invalidate();
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{Color.parseColor("#4CAF50"), Color.parseColor("#FF9800")});
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(50f);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(55f);
        pieChart.setCenterText("Task\nStats");
        pieChart.setCenterTextSize(16f);
        pieChart.setCenterTextColor(Color.parseColor("#333333"));
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setTextSize(13f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        String[] days = new String[7];
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        for (int i = 6; i >= 0; i--) {
            cal.add(Calendar.DATE, -i);
            days[6 - i] = sdf.format(cal.getTime());
            int count = (int) (Math.random() * 10);
            entries.add(new BarEntry(6 - i, count));
            cal = Calendar.getInstance();
        }

        BarDataSet dataSet = new BarDataSet(entries, "Task hoan thanh");
        dataSet.setColor(Color.parseColor("#6C63FF"));
        dataSet.setValueTextColor(Color.parseColor("#333333"));
        dataSet.setValueTextSize(10f);

        BarData data = new BarData(dataSet);
        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setTextSize(12f);

        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void loadAchievements() {
        llAchievements.removeAllViews();
        List<Achievement> achievements = achievementManager.getAllAchievements(currentUser);

        for (Achievement a : achievements) {
            CardView card = new CardView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 16);
            card.setLayoutParams(params);
            card.setCardBackgroundColor(a.isUnlocked() ? Color.WHITE : Color.parseColor("#F0F0F0"));
            card.setRadius(16);
            card.setCardElevation(4);

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(16, 16, 16, 16);
            layout.setGravity(Gravity.CENTER_VERTICAL);

            TextView tvIcon = new TextView(this);
            tvIcon.setText(a.getIcon());
            tvIcon.setTextSize(28);

            LinearLayout textLayout = new LinearLayout(this);
            textLayout.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            textParams.setMargins(16, 0, 16, 0);
            textLayout.setLayoutParams(textParams);

            TextView tvTitle = new TextView(this);
            tvTitle.setText(a.getTitle());
            tvTitle.setTextSize(16);
            tvTitle.setTextColor(a.isUnlocked() ? Color.parseColor("#333333") : Color.GRAY);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

            TextView tvDesc = new TextView(this);
            tvDesc.setText(a.getDescription());
            tvDesc.setTextSize(13);
            tvDesc.setTextColor(a.isUnlocked() ? Color.parseColor("#666666") : Color.LTGRAY);

            textLayout.addView(tvTitle);
            textLayout.addView(tvDesc);

            TextView tvStatus = new TextView(this);
            tvStatus.setText(a.isUnlocked() ? "✅" : "🔒");
            tvStatus.setTextSize(20);

            layout.addView(tvIcon);
            layout.addView(textLayout);
            layout.addView(tvStatus);
            card.addView(layout);
            llAchievements.addView(card);
        }
    }
}