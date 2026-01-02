package com.example.vagmobile.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import com.example.vagmobile.model.Exhibition;
import com.example.vagmobile.model.User;
import com.example.vagmobile.ui.activity.MainActivity;
import com.example.vagmobile.ui.activity.ArtworkDetailActivity;
import com.example.vagmobile.ui.activity.ArtistArtworksActivity;
import com.example.vagmobile.ui.activity.ArtworkListActivity;
import com.example.vagmobile.ui.activity.ArtistsActivity;
import com.example.vagmobile.ui.activity.ExhibitionDetailActivity;
import com.example.vagmobile.ui.activity.ExhibitionListActivity;
import com.example.vagmobile.ui.adapter.ArtworkAdapter;
import com.example.vagmobile.ui.adapter.ArtistsAdapter;
import com.example.vagmobile.ui.adapter.ExhibitionAdapter;
import com.example.vagmobile.viewmodel.ArtworkViewModel;
import com.example.vagmobile.viewmodel.ExhibitionViewModel;
import com.example.vagmobile.viewmodel.UserViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment {

    private ArtworkViewModel artworkViewModel;
    private UserViewModel userViewModel;
    private ExhibitionViewModel exhibitionViewModel;

    private RecyclerView rvFeaturedArtworks, rvFeaturedArtists, rvFeaturedExhibitions;
    private ArtworkAdapter featuredArtworkAdapter;
    private ArtistsAdapter featuredArtistsAdapter;
    private ExhibitionAdapter featuredExhibitionAdapter;
    private List<Artwork> featuredArtworks = new ArrayList<>();
    private List<User> featuredArtists = new ArrayList<>();
    private List<Exhibition> featuredExhibitions = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvArtworksEmpty, tvArtistsEmpty, tvExhibitionsEmpty;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        initViews(view);
        setupSwipeRefresh();
        setupRecyclerViews();
        loadFeaturedContent();

        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        rvFeaturedArtworks = view.findViewById(R.id.rvFeaturedArtworks);
        rvFeaturedArtists = view.findViewById(R.id.rvFeaturedArtists);
        rvFeaturedExhibitions = view.findViewById(R.id.rvFeaturedExhibitions);
        progressBar = view.findViewById(R.id.progressBar);
        tvArtworksEmpty = view.findViewById(R.id.tvArtworksEmpty);
        tvArtistsEmpty = view.findViewById(R.id.tvArtistsEmpty);
        tvExhibitionsEmpty = view.findViewById(R.id.tvExhibitionsEmpty);

        TextView tvSeeAllArtworks = view.findViewById(R.id.tvSeeAllArtworks);
        TextView tvSeeAllArtists = view.findViewById(R.id.tvSeeAllArtists);

        // Исправленная навигация
        tvSeeAllArtworks.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ArtworkListActivity.class);
            intent.putExtra("list_type", "all");
            startActivity(intent);
        });

        tvSeeAllArtists.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ArtistsActivity.class));
        });

        TextView tvSeeAllExhibitions = view.findViewById(R.id.tvSeeAllExhibitions);
        tvSeeAllExhibitions.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ExhibitionListActivity.class);
            intent.putExtra("list_type", "all");
            startActivity(intent);
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadFeaturedContent);
        // Настраиваем цвета индикатора обновления
        swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
    }

    private void setupRecyclerViews() {
        // Исправленный конструктор ArtworkAdapter с 3 параметрами
        featuredArtworkAdapter = new ArtworkAdapter(featuredArtworks, new ArtworkAdapter.OnArtworkClickListener() {
            @Override
            public void onArtworkClick(Artwork artwork) {
                Intent intent = new Intent(getActivity(), ArtworkDetailActivity.class);
                intent.putExtra("artwork_id", artwork.getId());
                startActivity(intent);
            }

            @Override
            public void onEditClick(Artwork artwork) {
                // Не используется на главной странице
            }

            @Override
            public void onDeleteClick(Artwork artwork) {
                // Не используется на главной странице
            }
        }, false); // false - не показываем кнопки действий на главной странице

        LinearLayoutManager artworksLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvFeaturedArtworks.setLayoutManager(artworksLayoutManager);
        rvFeaturedArtworks.setAdapter(featuredArtworkAdapter);

        featuredArtistsAdapter = new ArtistsAdapter(featuredArtists, artist -> {
            // Открываем профиль пользователя вместо публикаций
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openUserProfile(artist.getId());
            }
        });

        LinearLayoutManager artistsLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvFeaturedArtists.setLayoutManager(artistsLayoutManager);
        rvFeaturedArtists.setAdapter(featuredArtistsAdapter);

        featuredExhibitionAdapter = new ExhibitionAdapter(featuredExhibitions, exhibition -> {
            Intent intent = new Intent(getActivity(), ExhibitionDetailActivity.class);
            intent.putExtra("exhibition_id", exhibition.getId());
            startActivity(intent);
        });

        LinearLayoutManager exhibitionsLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        rvFeaturedExhibitions.setLayoutManager(exhibitionsLayoutManager);
        rvFeaturedExhibitions.setAdapter(featuredExhibitionAdapter);
    }

    private void loadFeaturedContent() {
        progressBar.setVisibility(View.VISIBLE);

        artworkViewModel = new ViewModelProvider(requireActivity()).get(ArtworkViewModel.class);
        userViewModel = new ViewModelProvider(requireActivity()).get(UserViewModel.class);
        exhibitionViewModel = new ViewModelProvider(requireActivity()).get(ExhibitionViewModel.class);

        artworkViewModel.getArtworksResult().observe(getViewLifecycleOwner(), result -> {
            progressBar.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    List<Map<String, Object>> artworksData = (List<Map<String, Object>>) result.get("artworks");
                    if (artworksData != null && !artworksData.isEmpty()) {
                        List<Artwork> allArtworks = new ArrayList<>();
                        for (Map<String, Object> artworkData : artworksData) {
                            Artwork artwork = convertToArtwork(artworkData);
                            if (artwork != null) {
                                allArtworks.add(artwork);
                            }
                        }

                        featuredArtworks.clear();
                        if (allArtworks.size() > 4) {
                            Collections.shuffle(allArtworks);
                            featuredArtworks.addAll(allArtworks.subList(0, 4));
                        } else {
                            featuredArtworks.addAll(allArtworks);
                        }
                        featuredArtworkAdapter.notifyDataSetChanged();

                        tvArtworksEmpty.setVisibility(View.GONE);
                        rvFeaturedArtworks.setVisibility(View.VISIBLE);
                        Log.d("HomeFragment", getString(R.string.loaded_featured_artworks, featuredArtworks.size()));
                    } else {
                        tvArtworksEmpty.setVisibility(View.VISIBLE);
                        rvFeaturedArtworks.setVisibility(View.GONE);
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_artworks, message), Toast.LENGTH_SHORT).show();
                    tvArtworksEmpty.setVisibility(View.VISIBLE);
                    rvFeaturedArtworks.setVisibility(View.GONE);
                }
            }
        });

        userViewModel.getArtistsResult().observe(getViewLifecycleOwner(), result -> {
            swipeRefreshLayout.setRefreshing(false);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Object usersObj = result.get("users");
                    if (usersObj instanceof List) {
                        List<?> usersList = (List<?>) usersObj;
                        List<User> allArtists = new ArrayList<>();

                        for (Object userObj : usersList) {
                            if (userObj instanceof User) {
                                allArtists.add((User) userObj);
                            } else if (userObj instanceof Map) {
                                User user = convertToUser((Map<String, Object>) userObj);
                                if (user != null) {
                                    allArtists.add(user);
                                }
                            }
                        }

                        featuredArtists.clear();
                        if (allArtists.size() > 4) {
                            Collections.shuffle(allArtists);
                            featuredArtists.addAll(allArtists.subList(0, 4));
                        } else {
                            featuredArtists.addAll(allArtists);
                        }
                        featuredArtistsAdapter.notifyDataSetChanged();

                        tvArtistsEmpty.setVisibility(View.GONE);
                        rvFeaturedArtists.setVisibility(View.VISIBLE);
                        Log.d("HomeFragment", getString(R.string.loaded_featured_artists, featuredArtists.size()));
                    } else {
                        tvArtistsEmpty.setVisibility(View.VISIBLE);
                        rvFeaturedArtists.setVisibility(View.GONE);
                        Log.d("HomeFragment", "No users data found in response");
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_artists, message), Toast.LENGTH_SHORT).show();
                    tvArtistsEmpty.setVisibility(View.VISIBLE);
                    rvFeaturedArtists.setVisibility(View.GONE);
                }
            }
        });

        exhibitionViewModel.getExhibitionsResult().observe(getViewLifecycleOwner(), result -> {
            swipeRefreshLayout.setRefreshing(false);

            if (result != null) {
                Boolean success = (Boolean) result.get("success");
                if (success != null && success) {
                    Object exhibitionsObj = result.get("exhibitions");
                    if (exhibitionsObj instanceof List) {
                        List<?> exhibitionsList = (List<?>) exhibitionsObj;
                        List<Exhibition> allExhibitions = new ArrayList<>();

                        for (Object exhibitionObj : exhibitionsList) {
                            if (exhibitionObj instanceof Exhibition) {
                                allExhibitions.add((Exhibition) exhibitionObj);
                            } else if (exhibitionObj instanceof Map) {
                                Exhibition exhibition = convertToExhibition((Map<String, Object>) exhibitionObj);
                                if (exhibition != null) {
                                    allExhibitions.add(exhibition);
                                }
                            }
                        }

                        featuredExhibitions.clear();
                        if (allExhibitions.size() > 4) {
                            Collections.shuffle(allExhibitions);
                            featuredExhibitions.addAll(allExhibitions.subList(0, 4));
                        } else {
                            featuredExhibitions.addAll(allExhibitions);
                        }
                        featuredExhibitionAdapter.updateData(featuredExhibitions);

                        tvExhibitionsEmpty.setVisibility(View.GONE);
                        rvFeaturedExhibitions.setVisibility(View.VISIBLE);
                        Log.d("HomeFragment", getString(R.string.loaded_featured_exhibitions, featuredExhibitions.size()));
                    } else {
                        tvExhibitionsEmpty.setVisibility(View.VISIBLE);
                        rvFeaturedExhibitions.setVisibility(View.GONE);
                        Log.d("HomeFragment", "No exhibitions data found in response");
                    }
                } else {
                    String message = (String) result.get("message");
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_exhibitions, message), Toast.LENGTH_SHORT).show();
                    tvExhibitionsEmpty.setVisibility(View.VISIBLE);
                    rvFeaturedExhibitions.setVisibility(View.GONE);
                }
            }
        });

        artworkViewModel.getArtworks(0, 50);
        userViewModel.getAllArtists();
        exhibitionViewModel.getExhibitions(0, 50);
    }

    private Artwork convertToArtwork(Map<String, Object> artworkData) {
        try {
            Artwork artwork = new Artwork();

            if (artworkData.get("id") != null) {
                artwork.setId(((Number) artworkData.get("id")).longValue());
            }

            artwork.setTitle((String) artworkData.get("title"));
            artwork.setDescription((String) artworkData.get("description"));
            artwork.setImagePath((String) artworkData.get("imagePath"));
            artwork.setStatus((String) artworkData.get("status"));

            if (artworkData.get("likes") != null) {
                artwork.setLikes(((Number) artworkData.get("likes")).intValue());
            }
            if (artworkData.get("views") != null) {
                artwork.setViews(((Number) artworkData.get("views")).intValue());
            }

            if (artworkData.get("user") != null) {
                Map<String, Object> userData = (Map<String, Object>) artworkData.get("user");
                User user = convertToUser(userData);
                artwork.setUser(user);
            }

            return artwork;
        } catch (Exception e) {
            Log.e("HomeFragment", "Error converting artwork: " + e.getMessage());
            return null;
        }
    }

    private User convertToUser(Map<String, Object> userData) {
        try {
            User user = new User();

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

            Object countObj = userData.get("artworksCount");
            if (countObj != null) {
                if (countObj instanceof Double) {
                    user.setArtworksCount(((Double) countObj).intValue());
                } else if (countObj instanceof Integer) {
                    user.setArtworksCount((Integer) countObj);
                }
            }

            return user;
        } catch (Exception e) {
            Log.e("HomeFragment", "Error converting user: " + e.getMessage());
            return null;
        }
    }

    private Exhibition convertToExhibition(Map<String, Object> exhibitionData) {
        try {
            Exhibition exhibition = new Exhibition();

            if (exhibitionData.get("id") != null) {
                exhibition.setId(((Number) exhibitionData.get("id")).longValue());
            }

            exhibition.setTitle((String) exhibitionData.get("title"));
            exhibition.setDescription((String) exhibitionData.get("description"));
            exhibition.setImageUrl((String) exhibitionData.get("imageUrl"));

            if (exhibitionData.get("authorOnly") != null) {
                exhibition.setAuthorOnly((Boolean) exhibitionData.get("authorOnly"));
            }

            if (exhibitionData.get("artworksCount") != null) {
                exhibition.setArtworksCount(((Number) exhibitionData.get("artworksCount")).intValue());
            }

            if (exhibitionData.get("user") != null) {
                Map<String, Object> userData = (Map<String, Object>) exhibitionData.get("user");
                User user = convertToUser(userData);
                exhibition.setUser(user);
            }

            if (exhibitionData.get("firstArtwork") != null) {
                Map<String, Object> artworkData = (Map<String, Object>) exhibitionData.get("firstArtwork");
                Artwork artwork = convertToArtwork(artworkData);
                exhibition.setFirstArtwork(artwork);
            }

            return exhibition;
        } catch (Exception e) {
            Log.e("HomeFragment", "Error converting exhibition: " + e.getMessage());
            return null;
        }
    }
}