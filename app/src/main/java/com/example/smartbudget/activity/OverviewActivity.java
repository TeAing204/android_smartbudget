package com.example.smartbudget.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbudget.R;
import com.example.smartbudget.adapter.TransactionListAdapter;
import com.example.smartbudget.database.DBHelper;
import com.example.smartbudget.model.Transaction;
import com.example.smartbudget.tmp.TransactionDisplay;

import java.util.ArrayList;
import java.util.Calendar;

public class OverviewActivity extends AppCompatActivity {

    private Button btnSeeAllWallet, btnSeeReport, btnSeeAllTransaction;
    private TextView tvTotalBalance, tvWallet1, tvBalanceWallet1, tvWallet2, tvBalanceWallet2;
    private TextView tvTotalExpenseThisMonth, tvTotalIncomeThisMonth;
    private ImageButton imgbtnEyeOpen;
    private ImageButton imgbtnOverview, imgbtnTransaction, imgbtnAddTransaction, imgbtnBudget, imgbtnAccount;
    private ListView lvRecentTransaction;

    boolean isBalanceVisible = false;
    double totalBalance = 0.0, balance1 = 0.0, balance2 = 0.0;
    String walletName1 = "", walletName2 = "";
    double totalIncome = 0.0, totalExpense = 0.0;

    int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        initViews();

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);

        if (userId == -1) {
            Toast.makeText(this, "Không tìm thấy người dùng đang đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }

        loadWalletData();
        loadIncomeExpenseData();
        loadRecentTransactions();
        setupEventListeners();
    }

    private void initViews() {
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvWallet1 = findViewById(R.id.tvWallet1);
        tvBalanceWallet1 = findViewById(R.id.tvBalanceWallet1);
        tvWallet2 = findViewById(R.id.tvWallet2);
        tvBalanceWallet2 = findViewById(R.id.tvBalanceWallet2);
        tvTotalExpenseThisMonth = findViewById(R.id.tvTotalExpenseThisMonth);
        tvTotalIncomeThisMonth = findViewById(R.id.tvTotalIncomeThisMonth);
        imgbtnEyeOpen = findViewById(R.id.imgbtnEyeOpen);
        lvRecentTransaction = findViewById(R.id.lvRecentTransaction);
        btnSeeAllWallet = findViewById(R.id.btnSeeAllWallet);
        btnSeeReport = findViewById(R.id.btnSeeReport);
        btnSeeAllTransaction = findViewById(R.id.btnSeeAllTransaction);
        imgbtnOverview = findViewById(R.id.imgbtnOverview);
        imgbtnTransaction = findViewById(R.id.imgbtnTransaction);
        imgbtnAddTransaction = findViewById(R.id.imgbtnAddTransaction);
        imgbtnBudget = findViewById(R.id.imgbtnBudget);
        imgbtnAccount = findViewById(R.id.imgbtnAccount);
    }

    private void loadWalletData() {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursorTotal = db.rawQuery("SELECT SUM(balance) FROM wallet WHERE userId = ?", new String[]{String.valueOf(userId)});
        if (cursorTotal.moveToFirst()) totalBalance = cursorTotal.getDouble(0);
        cursorTotal.close();

        Cursor cursorWallets = db.rawQuery("SELECT name, balance FROM wallet WHERE userId = ? LIMIT 2", new String[]{String.valueOf(userId)});
        int index = 0;
        while (cursorWallets.moveToNext()) {
            String name = cursorWallets.getString(0);
            double balance = cursorWallets.getDouble(1);
            if (index == 0) {
                walletName1 = name;
                balance1 = balance;
            } else if (index == 1) {
                walletName2 = name;
                balance2 = balance;
            }
            index++;
        }
        cursorWallets.close();
        db.close();

        updateBalanceViews();
    }

    private void loadIncomeExpenseData() {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Calendar calendar = Calendar.getInstance();
        String monthYear = String.format("/%02d/%d", calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));

        Cursor expenseCursor = db.rawQuery(
                "SELECT SUM(t.amount) FROM transactions t " +
                        "JOIN subcategory s ON t.subcategoryId = s.id " +
                        "JOIN category c ON s.categoryId = c.id " +
                        "JOIN wallet w ON t.walletId = w.id " +
                        "WHERE c.id = 1 AND w.userId = ? AND t.date LIKE ?",
                new String[]{String.valueOf(userId), "%" + monthYear}
        );
        if (expenseCursor.moveToFirst()) totalExpense = expenseCursor.isNull(0) ? 0 : expenseCursor.getDouble(0);
        expenseCursor.close();

        Cursor incomeCursor = db.rawQuery(
                "SELECT SUM(t.amount) FROM transactions t " +
                        "JOIN subcategory s ON t.subcategoryId = s.id " +
                        "JOIN category c ON s.categoryId = c.id " +
                        "JOIN wallet w ON t.walletId = w.id " +
                        "WHERE c.id = 2 AND w.userId = ? AND t.date LIKE ?",
                new String[]{String.valueOf(userId), "%" + monthYear}
        );
        if (incomeCursor.moveToFirst()) totalIncome = incomeCursor.isNull(0) ? 0 : incomeCursor.getDouble(0);
        incomeCursor.close();
        db.close();

        updateIncomeExpenseViews();
    }

    private void loadRecentTransactions() {
        ArrayList<TransactionDisplay> transactionDisplays = new ArrayList<>();
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT t.id, t.name, t.amount, t.note, t.date, t.walletId, t.subcategoryId, " +
                        "s.name, c.id FROM transactions t " +
                        "JOIN subcategory s ON t.subcategoryId = s.id " +
                        "JOIN category c ON s.categoryId = c.id " +
                        "JOIN wallet w ON t.walletId = w.id " +
                        "WHERE w.userId = ? ORDER BY t.date DESC LIMIT 10",
                new String[]{String.valueOf(userId)}
        );

        while (cursor.moveToNext()) {
            Transaction transaction = new Transaction(
                    cursor.getInt(0), cursor.getString(1), cursor.getDouble(2),
                    cursor.getString(3), cursor.getString(4), cursor.getInt(5), cursor.getInt(6)
            );
            String subcategoryName = cursor.getString(7);
            int categoryId = cursor.getInt(8);
            transactionDisplays.add(new TransactionDisplay(transaction, subcategoryName, categoryId));
        }

        cursor.close();
        db.close();

        TransactionListAdapter adapter = new TransactionListAdapter(this, transactionDisplays);
        lvRecentTransaction.setAdapter(adapter);
    }

    private void updateBalanceViews() {
        if (isBalanceVisible) {
            tvTotalBalance.setText(formatCurrency(totalBalance));
            tvWallet1.setText(walletName1);
            tvBalanceWallet1.setText(formatCurrency(balance1));
            tvWallet2.setText(walletName2);
            tvBalanceWallet2.setText(formatCurrency(balance2));
        } else {
            hideBalanceViews();
        }
    }

    private void updateIncomeExpenseViews() {
        if (isBalanceVisible) {
            tvTotalExpenseThisMonth.setText(formatCurrency(totalExpense));
            tvTotalIncomeThisMonth.setText(formatCurrency(totalIncome));
        } else {
            hideBalanceViews();
        }
    }

    private void hideBalanceViews() {
        String hidden = "**********";
        tvTotalBalance.setText(hidden);
        tvBalanceWallet1.setText(hidden);
        tvBalanceWallet2.setText(hidden);
        tvTotalExpenseThisMonth.setText(hidden);
        tvTotalIncomeThisMonth.setText(hidden);
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f đ", amount);
    }

    private void setupEventListeners() {
        imgbtnEyeOpen.setOnClickListener(v -> {
            isBalanceVisible = !isBalanceVisible;
            if (isBalanceVisible) {
                updateBalanceViews();
                updateIncomeExpenseViews();
                imgbtnEyeOpen.setImageResource(R.drawable.ic_eye_open);
            } else {
                hideBalanceViews();
                imgbtnEyeOpen.setImageResource(R.drawable.ic_eye_close);
            }
        });

//        btnSeeAllWallet.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));
        btnSeeReport.setOnClickListener(v -> startActivity(new Intent(this, ReportActivity.class)));
        btnSeeAllTransaction.setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        imgbtnTransaction.setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));
        imgbtnAddTransaction.setOnClickListener(v -> startActivity(new Intent(this, AddTransactionActivity.class)));
//        imgbtnBudget.setOnClickListener(v -> startActivity(new Intent(this, BudgetActivity.class)));
//        imgbtnAccount.setOnClickListener(v -> startActivity(new Intent(this, AccountActivity.class)));
    }
}