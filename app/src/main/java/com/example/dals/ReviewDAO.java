package com.example.dals;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.models.ReviewSubmissionResult;

import java.io.IOException;

public final class ReviewDAO {

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LENGTH = 500;

    private ReviewDAO() {
        // Không cho phép tạo đối tượng ReviewDAO.
    }

    /**
     * Thêm đánh giá và bình luận cho sản phẩm.
     *
     * Sau khi INSERT thành công, phương thức tự động
     * tính lại AverageRating và RatingCount của Product.
     */
    public static ReviewSubmissionResult submitReview(
            Context context,
            int userId,
            int productId,
            int rating,
            String comment
    ) throws IOException {

        if (userId <= 0) {
            return ReviewSubmissionResult.invalidUser();
        }

        if (productId <= 0) {
            return ReviewSubmissionResult.productNotFound();
        }

        if (rating < MIN_RATING
                || rating > MAX_RATING) {

            return ReviewSubmissionResult.invalidRating();
        }

        String normalizedComment =
                comment == null
                        ? ""
                        : comment.trim();

        if (normalizedComment.isEmpty()) {
            return ReviewSubmissionResult.emptyComment();
        }

        if (normalizedComment.length()
                > MAX_COMMENT_LENGTH) {

            return ReviewSubmissionResult.emptyComment();
        }

        SQLiteDatabase database = null;

        try {
            database =
                    DatabaseHelper.openDatabase(context);

            database.beginTransaction();

            try {
                /*
                 * Chỉ khách hàng hợp lệ, chưa bị khóa,
                 * mới được gửi đánh giá.
                 */
                if (!isValidCustomer(
                        database,
                        userId
                )) {

                    return ReviewSubmissionResult
                            .invalidUser();
                }

                /*
                 * Sản phẩm phải tồn tại và đang hoạt động.
                 */
                if (!isAvailableProduct(
                        database,
                        productId
                )) {

                    return ReviewSubmissionResult
                            .productNotFound();
                }

                /*
                 * Mỗi khách hàng chỉ đánh giá
                 * một sản phẩm một lần.
                 */
                if (hasExistingReview(
                        database,
                        userId,
                        productId
                )) {

                    return ReviewSubmissionResult
                            .alreadyReviewed();
                }

                /*
                 * INSERT đồng thời Rating và Comment
                 * vào bảng ProductReview.
                 */
                insertReview(
                        database,
                        userId,
                        productId,
                        rating,
                        normalizedComment
                );

                /*
                 * Ngay sau khi INSERT thành công,
                 * tự động tính lại điểm trung bình.
                 */
                recalculateProductRating(
                        database,
                        productId
                );

                ProductRatingSnapshot snapshot =
                        getProductRatingSnapshot(
                                database,
                                productId
                        );

                if (snapshot == null) {

                    throw new IllegalStateException(
                            "Unable to read updated product rating."
                    );
                }

                /*
                 * Chỉ commit khi:
                 * - INSERT đánh giá thành công
                 * - UPDATE điểm trung bình thành công
                 */
                database.setTransactionSuccessful();

                return ReviewSubmissionResult.inserted(
                        snapshot.averageRating,
                        snapshot.ratingCount
                );

            } finally {
                database.endTransaction();
            }

        } finally {

            if (database != null
                    && database.isOpen()) {

                database.close();
            }
        }
    }

    /**
     * Kiểm tra người dùng là Customer và chưa bị khóa.
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
     * Kiểm tra sản phẩm tồn tại và đang hoạt động.
     */
    private static boolean isAvailableProduct(
            SQLiteDatabase database,
            int productId
    ) {

        String sql =
                "SELECT 1 " +
                        "FROM Product " +
                        "WHERE ProductID = ? " +
                        "AND IsActive = 1 " +
                        "LIMIT 1";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(productId)
                                }
                        )
        ) {

            return cursor.moveToFirst();
        }
    }

    /**
     * Kiểm tra khách hàng đã đánh giá sản phẩm chưa.
     */
    private static boolean hasExistingReview(
            SQLiteDatabase database,
            int userId,
            int productId
    ) {

        String sql =
                "SELECT 1 " +
                        "FROM ProductReview " +
                        "WHERE UserID = ? " +
                        "AND ProductID = ? " +
                        "LIMIT 1";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(userId),
                                        String.valueOf(productId)
                                }
                        )
        ) {

            return cursor.moveToFirst();
        }
    }

    /**
     * INSERT Rating và Comment vào SQLite.
     */
    private static void insertReview(
            SQLiteDatabase database,
            int userId,
            int productId,
            int rating,
            String comment
    ) {

        String insertSql =
                "INSERT INTO ProductReview (" +
                        "UserID, " +
                        "ProductID, " +
                        "Rating, " +
                        "Comment, " +
                        "CreatedAt, " +
                        "UpdatedAt" +
                        ") VALUES (" +
                        "?, ?, ?, ?, " +
                        "datetime('now', 'localtime'), " +
                        "datetime('now', 'localtime')" +
                        ")";

        database.execSQL(
                insertSql,
                new Object[]{
                        userId,
                        productId,
                        rating,
                        comment
                }
        );
    }

    /**
     * Tính lại:
     * - AverageRating
     * - RatingCount
     *
     * ngay sau khi thêm đánh giá thành công.
     */
    private static void recalculateProductRating(
            SQLiteDatabase database,
            int productId
    ) {

        String updateSql =
                "UPDATE Product " +
                        "SET " +

                        "AverageRating = COALESCE((" +
                        "SELECT AVG(" +
                        "CAST(Rating AS REAL)" +
                        ") " +
                        "FROM ProductReview " +
                        "WHERE ProductID = ?" +
                        "), 0), " +

                        "RatingCount = (" +
                        "SELECT COUNT(*) " +
                        "FROM ProductReview " +
                        "WHERE ProductID = ?" +
                        "), " +

                        "UpdatedAt = " +
                        "datetime('now', 'localtime') " +

                        "WHERE ProductID = ?";

        database.execSQL(
                updateSql,
                new Object[]{
                        productId,
                        productId,
                        productId
                }
        );
    }

    /**
     * Đọc kết quả sau khi tính lại điểm.
     */
    private static ProductRatingSnapshot
    getProductRatingSnapshot(
            SQLiteDatabase database,
            int productId
    ) {

        String sql =
                "SELECT " +
                        "AverageRating, " +
                        "RatingCount " +
                        "FROM Product " +
                        "WHERE ProductID = ? " +
                        "LIMIT 1";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(productId)
                                }
                        )
        ) {

            if (!cursor.moveToFirst()) {
                return null;
            }

            double averageRating =
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "AverageRating"
                            )
                    );

            int ratingCount =
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    "RatingCount"
                            )
                    );

            return new ProductRatingSnapshot(
                    averageRating,
                    ratingCount
            );
        }
    }

    private static final class ProductRatingSnapshot {

        private final double averageRating;
        private final int ratingCount;

        private ProductRatingSnapshot(
                double averageRating,
                int ratingCount
        ) {
            this.averageRating = averageRating;
            this.ratingCount = ratingCount;
        }
    }
}