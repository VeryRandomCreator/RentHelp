package com.veryrandomcreator.tenanthelp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

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
public class PhotoFragment extends BottomSheetDialogFragment {

    // Callback invoked after the sheet is dismissed so the parent can refresh its
    // list
    public interface OnDismissCallback {
        void onPhotoDismissed();
    }

    private OnDismissCallback dismissCallback;

    public void setOnDismissCallback(OnDismissCallback callback) {
        this.dismissCallback = callback;
    }

    private ImageView imageViewPhoto;
    private EditText imageNotesEdt;
    private EditText imageLabelEdt;
    private Button buttonTakePhoto;
    private ImageButton buttonMoreOptions;

    // Store the bitmap when we take a picture or load an existing one
    private Bitmap currentBitmap;
    private Uri tempImageUri;
    // True only when the user has taken a NEW photo in this session
    private boolean bitmapChanged = false;

    // Register the contract to take a full-size picture
    private final ActivityResultLauncher<Uri> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success) {
                    try {
                        // Decode the saved image from the temporary URI
                        java.io.InputStream inputStream = requireContext().getContentResolver()
                                .openInputStream(tempImageUri);
                        currentBitmap = BitmapFactory.decodeStream(inputStream);
                        imageViewPhoto.setImageBitmap(currentBitmap);
                        bitmapChanged = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Failed to load captured image.", Toast.LENGTH_SHORT)
                                .show();
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

        // Prevent drag-to-dismiss: expand fully and make it not hideable
        View bottomSheetView = getDialog() != null && getDialog().getWindow() != null
                ? requireDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet)
                : null;
        if (bottomSheetView != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheetView);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setHideable(false);
            behavior.setDraggable(false);
        }

        imageViewPhoto = view.findViewById(R.id.image_view_photo);
        imageNotesEdt = view.findViewById(R.id.image_notes_edt);
        imageLabelEdt = view.findViewById(R.id.image_label_edt);
        buttonTakePhoto = view.findViewById(R.id.button_take_photo);
        buttonMoreOptions = view.findViewById(R.id.button_more_options);

        buttonTakePhoto.setOnClickListener(v -> {
            try {
                // Create a temporary file in the cache directory
                File tempFile = File.createTempFile("temp_image", ".jpg", requireContext().getCacheDir());

                // Get the URI for the file using the FileProvider we configured
                tempImageUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        tempFile);

                // Launch the camera app
                takePictureLauncher.launch(tempImageUri);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Could not create temporary file for camera.", Toast.LENGTH_SHORT)
                        .show();
            }
        });

        String propertyId = null;
        if (getArguments() != null) {
            propertyId = getArguments().getString("propertyId");
        }
        final String fPropertyId = propertyId;

        // If we are editing an existing item, load its data
        final boolean isEditing = getArguments() != null && getArguments().containsKey("itemId");
        final String itemId = isEditing ? getArguments().getString("itemId") : null;

        if (isEditing) {
            try {
                // Find the matching text data from storage
                List<PropertyImage> items = PhotoStorageManager.loadPropertyImageData(requireContext(), fPropertyId);
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
                } else {
                    System.out.println("THIS IS A PROBLEM " + itemId + ".png");
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), "Failed to load existing photo data.", Toast.LENGTH_SHORT).show();
            }
        }

        // Set up the more_vert overflow menu
        buttonMoreOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), buttonMoreOptions);
            popup.inflate(R.menu.menu_photo_options);

            // Hide delete if this is a new item
            Menu menu = popup.getMenu();
            MenuItem deleteItem = menu.findItem(R.id.action_delete);
            if (deleteItem != null) {
                deleteItem.setVisible(isEditing);
            }

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_save) {
                    savePhotoData(fPropertyId);
                    return true;
                } else if (id == R.id.action_delete && isEditing) {
                    deletePhotoData(fPropertyId, itemId);
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    private void savePhotoData(String propertyId) {
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

        buttonMoreOptions.setEnabled(false);

        // If editing an existing item AND the image wasn't changed, only update metadata (fast)
        String editingId = (getArguments() != null) ? getArguments().getString("itemId") : null;
        boolean needsImageWrite = (editingId == null) || bitmapChanged;

        if (needsImageWrite) {
            Toast.makeText(requireContext(), "Saving image, this may take a moment...", Toast.LENGTH_SHORT).show();
        }

        PhotoStorageManager.SaveCallback saveCallback = new PhotoStorageManager.SaveCallback() {
            @Override
            public void onSuccess(String id) {
                if (!isAdded())
                    return;
                Toast.makeText(requireContext(), "Saved securely!", Toast.LENGTH_SHORT).show();
                dismissWithCallback();
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded())
                    return;
                e.printStackTrace();
                Toast.makeText(requireContext(), "Error saving data.", Toast.LENGTH_SHORT).show();
                buttonMoreOptions.setEnabled(true);
            }
        };

        if (editingId != null && !bitmapChanged) {
            // Text-only edit: skip the file write entirely
            PhotoStorageManager.updatePhotoMetadataOnly(requireContext(), propertyId, editingId, label, notes, saveCallback);
        } else if (editingId != null) {
            // Existing item with a new photo taken
            PhotoStorageManager.updatePhotoData(requireContext(), propertyId, editingId, label, notes, currentBitmap,
                    saveCallback);
        } else {
            // Brand new item
            PhotoStorageManager.savePhotoData(requireContext(), propertyId, label, notes, currentBitmap, saveCallback);
        }
    }

    private void deletePhotoData(String propertyId, String itemId) {
        buttonMoreOptions.setEnabled(false);

        PhotoStorageManager.deletePhotoData(requireContext(), propertyId, itemId,
                new PhotoStorageManager.SaveCallback() {
                    @Override
                    public void onSuccess(String id) {
                        if (!isAdded())
                            return;
                        Toast.makeText(requireContext(), "Photo deleted.", Toast.LENGTH_SHORT).show();
                        dismissWithCallback();
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isAdded())
                            return;
                        e.printStackTrace();
                        Toast.makeText(requireContext(), "Error deleting data.", Toast.LENGTH_SHORT).show();
                        buttonMoreOptions.setEnabled(true);
                    }
                });
    }

    private void dismissWithCallback() {
        if (dismissCallback != null) {
            dismissCallback.onPhotoDismissed();
        }
        dismiss();
    }
}
