package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.util.SharedPreferencesHelper;
import com.example.vagmobile.viewmodel.UserViewModel;

import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etDescription, etNewPassword, etCurrentPassword;
    private Button btnSave, btnCancel;
    private SharedPreferencesHelper prefs;
    private UserViewModel userViewModel;

    private boolean isLoading = false;
    private String currentUsername, currentEmail, currentDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        prefs = new SharedPreferencesHelper(this);
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.setContext(this);

        initViews();
        loadCurrentProfile();
        setupListeners();
        observeViewModel();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etDescription = findViewById(R.id.etDescription);
        etNewPassword = findViewById(R.id.etNewPassword);
        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void loadCurrentProfile() {
        // Загружаем профиль через API
        userViewModel.getCurrentUser();
    }

    private void setupListeners() {
        btnSave.setOnClickListener(v -> saveProfile());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveProfile() {
        if (isLoading) return;

        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String currentPassword = etCurrentPassword.getText().toString().trim();

        // Проверяем обязательные поля
        if (username.isEmpty()) {
            etUsername.setError("Введите имя пользователя");
            etUsername.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            etEmail.setError("Введите email");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Введите корректный email");
            etEmail.requestFocus();
            return;
        }

        // Текущий пароль обязателен для любых изменений
        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Введите текущий пароль для подтверждения");
            etCurrentPassword.requestFocus();
            return;
        }

        // Проверяем, есть ли изменения
        boolean hasProfileChanges = !username.equals(currentUsername) ||
                                   !email.equals(currentEmail) ||
                                   !description.equals(currentDescription != null ? currentDescription : "");
        boolean hasPasswordChange = !newPassword.isEmpty();

        if (!hasProfileChanges && !hasPasswordChange) {
            Toast.makeText(this, "Нет изменений для сохранения", Toast.LENGTH_SHORT).show();
            return;
        }

        isLoading = true;
        btnSave.setEnabled(false);
        btnSave.setText("Сохранение...");

        // Всегда проверяем пароль на сервере
        if (hasProfileChanges) {
            // Обновляем профиль с проверкой пароля
            updateProfileWithPassword(username, email, description, currentPassword, hasPasswordChange ? newPassword : null);
        } else if (hasPasswordChange) {
            // Только смена пароля
            changePassword(currentPassword, newPassword);
        }
    }

    private void updateProfileWithPassword(String username, String email, String description, String currentPassword, String newPassword) {
        userViewModel.updateProfileWithPassword(username, email, description, currentPassword);

        if (newPassword != null && !newPassword.isEmpty()) {
            // После обновления профиля меняем пароль
            userViewModel.getUpdateProfileResult().observe(this, profileResult -> {
                if (profileResult != null) {
                    Boolean success = (Boolean) profileResult.get("success");
                    if (success != null && success) {
                        // Профиль обновлен, теперь меняем пароль (без повторной проверки текущего пароля)
                        changePasswordWithoutValidation(currentPassword, newPassword);
                    } else {
                        String message = (String) profileResult.get("message");
                        showError("Ошибка обновления профиля: " + message);
                    }
                    userViewModel.getUpdateProfileResult().removeObservers(this);
                }
            });
        }
    }

    private void changePasswordWithoutValidation(String currentPassword, String newPassword) {
        userViewModel.changePasswordSkipValidation(currentPassword, newPassword);
    }

    private void changePassword(String currentPassword, String newPassword) {
        userViewModel.changePassword(currentPassword, newPassword);
    }

    private void observeViewModel() {
        // Наблюдатель за загрузкой профиля
        userViewModel.getCurrentUserResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    // Загружаем данные профиля
                    Map<String, Object> userData = (Map<String, Object>) result.get("user");
                    if (userData != null) {
                        currentUsername = (String) userData.get("username");
                        currentEmail = (String) userData.get("email");
                        currentDescription = (String) userData.get("description");

                        etUsername.setText(currentUsername != null ? currentUsername : "");
                        etEmail.setText(currentEmail != null ? currentEmail : "");
                        etDescription.setText(currentDescription != null ? currentDescription : "");
                    }
                } else {
                    // Если не удалось загрузить через API, используем локальные данные
                    String username = prefs.getUsername();
                    String email = prefs.getEmail();
                    String description = prefs.getDescription();

                    currentUsername = username;
                    currentEmail = email;
                    currentDescription = description;

                    etUsername.setText(username != null ? username : "");
                    etEmail.setText(email != null ? email : "");
                    etDescription.setText(description != null ? description : "");
                }
            }
        });

        // Наблюдатель за обновлением профиля
        userViewModel.getUpdateProfileResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    // Сохраняем локально
                    String username = etUsername.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    prefs.setUsername(username);
                    prefs.setEmail(email);
                    prefs.setDescription(description);

                    Toast.makeText(this, "Профиль успешно обновлен", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String message = (String) result.get("message");
                    String userFriendlyMessage = getUserFriendlyErrorMessage(message);
                    showError(userFriendlyMessage);
                }
            }
        });

        // Наблюдатель за сменой пароля
        userViewModel.getChangePasswordResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    // Сохраняем локально изменения профиля, если они были
                    String username = etUsername.getText().toString().trim();
                    String email = etEmail.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    prefs.setUsername(username);
                    prefs.setEmail(email);
                    prefs.setDescription(description);

                    Toast.makeText(this, "Профиль и пароль успешно обновлены", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String message = (String) result.get("message");
                    String userFriendlyMessage = getUserFriendlyErrorMessage(message);
                    showError(userFriendlyMessage);
                }
            }
        });
    }

    private void showError(String message) {
        isLoading = false;
        btnSave.setEnabled(true);
        btnSave.setText("Сохранить");
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private String getUserFriendlyErrorMessage(String serverMessage) {
        if (serverMessage == null) {
            return "Произошла неизвестная ошибка";
        }

        // Преобразуем технические сообщения в понятные пользователю
        if (serverMessage.contains("Current password is incorrect") ||
            serverMessage.contains("Current password is required") ||
            serverMessage.contains(getString(R.string.invalid_password))) {
            return "Неправильный пароль для подтверждения изменений";
        }

        if (serverMessage.contains("Username already exists")) {
            return "Пользователь с таким именем уже существует";
        }

        if (serverMessage.contains("Email already exists")) {
            return "Пользователь с таким email уже существует";
        }

        if (serverMessage.contains("New password must be different")) {
            return "Новый пароль должен отличаться от текущего";
        }

        if (serverMessage.contains(getString(R.string.authentication_required))) {
            return "Требуется авторизация";
        }

        if (serverMessage.contains(getString(R.string.network_error))) {
            return "Ошибка сети. Проверьте подключение к интернету";
        }

        if (serverMessage.contains("Failed to update profile")) {
            return "Не удалось обновить профиль. Попробуйте позже";
        }

        if (serverMessage.contains("Failed to change password")) {
            return "Не удалось изменить пароль. Попробуйте позже";
        }

        // Для всех остальных случаев возвращаем общее сообщение
        return "Произошла ошибка: " + serverMessage;
    }
}