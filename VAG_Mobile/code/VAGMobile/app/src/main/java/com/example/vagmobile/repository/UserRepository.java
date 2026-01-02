package com.example.vagmobile.repository;

import android.content.Context;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.MutableLiveData;
import com.example.vagmobile.network.ApiClient;
import com.example.vagmobile.network.ApiService;
import com.example.vagmobile.util.SharedPreferencesHelper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private ApiService apiService;
    private Context context;

    public UserRepository() {
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    public UserRepository(Context context) {
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
    public MutableLiveData<Map<String, Object>> getAllArtists() {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getArtworks(0, 100).enqueue(new Callback<Map<String, Object>>() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                System.out.println("UserRepository: Artworks response code: " + response.code());

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> responseBody = response.body();
                    Boolean success = (Boolean) responseBody.get("success");

                    if (success != null && success) {
                        List<Map<String, Object>> artworksData = (List<Map<String, Object>>) responseBody.get("artworks");
                        if (artworksData != null && !artworksData.isEmpty()) {
                            Map<Long, Map<String, Object>> uniqueUsers = new HashMap<>();
                            Map<Long, Integer> artworkCounts = new HashMap<>();

                            for (Map<String, Object> artworkData : artworksData) {
                                if (artworkData.get("user") != null) {
                                    Map<String, Object> userData = (Map<String, Object>) artworkData.get("user");
                                    Object userIdObj = userData.get("id");

                                    if (userIdObj != null) {
                                        Long userId = null;
                                        if (userIdObj instanceof Double) {
                                            userId = ((Double) userIdObj).longValue();
                                        } else if (userIdObj instanceof Integer) {
                                            userId = ((Integer) userIdObj).longValue();
                                        } else if (userIdObj instanceof Long) {
                                            userId = (Long) userIdObj;
                                        }

                                        if (userId != null) {
                                            artworkCounts.put(userId, artworkCounts.getOrDefault(userId, 0) + 1);

                                            if (!uniqueUsers.containsKey(userId)) {
                                                uniqueUsers.put(userId, userData);
                                            }
                                        }
                                    }
                                }
                            }

                            List<Map<String, Object>> usersWithCounts = new ArrayList<>();
                            for (Map.Entry<Long, Map<String, Object>> entry : uniqueUsers.entrySet()) {
                                Map<String, Object> userData = new HashMap<>(entry.getValue());
                                userData.put("artworksCount", artworkCounts.get(entry.getKey()));
                                usersWithCounts.add(userData);
                            }

                            Map<String, Object> resultData = new HashMap<>();
                            resultData.put("success", true);
                            resultData.put("users", usersWithCounts);
                            result.setValue(resultData);

                            System.out.println("UserRepository: Extracted " + uniqueUsers.size() + " unique artists from artworks");
                        } else {
                            Map<String, Object> error = new HashMap<>();
                            error.put("success", false);
                            error.put("message", "No artworks found");
                            result.setValue(error);
                        }
                    } else {
                        String message = (String) responseBody.get("message");
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("message", "Failed to load artworks: " + message);
                        result.setValue(error);
                    }
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to load artworks: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                System.out.println("UserRepository: Network error loading artworks: " + t.getMessage());
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }

    private com.example.vagmobile.model.User convertToUser(Map<String, Object> userData) {
        try {
            com.example.vagmobile.model.User user = new com.example.vagmobile.model.User();

            Object idObj = userData.get("id");
            if (idObj != null) {
                if (idObj instanceof Double) {
                    user.setId(((Double) idObj).longValue());
                } else if (idObj instanceof Integer) {
                    user.setId(((Integer) idObj).longValue());
                } else if (idObj instanceof Long) {
                    user.setId((Long) idObj);
                }
            }

            user.setUsername((String) userData.get("username"));
            user.setEmail((String) userData.get("email"));

            if (userData.get("role") != null) {
                user.setRole((String) userData.get("role"));
            }

            return user;
        } catch (Exception e) {
            System.out.println("UserRepository: Error converting user: " + e.getMessage());
            return null;
        }
    }

    public MutableLiveData<Map<String, Object>> getCurrentUser() {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        System.out.println("UserRepository: getAuthHeader() returned: " + (authHeader != null ? "token present" : "null"));
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        ApiService authApiService = ApiClient.getClientWithAuth(authHeader).create(ApiService.class);
        authApiService.getCurrentUserProfile(authHeader).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to get user profile: " + response.message());
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

    public MutableLiveData<Map<String, Object>> getUser(Long userId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getUserProfile(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to get user: " + response.message());
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

    public MutableLiveData<Map<String, Object>> updateProfile(String username, String email, String description) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        Map<String, String> profileData = new HashMap<>();
        profileData.put("username", username);
        profileData.put("email", email);
        if (description != null) {
            profileData.put("description", description);
        }

        apiService.updateProfile(authHeader, profileData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to update profile: " + response.message());
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

    public MutableLiveData<Map<String, Object>> updateProfileWithPassword(String username, String email, String description, String currentPassword) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        Map<String, String> profileData = new HashMap<>();
        profileData.put("username", username);
        profileData.put("email", email);
        if (description != null) {
            profileData.put("description", description);
        }
        profileData.put("currentPassword", currentPassword);

        apiService.updateProfileWithPassword(authHeader, profileData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to update profile: " + response.message());
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

    public MutableLiveData<Map<String, Object>> changePassword(String currentPassword, String newPassword) {
        return changePassword(currentPassword, newPassword, false);
    }

    public MutableLiveData<Map<String, Object>> changePassword(String currentPassword, String newPassword, boolean skipPasswordCheck) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        Map<String, String> passwordData = new HashMap<>();
        passwordData.put("currentPassword", currentPassword);
        passwordData.put("newPassword", newPassword);
        if (skipPasswordCheck) {
            passwordData.put("skipPasswordCheck", "true");
        }

        apiService.changePassword(authHeader, passwordData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to change password: " + response.message());
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

    // Метод для удаления публикации
    public MutableLiveData<Map<String, Object>> getUserArtworks(Long userId, int page, int size) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        apiService.getUserArtworks(userId, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to get user artworks: " + response.message());
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

    // Метод для получения всех публикаций пользователя (включая не APPROVED)
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

        ApiService authApiService = ApiClient.getClientWithAuth(authHeader).create(ApiService.class);
        authApiService.getAllUserArtworks(authHeader, userId, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to get all user artworks: " + response.message());
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

        String authHeader = getAuthHeader();
        if (authHeader == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Authentication required");
            result.setValue(error);
            return result;
        }

        apiService.deleteUserArtwork(authHeader, artworkId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to delete artwork: " + response.message());
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