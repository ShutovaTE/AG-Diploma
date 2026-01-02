package com.example.vagmobile.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.activity.ArtworkDetailActivity;
import com.example.vagmobile.ui.adapter.ArtworkAdapter;
import com.example.vagmobile.viewmodel.ArtworkViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArtworksFragment extends Fragment {

    private ArtworkViewModel artworkViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private ArtworkAdapter artworkAdapter;
    private List<Artwork> artworkList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_artworks, container, false);

        initViews(view);
        setupRecyclerView();
        observeViewModels();
        loadArtworks();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        artworkAdapter = new ArtworkAdapter(artworkList, new ArtworkAdapter.OnArtworkClickListener() {
            @Override
            public void onArtworkClick(Artwork artwork) {
                Intent intent = new Intent(getActivity(), ArtworkDetailActivity.class);
                intent.putExtra("artwork_id", artwork.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Artwork artwork) {
                // Не используется для просмотра всех публикаций
            }

            @Override
            public void onDeleteClick(Artwork artwork) {
                // Не используется для просмотра всех публикаций
            }
        }, false); // false - не показываем кнопки действий для публичного просмотра

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(artworkAdapter);
    }

    private void observeViewModels() {
        artworkViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())).get(ArtworkViewModel.class);

        artworkViewModel.getArtworksResult().observe(getViewLifecycleOwner(), result -> {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");
                    if (artworksData != null) {
                        artworkList.clear();
                        for (Map<String, Object> artworkData : artworksData) {
                            Artwork artwork = convertToArtwork(artworkData);
                            artworkList.add(artwork);
                        }
                        artworkAdapter.notifyDataSetChanged();
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_artworks, message), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadArtworks() {
        progressBar.setVisibility(View.VISIBLE);
        artworkViewModel.getArtworks(0, 20);
    }

    private Artwork convertToArtwork(Map<String, Object> artworkData) {
        Artwork artwork = new Artwork();

        if (artworkData.get("id") != null) {
            if (artworkData.get("id") instanceof Double) {
                artwork.setId(((Double) artworkData.get("id")).longValue());
            } else if (artworkData.get("id") instanceof Long) {
                artwork.setId((Long) artworkData.get("id"));
            } else if (artworkData.get("id") instanceof Integer) {
                artwork.setId(((Integer) artworkData.get("id")).longValue());
            }
        }

        artwork.setTitle((String) artworkData.get("title"));
        artwork.setDescription((String) artworkData.get("description"));
        artwork.setImagePath((String) artworkData.get("imagePath"));

        if (artworkData.get("likes") != null) {
            if (artworkData.get("likes") instanceof Double) {
                artwork.setLikes(((Double) artworkData.get("likes")).intValue());
            } else if (artworkData.get("likes") instanceof Integer) {
                artwork.setLikes((Integer) artworkData.get("likes"));
            } else if (artworkData.get("likes") instanceof Long) {
                artwork.setLikes(((Long) artworkData.get("likes")).intValue());
            }
        }

        artwork.setUser(parseUserFromArtworkData(artworkData));

        return artwork;
    }

    private User parseUserFromArtworkData(Map<String, Object> artworkData) {
        User user = new User();

        Object userObj = artworkData.get("user");
        if (userObj instanceof Map) {
            Map<String, Object> userData = (Map<String, Object>) userObj;

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

            String username = null;
            if (userData.get("username") != null) {
                username = (String) userData.get("username");
            } else if (userData.get("userName") != null) {
                username = (String) userData.get("userName");
            } else if (userData.get("name") != null) {
                username = (String) userData.get("name");
            }
            user.setUsername(username != null ? username : "Неизвестный художник");

            if (userData.get("email") != null) {
                user.setEmail((String) userData.get("email"));
            }

            return user;
        }

        Object userIdObj = artworkData.get("userId");
        if (userIdObj != null) {
            if (userIdObj instanceof Double) {
                user.setId(((Double) userIdObj).longValue());
            } else if (userIdObj instanceof Integer) {
                user.setId(((Integer) userIdObj).longValue());
            } else if (userIdObj instanceof Long) {
                user.setId((Long) userIdObj);
            }
        }

        String username = null;
        if (artworkData.get("userName") != null) {
            username = (String) artworkData.get("userName");
        } else if (artworkData.get("author") != null) {
            username = (String) artworkData.get("author");
        } else if (artworkData.get("username") != null) {
            username = (String) artworkData.get("username");
        }

        user.setUsername(username != null ? username : "Неизвестный художник");

        return user;
    }
}