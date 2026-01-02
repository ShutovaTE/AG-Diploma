package com.example.vagmobile.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vagmobile.R;
import com.example.vagmobile.model.DocPage;
import com.example.vagmobile.ui.adapter.DocumentationAdapter;
import com.example.vagmobile.viewmodel.DocumentationViewModel;

public class DocumentationFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private DocumentationAdapter adapter;
    private DocumentationViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documentation_native, container, false);
        initViews(view);
        setupViewModel();
        setupRecyclerView();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(DocumentationViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new DocumentationAdapter(viewModel.docPages, this::onDocPageSelected);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void onDocPageSelected(DocPage docPage) {
        DocumentationDetailFragment detailFragment = DocumentationDetailFragment.newInstance(docPage);
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack("documentation")
                .commit();
    }
}