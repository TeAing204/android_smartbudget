package com.example.smartbudget.activity;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbudget.R;
import com.example.smartbudget.database.DBHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ReportActivity extends AppCompatActivity {
    ImageButton imgbtnBack;
    LineChart lineChart;
    PieChart pieChartIncome, pieChartExpense;

    private DBHelper dbHelper;
    private final SimpleDateFormat sdfInput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final SimpleDateFormat sdfMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        imgbtnBack = findViewById(R.id.imgbtnBack);
        lineChart = findViewById(R.id.lineChart);
        pieChartIncome = findViewById(R.id.pieChartIncome);
        pieChartExpense = findViewById(R.id.pieChartExpense);

        dbHelper = new DBHelper(this);

        // Lấy userId từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
        if (userId == -1) {
            finish(); // hoặc chuyển về màn hình đăng nhập
            return;
        }

        showLineChart();
        showPieChartByCategory(1, pieChartExpense); // Chi tiêu
        showPieChartByCategory(2, pieChartIncome);  // Thu nhập

        imgbtnBack.setOnClickListener(v -> finish());
    }

    private void showLineChart() {
        Map<String, Float> incomeByDate = new HashMap<>();
        Map<String, Float> expenseByDate = new HashMap<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT t.date, t.amount, c.id " +
                        "FROM transactions t " +
                        "JOIN wallet w ON t.walletId = w.id " +
                        "JOIN subcategory sc ON t.subcategoryId = sc.id " +
                        "JOIN category c ON sc.categoryId = c.id " +
                        "WHERE w.userId = ?",
                new String[]{String.valueOf(userId)}
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String dateStr = cursor.getString(0);
                float amount = cursor.getFloat(1);
                int categoryId = cursor.getInt(2);

                try {
                    Date date = sdfInput.parse(dateStr);
                    String dayKey = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(date);

                    if (categoryId == 2) {
                        incomeByDate.put(dayKey, incomeByDate.getOrDefault(dayKey, 0f) + amount);
                    } else if (categoryId == 1) {
                        expenseByDate.put(dayKey, expenseByDate.getOrDefault(dayKey, 0f) + amount);
                    }

                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            cursor.close();
        }

        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        List<String> xLabels = new ArrayList<>();
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();

        for (int i = 0; i < today; i++) {
            String label = new SimpleDateFormat("dd/MM", Locale.getDefault()).format(calendar.getTime());
            xLabels.add(label);

            float income = incomeByDate.getOrDefault(label, 0f);
            float expense = expenseByDate.getOrDefault(label, 0f);

            incomeEntries.add(new Entry(i, income));
            expenseEntries.add(new Entry(i, expense));

            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Khoản thu");
        incomeSet.setColor(getColor(R.color.blue));
        incomeSet.setCircleColor(getColor(R.color.blue));

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Khoản chi");
        expenseSet.setColor(getColor(R.color.red));
        expenseSet.setCircleColor(getColor(R.color.red));

        LineData lineData = new LineData(incomeSet, expenseSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(xLabels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setLabelRotationAngle(-45f);
        xAxis.setLabelCount(xLabels.size(), true);

        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate();
    }

    private void showPieChartByCategory(int categoryId, PieChart pieChart) {
        List<PieEntry> entries = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Date now = calendar.getTime();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date startOfMonth = calendar.getTime();

        Cursor cursor = db.rawQuery(
                "SELECT sc.name, SUM(t.amount) as totalAmount " +
                        "FROM transactions t " +
                        "JOIN wallet w ON t.walletId = w.id " +
                        "JOIN subcategory sc ON t.subcategoryId = sc.id " +
                        "WHERE t.date BETWEEN ? AND ? AND sc.categoryId = ? AND w.userId = ? " +
                        "GROUP BY sc.name",
                new String[]{
                        sdf.format(startOfMonth),
                        sdf.format(now),
                        String.valueOf(categoryId),
                        String.valueOf(userId)
                }
        );

        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            float total = cursor.getFloat(1);
            entries.add(new PieEntry(total, name));
        }

        cursor.close();

        PieDataSet dataSet = new PieDataSet(entries,
                categoryId == 1 ? "Khoản chi" : "Khoản thu");

        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        PieData pieData = new PieData(dataSet);

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(true);
        pieChart.animateY(1000);
    }
}