package com.veryrandomcreator.tenanthelp;

import static java.util.UUID.randomUUID;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;

import java.io.IOException;
import java.io.OutputStream;

// fetch the latest blockchain hash every time an image is taken (in case it takes time to take all the photos)
public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recycler_view_properties);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
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
            String newId = randomUUID().toString();
            Property newProperty = new Property(newId, "New Property", "");
            PropertyStorageManager.savePropertyData(this, newProperty, new PropertyStorageManager.SaveCallback() {
                @Override
                public void onSuccess(String id) {
                    PropertyFragment fragment = new PropertyFragment();
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
                PropertyFragment fragment = new PropertyFragment();
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