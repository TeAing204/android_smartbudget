package com.example.smartbudget.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbudget.R;
import com.example.smartbudget.database.DBHelper;

import java.util.regex.Pattern;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterActivity extends AppCompatActivity {
    private EditText edtUsernameRegister, edtPasswordRegister;
    private TextView btnRegister, btnGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.register), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        edtUsernameRegister = findViewById(R.id.edtUsernameRegister);
        edtPasswordRegister = findViewById(R.id.edtPasswordRegister);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoToLogin = findViewById(R.id.btnGoToLogin);

        btnRegister.setOnClickListener(v -> handleRegister());
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    private void handleRegister() {
        String username = edtUsernameRegister.getText().toString().trim();
        String password = edtPasswordRegister.getText().toString().trim();

        // Kiểm tra rỗng
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tất cả thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra định dạng email
        if (!isValidEmail(username)) {
            Toast.makeText(this, "Tên đăng nhập không đúng định dạng email!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kiểm tra mật khẩu
        String passwordError = validatePassword(password);
        if (passwordError != null) {
            Toast.makeText(this, passwordError, Toast.LENGTH_LONG).show();
            return;
        }

        SQLiteDatabase db = null;
        try {
            DBHelper dbHelper = new DBHelper(this);
            db = dbHelper.getWritableDatabase();

            // Kiểm tra email đã tồn tại chưa
            try (Cursor cursor = db.query(
                    "user",
                    new String[]{"id"},
                    "username = ?",
                    new String[]{username},
                    null, null, null
            )) {
                if (cursor.moveToFirst()) {
                    Toast.makeText(this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // MÃ HÓA MẬT KHẨU BẰNG BCRYPT TRƯỚC KHI LƯU
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));

            // Thêm người dùng mới
            ContentValues values = new ContentValues();
            values.put("username", username);
            values.put("password", hashedPassword); // Lưu mật khẩu đã mã hóa

            long newUserId = db.insert("user", null, values);
            if (newUserId == -1) {
                Toast.makeText(this, "Đăng ký thất bại!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo 2 ví mặc định cho user mới
            createDefaultWallets(db, newUserId);

            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();

        } catch (SQLiteException e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi cơ sở dữ liệu!", Toast.LENGTH_SHORT).show();
        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private String validatePassword(String password) {
        if (password.length() < 8) {
            return "Mật khẩu phải có ít nhất 8 ký tự!";
        }

        // Kiểm tra ký tự đầu tiên phải là chữ hoa
        if (!Character.isUpperCase(password.charAt(0))) {
            return "Ký tự đầu tiên của mật khẩu phải viết hoa!";
        }

        // Kiểm tra có ít nhất 1 chữ số
        if (!password.matches(".*\\d.*")) {
            return "Mật khẩu phải chứa ít nhất 1 chữ số!";
        }

        // Kiểm tra có ít nhất 1 ký tự đặc biệt
        Pattern specialCharPattern = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
        if (!specialCharPattern.matcher(password).find()) {
            return "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt (!@#$%^&*...)!";
        }

        return null; // Mật khẩu hợp lệ
    }

    private void createDefaultWallets(SQLiteDatabase db, long userId) {
        ContentValues wallet1 = new ContentValues();
        wallet1.put("name", "Tiền mặt");
        wallet1.put("balance", 0);
        wallet1.put("userId", userId);
        db.insert("wallet", null, wallet1);

        ContentValues wallet2 = new ContentValues();
        wallet2.put("name", "Ngân hàng");
        wallet2.put("balance", 0);
        wallet2.put("userId", userId);
        db.insert("wallet", null, wallet2);
    }
}