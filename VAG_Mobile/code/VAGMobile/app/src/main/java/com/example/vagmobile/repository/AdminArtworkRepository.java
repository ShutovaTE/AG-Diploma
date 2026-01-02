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

public class AdminArtworkRepository {
    private ApiService apiService;
    private Context context;

    public AdminArtworkRepository(Context context) {
        this.context = context;
        apiService = ApiClient.getClient().create(ApiService.class);
    }

    private String getAuthHeader() {
        if (context != null) {
            SharedPreferencesHelper prefs = new SharedPreferencesHelper(context);
            String token = prefs.getToken();
            System.out.println("AdminArtworkRepository: Token from prefs: " + (token != null ? "exists (length: " + token.length() + ")" : "null"));
            if (token != null) {
                String authHeader = "Bearer " + token;
                System.out.println("AdminArtworkRepository: AuthHeader: " + authHeader);
                return authHeader;
            } else {
                System.out.println("AdminArtworkRepository: Token is null - user may not be logged in");
            }
        } else {
            System.out.println("AdminArtworkRepository: Context is null");
        }
        return null;
    }

    public LiveData<Map<String, Object>> getAdminArtworks(int page, int size, String status) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        System.out.println("AdminArtworkRepository: AuthHeader: " + authHeader);
        System.out.println("AdminArtworkRepository: Request URL: /vag/api/mobile/admin/artworks?page=" + page + "&size=" + size);

        apiService.getAdminArtworks(authHeader, page, size).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                System.out.println("AdminArtworkRepository: Response code: " + response.code());
                System.out.println("AdminArtworkRepository: Response successful: " + response.isSuccessful());

                if (response.isSuccessful() && response.body() != null) {
                    System.out.println("AdminArtworkRepository: Response body: " + response.body());
                    result.setValue(response.body());
                } else {
                    System.out.println("AdminArtworkRepository: Error response: " + response.message());
                    System.out.println("AdminArtworkRepository: Response code: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            System.out.println("AdminArtworkRepository: Error body (first 500 chars): " +
                                (errorBody.length() > 500 ? errorBody.substring(0, 500) : errorBody));
                            if (errorBody.contains("<html") || errorBody.contains("<!DOCTYPE")) {
                                System.out.println("AdminArtworkRepository: Server returned HTML instead of JSON - likely authentication issue");
                            }
                        } else {
                            System.out.println("AdminArtworkRepository: No error body");
                        }
                    } catch (Exception e) {
                        System.out.println("AdminArtworkRepository: Error reading error body: " + e.getMessage());
                        e.printStackTrace();
                    }
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    String errorMsg = "Failed to load artworks: " + response.message() + " (code: " + response.code() + ")";
                    if (response.code() == 302 || response.code() == 200) {
                        errorMsg = "Authentication failed - please login again";
                    }
                    error.put("message", errorMsg);
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                System.out.println("AdminArtworkRepository: Network error: " + t.getMessage());
                t.printStackTrace();
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }

    public LiveData<Map<String, Object>> approveArtwork(Long artworkId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        apiService.approveArtwork(authHeader, artworkId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to approve artwork: " + response.message());
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

    public LiveData<Map<String, Object>> rejectArtwork(Long artworkId) {
        MutableLiveData<Map<String, Object>> result = new MutableLiveData<>();

        String authHeader = getAuthHeader();
        apiService.rejectArtwork(authHeader, artworkId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(response.body());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "Failed to reject artwork: " + response.message());
                    result.setValue(error);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                System.out.println("AdminArtworkRepository: Reject network error: " + t.getMessage());
                t.printStackTrace();
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Network error: " + t.getMessage());
                result.setValue(error);
            }
        });

        return result;
    }
}

