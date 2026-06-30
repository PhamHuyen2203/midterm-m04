package com.example.mcommercemobile04;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dals.UserDAO;
import com.example.models.LoginResult;
import com.example.utils.SessionManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String LOG_TAG =
            "LoginActivity";

    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;

    /*
     * Thực hiện truy vấn database ngoài UI thread.
     */
    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);

        mapViews();
        setEvents();
    }

    /**
     * Ánh xạ các View.
     */
    private void mapViews() {

        editTextUsername =
                findViewById(
                        R.id.editTextUsername
                );

        editTextPassword =
                findViewById(
                        R.id.editTextPassword
                );

        buttonLogin =
                findViewById(
                        R.id.buttonLogin
                );
    }

    /**
     * Đăng ký sự kiện.
     */
    private void setEvents() {

        buttonLogin.setOnClickListener(
                view -> attemptLogin()
        );

        editTextPassword.setOnEditorActionListener(
                (textView, actionId, event) -> {

                    if (actionId
                            == EditorInfo.IME_ACTION_DONE) {

                        attemptLogin();
                        return true;
                    }

                    return false;
                }
        );
    }

    /**
     * Kiểm tra dữ liệu nhập và bắt đầu đăng nhập.
     */
    private void attemptLogin() {

        editTextUsername.setError(null);
        editTextPassword.setError(null);

        String username =
                editTextUsername.getText()
                        .toString()
                        .trim();

        /*
         * Không trim mật khẩu vì khoảng trắng có thể
         * là một phần của mật khẩu.
         */
        String password =
                editTextPassword.getText()
                        .toString();

        boolean isValid = true;

        if (TextUtils.isEmpty(username)) {

            editTextUsername.setError(
                    getString(
                            R.string.login_username_required
                    )
            );

            editTextUsername.requestFocus();
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {

            editTextPassword.setError(
                    getString(
                            R.string.login_password_required
                    )
            );

            if (isValid) {
                editTextPassword.requestFocus();
            }

            isValid = false;
        }

        if (!isValid) {
            return;
        }

        buttonLogin.setEnabled(false);

        executorService.execute(() -> {

            try {
                LoginResult loginResult =
                        UserDAO.login(
                                getApplicationContext(),
                                username,
                                password
                        );

                runOnUiThread(() -> {

                    buttonLogin.setEnabled(true);

                    handleLoginResult(
                            loginResult
                    );
                });

            } catch (Exception exception) {

                Log.e(
                        LOG_TAG,
                        "Login query failed.",
                        exception
                );

                runOnUiThread(() -> {

                    buttonLogin.setEnabled(true);

                    Toast.makeText(
                            this,
                            R.string.login_database_error,
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    /**
     * Xử lý kết quả UserDAO trả về.
     */
    private void handleLoginResult(
            LoginResult loginResult
    ) {

        switch (loginResult.getStatus()) {

            case SUCCESS:
                handleSuccessfulLogin(loginResult);
                break;

            case USER_NOT_FOUND:
                showInvalidCredentialMessage();
                break;

            case WRONG_PASSWORD:
                showWrongPasswordMessage(loginResult);
                break;

            case ACCOUNT_LOCKED:
                showLockedAccountMessage();
                break;
        }
    }

    /**
     * Đăng nhập thành công và chuyển màn hình theo Role.
     */
    private void handleSuccessfulLogin(
            LoginResult loginResult
    ) {

        SessionManager.saveLogin(
                this,
                loginResult
        );

        Intent intent;

        if (loginResult.isAdmin()) {

            Toast.makeText(
                    this,
                    getString(
                            R.string.login_success_admin,
                            loginResult.getFullName()
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            intent = new Intent(
                    this,
                    AdminHomeActivity.class
            );

        } else if (loginResult.isCustomer()) {

            Toast.makeText(
                    this,
                    getString(
                            R.string.login_success_customer,
                            loginResult.getFullName()
                    ),
                    Toast.LENGTH_SHORT
            ).show();

            intent = new Intent(
                    this,
                    CustomerHomeActivity.class
            );

        } else {

            SessionManager.clearSession(this);

            Toast.makeText(
                    this,
                    R.string.login_invalid_role,
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        startActivity(intent);
        finish();
    }

    /**
     * Không tìm thấy username.
     */
    private void showInvalidCredentialMessage() {

        Toast.makeText(
                this,
                R.string.login_invalid_credentials,
                Toast.LENGTH_SHORT
        ).show();

        clearPasswordField();
    }

    /**
     * Sai mật khẩu nhưng chưa đủ 3 lần.
     */
    private void showWrongPasswordMessage(
            LoginResult loginResult
    ) {

        Toast.makeText(
                this,
                getString(
                        R.string.login_wrong_password_remaining,
                        loginResult.getRemainingAttempts()
                ),
                Toast.LENGTH_LONG
        ).show();

        clearPasswordField();
    }

    /**
     * Tài khoản bị khóa.
     */
    private void showLockedAccountMessage() {

        Toast.makeText(
                this,
                R.string.login_account_locked,
                Toast.LENGTH_LONG
        ).show();

        clearPasswordField();
    }

    private void clearPasswordField() {

        editTextPassword.setText(null);
        editTextPassword.requestFocus();
    }

    @Override
    protected void onDestroy() {

        executorService.shutdownNow();

        super.onDestroy();
    }
}