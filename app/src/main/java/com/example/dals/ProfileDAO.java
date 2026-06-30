package com.example.dals;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.models.MyProfileData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ProfileDAO {

    private ProfileDAO() {
        // Không cho phép khởi tạo đối tượng.
    }

    /**
     * Lấy toàn bộ dữ liệu cho màn hình My Profile.
     */
    public static MyProfileData getMyProfileData(
            Context context,
            int userId
    ) throws IOException {

        if (userId <= 0) {
            return MyProfileData.invalidCustomer();
        }

        SQLiteDatabase database = null;

        try {
            database =
                    DatabaseHelper.openDatabase(context);

            /*
             * Không chỉ tin vào SessionManager.
             * Kiểm tra lại người dùng trong SQLite.
             */
            if (!isValidCustomer(database, userId)) {

                return MyProfileData.invalidCustomer();
            }

            PurchaseStatistics statistics =
                    getPurchaseStatistics(
                            database,
                            userId
                    );

            List<String> comments =
                    getCustomerComments(
                            database,
                            userId
                    );

            return MyProfileData.success(
                    statistics.totalOrders,
                    statistics.deliveredOrders,
                    statistics.totalSpent,
                    comments
            );

        } finally {

            if (database != null
                    && database.isOpen()) {

                database.close();
            }
        }
    }

    /**
     * Kiểm tra Customer tồn tại và chưa bị khóa.
     */
    private static boolean isValidCustomer(
            SQLiteDatabase database,
            int userId
    ) {

        String sql =
                "SELECT 1 " +
                        "FROM \"User\" " +
                        "WHERE UserID = ? " +
                        "AND Role = 'CUSTOMER' " +
                        "AND IsLocked = 0 " +
                        "AND FailedLoginCount < 3 " +
                        "LIMIT 1";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(userId)
                                }
                        )
        ) {

            return cursor.moveToFirst();
        }
    }

    /**
     * Tính:
     * - Tổng số đơn hàng đã đặt.
     * - Số đơn hàng DELIVERED.
     * - Tổng tiền của đơn hàng DELIVERED.
     */
    private static PurchaseStatistics
    getPurchaseStatistics(
            SQLiteDatabase database,
            int userId
    ) {

        String sql =
                "SELECT " +

                        "COUNT(OrderID) AS TotalOrders, " +

                        "COALESCE(" +
                        "SUM(" +
                        "CASE " +
                        "WHEN OrderStatus = 'DELIVERED' " +
                        "THEN 1 " +
                        "ELSE 0 " +
                        "END" +
                        "), " +
                        "0" +
                        ") AS DeliveredOrders, " +

                        "COALESCE(" +
                        "SUM(" +
                        "CASE " +
                        "WHEN OrderStatus = 'DELIVERED' " +
                        "THEN TotalAmount " +
                        "ELSE 0 " +
                        "END" +
                        "), " +
                        "0" +
                        ") AS TotalSpent " +

                        "FROM Orders " +
                        "WHERE UserID = ?";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(userId)
                                }
                        )
        ) {

            if (!cursor.moveToFirst()) {

                return PurchaseStatistics.empty();
            }

            int totalOrders =
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    "TotalOrders"
                            )
                    );

            int deliveredOrders =
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    "DeliveredOrders"
                            )
                    );

            double totalSpent =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "TotalSpent"
                            )
                    );

            return new PurchaseStatistics(
                    totalOrders,
                    deliveredOrders,
                    totalSpent
            );
        }
    }

    /**
     * Lấy toàn bộ bình luận của Customer.
     */
    private static List<String> getCustomerComments(
            SQLiteDatabase database,
            int userId
    ) {

        List<String> comments =
                new ArrayList<>();

        String sql =
                "SELECT Comment " +
                        "FROM ProductReview " +
                        "WHERE UserID = ? " +
                        "AND Comment IS NOT NULL " +
                        "AND TRIM(Comment) <> '' " +
                        "ORDER BY CreatedAt DESC";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(userId)
                                }
                        )
        ) {

            while (cursor.moveToNext()) {

                String comment =
                        cursor.getString(
                                cursor.getColumnIndexOrThrow(
                                        "Comment"
                                )
                        );

                comments.add(comment);
            }
        }

        return comments;
    }

    private static final class PurchaseStatistics {

        private final int totalOrders;
        private final int deliveredOrders;
        private final double totalSpent;

        private PurchaseStatistics(
                int totalOrders,
                int deliveredOrders,
                double totalSpent
        ) {
            this.totalOrders = totalOrders;
            this.deliveredOrders = deliveredOrders;
            this.totalSpent = totalSpent;
        }

        private static PurchaseStatistics empty() {

            return new PurchaseStatistics(
                    0,
                    0,
                    0
            );
        }
    }
}