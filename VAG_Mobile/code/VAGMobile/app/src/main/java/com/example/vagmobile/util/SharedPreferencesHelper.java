package com.example.vagmobile.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {
    private static final String PREFS_NAME = "VAGMobilePrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_DESCRIPTION = "description";

    private SharedPreferences sharedPreferences;

    public SharedPreferencesHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setLoggedIn(boolean loggedIn) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void setUserId(Long userId) {
        sharedPreferences.edit().putLong(KEY_USER_ID, userId != null ? userId : -1).apply();
    }

    public Long getUserId() {
        long userId = sharedPreferences.getLong(KEY_USER_ID, -1);
        return userId != -1 ? userId : null;
    }

    public void setUsername(String username) {
        sharedPreferences.edit().putString(KEY_USERNAME, username).apply();
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public void setEmail(String email) {
        sharedPreferences.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    public void setDescription(String description) {
        sharedPreferences.edit().putString(KEY_DESCRIPTION, description).apply();
    }

    public String getDescription() {
        return sharedPreferences.getString(KEY_DESCRIPTION, null);
    }

    public void saveUserData(Long userId, String username, String email, String role) {
        saveUserData(userId, username, email, null, role);
    }

    public void saveUserData(Long userId, String username, String email, String description, String role) {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (userId != null) {
                editor.putLong(KEY_USER_ID, userId);
            }
            if (username != null) {
                editor.putString(KEY_USERNAME, username);
            }
            if (email != null) {
                editor.putString(KEY_EMAIL, email);
            }
            if (description != null) {
                editor.putString(KEY_DESCRIPTION, description);
            }
            editor.putString(KEY_USER_ROLE, role != null ? role : "USER");
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveToken(String token) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, "USER");
    }

    public void clearUserData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USERNAME);
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_DESCRIPTION);
        editor.remove(KEY_USER_ROLE);
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_TOKEN);
        editor.apply();
    }
}