package com.example.vagmobile.ui.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.bumptech.glide.Glide;
import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.Category;
import com.example.vagmobile.util.ImageUtils;
import com.example.vagmobile.viewmodel.ArtworkViewModel;
import com.example.vagmobile.viewmodel.CategoryViewModel;
import com.github.dhaval2404.imagepicker.ImagePicker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;

public class EditArtworkActivity extends AppCompatActivity {
    private static final int IMAGE_PICKER_REQUEST_CODE = 1002;

    private ArtworkViewModel artworkViewModel;
    private CategoryViewModel categoryViewModel;

    private EditText etTitle, etDescription;
    private ImageView ivArtworkImage;
    private Button btnSelectImage, btnUpdateArtwork;
    private ProgressBar progressBar;
    private ChipGroup chipContainer;
    private AutoCompleteTextView autoCompleteCategories;
    private TextView tvSelectedCategories;

    private Uri selectedImageUri;
    private Artwork artwork;
    private Long artworkId;
    private List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<Category> categoryAdapter;
    private List<Long> selectedCategoryIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_artwork);

        // Получаем ID публикации для редактирования
        artworkId = getIntent().getLongExtra("artwork_id", -1);
        if (artworkId == -1) {
            Toast.makeText(this, "Публикация не найдена", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        artworkViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ArtworkViewModel.class);
        categoryViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(CategoryViewModel.class);

        initViews();
        setupClickListeners();
        setupCategorySelection();
        loadCategories();
        loadArtwork();
        observeViewModels();

        // Изменяем заголовок
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Редактирование публикации");
        }

        // Изменяем текст кнопки
        btnUpdateArtwork.setText("Обновить публикацию");
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        ivArtworkImage = findViewById(R.id.ivArtworkImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnUpdateArtwork = findViewById(R.id.btnCreateArtwork);
        progressBar = findViewById(R.id.progressBar);
        chipContainer = findViewById(R.id.chipContainer);
        autoCompleteCategories = findViewById(R.id.autoCompleteCategories);
        tvSelectedCategories = findViewById(R.id.tvSelectedCategories);

        btnUpdateArtwork.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        updateSelectedCategoriesText();
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnUpdateArtwork.setOnClickListener(v -> updateArtwork());

        View rootLayout = findViewById(android.R.id.content);
        rootLayout.setOnClickListener(v -> {
            etTitle.clearFocus();
            etDescription.clearFocus();
            autoCompleteCategories.clearFocus();
        });
    }

    private void setupCategorySelection() {
        categoryAdapter = new ArrayAdapter<Category>(this,
                android.R.layout.simple_dropdown_item_1line, categoryList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                Category category = getItem(position);
                if (category != null) {
                    textView.setText(category.getName());
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                Category category = getItem(position);
                if (category != null) {
                    textView.setText(category.getName());
                    if (selectedCategoryIds.contains(category.getId())) {
                        textView.setTextColor(Color.GRAY);
                        textView.setBackgroundColor(Color.LTGRAY);
                    } else {
                        textView.setTextColor(Color.BLACK);
                        textView.setBackgroundColor(Color.WHITE);
                    }
                }
                return view;
            }
        };

        autoCompleteCategories.setAdapter(categoryAdapter);
        autoCompleteCategories.setThreshold(1);

        autoCompleteCategories.setOnItemClickListener((parent, view, position, id) -> {
            Category selectedCategory = (Category) parent.getItemAtPosition(position);
            if (selectedCategory != null) {
                if (!selectedCategoryIds.contains(selectedCategory.getId())) {
                    addCategoryChip(selectedCategory);
                    autoCompleteCategories.setText("");
                    Log.d("EditArtwork", "Category selected from dropdown: " + selectedCategory.getName());
                } else {
                    Toast.makeText(EditArtworkActivity.this, getString(R.string.category_already_selected), Toast.LENGTH_SHORT).show();
                }
            }
        });

        autoCompleteCategories.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && categoryList.size() > 0) {
                autoCompleteCategories.showDropDown();
            }
        });

        autoCompleteCategories.setOnClickListener(v -> {
            if (categoryList.size() > 0) {
                autoCompleteCategories.showDropDown();
            }
        });

        autoCompleteCategories.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    autoCompleteCategories.showDropDown();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void addCategoryChip(Category category) {
        Chip chip = new Chip(this);
        chip.setText(category.getName());
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.accent_primary);
        chip.setTextColor(Color.WHITE);
        chip.setCloseIconTintResource(R.color.surface_primary);
        chip.setTag(category.getId());

        chip.setOnCloseIconClickListener(v -> {
            chipContainer.removeView(v);
            selectedCategoryIds.remove(category.getId());
            updateSelectedCategoriesText();
            categoryAdapter.notifyDataSetChanged();
            Toast.makeText(EditArtworkActivity.this, getString(R.string.removed_category, category.getName()), Toast.LENGTH_SHORT).show();
        });

        selectedCategoryIds.add(category.getId());
        chipContainer.addView(chip);
        updateSelectedCategoriesText();

        categoryAdapter.notifyDataSetChanged();

        Toast.makeText(this, getString(R.string.added_category, category.getName()), Toast.LENGTH_SHORT).show();
    }

    private void observeViewModels() {
        categoryViewModel.getCategoriesResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> categoriesData = (List<Map<String, Object>>) result.get("categories");
                    if (categoriesData != null && !categoriesData.isEmpty()) {
                        categoryList.clear();
                        for (Map<String, Object> categoryData : categoriesData) {
                            Category category = convertToCategory(categoryData);
                            if (category != null) {
                                categoryList.add(category);
                            }
                        }
                        categoryAdapter.notifyDataSetChanged();
                        btnUpdateArtwork.setEnabled(true);

                        Toast.makeText(this, getString(R.string.loaded_categories, categoryList.size()), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_load_categories, message), Toast.LENGTH_SHORT).show();
                    btnUpdateArtwork.setEnabled(true);
                }
            }
        });

        artworkViewModel.getArtworkResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Map<String, Object> artworkData = (Map<String, Object>) result.get("artwork");
                    if (artworkData != null) {
                        artwork = convertToArtwork(artworkData);
                        fillArtworkData();
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_load_artwork, message), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        artworkViewModel.getUpdateResult().observe(this, result -> {
            resetUpdateButton();

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, getString(R.string.artwork_updated_successfully), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String message = (String) result.get("message");
                    String errorMessage = "Failed to update artwork";
                    if (message != null && !message.isEmpty()) {
                        errorMessage += ": " + message;
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("EditArtwork", "Update artwork failed: " + message);
                }
            } else {
                Toast.makeText(this, getString(R.string.failed_to_update_artwork, "null result"), Toast.LENGTH_LONG).show();
                Log.e("EditArtwork", "Update artwork failed: null result");
            }
        });
    }

    private void loadArtwork() {
        artworkViewModel.getArtwork(artworkId);
    }

    private void fillArtworkData() {
        if (artwork == null) return;

        etTitle.setText(artwork.getTitle());
        etDescription.setText(artwork.getDescription());

        // Загружаем изображение, если оно есть
        if (artwork.getImagePath() != null && !artwork.getImagePath().isEmpty()) {
            String imagePath = artwork.getImagePath();
            if (imagePath.startsWith("/")) {
                imagePath = imagePath.substring(1);
            }
            String imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imagePath;
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(ivArtworkImage);

            // Добавляем клик для открытия полноэкранного просмотра
            ivArtworkImage.setOnClickListener(v -> {
                Intent intent = new Intent(this, FullscreenImageActivity.class);
                intent.putExtra("image_url", imageUrl);
                startActivity(intent);
            });
        } else {
            ivArtworkImage.setOnClickListener(null); // Убираем клик если нет изображения
        }

        // Добавляем категории
        if (artwork.getCategories() != null) {
            for (Category category : artwork.getCategories()) {
                if (!selectedCategoryIds.contains(category.getId())) {
                    addCategoryChip(category);
                }
            }
        }

        progressBar.setVisibility(View.GONE);
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

    private void updateSelectedCategoriesText() {
        if (selectedCategoryIds.isEmpty()) {
            tvSelectedCategories.setText("No categories selected");
        } else {
            tvSelectedCategories.setText("Selected: " + selectedCategoryIds.size() + " categories");
        }
    }

    private void selectImage() {
        ImagePicker.with(this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .start(IMAGE_PICKER_REQUEST_CODE);
    }

    private void loadCategories() {
        categoryViewModel.getCategories();
    }

    private void updateArtwork() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Please enter title");
            etTitle.requestFocus();
            return;
        }

        if (title.length() < 3) {
            etTitle.setError("Title must be at least 3 characters");
            etTitle.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Please enter description");
            etDescription.requestFocus();
            return;
        }

        if (description.length() < 10) {
            etDescription.setError("Description must be at least 10 characters");
            etDescription.requestFocus();
            return;
        }

        if (selectedCategoryIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_category), Toast.LENGTH_SHORT).show();
            autoCompleteCategories.requestFocus();
            return;
        }

        try {
            MultipartBody.Part imagePart = null;
            if (selectedImageUri != null) {
                java.io.File imageFile = ImageUtils.uriToFile(selectedImageUri, this);
                if (imageFile == null || !imageFile.exists()) {
                    Toast.makeText(this, getString(R.string.failed_to_process_image_file), Toast.LENGTH_SHORT).show();
                    return;
                }
                imagePart = ImageUtils.prepareImagePart("imageFile", imageFile);
            }

            String categoryIdsString = convertCategoryIdsToString(selectedCategoryIds);

            Log.d("EditArtwork", "Updating artwork:");
            Log.d("EditArtwork", "ID: " + artworkId);
            Log.d("EditArtwork", "Title: " + title);
            Log.d("EditArtwork", "Description: " + description);
            Log.d("EditArtwork", "Category IDs: " + categoryIdsString);

            btnUpdateArtwork.setEnabled(false);
            btnUpdateArtwork.setText("Updating...");
            progressBar.setVisibility(View.VISIBLE);

            artworkViewModel.updateArtwork(artworkId, title, description, categoryIdsString, imagePart);

        } catch (Exception e) {
            Log.e("EditArtwork", "Error updating artwork: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_message, e.getMessage()), Toast.LENGTH_LONG).show();
            resetUpdateButton();
        }
    }

    private String convertCategoryIdsToString(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categoryIds.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(categoryIds.get(i));
        }
        return sb.toString();
    }

    private void resetUpdateButton() {
        btnUpdateArtwork.setEnabled(true);
        btnUpdateArtwork.setText("Update Artwork");
        progressBar.setVisibility(View.GONE);
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
        artwork.setStatus((String) artworkData.get("status"));

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

        if (artworkData.get("user") != null) {
            Map<String, Object> userData = (Map<String, Object>) artworkData.get("user");
            com.example.vagmobile.model.User user = new com.example.vagmobile.model.User();

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICKER_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    ivArtworkImage.setImageURI(selectedImageUri);
                    Toast.makeText(this, getString(R.string.image_selected), Toast.LENGTH_SHORT).show();

                    // Добавляем клик для открытия полноэкранного просмотра
                    ivArtworkImage.setOnClickListener(v -> {
                        Intent intent = new Intent(this, FullscreenImageActivity.class);
                        intent.putExtra("image_url", selectedImageUri.toString());
                        startActivity(intent);
                    });
                }
            } else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show();
                ivArtworkImage.setOnClickListener(null);
            } else {
                Toast.makeText(this, getString(R.string.image_selection_cancelled), Toast.LENGTH_SHORT).show();
                ivArtworkImage.setOnClickListener(null);
            }
        }
    }
}
