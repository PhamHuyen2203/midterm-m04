package com.example.models;

public final class CartActionResult {

    public enum Status {
        INSERTED,
        UPDATED,
        OUT_OF_STOCK,
        PRODUCT_NOT_FOUND,
        INVALID_USER
    }

    private final Status status;
    private final int currentQuantity;

    private CartActionResult(
            Status status,
            int currentQuantity
    ) {
        this.status = status;
        this.currentQuantity = currentQuantity;
    }

    public static CartActionResult inserted(
            int currentQuantity
    ) {
        return new CartActionResult(
                Status.INSERTED,
                currentQuantity
        );
    }

    public static CartActionResult updated(
            int currentQuantity
    ) {
        return new CartActionResult(
                Status.UPDATED,
                currentQuantity
        );
    }

    public static CartActionResult outOfStock() {
        return new CartActionResult(
                Status.OUT_OF_STOCK,
                0
        );
    }

    public static CartActionResult productNotFound() {
        return new CartActionResult(
                Status.PRODUCT_NOT_FOUND,
                0
        );
    }

    public static CartActionResult invalidUser() {
        return new CartActionResult(
                Status.INVALID_USER,
                0
        );
    }

    public Status getStatus() {
        return status;
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public boolean isSuccessful() {
        return status == Status.INSERTED
                || status == Status.UPDATED;
    }
}