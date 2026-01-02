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

import com.example.vagmobile.R;
import com.example.vagmobile.model.Category;
import com.example.vagmobile.util.ImageUtils;
import com.example.vagmobile.viewmodel.ArtworkViewModel;
import com.example.vagmobile.viewmodel.CategoryViewModel;
import com.example.vagmobile.viewmodel.ExhibitionViewModel;
import com.github.dhaval2404.imagepicker.ImagePicker;

import okhttp3.MultipartBody;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CreateArtworkActivity extends AppCompatActivity {
    private static final int IMAGE_PICKER_REQUEST_CODE = 1001;

    private ArtworkViewModel artworkViewModel;
    private CategoryViewModel categoryViewModel;
    private ExhibitionViewModel exhibitionViewModel;

    private EditText etTitle, etDescription;
    private ImageView ivArtworkImage;
    private Button btnSelectImage, btnCreateArtwork;
    private ProgressBar progressBar;
    private ChipGroup chipContainer;
    private AutoCompleteTextView autoCompleteCategories;
    private TextView tvSelectedCategories;

    private Uri selectedImageUri;
    private List<Category> categoryList = new ArrayList<>();
    private ArrayAdapter<Category> categoryAdapter;
    private List<Long> selectedCategoryIds = new ArrayList<>();

    // Параметры для создания из выставки
    private boolean fromExhibition = false;
    private Long exhibitionId;
    private String exhibitionTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_artwork);

        // Проверяем, открыта ли активность из выставки
        Intent intent = getIntent();
        fromExhibition = intent.getBooleanExtra("from_exhibition", false);
        exhibitionId = intent.getLongExtra("exhibition_id", -1);
        exhibitionTitle = intent.getStringExtra("exhibition_title");

        artworkViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(ArtworkViewModel.class);
        categoryViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(CategoryViewModel.class);
        exhibitionViewModel = new ViewModelProvider(this).get(ExhibitionViewModel.class);

        initViews();
        setupClickListeners();
        setupCategorySelection();
        loadCategories();
        observeViewModels();

        // Обновляем заголовок, если создание из выставки
        if (fromExhibition && exhibitionTitle != null && !exhibitionTitle.isEmpty()) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Добавить работу в \"" + exhibitionTitle + "\"");
            }
        }
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        ivArtworkImage = findViewById(R.id.ivArtworkImage);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnCreateArtwork = findViewById(R.id.btnCreateArtwork);
        progressBar = findViewById(R.id.progressBar);
        chipContainer = findViewById(R.id.chipContainer);
        autoCompleteCategories = findViewById(R.id.autoCompleteCategories);
        tvSelectedCategories = findViewById(R.id.tvSelectedCategories);

        btnCreateArtwork.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        updateSelectedCategoriesText();
    }

    private void setupClickListeners() {
        btnSelectImage.setOnClickListener(v -> selectImage());
        btnCreateArtwork.setOnClickListener(v -> createArtwork());

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
                    Log.d("CreateArtwork", "Category selected from dropdown: " + selectedCategory.getName());
                } else {
                    Toast.makeText(CreateArtworkActivity.this, getString(R.string.category_already_selected), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(CreateArtworkActivity.this, getString(R.string.removed_category, category.getName()), Toast.LENGTH_SHORT).show();
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
                        btnCreateArtwork.setEnabled(true);

                        Toast.makeText(this, getString(R.string.loaded_categories, categoryList.size()), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_load_categories, message), Toast.LENGTH_SHORT).show();
                    btnCreateArtwork.setEnabled(true);
                }
            }
        });

        artworkViewModel.getCreateResult().observe(this, result -> {
            resetCreateButton();

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    if (fromExhibition && exhibitionId != null && exhibitionId != -1) {
                        // Если создание из выставки, получаем ID работы и добавляем в выставку
                        Map<String, Object> artworkData = (Map<String, Object>) result.get("artwork");
                        if (artworkData != null) {
                            Long artworkId = null;
                            Object idObj = artworkData.get("id");
                            if (idObj instanceof Double) {
                                artworkId = ((Double) idObj).longValue();
                            } else if (idObj instanceof Long) {
                                artworkId = (Long) idObj;
                            }

                            if (artworkId != null) {
                                // Добавляем работу в выставку
                                addArtworkToExhibitionAndFinish(artworkId);
                                return; // Не завершаем активность сразу, ждем результат добавления
                            }
                        }
                    }

                    // Обычное завершение создания
                    Toast.makeText(this, getString(R.string.artwork_created_successfully), Toast.LENGTH_SHORT).show();
                    resetForm();
                    finish();
                } else {
                    String message = (String) result.get("message");
                    String errorMessage = "Failed to create artwork";
                    if (message != null && !message.isEmpty()) {
                        errorMessage += ": " + message;
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
                    Log.e("CreateArtwork", "Create artwork failed: " + message);
                }
            } else {
                Toast.makeText(this, getString(R.string.failed_to_create_artwork, "null result"), Toast.LENGTH_LONG).show();
                Log.e("CreateArtwork", "Create artwork failed: null result");
            }
        });

        exhibitionViewModel.getAddArtworkResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                String message = (String) result.get("message");

                if (success != null && success) {
                    Toast.makeText(this, "Работа создана и добавлена в выставку!", Toast.LENGTH_SHORT).show();
                    resetForm();
                    finish();
                } else {
                    Toast.makeText(this, message != null ? message : "Не удалось добавить работу в выставку", Toast.LENGTH_SHORT).show();
                    // Даже если не удалось добавить в выставку, работа создана, завершаем активность
                    finish();
                }
            }
        });
    }

    private void addArtworkToExhibitionAndFinish(Long artworkId) {
        if (exhibitionId != null && exhibitionId != -1) {
            exhibitionViewModel.addArtworkToExhibition(exhibitionId, artworkId);
        } else {
            // Если exhibitionId не найден, просто завершаем
            Toast.makeText(this, "Работа создана!", Toast.LENGTH_SHORT).show();
            resetForm();
            finish();
        }
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

    private void createArtwork() {
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

        if (selectedImageUri == null) {
            Toast.makeText(this, getString(R.string.please_select_an_image), Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryIds.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_at_least_one_category), Toast.LENGTH_SHORT).show();
            autoCompleteCategories.requestFocus();
            return;
        }

        try {
            File imageFile = ImageUtils.uriToFile(selectedImageUri, this);
            if (imageFile == null || !imageFile.exists()) {
                Toast.makeText(this, getString(R.string.failed_to_process_image_file), Toast.LENGTH_SHORT).show();
                return;
            }

            MultipartBody.Part imagePart = ImageUtils.prepareImagePart("imageFile", imageFile);

            String categoryIdsString = convertCategoryIdsToString(selectedCategoryIds);

            Log.d("CreateArtwork", "Creating artwork with:");
            Log.d("CreateArtwork", "Title: " + title);
            Log.d("CreateArtwork", "Description: " + description);
            Log.d("CreateArtwork", "Category IDs: " + categoryIdsString);
            Log.d("CreateArtwork", "Image file: " + imageFile.getAbsolutePath());

            btnCreateArtwork.setEnabled(false);
            btnCreateArtwork.setText("Creating...");
            progressBar.setVisibility(View.VISIBLE);

            artworkViewModel.createArtwork(title, description, categoryIdsString, imagePart);

        } catch (Exception e) {
            Log.e("CreateArtwork", "Error creating artwork: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.error_message, e.getMessage()), Toast.LENGTH_LONG).show();
            resetCreateButton();
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

    private void resetCreateButton() {
        btnCreateArtwork.setEnabled(true);
        btnCreateArtwork.setText("Create Artwork");
        progressBar.setVisibility(View.GONE);
    }

    private void resetForm() {
        etTitle.setText("");
        etDescription.setText("");
        ivArtworkImage.setImageResource(android.R.drawable.ic_menu_gallery);
        selectedImageUri = null;
        selectedCategoryIds.clear();
        chipContainer.removeAllViews();
        updateSelectedCategoriesText();
        autoCompleteCategories.setText("");
        ivArtworkImage.setOnClickListener(null); // Убираем клик при сбросе формы

        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
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