package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtistArtworksActivity extends AppCompatActivity {

    private ArtworkViewModel artworkViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvArtistName, tvEmpty;
    private ArtworkAdapter artworkAdapter;
    private List<Artwork> artworkList = new ArrayList<>();

    private Long artistId;
    private String artistName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_artworks);

        artistId = getIntent().getLongExtra("artist_id", -1);
        artistName = getIntent().getStringExtra("artist_name");

        if (artistId == -1) {
            Toast.makeText(this, getString(R.string.artist_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        observeViewModels();
        loadArtistArtworks();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvArtistName = findViewById(R.id.tvArtistName);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (artistName != null) {
            tvArtistName.setText("Публикации " + artistName);
        }
    }

    private void setupRecyclerView() {
        artworkAdapter = new ArtworkAdapter(artworkList, new ArtworkAdapter.OnArtworkClickListener() {
            @Override
            public void onArtworkClick(Artwork artwork) {
                Intent intent = new Intent(ArtistArtworksActivity.this, ArtworkDetailActivity.class);
                intent.putExtra("artwork_id", artwork.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Artwork artwork) {
                // Не нужно для просмотра публикаций художника
            }

            @Override
            public void onDeleteClick(Artwork artwork) {
                // Не нужно для просмотра публикаций художника
            }
        }, false); // false - не показываем кнопки действий для просмотра чужих публикаций

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(artworkAdapter);
    }

    private void observeViewModels() {
        artworkViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ArtworkViewModel.class);

        artworkViewModel.getArtworksResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");
                    if (artworksData != null && !artworksData.isEmpty()) {
                        artworkList.clear();
                        for (Map<String, Object> artworkData : artworksData) {
                            Artwork artwork = convertToArtwork(artworkData);

                            // Проверяем user ID
                            User user = artwork.getUser();
                            if (user != null) {
                                Long userId = user.getId();
                                if (userId != null && userId.equals(artistId)) {
                                    // ВАЖНО: Показываем все публикации художника
                                    artworkList.add(artwork);
                                }
                            }
                        }
                        artworkAdapter.notifyDataSetChanged();

                        if (artworkList.isEmpty()) {
                            showEmptyState("У этого художника пока нет публикаций");
                        } else {
                            hideEmptyState();
                        }
                    } else {
                        showEmptyState("У этого художника пока нет публикаций");
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_load_artworks, message), Toast.LENGTH_SHORT).show();
                    showEmptyState("Ошибка загрузки публикаций");
                }
            }
        });
    }

    private void loadArtistArtworks() {
        progressBar.setVisibility(View.VISIBLE);
        artworkViewModel.getArtworks(0, 100);
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

        if (artworkData.get("views") != null) {
            if (artworkData.get("views") instanceof Double) {
                artwork.setViews(((Double) artworkData.get("views")).intValue());
            } else if (artworkData.get("views") instanceof Integer) {
                artwork.setViews((Integer) artworkData.get("views"));
            } else if (artworkData.get("views") instanceof Long) {
                artwork.setViews(((Long) artworkData.get("views")).intValue());
            }
        }

        if (artworkData.get("user") != null && artworkData.get("user") instanceof Map) {
            Map<String, Object> userData = (Map<String, Object>) artworkData.get("user");
            User user = new User();

            if (userData.get("id") != null) {
                if (userData.get("id") instanceof Double) {
                    user.setId(((Double) userData.get("id")).longValue());
                } else if (userData.get("id") instanceof Long) {
                    user.setId((Long) userData.get("id"));
                } else if (userData.get("id") instanceof Integer) {
                    user.setId(((Integer) userData.get("id")).longValue());
                }
            }

            user.setUsername((String) userData.get("username"));
            user.setEmail((String) userData.get("email"));
            artwork.setUser(user);
        } else {
            // Создаем пустого пользователя, чтобы избежать NullPointerException
            User unknownUser = new User();
            unknownUser.setUsername("Неизвестный художник");
            artwork.setUser(unknownUser);
        }

        return artwork;
    }
}