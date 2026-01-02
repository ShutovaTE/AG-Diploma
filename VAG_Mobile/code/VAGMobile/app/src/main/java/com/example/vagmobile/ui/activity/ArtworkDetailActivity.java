package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.Category;
import com.example.vagmobile.model.Comment;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.adapter.CommentAdapter;
import com.example.vagmobile.util.SharedPreferencesHelper;
import com.example.vagmobile.viewmodel.ArtworkViewModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ArtworkDetailActivity extends AppCompatActivity {

    private ArtworkViewModel artworkViewModel;
    private Artwork artwork;
    private Long artworkId;

    private ImageView ivArtwork;
    private TextView tvTitle, tvArtist, tvDescription, tvLikes, tvViews, tvCategories;
    private ImageButton btnLike;
    private Button btnComment;
    private ImageButton btnEdit, btnDelete;
    private EditText etComment;
    private LinearLayout layoutAuthorActions;
    private RecyclerView recyclerViewComments;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private CommentAdapter commentAdapter;
    private List<Comment> commentList = new ArrayList<>();
    private SharedPreferencesHelper prefs;
    private boolean isLikeInProgress = false;
    private SharedPreferences likePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artwork_detail);

        prefs = new SharedPreferencesHelper(this);
        artworkId = getIntent().getLongExtra("artwork_id", -1);
        likePrefs = getSharedPreferences("likes_prefs", MODE_PRIVATE);

        if (artworkId == -1) {
            Toast.makeText(this, "Публикация не найдена", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        observeViewModels();
        loadArtwork();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        ivArtwork = findViewById(R.id.ivArtwork);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvDescription = findViewById(R.id.tvDescription);
        tvLikes = findViewById(R.id.tvLikes);
        tvViews = findViewById(R.id.tvViews);
        tvCategories = findViewById(R.id.tvCategories);
        btnLike = findViewById(R.id.btnLike);
        btnComment = findViewById(R.id.btnComment);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        etComment = findViewById(R.id.etComment);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        progressBar = findViewById(R.id.progressBar);
        layoutAuthorActions = findViewById(R.id.layoutAuthorActions);

        btnLike.setOnClickListener(v -> toggleLike());
        btnComment.setOnClickListener(v -> addComment());
        btnEdit.setOnClickListener(v -> editArtwork());
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());

        if (!prefs.isLoggedIn()) {
            btnComment.setVisibility(View.GONE);
            etComment.setVisibility(View.GONE);
            // btnLike остается видимым для показа приглашения к авторизации
        }
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadArtwork);
        // Настраиваем цвета индикатора обновления
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(commentList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerViewComments.setLayoutManager(layoutManager);
        recyclerViewComments.setAdapter(commentAdapter);
        recyclerViewComments.setNestedScrollingEnabled(false);
    }

    private void observeViewModels() {
        artworkViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ArtworkViewModel.class);

        artworkViewModel.getArtworkResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Map<String, Object> artworkData = (Map<String, Object>) result.get("artwork");
                    if (artworkData != null) {
                        artwork = convertToArtwork(artworkData);

                        // НЕ проверяем сохраненное состояние из SharedPreferences
                        // Полагаемся только на данные с сервера
                        Log.d("LIKE_DEBUG", "Загружена публикация с сервера - artwork.isLiked(): " + artwork.isLiked());

                        updateUI();
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, "Не удалось загрузить публикацию: " + message, Toast.LENGTH_SHORT).show();
                }
            }
            swipeRefreshLayout.setRefreshing(false);
        });

        artworkViewModel.getToggleLikeResult().observe(this, result -> {
            isLikeInProgress = false;
            btnLike.setEnabled(true);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    // Обновляем данные из ответа сервера
                    Map<String, Object> artworkData = (Map<String, Object>) result.get("artwork");
                    if (artworkData != null) {
                        artwork = convertToArtwork(artworkData);
                        updateUI();
                    }

                    Toast.makeText(this,
                            artwork != null && artwork.isLiked() ? "Лайк добавлен" : "Лайк убран",
                            Toast.LENGTH_SHORT).show();
                } else {
                    String msg = result != null ? (String) result.get("message") : "Ошибка";
                    Toast.makeText(this, "Не удалось: " + msg, Toast.LENGTH_SHORT).show();

                    // Откатываем изменение если сервер вернул ошибку
                    if (artwork != null) {
                        artwork.setLiked(!artwork.isLiked());
                        artwork.setLikes(artwork.isLiked() ? artwork.getLikes() + 1 : artwork.getLikes() - 1);
                        updateLikeButton();
                        tvLikes.setText(String.valueOf(artwork.getLikes()));
                    }
                }
            }
        });

        artworkViewModel.getAddCommentResult().observe(this, result -> {
            btnComment.setEnabled(true);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, "Комментарий добавлен", Toast.LENGTH_SHORT).show();

                    Map<String, Object> artworkData = (Map<String, Object>) result.get("artwork");
                    if (artworkData != null) {
                        artwork = convertToArtwork(artworkData);
                        updateUI();
                    }

                    if (commentList.size() > 0) {
                        recyclerViewComments.smoothScrollToPosition(commentList.size() - 1);
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, "Ошибка: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });

        artworkViewModel.getDeleteResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, "Публикация успешно удалена", Toast.LENGTH_SHORT).show();
                    finish(); // Закрываем активность после успешного удаления
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, "Не удалось удалить публикацию: " + message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadArtwork() {
        progressBar.setVisibility(View.VISIBLE);
        artworkViewModel.getArtwork(artworkId);
    }

    private void updateUI() {
        if (artwork == null) return;

        tvTitle.setText(artwork.getTitle());
        tvDescription.setText(artwork.getDescription());
        tvLikes.setText(String.valueOf(artwork.getLikes()));

        if (artwork.getViews() > 0) {
            tvViews.setText(artwork.getViews() + " просмотров");
            tvViews.setVisibility(View.VISIBLE);
        }

        Log.d("LIKE_DEBUG", "=== updateUI() вызван ===");
        Log.d("LIKE_DEBUG", "artwork.isLiked() = " + artwork.isLiked());
        Log.d("LIKE_DEBUG", "artwork.getLikes() = " + artwork.getLikes());

        if (artwork.hasCategories()) {
            tvCategories.setText("Категории: " + artwork.getCategoriesString());
            tvCategories.setVisibility(View.VISIBLE);
        } else {
            tvCategories.setText("Категории: Без категории");
            tvCategories.setVisibility(View.VISIBLE);
        }

        if (artwork.getUser() != null && artwork.getUser().getUsername() != null) {
            tvArtist.setText("Художник: " + artwork.getUser().getUsername());
        } else {
            tvArtist.setText("Художник: Неизвестно");
        }

        if (artwork.getImagePath() != null && !artwork.getImagePath().isEmpty()) {
            String imagePath = artwork.getImagePath();
            if (imagePath.startsWith("/")) {
                imagePath = imagePath.substring(1);
            }
//            String imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imagePath;
            String imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imagePath;
            Log.d("ArtworkDetail", "Загрузка изображения с URL: " + imageUrl);
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(ivArtwork);

            // Добавляем клик для открытия полноэкранного просмотра
            ivArtwork.setOnClickListener(v -> {
                Intent intent = new Intent(this, FullscreenImageActivity.class);
                intent.putExtra("image_url", imageUrl);
                startActivity(intent);
            });
        } else {
            Log.d("ArtworkDetail", "Путь к изображению пуст");
            ivArtwork.setOnClickListener(null); // Убираем клик если нет изображения
        }

        if (artwork.getComments() != null) {
            Log.d("COMMENTS_DEBUG", "=== updateUI: Обработка комментариев ===");
            Log.d("COMMENTS_DEBUG", "artwork.getComments().size(): " + artwork.getComments().size());

            for (Comment c : artwork.getComments()) {
                Log.d("COMMENTS_DEBUG", "Комментарий ID: " + c.getId() + ", content: " + c.getContent());
            }

            List<Comment> tempComments = new ArrayList<>();

            // Сохраняем только временные комментарии (еще не отправленные на сервер)
            for (Comment comment : commentList) {
                if (comment.getId() == null || comment.getId() == 0) {
                    tempComments.add(comment);
                }
            }

            Log.d("COMMENTS_DEBUG", "tempComments.size(): " + tempComments.size());
            Log.d("COMMENTS_DEBUG", "commentList.size() до очистки: " + commentList.size());

            commentList.clear();

            // Добавляем комментарии от сервера, избегая дублирования
            for (Comment serverComment : artwork.getComments()) {
                boolean alreadyExists = false;
                for (Comment existingComment : commentList) {
                    if (existingComment.getId() != null && existingComment.getId().equals(serverComment.getId())) {
                        alreadyExists = true;
                        break;
                    }
                }
                if (!alreadyExists) {
                    commentList.add(serverComment);
                }
            }

            // Добавляем временные комментарии
            commentList.addAll(tempComments);

            Log.d("COMMENTS_DEBUG", "commentList.size() после добавления: " + commentList.size());

            commentAdapter.notifyDataSetChanged();

            if (!commentList.isEmpty()) {
                recyclerViewComments.smoothScrollToPosition(commentList.size() - 1);
            }
        } else {
            Log.d("COMMENTS_DEBUG", "artwork.getComments() is null");
        }

        updateLikeButton();
        updateAuthorActions();
        updateCommentVisibility();
    }

    private void updateLikeButton() {
        if (artwork == null) return;

        Log.d("LIKE_DEBUG", "updateLikeButton - artworkId: " + artwork.getId() +
                ", artwork.isLiked(): " + artwork.isLiked() +
                ", artwork.likes: " + artwork.getLikes());

        if (prefs.isLoggedIn()) {
            // Для авторизованных пользователей показываем реальное состояние лайка
            if (artwork.isLiked()) {
                btnLike.setImageResource(R.drawable.ic_heart_filled);
                btnLike.setColorFilter(Color.RED);
                Log.d("LIKE_DEBUG", "Setting heart to RED (liked)");
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline);
                btnLike.setColorFilter(Color.GRAY);
                Log.d("LIKE_DEBUG", "Setting heart to GRAY (not liked)");
            }
        } else {
            // Для неавторизованных пользователей всегда показываем outline сердце
            btnLike.setImageResource(R.drawable.ic_heart_outline);
            btnLike.setColorFilter(Color.GRAY);
            Log.d("LIKE_DEBUG", "Setting heart to GRAY outline (not logged in)");
        }
    }

    private void updateAuthorActions() {
        if (artwork == null || !prefs.isLoggedIn()) {
            if (layoutAuthorActions != null) {
                layoutAuthorActions.setVisibility(View.GONE);
            }
            return;
        }

        Long currentUserId = prefs.getUserId();
        Long artworkAuthorId = artwork.getUser() != null ? artwork.getUser().getId() : null;

        if (currentUserId != null && currentUserId.equals(artworkAuthorId)) {
            // Показываем кнопки действий автора
            layoutAuthorActions.setVisibility(View.VISIBLE);

            // Кнопка "Редактировать" только для отклоненных публикаций
            if ("REJECTED".equals(artwork.getStatus())) {
                btnEdit.setVisibility(View.VISIBLE);
            } else {
                btnEdit.setVisibility(View.GONE);
            }

            // Кнопка "Удалить" всегда видима для автора
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            // Скрываем кнопки действий для не-авторов
            layoutAuthorActions.setVisibility(View.GONE);
        }
    }

    private void toggleLike() {
        if (!prefs.isLoggedIn()) {
            Toast.makeText(this, "Авторизуйтесь, чтобы оценить публикацию", Toast.LENGTH_SHORT).show();
            return;
        }

        if (artwork == null) {
            Toast.makeText(this, "Данные публикации не загружены", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isLikeInProgress) {
            return;
        }

        // Получаем текущее состояние лайка
        boolean currentLiked = artwork.isLiked();

        Log.d("LIKE_DEBUG", "toggleLike - artworkId: " + artworkId +
                ", artwork.isLiked(): " + currentLiked +
                ", artwork.getLikes(): " + artwork.getLikes());

        // Временно обновляем UI для быстрого отклика
        boolean newLikedState = !currentLiked;
        artwork.setLiked(newLikedState);
        artwork.setLikes(newLikedState ? artwork.getLikes() + 1 : artwork.getLikes() - 1);

        updateLikeButton();
        tvLikes.setText(String.valueOf(artwork.getLikes()));

        isLikeInProgress = true;
        btnLike.setEnabled(false);

        // Отправляем запрос на сервер
        artworkViewModel.toggleLike(artworkId, !newLikedState); // Передаем текущее состояние (до изменения)
    }

    private void addComment() {
        if (!prefs.isLoggedIn()) {
            Toast.makeText(this, "Пожалуйста, войдите в систему чтобы комментировать", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etComment.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите комментарий", Toast.LENGTH_SHORT).show();
            return;
        }

        etComment.setText("");
        btnComment.setEnabled(false);
        artworkViewModel.addComment(artworkId, content);
    }

    private void editArtwork() {
        if (artwork == null) {
            Toast.makeText(this, "Данные публикации не загружены", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, EditArtworkActivity.class);
        intent.putExtra("artwork_id", artwork.getId());
        startActivity(intent);
    }

    private void showDeleteConfirmationDialog() {
        if (artwork == null) {
            Toast.makeText(this, "Данные публикации не загружены", Toast.LENGTH_SHORT).show();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_artwork))
                .setMessage(getString(R.string.confirm_delete_artwork, artwork.getTitle()))
                .setPositiveButton("Удалить", (dialog, which) -> deleteArtwork())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteArtwork() {
        if (artworkId != null) {
            artworkViewModel.deleteArtwork(artworkId);
        }
    }

    private void saveLikeState(Long artworkId, boolean liked) {
        SharedPreferences.Editor editor = likePrefs.edit();
        editor.putBoolean("like_" + artworkId, liked);
        editor.apply();
    }

    private boolean getLikeState(Long artworkId) {
        return likePrefs.getBoolean("like_" + artworkId, false);
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

        artwork.setTitle((String) artworkData.get("title"));
        artwork.setDescription((String) artworkData.get("description"));
        artwork.setImagePath((String) artworkData.get("imagePath"));

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
        }

        artwork.setStatus((String) artworkData.get("status"));

        // ИСПРАВЛЕНО: Получаем состояние лайка
        boolean liked = false;

        Long currentUserId = prefs.getUserId();

        // 1. Проверяем поле "liked"
        Object likedObj = artworkData.get("liked");
        if (likedObj != null) {
            if (likedObj instanceof Boolean) {
                liked = (Boolean) likedObj;
            } else if (likedObj instanceof String) {
                liked = "true".equalsIgnoreCase((String) likedObj);
            } else if (likedObj instanceof Integer) {
                liked = ((Integer) likedObj) == 1;
            } else if (likedObj instanceof Long) {
                liked = ((Long) likedObj) == 1;
            }
        }

        // 2. Проверяем массив пользователей, которые лайкнули
        if (!liked && currentUserId != null) {
            Object likedByUsersObj = artworkData.get("likedByUsers");
            Object likedByObj = artworkData.get("likedBy");

            List<?> likedByList = null;
            if (likedByUsersObj instanceof List) {
                likedByList = (List<?>) likedByUsersObj;
            } else if (likedByObj instanceof List) {
                likedByList = (List<?>) likedByObj;
            }

            if (likedByList != null) {
                // Проверяем, есть ли текущий пользователь в списке лайкнувших
                for (Object userObj : likedByList) {
                    Long userId = extractUserId(userObj);
                    if (userId != null && userId.equals(currentUserId)) {
                        liked = true;
                        break;
                    }
                }
            }
        }

        // 3. Проверяем поле "likedByCurrentUser"
        Object likedByCurrentUserObj = artworkData.get("likedByCurrentUser");
        if (!liked && likedByCurrentUserObj != null) {
            if (likedByCurrentUserObj instanceof Boolean) {
                liked = (Boolean) likedByCurrentUserObj;
            } else if (likedByCurrentUserObj instanceof String) {
                liked = "true".equalsIgnoreCase((String) likedByCurrentUserObj);
            }
        }

        artwork.setLiked(liked);
        Log.d("LIKE_DEBUG", "convertToArtwork - artworkId: " + artwork.getId() +
                ", liked: " + liked +
                ", likedByCurrentUser field: " + likedByCurrentUserObj +
                ", liked field: " + likedObj);

        if (artworkData.get("user") != null) {
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

            user.setUsername((String) userData.get("username"));
            user.setEmail((String) userData.get("email"));
            artwork.setUser(user);
        } else {
            User unknownUser = new User();
            unknownUser.setUsername("Неизвестный художник");
            artwork.setUser(unknownUser);
        }

        if (artworkData.get("comments") != null) {
            List<Map<String, Object>> commentsData = (List<Map<String, Object>>) artworkData.get("comments");
            Log.d("COMMENTS_DEBUG", "convertToArtwork: commentsData.size(): " + commentsData.size());
            List<Comment> comments = new ArrayList<>();
            for (Map<String, Object> commentData : commentsData) {
                Comment comment = convertToComment(commentData);
                comments.add(comment);
                Log.d("COMMENTS_DEBUG", "convertToArtwork: Добавлен комментарий ID: " + comment.getId());
            }
            artwork.setComments(comments);
            Log.d("COMMENTS_DEBUG", "convertToArtwork: Всего комментариев: " + comments.size());
        } else {
            Log.d("COMMENTS_DEBUG", "convertToArtwork: commentsData is null");
        }

        if (artworkData.get("categories") != null) {
            List<Map<String, Object>> categoriesData = (List<Map<String, Object>>) artworkData.get("categories");
            List<Category> categories = new ArrayList<>();
            for (Map<String, Object> categoryData : categoriesData) {
                Category category = convertToCategory(categoryData);
                categories.add(category);
            }
            artwork.setCategories(categories);
        }

        return artwork;
    }

    // ДОБАВЛЕНО: Вспомогательный метод для извлечения ID пользователя
    private Long extractUserId(Object userObj) {
        if (userObj instanceof Map) {
            Map<String, Object> userMap = (Map<String, Object>) userObj;
            Object userIdObj = userMap.get("id");
            if (userIdObj != null) {
                if (userIdObj instanceof Double) {
                    return ((Double) userIdObj).longValue();
                } else if (userIdObj instanceof Integer) {
                    return ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof Long) {
                    return (Long) userIdObj;
                }
            }
        } else if (userObj instanceof Number) {
            if (userObj instanceof Double) {
                return ((Double) userObj).longValue();
            } else if (userObj instanceof Integer) {
                return ((Integer) userObj).longValue();
            } else if (userObj instanceof Long) {
                return (Long) userObj;
            }
        }
        return null;
    }
    private Comment convertToComment(Map<String, Object> commentData) {
        Comment comment = new Comment();

        if (commentData.get("id") != null) {
            if (commentData.get("id") instanceof Double) {
                comment.setId(((Double) commentData.get("id")).longValue());
            } else if (commentData.get("id") instanceof Long) {
                comment.setId((Long) commentData.get("id"));
            } else if (commentData.get("id") instanceof Integer) {
                comment.setId(((Integer) commentData.get("id")).longValue());
            }
        }

        comment.setContent((String) commentData.get("content"));

        if (commentData.get("user") != null) {
            Map<String, Object> userData = (Map<String, Object>) commentData.get("user");
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
            comment.setUser(user);
        }

        if (commentData.get("dateCreated") != null) {
            Object dateObj = commentData.get("dateCreated");
            if (dateObj instanceof String) {
                String dateString = (String) dateObj;
                try {
                    SimpleDateFormat[] formats = {
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault()),
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    };

                    Date date = null;
                    for (SimpleDateFormat format : formats) {
                        try {
                            date = format.parse(dateString);
                            break;
                        } catch (ParseException e) {
                            // Пропускаем и пробуем следующий формат
                        }
                    }

                    if (date != null) {
                        comment.setDateCreated(date);
                    } else {
                        comment.setDateCreated(new Date());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    comment.setDateCreated(new Date());
                }
            } else if (dateObj instanceof Long) {
                comment.setDateCreated(new Date((Long) dateObj));
            }
        } else {
            comment.setDateCreated(new Date());
        }

        return comment;
    }

    private Category convertToCategory(Map<String, Object> categoryData) {
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

        category.setName(categoryData.get("name") != null ? categoryData.get("name").toString() : "Без названия");
        category.setDescription(categoryData.get("description") != null ? categoryData.get("description").toString() : "");

        Object countObj = categoryData.get("approvedArtworksCount");
        if (countObj != null) {
            if (countObj instanceof Double) {
                category.setApprovedArtworksCount(((Double) countObj).longValue());
            } else if (countObj instanceof Integer) {
                category.setApprovedArtworksCount(((Integer) countObj).longValue());
            } else if (countObj instanceof Long) {
                category.setApprovedArtworksCount((Long) countObj);
            }
        } else {
            category.setApprovedArtworksCount(0L);
        }

        return category;
    }

    private void updateCommentVisibility() {
        if (artwork == null) return;

        String status = artwork.getStatus();

        // Скрываем комментарии для работ со статусом PENDING или REJECTED
        if ("PENDING".equals(status) || "REJECTED".equals(status)) {
            btnComment.setVisibility(View.GONE);
            etComment.setVisibility(View.GONE);
            recyclerViewComments.setVisibility(View.GONE);
        } else {
            // Для APPROVED работ показываем комментарии если пользователь авторизован
            if (prefs.isLoggedIn()) {
                btnComment.setVisibility(View.VISIBLE);
                etComment.setVisibility(View.VISIBLE);
            } else {
                btnComment.setVisibility(View.GONE);
                etComment.setVisibility(View.GONE);
            }
            recyclerViewComments.setVisibility(View.VISIBLE);
        }
    }
}