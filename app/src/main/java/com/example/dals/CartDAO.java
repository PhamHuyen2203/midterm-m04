package com.example.dals;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.example.models.CartActionResult;

import java.io.IOException;

public final class CartDAO {

    private CartDAO() {
        // Không cho phép khởi tạo đối tượng.
    }

    /**
     * Thêm sản phẩm vào giỏ hàng.
     *
     * Nếu chưa có trong giỏ:
     * INSERT CartItem.
     *
     * Nếu đã có trong giỏ:
     * UPDATE Quantity.
     */
    public static CartActionResult addProductToCart(
            Context context,
            int userId,
            int productId,
            int quantityToAdd
    ) throws IOException {

        if (userId <= 0
                || productId <= 0
                || quantityToAdd <= 0) {

            return CartActionResult.invalidUser();
        }

        SQLiteDatabase database = null;

        try {
            database =
                    DatabaseHelper.openDatabase(context);

            database.beginTransaction();

            CartActionResult result =
                    CartActionResult.productNotFound();

            try {

                if (!isValidCustomer(
                        database,
                        userId
                )) {

                    result =
                            CartActionResult.invalidUser();

                } else {

                    ProductSnapshot product =
                            getProductSnapshot(
                                    database,
                                    productId
                            );

                    if (product == null) {

                        result =
                                CartActionResult
                                        .productNotFound();

                    } else {

                        int cartId =
                                getOrCreateActiveCart(
                                        database,
                                        userId
                                );

                        CartItemSnapshot cartItem =
                                getCartItem(
                                        database,
                                        cartId,
                                        productId
                                );

                        if (cartItem == null) {

                            /*
                             * Sản phẩm chưa có trong giỏ:
                             * dùng INSERT.
                             */
                            if (quantityToAdd
                                    > product.stockQuantity) {

                                result =
                                        CartActionResult
                                                .outOfStock();

                            } else {

                                insertCartItem(
                                        database,
                                        cartId,
                                        productId,
                                        quantityToAdd,
                                        product.price
                                );

                                touchCart(
                                        database,
                                        cartId
                                );

                                result =
                                        CartActionResult
                                                .inserted(
                                                        quantityToAdd
                                                );
                            }

                        } else {

                            /*
                             * Sản phẩm đã có trong giỏ:
                             * dùng UPDATE.
                             */
                            int newQuantity =
                                    cartItem.quantity
                                            + quantityToAdd;

                            if (newQuantity
                                    > product.stockQuantity) {

                                result =
                                        CartActionResult
                                                .outOfStock();

                            } else {

                                updateCartItem(
                                        database,
                                        cartItem.cartItemId,
                                        newQuantity,
                                        product.price
                                );

                                touchCart(
                                        database,
                                        cartId
                                );

                                result =
                                        CartActionResult
                                                .updated(
                                                        newQuantity
                                                );
                            }
                        }
                    }
                }

                database.setTransactionSuccessful();

            } finally {
                database.endTransaction();
            }

            return result;

        } finally {

            if (database != null
                    && database.isOpen()) {

                database.close();
            }
        }
    }

    /**
     * Lấy tổng số lượng sản phẩm trong giỏ ACTIVE.
     */
    public static int getTotalQuantity(
            Context context,
            int userId
    ) throws IOException {

        SQLiteDatabase database = null;

        try {
            database =
                    DatabaseHelper.openDatabase(context);

            String sql =
                    "SELECT " +
                            "COALESCE(SUM(ci.Quantity), 0) " +
                            "FROM CartItem AS ci " +
                            "WHERE ci.CartID = (" +
                            "SELECT CartID " +
                            "FROM Cart " +
                            "WHERE UserID = ? " +
                            "AND Status = 'ACTIVE' " +
                            "ORDER BY CartID DESC " +
                            "LIMIT 1" +
                            ")";

            try (
                    Cursor cursor =
                            database.rawQuery(
                                    sql,
                                    new String[]{
                                            String.valueOf(
                                                    userId
                                            )
                                    }
                            )
            ) {

                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }

                return 0;
            }

        } finally {

            if (database != null
                    && database.isOpen()) {

                database.close();
            }
        }
    }

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

    private static ProductSnapshot getProductSnapshot(
            SQLiteDatabase database,
            int productId
    ) {

        String sql =
                "SELECT Price, StockQuantity " +
                        "FROM Product " +
                        "WHERE ProductID = ? " +
                        "AND IsActive = 1 " +
                        "LIMIT 1";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(
                                                productId
                                        )
                                }
                        )
        ) {

            if (!cursor.moveToFirst()) {
                return null;
            }

            return new ProductSnapshot(
                    cursor.getDouble(
                            cursor.getColumnIndexOrThrow(
                                    "Price"
                            )
                    ),

                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    "StockQuantity"
                            )
                    )
            );
        }
    }

    private static int getOrCreateActiveCart(
            SQLiteDatabase database,
            int userId
    ) {

        String selectSql =
                "SELECT CartID " +
                        "FROM Cart " +
                        "WHERE UserID = ? " +
                        "AND Status = 'ACTIVE' " +
                        "ORDER BY CartID DESC " +
                        "LIMIT 1";

        try (
                Cursor cursor =
                        database.rawQuery(
                                selectSql,
                                new String[]{
                                        String.valueOf(userId)
                                }
                        )
        ) {

            if (cursor.moveToFirst()) {

                return cursor.getInt(
                        cursor.getColumnIndexOrThrow(
                                "CartID"
                        )
                );
            }
        }

        /*
         * Chưa có giỏ ACTIVE:
         * thực hiện INSERT trực tiếp.
         */
        String insertSql =
                "INSERT INTO Cart (" +
                        "UserID, " +
                        "Status, " +
                        "CreatedAt, " +
                        "UpdatedAt" +
                        ") VALUES (" +
                        "?, " +
                        "'ACTIVE', " +
                        "datetime('now', 'localtime'), " +
                        "datetime('now', 'localtime')" +
                        ")";

        SQLiteStatement statement =
                database.compileStatement(
                        insertSql
                );

        try {
            statement.bindLong(
                    1,
                    userId
            );

            long cartId =
                    statement.executeInsert();

            return (int) cartId;

        } finally {
            statement.close();
        }
    }

    private static CartItemSnapshot getCartItem(
            SQLiteDatabase database,
            int cartId,
            int productId
    ) {

        String sql =
                "SELECT CartItemID, Quantity " +
                        "FROM CartItem " +
                        "WHERE CartID = ? " +
                        "AND ProductID = ? " +
                        "LIMIT 1";

        try (
                Cursor cursor =
                        database.rawQuery(
                                sql,
                                new String[]{
                                        String.valueOf(cartId),
                                        String.valueOf(productId)
                                }
                        )
        ) {

            if (!cursor.moveToFirst()) {
                return null;
            }

            return new CartItemSnapshot(
                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    "CartItemID"
                            )
                    ),

                    cursor.getInt(
                            cursor.getColumnIndexOrThrow(
                                    "Quantity"
                            )
                    )
            );
        }
    }

    private static void insertCartItem(
            SQLiteDatabase database,
            int cartId,
            int productId,
            int quantity,
            double unitPrice
    ) {

        String insertSql =
                "INSERT INTO CartItem (" +
                        "CartID, " +
                        "ProductID, " +
                        "Quantity, " +
                        "UnitPrice, " +
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
                        cartId,
                        productId,
                        quantity,
                        unitPrice
                }
        );
    }

    private static void updateCartItem(
            SQLiteDatabase database,
            int cartItemId,
            int quantity,
            double unitPrice
    ) {

        String updateSql =
                "UPDATE CartItem " +
                        "SET " +
                        "Quantity = ?, " +
                        "UnitPrice = ?, " +
                        "UpdatedAt = " +
                        "datetime('now', 'localtime') " +
                        "WHERE CartItemID = ?";

        database.execSQL(
                updateSql,
                new Object[]{
                        quantity,
                        unitPrice,
                        cartItemId
                }
        );
    }

    private static void touchCart(
            SQLiteDatabase database,
            int cartId
    ) {

        String updateSql =
                "UPDATE Cart " +
                        "SET UpdatedAt = " +
                        "datetime('now', 'localtime') " +
                        "WHERE CartID = ?";

        database.execSQL(
                updateSql,
                new Object[]{cartId}
        );
    }

    private static final class ProductSnapshot {

        private final double price;
        private final int stockQuantity;

        private ProductSnapshot(
                double price,
                int stockQuantity
        ) {
            this.price = price;
            this.stockQuantity = stockQuantity;
        }
    }

    private static final class CartItemSnapshot {

        private final int cartItemId;
        private final int quantity;

        private CartItemSnapshot(
                int cartItemId,
                int quantity
        ) {
            this.cartItemId = cartItemId;
            this.quantity = quantity;
        }
    }
}