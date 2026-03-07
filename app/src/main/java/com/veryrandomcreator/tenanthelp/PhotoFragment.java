package com.veryrandomcreator.tenanthelp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.List;

import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.widget.Toast;

/*
Todo:
 - Ensure image label is not left empty
 */
public class PhotoFragment extends Fragment {

    private ImageView imageViewPhoto;
    private EditText imageNotesEdt;
    private EditText imageLabelEdt;
    private Button buttonTakePhoto;
    private Button buttonSavePhoto;
    private Button buttonDeletePhoto;
    
    // Store the bitmap when we take a picture or load an existing one
    private Bitmap currentBitmap;
    private Uri tempImageUri;

    // Register the contract to take a full-size picture
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    try {
                        // Decode the saved image from the temporary URI
                        java.io.InputStream inputStream = requireContext().getContentResolver().openInputStream(tempImageUri);
                        currentBitmap = BitmapFactory.decodeStream(inputStream);
                        imageViewPhoto.setImageBitmap(currentBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Failed to load captured image.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    public PhotoFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_photo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageViewPhoto = view.findViewById(R.id.image_view_photo);
        imageNotesEdt = view.findViewById(R.id.image_notes_edt);
        imageLabelEdt = view.findViewById(R.id.image_label_edt);
        buttonTakePhoto = view.findViewById(R.id.button_take_photo);
        buttonSavePhoto = view.findViewById(R.id.button_save_photo);
        buttonDeletePhoto = view.findViewById(R.id.button_delete_photo);

        buttonTakePhoto.setOnClickListener(v -> {
            try {
                // Create a temporary file in the cache directory
                File tempFile = File.createTempFile("temp_image", ".jpg", requireContext().getCacheDir());

                // Get the URI for the file using the FileProvider we configured
                tempImageUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        tempFile
                );

                // Launch the camera app
                takePictureLauncher.launch(tempImageUri);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Could not create temporary file for camera.", Toast.LENGTH_SHORT).show();
            }
        });

        buttonSavePhoto.setOnClickListener(v -> savePhotoData());

        // If we are editing an existing item, load its data and show the delete button
        if (getArguments() != null && getArguments().containsKey("itemId")) {
            String itemId = getArguments().getString("itemId");
            
            // Show the delete button
            buttonDeletePhoto.setVisibility(View.VISIBLE);
            buttonDeletePhoto.setOnClickListener(v -> deletePhotoData(itemId));
            
            try {
                // Find the matching text data from storage
                List<PropertyImage> items = PhotoStorageManager.loadPhotos(requireContext());
                for (PropertyImage item : items) {
                    if (item.getId().equals(itemId)) {
                        imageLabelEdt.setText(item.getLabel());
                        imageNotesEdt.setText(item.getNotes());
                        break;
                    }
                }

                // Load the image bitmap
                currentBitmap = PhotoStorageManager.loadPhotoBitmap(requireContext(), itemId, false);
                if (currentBitmap != null) {
                    imageViewPhoto.setImageBitmap(currentBitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to load existing photo data.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void savePhotoData() {
        String label = imageLabelEdt.getText().toString().trim();
        String notes = imageNotesEdt.getText().toString().trim();

        if (label.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an image label.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentBitmap == null) {
            Toast.makeText(requireContext(), "Please take a photo first.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable button to prevent multi-clicks and indicate progress
        buttonSavePhoto.setEnabled(false);
        buttonSavePhoto.setText("Saving...");

        PhotoStorageManager.savePhotoData(requireContext(), label, notes, currentBitmap, new PhotoStorageManager.SaveCallback() {
            @Override
            public void onSuccess(String id) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Saved securely!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error saving data.", Toast.LENGTH_SHORT).show();
            }
        });
        // Pop back to the list immediately while it saves in the background
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void deletePhotoData(String itemId) {
        // Disable button to prevent multi-clicks
        buttonDeletePhoto.setEnabled(false);
        buttonDeletePhoto.setText("Deleting...");
        buttonSavePhoto.setEnabled(false); // Also disable save to prevent conflicts

        PhotoStorageManager.deletePhotoData(requireContext(), itemId, new PhotoStorageManager.SaveCallback() {
            @Override
            public void onSuccess(String id) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Photo deleted.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error deleting data.", Toast.LENGTH_SHORT).show();

                // Re-enable so the user can try again
                buttonDeletePhoto.setEnabled(true);
                buttonDeletePhoto.setText("Delete");
                buttonSavePhoto.setEnabled(true);
            }
        });

        // Pop back immediately while it deletes in the background
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}

