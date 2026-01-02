package com.example.vagmobile.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vagmobile.R;
import com.example.vagmobile.model.DocPage;

import java.util.ArrayList;
import java.util.List;

public class DocumentationAdapter extends RecyclerView.Adapter<DocumentationAdapter.ViewHolder> {

    private List<DocPage> pages;
    private final OnDocPageClickListener listener;

    public DocumentationAdapter(List<DocPage> pages, OnDocPageClickListener listener) {
        this.pages = new ArrayList<>(pages);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doc_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocPage page = pages.get(position);
        holder.bind(page, listener);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public interface OnDocPageClickListener {
        void onDocPageClick(DocPage docPage);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_doc_title);
        }

        void bind(DocPage page, OnDocPageClickListener listener) {
            titleTextView.setText(page.getTitle());
            itemView.setOnClickListener(v -> listener.onDocPageClick(page));
        }
    }
}