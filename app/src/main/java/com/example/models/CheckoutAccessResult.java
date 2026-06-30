package com.example.models;

public final class CheckoutAccessResult {

    public enum Status {
        ALLOWED,
        ACCOUNT_LOCKED,
        USER_NOT_FOUND,
        INVALID_ROLE
    }

    private final Status status;

    private CheckoutAccessResult(
            Status status
    ) {
        this.status = status;
    }

    public static CheckoutAccessResult allowed() {

        return new CheckoutAccessResult(
                Status.ALLOWED
        );
    }

    public static CheckoutAccessResult accountLocked() {

        return new CheckoutAccessResult(
                Status.ACCOUNT_LOCKED
        );
    }

    public static CheckoutAccessResult userNotFound() {

        return new CheckoutAccessResult(
                Status.USER_NOT_FOUND
        );
    }

    public static CheckoutAccessResult invalidRole() {

        return new CheckoutAccessResult(
                Status.INVALID_ROLE
        );
    }

    public Status getStatus() {
        return status;
    }

    public boolean isAllowed() {
        return status == Status.ALLOWED;
    }
}