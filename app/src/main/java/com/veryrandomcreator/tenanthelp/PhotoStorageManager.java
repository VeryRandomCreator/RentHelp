package com.veryrandomcreator.tenanthelp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PhotoStorageManager {

    private static final String PREFS_FILENAME = "secure_photo_prefs";
    private static final String BASE_KEY_PHOTOS_JSON = "photos_json_";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SaveCallback {
        void onSuccess(String id);
        void onError(Exception e);
    }

    /*
    // Updates an existing bitmap in storage at id dest with bitmap
    // Does nothing if dest does not correspond to existing image
    public static void updateBitmap(Context context, Bitmap bitmap, String dest) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        File file = new File(context.getFilesDir(), dest + ".png");
        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        if (!file.exists()) {
            return;
        }

        try (FileOutputStream outputStream = encryptedFile.openFileOutput()) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        }
    }

     */


    public static void savePhotoData(Context context, String propertyId, String label, String notes, Bitmap bitmap, SaveCallback callback) {
        executor.execute(() -> {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                // 1. Generate a unique ID for this item
                String id = UUID.randomUUID().toString();

                // 2. Save the Bitmap to an EncryptedFile
                File file = new File(context.getFilesDir(), id + ".png");
                EncryptedFile encryptedFile = new EncryptedFile.Builder(
                        context,
                        file,
                        masterKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build();

                if (file.exists()) {
                    file.delete();
                }

                try (FileOutputStream outputStream = encryptedFile.openFileOutput()) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }

                // 3. Save the metadata to EncryptedSharedPreferences
                EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILENAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                String prefsKey = BASE_KEY_PHOTOS_JSON + propertyId;
                System.out.println("SAVED TO " + id + ".png");
                JSONArray jsonArray;
                String existingJson = sharedPreferences.getString(prefsKey, "[]");
                jsonArray = new JSONArray(existingJson);

                JSONObject newItem = new JSONObject();
                newItem.put("id", id);
                newItem.put("label", label);
                newItem.put("notes", notes);

                jsonArray.put(newItem);

                sharedPreferences.edit().putString(prefsKey, jsonArray.toString()).apply();

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(id);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    // Updates an existing photo entry: overwrites the bitmap file and patches label/notes in JSON
    public static void updatePhotoData(Context context, String propertyId, String existingId, String label, String notes, Bitmap bitmap, SaveCallback callback) {
        executor.execute(() -> {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                // 1. Overwrite the existing encrypted PNG file
                File file = new File(context.getFilesDir(), existingId + ".png");
                if (file.exists()) {
                    file.delete();
                }

                EncryptedFile encryptedFile = new EncryptedFile.Builder(
                        context,
                        file,
                        masterKey,
                        EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build();

                try (FileOutputStream outputStream = encryptedFile.openFileOutput()) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                }

                // 2. Patch the metadata entry in EncryptedSharedPreferences
                EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILENAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                String prefsKey = BASE_KEY_PHOTOS_JSON + propertyId;
                String existingJson = sharedPreferences.getString(prefsKey, "[]");
                JSONArray jsonArray = new JSONArray(existingJson);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    if (obj.getString("id").equals(existingId)) {
                        obj.put("label", label);
                        obj.put("notes", notes);
                        break;
                    }
                }

                sharedPreferences.edit().putString(prefsKey, jsonArray.toString()).apply();

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(existingId);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    // Updates only the label and notes in the JSON, skipping the (expensive) file write entirely
    public static void updatePhotoMetadataOnly(Context context, String propertyId, String existingId, String label, String notes, SaveCallback callback) {
        executor.execute(() -> {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILENAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                String prefsKey = BASE_KEY_PHOTOS_JSON + propertyId;
                String existingJson = sharedPreferences.getString(prefsKey, "[]");
                JSONArray jsonArray = new JSONArray(existingJson);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    if (obj.getString("id").equals(existingId)) {
                        obj.put("label", label);
                        obj.put("notes", notes);
                        break;
                    }
                }

                sharedPreferences.edit().putString(prefsKey, jsonArray.toString()).apply();

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(existingId);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    public static void deletePhotoData(Context context, String propertyId, String id, SaveCallback callback) {
        executor.execute(() -> {
            try {
                MasterKey masterKey = new MasterKey.Builder(context)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();

                // 1. Delete the image file if it exists
                File file = new File(context.getFilesDir(), id + ".png");
                if (file.exists()) {
                    file.delete();
                }

                // 2. Remove the entry from EncryptedSharedPreferences
                EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                        context,
                        PREFS_FILENAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );

                String prefsKey = BASE_KEY_PHOTOS_JSON + propertyId;
                String existingJson = sharedPreferences.getString(prefsKey, "[]");
                JSONArray jsonArray = new JSONArray(existingJson);
                JSONArray newArray = new JSONArray();

                // Copy all items EXCEPT the one mapping to our deleted ID
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    if (!obj.getString("id").equals(id)) {
                        newArray.put(obj);
                    }
                }

                sharedPreferences.edit().putString(prefsKey, newArray.toString()).apply();

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(id);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    public static List<PropertyImage> loadPropertyImageData(Context context, String propertyId) throws GeneralSecurityException, IOException, JSONException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        EncryptedSharedPreferences sharedPreferences = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                context,
                PREFS_FILENAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        String prefsKey = BASE_KEY_PHOTOS_JSON + propertyId;
        String jsonString = sharedPreferences.getString(prefsKey, "[]");
        JSONArray jsonArray = new JSONArray(jsonString);

        List<PropertyImage> items = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String id = obj.getString("id");
            String label = obj.getString("label");
            String notes = obj.getString("notes");
            items.add(new PropertyImage(id, label, notes));
        }

        return items;
    }

    public static Bitmap loadPhotoBitmap(Context context, String id, boolean mutable) throws GeneralSecurityException, IOException {
        MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        File file = new File(context.getFilesDir(), id + ".png");
        if (!file.exists()) {
            System.out.println("IT DOESN'T EXIST: " + id + ".png");
            return null;
        }

        EncryptedFile encryptedFile = new EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build();

        try (InputStream inputStream = encryptedFile.openFileInput()) {
            if (mutable) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                return BitmapFactory.decodeStream(inputStream, null, options);
            }

            return BitmapFactory.decodeStream(inputStream);
        }
    }
}
