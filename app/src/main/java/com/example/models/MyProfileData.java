package com.example.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MyProfileData {

    private final boolean validCustomer;

    private final int totalOrders;
    private final int deliveredOrders;
    private final double totalSpent;

    private final List<String> comments;

    private MyProfileData(
            boolean validCustomer,
            int totalOrders,
            int deliveredOrders,
            double totalSpent,
            List<String> comments
    ) {
        this.validCustomer = validCustomer;
        this.totalOrders = totalOrders;
        this.deliveredOrders = deliveredOrders;
        this.totalSpent = totalSpent;

        this.comments =
                comments == null
                        ? Collections.emptyList()
                        : Collections.unmodifiableList(
                        new ArrayList<>(comments)
                );
    }

    public static MyProfileData success(
            int totalOrders,
            int deliveredOrders,
            double totalSpent,
            List<String> comments
    ) {
        return new MyProfileData(
                true,
                totalOrders,
                deliveredOrders,
                totalSpent,
                comments
        );
    }

    public static MyProfileData invalidCustomer() {
        return new MyProfileData(
                false,
                0,
                0,
                0,
                Collections.emptyList()
        );
    }

    public boolean isValidCustomer() {
        return validCustomer;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public int getDeliveredOrders() {
        return deliveredOrders;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public List<String> getComments() {
        return comments;
    }

    public int getCommentCount() {
        return comments.size();
    }
}