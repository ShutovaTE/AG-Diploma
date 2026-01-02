package com.example.vagmobile.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vagmobile.R;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.activity.MainActivity;
import com.example.vagmobile.ui.activity.ArtistArtworksActivity;
import com.example.vagmobile.ui.adapter.ArtistsAdapter;
import com.example.vagmobile.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtistsFragment extends Fragment {

    private UserViewModel userViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ArtistsAdapter artistsAdapter;
    private List<User> artistList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artists, container, false);

        initViews(view);
        setupRecyclerView();
        observeViewModels();
        loadArtists();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void setupRecyclerView() {
        artistsAdapter = new ArtistsAdapter(artistList, artist -> {
            // Открываем профиль пользователя вместо публикаций
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openUserProfile(artist.getId());
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(artistsAdapter);
    }

    private void observeViewModels() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        userViewModel.getArtistsResult().observe(getViewLifecycleOwner(), result -> {
            progressBar.setVisibility(View.GONE);

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
                    Toast.makeText(getContext(), "Ошибка загрузки художников: " + message, Toast.LENGTH_SHORT).show();
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
        try {
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

            Object countObj = userData.get("artworksCount");
            if (countObj != null) {
                if (countObj instanceof Double) {
                    user.setArtworksCount(((Double) countObj).intValue());
                } else if (countObj instanceof Integer) {
                    user.setArtworksCount((Integer) countObj);
                }
            }

            return user;
        } catch (Exception e) {
            Log.e("HomeFragment", "Error converting user: " + e.getMessage());
            return null;
        }
    }
}