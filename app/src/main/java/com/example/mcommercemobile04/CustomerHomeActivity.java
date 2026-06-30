package com.example.mcommercemobile04;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.utils.SessionManager;

public class CustomerHomeActivity
        extends AppCompatActivity {

    private TextView textViewHomeTitle;
    private TextView textViewWelcome;
    private TextView textViewRoleDescription;

    private Button buttonBrowseProducts;
    private Button buttonMyProfile;
    private Button buttonLogout;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.isLoggedIn(this)
                || !SessionManager.isCustomer(this)) {

            openLoginScreen();
            return;
        }

        setContentView(
                R.layout.activity_role_home
        );

        mapViews();
        displayCustomerInformation();
        setEvents();
    }

    private void mapViews() {

        textViewHomeTitle =
                findViewById(
                        R.id.textViewHomeTitle
                );

        textViewWelcome =
                findViewById(
                        R.id.textViewWelcome
                );

        textViewRoleDescription =
                findViewById(
                        R.id.textViewRoleDescription
                );

        buttonBrowseProducts =
                findViewById(
                        R.id.buttonBrowseProducts
                );

        buttonMyProfile =
                findViewById(
                        R.id.buttonMyProfile
                );

        buttonLogout =
                findViewById(
                        R.id.buttonLogout
                );

        buttonBrowseProducts.setVisibility(
                View.VISIBLE
        );

        buttonMyProfile.setVisibility(
                View.VISIBLE
        );
    }

    private void displayCustomerInformation() {

        textViewHomeTitle.setText(
                R.string.customer_home_title
        );

        textViewWelcome.setText(
                getString(
                        R.string.home_welcome,
                        SessionManager.getFullName(this)
                )
        );

        textViewRoleDescription.setText(
                R.string.home_role_customer
        );
    }

    private void setEvents() {

        buttonBrowseProducts.setOnClickListener(
                view -> openProductScreen()
        );

        buttonMyProfile.setOnClickListener(
                view -> openProfileScreen()
        );

        buttonLogout.setOnClickListener(
                view -> logout()
        );
    }

    private void openProductScreen() {

        Intent intent =
                new Intent(
                        this,
                        ProductSearchActivity.class
                );

        startActivity(intent);
    }

    private void openProfileScreen() {

        Intent intent =
                new Intent(
                        this,
                        MyProfileActivity.class
                );

        startActivity(intent);
    }

    private void logout() {

        SessionManager.clearSession(this);

        Toast.makeText(
                this,
                R.string.logout_success,
                Toast.LENGTH_SHORT
        ).show();

        openLoginScreen();
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
}