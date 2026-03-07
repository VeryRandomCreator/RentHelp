package com.veryrandomcreator.tenanthelp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PropertiesAdapter extends RecyclerView.Adapter<PropertiesAdapter.PropertyViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(Property item);
    }

    private final List<Property> properties;
    private final OnItemClickListener listener;

    public PropertiesAdapter(List<Property> properties, OnItemClickListener listener) {
        this.properties = properties;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_property, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Property item = properties.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return properties == null ? 0 : properties.size();
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        private final TextView labelTextView;
        private final TextView descriptionTextView;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            labelTextView = itemView.findViewById(R.id.text_view_property_label);
            descriptionTextView = itemView.findViewById(R.id.text_view_property_description);
        }

        public void bind(Property item, OnItemClickListener listener) {
            labelTextView.setText(item.getLabel());
            descriptionTextView.setText(item.getDescription());
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}
