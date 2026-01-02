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

public class ArtworkAdapter extends RecyclerView.Adapter<ArtworkAdapter.ArtworkViewHolder> {
    private List<Artwork> artworkList;
    private OnArtworkClickListener onArtworkClickListener;
    private boolean showActions; // ДОБАВЛЕНО: показывать ли кнопки действий

    public ArtworkAdapter(List<Artwork> artworkList, OnArtworkClickListener onArtworkClickListener, boolean showActions) {
        this.artworkList = artworkList;
        this.onArtworkClickListener = onArtworkClickListener;
        this.showActions = showActions;
    }

    @NonNull
    @Override
    public ArtworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_artwork_with_actions, parent, false); // ИСПРАВЛЕНО
        return new ArtworkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtworkViewHolder holder, int position) {
        Artwork artwork = artworkList.get(position);
        holder.bind(artwork, showActions); // ДОБАВЛЕНО параметр

        holder.itemView.setOnClickListener(v -> {
            if (onArtworkClickListener != null) {
                onArtworkClickListener.onArtworkClick(artwork);
            }
        });

        // ДОБАВЛЕНО: Обработчики кнопок
        if (showActions) {
            holder.btnEdit.setOnClickListener(v -> {
                if (onArtworkClickListener != null) {
                    onArtworkClickListener.onEditClick(artwork);
                }
            });

            holder.btnDelete.setOnClickListener(v -> {
                if (onArtworkClickListener != null) {
                    onArtworkClickListener.onDeleteClick(artwork);
                }
            });
        }
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

    static class ArtworkViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivArtwork;
        private TextView tvTitle, tvArtist, tvLikes, tvCategories, tvStatus;
        private Button btnEdit, btnDelete; // ДОБАВЛЕНО

        public ArtworkViewHolder(@NonNull View itemView) {
            super(itemView);
            ivArtwork = itemView.findViewById(R.id.ivArtwork);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvArtist = itemView.findViewById(R.id.tvArtist);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvCategories = itemView.findViewById(R.id.tvCategories);
            tvStatus = itemView.findViewById(R.id.tvStatus); // ДОБАВЛЕНО
            btnEdit = itemView.findViewById(R.id.btnEdit); // ДОБАВЛЕНО
            btnDelete = itemView.findViewById(R.id.btnDelete); // ДОБАВЛЕНО
        }

        public void bind(Artwork artwork, boolean showActions) {
            tvTitle.setText(artwork.getTitle());
            tvLikes.setText("❤️ " + artwork.getLikes());

            // ДОБАВЛЕНО: Отображение статуса
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

            if (showActions) {
                // Показываем обе кнопки
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                // Настраиваем состояние кнопки редактирования
                if ("APPROVED".equals(artwork.getStatus())) {
                    // Для одобренных публикаций делаем кнопку редактирования неактивной
                    btnEdit.setEnabled(false);
                    btnEdit.setAlpha(0.5f);
                    btnEdit.setText("Нельзя редактировать");
                } else {
                    // Для PENDING и REJECTED кнопка активна
                    btnEdit.setEnabled(true);
                    btnEdit.setAlpha(1.0f);
                    btnEdit.setText("Редактировать");
                }

                // Кнопка удаления всегда активна
                btnDelete.setEnabled(true);
                btnDelete.setAlpha(1.0f);
                btnDelete.setText("Удалить");
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }


            if (artwork.getImagePath() != null && !artwork.getImagePath().isEmpty()) {
                String imagePath = artwork.getImagePath();
                if (imagePath.startsWith("/")) {
                    imagePath = imagePath.substring(1);
                }
//                String imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imagePath;
                String imageUrl = "http://192.168.0.40:8080/vag/uploads/" + imagePath;
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_error)
                        .into(ivArtwork);
            } else {
                ivArtwork.setImageResource(R.drawable.ic_placeholder);
            }
        }

        // ДОБАВЛЕНО: Метод для преобразования статуса
        private String getStatusText(String status) {
            switch (status) {
                case "PENDING": return "На рассмотрении";
                case "APPROVED": return "Одобрено";
                case "REJECTED": return "Отклонено";
                default: return "Неизвестно";
            }
        }
    }

    public interface OnArtworkClickListener {
        void onArtworkClick(Artwork artwork);
        void onEditClick(Artwork artwork); // ДОБАВЛЕНО
        void onDeleteClick(Artwork artwork); // ДОБАВЛЕНО
    }
}