package com.example.dals;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.models.LoginResult;

import java.io.IOException;

public final class UserDAO {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 3;

    private UserDAO() {
        // Không cho phép khởi tạo đối tượng UserDAO.
    }

    /**
     * Kiểm tra tài khoản đăng nhập cho cả ADMIN và CUSTOMER.
     */
    public static LoginResult login(
            Context context,
            String username,
            String password
    ) throws IOException {

        SQLiteDatabase database = null;
        Cursor cursor = null;

        try {
            database = DatabaseHelper.openDatabase(context);

            String selectSql =
                    "SELECT " +
                            "UserID, " +
                            "FullName, " +
                            "Password, " +
                            "Role, " +
                            "FailedLoginCount, " +
                            "IsLocked " +
                            "FROM \"User\" " +
                            "WHERE Username = ? " +
                            "LIMIT 1";

            cursor = database.rawQuery(
                    selectSql,
                    new String[]{username}
            );

            /*
             * Không tìm thấy username.
             */
            if (!cursor.moveToFirst()) {
                return LoginResult.userNotFound();
            }

            int userId = cursor.getInt(
                    cursor.getColumnIndexOrThrow("UserID")
            );

            String fullName = cursor.getString(
                    cursor.getColumnIndexOrThrow("FullName")
            );

            String storedPassword = cursor.getString(
                    cursor.getColumnIndexOrThrow("Password")
            );

            String role = cursor.getString(
                    cursor.getColumnIndexOrThrow("Role")
            );

            int failedLoginCount = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                            "FailedLoginCount"
                    )
            );

            int isLocked = cursor.getInt(
                    cursor.getColumnIndexOrThrow("IsLocked")
            );

            /*
             * Đã đọc đủ dữ liệu, đóng Cursor trước khi UPDATE.
             */
            cursor.close();
            cursor = null;

            /*
             * Tài khoản đã khóa thì không được kiểm tra
             * mật khẩu nữa, kể cả mật khẩu nhập đúng.
             */
            if (isLocked == 1) {
                return LoginResult.accountLocked(
                        failedLoginCount
                );
            }

            /*
             * Phòng trường hợp dữ liệu có FailedLoginCount >= 3
             * nhưng IsLocked chưa được cập nhật.
             */
            if (failedLoginCount
                    >= MAX_FAILED_LOGIN_ATTEMPTS) {

                lockAccount(database, userId);

                return LoginResult.accountLocked(
                        failedLoginCount
                );
            }

            /*
             * Mật khẩu đúng:
             * reset số lần đăng nhập sai liên tiếp về 0.
             */
            if (storedPassword.equals(password)) {

                resetFailedLoginCount(
                        database,
                        userId
                );

                return LoginResult.success(
                        userId,
                        fullName,
                        role
                );
            }

            /*
             * Mật khẩu sai:
             * tăng số lần sai và khóa nếu đạt 3 lần.
             */
            return registerFailedLogin(
                    database,
                    userId
            );

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            if (database != null
                    && database.isOpen()) {

                database.close();
            }
        }
    }

    /**
     * Ghi nhận một lần nhập sai mật khẩu.
     *
     * UPDATE được thực hiện trực tiếp trong SQLite.
     */
    private static LoginResult registerFailedLogin(
            SQLiteDatabase database,
            int userId
    ) {

        Cursor cursor = null;

        database.beginTransaction();

        try {
            String updateSql =
                    "UPDATE \"User\" " +
                            "SET " +
                            "FailedLoginCount = " +
                            "FailedLoginCount + 1, " +

                            "IsLocked = CASE " +
                            "WHEN FailedLoginCount + 1 >= ? " +
                            "THEN 1 " +
                            "ELSE 0 " +
                            "END, " +

                            "UpdatedAt = " +
                            "datetime('now', 'localtime') " +

                            "WHERE UserID = ?";

            database.execSQL(
                    updateSql,
                    new Object[]{
                            MAX_FAILED_LOGIN_ATTEMPTS,
                            userId
                    }
            );

            String checkSql =
                    "SELECT " +
                            "FailedLoginCount, " +
                            "IsLocked " +
                            "FROM \"User\" " +
                            "WHERE UserID = ? " +
                            "LIMIT 1";

            cursor = database.rawQuery(
                    checkSql,
                    new String[]{
                            String.valueOf(userId)
                    }
            );

            if (!cursor.moveToFirst()) {
                throw new IllegalStateException(
                        "Unable to read updated user."
                );
            }

            int updatedFailedCount = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                            "FailedLoginCount"
                    )
            );

            int updatedLockedStatus = cursor.getInt(
                    cursor.getColumnIndexOrThrow(
                            "IsLocked"
                    )
            );

            database.setTransactionSuccessful();

            if (updatedLockedStatus == 1) {

                return LoginResult.accountLocked(
                        updatedFailedCount
                );
            }

            int remainingAttempts =
                    MAX_FAILED_LOGIN_ATTEMPTS
                            - updatedFailedCount;

            return LoginResult.wrongPassword(
                    updatedFailedCount,
                    remainingAttempts
            );

        } finally {

            if (cursor != null) {
                cursor.close();
            }

            database.endTransaction();
        }
    }

    /**
     * Khi đăng nhập đúng, đưa số lần sai liên tiếp về 0.
     */
    private static void resetFailedLoginCount(
            SQLiteDatabase database,
            int userId
    ) {

        String updateSql =
                "UPDATE \"User\" " +
                        "SET " +
                        "FailedLoginCount = 0, " +
                        "IsLocked = 0, " +
                        "UpdatedAt = " +
                        "datetime('now', 'localtime') " +
                        "WHERE UserID = ?";

        database.execSQL(
                updateSql,
                new Object[]{userId}
        );
    }

    /**
     * Khóa tài khoản trực tiếp trong SQLite.
     */
    private static void lockAccount(
            SQLiteDatabase database,
            int userId
    ) {

        String updateSql =
                "UPDATE \"User\" " +
                        "SET " +
                        "FailedLoginCount = CASE " +
                        "WHEN FailedLoginCount < ? " +
                        "THEN ? " +
                        "ELSE FailedLoginCount " +
                        "END, " +

                        "IsLocked = 1, " +

                        "UpdatedAt = " +
                        "datetime('now', 'localtime') " +

                        "WHERE UserID = ?";

        database.execSQL(
                updateSql,
                new Object[]{
                        MAX_FAILED_LOGIN_ATTEMPTS,
                        MAX_FAILED_LOGIN_ATTEMPTS,
                        userId
                }
        );
    }
}