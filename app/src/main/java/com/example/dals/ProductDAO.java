package com.example.dals;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.models.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ProductDAO {

    private ProductDAO() {
        // Không cho phép khởi tạo đối tượng.
    }

    /**
     * Tìm sản phẩm theo:
     * - Tên gần đúng bằng LIKE
     * - Giá tối thiểu
     * - Giá tối đa
     */
    public static List<Product> searchProducts(
            Context context,
            String keyword,
            Double minPrice,
            Double maxPrice
    ) throws IOException {

        SQLiteDatabase database = null;

        List<Product> products =
                new ArrayList<>();

        try {
            database =
                    DatabaseHelper.openDatabase(context);

            StringBuilder sql =
                    new StringBuilder();

            List<String> arguments =
                    new ArrayList<>();

            sql.append(
                    "SELECT " +
                            "p.ProductID, " +
                            "p.CategoryID, " +
                            "c.CategoryName, " +
                            "p.ProductName, " +
                            "p.Description, " +
                            "p.Price, " +
                            "p.StockQuantity, " +
                            "p.AverageRating, " +
                            "p.RatingCount " +
                            "FROM Product AS p " +
                            "INNER JOIN Category AS c " +
                            "ON p.CategoryID = c.CategoryID " +
                            "WHERE p.IsActive = 1 "
            );

            /*
             * Tìm tên gần đúng:
             * Ví dụ nhập "phone" sẽ tìm "%phone%".
             */
            if (keyword != null
                    && !keyword.trim().isEmpty()) {

                sql.append(
                        "AND p.ProductName " +
                                "LIKE ? ESCAPE '\\' " +
                                "COLLATE NOCASE "
                );

                arguments.add(
                        "%"
                                + escapeLikeValue(
                                keyword.trim()
                        )
                                + "%"
                );
            }

            if (minPrice != null) {

                sql.append(
                        "AND p.Price >= ? "
                );

                arguments.add(
                        String.valueOf(minPrice)
                );
            }

            if (maxPrice != null) {

                sql.append(
                        "AND p.Price <= ? "
                );

                arguments.add(
                        String.valueOf(maxPrice)
                );
            }

            sql.append(
                    "ORDER BY " +
                            "p.ProductName COLLATE NOCASE ASC"
            );

            try (
                    Cursor cursor =
                            database.rawQuery(
                                    sql.toString(),
                                    arguments.toArray(
                                            new String[0]
                                    )
                            )
            ) {

                while (cursor.moveToNext()) {

                    Product product =
                            new Product(
                                    cursor.getInt(
                                            cursor.getColumnIndexOrThrow(
                                                    "ProductID"
                                            )
                                    ),

                                    cursor.getInt(
                                            cursor.getColumnIndexOrThrow(
                                                    "CategoryID"
                                            )
                                    ),

                                    cursor.getString(
                                            cursor.getColumnIndexOrThrow(
                                                    "CategoryName"
                                            )
                                    ),

                                    cursor.getString(
                                            cursor.getColumnIndexOrThrow(
                                                    "ProductName"
                                            )
                                    ),

                                    cursor.getString(
                                            cursor.getColumnIndexOrThrow(
                                                    "Description"
                                            )
                                    ),

                                    cursor.getDouble(
                                            cursor.getColumnIndexOrThrow(
                                                    "Price"
                                            )
                                    ),

                                    cursor.getInt(
                                            cursor.getColumnIndexOrThrow(
                                                    "StockQuantity"
                                            )
                                    ),

                                    cursor.getDouble(
                                            cursor.getColumnIndexOrThrow(
                                                    "AverageRating"
                                            )
                                    ),

                                    cursor.getInt(
                                            cursor.getColumnIndexOrThrow(
                                                    "RatingCount"
                                            )
                                    )
                            );

                    products.add(product);
                }
            }

            return products;

        } finally {

            if (database != null
                    && database.isOpen()) {

                database.close();
            }
        }
    }

    /**
     * Không cho ký tự %, _ người dùng nhập trở thành
     * ký tự đại diện của câu lệnh LIKE.
     */
    private static String escapeLikeValue(
            String value
    ) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}