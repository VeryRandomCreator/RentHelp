package com.veryrandomcreator.tenanthelp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PropertyPhotosAdapter extends RecyclerView.Adapter<PropertyPhotosAdapter.PhotoViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(PropertyImage item);
    }

    private final List<PropertyImage> photos;
    private final OnItemClickListener listener;

    public PropertyPhotosAdapter(List<PropertyImage> photos, OnItemClickListener listener) {
        this.photos = photos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_property_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        PropertyImage item = photos.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return photos == null ? 0 : photos.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final TextView labelTextView;
        private final TextView notesTextView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            labelTextView = itemView.findViewById(R.id.text_view_label);
            notesTextView = itemView.findViewById(R.id.text_view_notes);
        }

        public void bind(PropertyImage item, OnItemClickListener listener) {
            labelTextView.setText(item.getLabel());
            notesTextView.setText(item.getNotes());
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
