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
import com.example.vagmobile.model.Category;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.adapter.ArtworkAdapter;
import com.example.vagmobile.viewmodel.CategoryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoryDetailActivity extends AppCompatActivity {

    private CategoryViewModel categoryViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvCategoryName, tvCategoryDescription, tvArtworksCount, tvEmpty;
    private ArtworkAdapter artworkAdapter;
    private List<Artwork> artworkList = new ArrayList<>();

    private Long categoryId;
    private String categoryName;
    private String categoryDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        categoryId = getIntent().getLongExtra("category_id", -1);
        categoryName = getIntent().getStringExtra("category_name");
        categoryDescription = getIntent().getStringExtra("category_description");

        if (categoryId == -1) {
            Toast.makeText(this, "Категория не найдена", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        observeViewModels();
        loadCategoryArtworks();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvCategoryDescription = findViewById(R.id.tvCategoryDescription);
        tvArtworksCount = findViewById(R.id.tvArtworksCount);
        tvEmpty = findViewById(R.id.tvEmpty);

        if (categoryName != null) {
            tvCategoryName.setText(categoryName);
        }

        if (categoryDescription != null && !categoryDescription.isEmpty()) {
            tvCategoryDescription.setText(categoryDescription);
        } else {
            tvCategoryDescription.setText("Нет описания");
        }
    }

    private void setupRecyclerView() {
        artworkAdapter = new ArtworkAdapter(artworkList, new ArtworkAdapter.OnArtworkClickListener() {
            @Override
            public void onArtworkClick(Artwork artwork) {
                Intent intent = new Intent(CategoryDetailActivity.this, ArtworkDetailActivity.class);
                intent.putExtra("artwork_id", artwork.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Artwork artwork) {
                // Не используется для просмотра категорий
            }

            @Override
            public void onDeleteClick(Artwork artwork) {
                // Не используется для просмотра категорий
            }
        }, false); // false - не показываем кнопки действий для просмотра категорий

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(artworkAdapter);
    }

    private void observeViewModels() {
        categoryViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(CategoryViewModel.class);

        categoryViewModel.getCategoryArtworksResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");
                    if (artworksData != null && !artworksData.isEmpty()) {
                        int totalItems = 0;
                        try {
                            Object totalItemsObj = result.get("totalItems");
                            if (totalItemsObj instanceof Double) {
                                totalItems = ((Double) totalItemsObj).intValue();
                            } else if (totalItemsObj instanceof Integer) {
                                totalItems = (Integer) totalItemsObj;
                            } else if (totalItemsObj instanceof Long) {
                                totalItems = ((Long) totalItemsObj).intValue();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (totalItems <= 0) {
                            totalItems = artworksData.size();
                        }

                        updateArtworksCount(totalItems);

                        tvEmpty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        artworkList.clear();
                        for (Map<String, Object> artworkData : artworksData) {
                            Artwork artwork = convertToArtwork(artworkData);
                            artworkList.add(artwork);
                        }
                        artworkAdapter.notifyDataSetChanged();
                    } else {
                        updateArtworksCount(0);
                        tvEmpty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, "Не удалось загрузить публикации: " + message, Toast.LENGTH_SHORT).show();
                    updateArtworksCount(0);
                }
            } else {
                Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show();
                updateArtworksCount(0);
            }
        });
    }

    private void updateArtworksCount(int count) {
        String countText;
        if (count == 0) {
            countText = "Нет публикаций";
        } else if (count % 10 == 1 && count % 100 != 11) {
            countText = count + " публикация";
        } else if (count % 10 >= 2 && count % 10 <= 4 && (count % 100 < 10 || count % 100 >= 20)) {
            countText = count + " публикации";
        } else {
            countText = count + " публикаций";
        }
        tvArtworksCount.setText(countText);
    }

    private void loadCategoryArtworks() {
        progressBar.setVisibility(View.VISIBLE);
        categoryViewModel.getCategoryArtworks(categoryId, 0, 20);
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

        artwork.setTitle(artworkData.get("title") != null ? artworkData.get("title").toString() : getString(R.string.no_title));
        artwork.setDescription(artworkData.get("description") != null ? artworkData.get("description").toString() : "Нет описания");
        artwork.setImagePath(artworkData.get("imagePath") != null ? artworkData.get("imagePath").toString() : "");

        if (artworkData.get("likes") != null) {
            if (artworkData.get("likes") instanceof Double) {
                artwork.setLikes(((Double) artworkData.get("likes")).intValue());
            } else if (artworkData.get("likes") instanceof Integer) {
                artwork.setLikes((Integer) artworkData.get("likes"));
            } else if (artworkData.get("likes") instanceof Long) {
                artwork.setLikes(((Long) artworkData.get("likes")).intValue());
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
                }
            }

            user.setUsername(userData.get("username") != null ? userData.get("username").toString() : "Неизвестный автор");
            artwork.setUser(user);
        } else {
            User unknownUser = new User();
            unknownUser.setUsername("Неизвестный автор");
            artwork.setUser(unknownUser);
        }

        if (artworkData.get("categories") != null && artworkData.get("categories") instanceof List) {
            List<Map<String, Object>> categoriesData = (List<Map<String, Object>>) artworkData.get("categories");
            List<Category> categories = new ArrayList<>();
            for (Map<String, Object> categoryData : categoriesData) {
                Category category = new Category();

                Object idObj = categoryData.get("id");
                if (idObj != null) {
                    if (idObj instanceof Double) {
                        category.setId(((Double) idObj).longValue());
                    } else if (idObj instanceof Integer) {
                        category.setId(((Integer) idObj).longValue());
                    } else if (idObj instanceof Long) {
                        category.setId((Long) idObj);
                    }
                }

                category.setName(categoryData.get("name") != null ? categoryData.get("name").toString() : "");
                category.setDescription(categoryData.get("description") != null ? categoryData.get("description").toString() : "");
                categories.add(category);
            }
            artwork.setCategories(categories);
        }

        return artwork;
    }
}