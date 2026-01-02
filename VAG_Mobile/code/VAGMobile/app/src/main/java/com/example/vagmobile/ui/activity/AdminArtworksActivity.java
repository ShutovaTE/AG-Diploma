package com.example.vagmobile.ui.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.ui.adapter.AdminArtworkAdapter;
import com.example.vagmobile.viewmodel.AdminArtworkViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminArtworksActivity extends AppCompatActivity {

    private AdminArtworkViewModel adminArtworkViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Spinner statusSpinner;
    private SwipeRefreshLayout swipeRefreshLayout;
    private AdminArtworkAdapter artworkAdapter;
    private List<Artwork> artworkList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_artworks);

        initViews();
        setupSwipeRefresh();
        setupRecyclerView();
        observeViewModels();
        loadArtworks(null);
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        statusSpinner = findViewById(R.id.statusSpinner);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> loadArtworks(null));
        // Настраиваем цвета индикатора обновления
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupRecyclerView() {
        artworkAdapter = new AdminArtworkAdapter(artworkList, new AdminArtworkAdapter.ArtworkActionListener() {
            @Override
            public void onApprove(Artwork artwork) {
                approveArtwork(artwork.getId());
            }

            @Override
            public void onReject(Artwork artwork) {
                rejectArtwork(artwork.getId());
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(artworkAdapter);
    }

    private void observeViewModels() {
        adminArtworkViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication())).get(AdminArtworkViewModel.class);

        adminArtworkViewModel.getArtworksResult().observe(this, result -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");
                    if (artworksData != null) {
                        System.out.println("=== ADMIN ARTWORKS DATA DEBUG ===");
                        System.out.println("Total artworks: " + artworksData.size());

                        for (int i = 0; i < Math.min(artworksData.size(), 2); i++) {
                            Map<String, Object> artworkData = artworksData.get(i);
                            System.out.println("--- Artwork " + i + " ---");
                            System.out.println("All keys: " + artworkData.keySet());

                            for (String key : artworkData.keySet()) {
                                Object value = artworkData.get(key);
                                System.out.println(key + ": " + value + " (type: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
                            }

                            Object userObj = artworkData.get("user");
                            System.out.println("User object: " + userObj);
                            if (userObj != null) {
                                System.out.println("User object type: " + userObj.getClass().getSimpleName());
                                if (userObj instanceof Map) {
                                    Map<String, Object> userData = (Map<String, Object>) userObj;
                                    System.out.println("User data keys: " + userData.keySet());
                                    for (String userKey : userData.keySet()) {
                                        System.out.println("User." + userKey + ": " + userData.get(userKey));
                                    }
                                }
                            }
                            System.out.println("----------------------------");
                        }

                        artworkList.clear();
                        for (Map<String, Object> artworkData : artworksData) {
                            Artwork artwork = convertToArtwork(artworkData);
                            artworkList.add(artwork);
                        }
                        artworkAdapter.notifyDataSetChanged();
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_load_artworks, message), Toast.LENGTH_SHORT).show();
                }
            }
        });

        adminArtworkViewModel.getApproveResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, getString(R.string.artwork_approved_successfully), Toast.LENGTH_SHORT).show();
                    loadArtworks(null);
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_approve_artwork, message), Toast.LENGTH_SHORT).show();
                }
            }
        });

        adminArtworkViewModel.getRejectResult().observe(this, result -> {
            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Toast.makeText(this, getString(R.string.artwork_rejected_successfully), Toast.LENGTH_SHORT).show();
                    loadArtworks(null);
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(this, getString(R.string.failed_to_reject_artwork, message), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadArtworks(String status) {
        progressBar.setVisibility(View.VISIBLE);
        adminArtworkViewModel.getAdminArtworks(0, 20, null);
    }

    private void approveArtwork(Long artworkId) {
        adminArtworkViewModel.approveArtwork(artworkId);
    }

    private void rejectArtwork(Long artworkId) {
        adminArtworkViewModel.rejectArtwork(artworkId);
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
            }
        }

        Object viewsObj = artworkData.get("views");
        if (viewsObj != null) {
            if (viewsObj instanceof Double) {
                artwork.setViews(((Double) viewsObj).intValue());
            } else if (viewsObj instanceof Integer) {
                artwork.setViews((Integer) viewsObj);
            }
        }

        artwork.setUser(parseUserFromArtworkData(artworkData));

        return artwork;
    }

    private com.example.vagmobile.model.User parseUserFromArtworkData(Map<String, Object> artworkData) {
        com.example.vagmobile.model.User user = new com.example.vagmobile.model.User();

        System.out.println("=== PARSING USER DATA ===");
        System.out.println("Artwork data keys: " + artworkData.keySet());

        Object userObj = artworkData.get("user");
        if (userObj instanceof Map) {
            Map<String, Object> userData = (Map<String, Object>) userObj;
            System.out.println("Found user as Map: " + userData);
            System.out.println("User data keys: " + userData.keySet());

            Object userIdObj = userData.get("id");
            if (userIdObj != null) {
                System.out.println("User ID object: " + userIdObj + " (type: " + userIdObj.getClass().getSimpleName() + ")");
                if (userIdObj instanceof Double) {
                    user.setId(((Double) userIdObj).longValue());
                } else if (userIdObj instanceof Integer) {
                    user.setId(((Integer) userIdObj).longValue());
                } else if (userIdObj instanceof Long) {
                    user.setId((Long) userIdObj);
                } else if (userIdObj instanceof String) {
                    try {
                        user.setId(Long.parseLong((String) userIdObj));
                    } catch (NumberFormatException e) {
                        System.out.println("Failed to parse user ID from string: " + userIdObj);
                    }
                }
                System.out.println("Parsed user ID: " + user.getId());
            }

            String username = null;
            if (userData.get("username") != null) {
                username = userData.get("username").toString();
                System.out.println("Found username in 'username': " + username);
            } else if (userData.get("userName") != null) {
                username = userData.get("userName").toString();
                System.out.println("Found username in 'userName': " + username);
            } else if (userData.get("name") != null) {
                username = userData.get("name").toString();
                System.out.println("Found username in 'name': " + username);
            } else if (userData.get("login") != null) {
                username = userData.get("login").toString();
                System.out.println("Found username in 'login': " + username);
            }

            user.setUsername(username != null ? username : "Неизвестный пользователь");

            if (userData.get("email") != null) {
                user.setEmail(userData.get("email").toString());
                System.out.println("Found email: " + user.getEmail());
            }

            System.out.println("Final parsed user: " + user.getUsername() + " (ID: " + user.getId() + ", Email: " + user.getEmail() + ")");
            return user;
        } else if (userObj != null) {
            System.out.println("User object is not a Map, type: " + userObj.getClass().getSimpleName());
            System.out.println("User object value: " + userObj);
        }

        System.out.println("Checking for direct user fields in artwork...");

        Object userIdObj = artworkData.get("userId");
        if (userIdObj != null) {
            System.out.println("Found userId in artwork: " + userIdObj);
            if (userIdObj instanceof Double) {
                user.setId(((Double) userIdObj).longValue());
            } else if (userIdObj instanceof Integer) {
                user.setId(((Integer) userIdObj).longValue());
            } else if (userIdObj instanceof Long) {
                user.setId((Long) userIdObj);
            } else if (userIdObj instanceof String) {
                try {
                    user.setId(Long.parseLong((String) userIdObj));
                } catch (NumberFormatException e) {
                    System.out.println("Failed to parse userId from string: " + userIdObj);
                }
            }
        }

        String username = null;
        if (artworkData.get("userName") != null) {
            username = artworkData.get("userName").toString();
            System.out.println("Found username in 'userName': " + username);
        } else if (artworkData.get("author") != null) {
            username = artworkData.get("author").toString();
            System.out.println("Found username in 'author': " + username);
        } else if (artworkData.get("creator") != null) {
            username = artworkData.get("creator").toString();
            System.out.println("Found username in 'creator': " + username);
        } else if (artworkData.get("username") != null) {
            username = artworkData.get("username").toString();
            System.out.println("Found username in 'username': " + username);
        } else if (artworkData.get("userUsername") != null) {
            username = artworkData.get("userUsername").toString();
            System.out.println("Found username in 'userUsername': " + username);
        }

        user.setUsername(username != null ? username :
                (user.getId() != null ? "Пользователь #" + user.getId() : "Неизвестный пользователь"));

        if (artworkData.get("userEmail") != null) {
            user.setEmail(artworkData.get("userEmail").toString());
        } else if (artworkData.get("email") != null) {
            user.setEmail(artworkData.get("email").toString());
        }

        System.out.println("Final parsed user from direct fields: " + user.getUsername() + " (ID: " + user.getId() + ")");
        return user;
    }
}