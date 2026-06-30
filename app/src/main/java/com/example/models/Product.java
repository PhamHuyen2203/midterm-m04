package com.example.models;

public final class Product {

    private final int productId;
    private final int categoryId;
    private final String categoryName;
    private final String productName;
    private final String description;
    private final double price;
    private final int stockQuantity;
    private final double averageRating;
    private final int ratingCount;

    public Product(
            int productId,
            int categoryId,
            String categoryName,
            String productName,
            String description,
            double price,
            int stockQuantity,
            double averageRating,
            int ratingCount
    ) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.productName = productName;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public int getProductId() {
        return productId;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getProductName() {
        return productName;
    }

    public String getDescription() {
        return description;
    }

    public double getPrice() {
        return price;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }
}