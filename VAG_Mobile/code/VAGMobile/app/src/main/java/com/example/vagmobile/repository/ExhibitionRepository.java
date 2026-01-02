package com.example.vagmobile.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;
import com.example.vagmobile.network.ApiClient;
import com.example.vagmobile.network.ApiService;
import com.example.vagmobile.util.SharedPreferencesHelper;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.HashMap;
import java.util.Map;

public class ExhibitionRepository {
    private ApiService apiService;
    private Context context;

    public ExhibitionRepository() {
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public ExhibitionRepository(Context context) {
        this.context = context;
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public void setContext(Context context) {
        this.context = context;
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

    private ApiService getApiServiceWithAuth() {
        String authHeader = getAuthHeader();
        if (authHeader != null) {
            return ApiClient.getClientWithAuth(authHeader).create(ApiService.class);
        }
        return apiService;
    }

    public MutableLiveData<Map<String, Object>> getExhibitions(int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getExhibitions(page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Не удалось загрузить выставки: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getExhibition(Long id) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getExhibition(id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Не удалось загрузить выставку: " + response.message());
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

    public MutableLiveData<Map<String, Object>> createExhibition(
            String title,
            String description,
            boolean authorOnly) {

        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Требуется аутентификация");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.createExhibition(authHeader, title, description, authorOnly)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Не удалось создать выставку: " + response.message());
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

    public MutableLiveData<Map<String, Object>> updateExhibition(
            Long exhibitionId,
            String title,
            String description,
            boolean authorOnly) {

        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Требуется аутентификация");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.updateExhibition(authHeader, exhibitionId, title, description, authorOnly)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Не удалось обновить выставку: " + response.message());
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

    public MutableLiveData<Map<String, Object>> deleteExhibition(Long exhibitionId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Требуется аутентификация");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.deleteExhibition(authHeader, exhibitionId)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Не удалось удалить выставку: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getExhibitionArtworks(Long exhibitionId, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        Call<Map<String, Object>> call;


        if (authHeader != null) {
            // Если есть токен, используем метод с авторизацией
            call = apiService.getExhibitionArtworksWithAuth(authHeader, exhibitionId, page, size);
        } else {
            // Если токена нет, используем метод без авторизации
            call = apiService.getExhibitionArtworks(exhibitionId, page, size);
        }

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Не удалось загрузить работы выставки: " + response.message());
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

    public MutableLiveData<Map<String, Object>> addArtworkToExhibition(Long exhibitionId, Long artworkId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Требуется аутентификация");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.addArtworkToExhibition(authHeader, exhibitionId, artworkId)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Не удалось добавить работу в выставку: " + response.message());
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

    public MutableLiveData<Map<String, Object>> removeArtworkFromExhibition(Long exhibitionId, Long artworkId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Требуется аутентификация");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.removeArtworkFromExhibition(authHeader, exhibitionId, artworkId)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Не удалось удалить работу из выставки: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getUserExhibitions(Long userId, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getUserExhibitions(userId, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Не удалось загрузить выставки пользователя: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getUserArtworksForExhibition(Long exhibitionId, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Требуется аутентификация");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.getUserArtworksForExhibition(authHeader, exhibitionId, page, size)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Не удалось загрузить работы для добавления в выставку: " + response.message());
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
