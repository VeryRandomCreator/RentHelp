package com.veryrandomcreator.tenanthelp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO:
//  - UPDATE RECYCLERVIEW AFTER NEW ITEM HAS BEEN ADDED (ONCE FRAGMENT HAS BEEN POPPED BACK INTO STACK)
public class PropertyFragment extends Fragment {

    private final ActivityResultLauncher<Uri> openDirectoryLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(),
            uri -> {
                if (uri != null) {
                    // The user selected a directory
                    writeOutputToUri(uri);
                } else {
                    // The user cancelled the directory picker
                    hideProgress();
                }
            });

    private byte[] signature;
    private byte[] pdf;
    private byte[] certificateChain;

    private PropertyPackage propertyPackage;
    private String propertyId;
    private EditText labelEditText;
    private EditText descriptionEditText;
    private RecyclerView recyclerView;
    
    // UI elements for progress
    private LinearLayout progressOverlay;
    private TextView progressStatusText;
    private Handler mainHandler;

    public PropertyFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_property_photos_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        propertyPackage = new PropertyPackage();
        mainHandler = new Handler(Looper.getMainLooper());
        
        progressOverlay = view.findViewById(R.id.layout_progress_overlay);
        progressStatusText = view.findViewById(R.id.text_view_progress_status);

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
        FloatingActionButton fabPropertyOptions = view.findViewById(R.id.fab_property_options);

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

        // Set up the Property options FAB click listener
        fabPropertyOptions.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), fabPropertyOptions);
            popup.getMenuInflater().inflate(R.menu.menu_property_options, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_generate_pdf) {
                    generatePdf();
                    return true;
                }
                return false;
            });

            popup.show();
        });
    }

    private void updateProgress(String status) {
        if (mainHandler != null && progressStatusText != null) {
            mainHandler.post(() -> progressStatusText.setText(status));
        }
    }

    private void hideProgress() {
        if (mainHandler != null && progressOverlay != null) {
            mainHandler.post(() -> progressOverlay.setVisibility(View.GONE));
        }
    }

    private void showProgress(String initialStatus) {
        if (progressOverlay != null && progressStatusText != null) {
            progressStatusText.setText(initialStatus);
            progressOverlay.setVisibility(View.VISIBLE);
        }
    }

    // only going to be called after signature, certificate chain, and pdf have been
    // set
    private void writeOutputToUri(Uri treeUri) {
        // 1. Get the directory document from the URI
        DocumentFile directory = DocumentFile.fromTreeUri(getContext(), treeUri);

        if (directory == null || !directory.canWrite()) {
            return;
        }

        try {
            // Using "application/octet-stream" for raw binary signatures
            saveBytesToFile(getContext(), directory, "signature.sig", "application/octet-stream", signature);

            // "application/x-x509-ca-cert" is standard for PEM/Certificates
            saveBytesToFile(getContext(), directory, "certificates.pem", "application/x-x509-ca-cert",
                    certificateChain);

            saveBytesToFile(getContext(), directory, "document.pdf", "application/pdf", pdf);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to save files: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            hideProgress();
        }
    }

    private void saveBytesToFile(Context context, DocumentFile directory, String fileName, String mimeType, byte[] data)
            throws IOException {
        // Create the file in the selected directory
        DocumentFile file = directory.createFile(mimeType, fileName);

        if (file == null) {
            throw new IOException("Failed to create file: " + fileName);
        }

        // Open an OutputStream via the ContentResolver using the file's Uri
        try (OutputStream os = context.getContentResolver().openOutputStream(file.getUri())) {
            if (os != null) {
                os.write(data);
                os.flush();
            } else {
                throw new IOException("OutputStream was null for: " + fileName);
            }
        }
    }

    private void generatePdf() {
        showProgress("Starting PDF generation...");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {
                updateProgress("Fetching latest blockchain hash...");
                HardwareKeyManager.generateAttestedKey(PhotoStorageManager.getLatestHash().getBytes());

                updateProgress("Generating PDF document...");
                PdfDocument pdfDocument = propertyPackage.generatePdf(getContext());

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                pdfDocument.writeTo(outputStream);
                pdf = outputStream.toByteArray();

                updateProgress("Signing PDF document...");
                signature = HardwareKeyManager.signPdfDocument(pdf);
                pdfDocument.close();

                updateProgress("Retrieving certificate chain...");
                certificateChain = HardwareKeyManager.getCertificateChainBytes();

                // Launch directory picker. Progress will be hidden when selection finishes (in writeOutputToUri)
                // or if the user cancels (you might want to handle cancellation in the launcher to hide it).
                mainHandler.post(() -> openDirectoryLauncher.launch(null));
                
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    hideProgress();
                    Toast.makeText(getContext(), "Error during PDF generation: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    hideProgress();
                    Toast.makeText(getContext(), "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void refreshPhotosList() {
        // Load actual data from secure storage
        try {
            propertyPackage.setLabel(labelEditText.getText().toString());
            propertyPackage.setDescription(descriptionEditText.getText().toString());
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
