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
import com.example.vagmobile.model.Exhibition;
import com.example.vagmobile.ui.adapter.ExhibitionAdapter;
import com.example.vagmobile.viewmodel.ExhibitionViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExhibitionListActivity extends AppCompatActivity {

    private ExhibitionViewModel exhibitionViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvTitle;
    private ExhibitionAdapter exhibitionAdapter;
    private List<Exhibition> exhibitionList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMorePages = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exhibition_list);

        initViews();
        setupRecyclerView();
        observeViewModels();
        loadExhibitions();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvTitle = findViewById(R.id.tvTitle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        swipeRefreshLayout.setOnRefreshListener(this::refreshExhibitions);
    }

    private void setupRecyclerView() {
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);

        exhibitionAdapter = new ExhibitionAdapter(exhibitionList, exhibition -> {
            Intent intent = new Intent(ExhibitionListActivity.this, ExhibitionDetailActivity.class);
            intent.putExtra("exhibition_id", exhibition.getId());
            startActivity(intent);
        });

        recyclerView.setAdapter(exhibitionAdapter);

        // Добавляем пагинацию при прокрутке
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMorePages &&
                        (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadNextPage();
                    }
                }
            }
        });
    }

    private void observeViewModels() {
        exhibitionViewModel = new ViewModelProvider(this).get(ExhibitionViewModel.class);

        exhibitionViewModel.getExhibitionsResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> exhibitionsData = (List<Map<String, Object>>) result.get("exhibitions");

                    if (currentPage == 0) {
                        exhibitionList.clear();
                    }

                    if (exhibitionsData != null && !exhibitionsData.isEmpty()) {
                        for (Map<String, Object> exhibitionData : exhibitionsData) {
                            Exhibition exhibition = parseExhibitionFromMap(exhibitionData);
                            if (exhibition != null) {
                                exhibitionList.add(exhibition);
                            }
                        }
                        exhibitionAdapter.notifyDataSetChanged();
                    } else {
                        hasMorePages = false;
                    }

                    // Проверяем, есть ли ещё страницы
                    Object totalPagesObj = result.get("totalPages");
                    if (totalPagesObj != null) {
                        int totalPages = 0;
                        if (totalPagesObj instanceof Double) {
                            totalPages = ((Double) totalPagesObj).intValue();
                        } else if (totalPagesObj instanceof Integer) {
                            totalPages = (Integer) totalPagesObj;
                        } else if (totalPagesObj instanceof Long) {
                            totalPages = ((Long) totalPagesObj).intValue();
                        }

                        if (currentPage + 1 >= totalPages) {
                            hasMorePages = false;
                        }
                    }
                } else {
                    String message = (String) result.get("message");
                    if (message != null) {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            isLoading = false;
        });
    }

    private void loadExhibitions() {
        if (isLoading) return;

        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);
        exhibitionViewModel.getExhibitions(currentPage, 20);
    }

    private void loadNextPage() {
        if (!hasMorePages || isLoading) return;

        currentPage++;
        loadExhibitions();
    }

    private void refreshExhibitions() {
        currentPage = 0;
        hasMorePages = true;
        loadExhibitions();
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

            // Парсинг даты
            if (data.get("createdAt") != null) {
                exhibition.setCreatedAt(new java.util.Date());
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

            // Парсинг первой работы
            Map<String, Object> firstArtworkData = (Map<String, Object>) data.get("firstArtwork");
            if (firstArtworkData != null) {
                com.example.vagmobile.model.Artwork firstArtwork = new com.example.vagmobile.model.Artwork();

                // Безопасный парсинг id первой работы
                Object artworkIdObj = firstArtworkData.get("id");
                if (artworkIdObj != null) {
                    long artworkId = 0;
                    if (artworkIdObj instanceof Double) {
                        artworkId = ((Double) artworkIdObj).longValue();
                    } else if (artworkIdObj instanceof Integer) {
                        artworkId = ((Integer) artworkIdObj).longValue();
                    } else if (artworkIdObj instanceof Long) {
                        artworkId = (Long) artworkIdObj;
                    } else {
                        artworkId = Long.valueOf(artworkIdObj.toString());
                    }
                    firstArtwork.setId(artworkId);
                }

                firstArtwork.setTitle((String) firstArtworkData.get("title"));
                firstArtwork.setImagePath((String) firstArtworkData.get("imagePath"));
                exhibition.setFirstArtwork(firstArtwork);
            }

            return exhibition;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
