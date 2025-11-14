package com.example.smartbudget.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.mindrot.jbcrypt.BCrypt;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "smart_budget.db";
    private static final int DATABASE_VERSION = 2;

    private static final String TABLE_USER = "user";
    private static final String TABLE_WALLET = "wallet";
    private static final String TABLE_CATEGORY = "category";
    private static final String TABLE_SUB_CATEGORY = "subcategory";
    private static final String TABLE_TRANSACTION = "transactions";
    private static final String TABLE_BUDGET = "budget";

    private static final String CREATE_USER_TABLE =
            "CREATE TABLE " + TABLE_USER + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE NOT NULL," +
                    "password TEXT NOT NULL)";

    private static final String CREATE_WALLET_TABLE =
            "CREATE TABLE " + TABLE_WALLET + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "balance REAL," +
                    "userId INTEGER," +
                    "FOREIGN KEY (userId) REFERENCES " + TABLE_USER + "(id) ON DELETE CASCADE)";

    private static final String CREATE_CATEGORY_TABLE =
            "CREATE TABLE " + TABLE_CATEGORY + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT)";

    private static final String CREATE_SUB_CATEGORY_TABLE =
            "CREATE TABLE " + TABLE_SUB_CATEGORY + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "categoryId INTEGER," +
                    "FOREIGN KEY (categoryId) REFERENCES " + TABLE_CATEGORY + "(id) ON DELETE CASCADE)";

    private static final String CREATE_BUDGET_TABLE =
            "CREATE TABLE " + TABLE_BUDGET + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "subcategoryId INTEGER," +
                    "budget REAL," +
                    "FOREIGN KEY (subcategoryId) REFERENCES " + TABLE_SUB_CATEGORY + "(id) ON DELETE CASCADE)";

    private static final String CREATE_TRANSACTION_TABLE =
            "CREATE TABLE " + TABLE_TRANSACTION + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT," +
                    "amount REAL," +
                    "note TEXT," +
                    "date TEXT," +
                    "walletId INTEGER," +
                    "subcategoryId INTEGER," +
                    "FOREIGN KEY (walletId) REFERENCES " + TABLE_WALLET + "(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (subcategoryId) REFERENCES " + TABLE_SUB_CATEGORY + "(id) ON DELETE CASCADE)";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_WALLET_TABLE);
        db.execSQL(CREATE_CATEGORY_TABLE);
        db.execSQL(CREATE_SUB_CATEGORY_TABLE);
        db.execSQL(CREATE_BUDGET_TABLE);
        db.execSQL(CREATE_TRANSACTION_TABLE);
        insertSampleData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TRANSACTION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BUDGET);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SUB_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORY);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WALLET);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER);
        onCreate(db);
    }

    private void insertSampleData(SQLiteDatabase db) {
        // Mã hóa mật khẩu admin trước khi insert
        String adminPassword = "Admin@123";
        String hashedPassword = BCrypt.hashpw(adminPassword, BCrypt.gensalt(12));

        // Sử dụng prepared statement để tránh SQL injection
        db.execSQL("INSERT INTO user (username, password) VALUES (?, ?);",
                new Object[]{"admin@gmail.com", hashedPassword});

        db.execSQL("INSERT INTO wallet (name, balance, userId) VALUES ('Tiền mặt', 500000, 1);");
        db.execSQL("INSERT INTO wallet (name, balance, userId) VALUES ('Ngân hàng', 10000000, 1);");
        db.execSQL("INSERT INTO category (name) VALUES ('Chi tiêu');");
        db.execSQL("INSERT INTO category (name) VALUES ('Thu nhập');");
        db.execSQL("INSERT INTO subcategory (name, categoryId) VALUES ('Ăn uống', 1);");
        db.execSQL("INSERT INTO subcategory (name, categoryId) VALUES ('Đi lại', 1);");
        db.execSQL("INSERT INTO subcategory (name, categoryId) VALUES ('Lương', 2);");
        db.execSQL("INSERT INTO subcategory (name, categoryId) VALUES ('Thưởng', 2);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Ăn sáng', 30000, 'Phở bò', '14/05/2025', 1, 1);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Ăn trưa', 50000, 'Cơm tấm', '14/05/2025', 1, 1);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Grab', 40000, 'Đi làm', '13/05/2025', 1, 2);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Xe buýt', 7000, 'Đi học', '12/05/2025', 1, 2);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Ăn vặt', 20000, 'Trà sữa', '11/05/2025', 1, 1);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Lương tháng 5', 15000000, 'Công ty ABC', '01/05/2025', 2, 3);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Lương freelance', 5000000, 'Dự án web', '03/05/2025', 2, 3);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Thưởng hiệu suất', 2000000, 'Tháng 4', '05/05/2025', 2, 4);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Thưởng lễ', 1000000, '30/4-1/5', '06/05/2025', 2, 4);");
        db.execSQL("INSERT INTO transactions (name, amount, note, date, walletId, subcategoryId) VALUES ('Lương part-time', 3000000, 'Trợ giảng', '07/05/2025', 2, 3);");
    }
}