package com.example.vagmobile.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.vagmobile.R;
import com.example.vagmobile.ui.activity.ArtistsActivity;
import com.example.vagmobile.ui.activity.ArtworkListActivity;
import com.example.vagmobile.ui.activity.CategoryActivity;
import com.example.vagmobile.ui.activity.ExhibitionListActivity;

public class MoreFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        LinearLayout btnExhibitions = view.findViewById(R.id.btnExhibitions);
        LinearLayout btnArtworks = view.findViewById(R.id.btnArtworks);
        LinearLayout btnArtists = view.findViewById(R.id.btnArtists);
        LinearLayout btnCategories = view.findViewById(R.id.btnCategories);

        btnExhibitions.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ExhibitionListActivity.class));
        });

        btnArtworks.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ArtworkListActivity.class);
            intent.putExtra("list_type", "all");
            startActivity(intent);
        });

        btnArtists.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ArtistsActivity.class));
        });

        btnCategories.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CategoryActivity.class));
        });

        return view;
    }
}