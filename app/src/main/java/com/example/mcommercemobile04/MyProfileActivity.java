package com.example.mcommercemobile04;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dals.ProfileDAO;
import com.example.models.MyProfileData;
import com.example.models.WordFrequency;
import com.example.utils.SessionManager;
import com.example.utils.WordCloudProcessor;
import com.example.views.WordCloudView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyProfileActivity
        extends AppCompatActivity {

    private static final String LOG_TAG =
            "MyProfileActivity";

    private static final int MAXIMUM_CLOUD_WORDS =
            30;

    private static final int MAXIMUM_ACCESSIBILITY_WORDS =
            8;

    private View rootMyProfile;

    private MaterialToolbar toolbarMyProfile;

    private ProgressBar progressBarMyProfile;
    private ScrollView scrollViewMyProfile;

    private TextView textViewProfileName;
    private TextView textViewTotalOrdersValue;
    private TextView textViewDeliveredOrdersValue;
    private TextView textViewTotalSpentValue;
    private TextView textViewWordCloudCommentCount;
    private TextView textViewWordCloudEmpty;

    private WordCloudView wordCloudView;

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

        if (!SessionManager.isLoggedIn(this)
                || !SessionManager.isCustomer(this)
                || SessionManager.getUserId(this) <= 0) {

            openLoginScreen();
            return;
        }

        setContentView(
                R.layout.activity_my_profile
        );

        currencyFormatter.setMaximumFractionDigits(0);

        mapViews();
        setupToolbar();

        loadProfileData();
    }

    private void mapViews() {

        rootMyProfile =
                findViewById(
                        R.id.rootMyProfile
                );

        toolbarMyProfile =
                findViewById(
                        R.id.toolbarMyProfile
                );

        progressBarMyProfile =
                findViewById(
                        R.id.progressBarMyProfile
                );

        scrollViewMyProfile =
                findViewById(
                        R.id.scrollViewMyProfile
                );

        textViewProfileName =
                findViewById(
                        R.id.textViewProfileName
                );

        textViewTotalOrdersValue =
                findViewById(
                        R.id.textViewTotalOrdersValue
                );

        textViewDeliveredOrdersValue =
                findViewById(
                        R.id.textViewDeliveredOrdersValue
                );

        textViewTotalSpentValue =
                findViewById(
                        R.id.textViewTotalSpentValue
                );

        textViewWordCloudCommentCount =
                findViewById(
                        R.id.textViewWordCloudCommentCount
                );

        textViewWordCloudEmpty =
                findViewById(
                        R.id.textViewWordCloudEmpty
                );

        wordCloudView =
                findViewById(
                        R.id.wordCloudView
                );
    }

    private void setupToolbar() {

        toolbarMyProfile
                .setNavigationContentDescription(
                        R.string.navigation_back
                );

        toolbarMyProfile.setNavigationOnClickListener(
                view -> finish()
        );
    }

    /**
     * Truy vấn dữ liệu hồ sơ ngoài UI thread.
     */
    private void loadProfileData() {

        showLoading(true);

        int userId =
                SessionManager.getUserId(this);

        executorService.execute(() -> {

            try {
                MyProfileData profileData =
                        ProfileDAO.getMyProfileData(
                                getApplicationContext(),
                                userId
                        );

                runOnUiThread(() -> {

                    showLoading(false);

                    handleProfileData(
                            profileData
                    );
                });

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Unable to load personal profile.",
                        exception
                );

                runOnUiThread(() -> {

                    showLoading(false);

                    Snackbar.make(
                            rootMyProfile,
                            R.string.profile_load_error,
                            Snackbar.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void handleProfileData(
            MyProfileData profileData
    ) {

        if (!profileData.isValidCustomer()) {

            SessionManager.clearSession(this);

            Toast.makeText(
                    this,
                    R.string.profile_invalid_session,
                    Toast.LENGTH_LONG
            ).show();

            openLoginScreen();
            return;
        }

        displayStatistics(profileData);
        displayWordCloud(profileData);
    }

    private void displayStatistics(
            MyProfileData profileData
    ) {

        textViewProfileName.setText(
                SessionManager.getFullName(this)
        );

        textViewTotalOrdersValue.setText(
                getString(
                        R.string.profile_order_count_format,
                        profileData.getTotalOrders()
                )
        );

        textViewDeliveredOrdersValue.setText(
                getString(
                        R.string.profile_order_count_format,
                        profileData.getDeliveredOrders()
                )
        );

        String formattedTotalSpent =
                currencyFormatter.format(
                        profileData.getTotalSpent()
                );

        textViewTotalSpentValue.setText(
                getString(
                        R.string.profile_money_format,
                        formattedTotalSpent
                )
        );
    }

    private void displayWordCloud(
            MyProfileData profileData
    ) {

        List<WordFrequency> frequencies =
                WordCloudProcessor
                        .buildWordFrequencies(
                                this,
                                profileData.getComments(),
                                MAXIMUM_CLOUD_WORDS
                        );

        textViewWordCloudCommentCount.setText(
                getString(
                        R.string.word_cloud_comment_count,
                        profileData.getCommentCount()
                )
        );

        boolean isEmpty =
                frequencies.isEmpty();

        wordCloudView.setVisibility(
                isEmpty
                        ? View.GONE
                        : View.VISIBLE
        );

        textViewWordCloudEmpty.setVisibility(
                isEmpty
                        ? View.VISIBLE
                        : View.GONE
        );

        if (isEmpty) {

            wordCloudView.setWords(
                    new ArrayList<>()
            );

            return;
        }

        wordCloudView.setWords(frequencies);

        wordCloudView.setContentDescription(
                getString(
                        R.string.word_cloud_accessibility,
                        buildAccessibilityWordList(
                                frequencies
                        )
                )
        );
    }

    private String buildAccessibilityWordList(
            List<WordFrequency> frequencies
    ) {

        List<String> words =
                new ArrayList<>();

        int maximum =
                Math.min(
                        frequencies.size(),
                        MAXIMUM_ACCESSIBILITY_WORDS
                );

        for (
                int index = 0;
                index < maximum;
                index++
        ) {

            words.add(
                    frequencies.get(index)
                            .getWord()
            );
        }

        return TextUtils.join(
                ", ",
                words
        );
    }

    private void showLoading(
            boolean isLoading
    ) {

        progressBarMyProfile.setVisibility(
                isLoading
                        ? View.VISIBLE
                        : View.GONE
        );

        scrollViewMyProfile.setVisibility(
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