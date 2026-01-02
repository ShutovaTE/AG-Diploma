package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.adapter.ArtworkAdapter;
import com.example.vagmobile.viewmodel.ArtworkViewModel;
import com.example.vagmobile.viewmodel.CategoryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtworkListActivity extends AppCompatActivity {

    private ArtworkViewModel artworkViewModel;
    private CategoryViewModel categoryViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArtworkAdapter artworkAdapter;
    private List<Artwork> artworkList = new ArrayList<>();

    private String listType;
    private Long categoryId;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artwork_list);

        listType = getIntent().getStringExtra("list_type"); // "category", "user", "liked"
        categoryId = getIntent().getLongExtra("category_id", -1);
        categoryName = getIntent().getStringExtra("category_name");

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        observeViewModels();
        loadArtworks();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);

        if (categoryName != null) {
            tvTitle.setText(categoryName);
        } else if ("liked".equals(listType)) {
            tvTitle.setText("Liked Artworks");
        } else {
            tvTitle.setText("Artworks");
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadArtworks);
        // Настраиваем цвета индикатора обновления
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupRecyclerView() {
        artworkAdapter = new ArtworkAdapter(artworkList, new ArtworkAdapter.OnArtworkClickListener() {
            @Override
            public void onArtworkClick(Artwork artwork) {
                Intent intent = new Intent(ArtworkListActivity.this, ArtworkDetailActivity.class);
                intent.putExtra("artwork_id", artwork.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Artwork artwork) {
                // Не используется для обычного просмотра
            }

            @Override
            public void onDeleteClick(Artwork artwork) {
                // Не используется для обычного просмотра
            }
        }, false); // false - не показываем кнопки действий для обычного просмотра

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(artworkAdapter);
    }

    private void observeViewModels() {
        artworkViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ArtworkViewModel.class);
        categoryViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(CategoryViewModel.class);

        artworkViewModel.getArtworksResult().observe(this, result -> {
            handleArtworksResult(result);
        });

        categoryViewModel.getCategoryArtworksResult().observe(this, result -> {
            handleArtworksResult(result);
        });

        artworkViewModel.getLikedArtworksResult().observe(this, result -> {
            handleArtworksResult(result);
        });
    }

    private void handleArtworksResult(Map<String, Object> result) {
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);

        if (result != null) {
            Boolean success = (Boolean) result.get("success");
            if (success != null && success) {
                List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");
                if (artworksData != null) {
                    System.out.println("=== REGULAR ARTWORKS DATA DEBUG ===");
                    if (artworksData.size() > 0) {
                        Map<String, Object> firstArtwork = artworksData.get(0);
                        System.out.println("First artwork keys: " + firstArtwork.keySet());
                        System.out.println("User object in first artwork: " + firstArtwork.get("user"));
                    }

                    artworkList.clear();
                    for (Map<String, Object> artworkData : artworksData) {
                        Artwork artwork = convertToArtwork(artworkData);
                        artworkList.add(artwork);
                    }
                    artworkAdapter.notifyDataSetChanged();
                }
            } else {
                String message = (String) result.get("message");
                Toast.makeText(this, getString(R.string.failed_to_load_artworks, message), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadArtworks() {
        progressBar.setVisibility(View.VISIBLE);

        if (categoryId != -1) {
            categoryViewModel.getCategoryArtworks(categoryId, 0, 20);
        } else if ("liked".equals(listType)) {
            artworkViewModel.getLikedArtworks(0, 20);
        } else {
            artworkViewModel.getArtworks(0, 20);
        }
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

        Object userIdObj = artworkData.get("userId");
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
        if (artworkData.get("userName") != null) {
            username = (String) artworkData.get("userName");
        } else if (artworkData.get("author") != null) {
            username = (String) artworkData.get("author");
        } else if (artworkData.get("username") != null) {
            username = (String) artworkData.get("username");
        }

        user.setUsername(username != null ? username : "Неизвестный художник");

        return user;
    }
}