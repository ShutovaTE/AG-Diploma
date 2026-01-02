package com.example.vagmobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectableArtworkAdapter extends RecyclerView.Adapter<SelectableArtworkAdapter.SelectableArtworkViewHolder> {
    private List<Artwork> artworkList;
    private Set<Long> selectedArtworkIds = new HashSet<>();
    private OnSelectionChangedListener onSelectionChangedListener;

    public SelectableArtworkAdapter(List<Artwork> artworkList, OnSelectionChangedListener onSelectionChangedListener) {
        this.artworkList = artworkList;
        this.onSelectionChangedListener = onSelectionChangedListener;
    }

    @NonNull
    @Override
    public SelectableArtworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selectable_artwork, parent, false);
        return new SelectableArtworkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectableArtworkViewHolder holder, int position) {
        Artwork artwork = artworkList.get(position);
        holder.bind(artwork, selectedArtworkIds.contains(artwork.getId()));
    }

    @Override
    public int getItemCount() {
        return artworkList != null ? artworkList.size() : 0;
    }

    public void updateData(List<Artwork> newArtworkList) {
        this.artworkList = newArtworkList;
        notifyDataSetChanged();
    }

    public void addItems(List<Artwork> newItems) {
        if (newItems != null && !newItems.isEmpty()) {
            int startPosition = this.artworkList.size();
            this.artworkList.addAll(newItems);
            notifyItemRangeInserted(startPosition, newItems.size());
        }
    }

    public Set<Long> getSelectedArtworkIds() {
        return new HashSet<>(selectedArtworkIds);
    }

    public List<Artwork> getSelectedArtworks() {
        List<Artwork> selectedArtworks = new ArrayList<>();
        for (Artwork artwork : artworkList) {
            if (selectedArtworkIds.contains(artwork.getId())) {
                selectedArtworks.add(artwork);
            }
        }
        return selectedArtworks;
    }

    public void clearSelection() {
        selectedArtworkIds.clear();
        notifyDataSetChanged();
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(0);
        }
    }

    public void selectAll() {
        selectedArtworkIds.clear();
        for (Artwork artwork : artworkList) {
            selectedArtworkIds.add(artwork.getId());
        }
        notifyDataSetChanged();
        if (onSelectionChangedListener != null) {
            onSelectionChangedListener.onSelectionChanged(selectedArtworkIds.size());
        }
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    class SelectableArtworkViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivArtwork;
        private TextView tvTitle, tvArtist, tvLikes, tvCategories;
        private CheckBox checkBox;

        public SelectableArtworkViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvCategories = itemView.findViewById(R.id.tvCategories);
            checkBox = itemView.findViewById(R.id.checkBox);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Artwork artwork = artworkList.get(position);
                    toggleSelection(artwork.getId());
                }
            });

            checkBox.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Artwork artwork = artworkList.get(position);
                    toggleSelection(artwork.getId());
                }
            });
        }

        private void toggleSelection(Long artworkId) {
            if (selectedArtworkIds.contains(artworkId)) {
                selectedArtworkIds.remove(artworkId);
            } else {
                selectedArtworkIds.add(artworkId);
            }

            if (onSelectionChangedListener != null) {
                onSelectionChangedListener.onSelectionChanged(selectedArtworkIds.size());
            }

            notifyItemChanged(getAdapterPosition());
        }

        public void bind(Artwork artwork, boolean isSelected) {
            tvTitle.setText(artwork.getTitle() != null ? artwork.getTitle() : "Без названия");

            if (artwork.getUser() != null) {
                tvArtist.setText(artwork.getUser().getUsername() != null ?
                    artwork.getUser().getUsername() : "Неизвестный автор");
            } else {
                tvArtist.setText("Неизвестный автор");
            }

            tvLikes.setText("❤️ " + artwork.getLikes());

            // Загрузка изображения
            String imageUrl = artwork.getImagePath();
            if (imageUrl != null && !imageUrl.startsWith("http")) {
                imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imageUrl;
            }

            if (imageUrl != null) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(ivArtwork);
            } else {
                ivArtwork.setImageResource(R.drawable.placeholder_image);
            }

            // Настройка чекбокса
            checkBox.setChecked(isSelected);

            // Категории (если есть)
            if (artwork.getCategories() != null && !artwork.getCategories().isEmpty()) {
                StringBuilder categoriesText = new StringBuilder();
                for (int i = 0; i < Math.min(artwork.getCategories().size(), 2); i++) {
                    if (i > 0) categoriesText.append(", ");
                    categoriesText.append(artwork.getCategories().get(i).getName());
                }
                if (artwork.getCategories().size() > 2) {
                    categoriesText.append("...");
                }
                tvCategories.setText(categoriesText.toString());
                tvCategories.setVisibility(View.VISIBLE);
            } else {
                tvCategories.setVisibility(View.GONE);
            }
        }
    }
}
