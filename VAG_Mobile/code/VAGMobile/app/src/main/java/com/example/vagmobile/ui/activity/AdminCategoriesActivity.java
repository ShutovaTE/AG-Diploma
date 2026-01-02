package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.model.Category;
import com.example.vagmobile.ui.adapter.AdminCategoryAdapter;
import com.example.vagmobile.viewmodel.CategoryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminCategoriesActivity extends AppCompatActivity {

    private CategoryViewModel categoryViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ImageButton btnAddCategory;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdminCategoryAdapter categoryAdapter;
    private List<Category> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_categories);

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        observeViewModels();
        loadCategories();
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        btnAddCategory = findViewById(R.id.btnAddCategory);

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadCategories);
        // Настраиваем цвета индикатора обновления
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupRecyclerView() {
        categoryAdapter = new AdminCategoryAdapter(categoryList, new AdminCategoryAdapter.CategoryActionListener() {
            @Override
            public void onEdit(Category category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onDelete(Category category) {
                showDeleteConfirmationDialog(category);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(categoryAdapter);
    }

    private void observeViewModels() {
        categoryViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(CategoryViewModel.class);

        // Наблюдатель для загрузки категорий
        categoryViewModel.getCategoriesResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> categoriesData = (List<Map<String, Object>>) result.get("categories");
                    if (categoriesData != null) {
                        categoryList.clear();
                        for (Map<String, Object> categoryData : categoriesData) {
                            Category category = convertToCategory(categoryData);
                            categoryList.add(category);
                        }
                        categoryAdapter.notifyDataSetChanged();
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_load_categories, message), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Наблюдатель для создания категории
        categoryViewModel.getCreateCategoryResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, getString(R.string.category_created_successfully), Toast.LENGTH_SHORT).show();
                    loadCategories(); // Перезагружаем список
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_create_category, message), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Наблюдатель для обновления категории
        categoryViewModel.getUpdateCategoryResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, getString(R.string.category_updated_successfully), Toast.LENGTH_SHORT).show();
                    loadCategories(); // Перезагружаем список
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_update_category, message), Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Наблюдатель для удаления категории
        categoryViewModel.getDeleteCategoryResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, getString(R.string.category_deleted_successfully), Toast.LENGTH_SHORT).show();
                    loadCategories(); // Перезагружаем список
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_delete_category, message), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadCategories() {
        progressBar.setVisibility(View.VISIBLE);
        categoryViewModel.getCategories();
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);

        builder.setView(dialogView)
                .setTitle(getString(R.string.dialog_add_category))
                .setPositiveButton(getString(R.string.btn_add), (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_enter_category_name), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createCategory(name, description);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showEditCategoryDialog(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_category, null);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);

        etName.setText(category.getName());
        etDescription.setText(category.getDescription());

        builder.setView(dialogView)
                .setTitle(getString(R.string.dialog_edit_category))
                .setPositiveButton(getString(R.string.btn_save), (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String description = etDescription.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, getString(R.string.please_enter_category_name), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateCategory(category.getId(), name, description);
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void showDeleteConfirmationDialog(Category category) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_delete_category))
                .setMessage(getString(R.string.confirm_delete_category, category.getName()))
                .setPositiveButton(getString(R.string.btn_delete), (dialog, which) -> {
                    deleteCategory(category.getId());
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void createCategory(String name, String description) {
        categoryViewModel.createCategory(name, description);
    }

    private void updateCategory(Long categoryId, String name, String description) {
        categoryViewModel.updateCategory(categoryId, name, description);
    }

    private void deleteCategory(Long categoryId) {
        categoryViewModel.deleteCategory(categoryId);
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
}