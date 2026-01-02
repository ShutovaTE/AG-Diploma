package com.example.vagmobile.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.vagmobile.R;
import com.example.vagmobile.repository.ExhibitionRepository;
import com.example.vagmobile.repository.UserRepository;
import com.example.vagmobile.ui.activity.MainActivity;
import com.example.vagmobile.ui.adapter.ProfilePagerAdapter;
import com.example.vagmobile.util.SharedPreferencesHelper;
import com.example.vagmobile.viewmodel.UserViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private TextView tvUsername, tvEmail, tvDescription, tvArtworksCount, tvExhibitionsCount;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private UserViewModel userViewModel;
    private Long userId;
    private boolean isOwnProfile = true;

    public static ProfileFragment newInstance(Long userId) {
        ProfileFragment fragment = new ProfileFragment();
        if (userId != null) {
            Bundle args = new Bundle();
            args.putLong("userId", userId);
            fragment.setArguments(args);
        }
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        if (getContext() != null) {
            userViewModel.setContext(getContext());
        }

        initViews(view);
        setupSwipeRefresh();
        setupToolbar();
        loadUserData();

        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvDescription = view.findViewById(R.id.tvDescription);
        tvArtworksCount = view.findViewById(R.id.tvArtworksCount);
        tvExhibitionsCount = view.findViewById(R.id.tvExhibitionsCount);
        progressBar = view.findViewById(R.id.progressBar);
        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadUserData);
        // Настраиваем цвета индикатора обновления
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupToolbar() {
        // Настройка меню в toolbar (шестерёнка для настроек)
        if (getActivity() != null) {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setTitle("Профиль");
                toolbar.getMenu().clear();
                // Показываем меню только для своего профиля
                if (isOwnProfile) {
                    toolbar.inflateMenu(R.menu.profile_menu);
                    toolbar.setOnMenuItemClickListener(this::onMenuItemClick);
                }
            }
        }
    }

    private boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            openSettings();
            return true;
        }
        return false;
    }

    private void openSettings() {
        // Переход к настройкам профиля (старый ProfileFragment)
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragmentWithBackStack(new EditProfileFragment());
        }
    }

    private void loadUserData() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Получаем ID пользователя из аргументов фрагмента (для чужого профиля) или из SharedPreferences (для своего профиля)
        if (getArguments() != null) {
            userId = getArguments().getLong("userId", -1);
            if (userId == -1) {
                // Если userId не передан в аргументах, загружаем текущего пользователя
                SharedPreferencesHelper prefs = new SharedPreferencesHelper(getContext());
                userId = prefs.getUserId();
                isOwnProfile = true;
            } else {
                // Проверяем, является ли это профилем текущего пользователя
                SharedPreferencesHelper prefs = new SharedPreferencesHelper(getContext());
                Long currentUserId = prefs.getUserId();
                isOwnProfile = currentUserId != null && currentUserId.equals(userId);
            }
        } else {
            // Загружаем текущего пользователя
            SharedPreferencesHelper prefs = new SharedPreferencesHelper(getContext());
            userId = prefs.getUserId();
            isOwnProfile = true;
        }

        if (userId != null) {
            if (isOwnProfile) {
                loadCurrentUserProfile();
            } else {
                loadUserProfile(userId);
            }
        } else {
            showError("Не удалось получить ID пользователя");
        }
    }

    private void loadCurrentUserProfile() {
        userViewModel.getCurrentUser();
        userViewModel.getCurrentUserResult().observe(getViewLifecycleOwner(), result -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
            handleUserData(result);
        });
    }

    private void loadUserProfile(Long userId) {
        userViewModel.getUser(userId);
        userViewModel.getUserResult().observe(getViewLifecycleOwner(), result -> {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            swipeRefreshLayout.setRefreshing(false);
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

        if (tvUsername != null) {
            tvUsername.setText(username != null ? username : "Пользователь");
        }
        if (tvEmail != null) {
            tvEmail.setText(email != null ? email : "email@example.com");
        }
        if (tvDescription != null) {
            tvDescription.setText(description != null && !description.trim().isEmpty() ? description : "Описание не указано");
        }

        // Загружаем счетчики публикаций и выставок
        loadUserStats();
    }

    private void loadUserStats() {
        if (userId != null && getContext() != null) {
            // Загружаем публикации пользователя для подсчета
            UserRepository userRepo = new UserRepository(getContext());
            userRepo.getUserArtworks(userId, 0, 1).observe(getViewLifecycleOwner(), artworksResult -> {
                if (tvArtworksCount != null && artworksResult != null && Boolean.TRUE.equals(artworksResult.get("success"))) {
                    List<?> artworks = (List<?>) artworksResult.get("artworks");
                    if (artworks != null) {
                        tvArtworksCount.setText(String.valueOf(artworks.size()));
                    } else {
                        tvArtworksCount.setText("0");
                    }
                } else if (tvArtworksCount != null) {
                    tvArtworksCount.setText("0");
                }
            });

            // Загружаем выставки пользователя для подсчета
            ExhibitionRepository exhibitionRepo = new ExhibitionRepository(getContext());
            exhibitionRepo.getUserExhibitions(userId, 0, 100).observe(getViewLifecycleOwner(), exhibitionsResult -> {
                if (tvExhibitionsCount != null && exhibitionsResult != null && Boolean.TRUE.equals(exhibitionsResult.get("success"))) {
                    List<?> exhibitions = (List<?>) exhibitionsResult.get("exhibitions");
                    if (exhibitions != null) {
                        tvExhibitionsCount.setText(String.valueOf(exhibitions.size()));
                    } else {
                        tvExhibitionsCount.setText("0");
                    }
                } else if (tvExhibitionsCount != null) {
                    tvExhibitionsCount.setText("0");
                }
            });
        }
    }

    private void setupTabs() {
        if (getActivity() != null && viewPager != null && tabLayout != null) {
            // Создаем адаптер для ViewPager
            ProfilePagerAdapter adapter = new ProfilePagerAdapter(getActivity(), userId, isOwnProfile);
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
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        }
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupToolbar();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Очищаем меню при уничтожении view
        if (getActivity() != null) {
            androidx.appcompat.widget.Toolbar toolbar = getActivity().findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.getMenu().clear();
            }
        }
    }
}