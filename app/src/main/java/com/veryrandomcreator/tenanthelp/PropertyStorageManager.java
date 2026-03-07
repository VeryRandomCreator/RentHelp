package com.veryrandomcreator.tenanthelp;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PropertyStorageManager {

    private static final String PREFS_FILENAME = "secure_properties_prefs";
    private static final String KEY_PROPERTIES_JSON = "properties_json";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SaveCallback {
        void onSuccess(String id);
        void onError(Exception e);
    }

    public static void savePropertyData(Context context, Property property, SaveCallback callback) {
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

                String existingJson = sharedPreferences.getString(KEY_PROPERTIES_JSON, "[]");
                JSONArray jsonArray = new JSONArray(existingJson);

                boolean found = false;
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    if (obj.getString("id").equals(property.getId())) {
                        obj.put("label", property.getLabel());
                        obj.put("description", property.getDescription());
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    JSONObject newItem = new JSONObject();
                    newItem.put("id", property.getId());
                    newItem.put("label", property.getLabel());
                    newItem.put("description", property.getDescription());
                    jsonArray.put(newItem);
                }

                sharedPreferences.edit().putString(KEY_PROPERTIES_JSON, jsonArray.toString()).apply();

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess(property.getId());
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    public static void deletePropertyData(Context context, String id, SaveCallback callback) {
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

                String existingJson = sharedPreferences.getString(KEY_PROPERTIES_JSON, "[]");
                JSONArray jsonArray = new JSONArray(existingJson);
                JSONArray newArray = new JSONArray();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    if (!obj.getString("id").equals(id)) {
                        newArray.put(obj);
                    }
                }

                sharedPreferences.edit().putString(KEY_PROPERTIES_JSON, newArray.toString()).apply();

                // Also we should ideally delete all photos associated with this property
                // But for now we just delete the property metadata

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

    public static List<Property> loadProperties(Context context) throws GeneralSecurityException, IOException, JSONException {
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

        String jsonString = sharedPreferences.getString(KEY_PROPERTIES_JSON, "[]");
        JSONArray jsonArray = new JSONArray(jsonString);

        List<Property> items = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);
            String id = obj.getString("id");
            String label = obj.optString("label", "");
            String description = obj.optString("description", "");
            items.add(new Property(id, label, description));
        }

        return items;
    }
    
    public static Property loadProperty(Context context, String id) throws GeneralSecurityException, IOException, JSONException {
        List<Property> all = loadProperties(context);
        for (Property p : all) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }
}
