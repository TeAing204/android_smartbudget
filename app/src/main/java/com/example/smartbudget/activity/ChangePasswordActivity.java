package com.example.smartbudget.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartbudget.R;
import com.example.smartbudget.database.DBHelper;

public class ChangePasswordActivity extends AppCompatActivity {

    EditText edtOldPassword, edtNewPassword, edtConfirmPassword;
    Button btnChangePassword;

    int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        edtOldPassword = findViewById(R.id.edtOldPassword);
        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("userId", -1);

        btnChangePassword.setOnClickListener(v -> {
            String oldPassword = edtOldPassword.getText().toString().trim();
            String newPassword = edtNewPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 8) {
                Toast.makeText(this, "Mật khẩu mới phải có ít nhất 8 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId == -1) {
                Toast.makeText(this, "Không tìm thấy ID người dùng", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra mật khẩu cũ đúng không
            if (!checkOldPassword(userId, oldPassword)) {
                Toast.makeText(this, "Mật khẩu cũ không đúng", Toast.LENGTH_SHORT).show();
                return;
            }

            // Cập nhật mật khẩu mới
            updatePassword(userId, newPassword);
        });
    }

    private boolean checkOldPassword(int userId, String oldPassword) {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT password FROM user WHERE id = ?", new String[]{String.valueOf(userId)});
        boolean isCorrect = false;

        if (cursor.moveToFirst()) {
            String currentPassword = cursor.getString(cursor.getColumnIndexOrThrow("password"));
            isCorrect = currentPassword.equals(oldPassword);
        }

        cursor.close();
        db.close();
        return isCorrect;
    }

    private void updatePassword(int userId, String newPassword) {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL("UPDATE user SET password = ? WHERE id = ?", new Object[]{newPassword, userId});
        db.close();

        // Xóa thông tin đăng nhập đã lưu
        SharedPreferences.Editor editor = getSharedPreferences("UserPrefs", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Đổi mật khẩu thành công. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(ChangePasswordActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}