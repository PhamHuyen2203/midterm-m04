package com.example.mcommercemobile04;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.dals.DatabaseHelper;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG =
            "DatabaseCheck";

    private static final String[] EXPECTED_TABLES = {
            "User",
            "Category",
            "Product",
            "Cart",
            "CartItem",
            "Orders",
            "OrderDetail",
            "Payment",
            "ProductReview"
    };

    private TextView textViewDatabaseStatus;
    private Button buttonCheckDatabase;

    /*
     * Giữ kết nối mở trong lúc Activity tồn tại
     * để Database Inspector có thể nhìn thấy database.
     */
    private SQLiteDatabase database;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mapViews();
        setEvents();

        openAndCheckDatabase();
    }

    /**
     * Ánh xạ View.
     */
    private void mapViews() {

        textViewDatabaseStatus =
                findViewById(
                        R.id.textViewDatabaseStatus
                );

        buttonCheckDatabase =
                findViewById(
                        R.id.buttonCheckDatabase
                );
    }

    /**
     * Gán sự kiện.
     */
    private void setEvents() {

        buttonCheckDatabase.setOnClickListener(
                view -> openAndCheckDatabase()
        );
    }

    /**
     * Mở database và kiểm tra cấu trúc.
     */
    private void openAndCheckDatabase() {

        try {

            if (database == null
                    || !database.isOpen()) {

                database =
                        DatabaseHelper.openDatabase(this);
            }

            int tableCount =
                    countExpectedTables(database);

            if (tableCount
                    != EXPECTED_TABLES.length) {

                showMissingTableStatus(tableCount);
                return;
            }

            if (hasForeignKeyViolation(database)) {

                showForeignKeyErrorStatus();
                return;
            }

            showSuccessStatus(tableCount);

        } catch (
                IOException
                | SQLiteException exception
        ) {

            showDatabaseErrorStatus();

            Log.e(
                    LOG_TAG,
                    "Database initialization failed.",
                    exception
            );
        }
    }

    /**
     * Đếm đúng các bảng mà ứng dụng yêu cầu.
     */
    private int countExpectedTables(
            SQLiteDatabase database
    ) {

        String sql =
                "SELECT COUNT(*) " +
                        "FROM sqlite_master " +
                        "WHERE type = 'table' " +
                        "AND name IN (" +
                        "?, ?, ?, ?, ?, ?, ?, ?, ?" +
                        ")";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                EXPECTED_TABLES
                        )
        ) {

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

            return 0;
        }
    }

    /**
     * Kiểm tra dữ liệu vi phạm khóa ngoại.
     */
    private boolean hasForeignKeyViolation(
            SQLiteDatabase database
    ) {

        try (
                Cursor cursor =
                        database.rawQuery(
                                "PRAGMA foreign_key_check",
                                null
                        )
        ) {

            return cursor.moveToFirst();
        }
    }

    /**
     * Hiển thị trạng thái thành công.
     */
    private void showSuccessStatus(
            int tableCount
    ) {

        textViewDatabaseStatus.setText(
                getString(
                        R.string.database_check_success,
                        tableCount
                )
        );

        textViewDatabaseStatus.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.mc_success
                )
        );
    }

    /**
     * Hiển thị trạng thái thiếu bảng.
     */
    private void showMissingTableStatus(
            int currentTableCount
    ) {

        textViewDatabaseStatus.setText(
                getString(
                        R.string.database_table_missing,
                        currentTableCount,
                        EXPECTED_TABLES.length
                )
        );

        textViewDatabaseStatus.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.mc_warning
                )
        );
    }

    /**
     * Hiển thị lỗi khóa ngoại.
     */
    private void showForeignKeyErrorStatus() {

        textViewDatabaseStatus.setText(
                R.string.database_foreign_key_error
        );

        textViewDatabaseStatus.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.mc_error
                )
        );
    }

    /**
     * Hiển thị lỗi không mở được database.
     */
    private void showDatabaseErrorStatus() {

        textViewDatabaseStatus.setText(
                R.string.database_check_failed
        );

        textViewDatabaseStatus.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.mc_error
                )
        );
    }

    @Override
    protected void onDestroy() {

        if (database != null
                && database.isOpen()) {

            database.close();
        }

        super.onDestroy();
    }
}