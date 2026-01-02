package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.adapter.ArtistsAdapter;
import com.example.vagmobile.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtistsActivity extends AppCompatActivity {

    private UserViewModel userViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArtistsAdapter artistsAdapter;
    private List<User> artistList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artists);

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        observeViewModels();
        loadArtists();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadArtists);
        // Настраиваем цвета индикатора обновления
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupRecyclerView() {
        artistsAdapter = new ArtistsAdapter(artistList, artist -> {
            // Открываем профиль пользователя вместо публикаций
            Intent intent = new Intent(ArtistsActivity.this, MainActivity.class);
            intent.putExtra("openProfile", true);
            intent.putExtra("userId", artist.getId());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(artistsAdapter);
    }

    private void observeViewModels() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        userViewModel.getArtistsResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> usersData = (List<Map<String, Object>>) result.get("users");
                    if (usersData != null && !usersData.isEmpty()) {
                        artistList.clear();
                        for (Map<String, Object> userData : usersData) {
                            User user = convertToUser(userData);
                            artistList.add(user);
                        }
                        artistsAdapter.notifyDataSetChanged();
                        hideEmptyState();
                    } else {
                        showEmptyState("Художники не найдены");
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, "Ошибка загрузки художников: " + message, Toast.LENGTH_SHORT).show();
                    showEmptyState("Ошибка загрузки");
                }
            } else {
                showEmptyState("Нет данных");
            }
        });
    }

    private void loadArtists() {
        progressBar.setVisibility(View.VISIBLE);
        userViewModel.getAllArtists();
    }

    private void showEmptyState(String message) {
        tvEmpty.setText(message);
        tvEmpty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        tvEmpty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private User convertToUser(Map<String, Object> userData) {
        User user = new User();

        Object idObj = userData.get("id");
        if (idObj != null) {
            if (idObj instanceof Double) {
                user.setId(((Double) idObj).longValue());
            } else if (idObj instanceof Integer) {
                user.setId(((Integer) idObj).longValue());
            } else if (idObj instanceof Long) {
                user.setId((Long) idObj);
            }
        }

        user.setUsername((String) userData.get("username"));
        user.setEmail((String) userData.get("email"));

        // ДОБАВЬТЕ ЭТО:
        Object countObj = userData.get("artworksCount");
        if (countObj != null) {
            if (countObj instanceof Double) {
                user.setArtworksCount(((Double) countObj).intValue());
            } else if (countObj instanceof Integer) {
                user.setArtworksCount((Integer) countObj);
            } else if (countObj instanceof Long) {
                user.setArtworksCount(((Long) countObj).intValue());
            }
        }

        return user;
    }
}