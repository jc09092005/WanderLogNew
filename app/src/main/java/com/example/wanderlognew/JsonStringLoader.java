package com.example.wanderlognew;

import android.content.Context;

import org.json.JSONObject;

import java.io.InputStream;

/**
 * Utility class for loading JSON-based string resources from the assets folder.
 * Used for localization fallback when translated strings are not yet cached.
 */
public class JsonStringLoader {
    private JSONObject strings; // Holds the key-value pairs from the JSON file

    // Constructor that loads the JSON file from the assets folder and parses it into a JSONObject
    public JsonStringLoader(Context context, String filename) {
        try {
            // Open the file and read its content into a byte array
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            // Convert the byte array into a UTF-8 string and parse as JSON
            String json = new String(buffer, "UTF-8");
            strings = new JSONObject(json);
        } catch (Exception e) {
            // In the case of error user an empty object
            e.printStackTrace();
            strings = new JSONObject();
        }
    }

    // Retrieves a string value by its key. If the key is not found, returns the key itself as fallback
    public String getString(String key) {
        return strings.optString(key, key); // fallback to key if not found
    }
}
