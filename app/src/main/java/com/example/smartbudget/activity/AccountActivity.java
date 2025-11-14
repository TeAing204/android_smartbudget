package com.example.smartbudget.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbudget.R;
import com.example.smartbudget.database.DBHelper;

public class AccountActivity extends AppCompatActivity {
    private TextView tvUsername, tvMyWallet, tvMyCategory, tvChangePassword, tvLogout, tvDeleteAccount;
    private ImageButton imgbtnOverviewAccount, imgbtnTransactionAccount, imgbtnAddTransactionAccount, imgbtnBudgetAccount;
    private int userId;
    private SharedPreferences sharedPreferences;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.account), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        initViews();
        dbHelper = new DBHelper(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);

//        if (userId == -1) {
//            Toast.makeText(this, "Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }

        loadUserInfo();

        setListeners();

    }

    private void initViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvMyWallet = findViewById(R.id.tvMyWallet);
        tvMyCategory = findViewById(R.id.tvMyCategory);
        tvChangePassword = findViewById(R.id.tvChangePassword);
        tvLogout = findViewById(R.id.tvLogout);
        tvDeleteAccount = findViewById(R.id.tvDeleteAccount);

        imgbtnOverviewAccount = findViewById(R.id.imgbtnOverviewAccount);
        imgbtnTransactionAccount = findViewById(R.id.imgbtnTransactionAccount);
        imgbtnAddTransactionAccount = findViewById(R.id.imgbtnAddTransactionAccount);
        imgbtnBudgetAccount = findViewById(R.id.imgbtnBudgetAccount);
    }

    private void loadUserInfo() {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery("SELECT username FROM user WHERE id = ?", new String[]{String.valueOf(userId)})) {

            if (cursor.moveToFirst()) {
                String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
                tvUsername.setText(username);
            } else {
                tvUsername.setText("nhattrinh");
            }

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi tải thông tin người dùng", Toast.LENGTH_SHORT).show();
            tvUsername.setText("Lỗi");
        }
    }

    private void setListeners() {
        tvMyWallet.setOnClickListener(v -> startActivity(new Intent(this, WalletActivity.class)));

        tvMyCategory.setOnClickListener(v -> startActivity(new Intent(this, SubCategoryActivity.class)));

        //tvChangePassword.setOnClickListener(v -> startActivity(new Intent(this, ChangePasswordActivity.class)));

        tvLogout.setOnClickListener(v -> {
            sharedPreferences.edit().clear().apply();
            Intent intent = new Intent(AccountActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvDeleteAccount.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tài khoản không?")
                .setPositiveButton("Xóa", (dialog, which) -> deleteAccount())
                .setNegativeButton("Hủy", null)
                .show());

        //imgbtnOverviewAccount.setOnClickListener(v -> startActivity(new Intent(this, OverviewActivity.class)));

        imgbtnTransactionAccount.setOnClickListener(v -> startActivity(new Intent(this, TransactionActivity.class)));

        imgbtnAddTransactionAccount.setOnClickListener(v -> startActivity(new Intent(this, AddTransactionActivity.class)));

        //imgbtnBudgetAccount.setOnClickListener(v -> startActivity(new Intent(this, BudgetActivity.class)));
    }
    private void deleteAccount() {
        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            int rowsDeleted = db.delete("user", "id = ?", new String[]{String.valueOf(userId)});
            if (rowsDeleted > 0) {
                sharedPreferences.edit().clear().apply();
                Toast.makeText(this, "Đã xóa tài khoản", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(AccountActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Xóa tài khoản thất bại", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi xóa tài khoản: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}