package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.vagmobile.R;
import com.github.chrisbanes.photoview.PhotoView;

public class FullscreenImageActivity extends AppCompatActivity {

    private PhotoView photoView;
    private ImageButton btnClose;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        // Скрываем ActionBar для полноэкранного режима
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Делаем статус бар прозрачным для полного погружения
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        initViews();
        setupClickListeners();
        loadImage();
    }

    private void initViews() {
        photoView = findViewById(R.id.photoView);
        btnClose = findViewById(R.id.btnClose);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        btnClose.setOnClickListener(v -> finish());

        // Также закрываем активность при клике на изображение
        photoView.setOnClickListener(v -> finish());
    }

    private void loadImage() {
        String imageUrl = getIntent().getStringExtra("image_url");

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "URL изображения не найден", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Показываем прогресс бар
        progressBar.setVisibility(View.VISIBLE);

        // Загружаем изображение с Glide
        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.color.black)
                .error(R.drawable.ic_error_image)
                .into(photoView);

        // Скрываем прогресс бар после загрузки
        photoView.postDelayed(() -> progressBar.setVisibility(View.GONE), 500);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Плавная анимация закрытия
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Восстанавливаем иммерсивный режим при возвращении
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }
}
