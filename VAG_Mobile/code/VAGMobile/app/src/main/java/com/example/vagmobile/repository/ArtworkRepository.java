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

public class ArtworkRepository {
    private ApiService apiService;
    private Context context;

    public ArtworkRepository() {
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public ArtworkRepository(Context context) {
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

    private ApiService getApiServiceWithAuth() {
        String authHeader = getAuthHeader();
        if (authHeader != null) {
            return ApiClient.getClientWithAuth(authHeader).create(ApiService.class);
        }
        return apiService;
    }

    public MutableLiveData<Map<String, Object>> getArtworkForAdmin(Long id) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.getArtworkForAdmin(authHeader, id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                System.out.println("ArtworkRepository: Admin artwork response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    System.out.println("ArtworkRepository: Admin artwork loaded successfully");
                    result.setValue(response.body());
                } else {
                    System.out.println("ArtworkRepository: Failed to load admin artwork: " + response.message());
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load artwork: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                System.out.println("ArtworkRepository: Network error loading admin artwork: " + t.getMessage());
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }

    public MutableLiveData<Map<String, Object>> getArtworks(int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getArtworks(page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load artworks: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getArtwork(Long id) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        Call<Map<String, Object>> call;

        if (authHeader != null) {
            // Если есть токен, используем метод с авторизацией
            call = apiService.getArtworkWithAuth(authHeader, id);
        } else {
            // Если токена нет, используем метод без авторизации
            call = apiService.getArtwork(id);
        }

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load artwork: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getLikedArtworks(int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.getLikedArtworks(authHeader, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load liked artworks: " + response.message());
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

    public MutableLiveData<Map<String, Object>> createArtwork(
            String title,
            String description,
            String categoryIds,
            MultipartBody.Part image) {

        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        RequestBody titleBody = RequestBody.create(MultipartBody.FORM, title);
        RequestBody descriptionBody = RequestBody.create(MultipartBody.FORM, description);
        RequestBody categoryIdsBody = RequestBody.create(MultipartBody.FORM, categoryIds);

        String authHeader = getAuthHeader();
        System.out.println("ArtworkRepository: Creating artwork with authHeader: " + authHeader);

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.createArtwork(authHeader, titleBody, descriptionBody, categoryIdsBody, image)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Failed to create artwork: " + response.message());
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

    public MutableLiveData<Map<String, Object>> updateArtwork(
            Long artworkId,
            String title,
            String description,
            String categoryIds,
            MultipartBody.Part image) {

        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        RequestBody titleBody = RequestBody.create(MultipartBody.FORM, title);
        RequestBody descriptionBody = RequestBody.create(MultipartBody.FORM, description);
        RequestBody categoryIdsBody = RequestBody.create(MultipartBody.FORM, categoryIds);

        String authHeader = getAuthHeader();
        System.out.println("ArtworkRepository: Updating artwork with authHeader: " + authHeader);

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.updateArtwork(authHeader, artworkId, titleBody, descriptionBody, categoryIdsBody, image)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Failed to update artwork: " + response.message());
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

    public MutableLiveData<Map<String, Object>> likeArtwork(Long id) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.likeArtwork(authHeader, id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to like artwork: " + response.message());
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

    public MutableLiveData<Map<String, Object>> unlikeArtwork(Long id) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.unlikeArtwork(authHeader, id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to unlike artwork: " + response.message());
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

    public MutableLiveData<Map<String, Object>> addComment(Long id, String content) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        RequestBody contentBody = RequestBody.create(MultipartBody.FORM, content);

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.addComment(authHeader, id, contentBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();
                    Boolean success = (Boolean) responseBody.get("success");
                    if (success != null && success) {
                        result.setValue(responseBody);
                    } else {
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("message", "Failed to add comment: " + responseBody.get("message"));
                        result.setValue(error);
                    }
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to add comment: " + response.message());
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

    public MutableLiveData<Map<String, Object>> searchArtworks(String query, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.searchArtworks(query, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Search failed: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getCategoryArtworks(Long categoryId, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getCategoryArtworks(categoryId, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load category artworks");
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

    public MutableLiveData<Map<String, Object>> toggleLike(Long artworkId, boolean isCurrentlyLiked) {
        System.out.println("ArtworkRepository: Toggle like - artworkId: " + artworkId + ", isCurrentlyLiked: " + isCurrentlyLiked);

        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();

        if (isCurrentlyLiked) {
            System.out.println("Calling UNLIKE for artwork: " + artworkId);
            authApiService.unlikeArtwork(authHeader, artworkId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    System.out.println("UNLIKE response - Code: " + response.code() + ", Success: " + response.isSuccessful());
                    if (response.isSuccessful() && response.body() != null) {
                        System.out.println("UNLIKE successful: " + response.body());
                        result.setValue(response.body());
                    } else {
                        System.out.println("UNLIKE failed: " + response.message());
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("message", "Failed to unlike artwork: " + response.message());
                        result.setValue(error);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    System.out.println("UNLIKE network error: " + t.getMessage());
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Network error: " + t.getMessage());
                    result.setValue(error);
                }
            });
        } else {
            System.out.println("Calling LIKE for artwork: " + artworkId);
            authApiService.likeArtwork(authHeader, artworkId).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    System.out.println("LIKE response - Code: " + response.code() + ", Success: " + response.isSuccessful());
                    if (response.isSuccessful() && response.body() != null) {
                        System.out.println("LIKE successful: " + response.body());
                        result.setValue(response.body());
                    } else {
                        System.out.println("LIKE failed: " + response.message());
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("message", "Failed to like artwork: " + response.message());
                        result.setValue(error);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    System.out.println("LIKE network error: " + t.getMessage());
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Network error: " + t.getMessage());
                    result.setValue(error);
                }
            });
        }

        return result;
    }
    public MutableLiveData<Map<String, Object>> getAllUserArtworks(Long userId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getUserArtworks(userId, 0, 100).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load user artworks: " + response.message());
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

    private MutableLiveData<Map<String, Object>> userArtworksResult = new MutableLiveData<>();

    public void getUserArtworks(Long userId, int page, int size) {
        apiService.getUserArtworks(userId, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userArtworksResult.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load user artworks: " + response.message());
                    userArtworksResult.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                userArtworksResult.setValue(error);
            }
        });
    }

    public MutableLiveData<Map<String, Object>> getUserArtworksResult() {
        return userArtworksResult;
    }

    public MutableLiveData<Map<String, Object>> getAllUserArtworks(Long userId, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();
        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = getApiServiceWithAuth();
        authApiService.getAllUserArtworks(authHeader, userId, page, size)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Ошибка загрузки: " + response.message());
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
    public MutableLiveData<Map<String, Object>> deleteArtwork(Long artworkId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();  // ← используем уже готовый метод!
        if (authHeader == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("message", "Токен отсутствует");
            result.setValue(err);
            return result;
        }

        // Используем уже готовый getApiServiceWithAuth() — он сам добавляет заголовок
        getApiServiceWithAuth().deleteArtwork(authHeader, artworkId)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> err = new HashMap<>();
                            err.put("success", false);
                            err.put("message", "Ошибка сервера: " + response.code());
                            result.setValue(err);
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Map<String, Object> err = new HashMap<>();
                        err.put("success", false);
                        err.put("message", "Нет интернета");
                        result.setValue(err);
                    }
                });

        return result;
    }

    public MutableLiveData<Map<String, Object>> createArtworkSimple(String title, String description) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Требуется аутентификация");
            result.setValue(error);
            return result;
        }

        apiService.createArtworkSimple(authHeader, title, description)
                .enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            result.setValue(response.body());
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "Не удалось создать работу: " + response.message());
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