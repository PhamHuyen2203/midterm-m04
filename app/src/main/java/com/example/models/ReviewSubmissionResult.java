package com.example.models;

public final class ReviewSubmissionResult {

    public enum Status {
        INSERTED,
        ALREADY_REVIEWED,
        INVALID_USER,
        PRODUCT_NOT_FOUND,
        INVALID_RATING,
        EMPTY_COMMENT
    }

    private final Status status;
    private final double averageRating;
    private final int ratingCount;

    private ReviewSubmissionResult(
            Status status,
            double averageRating,
            int ratingCount
    ) {
        this.status = status;
        this.averageRating = averageRating;
        this.ratingCount = ratingCount;
    }

    public static ReviewSubmissionResult inserted(
            double averageRating,
            int ratingCount
    ) {
        return new ReviewSubmissionResult(
                Status.INSERTED,
                averageRating,
                ratingCount
        );
    }

    public static ReviewSubmissionResult alreadyReviewed() {
        return new ReviewSubmissionResult(
                Status.ALREADY_REVIEWED,
                0,
                0
        );
    }

    public static ReviewSubmissionResult invalidUser() {
        return new ReviewSubmissionResult(
                Status.INVALID_USER,
                0,
                0
        );
    }

    public static ReviewSubmissionResult productNotFound() {
        return new ReviewSubmissionResult(
                Status.PRODUCT_NOT_FOUND,
                0,
                0
        );
    }

    public static ReviewSubmissionResult invalidRating() {
        return new ReviewSubmissionResult(
                Status.INVALID_RATING,
                0,
                0
        );
    }

    public static ReviewSubmissionResult emptyComment() {
        return new ReviewSubmissionResult(
                Status.EMPTY_COMMENT,
                0,
                0
        );
    }

    public Status getStatus() {
        return status;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public boolean isSuccessful() {
        return status == Status.INSERTED;
    }
}