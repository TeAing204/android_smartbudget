package com.example.smartbudget.activity;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbudget.R;
import com.example.smartbudget.database.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class WalletActivity extends AppCompatActivity {

    private EditText edtWalletName, edtWalletBalance;
    private Button btnSaveWallet;
    private ImageButton imgbtnBack;
    private ListView lvWallet;

    private int selectedWalletId = -1;
    private int userId;
    private DBHelper dbHelper;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wallet);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getInt("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        dbHelper = new DBHelper(this);

        setupListeners();
        loadWallets();
    }


    private void initViews() {
        imgbtnBack = findViewById(R.id.imgbtnBack);
        edtWalletName = findViewById(R.id.edtWalletName);
        edtWalletBalance = findViewById(R.id.edtWalletBalance);
        btnSaveWallet = findViewById(R.id.btnSaveWallet);
        lvWallet = findViewById(R.id.lvWallet);
    }

    private void setupListeners() {
        imgbtnBack.setOnClickListener(v -> finish());

        btnSaveWallet.setOnClickListener(v -> saveWallet());

        lvWallet.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            String name = selected.split(":")[0].trim();
            loadWalletForEdit(name);
        });

        lvWallet.setOnItemLongClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            String name = selected.split(":")[0].trim();
            confirmDeleteWallet(name);
            return true;
        });
    }

    private void loadWalletForEdit(String name) {
        String query = "SELECT id, balance FROM wallet WHERE name = ? AND userId = ?";
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(query, new String[]{name, String.valueOf(userId)})) {
            if (cursor.moveToFirst()) {
                selectedWalletId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                double balance = cursor.getDouble(cursor.getColumnIndexOrThrow("balance"));
                edtWalletName.setText(name);
                edtWalletBalance.setText(String.valueOf((int) balance));
            }
        }
    }

    private void confirmDeleteWallet(String name) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn có chắc chắn muốn xóa ví \"" + name + "\" không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteWallet(name))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteWallet(String name) {
        String sql = "DELETE FROM wallet WHERE name = ? AND userId = ?";
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            db.execSQL(sql, new Object[]{name, userId});
            Toast.makeText(this, "Đã xóa ví", Toast.LENGTH_SHORT).show();
            resetInputFields();
            loadWallets();
        } catch (Exception e) {
            Toast.makeText(this, "Xóa ví thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveWallet() {
        String name = edtWalletName.getText().toString().trim();
        String balanceStr = edtWalletBalance.getText().toString().trim();

        if (name.isEmpty()) {
            edtWalletName.setError("Vui lòng nhập tên ví");
            return;
        }

        if (balanceStr.isEmpty()) {
            edtWalletBalance.setError("Vui lòng nhập số dư");
            return;
        }

        double balance;
        try {
            balance = Double.parseDouble(balanceStr);
        } catch (NumberFormatException e) {
            edtWalletBalance.setError("Số dư không hợp lệ");
            return;
        }

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            if (selectedWalletId == -1) {
                db.execSQL("INSERT INTO wallet (name, balance, userId) VALUES (?, ?, ?)", new Object[]{name, balance, userId});
                Toast.makeText(this, "Đã thêm ví", Toast.LENGTH_SHORT).show();
            } else {
                db.execSQL("UPDATE wallet SET name = ?, balance = ? WHERE id = ?", new Object[]{name, balance, selectedWalletId});
                Toast.makeText(this, "Đã cập nhật ví", Toast.LENGTH_SHORT).show();
            }
            resetInputFields();
            loadWallets();
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi lưu ví: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void resetInputFields() {
        edtWalletName.setText("");
        edtWalletBalance.setText("");
        selectedWalletId = -1;
    }

    private void loadWallets() {
        List<String> walletList = new ArrayList<>();
        String query = "SELECT name, balance FROM wallet WHERE userId = ?";

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)})) {

            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                double balance = cursor.getDouble(cursor.getColumnIndexOrThrow("balance"));
                String formattedBalance = String.format("%,d đ", (int) balance);
                walletList.add(name + ": " + formattedBalance);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, walletList);
        lvWallet.setAdapter(adapter);
    }
}