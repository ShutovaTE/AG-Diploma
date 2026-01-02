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
import com.example.vagmobile.model.Exhibition;
import java.util.List;

public class ExhibitionAdapter extends RecyclerView.Adapter<ExhibitionAdapter.ExhibitionViewHolder> {
    private List<Exhibition> exhibitionList;
    private OnExhibitionClickListener onExhibitionClickListener;

    public ExhibitionAdapter(List<Exhibition> exhibitionList, OnExhibitionClickListener onExhibitionClickListener) {
        this.exhibitionList = exhibitionList;
        this.onExhibitionClickListener = onExhibitionClickListener;
    }

    @NonNull
    @Override
    public ExhibitionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exhibition, parent, false);
        return new ExhibitionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExhibitionViewHolder holder, int position) {
        Exhibition exhibition = exhibitionList.get(position);
        holder.bind(exhibition);

        holder.itemView.setOnClickListener(v -> {
            if (onExhibitionClickListener != null) {
                onExhibitionClickListener.onExhibitionClick(exhibition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return exhibitionList != null ? exhibitionList.size() : 0;
    }

    public void updateData(List<Exhibition> newExhibitionList) {
        this.exhibitionList = newExhibitionList;
        notifyDataSetChanged();
    }

    static class ExhibitionViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivExhibitionImage, ivPrivate;
        private TextView tvTitle, tvDescription, tvArtworksCount, tvAuthor;

        public ExhibitionViewHolder(@NonNull View itemView) {
            super(itemView);
            ivExhibitionImage = itemView.findViewById(R.id.ivExhibitionImage);
            ivPrivate = itemView.findViewById(R.id.ivPrivate);
            tvTitle = itemView.findViewById(R.id.tvExhibitionTitle);
            tvDescription = itemView.findViewById(R.id.tvExhibitionDescription);
            tvArtworksCount = itemView.findViewById(R.id.tvArtworksCount);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
        }

        public void bind(Exhibition exhibition) {
            tvTitle.setText(exhibition.getTitle());
            tvDescription.setText(exhibition.getDescription());
            tvArtworksCount.setText(exhibition.getArtworksCountText());

            if (exhibition.getUser() != null) {
                tvAuthor.setText("Автор: " + exhibition.getUser().getUsername());
                tvAuthor.setVisibility(View.VISIBLE);
            } else {
                tvAuthor.setVisibility(View.GONE);
            }

            if (exhibition.isAuthorOnly()) {
                ivPrivate.setVisibility(View.VISIBLE);
            } else {
                ivPrivate.setVisibility(View.GONE);
            }

            // Загрузка изображения первой работы или изображения выставки
            String imageUrl = null;
            if (exhibition.hasImage()) {
                imageUrl = exhibition.getImageUrl();
            } else if (exhibition.getFirstArtwork() != null && exhibition.getFirstArtwork().getImagePath() != null) {
                // Преобразуем относительный путь в полный URL
                String relativePath = exhibition.getFirstArtwork().getImagePath();
                if (!relativePath.startsWith("http")) {
                    imageUrl = "http://192.168.0.40:8080/vag/uploads/" + relativePath;
                } else {
                    imageUrl = relativePath;
                }
            }

            if (imageUrl != null) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(ivExhibitionImage);
            } else {
                ivExhibitionImage.setImageResource(R.drawable.placeholder_image);
            }
        }
    }

    public interface OnExhibitionClickListener {
        void onExhibitionClick(Exhibition exhibition);
    }
}
