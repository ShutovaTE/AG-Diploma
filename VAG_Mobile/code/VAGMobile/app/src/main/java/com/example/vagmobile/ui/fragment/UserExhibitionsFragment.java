package com.example.vagmobile.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
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

import com.example.vagmobile.R;
import com.example.vagmobile.model.Exhibition;
import com.example.vagmobile.ui.activity.ExhibitionDetailActivity;
import com.example.vagmobile.ui.adapter.ExhibitionAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UserExhibitionsFragment extends Fragment {

    private static final String ARG_USER_ID = "user_id";

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ExhibitionAdapter exhibitionAdapter;
    private List<Exhibition> exhibitionList = new ArrayList<>();
    private Long userId;

    public static UserExhibitionsFragment newInstance(Long userId) {
        UserExhibitionsFragment fragment = new UserExhibitionsFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_exhibitions, container, false);

        initViews(view);
        setupRecyclerView();
        loadUserExhibitions();

        return view;
    }


    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);
    }

    private void setupRecyclerView() {
        exhibitionAdapter = new ExhibitionAdapter(exhibitionList, exhibition -> {
            // Обработка клика по выставке
            Intent intent = new Intent(getActivity(), ExhibitionDetailActivity.class);
            intent.putExtra("exhibition_id", exhibition.getId());
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(exhibitionAdapter);

        // Оптимизации для плавной прокрутки
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }


    private void loadUserExhibitions() {
        if (userId != null) {
            progressBar.setVisibility(View.VISIBLE);
            // Создаем репозиторий с контекстом для загрузки выставок пользователя
            com.example.vagmobile.repository.ExhibitionRepository repoWithContext = new com.example.vagmobile.repository.ExhibitionRepository(getContext());
            repoWithContext.getUserExhibitions(userId, 0, 50).observe(getViewLifecycleOwner(), result -> {
                progressBar.setVisibility(View.GONE);

                if (result != null) {
                    Boolean success = (Boolean) result.get("success");
                    if (success != null && success) {
                        List<Map<String, Object>> exhibitionsData = (List<Map<String, Object>>) result.get("exhibitions");

                        if (exhibitionsData != null && !exhibitionsData.isEmpty()) {
                            exhibitionList.clear();
                            // Используем Set для отслеживания уже добавленных ID и предотвращения дубликатов
                            java.util.Set<Long> addedExhibitionIds = new java.util.HashSet<>();
                            for (Map<String, Object> exhibitionData : exhibitionsData) {
                                Exhibition exhibition = convertToExhibition(exhibitionData);
                                // Проверяем, не добавляли ли уже эту выставку
                                if (exhibition.getId() != null && !addedExhibitionIds.contains(exhibition.getId())) {
                                    exhibitionList.add(exhibition);
                                    addedExhibitionIds.add(exhibition.getId());
                                }
                            }
                            exhibitionAdapter.notifyDataSetChanged();
                            showContent();
                        } else {
                            showEmpty("У пользователя пока нет выставок");
                        }
                    } else {
                        String message = (String) result.get("message");
                        showError("Не удалось загрузить выставки: " + message);
                    }
                }
            });
        }
    }

    private void showContent() {
        recyclerView.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
    }

    private void showEmpty(String message) {
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.VISIBLE);
        tvEmpty.setText(message);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        showEmpty("Ошибка загрузки выставок");
    }

    private Exhibition convertToExhibition(Map<String, Object> exhibitionData) {
        Exhibition exhibition = new Exhibition();

        if (exhibitionData.get("id") != null) {
            if (exhibitionData.get("id") instanceof Double) {
                exhibition.setId(((Double) exhibitionData.get("id")).longValue());
            } else if (exhibitionData.get("id") instanceof Long) {
                exhibition.setId((Long) exhibitionData.get("id"));
            } else if (exhibitionData.get("id") instanceof Integer) {
                exhibition.setId(((Integer) exhibitionData.get("id")).longValue());
            }
        }

        exhibition.setTitle((String) exhibitionData.get("title"));
        exhibition.setDescription((String) exhibitionData.get("description"));
        exhibition.setImageUrl((String) exhibitionData.get("imageUrl"));

        if (exhibitionData.get("authorOnly") != null) {
            exhibition.setAuthorOnly((Boolean) exhibitionData.get("authorOnly"));
        }

        if (exhibitionData.get("createdAt") != null) {
            // TODO: Обработать дату создания
        }

        if (exhibitionData.get("artworksCount") != null) {
            if (exhibitionData.get("artworksCount") instanceof Double) {
                exhibition.setArtworksCount(((Double) exhibitionData.get("artworksCount")).intValue());
            } else if (exhibitionData.get("artworksCount") instanceof Integer) {
                exhibition.setArtworksCount((Integer) exhibitionData.get("artworksCount"));
            } else if (exhibitionData.get("artworksCount") instanceof Long) {
                exhibition.setArtworksCount(((Long) exhibitionData.get("artworksCount")).intValue());
            }
        }

        // Обработка данных пользователя
        Object userObj = exhibitionData.get("user");
        if (userObj instanceof Map) {
            Map<String, Object> userData = (Map<String, Object>) userObj;
            com.example.vagmobile.model.User user = new com.example.vagmobile.model.User();

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

            user.setUsername((String) userData.get("username"));
            user.setEmail((String) userData.get("email"));
            user.setDescription((String) userData.get("description"));

            exhibition.setUser(user);
        }

        // Обработка первой работы
        Object firstArtworkObj = exhibitionData.get("firstArtwork");
        if (firstArtworkObj instanceof Map) {
            Map<String, Object> artworkData = (Map<String, Object>) firstArtworkObj;
            com.example.vagmobile.model.Artwork firstArtwork = new com.example.vagmobile.model.Artwork();

            if (artworkData.get("id") != null) {
                if (artworkData.get("id") instanceof Double) {
                    firstArtwork.setId(((Double) artworkData.get("id")).longValue());
                } else if (artworkData.get("id") instanceof Long) {
                    firstArtwork.setId((Long) artworkData.get("id"));
                } else if (artworkData.get("id") instanceof Integer) {
                    firstArtwork.setId(((Integer) artworkData.get("id")).longValue());
                }
            }

            firstArtwork.setTitle((String) artworkData.get("title"));
            firstArtwork.setImagePath((String) artworkData.get("imagePath"));

            exhibition.setFirstArtwork(firstArtwork);
        }

        return exhibition;
    }
}
