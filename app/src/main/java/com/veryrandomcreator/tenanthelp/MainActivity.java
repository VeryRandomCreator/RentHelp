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
            }
    );

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

    private Button button;
    private Button savePdf;

    private PdfDocument output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PropertyPackage propertyPackage = new PropertyPackage();
        propertyPackage.fetchLatestHash();
        try {
            propertyPackage.setImages(PhotoStorageManager.loadPhotos(this));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        button = findViewById(R.id.temp_btn);
        savePdf = findViewById(R.id.savePdf);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager().beginTransaction().setReorderingAllowed(true)
                        .add(R.id.fragmentContainerView, PropertyPhotosListFragment.class, null).commit();
            }
        });
        
        savePdf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    output = propertyPackage.generatePdf(getApplicationContext());

                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

                    intent.addCategory(Intent.CATEGORY_OPENABLE);

                    intent.setType("application/pdf");

                    intent.putExtra(Intent.EXTRA_TITLE, "Timestamped-Inspection.pdf");

                    savePdfLauncher.launch(intent);
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}