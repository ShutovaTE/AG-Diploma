package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.Category;
import com.example.vagmobile.model.User;
import com.example.vagmobile.viewmodel.ArtworkViewModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AdminArtworkDetailActivity extends AppCompatActivity {

    private ArtworkViewModel artworkViewModel;
    private ProgressBar progressBar;
    private LinearLayout contentLayout;
    private Long artworkId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_artwork_detail);

        artworkId = getIntent().getLongExtra("artwork_id", -1);

        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);

        if (artworkId != -1) {
            initViewModel();
            loadArtworkDetailsForAdmin(artworkId);
        } else {
            Toast.makeText(this, "ID публикации недоступен", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViewModel() {
        artworkViewModel = new ViewModelProvider(this).get(ArtworkViewModel.class);

        artworkViewModel.getArtworkForAdminResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Map<String, Object> artworkData = (Map<String, Object>) result.get("artwork");
                    if (artworkData != null) {
                        Artwork artwork = convertToArtwork(artworkData);
                        displayArtworkDetails(artwork);
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, "Не удалось загрузить детали публикации: " + message, Toast.LENGTH_SHORT).show();

                    if (message != null && message.contains(getString(R.string.access_denied))) {
                        loadArtworkWithRegularEndpoint(artworkId);
                    }
                }
            }
        });

        artworkViewModel.getArtworkResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            contentLayout.setVisibility(View.VISIBLE);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Map<String, Object> artworkData = (Map<String, Object>) result.get("artwork");
                    if (artworkData != null) {
                        Artwork artwork = convertToArtwork(artworkData);
                        displayArtworkDetails(artwork);
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, "Не удалось загрузить публикацию: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadArtworkDetailsForAdmin(Long artworkId) {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
        artworkViewModel.getArtworkForAdmin(artworkId);
    }

    private void loadArtworkWithRegularEndpoint(Long artworkId) {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);
        artworkViewModel.getArtwork(artworkId);
    }

    private Artwork convertToArtwork(Map<String, Object> artworkData) {
        Artwork artwork = new Artwork();

        Object idObj = artworkData.get("id");
        if (idObj != null) {
            if (idObj instanceof Double) {
                artwork.setId(((Double) idObj).longValue());
            } else if (idObj instanceof Integer) {
                artwork.setId(((Integer) idObj).longValue());
            } else if (idObj instanceof Long) {
                artwork.setId((Long) idObj);
            }
        }

        artwork.setTitle(artworkData.get("title") != null ? artworkData.get("title").toString() : getString(R.string.no_title));
        artwork.setDescription(artworkData.get("description") != null ? artworkData.get("description").toString() : "Нет описания");
        artwork.setImagePath(artworkData.get("imagePath") != null ? artworkData.get("imagePath").toString() : "");
        artwork.setStatus(artworkData.get("status") != null ? artworkData.get("status").toString() : "UNKNOWN");

        Object likesObj = artworkData.get("likes");
        if (likesObj != null) {
            if (likesObj instanceof Double) {
                artwork.setLikes(((Double) likesObj).intValue());
            } else if (likesObj instanceof Integer) {
                artwork.setLikes((Integer) likesObj);
            } else if (likesObj instanceof Long) {
                artwork.setLikes(((Long) likesObj).intValue());
            }
        } else {
            artwork.setLikes(0);
        }

        Object viewsObj = artworkData.get("views");
        if (viewsObj != null) {
            if (viewsObj instanceof Double) {
                artwork.setViews(((Double) viewsObj).intValue());
            } else if (viewsObj instanceof Integer) {
                artwork.setViews((Integer) viewsObj);
            } else if (viewsObj instanceof Long) {
                artwork.setViews(((Long) viewsObj).intValue());
            }
        } else {
            artwork.setViews(0);
        }

        if (artworkData.get("user") != null && artworkData.get("user") instanceof Map) {
            Map<String, Object> userData = (Map<String, Object>) artworkData.get("user");
            User user = new User();

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

            user.setUsername(userData.get("username") != null ? userData.get("username").toString() : "Неизвестно");
            user.setEmail(userData.get("email") != null ? userData.get("email").toString() : "Н/Д");
            artwork.setUser(user);
        } else {
            User defaultUser = new User();
            defaultUser.setUsername("Неизвестно");
            defaultUser.setEmail("Н/Д");
            artwork.setUser(defaultUser);
        }

        if (artworkData.get("categories") != null && artworkData.get("categories") instanceof List) {
            List<Map<String, Object>> categoriesData = (List<Map<String, Object>>) artworkData.get("categories");
            List<Category> categories = new ArrayList<>();
            for (Map<String, Object> categoryData : categoriesData) {
                Category category = new Category();

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

                category.setName(categoryData.get("name") != null ? categoryData.get("name").toString() : "Без названия");
                category.setDescription(categoryData.get("description") != null ? categoryData.get("description").toString() : "");
                categories.add(category);
            }
            artwork.setCategories(categories);
        } else {
            artwork.setCategories(new ArrayList<>());
        }

        return artwork;
    }

    private void displayArtworkDetails(Artwork artwork) {
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvDescription = findViewById(R.id.tvDescription);
        TextView tvArtist = findViewById(R.id.tvArtist);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvLikes = findViewById(R.id.tvLikes);
        TextView tvViews = findViewById(R.id.tvViews);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        TextView tvUserId = findViewById(R.id.tvUserId);
        TextView tvCategories = findViewById(R.id.tvCategories);
        ImageView ivArtwork = findViewById(R.id.ivArtwork);

        System.out.println("AdminArtworkDetailActivity: Отображение публикации - " + artwork.getTitle());
        System.out.println("AdminArtworkDetailActivity: Пользователь - " + artwork.getUser());
        if (artwork.getUser() != null) {
            System.out.println("AdminArtworkDetailActivity: ID пользователя - " + artwork.getUser().getId());
            System.out.println("AdminArtworkDetailActivity: Имя пользователя - " + artwork.getUser().getUsername());
            System.out.println("AdminArtworkDetailActivity: Email - " + artwork.getUser().getEmail());
        }
        System.out.println("AdminArtworkDetailActivity: Путь к изображению - " + artwork.getImagePath());
        System.out.println("AdminArtworkDetailActivity: Описание - " + artwork.getDescription());
        System.out.println("AdminArtworkDetailActivity: Категории - " + (artwork.hasCategories() ? artwork.getCategoriesString() : "Нет категорий"));

        tvTitle.setText(artwork.getTitle());
        tvDescription.setText(artwork.getDescription());
        tvStatus.setText("Статус: " + artwork.getStatus());
        tvLikes.setText("Лайки: " + artwork.getLikes());
        tvViews.setText("Просмотры: " + artwork.getViews());

        if (artwork.hasCategories()) {
            tvCategories.setText("Категории: " + artwork.getCategoriesString());
            tvCategories.setVisibility(View.VISIBLE);
        } else {
            tvCategories.setText("Категории: Без категории");
            tvCategories.setVisibility(View.VISIBLE);
        }

        if (artwork.getUser() != null) {
            tvArtist.setText("Художник: " + artwork.getUser().getUsername());
            tvUserEmail.setText("Email: " + artwork.getUser().getEmail());
            tvUserId.setText("ID пользователя: " + (artwork.getUser().getId() != null ? artwork.getUser().getId().toString() : "Н/Д"));
        } else {
            tvArtist.setText("Художник: Неизвестно");
            tvUserEmail.setText("Email: Н/Д");
            tvUserId.setText("ID пользователя: Н/Д");
        }

        if (artwork.getImagePath() != null && !artwork.getImagePath().isEmpty()) {
            String imagePath = artwork.getImagePath();
            if (imagePath.startsWith("/")) {
                imagePath = imagePath.substring(1);
            }
//            String imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imagePath;
            String imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imagePath;
            System.out.println("AdminArtworkDetailActivity: Загрузка изображения с URL: " + imageUrl);

            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_error_image)
                    .into(ivArtwork);

            // Добавляем клик для открытия полноэкранного просмотра
            ivArtwork.setOnClickListener(v -> {
                Intent intent = new Intent(this, FullscreenImageActivity.class);
                intent.putExtra("image_url", imageUrl);
                startActivity(intent);
            });
        } else {
            System.out.println("AdminArtworkDetailActivity: Путь к изображению пуст");
            ivArtwork.setImageResource(R.drawable.ic_image_placeholder);
            ivArtwork.setOnClickListener(null); // Убираем клик если нет изображения
        }
    }
}