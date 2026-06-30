package com.example.mcommercemobile04;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dals.ReviewDAO;
import com.example.models.ReviewSubmissionResult;
import com.example.utils.SessionManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReviewActivity
        extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID =
            "extra_product_id";

    public static final String EXTRA_PRODUCT_NAME =
            "extra_product_name";

    public static final String EXTRA_AVERAGE_RATING =
            "extra_average_rating";

    public static final String EXTRA_RATING_COUNT =
            "extra_rating_count";

    private static final String LOG_TAG =
            "ReviewActivity";

    private View rootReview;
    private MaterialToolbar toolbarReview;

    private TextView textViewReviewProductName;
    private TextView textViewReviewCurrentRating;
    private TextView textViewReviewRatingError;

    private RatingBar ratingBarReview;

    private TextInputLayout
            textInputLayoutReviewComment;

    private TextInputEditText
            editTextReviewComment;

    private MaterialButton buttonSubmitReview;

    private int productId;

    private boolean submissionRunning;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.isLoggedIn(this)
                || !SessionManager.isCustomer(this)
                || SessionManager.getUserId(this) <= 0) {

            openLoginScreen();
            return;
        }

        productId =
                getIntent().getIntExtra(
                        EXTRA_PRODUCT_ID,
                        -1
                );

        if (productId <= 0) {
            finish();
            return;
        }

        setContentView(
                R.layout.activity_review
        );

        mapViews();
        setupToolbar();
        displayProductInformation();
        setEvents();
    }

    private void mapViews() {

        rootReview =
                findViewById(
                        R.id.rootReview
                );

        toolbarReview =
                findViewById(
                        R.id.toolbarReview
                );

        textViewReviewProductName =
                findViewById(
                        R.id.textViewReviewProductName
                );

        textViewReviewCurrentRating =
                findViewById(
                        R.id.textViewReviewCurrentRating
                );

        textViewReviewRatingError =
                findViewById(
                        R.id.textViewReviewRatingError
                );

        ratingBarReview =
                findViewById(
                        R.id.ratingBarReview
                );

        textInputLayoutReviewComment =
                findViewById(
                        R.id.textInputLayoutReviewComment
                );

        editTextReviewComment =
                findViewById(
                        R.id.editTextReviewComment
                );

        buttonSubmitReview =
                findViewById(
                        R.id.buttonSubmitReview
                );
    }

    private void setupToolbar() {

        toolbarReview.setNavigationContentDescription(
                R.string.navigation_back
        );

        toolbarReview.setNavigationOnClickListener(
                view -> finish()
        );
    }

    private void displayProductInformation() {

        String productName =
                getIntent().getStringExtra(
                        EXTRA_PRODUCT_NAME
                );

        double averageRating =
                getIntent().getDoubleExtra(
                        EXTRA_AVERAGE_RATING,
                        0
                );

        int ratingCount =
                getIntent().getIntExtra(
                        EXTRA_RATING_COUNT,
                        0
                );

        textViewReviewProductName.setText(
                productName == null
                        ? ""
                        : productName
        );

        textViewReviewCurrentRating.setText(
                getString(
                        R.string.review_current_rating_format,
                        averageRating,
                        ratingCount
                )
        );
    }

    private void setEvents() {

        buttonSubmitReview.setOnClickListener(
                view -> submitReview()
        );

        ratingBarReview.setOnRatingBarChangeListener(
                (ratingBar, rating, fromUser) -> {

                    if (rating >= 1) {

                        textViewReviewRatingError
                                .setVisibility(View.GONE);
                    }
                }
        );
    }

    private void submitReview() {

        if (submissionRunning) {
            return;
        }

        textViewReviewRatingError.setVisibility(
                View.GONE
        );

        textInputLayoutReviewComment.setError(
                null
        );

        int rating =
                Math.round(
                        ratingBarReview.getRating()
                );

        String comment =
                getCommentText().trim();

        boolean isValid = true;

        if (rating < 1 || rating > 5) {

            textViewReviewRatingError.setVisibility(
                    View.VISIBLE
            );

            isValid = false;
        }

        if (comment.isEmpty()) {

            textInputLayoutReviewComment.setError(
                    getString(
                            R.string.review_comment_required
                    )
            );

            editTextReviewComment.requestFocus();

            isValid = false;

        } else if (comment.length() > 500) {

            textInputLayoutReviewComment.setError(
                    getString(
                            R.string.review_comment_too_long
                    )
            );

            editTextReviewComment.requestFocus();

            isValid = false;
        }

        if (!isValid) {
            return;
        }

        submissionRunning = true;
        buttonSubmitReview.setEnabled(false);

        int userId =
                SessionManager.getUserId(this);

        executorService.execute(() -> {

            try {
                ReviewSubmissionResult result =
                        ReviewDAO.submitReview(
                                getApplicationContext(),
                                userId,
                                productId,
                                rating,
                                comment
                        );

                runOnUiThread(() -> {

                    submissionRunning = false;
                    buttonSubmitReview.setEnabled(true);

                    handleSubmissionResult(result);
                });

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Unable to submit product review.",
                        exception
                );

                runOnUiThread(() -> {

                    submissionRunning = false;
                    buttonSubmitReview.setEnabled(true);

                    Snackbar.make(
                            rootReview,
                            R.string.review_submit_error,
                            Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void handleSubmissionResult(
            ReviewSubmissionResult result
    ) {

        switch (result.getStatus()) {

            case INSERTED:

                Toast.makeText(
                        this,
                        getString(
                                R.string.review_insert_success,
                                result.getAverageRating()
                        ),
                        Toast.LENGTH_LONG
                ).show();

                /*
                 * Báo cho ProductSearchActivity biết
                 * cần tải lại điểm đánh giá.
                 */
                setResult(RESULT_OK);
                finish();
                break;

            case ALREADY_REVIEWED:

                Snackbar.make(
                        rootReview,
                        R.string.review_already_exists,
                        Snackbar.LENGTH_LONG
                ).show();

                break;

            case INVALID_USER:

                SessionManager.clearSession(this);

                Toast.makeText(
                        this,
                        R.string.review_invalid_session,
                        Toast.LENGTH_LONG
                ).show();

                openLoginScreen();
                break;

            case PRODUCT_NOT_FOUND:

                Snackbar.make(
                        rootReview,
                        R.string.review_product_unavailable,
                        Snackbar.LENGTH_LONG
                ).show();

                break;

            case INVALID_RATING:

                textViewReviewRatingError.setVisibility(
                        View.VISIBLE
                );

                break;

            case EMPTY_COMMENT:

                textInputLayoutReviewComment.setError(
                        getString(
                                R.string.review_comment_required
                        )
                );

                editTextReviewComment.requestFocus();
                break;
        }
    }

    private String getCommentText() {

        Editable editable =
                editTextReviewComment.getText();

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