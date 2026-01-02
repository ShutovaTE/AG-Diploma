package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.viewmodel.ExhibitionViewModel;

public class CreateExhibitionActivity extends AppCompatActivity {

    private ExhibitionViewModel exhibitionViewModel;

    private EditText etTitle, etDescription;
    private CheckBox cbAuthorOnly;
    private Button btnCreateExhibition;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_exhibition);

        exhibitionViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ExhibitionViewModel.class);

        initViews();
        setupClickListeners();
        observeViewModels();
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        cbAuthorOnly = findViewById(R.id.cbAuthorOnly);
        btnCreateExhibition = findViewById(R.id.btnCreateExhibition);
        progressBar = findViewById(R.id.progressBar);

        btnCreateExhibition.setEnabled(true);
    }

    private void setupClickListeners() {
        btnCreateExhibition.setOnClickListener(v -> createExhibition());

        View rootLayout = findViewById(android.R.id.content);
        rootLayout.setOnClickListener(v -> {
            etTitle.clearFocus();
            etDescription.clearFocus();
        });
    }

    private void observeViewModels() {
        exhibitionViewModel.getCreateResult().observe(this, result -> {
            resetCreateButton();

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, "Выставка создана успешно!", Toast.LENGTH_SHORT).show();
                    resetForm();
                    finish();
                } else {
                    String message = (String) result.get("message");
                    String errorMessage = "Не удалось создать выставку";
                    if (message != null && !message.isEmpty()) {
                        errorMessage += ": " + message;
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Не удалось создать выставку: null result", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createExhibition() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        boolean authorOnly = cbAuthorOnly.isChecked();

        if (title.isEmpty()) {
            etTitle.setError("Введите название выставки");
            etTitle.requestFocus();
            return;
        }

        if (title.length() < 3) {
            etTitle.setError("Название должно содержать минимум 3 символа");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Введите описание выставки");
            etDescription.requestFocus();
            return;
        }

        if (description.length() < 10) {
            etDescription.setError("Описание должно содержать минимум 10 символов");
            etDescription.requestFocus();
            return;
        }

        btnCreateExhibition.setEnabled(false);
        btnCreateExhibition.setText("Создание...");
        progressBar.setVisibility(View.VISIBLE);

        exhibitionViewModel.createExhibition(title, description, authorOnly);
    }

    private void resetCreateButton() {
        btnCreateExhibition.setEnabled(true);
        btnCreateExhibition.setText("Создать выставку");
        progressBar.setVisibility(View.GONE);
    }

    private void resetForm() {
        etTitle.setText("");
        etDescription.setText("");
        cbAuthorOnly.setChecked(false);
    }
}
