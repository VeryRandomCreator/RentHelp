package com.veryrandomcreator.tenanthelp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

// TODO:
//  - UPDATE RECYCLERVIEW AFTER NEW ITEM HAS BEEN ADDED (ONCE FRAGMENT HAS BEEN POPPED BACK INTO STACK)
public class PropertyPhotosListFragment extends Fragment {

    private PropertyPackage propertyPackage;
    private String propertyId;
    private EditText labelEditText;
    private EditText descriptionEditText;
    private RecyclerView recyclerView;

    public PropertyPhotosListFragment() { }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_property_photos_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            propertyId = getArguments().getString("propertyId");
        }

        labelEditText = view.findViewById(R.id.edit_text_property_label);
        descriptionEditText = view.findViewById(R.id.edit_text_property_description);

        if (propertyId != null) {
            try {
                Property property = PropertyStorageManager.loadProperty(requireContext(), propertyId);
                if (property != null) {
                    labelEditText.setText(property.getLabel());
                    descriptionEditText.setText(property.getDescription());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        recyclerView = view.findViewById(R.id.recycler_view_photos);
        FloatingActionButton fabAddPhoto = view.findViewById(R.id.fab_add_photo);

        // Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        refreshPhotosList();

        // Set up the FAB click listener to navigate to the PhotoFragment for a new item
        fabAddPhoto.setOnClickListener(v -> {
            PhotoFragment fragment = new PhotoFragment();
            Bundle bundle = new Bundle();
            bundle.putString("propertyId", propertyId);
            fragment.setArguments(bundle);
            fragment.setOnDismissCallback(this::refreshPhotosList);
            fragment.show(getChildFragmentManager(), "photo_fragment");
        });
    }

    private void refreshPhotosList() {
        // Load actual data from secure storage
        propertyPackage = new PropertyPackage();
        try {
            propertyPackage.setImages(PhotoStorageManager.loadPropertyImageData(requireContext(), propertyId));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to load photos.", Toast.LENGTH_SHORT).show();
        }

        PropertyPhotosAdapter adapter = new PropertyPhotosAdapter(propertyPackage.getImages(), item -> {
            PhotoFragment fragment = new PhotoFragment();
            Bundle bundle = new Bundle();
            bundle.putString("itemId", item.getId());
            bundle.putString("propertyId", propertyId);
            fragment.setArguments(bundle);
            fragment.setOnDismissCallback(this::refreshPhotosList);
            fragment.show(getChildFragmentManager(), "photo_fragment");
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (propertyId != null) {
            String newLabel = labelEditText.getText().toString();
            String newDesc = descriptionEditText.getText().toString();
            Property p = new Property(propertyId, newLabel, newDesc);
            PropertyStorageManager.savePropertyData(requireContext(), p, null);
        }
    }
}
