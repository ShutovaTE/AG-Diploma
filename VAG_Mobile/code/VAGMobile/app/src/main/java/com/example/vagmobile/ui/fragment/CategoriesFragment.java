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
import com.example.vagmobile.model.Category;
import com.example.vagmobile.ui.activity.ArtworkListActivity;
import com.example.vagmobile.ui.activity.CategoryDetailActivity;
import com.example.vagmobile.ui.adapter.CategoryAdapter;
import com.example.vagmobile.viewmodel.CategoryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CategoriesFragment extends Fragment {

    private CategoryViewModel categoryViewModel;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private CategoryAdapter categoryAdapter;
    private List<Category> categoryList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        initViews(view);
        setupRecyclerView();
        observeViewModels();
        loadCategories();

        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter(categoryList, category -> {
            Intent intent = new Intent(getActivity(), CategoryDetailActivity.class);
            intent.putExtra("category_id", category.getId());
            intent.putExtra("category_name", category.getName());
            intent.putExtra("category_description", category.getDescription());
            startActivity(intent);
        });

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(categoryAdapter);
    }

    private void observeViewModels() {
        categoryViewModel = new ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())).get(CategoryViewModel.class);

        categoryViewModel.getCategoriesResult().observe(getViewLifecycleOwner(), result -> {
            progressBar.setVisibility(View.GONE);

            if (result != null) {
                System.out.println("CategoriesFragment: Received result: " + result);
                Boolean success = (Boolean) result.get("success");
                System.out.println("CategoriesFragment: Success: " + success);

                if (success != null && success) {
                    Object categoriesObj = result.get("categories");
                    System.out.println("CategoriesFragment: Categories object: " + categoriesObj);
                    System.out.println("CategoriesFragment: Categories object type: " + (categoriesObj != null ? categoriesObj.getClass().getName() : "null"));

                    if (categoriesObj instanceof List) {
                        List<?> categoriesList = (List<?>) categoriesObj;
                        System.out.println("CategoriesFragment: Categories list size: " + categoriesList.size());

                        categoryList.clear();
                        for (Object categoryObj : categoriesList) {
                            if (categoryObj instanceof Map) {
                                Map<String, Object> categoryData = (Map<String, Object>) categoryObj;
                                System.out.println("CategoriesFragment: Category data: " + categoryData);
                                Category category = convertToCategory(categoryData);
                                categoryList.add(category);
                            }
                        }
                        System.out.println("CategoriesFragment: Final category list size: " + categoryList.size());
                        categoryAdapter.notifyDataSetChanged();
                    } else {
                        System.out.println("CategoriesFragment: Categories is not a List!");
                        Toast.makeText(getContext(), getString(R.string.failed_to_parse_categories), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String message = (String) result.get("message");
                    System.out.println("CategoriesFragment: Error message: " + message);
                    Toast.makeText(getContext(), getString(R.string.failed_to_load_categories, message), Toast.LENGTH_SHORT).show();
                }
            } else {
                System.out.println("CategoriesFragment: Result is null!");
                Toast.makeText(getContext(), getString(R.string.no_data_received), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadCategories() {
        progressBar.setVisibility(View.VISIBLE);
        categoryViewModel.getCategories();
    }

    private Category convertToCategory(Map<String, Object> categoryData) {
        Category category = new Category();

        Object idObj = categoryData.get("id");
        if (idObj != null) {
            if (idObj instanceof Double) {
                category.setId(((Double) idObj).longValue());
            } else if (idObj instanceof Integer) {
                category.setId(((Integer) idObj).longValue());
            } else if (idObj instanceof Long) {
                category.setId((Long) idObj);
            }
        }

        category.setName(categoryData.get("name") != null ? categoryData.get("name").toString() : "Без названия");
        category.setDescription(categoryData.get("description") != null ? categoryData.get("description").toString() : "");

        Object countObj = categoryData.get("approvedArtworksCount");
        if (countObj != null) {
            if (countObj instanceof Double) {
                category.setApprovedArtworksCount(((Double) countObj).longValue());
            } else if (countObj instanceof Integer) {
                category.setApprovedArtworksCount(((Integer) countObj).longValue());
            } else if (countObj instanceof Long) {
                category.setApprovedArtworksCount((Long) countObj);
            } else {
                category.setApprovedArtworksCount(0L);
            }
        } else {
            category.setApprovedArtworksCount(0L);
        }

        return category;
    }
}