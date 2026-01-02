package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.Exhibition;
import com.example.vagmobile.ui.adapter.SelectableArtworkAdapter;
import com.example.vagmobile.viewmodel.ExhibitionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddArtworkToExhibitionActivity extends AppCompatActivity {

    private ExhibitionViewModel exhibitionViewModel;
    private Exhibition exhibition;
    private Long exhibitionId;

    private ProgressBar progressBar;
    private LinearLayout layoutExhibitionInfo, layoutEmpty, layoutActions;
    private TextView tvExhibitionTitle, tvExhibitionDescription;
    private RecyclerView recyclerViewArtworks;
    private Button btnCancel, btnAddSelected;

    private SelectableArtworkAdapter artworkAdapter;
    private List<Artwork> artworkList = new ArrayList<>();

    // Pagination variables
    private int currentPage = 0;
    private final int PAGE_SIZE = 20;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_artwork_to_exhibition);

        exhibitionId = getIntent().getLongExtra("exhibition_id", -1);

        if (exhibitionId == -1) {
            Toast.makeText(this, "Выставка не найдена", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupActionBar();
        setupRecyclerView();
        observeViewModels();
        loadExhibition();
    }

    private void initViews() {
        progressBar = findViewById(R.id.progressBar);
        layoutExhibitionInfo = findViewById(R.id.layoutExhibitionInfo);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        layoutActions = findViewById(R.id.layoutActions);
        tvExhibitionTitle = findViewById(R.id.tvExhibitionTitle);
        tvExhibitionDescription = findViewById(R.id.tvExhibitionDescription);
        recyclerViewArtworks = findViewById(R.id.recyclerViewArtworks);
        btnCancel = findViewById(R.id.btnCancel);
        btnAddSelected = findViewById(R.id.btnAddSelected);

        btnCancel.setOnClickListener(v -> finish());
        btnAddSelected.setOnClickListener(v -> addSelectedArtworksToExhibition());
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Добавить работы");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerViewArtworks.setLayoutManager(layoutManager);

        artworkAdapter = new SelectableArtworkAdapter(artworkList, selectedCount -> {
            updateAddButtonState(selectedCount > 0);
        });

        recyclerViewArtworks.setAdapter(artworkAdapter);

        // Add infinite scroll listener
        recyclerViewArtworks.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMorePages) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                                && firstVisibleItemPosition >= 0
                                && totalItemCount >= PAGE_SIZE) {
                            loadNextPage();
                        }
                    }
                }
            }
        });
    }

    private void observeViewModels() {
        exhibitionViewModel = new ViewModelProvider(this).get(ExhibitionViewModel.class);

        exhibitionViewModel.getExhibitionResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Map<String, Object> exhibitionData = (Map<String, Object>) result.get("exhibition");
                    exhibition = parseExhibitionFromMap(exhibitionData);
                    displayExhibition();

                    // Загружаем работы пользователя для добавления
                    loadUserArtworksForExhibition();
                } else {
                    String message = (String) result.get("message");
                    if (message != null) {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            }
            progressBar.setVisibility(View.GONE);
        });

        exhibitionViewModel.getUserArtworksForExhibitionResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");

                    // Безопасный парсинг totalPages
                    Integer totalPages = null;
                    Object totalPagesObj = result.get("totalPages");
                    if (totalPagesObj != null) {
                        if (totalPagesObj instanceof Double) {
                            totalPages = ((Double) totalPagesObj).intValue();
                        } else if (totalPagesObj instanceof Integer) {
                            totalPages = (Integer) totalPagesObj;
                        } else if (totalPagesObj instanceof Long) {
                            totalPages = ((Long) totalPagesObj).intValue();
                        }
                    }

                    List<Artwork> newArtworks = new ArrayList<>();

                    if (artworksData != null) {
                        for (Map<String, Object> artworkData : artworksData) {
                            Artwork artwork = parseArtworkFromMap(artworkData);
                            if (artwork != null) {
                                newArtworks.add(artwork);
                            }
                        }
                    }

                    // Для первой страницы очищаем список, для последующих - добавляем
                    if (currentPage == 0) {
                        artworkList.clear();
                        artworkList.addAll(newArtworks);
                        artworkAdapter.notifyDataSetChanged();
                    } else {
                        artworkAdapter.addItems(newArtworks);
                    }

                    // Проверяем, есть ли еще страницы
                    hasMorePages = totalPages != null && currentPage < totalPages - 1;
                    isLoading = false;

                    updateEmptyState();
                } else {
                    isLoading = false;
                    updateEmptyState();
                }
            } else {
                isLoading = false;
                updateEmptyState();
            }
        });

        exhibitionViewModel.getAddArtworkResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                String message = (String) result.get("message");

                if (success != null && success) {
                    Toast.makeText(this, message != null ? message : "Работа успешно добавлена", Toast.LENGTH_SHORT).show();
                    // Обновляем список - убираем добавленную работу
                    refreshArtworksList();
                } else {
                    Toast.makeText(this, message != null ? message : "Не удалось добавить работу", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadExhibition() {
        progressBar.setVisibility(View.VISIBLE);
        exhibitionViewModel.getExhibition(exhibitionId);
    }

    private void loadUserArtworksForExhibition() {
        currentPage = 0;
        isLoading = true;
        hasMorePages = true;
        exhibitionViewModel.getUserArtworksForExhibition(exhibitionId, currentPage, PAGE_SIZE);
    }

    private void loadNextPage() {
        if (!isLoading && hasMorePages) {
            isLoading = true;
            currentPage++;
            exhibitionViewModel.getUserArtworksForExhibition(exhibitionId, currentPage, PAGE_SIZE);
        }
    }

    private void refreshArtworksList() {
        // Очищаем текущий список и загружаем заново
        currentPage = 0;
        artworkList.clear();
        artworkAdapter.notifyDataSetChanged();
        loadUserArtworksForExhibition();
    }

    private void displayExhibition() {
        if (exhibition == null) return;

        tvExhibitionTitle.setText(exhibition.getTitle());
        tvExhibitionDescription.setText(exhibition.getDescription());
        layoutExhibitionInfo.setVisibility(View.VISIBLE);
    }

    private void updateEmptyState() {
        if (artworkList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerViewArtworks.setVisibility(View.GONE);
            layoutActions.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerViewArtworks.setVisibility(View.VISIBLE);
            layoutActions.setVisibility(View.VISIBLE);
        }
    }

    private void updateAddButtonState(boolean enabled) {
        btnAddSelected.setEnabled(enabled);
        btnAddSelected.setAlpha(enabled ? 1.0f : 0.5f);
    }

    private void addSelectedArtworksToExhibition() {
        Set<Long> selectedArtworkIds = artworkAdapter.getSelectedArtworkIds();

        if (selectedArtworkIds.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы одну работу", Toast.LENGTH_SHORT).show();
            return;
        }

        // Добавляем каждую выбранную работу в выставку
        for (Long artworkId : selectedArtworkIds) {
            exhibitionViewModel.addArtworkToExhibition(exhibitionId, artworkId);
        }

        // Очищаем выбор и обновляем список
        artworkAdapter.clearSelection();
        updateAddButtonState(false);

        Toast.makeText(this, "Работы добавляются в выставку...", Toast.LENGTH_LONG).show();
    }

    private Exhibition parseExhibitionFromMap(Map<String, Object> data) {
        try {
            Exhibition exhibition = new Exhibition();

            // Безопасный парсинг id
            Object idObj = data.get("id");
            if (idObj != null) {
                long id = 0;
                if (idObj instanceof Double) {
                    id = ((Double) idObj).longValue();
                } else if (idObj instanceof Integer) {
                    id = ((Integer) idObj).longValue();
                } else if (idObj instanceof Long) {
                    id = (Long) idObj;
                } else {
                    id = Long.valueOf(idObj.toString());
                }
                exhibition.setId(id);
            }

            exhibition.setTitle((String) data.get("title"));
            exhibition.setDescription((String) data.get("description"));
            exhibition.setImageUrl((String) data.get("imageUrl"));
            exhibition.setAuthorOnly((Boolean) data.get("authorOnly"));

            // Безопасный парсинг artworksCount
            Object artworksCountObj = data.get("artworksCount");
            if (artworksCountObj != null) {
                int artworksCount = 0;
                if (artworksCountObj instanceof Double) {
                    artworksCount = ((Double) artworksCountObj).intValue();
                } else if (artworksCountObj instanceof Integer) {
                    artworksCount = (Integer) artworksCountObj;
                } else if (artworksCountObj instanceof Long) {
                    artworksCount = ((Long) artworksCountObj).intValue();
                } else {
                    artworksCount = Integer.valueOf(artworksCountObj.toString());
                }
                exhibition.setArtworksCount(artworksCount);
            }

            // Парсинг пользователя
            Map<String, Object> userData = (Map<String, Object>) data.get("user");
            if (userData != null) {
                com.example.vagmobile.model.User user = new com.example.vagmobile.model.User();

                // Безопасный парсинг id пользователя
                Object userIdObj = userData.get("id");
                if (userIdObj != null) {
                    long userId = 0;
                    if (userIdObj instanceof Double) {
                        userId = ((Double) userIdObj).longValue();
                    } else if (userIdObj instanceof Integer) {
                        userId = ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else {
                        userId = Long.valueOf(userIdObj.toString());
                    }
                    user.setId(userId);
                }

                user.setUsername((String) userData.get("username"));
                user.setEmail((String) userData.get("email"));
                exhibition.setUser(user);
            }

            return exhibition;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Artwork parseArtworkFromMap(Map<String, Object> data) {
        try {
            Artwork artwork = new Artwork();

            // Безопасный парсинг id работы
            Object idObj = data.get("id");
            if (idObj != null) {
                long id = 0;
                if (idObj instanceof Double) {
                    id = ((Double) idObj).longValue();
                } else if (idObj instanceof Integer) {
                    id = ((Integer) idObj).longValue();
                } else if (idObj instanceof Long) {
                    id = (Long) idObj;
                } else {
                    id = Long.valueOf(idObj.toString());
                }
                artwork.setId(id);
            }

            artwork.setTitle((String) data.get("title"));
            artwork.setDescription((String) data.get("description"));
            artwork.setImagePath((String) data.get("imagePath"));
            artwork.setStatus((String) data.get("status"));

            // Безопасный парсинг likes
            Object likesObj = data.get("likes");
            if (likesObj != null) {
                int likes = 0;
                if (likesObj instanceof Double) {
                    likes = ((Double) likesObj).intValue();
                } else if (likesObj instanceof Integer) {
                    likes = (Integer) likesObj;
                } else if (likesObj instanceof Long) {
                    likes = ((Long) likesObj).intValue();
                } else {
                    likes = Integer.valueOf(likesObj.toString());
                }
                artwork.setLikes(likes);
            }

            // Безопасный парсинг views
            Object viewsObj = data.get("views");
            if (viewsObj != null) {
                int views = 0;
                if (viewsObj instanceof Double) {
                    views = ((Double) viewsObj).intValue();
                } else if (viewsObj instanceof Integer) {
                    views = (Integer) viewsObj;
                } else if (viewsObj instanceof Long) {
                    views = ((Long) viewsObj).intValue();
                } else {
                    views = Integer.valueOf(viewsObj.toString());
                }
                artwork.setViews(views);
            }

            // Парсинг пользователя
            Map<String, Object> userData = (Map<String, Object>) data.get("user");
            if (userData != null) {
                com.example.vagmobile.model.User user = new com.example.vagmobile.model.User();

                // Безопасный парсинг id пользователя
                Object userIdObj = userData.get("id");
                if (userIdObj != null) {
                    long userId = 0;
                    if (userIdObj instanceof Double) {
                        userId = ((Double) userIdObj).longValue();
                    } else if (userIdObj instanceof Integer) {
                        userId = ((Integer) userIdObj).longValue();
                    } else if (userIdObj instanceof Long) {
                        userId = (Long) userIdObj;
                    } else {
                        userId = Long.valueOf(userIdObj.toString());
                    }
                    user.setId(userId);
                }

                user.setUsername((String) userData.get("username"));
                user.setEmail((String) userData.get("email"));
                artwork.setUser(user);
            }

            return artwork;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
