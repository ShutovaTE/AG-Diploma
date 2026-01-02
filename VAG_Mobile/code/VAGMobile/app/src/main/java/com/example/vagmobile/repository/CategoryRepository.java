package com.example.vagmobile.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.vagmobile.network.ApiClient;
import com.example.vagmobile.network.ApiService;
import com.example.vagmobile.util.SharedPreferencesHelper;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoryRepository {

    private ApiService apiService;
    private Context context;

    public CategoryRepository() {
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public CategoryRepository(Context context) {
        this.context = context;
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    private String getAuthHeader() {
        if (context != null) {
            SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
            String token = prefs.getToken();
            if (token != null) {
                return "Bearer " + token;
            }
        }
        return null;
    }

    public LiveData<Map<String, Object>> getCategories() {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getCategories().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load categories: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }

    public LiveData<Map<String, Object>> createCategory(String name, String description) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        Map<String, String> categoryData = new HashMap<>();
        categoryData.put("name", name);
        categoryData.put("description", description);

        String authHeader = getAuthHeader();
        apiService.createCategory(authHeader, categoryData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to create category: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }

    public LiveData<Map<String, Object>> updateCategory(Long categoryId, String name, String description) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        Map<String, String> categoryData = new HashMap<>();
        categoryData.put("name", name);
        categoryData.put("description", description);

        String authHeader = getAuthHeader();
        apiService.updateCategory(authHeader, categoryId, categoryData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to update category: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }

    public LiveData<Map<String, Object>> deleteCategory(Long categoryId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        apiService.deleteCategory(authHeader, categoryId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to delete category: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }

    public LiveData<Map<String, Object>> getCategoryArtworks(Long categoryId, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getCategoryArtworks(categoryId, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load category artworks: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }
}