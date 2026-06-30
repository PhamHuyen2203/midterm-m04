package com.example.mcommercemobile04;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dals.CartDAO;
import com.example.models.CartSummary;
import com.example.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CheckoutActivity
        extends AppCompatActivity {

    private static final String LOG_TAG =
            "CheckoutActivity";

    private View rootCheckout;
    private MaterialToolbar toolbarCheckout;
    private ProgressBar progressBarCheckout;
    private ScrollView scrollViewCheckoutContent;

    private TextView textViewCheckoutCustomerName;
    private TextView textViewCheckoutProductLines;
    private TextView textViewCheckoutTotalQuantity;
    private TextView textViewCheckoutTotalAmount;
    private TextView textViewCheckoutEmpty;

    private MaterialButton buttonContinueShopping;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private final NumberFormat currencyFormatter =
            NumberFormat.getCurrencyInstance(
                    Locale.forLanguageTag("vi-VN")
            );

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        /*
         * Bảo vệ Activity trong trường hợp được mở
         * không thông qua nút Checkout.
         */
        if (!SessionManager.isLoggedIn(this)
                || !SessionManager.isCustomer(this)
                || SessionManager.getUserId(this) <= 0) {

            Toast.makeText(
                    this,
                    R.string.checkout_login_required,
                    Toast.LENGTH_LONG
            ).show();

            openLoginScreen();
            return;
        }

        setContentView(
                R.layout.activity_checkout
        );

        currencyFormatter.setMaximumFractionDigits(0);

        mapViews();
        setupToolbar();
        setEvents();

        loadCartSummary();
    }

    private void mapViews() {

        rootCheckout =
                findViewById(
                        R.id.rootCheckout
                );

        toolbarCheckout =
                findViewById(
                        R.id.toolbarCheckout
                );

        progressBarCheckout =
                findViewById(
                        R.id.progressBarCheckout
                );

        scrollViewCheckoutContent =
                findViewById(
                        R.id.scrollViewCheckoutContent
                );

        textViewCheckoutCustomerName =
                findViewById(
                        R.id.textViewCheckoutCustomerName
                );

        textViewCheckoutProductLines =
                findViewById(
                        R.id.textViewCheckoutProductLines
                );

        textViewCheckoutTotalQuantity =
                findViewById(
                        R.id.textViewCheckoutTotalQuantity
                );

        textViewCheckoutTotalAmount =
                findViewById(
                        R.id.textViewCheckoutTotalAmount
                );

        textViewCheckoutEmpty =
                findViewById(
                        R.id.textViewCheckoutEmpty
                );

        buttonContinueShopping =
                findViewById(
                        R.id.buttonContinueShopping
                );
    }

    private void setupToolbar() {

        toolbarCheckout
                .setNavigationContentDescription(
                        R.string.navigation_back
                );

        toolbarCheckout.setNavigationOnClickListener(
                view -> finish()
        );
    }

    private void setEvents() {

        buttonContinueShopping.setOnClickListener(
                view -> finish()
        );
    }

    /**
     * Đọc dữ liệu giỏ hàng từ SQLite.
     */
    private void loadCartSummary() {

        showLoading(true);

        int userId =
                SessionManager.getUserId(this);

        executorService.execute(() -> {

            try {
                CartSummary cartSummary =
                        CartDAO.getCartSummary(
                                getApplicationContext(),
                                userId
                        );

                runOnUiThread(() -> {

                    showLoading(false);
                    displayCartSummary(cartSummary);
                });

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Unable to load checkout summary.",
                        exception
                );

                runOnUiThread(() -> {

                    showLoading(false);

                    Snackbar.make(
                            rootCheckout,
                            R.string.checkout_load_error,
                            Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void displayCartSummary(
            CartSummary cartSummary
    ) {

        textViewCheckoutCustomerName.setText(
                SessionManager.getFullName(this)
        );

        textViewCheckoutProductLines.setText(
                getString(
                        R.string.checkout_product_lines_format,
                        cartSummary.getProductLineCount()
                )
        );

        textViewCheckoutTotalQuantity.setText(
                getString(
                        R.string.checkout_total_quantity_format,
                        cartSummary.getTotalQuantity()
                )
        );

        String formattedTotal =
                currencyFormatter.format(
                        cartSummary.getTotalAmount()
                );

        textViewCheckoutTotalAmount.setText(
                getString(
                        R.string.checkout_total_amount_format,
                        formattedTotal
                )
        );

        textViewCheckoutEmpty.setVisibility(
                cartSummary.isEmpty()
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private void showLoading(
            boolean isLoading
    ) {

        progressBarCheckout.setVisibility(
                isLoading
                        ? View.VISIBLE
                        : View.GONE
        );

        scrollViewCheckoutContent.setVisibility(
                isLoading
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private void openLoginScreen() {

        Intent intent =
                new Intent(
                        this,
                        LoginActivity.class
                );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {

        executorService.shutdownNow();

        super.onDestroy();
    }
}