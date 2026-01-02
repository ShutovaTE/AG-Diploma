package com.example.vagmobile.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.Category;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.activity.ArtworkDetailActivity;
import com.example.vagmobile.ui.adapter.ArtworkAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserArtworksFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_IS_OWN_PROFILE = "is_own_profile";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ArtworkAdapter artworkAdapter;
    private List<Artwork> artworkList = new ArrayList<>();
    private Long userId;
    private boolean isOwnProfile;

    public static UserArtworksFragment newInstance(Long userId, boolean isOwnProfile) {
        UserArtworksFragment fragment = new UserArtworksFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        args.putBoolean(ARG_IS_OWN_PROFILE, isOwnProfile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID);
            isOwnProfile = getArguments().getBoolean(ARG_IS_OWN_PROFILE, false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_artworks, container, false);

        initViews(view);
        setupRecyclerView();
        loadUserArtworks();

        return view;
    }


    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void setupRecyclerView() {
        artworkAdapter = new ArtworkAdapter(artworkList, new ArtworkAdapter.OnArtworkClickListener() {
            @Override
            public void onArtworkClick(Artwork artwork) {
                Intent intent = new Intent(getActivity(), ArtworkDetailActivity.class);
                intent.putExtra("artwork_id", artwork.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Artwork artwork) {
                // Не используется в профиле
            }

            @Override
            public void onDeleteClick(Artwork artwork) {
                // Не используется в профиле
            }
        }, false); // false - не показываем кнопки действий

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(artworkAdapter);

        // Оптимизации для плавной прокрутки
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }


    private void loadUserArtworks() {
        if (userId != null) {
            progressBar.setVisibility(View.VISIBLE);
            // Создаем репозиторий с контекстом для загрузки публикаций пользователя
            com.example.vagmobile.repository.UserRepository repoWithContext = new com.example.vagmobile.repository.UserRepository(getContext());

            // Выбираем метод в зависимости от того, является ли профиль собственным
            if (isOwnProfile) {
                // Для собственного профиля загружаем все публикации
                repoWithContext.getAllUserArtworks(userId, 0, 50).observe(getViewLifecycleOwner(), result -> {
                    handleArtworksResult(result);
                });
            } else {
                // Для чужого профиля загружаем только APPROVED публикации
                repoWithContext.getUserArtworks(userId, 0, 50).observe(getViewLifecycleOwner(), result -> {
                    handleArtworksResult(result);
                });
            }
        }
    }

    private void handleArtworksResult(Map<String, Object> result) {
        progressBar.setVisibility(View.GONE);

        if (result != null) {
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                Map<String, Object> userData = (Map<String, Object>) result.get("user");
                List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");

                if (artworksData != null && !artworksData.isEmpty()) {
                    artworkList.clear();
                    // Используем Set для отслеживания уже добавленных ID и предотвращения дубликатов
                    java.util.Set<Long> addedArtworkIds = new java.util.HashSet<>();
                    for (Map<String, Object> artworkData : artworksData) {
                        Artwork artwork = convertToArtwork(artworkData);
                        // Проверяем, не добавляли ли уже эту публикацию
                        if (artwork.getId() != null && !addedArtworkIds.contains(artwork.getId())) {
                            artworkList.add(artwork);
                            addedArtworkIds.add(artwork.getId());
                        }
                    }
                    artworkAdapter.notifyDataSetChanged();
                    showContent();
                } else {
                    showEmpty("У пользователя пока нет публикаций");
                }
            } else {
                String message = (String) result.get("message");
                showError("Не удалось загрузить публикации: " + message);
            }
        }
    }

    private void showContent() {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        showEmpty("Ошибка загрузки публикаций");
    }

    private Artwork convertToArtwork(Map<String, Object> artworkData) {
        Artwork artwork = new Artwork();

        if (artworkData.get("id") != null) {
            if (artworkData.get("id") instanceof Double) {
                artwork.setId(((Double) artworkData.get("id")).longValue());
            } else if (artworkData.get("id") instanceof Long) {
                artwork.setId((Long) artworkData.get("id"));
            } else if (artworkData.get("id") instanceof Integer) {
                artwork.setId(((Integer) artworkData.get("id")).longValue());
            }
        }

        artwork.setTitle((String) artworkData.get("title"));
        artwork.setDescription((String) artworkData.get("description"));
        artwork.setImagePath((String) artworkData.get("imagePath"));
        artwork.setStatus((String) artworkData.get("status"));

        if (artworkData.get("likes") != null) {
            if (artworkData.get("likes") instanceof Double) {
                artwork.setLikes(((Double) artworkData.get("likes")).intValue());
            } else if (artworkData.get("likes") instanceof Integer) {
                artwork.setLikes((Integer) artworkData.get("likes"));
            } else if (artworkData.get("likes") instanceof Long) {
                artwork.setLikes(((Long) artworkData.get("likes")).intValue());
            }
        }

        artwork.setUser(parseUserFromArtworkData(artworkData));

        // Обработка категорий
        Object categoriesObj = artworkData.get("categories");
        if (categoriesObj instanceof List) {
            List<Map<String, Object>> categoriesData = (List<Map<String, Object>>) categoriesObj;
            List<com.example.vagmobile.model.Category> categories = new ArrayList<>();
            for (Map<String, Object> categoryData : categoriesData) {
                com.example.vagmobile.model.Category category = new com.example.vagmobile.model.Category();

                Object categoryIdObj = categoryData.get("id");
                if (categoryIdObj != null) {
                    if (categoryIdObj instanceof Double) {
                        category.setId(((Double) categoryIdObj).longValue());
                    } else if (categoryIdObj instanceof Integer) {
                        category.setId(((Integer) categoryIdObj).longValue());
                    } else if (categoryIdObj instanceof Long) {
                        category.setId((Long) categoryIdObj);
                    }
                }

                category.setName((String) categoryData.get("name"));
                category.setDescription((String) categoryData.get("description"));

                categories.add(category);
            }
            artwork.setCategories(categories);
        }

        return artwork;
    }

    private User parseUserFromArtworkData(Map<String, Object> artworkData) {
        User user = new User();

        Object userObj = artworkData.get("user");
        if (userObj instanceof Map) {
            Map<String, Object> userData = (Map<String, Object>) userObj;

            Object userIdObj = userData.get("id");
            if (userIdObj != null) {
                if (userIdObj instanceof Double) {
                    user.setId(((Double) userIdObj).longValue());
                } else if (userIdObj instanceof Integer) {
                    user.setId(((Integer) userIdObj).longValue());
                } else if (userIdObj instanceof Long) {
                    user.setId((Long) userIdObj);
                }
            }

            String username = null;
            if (userData.get("username") != null) {
                username = (String) userData.get("username");
            } else if (userData.get("userName") != null) {
                username = (String) userData.get("userName");
            } else if (userData.get("name") != null) {
                username = (String) userData.get("name");
            }
            user.setUsername(username != null ? username : "Неизвестный художник");

            if (userData.get("email") != null) {
                user.setEmail((String) userData.get("email"));
            }

            return user;
        }

        return user;
    }
}
