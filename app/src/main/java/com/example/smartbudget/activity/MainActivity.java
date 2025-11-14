package com.example.smartbudget.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smartbudget.R;
import com.example.smartbudget.database.DBHelper;

//import com.example.smartbudget.notification.NotificationScheduler;


public class MainActivity extends AppCompatActivity {
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private TextView btnRegisterMain, btnLoginMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Kiểm tra nếu đã đăng nhập thì chuyển sang OverviewActivity
//        if (isUserLoggedIn()) {
//            // Lên lịch thông báo hàng ngày nếu đã đăng nhập
////            NotificationScheduler.scheduleDailyNotification(this);
//
//            startActivity(new Intent(MainActivity.this, OverviewActivity.class));
//            finish();
//            return;
//        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Khởi tạo database
        initDatabase();

        // Khởi tạo view
        btnRegisterMain = findViewById(R.id.btnRegisterMain);
        btnLoginMain = findViewById(R.id.btnLoginMain);

        // Gắn sự kiện
        btnRegisterMain.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        btnLoginMain.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
    }

//    private boolean isUserLoggedIn() {
//        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
//        return sharedPreferences.getInt("userId", -1) != -1;
//    }

    private void initDatabase() {
        dbHelper = new DBHelper(this);
        try {
            db = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }
}