package com.example.smartbudget.activity;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class SubCategoryActivity extends AppCompatActivity {
    private ImageButton imgbtnBack;
    private RadioGroup rgType;
    private RadioButton rdIncome, rdExpense;
    private EditText edtSubCategoryName;
    private Button btnSaveSubCategory;
    private ListView lvIncomeSubCategory, lvExpenseSubcategory;

    private List<String> incomeSubcategories = new ArrayList<>();
    private List<String> expenseSubcategories = new ArrayList<>();

    private ArrayAdapter<String> incomeAdapter;
    private ArrayAdapter<String> expenseAdapter;

    private int selectedId = -1;
    private int selectedCategoryId = 2; // 1: Expense, 2: Income

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_category);

        initViews();
        setupAdapters();
        setupListeners();
        loadSubcategories();
    }

    private void initViews() {
        imgbtnBack = findViewById(R.id.imgbtnBack);
        rgType = findViewById(R.id.rgType);
        rdIncome = findViewById(R.id.rdIncome);
        rdExpense = findViewById(R.id.rdExpense);
        edtSubCategoryName = findViewById(R.id.edtSubCategoryName);
        btnSaveSubCategory = findViewById(R.id.btnSaveSubCategory);
        lvIncomeSubCategory = findViewById(R.id.lvIncomeSubCategory);
        lvExpenseSubcategory = findViewById(R.id.lvExpenseSubCategory);
    }

    private void setupAdapters() {
        incomeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, incomeSubcategories);
        expenseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, expenseSubcategories);
        lvIncomeSubCategory.setAdapter(incomeAdapter);
        lvExpenseSubcategory.setAdapter(expenseAdapter);
    }

    private void setupListeners() {
        imgbtnBack.setOnClickListener(v -> finish());

        btnSaveSubCategory.setOnClickListener(v -> saveOrUpdateSubcategory());

        rgType.setOnCheckedChangeListener((group, checkedId) -> {
            selectedCategoryId = (checkedId == R.id.rdExpense) ? 1 : 2;
            resetSelection();
        });

        AdapterView.OnItemClickListener onItemClickListener = (parent, view, position, id) -> {
            String name = (String) parent.getItemAtPosition(position);
            selectedCategoryId = (parent == lvExpenseSubcategory) ? 1 : 2;
            loadSelectedSubcategory(name, selectedCategoryId);
            if (selectedCategoryId == 1) rdExpense.setChecked(true);
            else rdIncome.setChecked(true);
        };

        lvExpenseSubcategory.setOnItemClickListener(onItemClickListener);
        lvIncomeSubCategory.setOnItemClickListener(onItemClickListener);

        AdapterView.OnItemLongClickListener onItemLongClickListener = (parent, view, position, id) -> {
            String name = (String) parent.getItemAtPosition(position);
            int categoryId = (parent == lvExpenseSubcategory) ? 1 : 2;
            confirmDelete(name, categoryId);
            return true;
        };

        lvExpenseSubcategory.setOnItemLongClickListener(onItemLongClickListener);
        lvIncomeSubCategory.setOnItemLongClickListener(onItemLongClickListener);
    }

    private void loadSelectedSubcategory(String name, int categoryId) {
        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT id FROM subcategory WHERE name = ? AND categoryId = ?", new String[]{name, String.valueOf(categoryId)});
            if (cursor.moveToFirst()) {
                selectedId = cursor.getInt(0);
                edtSubCategoryName.setText(name);
            }
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
    }

    private void confirmDelete(String name, int categoryId) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa \"" + name + "\"?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    DBHelper dbHelper = new DBHelper(this);
                    SQLiteDatabase db = null;
                    try {
                        db = dbHelper.getWritableDatabase();
                        db.execSQL("DELETE FROM subcategory WHERE name = ? AND categoryId = ?", new Object[]{name, categoryId});
                        Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                        resetSelection();
                        loadSubcategories();
                    } finally {
                        if (db != null) db.close();
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void saveOrUpdateSubcategory() {
        String name = edtSubCategoryName.getText().toString().trim();
        if (name.isEmpty()) {
            edtSubCategoryName.setError("Nhập tên phân loại");
            return;
        }

        int checkedId = rgType.getCheckedRadioButtonId();
        selectedCategoryId = (checkedId == R.id.rdExpense) ? 1 : 2;

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getWritableDatabase();

            // Kiểm tra trùng tên trong cùng category, trừ chính id đang sửa
            cursor = db.rawQuery(
                    "SELECT id FROM subcategory WHERE name = ? AND categoryId = ?",
                    new String[]{name, String.valueOf(selectedCategoryId)}
            );

            boolean exists = false;
            while (cursor.moveToNext()) {
                int idFound = cursor.getInt(0);
                if (selectedId == -1 || idFound != selectedId) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                Toast.makeText(this, "Tên phân loại đã tồn tại", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedId == -1) {
                db.execSQL("INSERT INTO subcategory (name, categoryId) VALUES (?, ?)",
                        new Object[]{name, selectedCategoryId});
                Toast.makeText(this, "Đã thêm", Toast.LENGTH_SHORT).show();
            } else {
                db.execSQL("UPDATE subcategory SET name = ?, categoryId = ? WHERE id = ?",
                        new Object[]{name, selectedCategoryId, selectedId});
                Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
            }
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        resetSelection();
        loadSubcategories();
    }

    private void resetSelection() {
        selectedId = -1;
        edtSubCategoryName.setText("");
        // Đặt lại RadioGroup theo selectedCategoryId
        if (selectedCategoryId == 1) rdExpense.setChecked(true);
        else rdIncome.setChecked(true);
    }

    private void loadSubcategories() {
        incomeSubcategories.clear();
        expenseSubcategories.clear();

        DBHelper dbHelper = new DBHelper(this);
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT name, categoryId FROM subcategory", null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                int categoryId = cursor.getInt(1);
                if (categoryId == 1) expenseSubcategories.add(name);
                else incomeSubcategories.add(name);
            }
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        incomeAdapter.notifyDataSetChanged();
        expenseAdapter.notifyDataSetChanged();
    }
}