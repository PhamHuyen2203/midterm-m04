package com.example.models;

public final class LoginResult {

    public enum Status {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_PASSWORD,
        ACCOUNT_LOCKED
    }

    private final Status status;
    private final int userId;
    private final String fullName;
    private final String role;
    private final int failedLoginCount;
    private final int remainingAttempts;

    private LoginResult(
            Status status,
            int userId,
            String fullName,
            String role,
            int failedLoginCount,
            int remainingAttempts
    ) {
        this.status = status;
        this.userId = userId;
        this.fullName = fullName;
        this.role = role;
        this.failedLoginCount = failedLoginCount;
        this.remainingAttempts = remainingAttempts;
    }

    public static LoginResult success(
            int userId,
            String fullName,
            String role
    ) {
        return new LoginResult(
                Status.SUCCESS,
                userId,
                fullName,
                role,
                0,
                3
        );
    }

    public static LoginResult userNotFound() {
        return new LoginResult(
                Status.USER_NOT_FOUND,
                -1,
                null,
                null,
                0,
                0
        );
    }

    public static LoginResult wrongPassword(
            int failedLoginCount,
            int remainingAttempts
    ) {
        return new LoginResult(
                Status.WRONG_PASSWORD,
                -1,
                null,
                null,
                failedLoginCount,
                remainingAttempts
        );
    }

    public static LoginResult accountLocked(
            int failedLoginCount
    ) {
        return new LoginResult(
                Status.ACCOUNT_LOCKED,
                -1,
                null,
                null,
                failedLoginCount,
                0
        );
    }

    public Status getStatus() {
        return status;
    }

    public int getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public int getFailedLoginCount() {
        return failedLoginCount;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isCustomer() {
        return "CUSTOMER".equalsIgnoreCase(role);
    }
}