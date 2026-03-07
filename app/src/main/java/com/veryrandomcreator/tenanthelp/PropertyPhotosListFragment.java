package com.veryrandomcreator.tenanthelp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_photos);
        FloatingActionButton fabAddPhoto = view.findViewById(R.id.fab_add_photo);

        // Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Load actual data from secure storage
        propertyPackage = new PropertyPackage();
        try {
            propertyPackage.setImages(PhotoStorageManager.loadPhotos(requireContext()));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to load photos.", android.widget.Toast.LENGTH_SHORT).show();
        }

        PropertyPhotosAdapter adapter = new PropertyPhotosAdapter(propertyPackage.getImages(), item -> {
            PhotoFragment fragment = new PhotoFragment();
            Bundle bundle = new Bundle();
            bundle.putString("itemId", item.getId());
            fragment.setArguments(bundle);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, fragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerView.setAdapter(adapter);

        // Set up the FAB click listener to navigate to the PhotoFragment for a new item
        fabAddPhoto.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainerView, new PhotoFragment())
                    .addToBackStack(null) // Allows the user to use the back button to return to the list
                    .commit();
        });
    }
}
