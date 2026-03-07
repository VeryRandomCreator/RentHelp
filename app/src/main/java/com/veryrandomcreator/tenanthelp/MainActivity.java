package com.veryrandomcreator.tenanthelp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;

import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> savePdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // This URI represents the exact location the user chose
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        writePdfToUri(selectedUri);
                    }
                }
            });

    private void writePdfToUri(Uri uri) {
        // NOTE: For production apps, execute this block on a background thread!

        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            if (outputStream != null) {
                // Assuming 'myPdfDocument' is your ready-to-save PdfDocument instance
                output.writeTo(outputStream);

                // Show a success message to the user here
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Show an error message to the user here
        } finally {
            // Always close the document when you are completely done with it
            if (output != null) {
                output.close();
            }
        }
    }

    private PdfDocument output;
    private androidx.recyclerview.widget.RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view_properties);
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        refreshPropertiesList();

        // Refresh the list any time a fragment pops back to this screen
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                refreshPropertiesList();
            }
        });

        com.google.android.material.floatingactionbutton.FloatingActionButton fabAddProperty = findViewById(
                R.id.fab_add_property);

        fabAddProperty.setOnClickListener(v -> {
            String newId = java.util.UUID.randomUUID().toString();
            Property newProperty = new Property(newId, "New Property", "");
            PropertyStorageManager.savePropertyData(this, newProperty, new PropertyStorageManager.SaveCallback() {
                @Override
                public void onSuccess(String id) {
                    PropertyPhotosListFragment fragment = new PropertyPhotosListFragment();
                    Bundle args = new Bundle();
                    args.putString("propertyId", id);
                    fragment.setArguments(args);

                    getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                            .add(R.id.fragmentContainerView, fragment)
                            .addToBackStack(null)
                            .commit();
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void refreshPropertiesList() {
        try {
            java.util.List<Property> properties = PropertyStorageManager.loadProperties(this);
            PropertiesAdapter adapter = new PropertiesAdapter(properties, item -> {
                PropertyPhotosListFragment fragment = new PropertyPhotosListFragment();
                Bundle args = new Bundle();
                args.putString("propertyId", item.getId());
                fragment.setArguments(args);

                getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                        .add(R.id.fragmentContainerView, fragment)
                        .addToBackStack(null)
                        .commit();
            });
            recyclerView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}