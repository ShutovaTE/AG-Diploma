package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.model.User;
import com.example.vagmobile.repository.ExhibitionRepository;
import com.example.vagmobile.repository.UserRepository;
import com.example.vagmobile.ui.adapter.ProfilePagerAdapter;
import com.example.vagmobile.util.SharedPreferencesHelper;
import com.example.vagmobile.viewmodel.UserViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvEmail, tvDescription, tvArtworksCount, tvExhibitionsCount;
    private ProgressBar progressBar;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private UserViewModel userViewModel;
    private User currentUser;
    private Long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        userViewModel.setContext(this); // Передаем контекст в ViewModel

        initViews();
        setupToolbar();
        loadUserData();
    }

    private void initViews() {
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvDescription = findViewById(R.id.tvDescription);
        tvArtworksCount = findViewById(R.id.tvArtworksCount);
        tvExhibitionsCount = findViewById(R.id.tvExhibitionsCount);
        progressBar = findViewById(R.id.progressBar);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Профиль пользователя");
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);

        // Получаем ID пользователя из intent или из SharedPreferences (для текущего пользователя)
        userId = getIntent().getLongExtra("userId", -1);
        if (userId == -1) {
            // Если ID не передан, загружаем текущего пользователя
            SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
            userId = prefs.getUserId();
            if (userId != null) {
                loadCurrentUserProfile();
            } else {
                showError("Не удалось получить ID пользователя");
            }
        } else {
            // Загружаем профиль другого пользователя
            loadUserProfile(userId);
        }
    }

    private void loadCurrentUserProfile() {
        userViewModel.getCurrentUser();
        userViewModel.getCurrentUserResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            handleUserData(result);
        });
    }

    private void loadUserProfile(Long userId) {
        userViewModel.getUser(userId);
        userViewModel.getUserResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            handleUserData(result);
        });
    }

    private void handleUserData(Map<String, Object> result) {
        Boolean success = (Boolean) result.get("success");
        if (success != null && success) {
            Map<String, Object> userData = (Map<String, Object>) result.get("user");
            if (userData != null) {
                updateUserInfo(userData);
                setupTabs();
            }
        } else {
            String message = (String) result.get("message");
            showError(message != null ? message : "Ошибка загрузки данных пользователя");
        }
    }

    private void updateUserInfo(Map<String, Object> userData) {
        String username = (String) userData.get("username");
        String email = (String) userData.get("email");
        String description = (String) userData.get("description");

        tvUsername.setText(username != null ? username : "Пользователь");
        tvEmail.setText(email != null ? email : "email@example.com");
        tvDescription.setText(description != null && !description.trim().isEmpty() ? description : "Описание не указано");

        // Загружаем счетчики публикаций и выставок
        loadUserStats();
    }

    private void loadUserStats() {
        if (userId != null) {
            // Загружаем публикации пользователя для подсчета
            UserRepository userRepo = new UserRepository(this);
            userRepo.getUserArtworks(userId, 0, 1).observe(this, artworksResult -> {
                if (artworksResult != null && Boolean.TRUE.equals(artworksResult.get("success"))) {
                    List<?> artworks = (List<?>) artworksResult.get("artworks");
                    if (artworks != null) {
                        tvArtworksCount.setText(String.valueOf(artworks.size()));
                    } else {
                        tvArtworksCount.setText("0");
                    }
                } else {
                    tvArtworksCount.setText("0");
                }
            });

            // Загружаем выставки пользователя для подсчета
            ExhibitionRepository exhibitionRepo = new ExhibitionRepository(this);
            exhibitionRepo.getUserExhibitions(userId, 0, 100).observe(this, exhibitionsResult -> {
                if (exhibitionsResult != null && Boolean.TRUE.equals(exhibitionsResult.get("success"))) {
                    List<?> exhibitions = (List<?>) exhibitionsResult.get("exhibitions");
                    if (exhibitions != null) {
                        tvExhibitionsCount.setText(String.valueOf(exhibitions.size()));
                    } else {
                        tvExhibitionsCount.setText("0");
                    }
                } else {
                    tvExhibitionsCount.setText("0");
                }
            });
        }
    }

    private void setupTabs() {
        // Определяем, является ли профиль собственным
        boolean isOwnProfile = isOwnProfile();

        // Создаем адаптер для ViewPager с параметром isOwnProfile
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(this, userId, isOwnProfile);
        viewPager.setAdapter(adapter);

        // Отключаем прокрутку ViewPager2, чтобы NestedScrollView мог обрабатывать всю прокрутку
        viewPager.setUserInputEnabled(false);

        // Настраиваем TabLayout с ViewPager
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Публикации");
                    break;
                case 1:
                    tab.setText("Выставки");
                    break;
            }
        }).attach();
    }

    private boolean isOwnProfile() {
        // Получаем ID текущего пользователя из SharedPreferences
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(this);
        Long currentUserId = prefs.getUserId();

        // Если ID пользователя совпадает с текущим, то это собственный профиль
        return currentUserId != null && currentUserId.equals(userId);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.GONE);
    }
}