package com.example.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.models.LoginResult;

public final class SessionManager {

    private static final String PREFERENCE_NAME =
            "mcommerce_login_session";

    private static final String KEY_IS_LOGGED_IN =
            "is_logged_in";

    private static final String KEY_USER_ID =
            "user_id";

    private static final String KEY_FULL_NAME =
            "full_name";

    private static final String KEY_ROLE =
            "role";

    private SessionManager() {
        // Không cho phép khởi tạo.
    }

    private static SharedPreferences getPreferences(
            Context context
    ) {
        return context.getApplicationContext()
                .getSharedPreferences(
                        PREFERENCE_NAME,
                        Context.MODE_PRIVATE
                );
    }

    public static void saveLogin(
            Context context,
            LoginResult result
    ) {
        getPreferences(context)
                .edit()
                .putBoolean(
                        KEY_IS_LOGGED_IN,
                        true
                )
                .putInt(
                        KEY_USER_ID,
                        result.getUserId()
                )
                .putString(
                        KEY_FULL_NAME,
                        result.getFullName()
                )
                .putString(
                        KEY_ROLE,
                        result.getRole()
                )
                .apply();
    }

    public static boolean isLoggedIn(
            Context context
    ) {
        return getPreferences(context)
                .getBoolean(
                        KEY_IS_LOGGED_IN,
                        false
                );
    }

    public static int getUserId(
            Context context
    ) {
        return getPreferences(context)
                .getInt(
                        KEY_USER_ID,
                        -1
                );
    }

    public static String getFullName(
            Context context
    ) {
        return getPreferences(context)
                .getString(
                        KEY_FULL_NAME,
                        ""
                );
    }

    public static String getRole(
            Context context
    ) {
        return getPreferences(context)
                .getString(
                        KEY_ROLE,
                        ""
                );
    }

    public static boolean isAdmin(
            Context context
    ) {
        return "ADMIN".equalsIgnoreCase(
                getRole(context)
        );
    }

    public static boolean isCustomer(
            Context context
    ) {
        return "CUSTOMER".equalsIgnoreCase(
                getRole(context)
        );
    }

    public static void clearSession(
            Context context
    ) {
        getPreferences(context)
                .edit()
                .clear()
                .apply();
    }
}