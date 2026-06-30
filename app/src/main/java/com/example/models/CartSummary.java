package com.example.models;

public final class CartSummary {

    private final int productLineCount;
    private final int totalQuantity;
    private final double totalAmount;

    public CartSummary(
            int productLineCount,
            int totalQuantity,
            double totalAmount
    ) {
        this.productLineCount = productLineCount;
        this.totalQuantity = totalQuantity;
        this.totalAmount = totalAmount;
    }

    public static CartSummary empty() {

        return new CartSummary(
                0,
                0,
                0
        );
    }

    public int getProductLineCount() {
        return productLineCount;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public boolean isEmpty() {
        return totalQuantity <= 0;
    }
}