package com.example.mcommercemobile04;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.dals.UserDAO;
import com.example.models.CheckoutAccessResult;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.adapters.ProductAdapter;
import com.example.dals.CartDAO;
import com.example.dals.ProductDAO;
import com.example.models.CartActionResult;
import com.example.models.Product;
import com.example.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductSearchActivity
        extends AppCompatActivity
        implements ProductAdapter.OnProductActionListener {

    private static final String LOG_TAG =
            "ProductSearchActivity";

    private View rootProductSearch;

    private MaterialToolbar toolbarProducts;

    private TextInputLayout
            textInputLayoutMinPrice;

    private TextInputLayout
            textInputLayoutMaxPrice;

    private TextInputEditText
            editTextProductName;

    private TextInputEditText
            editTextMinPrice;

    private TextInputEditText
            editTextMaxPrice;

    private MaterialButton
            buttonSearchProduct;

    private MaterialButton
            buttonClearProductFilter;
    private MaterialButton buttonCheckout;
    private TextView
            textViewProductResultCount;

    private TextView
            textViewCartSummary;

    private TextView
            textViewEmptyProducts;

    private ProgressBar
            progressBarProducts;

    private RecyclerView
            recyclerViewProducts;

    private ProductAdapter
            productAdapter;

    private boolean cartOperationRunning;
    private boolean checkoutAccessChecking;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<Intent>
            reviewActivityLauncher =

            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),

                    result -> {

                        if (result.getResultCode()
                                == RESULT_OK) {

                            /*
                             * Đánh giá đã được INSERT
                             * và điểm trung bình đã cập nhật.
                             * Tải lại sản phẩm để hiện điểm mới.
                             */
                            performSearch();
                        }
                    }
            );

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        /*
         * Chỉ khách hàng đã đăng nhập mới được
         * sử dụng chức năng mua sản phẩm.
         */
        if (!SessionManager.isLoggedIn(this)
                || !SessionManager.isCustomer(this)) {

            openLoginScreen();
            return;
        }

        setContentView(
                R.layout.activity_product_search
        );

        mapViews();
        setupToolbar();
        setupRecyclerView();
        setEvents();

        performSearch();
        refreshCartSummary();
    }

    private void mapViews() {

        rootProductSearch =
                findViewById(
                        R.id.rootProductSearch
                );

        toolbarProducts =
                findViewById(
                        R.id.toolbarProducts
                );

        textInputLayoutMinPrice =
                findViewById(
                        R.id.textInputLayoutMinPrice
                );

        textInputLayoutMaxPrice =
                findViewById(
                        R.id.textInputLayoutMaxPrice
                );

        editTextProductName =
                findViewById(
                        R.id.editTextProductName
                );

        editTextMinPrice =
                findViewById(
                        R.id.editTextMinPrice
                );

        editTextMaxPrice =
                findViewById(
                        R.id.editTextMaxPrice
                );

        buttonSearchProduct =
                findViewById(
                        R.id.buttonSearchProduct
                );

        buttonClearProductFilter =
                findViewById(
                        R.id.buttonClearProductFilter
                );

        buttonCheckout =
                findViewById(
                        R.id.buttonCheckout
                );

        textViewProductResultCount =
                findViewById(
                        R.id.textViewProductResultCount
                );

        textViewCartSummary =
                findViewById(
                        R.id.textViewCartSummary
                );

        textViewEmptyProducts =
                findViewById(
                        R.id.textViewEmptyProducts
                );

        progressBarProducts =
                findViewById(
                        R.id.progressBarProducts
                );

        recyclerViewProducts =
                findViewById(
                        R.id.recyclerViewProducts
                );
    }

    private void setupToolbar() {

        toolbarProducts
                .setNavigationContentDescription(
                        R.string.navigation_back
                );

        toolbarProducts.setNavigationOnClickListener(
                view -> finish()
        );
    }

    private void setupRecyclerView() {

        productAdapter =
                new ProductAdapter(this);

        recyclerViewProducts.setLayoutManager(
                new LinearLayoutManager(this)
        );

        recyclerViewProducts.setAdapter(
                productAdapter
        );

        recyclerViewProducts.setHasFixedSize(
                false
        );
    }

    private void setEvents() {

        buttonSearchProduct.setOnClickListener(
                view -> performSearch()
        );

        buttonClearProductFilter.setOnClickListener(
                view -> clearFilters()
        );

        buttonCheckout.setOnClickListener(
                view -> handleCheckoutClick()
        );

        editTextMaxPrice.setOnEditorActionListener(
                (textView, actionId, event) -> {

                    if (actionId
                            == EditorInfo.IME_ACTION_SEARCH) {

                        performSearch();
                        return true;
                    }

                    return false;
                }
        );
    }

    private void clearFilters() {

        editTextProductName.setText(null);
        editTextMinPrice.setText(null);
        editTextMaxPrice.setText(null);

        textInputLayoutMinPrice.setError(null);
        textInputLayoutMaxPrice.setError(null);

        performSearch();
    }

    /**
     * Đọc bộ lọc và truy vấn sản phẩm.
     */
    private void performSearch() {

        textInputLayoutMinPrice.setError(null);
        textInputLayoutMaxPrice.setError(null);

        String keyword =
                getInputText(
                        editTextProductName
                ).trim();

        Double minPrice;

        try {
            minPrice =
                    parseOptionalPrice(
                            editTextMinPrice
                    );

        } catch (NumberFormatException exception) {

            textInputLayoutMinPrice.setError(
                    getString(
                            R.string.product_invalid_price
                    )
            );

            editTextMinPrice.requestFocus();
            return;
        }

        Double maxPrice;

        try {
            maxPrice =
                    parseOptionalPrice(
                            editTextMaxPrice
                    );

        } catch (NumberFormatException exception) {

            textInputLayoutMaxPrice.setError(
                    getString(
                            R.string.product_invalid_price
                    )
            );

            editTextMaxPrice.requestFocus();
            return;
        }

        if (minPrice != null
                && maxPrice != null
                && maxPrice < minPrice) {

            textInputLayoutMaxPrice.setError(
                    getString(
                            R.string
                                    .product_invalid_price_range
                    )
            );

            editTextMaxPrice.requestFocus();
            return;
        }

        showLoading(true);

        executorService.execute(() -> {

            try {
                List<Product> products =
                        ProductDAO.searchProducts(
                                getApplicationContext(),
                                keyword,
                                minPrice,
                                maxPrice
                        );

                runOnUiThread(() -> {

                    showLoading(false);
                    displayProducts(products);
                });

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Unable to search products.",
                        exception
                );

                runOnUiThread(() -> {

                    showLoading(false);

                    Snackbar.make(
                            rootProductSearch,
                            R.string.product_search_error,
                            Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void displayProducts(
            List<Product> products
    ) {

        productAdapter.submitList(products);

        int productCount =
                products == null
                        ? 0
                        : products.size();

        textViewProductResultCount.setText(
                getString(
                        R.string.product_result_count,
                        productCount
                )
        );

        boolean isEmpty =
                productCount == 0;

        textViewEmptyProducts.setVisibility(
                isEmpty
                        ? View.VISIBLE
                        : View.GONE
        );

        recyclerViewProducts.setVisibility(
                isEmpty
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    /**
     * Sự kiện từ ProductAdapter.
     */
    @Override
    public void onAddToCart(
            Product product
    ) {

        if (cartOperationRunning) {
            return;
        }

        cartOperationRunning = true;

        int userId =
                SessionManager.getUserId(this);

        executorService.execute(() -> {

            try {
                CartActionResult result =
                        CartDAO.addProductToCart(
                                getApplicationContext(),
                                userId,
                                product.getProductId(),
                                1
                        );

                int totalQuantity = -1;

                if (result.isSuccessful()) {

                    totalQuantity =
                            CartDAO.getTotalQuantity(
                                    getApplicationContext(),
                                    userId
                            );
                }

                int finalTotalQuantity =
                        totalQuantity;

                runOnUiThread(() -> {

                    cartOperationRunning = false;

                    handleCartResult(
                            result,
                            finalTotalQuantity
                    );
                });

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Unable to update cart.",
                        exception
                );

                runOnUiThread(() -> {

                    cartOperationRunning = false;

                    Snackbar.make(
                            rootProductSearch,
                            R.string.cart_update_error,
                            Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    @Override
    public void onReviewProduct(
            Product product
    ) {

        Intent intent =
                new Intent(
                        this,
                        ReviewActivity.class
                );

        intent.putExtra(
                ReviewActivity.EXTRA_PRODUCT_ID,
                product.getProductId()
        );

        intent.putExtra(
                ReviewActivity.EXTRA_PRODUCT_NAME,
                product.getProductName()
        );

        intent.putExtra(
                ReviewActivity.EXTRA_AVERAGE_RATING,
                product.getAverageRating()
        );

        intent.putExtra(
                ReviewActivity.EXTRA_RATING_COUNT,
                product.getRatingCount()
        );

        reviewActivityLauncher.launch(intent);
    }

    private void handleCartResult(
            CartActionResult result,
            int totalQuantity
    ) {

        int duration =
                Snackbar.LENGTH_LONG;

        String message;

        switch (result.getStatus()) {

            case INSERTED:

                message =
                        getString(
                                R.string.cart_insert_success
                        );

                break;

            case UPDATED:

                message =
                        getString(
                                R.string.cart_update_success,
                                result.getCurrentQuantity()
                        );

                break;

            case OUT_OF_STOCK:

                message =
                        getString(
                                R.string.cart_out_of_stock
                        );

                break;

            case PRODUCT_NOT_FOUND:

                message =
                        getString(
                                R.string
                                        .cart_product_unavailable
                        );

                break;

            case INVALID_USER:

                message =
                        getString(
                                R.string.cart_invalid_session
                        );

                SessionManager.clearSession(this);

                Snackbar.make(
                        rootProductSearch,
                        message,
                        duration
                ).show();

                openLoginScreen();
                return;

            default:

                message =
                        getString(
                                R.string.cart_update_error
                        );

                break;
        }

        if (totalQuantity >= 0) {

            updateCartSummary(
                    totalQuantity
            );
        }

        Snackbar.make(
                rootProductSearch,
                message,
                duration
        ).show();
    }

    private void refreshCartSummary() {

        int userId =
                SessionManager.getUserId(this);

        executorService.execute(() -> {

            try {
                int totalQuantity =
                        CartDAO.getTotalQuantity(
                                getApplicationContext(),
                                userId
                        );

                runOnUiThread(() ->
                        updateCartSummary(
                                totalQuantity
                        )
                );

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Unable to load cart summary.",
                        exception
                );
            }
        });
    }

    private void updateCartSummary(
            int totalQuantity
    ) {

        textViewCartSummary.setText(
                getString(
                        R.string.cart_summary_format,
                        totalQuantity
                )
        );
    }

    private void showLoading(
            boolean isLoading
    ) {

        progressBarProducts.setVisibility(
                isLoading
                        ? View.VISIBLE
                        : View.GONE
        );

        buttonSearchProduct.setEnabled(
                !isLoading
        );

        buttonClearProductFilter.setEnabled(
                !isLoading
        );
    }
    /**
     * Xử lý khi khách hàng nhấn nút Thanh toán.
     */
    private void handleCheckoutClick() {

        /*
         * Bước 1:
         * Kiểm tra phiên đăng nhập trước.
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

        if (checkoutAccessChecking) {
            return;
        }

        checkoutAccessChecking = true;
        buttonCheckout.setEnabled(false);

        int userId =
                SessionManager.getUserId(this);

        /*
         * Bước 2:
         * Đọc lại trạng thái tài khoản từ SQLite.
         */
        executorService.execute(() -> {

            try {
                CheckoutAccessResult result =
                        UserDAO.checkCheckoutAccess(
                                getApplicationContext(),
                                userId
                        );

                runOnUiThread(() -> {

                    checkoutAccessChecking = false;
                    buttonCheckout.setEnabled(true);

                    handleCheckoutAccessResult(result);
                });

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Unable to check checkout access.",
                        exception
                );

                runOnUiThread(() -> {

                    checkoutAccessChecking = false;
                    buttonCheckout.setEnabled(true);

                    Snackbar.make(
                            rootProductSearch,
                            R.string.checkout_access_error,
                            Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    /**
     * Xử lý kết quả kiểm tra quyền Checkout.
     */
    private void handleCheckoutAccessResult(
            CheckoutAccessResult result
    ) {

        switch (result.getStatus()) {

            case ALLOWED:
                openCheckoutScreen();
                break;

            case ACCOUNT_LOCKED:

                /*
                 * Không giữ phiên của tài khoản đã khóa.
                 */
                SessionManager.clearSession(this);

                showLockedAccountDialog();
                break;

            case USER_NOT_FOUND:
            case INVALID_ROLE:

                SessionManager.clearSession(this);

                Toast.makeText(
                        this,
                        R.string.checkout_invalid_session,
                        Toast.LENGTH_LONG
                ).show();

                openLoginScreen();
                break;
        }
    }

    /**
     * Hiển thị thông báo chặn khi tài khoản bị khóa.
     */
    private void showLockedAccountDialog() {

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.checkout_locked_title
                )
                .setMessage(
                        R.string.checkout_locked_message
                )
                .setCancelable(false)
                .setPositiveButton(
                        R.string.checkout_locked_action,
                        (dialog, which) ->
                                openLoginScreen()
                )
                .show();
    }

    /**
     * Chuyển đến màn hình Checkout khi tài khoản hợp lệ.
     */
    private void openCheckoutScreen() {

        Intent intent =
                new Intent(
                        this,
                        CheckoutActivity.class
                );

        startActivity(intent);
    }

    private Double parseOptionalPrice(
            TextInputEditText editText
    ) throws NumberFormatException {

        String rawValue =
                getInputText(editText).trim();

        if (rawValue.isEmpty()) {
            return null;
        }

        /*
         * Cho phép người dùng nhập:
         * 5000000
         * 5.000.000
         * 5,000,000
         */
        String normalizedValue =
                rawValue
                        .replace(" ", "")
                        .replace(".", "")
                        .replace(",", "");

        if (normalizedValue.isEmpty()) {
            throw new NumberFormatException();
        }

        double value =
                Double.parseDouble(
                        normalizedValue
                );

        if (value < 0) {
            throw new NumberFormatException();
        }

        return value;
    }

    private String getInputText(
            TextInputEditText editText
    ) {

        Editable editable =
                editText.getText();

        return editable == null
                ? ""
                : editable.toString();
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