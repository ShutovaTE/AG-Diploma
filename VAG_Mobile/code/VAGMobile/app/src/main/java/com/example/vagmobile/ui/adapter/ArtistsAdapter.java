package com.example.vagmobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.vagmobile.R;
import com.example.vagmobile.model.User;

import java.util.List;

public class ArtistsAdapter extends RecyclerView.Adapter<ArtistsAdapter.ArtistViewHolder> {

    private List<User> artistList;
    private OnArtistClickListener onArtistClickListener;

    public interface OnArtistClickListener {
        void onArtistClick(User artist);
    }

    public ArtistsAdapter(List<User> artistList, OnArtistClickListener onArtistClickListener) {
        this.artistList = artistList;
        this.onArtistClickListener = onArtistClickListener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artist, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        User artist = artistList.get(position);
        holder.bind(artist);

        holder.itemView.setOnClickListener(v -> {
            if (onArtistClickListener != null) {
                onArtistClickListener.onArtistClick(artist);
            }
        });
    }

    @Override
    public int getItemCount() {
        return artistList != null ? artistList.size() : 0;
    }

    public void updateData(List<User> newArtistList) {
        this.artistList = newArtistList;
        notifyDataSetChanged();
    }

    static class ArtistViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivArtistAvatar;
        private TextView tvArtistName, tvArtistEmail, tvArtworksCount;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtistAvatar = itemView.findViewById(R.id.ivArtistAvatar);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
            tvArtistEmail = itemView.findViewById(R.id.tvArtistEmail);
            tvArtworksCount = itemView.findViewById(R.id.tvArtworksCount);
        }

        public void bind(User artist) {
            tvArtistName.setText(artist.getUsername());
            tvArtistEmail.setText(artist.getEmail() != null ? artist.getEmail() : "Email не указан");

            int artworksCount = artist.getArtworksCount() != null ? artist.getArtworksCount() : 0;
            if (artworksCount == 0) {
                tvArtworksCount.setText("Нет публикаций");
            } else if (artworksCount == 1) {
                tvArtworksCount.setText("1 публикация");
            } else if (artworksCount < 5) {
                tvArtworksCount.setText(artworksCount + " публикации");
            } else {
                tvArtworksCount.setText(artworksCount + " публикаций");
            }

            Glide.with(itemView.getContext())
                    .load(R.drawable.ic_people)
                    .circleCrop()
                    .into(ivArtistAvatar);
        }
    }
}