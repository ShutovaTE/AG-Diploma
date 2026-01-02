package com.example.vagmobile.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.vagmobile.R;
import com.example.vagmobile.model.Comment;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> commentList;
    private SimpleDateFormat dateFormat;

    public CommentAdapter(List<Comment> commentList) {
        this.commentList = commentList;
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", new Locale("ru"));
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        int count = commentList != null ? commentList.size() : 0;
        Log.d("COMMENT_ADAPTER", "getItemCount() возвращает: " + count);
        return count;
    }

    public void updateComments(List<Comment> comments) {
        Log.d("COMMENT_ADAPTER", "updateComments() вызван с размером: " + (comments != null ? comments.size() : 0));
        this.commentList = comments;
        notifyDataSetChanged();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUserName;
        private TextView tvCommentContent;
        private TextView tvCommentDate;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            tvCommentDate = itemView.findViewById(R.id.tvCommentDate);
        }

        public void bind(Comment comment) {
            tvCommentContent.setText(comment.getContent());

            if (comment.getUser() != null) {
                tvUserName.setText(comment.getUser().getUsername());
            } else {
                tvUserName.setText("Аноним");
            }

            if (comment.getDateCreated() != null) {
                try {
                    String formattedDate = dateFormat.format(comment.getDateCreated());
                    tvCommentDate.setText(formattedDate);
                    tvCommentDate.setVisibility(View.VISIBLE);
                    Log.d("COMMENT_ADAPTER", "Formatted date: " + formattedDate);
                } catch (Exception e) {
                    Log.e("COMMENT_ADAPTER", "Error formatting date: " + e.getMessage());
                    tvCommentDate.setVisibility(View.GONE);
                }
            } else {
                Log.d("COMMENT_ADAPTER", "Comment date is null");
                tvCommentDate.setVisibility(View.GONE);
            }
        }
    }
}