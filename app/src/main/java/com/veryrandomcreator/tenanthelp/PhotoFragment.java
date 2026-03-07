package com.veryrandomcreator.tenanthelp;

import android.graphics.Bitmap;
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

public class PhotoFragment extends Fragment {

    private ImageView imageViewPhoto;
    private EditText editTextNotes;
    private Button buttonTakePhoto;

    // Register the contract to take a picture and get the thumbnail as a Bitmap back.
    private final ActivityResultLauncher<Void> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicturePreview(),
            result -> {
                if (result != null) {
                    imageViewPhoto.setImageBitmap(result);
                }
            }
    );

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
        editTextNotes = view.findViewById(R.id.edit_text_notes);
        buttonTakePhoto = view.findViewById(R.id.button_take_photo);

        buttonTakePhoto.setOnClickListener(v -> {
            // Launch the camera app
            takePictureLauncher.launch(null);
        });
    }
}
