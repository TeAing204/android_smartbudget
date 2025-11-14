package com.example.smartbudget.activity;


import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbudget.R;
import com.example.smartbudget.database.DBHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {
    RadioGroup rgType;
    RadioButton rdIncome, rdExpense;
    Spinner spinnerWallet, spinnerSubcategory;
    EditText edtName, edtAmount, edtNote, edtDate;
    Button btnSaveTransaction;
    ImageButton imgbtnOverviewAddTransaction, imgbtnTransactionAddTransaction, imgbtnBudgetAddTransaction, imgbtnAccountAddTransaction;
    Calendar calendar;
    SimpleDateFormat sdf;

    int currentUserId;
    DBHelper dbHelper;
    SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_transaction);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.add_transaction), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Lấy userId hiện tại từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        currentUserId = prefs.getInt("userId", -1);
//        if (currentUserId == -1) {
//            Toast.makeText(this, "Không tìm thấy userId, vui lòng đăng nhập lại", Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
        if (currentUserId == -1) {
            // Gán tạm userId = 1 để test khi chưa có đăng nhập
            currentUserId = 1;
            Toast.makeText(this, "Chưa có đăng nhập — dùng tạm userId = 1 để test", Toast.LENGTH_SHORT).show();
        }

        // Ánh xạ view
        rgType = findViewById(R.id.rgType);
        rdIncome = findViewById(R.id.rdIncome);
        rdExpense = findViewById(R.id.rdExpense);
        spinnerWallet = findViewById(R.id.spinnerWallet);
        spinnerSubcategory = findViewById(R.id.spinnerSubcategory);
        edtName = findViewById(R.id.edtName);
        edtAmount = findViewById(R.id.edtAmount);
        edtNote = findViewById(R.id.edtNote);
        edtDate = findViewById(R.id.edtDate);
        btnSaveTransaction = findViewById(R.id.btnSaveTransaction);
//        imgbtnOverviewAddTransaction = findViewById(R.id.imgbtnOverviewAddTransaction);
        imgbtnTransactionAddTransaction = findViewById(R.id.imgbtnTransactionAddTransaction);
//        imgbtnBudgetAddTransaction = findViewById(R.id.imgbtnBudgetAddTransaction);
//        imgbtnAccountAddTransaction = findViewById(R.id.imgbtnAccountAddTransaction);

        // Khởi tạo DBHelper và mở DB 1 lần
        dbHelper = new DBHelper(this);
        db = dbHelper.getWritableDatabase();

        // Load ví của user hiện tại
        loadWallets();

        // Load subcategory theo mặc định dựa trên radio button
        loadSubcategories();

        rgType.setOnCheckedChangeListener((group, checkedId) -> loadSubcategories());

        calendar = Calendar.getInstance();
        sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        edtDate.setText(sdf.format(calendar.getTime()));

        edtDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
                calendar.set(selectedYear, selectedMonth, selectedDay);
                edtDate.setText(sdf.format(calendar.getTime()));
            }, year, month, day);

            datePickerDialog.show();
        });

        btnSaveTransaction.setOnClickListener(view -> saveTransaction());

        // Navigation button listeners
//        imgbtnOverviewAddTransaction.setOnClickListener(v -> startActivity(new Intent(AddTransactionActivity.this, OverviewActivity.class)));
        imgbtnTransactionAddTransaction.setOnClickListener(v -> startActivity(new Intent(AddTransactionActivity.this, TransactionActivity.class)));
//        imgbtnBudgetAddTransaction.setOnClickListener(v -> startActivity(new Intent(AddTransactionActivity.this, BudgetActivity.class)));
//        imgbtnAccountAddTransaction.setOnClickListener(v -> startActivity(new Intent(AddTransactionActivity.this, AccountActivity.class)));
    }

    // Hàm lưu giao dịch với kiểm tra và xử lý lỗi
    private void saveTransaction() {
        String name = edtName.getText().toString().trim();
        String amountStr = edtAmount.getText().toString().trim();
        String note = edtNote.getText().toString().trim();
        String date = edtDate.getText().toString().trim();

        // Kiểm tra nhập số tiền
        if (TextUtils.isEmpty(amountStr)) {
            edtAmount.setError("Vui lòng nhập số tiền");
            edtAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                edtAmount.setError("Số tiền phải lớn hơn 0");
                edtAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            edtAmount.setError("Số tiền không hợp lệ");
            edtAmount.requestFocus();
            return;
        }

        // Kiểm tra ngày hợp lệ (có thể parse ngày)
        try {
            sdf.parse(date);
        } catch (ParseException e) {
            Toast.makeText(this, "Ngày không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra chọn ví
        String selectedWallet = (String) spinnerWallet.getSelectedItem();
        if (TextUtils.isEmpty(selectedWallet)) {
            Toast.makeText(this, "Vui lòng chọn ví", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra chọn danh mục con
        String selectedSubcategory = (String) spinnerSubcategory.getSelectedItem();
        if (TextUtils.isEmpty(selectedSubcategory)) {
            Toast.makeText(this, "Vui lòng chọn danh mục", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Lấy walletId dựa trên walletName và userId
            int walletId = getWalletIdByName(db, selectedWallet, currentUserId);
            if (walletId == -1) {
                Toast.makeText(this, "Ví không tồn tại hoặc không thuộc user hiện tại", Toast.LENGTH_LONG).show();
                return;
            }

            int subcategoryId = getIdByName(db, "subcategory", selectedSubcategory);
            if (subcategoryId == -1) {
                Toast.makeText(this, "Danh mục con không tồn tại", Toast.LENGTH_LONG).show();
                return;
            }

            // Tên giao dịch mặc định nếu không nhập
            if (TextUtils.isEmpty(name)) {
                name = "Giao dịch";
            }

            // Thêm giao dịch vào bảng transactions
            db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES (?, ?, ?, ?, ?, ?)",
                    new Object[]{name, amount, note, date, walletId, subcategoryId});

            // Cập nhật số dư ví
            boolean isIncome = rdIncome.isChecked();

            Cursor cursor = db.rawQuery("SELECT balance FROM wallet WHERE id = ?", new String[]{String.valueOf(walletId)});
            if (cursor.moveToFirst()) {
                double currentBalance = cursor.getDouble(cursor.getColumnIndexOrThrow("balance"));
                double newBalance = isIncome ? currentBalance + amount : currentBalance - amount;

                db.execSQL("UPDATE wallet SET balance = ? WHERE id = ?", new Object[]{newBalance, walletId});
            }
            cursor.close();

            Toast.makeText(this, "Đã lưu giao dịch thành công", Toast.LENGTH_SHORT).show();
            finish();

        } catch (SQLException e) {
            Toast.makeText(this, "Lỗi khi lưu giao dịch: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    // Hàm lấy walletId dựa trên tên ví và userId
    private int getWalletIdByName(SQLiteDatabase db, String walletName, int userId) {
        Cursor cursor = db.rawQuery("SELECT id FROM wallet WHERE name = ? AND userId = ?", new String[]{walletName, String.valueOf(userId)});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();
        return id;
    }

    // Hàm lấy id dựa trên tên bảng và tên record
    private int getIdByName(SQLiteDatabase db, String tableName, String name) {
        Cursor cursor = db.rawQuery("SELECT id FROM " + tableName + " WHERE name = ?", new String[]{name});
        int id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();
        return id;
    }

    // Load ví chỉ của user hiện tại
    private void loadWallets() {
        Cursor cursor = db.rawQuery("SELECT name FROM wallet WHERE userId = ?", new String[]{String.valueOf(currentUserId)});
        List<String> walletNames = new ArrayList<>();
        int nameColumnIndex = cursor.getColumnIndexOrThrow("name");

        while (cursor.moveToNext()) {
            String name = cursor.getString(nameColumnIndex);
            walletNames.add(name);
        }

        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, walletNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWallet.setAdapter(adapter);
    }

    // Load subcategory theo loại thu hoặc chi
    private void loadSubcategories() {
        int categoryId = rdIncome.isChecked() ? 2 : 1;

        Cursor cursor = db.rawQuery("SELECT name FROM subcategory WHERE categoryId = ?", new String[]{String.valueOf(categoryId)});
        List<String> subcategoryNames = new ArrayList<>();
        int nameColumnIndex = cursor.getColumnIndexOrThrow("name");

        while (cursor.moveToNext()) {
            String name = cursor.getString(nameColumnIndex);
            subcategoryNames.add(name);
        }

        cursor.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, subcategoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubcategory.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Đóng database khi activity bị destroy
        if (db != null && db.isOpen()) {
            db.close();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}