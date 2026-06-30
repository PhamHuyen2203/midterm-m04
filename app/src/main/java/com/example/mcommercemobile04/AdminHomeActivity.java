package com.example.mcommercemobile04;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.utils.SessionManager;

public class AdminHomeActivity
        extends AppCompatActivity {

    private TextView textViewHomeTitle;
    private TextView textViewWelcome;
    private TextView textViewRoleDescription;
    private Button buttonLogout;

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        /*
         * Không cho mở trang quản trị nếu chưa đăng nhập
         * hoặc không phải ADMIN.
         */
        if (!SessionManager.isLoggedIn(this)
                || !SessionManager.isAdmin(this)) {

            openLoginScreen();
            return;
        }

        setContentView(
                R.layout.activity_role_home
        );

        mapViews();
        displayAdminInformation();
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

        buttonLogout =
                findViewById(
                        R.id.buttonLogout
                );
    }

    private void displayAdminInformation() {

        textViewHomeTitle.setText(
                R.string.admin_home_title
        );

        textViewWelcome.setText(
                getString(
                        R.string.home_welcome,
                        SessionManager.getFullName(this)
                )
        );

        textViewRoleDescription.setText(
                R.string.home_role_admin
        );
    }

    private void setEvents() {

        buttonLogout.setOnClickListener(
                view -> logout()
        );
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