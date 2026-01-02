package com.example.vagmobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.vagmobile.R;
import com.example.vagmobile.model.Artwork;
import java.util.List;

public class ExhibitionArtworkAdapter extends RecyclerView.Adapter<ExhibitionArtworkAdapter.ExhibitionArtworkViewHolder> {

    private List<Artwork> artworkList;
    private OnArtworkClickListener onArtworkClickListener;
    private Long currentUserId;
    private Long exhibitionOwnerId;
    private boolean isUserLoggedIn;

    public ExhibitionArtworkAdapter(List<Artwork> artworkList, OnArtworkClickListener onArtworkClickListener,
                                   Long currentUserId, Long exhibitionOwnerId, boolean isUserLoggedIn) {
        this.artworkList = artworkList;
        this.onArtworkClickListener = onArtworkClickListener;
        this.currentUserId = currentUserId;
        this.exhibitionOwnerId = exhibitionOwnerId;
        this.isUserLoggedIn = isUserLoggedIn;
    }

    public void updateUserInfo(Long currentUserId, Long exhibitionOwnerId, boolean isUserLoggedIn) {
        this.currentUserId = currentUserId;
        this.exhibitionOwnerId = exhibitionOwnerId;
        this.isUserLoggedIn = isUserLoggedIn;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExhibitionArtworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artwork_with_actions, parent, false);
        return new ExhibitionArtworkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExhibitionArtworkViewHolder holder, int position) {
        Artwork artwork = artworkList.get(position);
        holder.bind(artwork, shouldShowDeleteButton(artwork));

        holder.itemView.setOnClickListener(v -> {
            if (onArtworkClickListener != null) {
                onArtworkClickListener.onArtworkClick(artwork);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (onArtworkClickListener != null) {
                onArtworkClickListener.onDeleteClick(artwork);
            }
        });
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

    /**
     * Определяет, нужно ли показывать кнопку "Удалить" для данной работы
     */
    private boolean shouldShowDeleteButton(Artwork artwork) {
        if (!isUserLoggedIn) {
            return false; // Неавторизованные пользователи не видят кнопку
        }

        // Если пользователь - создатель выставки, показываем кнопку на всех работах
        if (currentUserId != null && currentUserId.equals(exhibitionOwnerId)) {
            return true;
        }

        // Если пользователь - создатель работы, показываем кнопку только на его работах
        if (artwork.getUser() != null && currentUserId != null) {
            return currentUserId.equals(artwork.getUser().getId());
        }

        return false;
    }

    private String getStatusText(String status) {
        switch (status) {
            case "PENDING":
                return "На рассмотрении";
            case "APPROVED":
                return "Одобрено";
            case "REJECTED":
                return "Отклонено";
            default:
                return status;
        }
    }

    public interface OnArtworkClickListener {
        void onArtworkClick(Artwork artwork);
        void onEditClick(Artwork artwork);
        void onDeleteClick(Artwork artwork);
    }

    static class ExhibitionArtworkViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivArtwork;
        private TextView tvTitle, tvArtist, tvLikes, tvCategories, tvStatus;
        private Button btnEdit, btnDelete;

        public ExhibitionArtworkViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvCategories = itemView.findViewById(R.id.tvCategories);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Artwork artwork, boolean showDeleteButton) {
            tvTitle.setText(artwork.getTitle() != null ? artwork.getTitle() : "Без названия");
            tvLikes.setText("❤️ " + artwork.getLikes());

            // Отображение статуса
            if (artwork.getStatus() != null) {
                String statusText = getStatusText(artwork.getStatus());
                tvStatus.setText(statusText);
                tvStatus.setVisibility(View.VISIBLE);

                // Цвет статуса
                switch (artwork.getStatus()) {
                    case "PENDING":
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_pending));
                        break;
                    case "APPROVED":
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_approved));
                        break;
                    case "REJECTED":
                        tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.status_rejected));
                        break;
                }
            } else {
                tvStatus.setVisibility(View.GONE);
            }

            if (artwork.hasCategories()) {
                tvCategories.setText(artwork.getCategoriesString());
                tvCategories.setVisibility(View.VISIBLE);
            } else {
                tvCategories.setVisibility(View.GONE);
            }

            if (artwork.getUser() != null && artwork.getUser().getUsername() != null) {
                tvArtist.setText("Автор: " + artwork.getUser().getUsername());
            } else {
                tvArtist.setText("Неизвестный художник");
            }

            // Управляем видимостью кнопок для выставки
            btnEdit.setVisibility(View.GONE); // В выставке не показываем кнопку редактирования

            if (showDeleteButton) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setEnabled(true);
                btnDelete.setAlpha(1.0f);
                btnDelete.setText("Удалить из выставки");
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            if (artwork.getImagePath() != null && !artwork.getImagePath().isEmpty()) {
                String imageUrl = artwork.getImagePath();
                if (!imageUrl.startsWith("http")) {
                    imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imageUrl;
                }

                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image)
                        .centerCrop()
                        .into(ivArtwork);
            } else {
                ivArtwork.setImageResource(R.drawable.placeholder_image);
            }
        }

        private String getStatusText(String status) {
            switch (status) {
                case "PENDING":
                    return "На рассмотрении";
                case "APPROVED":
                    return "Одобрено";
                case "REJECTED":
                    return "Отклонено";
                default:
                    return status;
            }
        }
    }
}
