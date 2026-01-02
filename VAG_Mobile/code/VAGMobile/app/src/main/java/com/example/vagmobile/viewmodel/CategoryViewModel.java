package com.example.vagmobile.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.vagmobile.repository.CategoryRepository;

import java.util.Map;

public class CategoryViewModel extends AndroidViewModel {
    private CategoryRepository categoryRepository;
    private MutableLiveData<Map<String, Object>> categoriesResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> categoryResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> categoryArtworksResult = new MutableLiveData<>();

    private MutableLiveData<Map<String, Object>> createCategoryResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> updateCategoryResult = new MutableLiveData<>();
    private MutableLiveData<Map<String, Object>> deleteCategoryResult = new MutableLiveData<>();

    public CategoryViewModel(Application application) {
        super(application);
        categoryRepository = new CategoryRepository(application);
    }

    public void getCategories() {
        categoryRepository.getCategories().observeForever(result -> {
            categoriesResult.setValue(result);
        });
    }

    public void getCategoryArtworks(Long categoryId, int page, int size) {
        categoryRepository.getCategoryArtworks(categoryId, page, size).observeForever(result -> {
            categoryArtworksResult.setValue(result);
        });
    }

    public void createCategory(String name, String description) {
        categoryRepository.createCategory(name, description).observeForever(result -> {
            createCategoryResult.setValue(result);
        });
    }

    public void updateCategory(Long categoryId, String name, String description) {
        categoryRepository.updateCategory(categoryId, name, description).observeForever(result -> {
            updateCategoryResult.setValue(result);
        });
    }

    public void deleteCategory(Long categoryId) {
        categoryRepository.deleteCategory(categoryId).observeForever(result -> {
            deleteCategoryResult.setValue(result);
        });
    }

    public LiveData<Map<String, Object>> getCategoriesResult() {
        return categoriesResult;
    }

    public LiveData<Map<String, Object>> getCategoryResult() {
        return categoryResult;
    }

    public LiveData<Map<String, Object>> getCategoryArtworksResult() {
        return categoryArtworksResult;
    }

    public LiveData<Map<String, Object>> getCreateCategoryResult() {
        return createCategoryResult;
    }

    public LiveData<Map<String, Object>> getUpdateCategoryResult() {
        return updateCategoryResult;
    }

    public LiveData<Map<String, Object>> getDeleteCategoryResult() {
        return deleteCategoryResult;
    }
}