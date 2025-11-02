package com.example.smartbudget.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.PopupMenu;

import com.example.smartbudget.R;
import com.example.smartbudget.adapter.TransactionListAdapter;
import com.example.smartbudget.database.DBHelper;
import com.example.smartbudget.model.Transaction;
import com.example.smartbudget.tmp.TransactionDisplay;

import java.util.*;

public class TransactionActivity extends AppCompatActivity {

    private TextView tvTotalBalanceTransaction;
    private ListView lvTransaction;
    private ImageButton btnFilter, imgbtnAddTransaction;
    private RadioGroup rgType;
    private RadioButton rdIncome, rdExpense, rdAll;
    private TextView tvSpendingTransaction, tvIncomeTransaction;


    private final List<TransactionDisplay> transactionList = new ArrayList<>();
    private DBHelper dbHelper;
    private int userId;

    // Biến lọc
    private String filterTime = "this_month"; // all, this_month, last_month, custom
    private String filterType = "all"; // all, income, expense
    private String selectedStartDate = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        initViews();
        dbHelper = new DBHelper(this);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
        if (userId == -1) userId = 1; // gán cứng userId mẫu

        setupFilterMenu();
        setupRadioGroup();

        loadTotalBalance();
        loadFilteredTransactions();
        imgbtnAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(TransactionActivity.this, AddTransactionActivity.class);
            startActivity(intent);
        });
    }

    private void initViews() {
        tvTotalBalanceTransaction = findViewById(R.id.tvTotalBalanceTransaction);
        btnFilter = findViewById(R.id.btnFilter);
        lvTransaction = findViewById(R.id.lvTransaction);
        rgType = findViewById(R.id.rgType);
        rdIncome = findViewById(R.id.rdIncome);
        rdExpense = findViewById(R.id.rdExpense);
        rdAll = findViewById(R.id.rdAll);
        tvSpendingTransaction = findViewById(R.id.tv_spending_transaction);
        tvIncomeTransaction = findViewById(R.id.tv_income_transaction);
        imgbtnAddTransaction = findViewById(R.id.imgbtnAddTransactionTransaction);

    }

    private void setupFilterMenu() {
        btnFilter.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(TransactionActivity.this, v);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_filter_transaction, popup.getMenu());
            popup.setOnMenuItemClickListener(this::onFilterMenuItemClick);
            popup.show();
        });
    }

    private boolean onFilterMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_all) {
            filterTime = "all";
        } else if (id == R.id.menu_this_month) {
            filterTime = "this_month";
        } else if (id == R.id.menu_last_month) {
            filterTime = "last_month";
        } else if (id == R.id.menu_custom) {
            filterTime = "custom";
            showDatePicker();
            return true;
        }

        loadFilteredTransactions();
        return true;
    }

    private void setupRadioGroup() {
        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rdIncome) {
                filterType = "income";
            } else if (checkedId == R.id.rdExpense) {
                filterType = "expense";
            } else {
                filterType = "all";
            }
            loadFilteredTransactions();
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedStartDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
            loadFilteredTransactions();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadTotalBalance() {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT SUM(balance) FROM wallet WHERE userId = ?", new String[]{String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                double totalBalance = cursor.getDouble(0);
                tvTotalBalanceTransaction.setText(String.format("%,.0f đ", totalBalance));
            }
        }
    }

    private void loadFilteredTransactions() {
        StringBuilder query = new StringBuilder(
                "SELECT t.*, s.name AS subcategoryName, s.categoryId " +
                        "FROM transactions t " +
                        "JOIN subcategory s ON t.subcategoryId = s.id " +
                        "JOIN wallet w ON t.walletId = w.id " +
                        "WHERE w.userId = ?"
        );

        List<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));

        // --- Lọc theo thời gian ---
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        switch (filterTime) {
            case "this_month":
                query.append(" AND substr(t.date, 4, 2) = ? AND substr(t.date, 7, 4) = ?");
                args.add(String.format(Locale.getDefault(), "%02d", month));
                args.add(String.valueOf(year));
                break;
            case "last_month":
                month--;
                if (month == 0) {
                    month = 12;
                    year--;
                }
                query.append(" AND substr(t.date, 4, 2) = ? AND substr(t.date, 7, 4) = ?");
                args.add(String.format(Locale.getDefault(), "%02d", month));
                args.add(String.valueOf(year));
                break;
            case "custom":
                if (selectedStartDate != null) {
                    String formatted = selectedStartDate.replace("-", "");
                    query.append(" AND (substr(t.date,7,4) || substr(t.date,4,2) || substr(t.date,1,2)) >= ?");
                    args.add(formatted);
                }
                break;
        }

        // --- Lọc theo loại giao dịch ---
        if (filterType.equals("income")) {
            query.append(" AND s.categoryId = 2");
        } else if (filterType.equals("expense")) {
            query.append(" AND s.categoryId = 1");
        }

        query.append(" ORDER BY substr(t.date, 7, 4) || '-' || substr(t.date, 4, 2) || '-' || substr(t.date, 1, 2) DESC");

        // --- Truy vấn DB ---
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(query.toString(), args.toArray(new String[0]))) {

            transactionList.clear();
            while (cursor.moveToNext()) {
                Transaction t = new Transaction(
                        cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                        cursor.getString(cursor.getColumnIndexOrThrow("note")),
                        cursor.getString(cursor.getColumnIndexOrThrow("date")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("walletId")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("subcategoryId"))
                );
                transactionList.add(new TransactionDisplay(
                        t,
                        cursor.getString(cursor.getColumnIndexOrThrow("subcategoryName")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("categoryId"))
                ));
            }
        }

        TransactionListAdapter adapter = new TransactionListAdapter(this, transactionList);
        lvTransaction.setAdapter(adapter);

        lvTransaction.setOnItemClickListener((parent, view, position, id) ->
                showTransactionDetail(transactionList.get(position))
        );

        lvTransaction.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(transactionList.get(position).getTransaction().getId());
            return true;
        });
        double totalIncome = 0;
        double totalExpense = 0;

        for (TransactionDisplay item : transactionList) {
            double amount = item.getTransaction().getAmount();
            if (item.getCategoryId() == 1) { // Chi tiêu
                totalExpense += amount;
            } else if (item.getCategoryId() == 2) { // Thu nhập
                totalIncome += amount;
            }
        }

// Cập nhật giao diện
        tvSpendingTransaction.setText(String.format("Chi tiêu: -%,.0f đ", totalExpense));
        tvIncomeTransaction.setText(String.format("Thu nhập: +%,.0f đ", totalIncome));
        tvSpendingTransaction.setTextColor(getColor(R.color.red));
        tvIncomeTransaction.setTextColor(getColor(R.color.blue));

    }

    private void showTransactionDetail(TransactionDisplay item) {
        Transaction t = item.getTransaction();
        String msg = "Tên giao dịch: " + t.getName() +
                "\n\nSố tiền: " + String.format("%,.0f đ", t.getAmount()) +
                "\n\nNgày: " + t.getDate() +
                "\n\nGhi chú: " + t.getNote() +
                "\n\nDanh mục: " + item.getSubcategoryName() +
                "\n\nLoại: " + (item.getCategoryId() == 1 ? "Chi tiêu" : "Thu nhập");

        new AlertDialog.Builder(this)
                .setTitle("Chi tiết giao dịch")
                .setMessage(msg)
                .setPositiveButton("Đóng", null)
                .show();
    }

    private void confirmDelete(int transactionId) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa giao dịch này?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteTransaction(transactionId))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteTransaction(int transactionId) {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            try (Cursor cursor = db.rawQuery(
                    "SELECT t.amount, t.walletId, s.categoryId FROM transactions t " +
                            "JOIN subcategory s ON t.subcategoryId = s.id WHERE t.id = ?",
                    new String[]{String.valueOf(transactionId)})) {

                if (cursor.moveToFirst()) {
                    double amount = cursor.getDouble(0);
                    int walletId = cursor.getInt(1);
                    int categoryId = cursor.getInt(2);

                    String sql = categoryId == 1
                            ? "UPDATE wallet SET balance = balance + ? WHERE id = ?"
                            : "UPDATE wallet SET balance = balance - ? WHERE id = ?";
                    db.execSQL(sql, new Object[]{amount, walletId});
                }
            }

            int rows = db.delete("transactions", "id = ?", new String[]{String.valueOf(transactionId)});
            if (rows > 0) {
                loadTotalBalance();
                loadFilteredTransactions();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadTotalBalance();
        loadFilteredTransactions();
    }
}
